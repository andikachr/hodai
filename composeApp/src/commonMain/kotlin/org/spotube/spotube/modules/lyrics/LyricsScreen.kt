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


import org.spotube.millennium.composables.MillenniumBarsLoader
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.ui.component.ApplicationBackButton
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.shell.formatPlayerTime
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error
import spotube.composeapp.generated.resources.lyrics_mode_plain
import spotube.composeapp.generated.resources.lyrics_mode_synced
import spotube.composeapp.generated.resources.lyrics_no_plain
import spotube.composeapp.generated.resources.lyrics_no_synced
import spotube.composeapp.generated.resources.lyrics_pill_romaji
import spotube.composeapp.generated.resources.lyrics_pill_translation
import spotube.composeapp.generated.resources.lyrics_plus_searching_other_sources
import spotube.composeapp.generated.resources.lyrics_rail_repeat
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.settings_section_lyrics
import spotube.composeapp.generated.resources.common_close

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MillenniumTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // The blurred backdrop below is scaled past its bounds to hide
            // the blur's soft edges; without clipping, that oversized layer
            // paints over the sidebar (the milky strip the owner kept
            // seeing). Clip the whole stage to its own bounds.
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    0f to colors.accent.copy(alpha = 0.14f),
                    0.55f to colors.background,
                    1f to colors.background
                )
            )
    ) {
        val isWide = maxWidth >= 840.dp

        // Premium lyrics stage (owner-locked mock): the current cover,
        // blurred into a backdrop and calmed by a deep veil, so the stage
        // glows with the song instead of sitting on a flat panel.
        AsyncImage(
            model = uiState.coverUrl.takeIf { it.isNotBlank() },
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.15f
                    scaleY = 1.15f
                    alpha = 0.75f
                }
                .blur(22.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.55f))
        )

        Column(Modifier.fillMaxSize()) {
            // Stage header (owner-locked mock): the song leads on the left,
            // the Lirik+ layer pills and the close button sit on the right.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (onClose == null) {
                    ApplicationBackButton()
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = uiState.trackTitle,
                        style = AppTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(
                        text = uiState.trackArtists.uppercase() + " · " +
                            stringResource(Res.string.settings_section_lyrics).uppercase(),
                        style = AppTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                MillenniumSegmentedButtonRow(Modifier.widthIn(max = 210.dp)) {
                    MillenniumSegmentedButton(
                        selected = uiState.mode == LyricType.SYNCED,
                        onClick = { viewModel.setMode(LyricType.SYNCED) },
                        shape = MillenniumSegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(Res.string.lyrics_mode_synced)) }
                    MillenniumSegmentedButton(
                        selected = uiState.mode == LyricType.STATIC,
                        onClick = { viewModel.setMode(LyricType.STATIC) },
                        shape = MillenniumSegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(Res.string.lyrics_mode_plain)) }
                }
                Spacer(Modifier.width(14.dp))
                LayerPill(
                    label = stringResource(Res.string.lyrics_pill_romaji).uppercase(),
                    selected = uiState.romajiEnabled,
                    onClick = {
                        val next = !uiState.romajiEnabled
                        settingsViewModel.updateSettings { copy(lyricsRomajiEnabled = next) }
                        viewModel.setRomajiEnabled(next)
                    })
                Spacer(Modifier.width(8.dp))
                LayerPill(
                    label = stringResource(Res.string.lyrics_pill_translation).uppercase(),
                    selected = uiState.translationEnabled,
                    onClick = {
                        val next = !uiState.translationEnabled
                        settingsViewModel.updateSettings {
                            copy(lyricsTranslationEnabled = next)
                        }
                        viewModel.setTranslationEnabled(next)
                    })
                if (onClose != null) {
                    Spacer(Modifier.width(8.dp))
                    MillenniumIconButton(
                        onClick = onClose,
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = stringResource(Res.string.common_close)
                        )
                    }
                }
            }

            if (isWide) {
                // Mock layout: the lyrics own the left, the cover and the
                // transport live on a rail at the right.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(0.58f).fillMaxHeight()) {
                        LyricsBody(uiState, viewModel)
                    }
                    PlayerRail(uiState, viewModel, Modifier.weight(0.42f))
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    LyricsBody(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LyricsBody(uiState: LyricsUiState, viewModel: LyricsViewModel) {
    when {
        uiState.isLoading -> CenteredMessage {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MillenniumBarsLoader(size = 48.dp)
                if (uiState.isSearchingFallback) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.lyrics_plus_searching_other_sources),
                        style = AppTheme.typography.bodyMedium,
                        color = MillenniumTheme.colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        uiState.error != null -> CenteredMessage {
            Text(
                text = uiState.error ?: stringResource(Res.string.common_unknown_error),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.danger,
                textAlign = TextAlign.Center
            )
        }

        uiState.mode == LyricType.SYNCED -> SyncedLyricsContent(
            syncedLyrics = uiState.syncedLyrics ?: emptyList(),
            currentIndex = uiState.currentIndex,
            onLineClick = viewModel::seekToLine,
            romajiLines = uiState.romajiLines,
            translationLines = uiState.translationLines
        )

        uiState.mode == LyricType.STATIC -> PlainLyricsContent(
            plainLyrics = uiState.plainLyrics ?: "",
            romajiLines = uiState.romajiLines,
            translationLines = uiState.translationLines
        )
    }
}

@Composable
private fun CoverArt(url: String, size: Dp, radius: Dp, elevation: Dp, modifier: Modifier = Modifier) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .millenniumSurface(
                background = millenniumPanelBrush(colors),
                shape = RoundedCornerShape(radius),
                borderColor = colors.border,
                borderWidth = 1.dp,
                shadowElevation = elevation
            )
    ) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/** A Lirik+ layer toggle in the stage header: gold while the layer is on. */
@Composable
private fun LayerPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accent else Color.Transparent)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, colors.border, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = if (selected) colors.onAccent else colors.muted
        )
    }
}

/**
 * The stage's right rail (owner-locked mock): the cover card, a thin gold
 * progress line with the times, and the transport row with a big gold
 * play/pause disc.
 */
@Composable
private fun PlayerRail(
    uiState: LyricsUiState,
    viewModel: LyricsViewModel,
    modifier: Modifier = Modifier) {
    val colors = MillenniumTheme.colors
    val fraction = if (uiState.duration > Duration.ZERO) {
        (uiState.position / uiState.duration).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoverArt(
            url = uiState.coverUrl,
            size = 300.dp,
            radius = 24.dp,
            elevation = 14.dp
        )
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.accent)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTime(uiState.position),
                    style = AppTheme.typography.bodySmall,
                    color = colors.muted)
                Text(
                    text = formatPlayerTime(uiState.duration),
                    style = AppTheme.typography.bodySmall,
                    color = colors.muted)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MillenniumIconButton(
                onClick = { viewModel.toggleShuffle() },
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxShuffle,
                    contentDescription = stringResource(
                        if (uiState.shuffleEnabled) {
                            Res.string.player_disable_shuffle_content_description
                        } else {
                            Res.string.player_enable_shuffle_content_description
                        }
                    ),
                    tint = if (uiState.shuffleEnabled) colors.accent else colors.muted)
            }
            MillenniumIconButton(
                onClick = { viewModel.skipToPrevious() },
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxPrevious,
                    contentDescription = stringResource(
                        Res.string.player_previous_content_description
                    ),
                    tint = colors.text)
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    imageVector = if (uiState.isPlaying) {
                        Iconsax.IconsaxPause
                    } else {
                        Iconsax.IconsaxPlay
                    },
                    contentDescription = stringResource(
                        if (uiState.isPlaying) {
                            Res.string.player_pause_content_description
                        } else {
                            Res.string.player_play_content_description
                        }
                    ),
                    modifier = Modifier.size(26.dp),
                    tint = colors.onAccent)
            }
            MillenniumIconButton(
                onClick = { viewModel.skipToNext() },
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxNext,
                    contentDescription = stringResource(
                        Res.string.player_next_content_description
                    ),
                    tint = colors.text)
            }
            MillenniumIconButton(
                onClick = { viewModel.cycleLoop() },
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    imageVector = if (uiState.loopState == LoopState.ONE) {
                        Iconsax.IconsaxRepeateOne
                    } else {
                        Iconsax.IconsaxRepeateMusic
                    },
                    contentDescription = stringResource(Res.string.lyrics_rail_repeat),
                    tint = if (uiState.loopState == LoopState.NONE) {
                        colors.muted
                    } else {
                        colors.accent
                    })
            }
        }
    }
}

@Composable
private fun ColumnScope.CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ColumnScope.SyncedLyricsContent(
    syncedLyrics: List<LyricsLine>,
    currentIndex: Int,
    onLineClick: (LyricsLine) -> Unit,
    romajiLines: List<String?>? = null,
    translationLines: List<String?>? = null) {
    if (syncedLyrics.isEmpty()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_synced),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.muted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    SyncedLyricsList(
        lines = syncedLyrics,
        currentIndex = currentIndex,
        onLineClick = onLineClick,
        romajiLines = romajiLines,
        translationLines = translationLines,
        modifier = Modifier.weight(1f).fillMaxWidth()
    )
}

@Composable
private fun ColumnScope.PlainLyricsContent(
    plainLyrics: String,
    romajiLines: List<String?>? = null,
    translationLines: List<String?>? = null) {
    if (plainLyrics.isBlank()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_plain),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.muted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (romajiLines == null && translationLines == null) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
        ) {
            item {
                Text(
                    text = plainLyrics,
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTheme.typography.titleMedium.copy(lineHeight = 34.sp),
                    color = MillenniumTheme.colors.text,
                    textAlign = TextAlign.Start
                )
            }
        }
        return
    }

    // With Lirik+ layers the plain lyrics render line by line so each line
    // can carry its romaji / translation underneath.
    val lines = plainLyrics.split("\n")
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            if (line.isBlank()) {
                Spacer(Modifier.height(6.dp))
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = line,
                        style = AppTheme.typography.titleMedium,
                        color = MillenniumTheme.colors.text,
                        textAlign = TextAlign.Start
                    )
                    romajiLines?.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { romaji ->
                        Text(
                            text = romaji,
                            modifier = Modifier.padding(top = 2.dp),
                            style = AppTheme.typography.bodyMedium.copy(
                                color = MillenniumTheme.colors.accent.copy(alpha = 0.9f)
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                    translationLines?.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { translation ->
                        Text(
                            text = translation,
                            modifier = Modifier.padding(top = 2.dp),
                            style = AppTheme.typography.bodyMedium.copy(
                                color = MillenniumTheme.colors.text.copy(alpha = 0.78f)
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
