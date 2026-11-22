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
 * Hangul → Latin reading aid (Revised Romanization), dependency-free.
 *
 * Each syllable block decomposes into initial / medial / final jamo and is
 * mapped through the standard RR tables. When a syllable ends in a
 * consonant and the NEXT syllable (immediately adjacent, no space) opens
 * with a silent ㅇ, the final consonant liaises into that syllable's onset
 * (연연 → yeonyeon, 읽어 → ilgeo); a final ㅇ stays behind as "ng" and a
 * final ㅎ drops, both as RR prescribes. Full nasal/liquid assimilation
 * between consonant clusters is deliberately out of scope: this is a
 * reading aid for lyrics, not a linguistic transcription.
 *
 * Non-Hangul characters (Latin words, digits, punctuation, spaces) pass
 * through unchanged.
 */
object HangulRomanizer {

    private val INITIALS = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
        "ss", "", "j", "jj", "ch", "k", "t", "p", "h")

    private val MEDIALS = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa",
        "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i")

    /** Coda form of each final jamo when nothing liaises out of it. */
    private val FINALS = arrayOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "l",
        "l", "l", "l", "l", "l", "l", "m", "p", "p", "t",
        "t", "ng", "t", "t", "k", "t", "p", "t")

    /**
     * Liaison split of each final jamo: (part that stays as coda, part
     * that moves into the next syllable's onset). Single consonants move
     * wholly; compound finals keep their first jamo as coda and move the
     * second; ㅇ stays ("ng", nothing moves); ㅎ drops entirely.
     */
    private val FINAL_SPLIT: Array<Pair<String, String>> = arrayOf(
        "" to "", "" to "g", "" to "kk", "k" to "s", "" to "n",
        "n" to "j", "n" to "", "" to "d", "" to "r", "l" to "g",
        "l" to "m", "l" to "b", "l" to "s", "l" to "t", "l" to "p",
        "l" to "", "" to "m", "" to "b", "p" to "s", "" to "s",
        "" to "ss", "ng" to "", "" to "j", "" to "ch", "" to "k",
        "" to "t", "" to "p", "" to "")

    private const val SILENT_INITIAL = 11 // ㅇ

    fun romanizeLines(lines: List<String>): List<String>? {
        if (lines.none { line -> line.any { it.code in 0xAC00..0xD7A3 } }) return null
        return lines.map { romanizeLine(it) }
    }

    private class Syllable(val initial: Int, val medial: Int, val final: Int)

    private fun romanizeLine(line: String): String {
        // Tokenize first: liaison and liquid clusters look one syllable
        // ahead and behind, which a single streaming pass gets wrong.
        val syllables = arrayOfNulls<Syllable>(line.length)
        for (i in line.indices) {
            val code = line[i].code
            if (code in 0xAC00..0xD7A3) {
                val s = code - 0xAC00
                syllables[i] = Syllable(s / 588, (s % 588) / 28, s % 28)
            }
        }
        val onsets = arrayOfNulls<String>(line.length)
        val codas = arrayOfNulls<String>(line.length)
        for (i in line.indices) {
            val syllable = syllables[i] ?: continue
            if (onsets[i] == null) onsets[i] = INITIALS[syllable.initial]
            codas[i] = FINALS[syllable.final]
            // Liaison: this syllable's final moves (partly) into the
            // next syllable when it is immediately adjacent and opens
            // with a silent ㅇ.
            val next = syllables.getOrNull(i + 1)
            if (syllable.final != 0 && next != null &&
                next.initial == SILENT_INITIAL
            ) {
                val (stay, move) = FINAL_SPLIT[syllable.final]
                codas[i] = stay
                onsets[i + 1] = move
            }
        }
        // Liquid clusters: ㄹ after a ㄹ or ㄴ coda reads as "l"
        // (불러 → bulleo, 신라 → silla, whose ㄴ coda also becomes "l").
        for (i in 1 until line.length) {
            val syllable = syllables[i] ?: continue
            val prev = syllables[i - 1] ?: continue
            if (syllable.initial == 5) { // ㄹ
                when (prev.final) {
                    8 -> onsets[i] = "l" // ㄹ coda
                    4 -> { // ㄴ coda
                        codas[i - 1] = "l"
                        onsets[i] = "l"
                    }
                }
            }
        }
        val out = StringBuilder()
        for (i in line.indices) {
            val syllable = syllables[i]
            if (syllable == null) {
                out.append(line[i])
            } else {
                out.append(onsets[i]).append(MEDIALS[syllable.medial]).append(codas[i])
            }
        }
        return out.toString()
    }
}
