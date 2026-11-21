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

/**
 * Lenient LRC parser shared by the fallback lyrics sources.
 *
 * More forgiving than the LRCLib plugin's parser: accepts missing fractional
 * seconds, 1–3 digit fractions, trailing junk inside the time tag (NetEase
 * writes tags like `[00:00.00-1]` on credit lines) and lines carrying several
 * time tags. Metadata tags (`[ti:…]`, `[ar:…]`) never match, so they drop out.
 */
internal object LrcParser {

    private val timeTag = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?[^\]]*\]""")

    fun parse(lrc: String): List<LyricsLine> {
        val out = ArrayList<LyricsLine>()
        for (rawLine in lrc.lines()) {
            val matches = timeTag.findAll(rawLine).toList()
            if (matches.isEmpty()) continue
            val text = rawLine.substring(matches.last().range.last + 1).trim()
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                val fractionMs = if (fraction.isEmpty()) 0L
                    else fraction.padEnd(3, '0').toLongOrNull() ?: 0L
                out.add(
                    LyricsLine(
                        time = minutes * 60_000 + seconds * 1_000 + fractionMs,
                        text = text))
            }
        }
        return out.sortedBy { it.time }
    }
}
