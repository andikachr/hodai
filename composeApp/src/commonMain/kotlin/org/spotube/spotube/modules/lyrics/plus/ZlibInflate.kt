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
 * Minimal zlib (RFC 1950) + DEFLATE (RFC 1951) decoder in pure Kotlin.
 *
 * commonMain has no compression library and the one payload that needs
 * inflating — KuGou's KRC lyrics — is a few dozen kilobytes, so a small
 * self-contained decoder beats a new expect/actual per platform. Any
 * malformed input raises [IllegalArgumentException]; callers treat that
 * as "no lyrics here".
 */
internal object ZlibInflate {

    /** Inflates a zlib stream (2-byte header + DEFLATE body + Adler-32). */
    fun inflateZlib(data: ByteArray): ByteArray {
        require(data.size > 6) { "zlib stream too short" }
        val cmf = data[0].toInt() and 0xFF
        require(cmf and 0x0F == 8) { "not a deflate zlib stream" }
        require((cmf shr 4) <= 7) { "zlib window too large" }
        val reader = BitReader(data, startByte = 2)
        return inflateDeflate(reader)
    }

    /** Inflates a raw DEFLATE stream starting at [startByte]. */
    fun inflateDeflate(reader: BitReader): ByteArray {
        val out = ByteSink()
        while (true) {
            val isFinal = reader.readBits(1) == 1
            when (val type = reader.readBits(2)) {
                0 -> {
                    reader.alignToByte()
                    val len = reader.readStoredShort()
                    val nlen = reader.readStoredShort()
                    require(len == (nlen xor 0xFFFF)) { "stored block length check failed" }
                    repeat(len) { out.write(reader.readStoredByte()) }
                }
                1 -> {
                    val (literal, distance) = fixedTables()
                    inflateBlock(reader, out, literal, distance)
                }
                2 -> {
                    val (literal, distance) = dynamicTables(reader)
                    inflateBlock(reader, out, literal, distance)
                }
                else -> throw IllegalArgumentException("reserved deflate block type $type")
            }
            if (isFinal) break
        }
        return out.toByteArray()
    }

    private fun inflateBlock(
        reader: BitReader,
        out: ByteSink,
        literalTable: Huffman,
        distanceTable: Huffman) {
        while (true) {
            val symbol = literalTable.decode(reader)
            when {
                symbol < 256 -> out.write(symbol)
                symbol == 256 -> return
                symbol in 257..285 -> {
                    val lengthIndex = symbol - 257
                    val length = LENGTH_BASE[lengthIndex] +
                        reader.readBits(LENGTH_EXTRA[lengthIndex])
                    val distanceSymbol = distanceTable.decode(reader)
                    require(distanceSymbol in 0..29) { "bad distance symbol $distanceSymbol" }
                    val distance = DISTANCE_BASE[distanceSymbol] +
                        reader.readBits(DISTANCE_EXTRA[distanceSymbol])
                    out.copyFromHistory(distance, length)
                }
                else -> throw IllegalArgumentException("bad literal/length symbol $symbol")
            }
        }
    }

    private fun fixedTables(): Pair<Huffman, Huffman> {
        val literalLengths = IntArray(288) { index ->
            when {
                index < 144 -> 8
                index < 256 -> 9
                index < 280 -> 7
                else -> 8
            }
        }
        val distanceLengths = IntArray(32) { 5 }
        return Huffman(literalLengths) to Huffman(distanceLengths)
    }

    private fun dynamicTables(reader: BitReader): Pair<Huffman, Huffman> {
        val literalCount = reader.readBits(5) + 257
        val distanceCount = reader.readBits(5) + 1
        val codeLengthCount = reader.readBits(4) + 4
        val codeLengths = IntArray(19)
        for (i in 0 until codeLengthCount) {
            codeLengths[CODE_LENGTH_ORDER[i]] = reader.readBits(3)
        }
        val codeLengthTable = Huffman(codeLengths)
        val lengths = IntArray(literalCount + distanceCount)
        var index = 0
        while (index < lengths.size) {
            when (val symbol = codeLengthTable.decode(reader)) {
                in 0..15 -> lengths[index++] = symbol
                16 -> {
                    require(index > 0) { "repeat with no previous length" }
                    val previous = lengths[index - 1]
                    repeat(3 + reader.readBits(2)) {
                        require(index < lengths.size) { "length repeat overflow" }
                        lengths[index++] = previous
                    }
                }
                17 -> repeat(3 + reader.readBits(3)) {
                    require(index < lengths.size) { "zero repeat overflow" }
                    lengths[index++] = 0
                }
                18 -> repeat(11 + reader.readBits(7)) {
                    require(index < lengths.size) { "zero repeat overflow" }
                    lengths[index++] = 0
                }
                else -> throw IllegalArgumentException("bad code length symbol $symbol")
            }
        }
        require(lengths[256] != 0) { "missing end-of-block code" }
        val literal = Huffman(lengths.copyOfRange(0, literalCount))
        val distance = Huffman(lengths.copyOfRange(literalCount, lengths.size))
        return literal to distance
    }

    /** Canonical Huffman decoder over a (length, code) lookup map. */
    private class Huffman(codeLengths: IntArray) {
        private val maxLength: Int = codeLengths.maxOrNull() ?: 0
        private val symbolsByCode: Map<Long, Int>

        init {
            // An all-zero length set is legal for an unused distance
            // table; it only fails if a symbol is actually decoded.
            require(maxLength in 0..15) { "bad huffman code lengths" }
            val counts = IntArray(maxLength + 1)
            for (length in codeLengths) {
                require(length in 0..15) { "code length out of range" }
                if (length > 0) counts[length]++
            }
            val nextCode = IntArray(maxLength + 1)
            var code = 0
            for (bits in 1..maxLength) {
                code = (code + counts[bits - 1]) shl 1
                nextCode[bits] = code
            }
            val map = HashMap<Long, Int>()
            for ((symbol, length) in codeLengths.withIndex()) {
                if (length == 0) continue
                val assigned = nextCode[length]++
                map[(length.toLong() shl 32) or (assigned.toLong() and 0xFFFFFFFFL)] = symbol
            }
            symbolsByCode = map
        }

        fun decode(reader: BitReader): Int {
            if (symbolsByCode.isEmpty()) {
                throw IllegalArgumentException("decode from an empty huffman table")
            }
            var code = 0
            for (length in 1..maxLength) {
                code = (code shl 1) or reader.readBits(1)
                symbolsByCode[(length.toLong() shl 32) or (code.toLong() and 0xFFFFFFFFL)]
                    ?.let { return it }
            }
            throw IllegalArgumentException("invalid huffman code")
        }
    }

    /** LSB-first bit reader over the deflate body. */
    class BitReader(private val data: ByteArray, startByte: Int) {
        private var byteIndex = startByte
        private var bitIndex = 0

        fun readBits(count: Int): Int {
            var value = 0
            for (i in 0 until count) {
                require(byteIndex < data.size) { "deflate stream truncated" }
                val bit = (data[byteIndex].toInt() shr bitIndex) and 1
                value = value or (bit shl i)
                bitIndex++
                if (bitIndex == 8) {
                    bitIndex = 0
                    byteIndex++
                }
            }
            return value
        }

        fun alignToByte() {
            if (bitIndex != 0) {
                bitIndex = 0
                byteIndex++
            }
        }

        /** Stored blocks are byte-aligned: a raw byte read. */
        fun readStoredByte(): Int {
            require(byteIndex < data.size) { "deflate stream truncated" }
            return data[byteIndex++].toInt() and 0xFF
        }

        /** Stored blocks carry little-endian 16-bit values. */
        fun readStoredShort(): Int =
            readStoredByte() or (readStoredByte() shl 8)
    }

    /** Growable byte output with LZ77 history access. */
    private class ByteSink {
        private var buffer = ByteArray(8192)
        private var size = 0

        fun write(byte: Int) {
            ensure(size + 1)
            buffer[size++] = byte.toByte()
        }

        fun copyFromHistory(distance: Int, length: Int) {
            require(distance in 1..size) { "distance too far back" }
            ensure(size + length)
            var from = size - distance
            repeat(length) {
                buffer[size++] = buffer[from++]
            }
        }

        private fun ensure(capacity: Int) {
            if (capacity <= buffer.size) return
            var grown = buffer.size * 2
            while (grown < capacity) grown *= 2
            buffer = buffer.copyOf(grown)
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }

    private val CODE_LENGTH_ORDER =
        intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    private val LENGTH_BASE = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
    private val LENGTH_EXTRA = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)

    private val DISTANCE_BASE = intArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
    private val DISTANCE_EXTRA = intArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
}
