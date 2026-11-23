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
import io.ktor.http.takeFrom
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.network.HttpJson

/**
 * Per-line lyric translation into the app language, via the keyless Google
 * Translate endpoint (`translate_a/single?client=gtx`). The source language
 * is left on `auto` — Google detects it per request, which stays correct even
 * when [LyricsLanguageDetector] could only say "other".
 *
 * Lines are translated in newline-joined batches (chunks of ~1200 chars). If
 * a batch comes back with a different line count — or with a count that only
 * coincidentally matches while a line was merged and another split — that
 * batch falls back to one request per line. Every failure is silent: the
 * result is null (or the affected lines stay null) and the original lyrics
 * render on their own.
 */
class LyricsTranslator(
    private val httpClientFactory: HttpClientFactory) {

    /**
     * Returns one entry per input line (blank lines map to ""), or null when
     * nothing could be translated at all.
     */
    suspend fun translate(lines: List<String>, targetLanguage: String): List<String?>? {
        val nonBlankIndices = lines.indices.filter { lines[it].isNotBlank() }
        if (nonBlankIndices.isEmpty()) return null

        val result = MutableList<String?>(lines.size) { null }
        lines.forEachIndexed { index, line -> if (line.isBlank()) result[index] = "" }

        var chunk = mutableListOf<Int>()
        var chunkLength = 0

        suspend fun flushChunk() {
            if (chunk.isEmpty()) return
            val texts = chunk.map { lines[it] }
            val translated = translateBatch(texts, targetLanguage)
            if (translated != null) {
                chunk.forEachIndexed { position, lineIndex ->
                    result[lineIndex] = translated.getOrNull(position)
                }
            }
            chunk = mutableListOf()
            chunkLength = 0
        }

        for (index in nonBlankIndices) {
            val lineLength = lines[index].length + 1
            if (chunk.isNotEmpty() && chunkLength + lineLength > MAX_CHUNK_CHARS) {
                flushChunk()
            }
            chunk.add(index)
            chunkLength += lineLength
        }
        flushChunk()

        return if (result.any { !it.isNullOrBlank() }) result else null
    }

    private suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String): List<String>? {
        val combined = requestTranslation(texts.joinToString("\n"), targetLanguage)
            ?: return null
        val parts = combined.split("\n")
        if (parts.size == texts.size &&
            parts.indices.all { index -> isPlausiblePair(texts[index], parts[index]) }
        ) {
            return parts
        }

        // Line counts drifted, or the count only coincidentally matches
        // while the pairing shifted (a merge here, a split there): redo
        // this batch line by line so alignment is guaranteed.
        val perLine = ArrayList<String>(texts.size)
        for (text in texts) {
            perLine.add(requestTranslation(text, targetLanguage) ?: return null)
        }
        return perLine
    }

    /**
     * Sanity check for one source/translated pair from a batch reply. A
     * merged line comes back far longer than its source while the line it
     * swallowed comes back blank or stub-short, so a blank part for a
     * non-blank source, or a wild length ratio either way, marks the
     * whole batch as untrustworthy.
     */
    private fun isPlausiblePair(source: String, translated: String): Boolean {
        if (translated.isBlank()) return false
        val sourceLength = source.trim().length
        val translatedLength = translated.trim().length
        if (translatedLength > sourceLength * MAX_LENGTH_RATIO + LENGTH_SLACK) {
            return false
        }
        if (sourceLength >= MIN_SOURCE_FOR_SHORT_CHECK &&
            translatedLength * MAX_LENGTH_RATIO + LENGTH_SLACK < sourceLength
        ) {
            return false
        }
        return true
    }

    /** One raw gtx request; returns the full translated text or null. */
    private suspend fun requestTranslation(
        text: String,
        targetLanguage: String): String? = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
        try {
            val raw = httpClientFactory.default.get {
                url {
                    takeFrom("https://translate.googleapis.com/translate_a/single")
                    parameters.append("client", "gtx")
                    parameters.append("sl", "auto")
                    parameters.append("tl", targetLanguage)
                    parameters.append("dt", "t")
                    parameters.append("q", text)
                }
            }.body<String>()

            val root = HttpJson.parseToJsonElement(raw).jsonArray
            val segments = root[0].jsonArray
            segments.joinToString("") { segment ->
                segment.jsonArray[0].jsonPrimitive.contentOrNull ?: ""
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val MAX_CHUNK_CHARS = 1200
        private const val REQUEST_TIMEOUT_MS = 10_000L

        /** Batch-pair sanity bounds; see [isPlausiblePair]. */
        private const val MAX_LENGTH_RATIO = 3
        private const val LENGTH_SLACK = 10
        private const val MIN_SOURCE_FOR_SHORT_CHECK = 20
    }
}
