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

package org.spotube.spotube.modules.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.AppTheme

/** Advances a phase value while [active]; frozen otherwise. */
@Composable
private fun rememberSpectrumPhase(active: Boolean): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                phase += (now - last) / 1_000_000_000f * 5.2f
                last = now
            }
        }
    }
    return phase
}

/**
 * The seek bar as a spectrum (owner-requested, locked mock): the plain
 * progress slider is replaced by a waveform of bars. Bars behind the
 * playhead glow gold-green and dance gently while playing; bars ahead stay
 * dim. Tap or drag anywhere on the strip to seek. The bar heights are a
 * decorative pattern seeded per track, not an FFT of the audio.
 */
@Composable
fun SpectrumSeekBar(
    progress: Float,
    isPlaying: Boolean,
    enabled: Boolean,
    seedKey: String,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 46.dp
) {
    val barCount = 56
    val heights = remember(seedKey) {
        val trackSeed = seedKey.hashCode().toDouble()
        List(barCount) { i ->
            val rand = (
                (kotlin.math.sin(i * 12.9898 + trackSeed) * 43758.5453) % 1.0 + 1.0
                ) % 1.0
            val shape = 0.55 + 0.45 * kotlin.math.sin(i * 0.5 + trackSeed % 3.0) *
                kotlin.math.sin(i * 0.19 + 1.1)
            (0.18 + 0.82 * (shape * 0.65 + rand * 0.35)).coerceIn(0.1, 1.0).toFloat()
        }
    }

    val phase = rememberSpectrumPhase(isPlaying)
    val playedBrush = Brush.verticalGradient(
        listOf(AppTheme.colors.primary, MillenniumTheme.colors.highlight)
    )
    val playedCount = (progress.coerceIn(0f, 1f) * barCount).toInt()
    val maxBar = (barHeight.value - 2f).coerceAtLeast(4f)

    Row(
        modifier = modifier
            .height(barHeight)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onSeekChange(
                        (down.position.x / size.width).coerceIn(0f, 1f)
                    )
                    down.consume()
                    var change = down
                    while (change.pressed) {
                        val event = awaitPointerEvent()
                        change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            onSeekChange(
                                (change.position.x / size.width)
                                    .coerceIn(0f, 1f)
                            )
                            change.consume()
                        }
                    }
                    onSeekFinished()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        heights.forEachIndexed { i, base ->
            val played = i < playedCount
            val dance = if (played && isPlaying) {
                0.88f + 0.12f * kotlin.math.sin(phase * 2.6 + i * 0.7).toFloat()
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((base * dance * maxBar).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .then(
                        if (played) {
                            Modifier.background(playedBrush)
                        } else {
                            Modifier.background(
                                MillenniumTheme.colors.muted.copy(alpha = 0.3f)
                            )
                        }
                    )
            )
        }
    }
}

/**
 * Mini spectrum dancing along the bottom of the currently playing track's
 * cover (owner-requested, locked mock). Draw inside a clipped cover box.
 */
@Composable
fun PlayingCoverOverlay(modifier: Modifier = Modifier) {
    val phase = rememberSpectrumPhase(active = true)
    val brush = Brush.verticalGradient(
        listOf(AppTheme.colors.primary, MillenniumTheme.colors.highlight)
    )
    val pattern = remember { listOf(6f, 11f, 8f, 13f, 9f, 12f, 7f, 10f) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            pattern.forEachIndexed { i, base ->
                val dance = 0.75f + 0.25f *
                    kotlin.math.sin(phase * 3.1 + i * 0.9).toFloat()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((base * dance).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(brush)
                )
            }
        }
    }
}
