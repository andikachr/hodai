package org.spotube.spotube.modules.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.composeunstyled.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.HodaiDarkRegion
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.floating_lyrics_waiting

/**
 * Hodai floating lyrics: the content of the borderless always-on-top
 * overlay window. A dark pill with the current line big, its romaji and
 * translation layers under it, and the next line dimmed below.
 */
@Composable
fun FloatingLyricsStrip(model: FloatingLyricsModel) {
    val state by model.state.collectAsState()

    HodaiDarkRegion {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.surface.copy(alpha = 0.88f))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val current = state.currentLine
                if (current == null) {
                    Text(
                        text = stringResource(Res.string.floating_lyrics_waiting),
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                } else {
                    // Karaoke when the current line carries word timings:
                    // same sweep colours as the lyrics stage, on the
                    // strip's own palette (primary = the gold accent).
                    val words = state.currentWords?.takeIf {
                        it.isNotEmpty() && state.positionMs >= 0L
                    }
                    if (words != null) {
                        Text(
                            text = buildKaraokeText(
                                words = words,
                                positionMs = state.positionMs,
                                sungColor = AppTheme.colors.primary,
                                activeColor = AppTheme.colors.primary,
                                activeBackground =
                                    AppTheme.colors.primary.copy(alpha = 0.25f),
                                pendingColor = AppTheme.colors.onSurface),
                            style = AppTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(
                            text = current,
                            style = AppTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    state.romajiLine?.takeIf { it.isNotBlank() }?.let { romaji ->
                        Text(
                            text = romaji,
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    state.translationLine?.takeIf { it.isNotBlank() }?.let { translation ->
                        Text(
                            text = translation,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    state.nextLine?.takeIf { it.isNotBlank() }?.let { next ->
                        Text(
                            text = next,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
