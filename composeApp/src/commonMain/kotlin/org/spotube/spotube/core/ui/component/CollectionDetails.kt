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

package org.spotube.spotube.core.ui.component


import androidx.compose.animation.ExperimentalSharedTransitionApi
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

/**
 * Collection header in the Hodai mock language: no card — the artwork's own
 * colors wash the backdrop (blurred, veiled), the artwork sits at the left,
 * and the right side carries the eyebrow, the big title, the description,
 * the owner/meta line and one gold play pill with ring outline actions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetails(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    /** Guest in a jam session: play/shuffle are replaced by Add to Jam. */
    isJamGuest: Boolean = false,
    onAddToJam: (() -> Unit)? = null,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    isPlayLoading: Boolean = false,
    isShuffleLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false,
    onFollowClick: () -> Unit = { },
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null,
    /** Small caps label above the title, e.g. PLAYLIST or ALBUM. */
    eyebrow: String? = null,
    /** Extra meta after the owner name, e.g. "24 songs · about 1 hr 40 min". */
    metaText: String? = null) {
    Box(modifier = modifier.fillMaxWidth()) {
        val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

        // Blurred artwork wash behind the header (locked blur recipe).
        // matchParentSize: the wash follows the header's height instead of
        // stretching it (this header lives inside a lazy list item).
        if (imageURL.isNotBlank()) {
            AsyncImage(
                model = imageURL,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                        alpha = 0.75f
                    }
                    .blur(28.dp),
                contentScale = ContentScale.Crop)
            Box(
                Modifier
                    .matchParentSize()
                    .background(AppTheme.colors.background.copy(alpha = 0.58f)))
        }

        val artwork = @Composable {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 100.dp else 200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .sharedElementOrNone(
                        key = sharedElementKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope)
                    .shimmerApply(),
                contentAlignment = Alignment.Center) {
                if (imageURL.isNotBlank()) {
                    AsyncImage(
                        model = imageURL,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else if (imageResource != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(imageResource),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = AppTheme.typography.displaySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }
        }

        val metadata = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (eyebrow != null) {
                    TextWithShimmer(
                        text = eyebrow.uppercase(),
                        style = AppTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp),
                        color = MillenniumTheme.colors.accent)
                }
                TextWithShimmer(
                    text = title,
                    style = if (isCompact) {
                        AppTheme.typography.titleLarge
                    } else {
                        AppTheme.typography.displaySmall
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)

                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }

        val metaRow = @Composable {
            Row(
                modifier = Modifier.clickable(onClick = onOwnerClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ownerImageURL != null) {
                    AsyncImage(
                        model = ownerImageURL,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .shimmerApply(),
                        contentScale = ContentScale.Crop)
                } else {
                    MillenniumIcon(
                        imageVector = Iconsax.User,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                AppTheme.colors.primaryContainer,
                                CircleShape
                            )
                            .padding(4.dp),
                        tint = AppTheme.colors.onPrimaryContainer)
                }
                TextWithShimmer(
                    text = ownerName,
                    style = AppTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold))
                if (metaText != null) {
                    TextWithShimmer(
                        text = "· $metaText",
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }
        }

        val actionsRow = @Composable {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isJamGuest) {
                    if (onAddToJam != null) {
                        MillenniumButton(onClick = onAddToJam) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = stringResource(Res.string.track_options_add_to_jam))
                            TextWithShimmer(text = stringResource(Res.string.track_options_add_to_jam))
                        }
                    }
                } else {
                    MillenniumButton(
                        onClick = onPlay,
                        loading = isPlayLoading) {
                        MillenniumIcon(
                            imageVector = if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                            contentDescription = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
                        TextWithShimmer(text = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
                    }
                    RingIconButton(
                        onClick = onShufflePlay,
                        loading = isShuffleLoading) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxShuffle,
                            contentDescription = stringResource(Res.string.collection_details_shuffle_play))
                    }
                    RingIconButton(
                        onClick = onAddToQueue,
                        loading = isAddToQueueLoading) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxAddSquare,
                            contentDescription = stringResource(Res.string.player_add_to_queue_content_description))
                    }
                    if (showFollowButton) {
                        RingIconButton(
                            onClick = onFollowClick,
                            loading = isFollowLoading) {
                            MillenniumIcon(
                                imageVector = if (isFollowing) Iconsax.IconsaxHeart2 else Iconsax.IconsaxHeart,
                                contentDescription = stringResource(
                                    if (isFollowing) Res.string.collection_details_unfollow
                                    else Res.string.collection_details_follow),
                                tint = if (isFollowing) {
                                    MillenniumTheme.colors.accent
                                } else {
                                    AppTheme.colors.onSurfaceVariant
                                })
                        }
                    }
                    if (onEdit != null) {
                        RingIconButton(onClick = onEdit) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxEdit,
                                contentDescription = stringResource(Res.string.collection_details_edit_playlist))
                        }
                    }
                }
            }
        }

        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    artwork()
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        metadata()
                        metaRow()
                    }
                }
                actionsRow()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                artwork()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metadata()
                    metaRow()
                    actionsRow()
                }
            }
        }
    }
}

/** A 44dp circle outline button, the mock's secondary action shape. */
@Composable
private fun RingIconButton(
    onClick: () -> Unit,
    loading: Boolean = false,
    content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, AppTheme.colors.outline, CircleShape),
        contentAlignment = Alignment.Center) {
        MillenniumIconButton(
            onClick = onClick,
            loading = loading,
            style = MillenniumIconButtonStyle.Quiet) {
            content()
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CollectionDetailsPreview() {
    CollectionDetails(
        title = "My Playlist",
        description = "A collection of my favorite songs.",
        imageURL = "https://example.com/playlist.jpg",
        ownerName = "John Doe",
        ownerImageURL = "https://example.com/john.jpg",
        onOwnerClick = {},
        onPlay = {},
        onShufflePlay = {},
        onAddToQueue = {})
}
