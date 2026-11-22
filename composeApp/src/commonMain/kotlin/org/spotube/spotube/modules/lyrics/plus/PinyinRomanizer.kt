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

import org.jetbrains.compose.resources.ExperimentalResourceApi
import spotube.composeapp.generated.resources.Res

/**
 * Hanzi → pinyin reading aid for Mandarin lyrics.
 *
 * Readings come from a bundled per-character table built from the
 * Unicode Unihan database (kMandarin = the most common Mandarin reading
 * of each character, with tone marks). Polyphonic characters therefore
 * occasionally show their common reading instead of the contextual one —
 * acceptable for a reading aid, and the same trade dictionary-based
 * romanizers make. Pinyin syllables are space-separated per character,
 * the usual lyric-sheet style; Latin words, digits and punctuation pass
 * through unchanged.
 */
object PinyinRomanizer {

    private var table: Map<Char, String>? = null

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadTable(): Map<Char, String> {
        table?.let { return it }
        val parsed = try {
            val text = Res.readBytes("files/pinyin_mandarin.tsv").decodeToString()
            val map = HashMap<Char, String>(text.length / 8)
            for (line in text.lineSequence()) {
                val tab = line.indexOf('\t')
                if (tab == 1 && line.length > 2) {
                    map[line[0]] = line.substring(2)
                }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
        table = parsed
        return parsed
    }

    suspend fun romanizeLines(lines: List<String>): List<String>? {
        if (lines.none { line -> line.any { it.code in 0x3400..0x9FFF } }) return null
        val map = loadTable()
        if (map.isEmpty()) return null
        return lines.map { romanizeLine(it, map) }
    }

    private fun romanizeLine(line: String, map: Map<Char, String>): String {
        val out = StringBuilder()
        var lastWasPinyin = false
        for (ch in line) {
            val pinyin = map[ch]
            when {
                pinyin != null -> {
                    if (lastWasPinyin) out.append(' ')
                    out.append(pinyin)
                    lastWasPinyin = true
                }
                ch == ' ' || ch == '　' -> {
                    out.append(' ')
                    lastWasPinyin = false
                }
                ch in "，。！？、；：,.!?;:" -> {
                    out.append(ch)
                    lastWasPinyin = false
                }
                else -> {
                    if (lastWasPinyin && ch.isLetterOrDigit()) out.append(' ')
                    out.append(ch)
                    lastWasPinyin = false
                }
            }
        }
        return out.toString().trim()
    }
}
