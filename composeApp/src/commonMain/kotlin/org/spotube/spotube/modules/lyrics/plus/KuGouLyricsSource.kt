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
import io.ktor.http.takeFrom
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsResponse
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.network.HttpClientFactory
import kotlin.math.abs

/** Word-timed lyrics found on KuGou: the response plus its word segments. */
data class KuGouResult(
    val response: LyricsResponse,
    /** Aligned with `response.syncedLyrics`; null entries are wordless lines. */
    val wordLines: List<LyricsWordLine?>)

/**
 * KuGou (KRC) — the word-timed lyrics source, and the preferred source of
 * the Lirik+ chain when it has the song: karaoke timings or nothing.
 *
 * The lookup is the three-step dance every KuGou client performs:
 *
 * 1. **Song search** (`mobileservice.kugou.com/api/v3/search/song`) finds
 *    the track's KuGou hash. Only duration-plausible hits are trusted —
 *    the same 15-second gate the line-timed fallback chain learned in v3:
 *    a live or cover version's word timings cannot fit this track.
 * 2. **Lyrics search** (`lyrics.kugou.com/search`) turns a hash (or, as a
 *    last resort, the bare keyword) into lyrics candidates; the first
 *    duration-plausible candidate wins.
 * 3. **Download** (`lyrics.kugou.com/download`, `fmt=krc`) returns the
 *    masked, deflated KRC payload that [KrcParser] decodes.
 *
 * Every failure is silent and yields null, so the caller falls back to
 * the line-timed chain (plugin, then [FallbackLyricsSource]) unchanged.
 */
class KuGouLyricsSource(
    private val httpClientFactory: HttpClientFactory) {

    suspend fun getWordLyrics(track: MetadataTrack): KuGouResult? =
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                searchAndFetch(track)
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun searchAndFetch(track: MetadataTrack): KuGouResult? {
        val artist = track.artists.firstOrNull()?.name.orEmpty()
        val keyword = (track.title + " " + artist).trim()
        if (keyword.isEmpty()) return null

        val searchResponse = httpClientFactory.json.get {
            url {
                takeFrom("https://mobileservice.kugou.com/api/v3/search/song")
                parameters.append("format", "json")
                parameters.append("keyword", keyword)
                parameters.append("page", "1")
                parameters.append("pagesize", "5")
                parameters.append("showtype", "1")
            }
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
        }.body<JsonObject>()

        val infos = searchResponse["data"]?.jsonObject
            ?.get("info")?.jsonArray
            ?: return fetchForHash(track, hash = null, keyword = keyword)

        data class SongHit(val hash: String, val durationMs: Long?)

        val hits = infos.mapNotNull { item ->
            val obj = item.jsonObject
            val hash = obj["hash"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val durationMs = obj["duration"]?.jsonPrimitive?.doubleOrNull
                ?.let { (it * 1000).toLong() }
            SongHit(hash, durationMs)
        }
        if (hits.isEmpty()) return fetchForHash(track, hash = null, keyword = keyword)

        // Duration-plausible hits first (stable order otherwise), then
        // the rest: a wrong-version hash usually yields no fitting
        // lyrics candidate anyway, so trying it costs little.
        val ordered = hits.sortedBy { hit ->
            if (isDurationPlausible(track, hit.durationMs)) 0 else 1
        }
        for (hit in ordered.take(MAX_SONG_CANDIDATES)) {
            fetchForHash(track, hash = hit.hash, keyword = keyword)?.let { return it }
        }
        return fetchForHash(track, hash = null, keyword = keyword)
    }

    private suspend fun fetchForHash(
        track: MetadataTrack,
        hash: String?,
        keyword: String): KuGouResult? {
        val search = httpClientFactory.json.get {
            url {
                takeFrom("https://lyrics.kugou.com/search")
                parameters.append("ver", "1")
                parameters.append("man", "yes")
                parameters.append("client", "pc")
                parameters.append("keyword", keyword)
                if (track.durationMs > 0) {
                    parameters.append("duration", track.durationMs.toString())
                }
                if (hash != null) parameters.append("hash", hash)
            }
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
        }.body<JsonObject>()

        val candidates = search["candidates"]?.jsonArray
            ?.map { it.jsonObject }
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        // The lyrics candidate carries its own duration (milliseconds):
        // prefer the first plausible one, else the first candidate.
        val chosen = candidates.firstOrNull { candidate ->
            isDurationPlausible(
                track,
                candidate["duration"]?.jsonPrimitive?.doubleOrNull?.toLong())
        } ?: candidates.first()

        val id = chosen["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val accessKey = chosen["accesskey"]?.jsonPrimitive?.contentOrNull ?: return null

        val download = httpClientFactory.json.get {
            url {
                takeFrom("https://lyrics.kugou.com/download")
                parameters.append("ver", "1")
                parameters.append("client", "pc")
                parameters.append("id", id)
                parameters.append("accesskey", accessKey)
                parameters.append("fmt", "krc")
                parameters.append("charset", "utf8")
            }
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
        }.body<JsonObject>()

        val content = download["content"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val krcText = KrcParser.decodeContent(content) ?: return null
        val parsed = KrcParser.parse(krcText)

        // Drop KuGou's liner-note lines (composer/lyricist credits) the
        // same way the NetEase source drops its credit lines.
        val kept = parsed.lines.indices.filter { index ->
            val text = parsed.lines[index].text
            text.isNotBlank() && !isCreditLine(text)
        }
        if (kept.size < MIN_LINES) return null
        val lines = kept.map { parsed.lines[it] }
        val wordLines = kept.map { parsed.wordLines[it] }
        if (wordLines.all { it == null }) return null

        // Coverage sanity: word timings ending far past the track's own
        // duration mean a different edit slipped through the gates.
        if (track.durationMs > 0) {
            val lastEnd = wordLines.filterNotNull()
                .maxOfOrNull { line -> line.words.maxOfOrNull { it.endMs } ?: 0L }
                ?: lines.last().time
            if (lastEnd > track.durationMs + MAX_DURATION_OVERSHOOT_MS) return null
        }

        return KuGouResult(
            response = LyricsResponse(
                syncedLyrics = lines,
                plainLyrics = lines.joinToString("\n") { it.text }),
            wordLines = wordLines)
    }

    private fun isDurationPlausible(track: MetadataTrack, candidateMs: Long?): Boolean =
        track.durationMs <= 0 || candidateMs == null ||
            abs(candidateMs - track.durationMs) <= MAX_DURATION_DIFF_MS

    /** KRC files open with composer/lyricist credit lines; drop them. */
    private fun isCreditLine(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("作词") || trimmed.startsWith("作曲") ||
            trimmed.startsWith("编曲") || trimmed.startsWith("制作")
    }

    companion object {
        const val SOURCE_KUGOU = "kugou-krc"

        /**
         * One timeout for the whole three-step lookup. Longer than the
         * line-timed sources' 7 s because the steps are sequential, still
         * short enough that a dead KuGou never stalls the lyrics screen.
         */
        private const val TIMEOUT_MS = 12_000L
        private const val BROWSER_USER_AGENT = "Mozilla/5.0"
        private const val MAX_SONG_CANDIDATES = 3
        private const val MIN_LINES = 3

        /** Same wrong-version gate as the line-timed fallback chain. */
        private const val MAX_DURATION_DIFF_MS = 15_000L

        /** How far word timings may run past the track before distrust. */
        private const val MAX_DURATION_OVERSHOOT_MS = 20_000L
    }
}
