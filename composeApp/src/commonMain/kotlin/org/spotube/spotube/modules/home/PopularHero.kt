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

package org.spotube.spotube.modules.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import com.composeunstyled.currentWindowWidthBreakpoint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.FeaturedPlayButton
import org.spotube.spotube.core.ui.coverflow.Coverflow
import org.spotube.spotube.core.ui.coverflow.CoverflowParams
import org.spotube.spotube.core.ui.coverflow.rememberCoverflowState
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.home_popular
import spotube.composeapp.generated.resources.home_popular_play
import spotube.composeapp.generated.resources.media_playlist

/**
 * Hodai home hero (owner-locked mock v11): a "Popular" band whose backdrop is
 * the centred item's artwork, blurred and darkened under a deep-green fade,
 * with the section label, title, artist and a gold play pill on the left and
 * a carousel of circular artwork discs on the right.
 *
 * It rides the same [Coverflow] engine and the same featured feed as the
 * carousel it replaces, and is composed inside a HodaiDarkRegion by the
 * caller so the band keeps the dark brand tokens in light mode too.
 */
@Composable
fun PopularHero(
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val filteredItems = remember(items) {
        items.filter {
            it is MetadataBrowseItem.Album ||
                it is MetadataBrowseItem.Playlist ||
                it is MetadataBrowseItem.Track
        }
    }

    if (filteredItems.isEmpty()) return

    PopularHeroContent(
        items = filteredItems,
        modifier = modifier
    )
}

@Composable
private fun PopularHeroContent(
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val centerIndex = items.size / 2
    var selectedIndex by remember { mutableIntStateOf(centerIndex) }
    val state = rememberCoverflowState(centerIndex) { index ->
        // The coverflow reports -1 while the centred item scrolls out; only
        // real indices may drive the text block and backdrop.
        if (index in items.indices) selectedIndex = index
    }
    val isSmallScreen = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val centerItem = items[selectedIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(if (isSmallScreen) 220.dp else 280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.background)
    ) {
        Crossfade(
            targetState = centerItem,
            label = "popular_hero_backdrop"
        ) { item ->
            AsyncImage(
                model = item.heroThumbnailUrl(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.18f
                        scaleY = 1.18f
                    }
                    .blur(24.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Darken the artwork, then fade it into the brand green from the left
        // so the text block stays readable (mock .herofade stops: .96/.55/.12).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppTheme.colors.background.copy(alpha = 0.96f),
                            AppTheme.colors.background.copy(alpha = 0.55f),
                            AppTheme.colors.background.copy(alpha = 0.12f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(if (isSmallScreen) 200.dp else 280.dp)) {
                Text(
                    text = stringResource(Res.string.home_popular).uppercase(),
                    style = AppTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    ),
                    color = AppTheme.colors.primary.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = centerItem.heroTitle(),
                    style = AppTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = centerItem.heroSubtitle(),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                PopularPlayPill(item = centerItem)
                Spacer(modifier = Modifier.height(16.dp))
                PopularDots(
                    count = items.size,
                    selectedIndex = selectedIndex
                )
            }

            Coverflow(
                state = state,
                params = CoverflowParams(
                    size = 0.52f,
                    offset = 0.5f,
                    angle = 12f,
                    shift = 0.255f,
                    zoom = 0.7864f,
                    mirror = false
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                items(
                    items = items,
                    key = { index: Int -> items[index].hashCode() }
                ) { item: MetadataBrowseItem ->
                    PopularDisc(
                        item = item,
                        isCentered = selectedIndex == items.indexOf(item),
                        onScrollToCenter = {
                            state.scrollToItem(items.indexOf(item))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularDisc(
    item: MetadataBrowseItem,
    isCentered: Boolean,
    modifier: Modifier = Modifier,
    onScrollToCenter: () -> Unit = {}
) {
    val navigationCommands = koinInject<NavigationCommands>()

    val dimAlpha by animateFloatAsState(
        targetValue = if (isCentered) 0f else 0.5f,
        label = "disc_dim_alpha"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable {
                if (!isCentered) {
                    onScrollToCenter()
                    return@clickable
                }
                when (item) {
                    is MetadataBrowseItem.Album -> navigationCommands.navigateTo(
                        Routes.Album(item.data.id)
                    )

                    is MetadataBrowseItem.Playlist -> navigationCommands.navigateTo(
                        Routes.Playlist(item.data.id)
                    )

                    else -> {}
                }
            }
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = item.heroThumbnailUrl(),
            contentDescription = item.heroTitle(),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
        )

        if (isCentered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, AppTheme.colors.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun PopularPlayPill(
    item: MetadataBrowseItem,
    modifier: Modifier = Modifier
) {
    val audioPlayerQueue = koinInject<AudioPlayerQueue>()
    val navigationCommands = koinInject<NavigationCommands>()

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AppTheme.colors.primary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeaturedPlayButton(
            item = item,
            audioPlayerQueue = audioPlayerQueue,
            navigationCommands = navigationCommands,
            enabled = true,
            contentColor = AppTheme.colors.onPrimary
        )
        Text(
            text = stringResource(Res.string.home_popular_play).uppercase(),
            style = AppTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = AppTheme.colors.onPrimary,
            modifier = Modifier.padding(end = 20.dp)
        )
    }
}

@Composable
private fun PopularDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isActive = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(if (isActive) 20.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            AppTheme.colors.primary
                        } else {
                            AppTheme.colors.onSurface.copy(alpha = 0.35f)
                        }
                    )
            )
        }
    }
}

private fun MetadataBrowseItem.heroThumbnailUrl(): String? = when (this) {
    is MetadataBrowseItem.Album -> data.thumbnails.firstOrNull()?.url
    is MetadataBrowseItem.Playlist -> data.thumbnails.firstOrNull()?.url
    is MetadataBrowseItem.Track ->
        (data.album?.thumbnails ?: data.thumbnails)?.firstOrNull()?.url

    else -> null
}

private fun MetadataBrowseItem.heroTitle(): String = when (this) {
    is MetadataBrowseItem.Album -> data.title
    is MetadataBrowseItem.Playlist -> data.title
    is MetadataBrowseItem.Track -> data.title
    else -> ""
}

@Composable
private fun MetadataBrowseItem.heroSubtitle(): String = when (this) {
    is MetadataBrowseItem.Album -> data.artists.joinToString { it.name }
    is MetadataBrowseItem.Playlist -> data.owner?.displayName ?: data.owner?.username
        ?: stringResource(Res.string.media_playlist)

    is MetadataBrowseItem.Track -> data.artists.joinToString { it.name }
    else -> ""
}
