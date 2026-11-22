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

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsResponse
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.network.HttpClientFactory
import kotlin.math.abs

/** Lyrics found by a fallback source, tagged with which source produced them. */
data class FallbackResult(
    val response: LyricsResponse,
    val source: String)

/**
 * Second-chance lyrics coverage for when the selected lyrics plugin comes
 * back empty. Sources are tried in a fixed order and every failure is silent:
 *
 * 1. **LRCLib `/api/search`** — the plugin only calls `/api/get` (exact match
 *    on title + artist + album + duration), whose response already carries
 *    both synced and plain lyrics, so there is no separate plain-text path to
 *    add there. `/api/search` is the genuinely different second LRCLib path:
 *    a fuzzy search whose best candidate (synced first, then closest
 *    duration) often exists when the exact match does not.
 * 2. **NetEase** (`music.163.com`) — best effort: cloud search for the song
 *    id, then its LRC. Short timeout, any error means "no lyrics here".
 * 3. **lyrics.ovh** — plain (unsynced) lyrics only, as a last resort so the
 *    Plain tab still has something when nobody has timings.
 *
 * A synced result from a later source beats a plain-only result from an
 * earlier one; a plain-only result is kept as a floor.
 */
class FallbackLyricsSource(
    private val httpClientFactory: HttpClientFactory) {

    suspend fun getLyrics(track: MetadataTrack): FallbackResult? {
        var plainOnly: FallbackResult? = null

        lrcLibSearch(track)?.let { response ->
            if (response.syncedLyrics?.isNotEmpty() == true) {
                return FallbackResult(response, SOURCE_LRCLIB_SEARCH)
            }
            plainOnly = FallbackResult(response, SOURCE_LRCLIB_SEARCH)
        }

        netEase(track)?.let { response ->
            if (response.syncedLyrics?.isNotEmpty() == true) {
                return FallbackResult(response, SOURCE_NETEASE)
            }
            if (plainOnly == null) plainOnly = FallbackResult(response, SOURCE_NETEASE)
        }

        if (plainOnly == null) {
            lyricsOvh(track)?.let { response ->
                plainOnly = FallbackResult(response, SOURCE_LYRICS_OVH)
            }
        }

        return plainOnly
    }

    // region LRCLib search

    private suspend fun lrcLibSearch(track: MetadataTrack): LyricsResponse? =
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                // YouTube Music titles Japanese songs as "正解 - Seikai":
                // one combined string that no lyrics database indexes, so
                // the exact title finds nothing while each half does. Try
                // the full title first, then each side of the dash.
                val usable = mutableListOf<LrcLibSearchItem>()
                for (candidate in titleVariants(track.title)) {
                    val items = httpClientFactory.json.get {
                        url {
                            takeFrom("https://lrclib.net/api/search")
                            parameters.append("track_name", candidate)
                            track.artists.firstOrNull()
                                ?.let { parameters.append("artist_name", it.name) }
                        }
                    }.body<List<LrcLibSearchItem>>()
                    usable += items.filter {
                        !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()
                    }
                    if (usable.any { !it.syncedLyrics.isNullOrBlank() }) break
                }
                if (usable.isEmpty()) return@withTimeoutOrNull null

                val trackSeconds = if (track.durationMs > 0) track.durationMs / 1000.0 else null
                val best = usable.minWithOrNull(
                    compareBy(
                        { it.syncedLyrics.isNullOrBlank() },
                        { item: LrcLibSearchItem ->
                            if (trackSeconds != null && item.duration != null) {
                                abs(item.duration - trackSeconds)
                            } else {
                                Double.MAX_VALUE
                            }
                        })
                ) ?: return@withTimeoutOrNull null

                LyricsResponse(
                    syncedLyrics = best.syncedLyrics
                        ?.let { LrcParser.parse(it) }
                        ?.takeIf { it.isNotEmpty() },
                    plainLyrics = best.plainLyrics)
            } catch (e: Exception) {
                null
            }
        }

    // endregion

    // region NetEase

    private suspend fun netEase(track: MetadataTrack): LyricsResponse? =
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val query = (track.title + " " +
                    (track.artists.firstOrNull()?.name ?: "")).trim()
                val search = httpClientFactory.json.get {
                    url {
                        takeFrom("https://music.163.com/api/cloudsearch/pc")
                        parameters.append("s", query)
                        parameters.append("type", "1")
                        parameters.append("limit", "3")
                    }
                    header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
                }.body<JsonObject>()

                val songId = search["result"]?.jsonObject
                    ?.get("songs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("id")?.jsonPrimitive?.longOrNull
                    ?: return@withTimeoutOrNull null

                val lyricResponse = httpClientFactory.json.get {
                    url {
                        takeFrom("https://music.163.com/api/song/lyric")
                        parameters.append("os", "pc")
                        parameters.append("id", songId.toString())
                        parameters.append("lv", "-1")
                        parameters.append("kv", "-1")
                        parameters.append("tv", "-1")
                    }
                    header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
                }.body<JsonObject>()

                val lrcText = lyricResponse["lrc"]?.jsonObject
                    ?.get("lyric")?.jsonPrimitive?.contentOrNull
                    ?: return@withTimeoutOrNull null

                val lines = LrcParser.parse(lrcText)
                    .filterNot { isCreditLine(it.text) }
                if (lines.isEmpty()) return@withTimeoutOrNull null

                LyricsResponse(
                    syncedLyrics = lines,
                    plainLyrics = lines.joinToString("\n") { it.text })
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Title forms worth searching for: the full title, plus — for dual
     * titles joined by " - " (the YouTube Music "original - romaji"
     * convention) — each side on its own, original first, then romaji.
     */
    private fun titleVariants(title: String): List<String> {
        val parts = title.split(" - ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size < 2) return listOf(title)
        return (listOf(title) + parts).distinct()
    }

    /** NetEase LRC files open with composer/lyricist credit lines; drop them. */
    private fun isCreditLine(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("作词") || trimmed.startsWith("作曲") ||
            trimmed.startsWith("编曲") || trimmed.startsWith("制作")
    }

    // endregion

    // region lyrics.ovh

    private suspend fun lyricsOvh(track: MetadataTrack): LyricsResponse? =
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val artist = track.artists.firstOrNull()?.name
                    ?: return@withTimeoutOrNull null
                val response = httpClientFactory.json.get {
                    url {
                        takeFrom("https://api.lyrics.ovh/v1/")
                        appendPathSegments(artist, titleVariants(track.title).last())
                    }
                }.body<JsonObject>()

                val text = response["lyrics"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?: return@withTimeoutOrNull null

                LyricsResponse(syncedLyrics = null, plainLyrics = text)
            } catch (e: Exception) {
                null
            }
        }

    // endregion

    companion object {
        const val SOURCE_LRCLIB_SEARCH = "lrclib-search"
        const val SOURCE_NETEASE = "netease"
        const val SOURCE_LYRICS_OVH = "lyrics-ovh"

        private const val TIMEOUT_MS = 7_000L
        private const val BROWSER_USER_AGENT = "Mozilla/5.0"
    }
}

/** One candidate from LRCLib `/api/search` (same shape as `/api/get`). */
@Serializable
data class LrcLibSearchItem(
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null)
