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


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.HodaiDarkRegion
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackCard
import org.spotube.spotube.core.ui.component.UserCard
import org.spotube.spotube.core.ui.component.VerticalScrollbar
import org.spotube.spotube.core.ui.component.cards.FeaturedPhotoCard
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.shell.ConnectStatusBanner
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.support.LocalSupportPrompt
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_app_name
import spotube.composeapp.generated.resources.support_button_label
import spotube.composeapp.generated.resources.devices_title
import spotube.composeapp.generated.resources.home_title_browse
import spotube.composeapp.generated.resources.jam_screen_title
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import spotube.composeapp.generated.resources.cookie_regular
import spotube.composeapp.generated.resources.search_placeholder

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel) {
    val navigationCommands = koinInject<NavigationCommands>()
    val openSupport = LocalSupportPrompt.current
    val isSmallScreen = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedGenreId = when (val s = state) {
        is HomeScreenState.Data -> s.selectedGenreId
        else -> null
    }

    LaunchedEffect(selectedGenreId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (
                    lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 2
                ) {
                    viewModel.loadMoreData()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)
    ) {
        ApplicationMainBar(
            backButton = false,
            title = {
                if (isSmallScreen) {
                    Text(
                        stringResource(Res.string.welcome_app_name),
                        style = AppTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily(
                                listOf(Font(Res.font.cookie_regular, weight = FontWeight.Normal))
                            )
                        ),
                        color = AppTheme.colors.onSurface
                    )
                } else {
                    HomeSearchPill(
                        onClick = { navigationCommands.navigateTo(Routes.Search) })
                }

            },
            actions = {
                if (isSmallScreen) {
                    MillenniumIconButton(
                        onClick = { navigationCommands.navigateTo(Routes.Devices) },
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxMirrorScreenRegular,
                            contentDescription = stringResource(Res.string.devices_title)
                        )
                    }
                    MillenniumIconButton(
                        onClick = { navigationCommands.navigateTo(Routes.Jam) },
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxSoundTwotone,
                            contentDescription = stringResource(Res.string.jam_screen_title)
                        )
                    }
                    val supportLabel = stringResource(Res.string.support_button_label)
                    MillenniumIconButton(
                        onClick = openSupport,
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxInformation,
                            contentDescription = supportLabel
                        )
                    }
                }
            })
        if (isSmallScreen) {
            ConnectStatusBanner(
                onOpenRemote = { navigationCommands.navigateTo(Routes.RemoteControl) },
                onOpenJam = { navigationCommands.navigateTo(Routes.Jam) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (!isSmallScreen) {
                HomeContent(
                    listState = listState,
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onGenreSelected = { viewModel.selectGenre(it) })
            } else {
                MillenniumPullToRefreshBox(
                    isRefreshing = state is HomeScreenState.Loading,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeContent(
                        listState = listState,
                        state = state,
                        onRetry = { viewModel.refresh() },
                        onGenreSelected = { viewModel.selectGenre(it) })
                }
            }

            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * The locked mock's top-of-home search pill: a quiet inset capsule that
 * opens the Search screen. Desktop only — small screens keep their icon
 * actions in the main bar.
 */
@Composable
private fun HomeSearchPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(380.dp)
            .clip(CircleShape)
            .background(MillenniumTheme.colors.inset)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MillenniumIcon(
            imageVector = Iconsax.IconsaxSearch,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MillenniumTheme.colors.muted
        )
        Text(
            text = stringResource(Res.string.search_placeholder),
            style = AppTheme.typography.bodyMedium,
            color = MillenniumTheme.colors.muted,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    state: HomeScreenState,
    onRetry: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (state) {
            is HomeScreenState.Loading -> {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(3) {
                            SkeletonTree(true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .padding(horizontal = 16.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(4) {
                                            PlayableCard(
                                                title = "Item Title",
                                                subtitle = "Subtitle",
                                                imageURL = "https://placehold.co/600x400"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is HomeScreenState.Error -> {
                item {
                    ErrorDisplay(
                        errorMessage = state.message,
                        onRetry = onRetry
                    )
                }
            }

            is HomeScreenState.Data -> {
                if (state.featuredItems.isNotEmpty()) {
                    item {
                        // The hero is a brand anchor like the sidebar and the
                        // player: it keeps the dark tokens in light mode too.
                        HodaiDarkRegion {
                            PopularHero(items = state.featuredItems)
                        }
                    }
                }

                if (state.genres.isNotEmpty()) {
                    item {
                        GenreTabs(
                            genres = state.genres,
                            selectedGenreId = state.selectedGenreId,
                            onGenreSelected = onGenreSelected
                        )
                    }
                }

                val currentSections = state.selectedGenreId?.let { state.browseSections[it] }

                if (currentSections == null) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400"
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(currentSections) { index, section ->
                        if (index == 0) {
                            HomeFeaturedSection(section = section)
                        } else {
                            HomeSection(
                                title = section.title,
                                subtitle = section.description,
                                items = section.items
                            )
                        }
                    }
                }

                if (state is HomeScreenState.Data.LoadingMore) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val rowState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!subtitle.isNullOrEmpty()) Text(
            text = subtitle,
            style = AppTheme.typography.labelMedium.copy(
                color = AppTheme.colors.secondary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = title,
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-12).dp)
        )

        LazyRow(
            state = rowState,
            modifier = Modifier.dragScrollable(rowState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(items) { browseItem ->
                when (browseItem) {
                    is MetadataBrowseItem.Album -> AlbumCard(album = browseItem.data)
                    is MetadataBrowseItem.Artist -> ArtistCard(artist = browseItem.data)
                    is MetadataBrowseItem.Playlist -> PlaylistCard(playlist = browseItem.data)
                    is MetadataBrowseItem.Track -> TrackCard(track = browseItem.data)
                    is MetadataBrowseItem.User -> UserCard(user = browseItem.data)
                }
            }
        }
    }
}

/**
 * The first browse section under the hero uses the wide photo cards of the
 * locked Hodai mock (artwork-filled card + caption chip); every later section
 * keeps the standard [HomeSection] cards. Header rendering mirrors
 * [HomeSection] so the two stay visually identical.
 */
@Composable
private fun HomeFeaturedSection(
    section: MetadataBrowseSection,
    modifier: Modifier = Modifier
) {
    val navigationCommands = koinInject<NavigationCommands>()
    val audioPlayerQueue = koinInject<AudioPlayerQueue>()
    val scope = rememberCoroutineScope()
    val rowState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!section.description.isNullOrEmpty()) Text(
            text = section.description!!,
            style = AppTheme.typography.labelMedium.copy(
                color = AppTheme.colors.secondary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = section.title,
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-12).dp)
        )

        LazyRow(
            state = rowState,
            modifier = Modifier.dragScrollable(rowState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(section.items) { browseItem ->
                when (browseItem) {
                    is MetadataBrowseItem.Album -> FeaturedPhotoCard(
                        title = browseItem.data.title,
                        subtitle = browseItem.data.artists.joinToString { it.name },
                        imageURL = browseItem.data.thumbnails.firstOrNull()?.url,
                        onClick = {
                            navigationCommands.navigateTo(Routes.Album(browseItem.data.id))
                        }
                    )

                    is MetadataBrowseItem.Playlist -> FeaturedPhotoCard(
                        title = browseItem.data.title,
                        subtitle = browseItem.data.owner?.displayName
                            ?: browseItem.data.owner?.username
                            ?: browseItem.data.description,
                        imageURL = browseItem.data.thumbnails.firstOrNull()?.url,
                        onClick = {
                            navigationCommands.navigateTo(Routes.Playlist(browseItem.data.id))
                        }
                    )

                    is MetadataBrowseItem.Track -> FeaturedPhotoCard(
                        title = browseItem.data.title,
                        subtitle = browseItem.data.artists.joinToString { it.name },
                        imageURL = (browseItem.data.album?.thumbnails
                            ?: browseItem.data.thumbnails)?.firstOrNull()?.url,
                        onClick = {
                            scope.launch {
                                audioPlayerQueue.load(
                                    entries = listOf(
                                        QueueEntry.StreamingTrack(
                                            track = browseItem.data,
                                            url = ""
                                        )
                                    ),
                                    autoPlay = true,
                                    startPosition = 0,
                                    collectionEntry = null
                                )
                            }
                        }
                    )

                    is MetadataBrowseItem.Artist -> FeaturedPhotoCard(
                        title = browseItem.data.name,
                        imageURL = browseItem.data.thumbnails.firstOrNull()?.url,
                        onClick = {
                            navigationCommands.navigateTo(Routes.Artist(browseItem.data.id))
                        }
                    )

                    // User cards have no destination yet (see UserCard's TODO),
                    // so the photo card stays non-interactive for them.
                    is MetadataBrowseItem.User -> FeaturedPhotoCard(
                        title = browseItem.data.displayName ?: browseItem.data.username,
                        imageURL = browseItem.data.thumbnails.firstOrNull()?.url
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreTabs(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit
) {
    val rowState = rememberLazyListState()

    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(genres) { genre ->
            MillenniumButton(
                onClick = { onGenreSelected(genre.id) },
                style = if (selectedGenreId == genre.id) {
                    MillenniumButtonStyle.Primary
                } else {
                    MillenniumButtonStyle.Secondary
                }
            ) {
                Text(genre.name)
            }
        }
    }
}
