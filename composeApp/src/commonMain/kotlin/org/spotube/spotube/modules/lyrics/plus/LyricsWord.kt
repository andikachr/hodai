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

import kotlinx.serialization.Serializable

/**
 * One sung word (or syllable chunk) of a karaoke line, with absolute
 * timings in milliseconds from the start of the track. Concatenating the
 * [text] of every word in a [LyricsWordLine] reproduces the line's text.
 */
@Serializable
data class LyricsWord(
    val startMs: Long,
    val endMs: Long,
    val text: String)

/**
 * The word segments of one synced line. Word data is OPTIONAL everywhere:
 * a line without an entry here behaves exactly like a plain line-timed
 * line (whole-line highlight), so word-timed and line-timed sources can
 * be mixed in one result.
 */
@Serializable
data class LyricsWordLine(
    val words: List<LyricsWord>)
