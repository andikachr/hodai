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
 * Supplies a romaji reading for each lyric line of a Japanese song.
 *
 * Lirik+ Paket 1 shipped this slot plus the UI layer that renders it; Paket 2
 * adds the real conversion engine behind [createRomajiProvider]: Kuromoji on
 * Android/desktop (androidJvmMain), [UnavailableRomajiProvider] on iOS until
 * a native tokenizer is wired up there.
 */
interface RomajiProvider {

    /**
     * Returns one romaji string per input line (same size, same order), or
     * null when romaji cannot be produced for this song.
     */
    suspend fun romajiFor(lines: List<String>, language: String): List<String>?
}

/** Fallback for platforms without a romaji engine; the layer never appears. */
class UnavailableRomajiProvider : RomajiProvider {
    override suspend fun romajiFor(lines: List<String>, language: String): List<String>? = null
}

/**
 * Creates the [RomajiProvider] for the current platform. Bound as the
 * [RomajiProvider] singleton in the DI module.
 */
expect fun createRomajiProvider(): RomajiProvider
