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

import okio.ByteString.Companion.decodeBase64
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine

/** KRC lyrics decoded into line-timed lines plus their word segments. */
data class KrcLyrics(
    val lines: List<LyricsLine>,
    /** Aligned with [lines]; null entries are lines without word data. */
    val wordLines: List<LyricsWordLine?>)

/**
 * KuGou's KRC format: the karaoke (word-timed) counterpart of LRC.
 *
 * The download endpoint serves the payload base64-wrapped; underneath,
 * the bytes are XOR-masked with a fixed 16-byte key, start with the
 * `krc1` magic and continue as a zlib stream. The inflated text looks
 * like LRC whose lines carry per-word tags:
 *
 *     [12340,2980]<0,460,0>foo<460,520,0>bar …
 *
 * The line tag is `[startMs,durationMs]`; each word tag is
 * `<offsetMs,durationMs,0>` with the offset relative to the line start.
 * Metadata tags (`[ti:…]`, `[language:…]`) never match the numeric line
 * pattern and drop out, like in [LrcParser].
 */
internal object KrcParser {

    /** Decodes the endpoint's base64 `content` field into KRC text. */
    fun decodeContent(base64: String): String? {
        return try {
            val masked = base64.decodeBase64()?.toByteArray() ?: return null
            if (masked.size <= 4) return null
            // The "krc1" header travels in the clear; only the bytes
            // after it are XOR-masked (verified against a live payload).
            val magic = masked.copyOfRange(0, 4).decodeToString()
            if (magic != "krc1") return null
            val plain = ByteArray(masked.size - 4) { index ->
                (masked[index + 4].toInt() xor XOR_KEY[index % XOR_KEY.size].toInt()).toByte()
            }
            ZlibInflate.inflateZlib(plain).decodeToString()
        } catch (e: Exception) {
            null
        }
    }

    /** Parses decoded KRC text; lines without any word tag stay wordless. */
    fun parse(krc: String): KrcLyrics {
        val lines = ArrayList<LyricsLine>()
        val wordLines = ArrayList<LyricsWordLine?>()
        for (rawLine in krc.lines()) {
            val lineMatch = LINE_TAG.find(rawLine) ?: continue
            val lineStart = lineMatch.groupValues[1].toLongOrNull() ?: continue
            val body = rawLine.substring(lineMatch.range.last + 1)
            val words = ArrayList<LyricsWord>()
            val text = StringBuilder()
            for (wordMatch in WORD_TAG.findAll(body)) {
                // Any text before this tag is not sung word material
                // (should not happen in well-formed KRC); keep it out.
                val offset = wordMatch.groupValues[1].toLongOrNull() ?: continue
                val duration = wordMatch.groupValues[2].toLongOrNull() ?: continue
                val wordStart = wordMatch.range.last + 1
                val wordEnd = body.indexOf('<', wordStart)
                    .takeIf { it >= 0 } ?: body.length
                val wordText = body.substring(wordStart, wordEnd)
                if (wordText.isEmpty()) continue
                words.add(
                    LyricsWord(
                        startMs = lineStart + offset,
                        endMs = lineStart + offset + duration,
                        text = wordText))
                text.append(wordText)
            }
            if (words.isEmpty()) {
                // A timed line without word tags (some files mix plain
                // lines in): keep the text, mark the line wordless.
                val plainText = body.trim()
                if (plainText.isEmpty()) continue
                lines.add(LyricsLine(time = lineStart, text = plainText))
                wordLines.add(null)
            } else {
                lines.add(LyricsLine(time = lineStart, text = text.toString()))
                wordLines.add(LyricsWordLine(words))
            }
        }
        if (lines.isEmpty()) return KrcLyrics(emptyList(), emptyList())
        val order = lines.indices.sortedBy { lines[it].time }
        return KrcLyrics(
            lines = order.map { lines[it] },
            wordLines = order.map { wordLines[it] })
    }

    private val LINE_TAG = Regex("""^\[(\d+),(\d+)\]""")
    private val WORD_TAG = Regex("""<(\d+),(\d+),\d+>""")

    /** The fixed XOR mask KuGou applies to KRC payloads. */
    private val XOR_KEY = byteArrayOf(
        0x40, 0x47, 0x61, 0x77, 0x5e, 0x32, 0x74, 0x47,
        0x51, 0x36, 0x31, 0x2d, 0xce.toByte(), 0xd2.toByte(), 0x6e, 0x69)
}
