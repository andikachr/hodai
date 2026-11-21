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
 * Deterministic, dependency-free guess of a song's language from its lyric text.
 *
 * Returns an ISO 639-1 code ("ja", "ko", "zh", "en", "id") or "other" when the
 * text does not give enough signal. Script detection runs first (any real amount
 * of kana means Japanese, since Chinese lyrics never use kana); Latin-script
 * songs are split into English vs Indonesian by stopword voting, which is
 * deliberately crude — a wrong guess only costs a redundant or missing
 * translation layer, never wrong lyrics.
 */
object LyricsLanguageDetector {

    fun detect(lines: List<String>): String = detect(lines.joinToString("\n"))

    fun detect(text: String): String {
        var kana = 0
        var hangul = 0
        var han = 0
        var latin = 0
        var cyrillic = 0

        for (ch in text) {
            when (ch.code) {
                in 0x3040..0x309F,   // Hiragana
                in 0x30A0..0x30FF,   // Katakana
                in 0xFF66..0xFF9D -> kana++   // Halfwidth katakana
                in 0xAC00..0xD7A3,   // Hangul syllables
                in 0x1100..0x11FF,   // Hangul jamo
                in 0x3130..0x318F -> hangul++
                in 0x4E00..0x9FFF,   // Han (CJK unified)
                in 0x3400..0x4DBF -> han++
                in 0x0400..0x04FF -> cyrillic++
                else -> if (ch in 'a'..'z' || ch in 'A'..'Z' || ch.code in 0x00C0..0x024F) latin++
            }
        }

        val scriptTotal = kana + hangul + han + latin + cyrillic
        if (scriptTotal == 0) return "other"

        // Kana is the discriminator between Japanese and Chinese: a Japanese
        // song mixes kanji with kana, a Chinese song has no kana at all.
        if (kana >= 2) return "ja"
        if (hangul * 5 >= scriptTotal) return "ko"
        if (han * 5 >= scriptTotal) return "zh"
        if (latin * 5 < scriptTotal) return "other"

        return detectLatin(text)
    }

    private fun detectLatin(text: String): String {
        val words = ArrayList<String>()
        val current = StringBuilder()
        for (ch in text.lowercase()) {
            if (ch.isLetter()) {
                current.append(ch)
            } else if (current.isNotEmpty()) {
                words.add(current.toString())
                current.clear()
            }
        }
        if (current.isNotEmpty()) words.add(current.toString())
        if (words.isEmpty()) return "other"

        val idScore = words.count { it in INDONESIAN_WORDS }
        val enScore = words.count { it in ENGLISH_WORDS }
        return when {
            idScore > enScore && idScore >= 2 -> "id"
            enScore > idScore && enScore >= 2 -> "en"
            idScore >= 1 && enScore == 0 -> "id"
            enScore >= 1 && idScore == 0 -> "en"
            else -> "other"
        }
    }

    private val INDONESIAN_WORDS = setOf(
        "yang", "dan", "di", "ke", "dari", "ini", "itu", "aku", "kamu", "kau",
        "dia", "tidak", "tak", "untuk", "dengan", "pada", "adalah", "akan",
        "sudah", "belum", "hanya", "juga", "karena", "saat", "lagi", "punya",
        "bisa", "ada", "dalam", "oleh", "kita", "kami", "mereka", "jangan",
        "telah", "bagi", "tentang", "seperti", "antara", "tanpa", "hingga",
        "sampai", "masih", "harus", "ingin", "cinta", "hati", "ku", "mu", "nya")

    private val ENGLISH_WORDS = setOf(
        "the", "and", "you", "i", "to", "of", "a", "in", "that", "it", "is",
        "was", "for", "on", "are", "with", "as", "his", "they", "be", "at",
        "one", "have", "this", "from", "or", "had", "by", "but", "not", "what",
        "all", "were", "we", "when", "your", "can", "said", "there", "an",
        "each", "which", "she", "do", "how", "their", "will", "other", "about",
        "out", "many", "then", "them", "these", "so", "some", "her", "would",
        "make", "like", "him", "into", "time", "has", "look", "two", "more",
        "go", "see", "no", "way", "could", "my", "than", "first", "been",
        "who", "its", "now", "love", "yeah", "oh", "baby")
}
