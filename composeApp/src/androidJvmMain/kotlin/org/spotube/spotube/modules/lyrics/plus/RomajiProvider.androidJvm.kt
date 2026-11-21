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

import co.touchlab.kermit.Logger
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createRomajiProvider(): RomajiProvider = KuromojiRomajiProvider()

/**
 * Romaji engine backed by Kuromoji (IPADIC). Each lyric line is tokenized,
 * every token's dictionary reading (katakana) is converted with
 * [KatakanaRomaji], and the pieces are re-assembled with spaces between
 * tokens (punctuation/symbol tokens stay attached).
 *
 * Dictionary readings already encode the special particle pronunciations
 * (は → ワ "wa", へ → エ "e", を → ヲ "wo"), so particles need no special
 * casing here. Tokens without a reading (latin words, digits, symbols, rare
 * unknown words) pass through as their surface form; an unknown word that
 * contains kana still gets its kana converted.
 */
class KuromojiRomajiProvider : RomajiProvider {

    private val logger = Logger.withTag("KuromojiRomajiProvider")

    /** Building a tokenizer loads the whole IPADIC dictionary; do it once. */
    private val tokenizer by lazy { Tokenizer() }

    override suspend fun romajiFor(lines: List<String>, language: String): List<String>? {
        if (language != "ja") return null
        if (lines.none { line -> line.any { it.isJapaneseScript() } }) return null
        return try {
            withContext(Dispatchers.Default) {
                lines.map { line -> convertLine(line) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w { "Romaji conversion failed: ${e.message}" }
            null
        }
    }

    private fun convertLine(line: String): String {
        if (line.isBlank()) return line

        val tokens = tokenizer.tokenize(line).filter { it.surface.isNotBlank() }
        if (tokens.isEmpty()) return line

        val pieces = mutableListOf<Piece>()
        val symbolTokens = mutableSetOf<Int>()
        tokens.forEachIndexed { tokenIndex, token ->
            if (token.partOfSpeechLevel1 == SYMBOL_POS) symbolTokens += tokenIndex
            val reading = token.reading?.takeIf { it.isNotBlank() && it != UNKNOWN_READING }
            val source = reading
                ?: token.surface.takeIf { surface -> surface.any { it.isKana() } }
            if (source == null) {
                pieces += Piece(tokenIndex, token.surface, Piece.Kind.RAW)
            } else {
                KatakanaRomaji.parse(source).forEach { unit ->
                    pieces += when (unit) {
                        is RomajiUnit.Mora ->
                            Piece(tokenIndex, unit.text, Piece.Kind.MORA, nasal = unit.nasal)
                        is RomajiUnit.Raw ->
                            Piece(tokenIndex, unit.text, Piece.Kind.RAW)
                        RomajiUnit.Geminate ->
                            Piece(tokenIndex, "", Piece.Kind.GEMINATE)
                        RomajiUnit.LongVowel ->
                            Piece(tokenIndex, "", Piece.Kind.LONG)
                    }
                }
            }
        }

        resolveMarkers(pieces)
        return assemble(tokens.indices.toList(), pieces, symbolTokens)
    }

    /**
     * Resolves the marker pieces against the flat piece list of the whole
     * line, so a ッ or ー at a token edge still finds its mora.
     */
    private fun resolveMarkers(pieces: MutableList<Piece>) {
        // Sokuon: double the initial consonant of the next mora ("t" before
        // "ch"); a trailing sokuon with no following mora becomes "t".
        var i = 0
        while (i < pieces.size) {
            val piece = pieces[i]
            if (piece.kind == Piece.Kind.GEMINATE) {
                val next = (i + 1 until pieces.size)
                    .firstOrNull { pieces[it].kind == Piece.Kind.MORA }
                if (next != null) {
                    val mora = pieces[next]
                    mora.text = if (mora.text.startsWith("ch")) {
                        "t" + mora.text
                    } else {
                        mora.text.first() + mora.text
                    }
                    pieces.removeAt(i)
                    continue
                }
                piece.kind = Piece.Kind.MORA
                piece.text = "t"
            }
            i++
        }
        // Chōonpu: repeat the last vowel of the previous mora.
        i = 0
        while (i < pieces.size) {
            if (pieces[i].kind == Piece.Kind.LONG) {
                val prev = (i - 1 downTo 0)
                    .firstOrNull { pieces[it].kind == Piece.Kind.MORA }
                if (prev != null) {
                    val mora = pieces[prev]
                    mora.text.lastOrNull { it in "aiueo" }?.let { vowel ->
                        mora.text += vowel
                    }
                }
                pieces.removeAt(i)
                continue
            }
            i++
        }
        // ン: "m" when the next mora starts with b, p or m.
        pieces.forEachIndexed { index, piece ->
            if (piece.kind == Piece.Kind.MORA && piece.nasal) {
                val next = (index + 1 until pieces.size)
                    .firstOrNull { pieces[it].kind == Piece.Kind.MORA }
                val nextInitial = next?.let { pieces[it].text.firstOrNull() }
                if (nextInitial == 'b' || nextInitial == 'p' || nextInitial == 'm') {
                    piece.text = "m"
                }
            }
        }
    }

    /** Joins per-token text with spaces, except around symbol tokens. */
    private fun assemble(
        tokenIndices: List<Int>,
        pieces: List<Piece>,
        symbolTokens: Set<Int>): String {
        val byToken = pieces.groupBy { it.tokenIndex }
        val result = StringBuilder()
        var previousWasSymbol = true // start of line: no leading space
        for (tokenIndex in tokenIndices) {
            val text = byToken[tokenIndex]?.joinToString("") { it.text }.orEmpty()
            if (text.isEmpty()) continue
            val isSymbol = tokenIndex in symbolTokens
            if (!previousWasSymbol && !isSymbol) result.append(' ')
            result.append(text)
            previousWasSymbol = isSymbol
        }
        return result.toString().trim()
    }

    private class Piece(
        val tokenIndex: Int,
        var text: String,
        var kind: Kind,
        val nasal: Boolean = false) {

        enum class Kind { MORA, RAW, GEMINATE, LONG }
    }

    private companion object {
        const val SYMBOL_POS = "記号"
        const val UNKNOWN_READING = "*"
    }
}

private fun Char.isKana(): Boolean = this in '\u3040'..'\u30FF'

private fun Char.isJapaneseScript(): Boolean =
    isKana() || this in '\u3400'..'\u4DBF' || this in '\u4E00'..'\u9FFF'
