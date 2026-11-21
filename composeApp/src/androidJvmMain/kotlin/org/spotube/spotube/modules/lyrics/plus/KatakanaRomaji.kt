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
 * One piece of a katakana string after parsing, before line-level assembly.
 * The marker units ([Geminate], [LongVowel]) are resolved against their
 * neighbouring [Mora]s by the caller, because their effect can reach across
 * token boundaries.
 */
internal sealed interface RomajiUnit {

    /** A finished romaji syllable. [nasal] marks the mora from ン. */
    data class Mora(val text: String, val nasal: Boolean = false) : RomajiUnit

    /** ッ (sokuon): doubles the initial consonant of the next mora. */
    data object Geminate : RomajiUnit

    /** ー (chōonpu): lengthens the vowel of the previous mora. */
    data object LongVowel : RomajiUnit

    /** Characters that are not kana (kanji, latin, digits) pass through. */
    data class Raw(val text: String) : RomajiUnit
}

/**
 * Katakana → romaji (modified Hepburn) parser used by the Kuromoji romaji
 * engine. Conventions chosen for lyric readability:
 *
 * - Long vowels double the vowel letter (ロー → "roo"), no macrons, so the
 *   output stays plain ASCII.
 * - ン is "n", but "m" before a mora starting with b, p or m.
 * - ッ doubles the next mora's initial consonant; before "ch" it becomes
 *   "t" instead (ッチ → "tchi"), per Hepburn.
 *
 * The parser works on any string: kana become [RomajiUnit.Mora]s, anything
 * else accumulates into [RomajiUnit.Raw] runs. Hiragana input is shifted to
 * katakana first, so surface forms can be fed in directly when a dictionary
 * reading is missing.
 */
internal object KatakanaRomaji {

    fun parse(text: String): List<RomajiUnit> {
        val units = mutableListOf<RomajiUnit>()
        val raw = StringBuilder()

        fun flushRaw() {
            if (raw.isNotEmpty()) {
                units += RomajiUnit.Raw(raw.toString())
                raw.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i].toKatakana()
            when {
                c == 'ッ' -> {
                    flushRaw()
                    units += RomajiUnit.Geminate
                    i++
                }
                c == 'ー' -> {
                    flushRaw()
                    units += RomajiUnit.LongVowel
                    i++
                }
                i + 1 < text.length && COMBOS.containsKey("$c${text[i + 1].toKatakana()}") -> {
                    flushRaw()
                    units += RomajiUnit.Mora(COMBOS.getValue("$c${text[i + 1].toKatakana()}"))
                    i += 2
                }
                BASE.containsKey(c) -> {
                    flushRaw()
                    units += RomajiUnit.Mora(BASE.getValue(c), nasal = c == 'ン')
                    i++
                }
                else -> {
                    raw.append(text[i])
                    i++
                }
            }
        }
        flushRaw()
        return units
    }

    /** Hiragana (ぁ..ゖ) sits exactly 0x60 below its katakana counterpart. */
    private fun Char.toKatakana(): Char =
        if (this in 'ぁ'..'ゖ') (code + 0x60).toChar() else this

    private val BASE: Map<Char, String> = mapOf(
        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヰ' to "wi", 'ヱ' to "we", 'ヲ' to "wo", 'ン' to "n",
        'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu", 'ゲ' to "ge", 'ゴ' to "go",
        'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo",
        'ダ' to "da", 'ヂ' to "ji", 'ヅ' to "zu", 'デ' to "de", 'ド' to "do",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo",
        'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po",
        'ヴ' to "vu",
        // Small kana standing alone.
        'ァ' to "a", 'ィ' to "i", 'ゥ' to "u", 'ェ' to "e", 'ォ' to "o",
        'ャ' to "ya", 'ュ' to "yu", 'ョ' to "yo", 'ヮ' to "wa",
        'ヵ' to "ka", 'ヶ' to "ka")

    private val COMBOS: Map<String, String> = mapOf(
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        // Loanword combinations.
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo",
        "フュ" to "fyu",
        "ティ" to "ti", "トゥ" to "tu", "テュ" to "tyu",
        "ディ" to "di", "ドゥ" to "du", "デュ" to "dyu",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo",
        "ヴァ" to "va", "ヴィ" to "vi", "ヴェ" to "ve", "ヴォ" to "vo",
        "シェ" to "she", "ジェ" to "je", "チェ" to "che",
        "ツァ" to "tsa", "ツィ" to "tsi", "ツェ" to "tse", "ツォ" to "tso",
        "クァ" to "kwa", "クィ" to "kwi", "クェ" to "kwe", "クォ" to "kwo",
        "グァ" to "gwa", "スィ" to "si", "ズィ" to "zi",
        "イェ" to "ye")
}
