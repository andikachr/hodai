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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.paths.Paths

/**
 * One cached Lirik+ resolution for a track: the lyrics that were found
 * (whatever source won), the detected song language, and the derived layers.
 * Keyed by track id and only valid for the [targetLanguage] it was computed
 * for, so switching the app language re-derives the translation layer.
 */
@Serializable
data class LyricsPlusCacheEntry(
    val trackId: String,
    val targetLanguage: String,
    val songLanguage: String,
    val source: String,
    val syncedLyrics: List<LyricsLine>? = null,
    val plainLyrics: String? = null,
    val romajiLines: List<String?>? = null,
    val translationLines: List<String?>? = null)

/**
 * File-per-track JSON cache in the app cache dir (`lyrics_plus/`), following
 * the same okio + [Paths] pattern as `CacheManager`. All failures are
 * swallowed: a broken cache must never break the lyrics screen.
 */
class LyricsPlusCache(
    private val paths: Paths) {

    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val json: Json = Json { ignoreUnknownKeys = true }

    suspend fun read(trackId: String, targetLanguage: String): LyricsPlusCacheEntry? =
        withContext(Dispatchers.IO) {
            try {
                val file = fileFor(trackId)
                if (!fileSystem.exists(file)) return@withContext null
                val entry = fileSystem.source(file).buffer().use { source ->
                    json.decodeFromString<LyricsPlusCacheEntry>(source.readUtf8())
                }
                entry.takeIf { it.targetLanguage == targetLanguage }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun write(entry: LyricsPlusCacheEntry) {
        withContext(Dispatchers.IO) {
            try {
                val file = fileFor(entry.trackId)
                file.parent?.let { fileSystem.createDirectories(it) }
                fileSystem.sink(file).buffer().use { sink ->
                    sink.writeUtf8(json.encodeToString(entry))
                }
            } catch (e: Exception) {
                // Cache is best-effort; the lyrics on screen are unaffected.
            }
        }
    }

    private fun fileFor(trackId: String): Path {
        val dir = paths.getApplicationCacheDirPath().toPath() / CACHE_DIR_NAME
        return dir / (sanitize(trackId) + ".json")
    }

    /** Track ids come from remote catalogues; keep them filesystem-safe. */
    private fun sanitize(trackId: String): String =
        trackId.map { ch -> if (ch.isLetterOrDigit() || ch == '-' || ch == '_') ch else '_' }
            .joinToString("")

    companion object {
        private const val CACHE_DIR_NAME = "lyrics_plus"
    }
}
