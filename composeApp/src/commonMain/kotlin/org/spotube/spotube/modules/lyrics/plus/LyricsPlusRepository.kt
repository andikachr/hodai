/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.modules.lyrics.plus

import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsResponse
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

/** Fully resolved lyrics for one track, with the Lirik+ layers attached. */
data class LyricsPlusResult(
    val syncedLyrics: List<LyricsLine>?,
    val plainLyrics: String?,
    val songLanguage: String,
    /** Aligned with the display lines (synced lines, or plain lines when unsynced). */
    val romajiLines: List<String?>?,
    /** Aligned the same way; null when the song already is in the app language. */
    val translationLines: List<String?>?)

/**
 * Orchestrates Lirik+ for one track:
 *
 * 1. Serve the per-track cache when present (same app language).
 * 2. Otherwise take the selected plugin's lyrics; when they are empty, walk
 *    the [FallbackLyricsSource] chain instead of giving up.
 * 3. Detect the song language, then derive layers under the smart rules:
 *    romaji only for Japanese songs, translation only when the song language
 *    differs from the app language.
 * 4. Persist the whole resolution so reopening the song is instant.
 */
class LyricsPlusRepository(
    private val fallbackSource: FallbackLyricsSource,
    private val translator: LyricsTranslator,
    private val romajiProvider: RomajiProvider,
    private val cache: LyricsPlusCache) {

    suspend fun resolve(
        track: MetadataTrack,
        primary: LyricsResponse?,
        appLanguage: String,
        romajiEnabled: Boolean = true,
        translationEnabled: Boolean = true): LyricsPlusResult? {

        // The layer toggles change the result, so they ride along in the
        // cache key: flipping a switch must not serve the other variant.
        // The "v2" prefix orphans entries computed before the reading
        // layer covered Korean and Mandarin: those entries stored a null
        // reading for songs that can now produce one.
        val cacheLanguage =
            "v2|$appLanguage|r${if (romajiEnabled) 1 else 0}t${if (translationEnabled) 1 else 0}"

        var heldPlainCache: LyricsPlusResult? = null
        cache.read(track.id, cacheLanguage)?.let { entry ->
            // The cached layers were computed over the cached lyrics text.
            // If the plugin now serves a different edit of the same song
            // (lines split or joined differently), serving the cache would
            // pair those layers with lines they were never aligned to —
            // the one-line translation drift. Recompute instead.
            val cachedLines = entry.syncedLyrics?.map { it.text }
                ?: entry.plainLyrics?.split("\n")
            val primaryLines = primary?.syncedLyrics?.map { it.text }
                ?: primary?.plainLyrics?.split("\n")
            val cacheResult = LyricsPlusResult(
                syncedLyrics = entry.syncedLyrics,
                plainLyrics = entry.plainLyrics,
                songLanguage = entry.songLanguage,
                romajiLines = entry.romajiLines,
                translationLines = entry.translationLines)
            // A plain-only cache entry must not lock out the fallback
            // chain when neither it nor the plugin has synced lyrics:
            // the chain's fuzzy search may well hold the synced lyrics
            // the plugin's exact match missed (or answered plain-only).
            // Hold the entry as a floor instead and let the chain try.
            val lockOut = entry.syncedLyrics.isNullOrEmpty() &&
                primary?.syncedLyrics?.isNotEmpty() != true
            if ((primaryLines == null || cachedLines == null ||
                    primaryLines == cachedLines) && !lockOut
            ) {
                return cacheResult
            }
            if (lockOut) heldPlainCache = cacheResult
        }

        // The plugin is the answer only when it has SYNCED lyrics. A
        // plain-only plugin answer does not end the search: the fallback
        // chain hunts synced lyrics first and its plain result is only a
        // floor, exactly as between the chain's own sources.
        var source = SOURCE_PLUGIN
        var response = primary?.takeIf { it.syncedLyrics?.isNotEmpty() == true }
        if (response == null) {
            val fallback = fallbackSource.getLyrics(track)
            if (fallback != null &&
                fallback.response.syncedLyrics?.isNotEmpty() == true
            ) {
                response = fallback.response
                source = fallback.source
            } else {
                response = primary?.takeIf { !it.plainLyrics.isNullOrBlank() }
                    ?: fallback?.response
                    ?: return heldPlainCache ?: return null
                if (fallback != null && response == fallback.response) {
                    source = fallback.source
                }
            }
        }

        val displayLines = response.syncedLyrics?.map { it.text }
            ?: response.plainLyrics?.split("\n")
            ?: return null

        val songLanguage = LyricsLanguageDetector.detect(displayLines)

        val romajiLines = if (romajiEnabled && songLanguage in ReadingRomanizer.SUPPORTED_LANGUAGES) {
            ReadingRomanizer.romanize(displayLines, songLanguage, romajiProvider)
                ?.takeIf { it.size == displayLines.size }
                ?.map { it as String? }
        } else {
            null
        }

        val appPrimaryLanguage = appLanguage.substringBefore('-').lowercase()
        val translationLines = if (translationEnabled && songLanguage != appPrimaryLanguage) {
            translator.translate(displayLines, appLanguage)?.mapIndexed { index, line ->
                // A line already written in the app language comes back from
                // the translator unchanged; showing it twice helps nobody.
                if (line != null &&
                    line.trim().equals(displayLines[index].trim(), ignoreCase = true)
                ) {
                    null
                } else {
                    line
                }
            }
        } else {
            null
        }

        val result = LyricsPlusResult(
            syncedLyrics = response.syncedLyrics,
            plainLyrics = response.plainLyrics,
            songLanguage = songLanguage,
            romajiLines = romajiLines,
            translationLines = translationLines)

        cache.write(
            LyricsPlusCacheEntry(
                trackId = track.id,
                targetLanguage = cacheLanguage,
                songLanguage = songLanguage,
                source = source,
                syncedLyrics = response.syncedLyrics,
                plainLyrics = response.plainLyrics,
                romajiLines = romajiLines,
                translationLines = translationLines))

        return result
    }

    companion object {
        const val SOURCE_PLUGIN = "plugin"
    }
}
