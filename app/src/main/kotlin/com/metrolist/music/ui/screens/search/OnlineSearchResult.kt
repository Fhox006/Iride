/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.search

import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_EPISODE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PODCAST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PROFILE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.component.LocalArtworkBackgroundColor
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SharedQuickPicksCarousel
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.recordSearchHistoryOpen
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlineSearchViewModel
import java.net.URLDecoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private fun filterSectionTitleRes(filter: YouTube.SearchFilter): Int =
    when (filter) {
        FILTER_SONG -> R.string.filter_songs
        FILTER_VIDEO -> R.string.filter_videos
        FILTER_ALBUM -> R.string.filter_albums
        FILTER_ARTIST -> R.string.filter_artists
        FILTER_COMMUNITY_PLAYLIST -> R.string.filter_community_playlists
        FILTER_FEATURED_PLAYLIST -> R.string.filter_featured_playlists
        FILTER_PODCAST -> R.string.filter_podcasts
        FILTER_EPISODE -> R.string.filter_episodes
        FILTER_PROFILE -> R.string.filter_profiles
        else -> R.string.filter_all
    }

@Composable
private fun SearchResultsStyle(content: @Composable () -> Unit) {
    if (LocalArtworkBackgroundColor.current != null) {
        content()
        return
    }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color.White,
            secondary = Color.White,
            tertiary = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White,
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalLibraryTextColor provides Color.White,
            content = content,
        )
    }
}

@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    pureBlack: Boolean = false,
    savedStateHandle: SavedStateHandle? = null,
) {
    val encodedQuery = navController.currentBackStackEntry?.arguments?.getString("query").orEmpty()
    val decodedQuery = remember(encodedQuery) {
        runCatching { URLDecoder.decode(encodedQuery, "UTF-8") }.getOrDefault(encodedQuery)
    }
    SearchScreen(
        navController = navController,
        pureBlack = pureBlack,
        savedStateHandle = savedStateHandle ?: navController.currentBackStackEntry?.savedStateHandle ?: return,
        initialQuery = decodedQuery,
        onlineSearchResultViewModel = viewModel,
    )
}

/** Original Smart Search results shared by the search tab and the query route. */
@Composable
fun OnlineSearchResultsBody(
    navController: NavController,
    viewModel: OnlineSearchViewModel,
    pureBlack: Boolean,
    useIrideStyle: Boolean,
    isSearchFocused: Boolean,
    queryText: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onDismissSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    onScrollChanged: (Boolean) -> Unit = {},
    pageMenuSections: List<List<LibraryPageMenuAction>> = emptyList(),
    lazyListState: LazyListState = rememberLazyListState(),
    submittedQuery: String = queryText,
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex > 0 }.collect(onScrollChanged)
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val searchQuery = submittedQuery.trim()
    val isUrlQuery = remember(searchQuery) { YouTubeUrlParser.parse(searchQuery) != null }
    LaunchedEffect(searchQuery, isUrlQuery) {
        if (isUrlQuery) return@LaunchedEffect
        if (searchQuery != viewModel.query && searchQuery.isNotEmpty()) delay(300)
        viewModel.search(searchQuery)
        viewModel.loadSmartSearch()
    }
    LaunchedEffect(viewModel.query, viewModel.searchRevision) {
        lazyListState.scrollToItem(0)
    }
    val ytItemContent: @Composable (YTItem, Modifier, Boolean) -> Unit = { item, itemModifier, isSwipeable ->
        val longClick = {
            menuState.show(
                anchored = true,
            ) {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    is ArtistItem ->
                        YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                    is PodcastItem ->
                        YouTubePlaylistMenu(
                            playlist = item.asPlaylistItem(),
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                    is EpisodeItem ->
                        YouTubeSongMenu(
                            song = item.asSongItem(),
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
            YouTubeListItem(
                item = item,
                isActive =
                    when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        is EpisodeItem -> mediaMetadata?.id == item.id
                        else -> false
                    },
                isPlaying = isPlaying,
                isSwipeable = isSwipeable,
                showDivider = false,
                trailingContent = {
                    if (item is ArtistItem) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        IconButton(onClick = longClick) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    }
                },
                modifier =
                    itemModifier
                        .irideFolioCombinedClickable(
                            onClick = {
                                if (!pauseSearchHistory)
                                    database.recordSearchHistoryOpen(viewModel.query, item)
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id)
                                            playerConnection.togglePlayPause()
                                        else
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata(),
                                                )
                                            )
                                    }
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem ->
                                        navController.navigate("online_playlist/${item.id}")
                                    is PodcastItem ->
                                        navController.navigate("online_podcast/${item.id}")
                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id)
                                            playerConnection.togglePlayPause()
                                        else
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata(),
                                                )
                                            )
                                    }
                                }
                            },
                            onLongClick = longClick,
                        ),
            )
        }
    }

    val searchResultRow: @Composable (List<YTItem>) -> Unit = { rowItems ->
        LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 8.dp)) {
            items(
                items = rowItems,
                key = { it.id },
            ) { rowItem ->
                val isAlbum = rowItem is AlbumItem
                val isVideo = rowItem is SongItem && rowItem.isVideoSong
                YouTubeGridItem(
                    item = rowItem,
                    isActive =
                        when (rowItem) {
                            is SongItem -> mediaMetadata?.id == rowItem.id
                            is AlbumItem -> mediaMetadata?.album?.id == rowItem.id
                            is EpisodeItem -> mediaMetadata?.id == rowItem.id
                            else -> false
                        },
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    thumbnailRatio = if (isVideo) 16f / 9f else 1f,
                    thumbnailCornerRadius = if (isVideo) 8.dp else 3.dp,
                    size =
                        when {
                            isAlbum -> 180.dp
                            isVideo -> 110.dp
                            else -> 148.dp
                        },
                    modifier =
                        Modifier.irideFolioCombinedClickable(
                                onClick = {
                                    if (!pauseSearchHistory)
                                        database.recordSearchHistoryOpen(viewModel.query, rowItem)
                                    when (rowItem) {
                                        is SongItem -> {
                                            if (rowItem.id == mediaMetadata?.id)
                                                playerConnection.togglePlayPause()
                                            else
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = rowItem.id),
                                                        rowItem.toMediaMetadata(),
                                                    )
                                                )
                                        }
                                        is AlbumItem ->
                                            navController.navigate("album/${rowItem.id}")
                                        is ArtistItem ->
                                            navController.navigate("artist/${rowItem.id}")
                                        is PlaylistItem ->
                                            navController.navigate("online_playlist/${rowItem.id}")
                                        is PodcastItem ->
                                            navController.navigate("online_podcast/${rowItem.id}")
                                        is EpisodeItem -> {
                                            if (rowItem.id == mediaMetadata?.id)
                                                playerConnection.togglePlayPause()
                                            else
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = rowItem.id),
                                                        rowItem.toMediaMetadata(),
                                                    )
                                                )
                                        }
                                    }
                                },
                                onLongClick = {
                                    menuState.show(
                                        anchored = true,
                                    ) {
                                        when (rowItem) {
                                            is SongItem ->
                                                YouTubeSongMenu(
                                                    song = rowItem,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            is AlbumItem ->
                                                YouTubeAlbumMenu(
                                                    albumItem = rowItem,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            is ArtistItem ->
                                                YouTubeArtistMenu(
                                                    artist = rowItem,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            is PlaylistItem ->
                                                YouTubePlaylistMenu(
                                                    playlist = rowItem,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            is PodcastItem ->
                                                YouTubePlaylistMenu(
                                                    playlist = rowItem.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            is EpisodeItem ->
                                                YouTubeSongMenu(
                                                    song = rowItem.asSongItem(),
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                        }
                                    }
                                },
                            )
                            .animateItem(),
                )
            }
        }
    }

    val searchResultRowPlaceholder: @Composable () -> Unit = {
        Row(modifier = Modifier.padding(start = 4.dp)) {
            repeat(3) { GridItemPlaceHolder() }
        }
    }

    val resultsListContent: LazyListScope.() -> Unit = {
        val order = viewModel.smartSearchOrder
        val topResult = viewModel.topResult
        var isFirstSection = true
        if (viewModel.query == searchQuery && order.isNotEmpty()) {
            topResult?.let { top ->
                val titleTopPadding = if (header != null) 0.dp else null
                isFirstSection = false
                item(key = "smart_top_title") {
                    SearchResultsStyle {
                        NavigationTitle(
                            top.title,
                            useIrideStyle = useIrideStyle,
                            showDivider = false,
                            topPadding = titleTopPadding,
                        )
                    }
                }
                items(
                    items = top.items,
                    key = { "smart_top_${it.id}" },
                ) { item ->
                    SearchResultsStyle {
                        ytItemContent(item, Modifier.padding(horizontal = 16.dp).animateItem(), true)
                    }
                }
            }

            order.forEach { sectionFilter ->
                val page = viewModel.viewStateMap[sectionFilter.value]
                val discovery = viewModel.songDiscovery
                if (sectionFilter == FILTER_ARTIST && discovery != null &&
                    (discovery.artists.isNotEmpty() || discovery.similarArtists.isNotEmpty())
                ) {
                    listOf(
                        "credited" to (R.string.search_track_artists to discovery.artists),
                        "similar" to (R.string.search_similar_artists to discovery.similarArtists),
                    ).forEach { (sectionKey, section) ->
                        val (titleRes, artists) = section
                        if (artists.isNotEmpty()) {
                            val titleTopPadding = if (header != null && isFirstSection) 0.dp else null
                            isFirstSection = false
                            item(key = "smart_title_$sectionKey") {
                                SearchResultsStyle {
                                    NavigationTitle(
                                        title = stringResource(titleRes),
                                        useIrideStyle = useIrideStyle,
                                        showDivider = false,
                                        topPadding = titleTopPadding,
                                    )
                                }
                            }
                            item(key = "smart_row_$sectionKey") {
                                SearchResultsStyle {
                                    key(viewModel.query, viewModel.searchRevision) { searchResultRow(artists) }
                                }
                            }
                        }
                    }
                    return@forEach
                }
                val sectionItems = viewModel.sectionItems(sectionFilter)
                if (page != null && sectionItems.isEmpty()) return@forEach
                val titleTopPadding = if (header != null && isFirstSection) 0.dp else null
                isFirstSection = false
                item(key = "smart_title_${sectionFilter.value}") {
                    val titleRes = when {
                        sectionFilter == FILTER_SONG && discovery?.recommendations?.isNotEmpty() == true ->
                            R.string.search_songs_recommendations
                        sectionFilter == FILTER_ALBUM && discovery?.albums?.isNotEmpty() == true ->
                            R.string.search_albums_to_explore
                        sectionFilter == FILTER_COMMUNITY_PLAYLIST && discovery?.playlists?.isNotEmpty() == true ->
                            R.string.search_playlists_to_explore
                        else -> filterSectionTitleRes(sectionFilter)
                    }
                    SearchResultsStyle {
                        NavigationTitle(
                            title = stringResource(titleRes),
                            useIrideStyle = useIrideStyle,
                            showDivider = false,
                            topPadding = titleTopPadding,
                        )
                    }
                }
                item(key = "smart_row_${sectionFilter.value}") {
                    SearchResultsStyle {
                        key(viewModel.query, viewModel.searchRevision) {
                            if (sectionFilter == FILTER_SONG) {
                                SharedQuickPicksCarousel(
                                    items = viewModel.songs,
                                    key = { it.id },
                                    loading = page == null && sectionItems.isEmpty(),
                                    contentPadding = 16.dp,
                                    usePager = true,
                                ) { song, itemWidth ->
                                    ytItemContent(song, Modifier.width(itemWidth), false)
                                }
                            } else if (page != null || sectionItems.isNotEmpty()) {
                                searchResultRow(sectionItems)
                            } else {
                                searchResultRowPlaceholder()
                            }
                        }
                    }
                }
            }

            val allResolved = order.all { viewModel.viewStateMap[it.value] != null }
            val allEmpty =
                allResolved &&
                    topResult == null &&
                    order.all { viewModel.viewStateMap[it.value]?.items.isNullOrEmpty() }
            if (allEmpty) {
                item {
                    SearchResultsStyle {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }
        } else {
            item {
                ShimmerHost {
                    repeat(8) { ListItemPlaceHolder() }
                }
            }
        }
        if (header == null) {
            item(key = "bottom_spacer") {
                Spacer(
                    modifier =
                        Modifier.height(
                            MiniPlayerHeight + MiniPlayerBottomSpacing + NavigationBarHeight
                        )
                )
            }
        }
    }

    if (header != null) {
        OnlineSearchScreen(
            query = submittedQuery,
            onQueryChange = onQueryChange,
            navController = navController,
            onSearch = onSearch,
            onDismiss = onDismissSuggestions,
            pureBlack = pureBlack,
            isFocused = isSearchFocused,
            header = header,
            resultsContent = resultsListContent,
            pageMenuSections = pageMenuSections,
            lazyListState = lazyListState,
        )
    } else if (searchQuery.isEmpty() || isUrlQuery) {
        OnlineSearchScreen(
            query = queryText,
            onQueryChange = onQueryChange,
            navController = navController,
            onSearch = onSearch,
            onDismiss = onDismissSuggestions,
            pureBlack = pureBlack,
            pageMenuSections = pageMenuSections,
        )
    } else {
        IridePageLayout(
            titleRes = R.string.search,
            state = lazyListState,
            modifier = modifier,
            bottomPadding =
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            menuSections = pageMenuSections,
        ) {
            resultsListContent()
        }
    }
}
