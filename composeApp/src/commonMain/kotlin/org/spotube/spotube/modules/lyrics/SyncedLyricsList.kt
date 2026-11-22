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

package org.spotube.spotube.modules.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.millennium.theme.MillenniumTheme

private const val LYRICS_BLANK_LINE = "\u266A"

/**
 * Time-synced lyrics list that keeps the active line vertically centered.
 *
 * With [onLineClick] it renders Millennium list tiles (the active one is the selected tile);
 * without it, it renders a passive compact preview.
 */
@Composable
fun SyncedLyricsList(
    lines: List<LyricsLine>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    onLineClick: ((LyricsLine) -> Unit)? = null,
    romajiLines: List<String?>? = null,
    translationLines: List<String?>? = null
) {
    val listState = rememberLazyListState()
    val colors = MillenniumTheme.colors

    BoxWithConstraints(modifier = modifier) {
        // Half-viewport padding lets the first/last lines reach the center too.
        val edgePadding = maxHeight / 2

        LaunchedEffect(currentIndex, lines.size) {
            if (currentIndex !in lines.indices) return@LaunchedEffect
            val visible = listState.layoutInfo.visibleItemsInfo
            val itemHeight = visible.firstOrNull { it.index == currentIndex }?.size
                ?: visible.firstOrNull()?.size
                ?: 0
            listState.animateScrollToItem(index = currentIndex, scrollOffset = itemHeight / 2)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().then(
                if (onLineClick != null) Modifier.fadingEdges(maxHeight * 0.18f) else Modifier
            ),
            contentPadding = PaddingValues(vertical = edgePadding),
            verticalArrangement = Arrangement.spacedBy(if (onLineClick != null) 4.dp else 2.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> "$index-${line.time}" }) { index, line ->
                val isCurrent = index == currentIndex
                val text = line.text.ifBlank { LYRICS_BLANK_LINE }
                val color by animateColorAsState(
                    when {
                        isCurrent -> colors.accent
                        index < currentIndex -> colors.muted.copy(alpha = 0.55f)
                        else -> colors.muted
                    }
                )
                // Lirik+ layers never attach to blank (instrumental) lines.
                val romaji = if (line.text.isBlank()) null
                    else romajiLines?.getOrNull(index)?.takeIf { it.isNotBlank() }
                val translation = if (line.text.isBlank()) null
                    else translationLines?.getOrNull(index)?.takeIf { it.isNotBlank() }
                if (onLineClick != null) {
                    ImmersiveLyricLine(
                        text = text,
                        romaji = romaji,
                        translation = translation,
                        distance = kotlin.math.abs(index - currentIndex),
                        isCurrent = isCurrent,
                        onClick = { onLineClick(line) }
                    )
                } else {
                    // Passive preview (expanded player): the same focus
                    // language as the stage — left-aligned, lines fall off
                    // with distance, layers only near the active line.
                    val distance = kotlin.math.abs(index - currentIndex)
                    val passiveAlpha = when (distance) {
                        0 -> 1f
                        1 -> 0.65f
                        2 -> 0.45f
                        else -> 0.3f
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { this.alpha = passiveAlpha }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = text,
                            style = (
                                if (isCurrent) AppTheme.typography.bodyLarge
                                else AppTheme.typography.bodyMedium
                                ).copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = color
                            ),
                            textAlign = TextAlign.Start,
                            maxLines = 2
                        )
                        if (distance <= 2 && romaji != null) {
                            Text(
                                text = romaji,
                                style = AppTheme.typography.bodySmall.copy(
                                    color = colors.accent.copy(alpha = 0.8f)
                                ),
                                textAlign = TextAlign.Start,
                                maxLines = 2
                            )
                        }
                        if (distance <= 2 && translation != null) {
                            Text(
                                text = translation,
                                style = AppTheme.typography.bodySmall.copy(color = color),
                                textAlign = TextAlign.Start,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One line on the premium lyrics stage (owner-locked mock): left-aligned,
 * the active line stands large with a gold focus bar, its romaji in gold and
 * its translation just under; lines fall off in opacity with distance, the
 * far ones blur, and the romaji/translation layers only stay sharp near the
 * active line.
 */
@Composable
private fun ImmersiveLyricLine(
    text: String,
    romaji: String?,
    translation: String?,
    distance: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val colors = MillenniumTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val targetAlpha = when (distance) {
        0 -> 1f
        1 -> 0.62f
        2 -> 0.45f
        else -> 0.3f
    }
    val alpha by animateFloatAsState(targetAlpha, tween(300))
    val hoverBackground by animateColorAsState(
        if (hovered && !isCurrent) colors.accent.copy(alpha = 0.10f) else Color.Transparent
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .blur(if (distance >= 3) 1.5.dp else 0.dp)
            .graphicsLayer {
                this.alpha = if (hovered) maxOf(alpha, 0.85f) else alpha
            }
            .clip(RoundedCornerShape(14.dp))
            .background(hoverBackground)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(if (isCurrent) colors.accent else Color.Transparent)
        )
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = text,
                style = (
                    if (isCurrent) AppTheme.typography.headlineMedium
                    else AppTheme.typography.headlineSmall
                    ).copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                ),
                textAlign = TextAlign.Start
            )
            if (distance <= 2 && romaji != null) {
                Text(
                    text = romaji,
                    modifier = Modifier.padding(top = 3.dp),
                    style = AppTheme.typography.bodyMedium.copy(
                        color = colors.accent.copy(alpha = 0.9f)
                    ),
                    textAlign = TextAlign.Start
                )
            }
            if (distance <= 2 && translation != null) {
                Text(
                    text = translation,
                    modifier = Modifier.padding(top = 3.dp),
                    style = AppTheme.typography.bodyLarge.copy(
                        color = colors.text.copy(alpha = 0.78f)
                    ),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/** Fades the top and bottom [fade] of the content to transparent. */
private fun Modifier.fadingEdges(fade: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fraction = (fade.toPx() / size.height).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                fraction to Color.Black,
                1f - fraction to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
