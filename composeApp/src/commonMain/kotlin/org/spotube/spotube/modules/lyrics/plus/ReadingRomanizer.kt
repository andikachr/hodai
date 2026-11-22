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

/**
 * The "cara baca" (reading aid) layer: one Latin reading line under each
 * lyric line for the three scripts Hodai supports.
 *
 * - Japanese → the platform [RomajiProvider] (Kuromoji romaji).
 * - Korean → [HangulRomanizer] (Revised Romanization, pure Kotlin).
 * - Mandarin → [PinyinRomanizer] (bundled Unihan pinyin table).
 *
 * Any other language returns null and the layer simply never appears.
 */
object ReadingRomanizer {

    val SUPPORTED_LANGUAGES = setOf("ja", "ko", "zh")

    suspend fun romanize(
        lines: List<String>,
        language: String,
        japaneseProvider: RomajiProvider): List<String>? = when (language) {
        "ja" -> japaneseProvider.romajiFor(lines, language)
        "ko" -> HangulRomanizer.romanizeLines(lines)
        "zh" -> PinyinRomanizer.romanizeLines(lines)
        else -> null
    }
}
