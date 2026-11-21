/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.search

import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.component.navigateSearchSection
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioSection
import com.metrolist.music.ui.component.IrideFolioSectionSkeleton
import com.metrolist.music.ui.component.IrideFolioSectionEntranceOrder
import com.metrolist.music.ui.component.LocalFolioSectionEntranceOrder
import com.metrolist.music.ui.component.LocalFolioSectionFastReveal
import com.metrolist.music.ui.component.LocalArtworkContentColor
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.rememberIrideFolioFlingBehavior
import com.metrolist.music.ui.component.rubberBandOverscroll
import androidx.compose.foundation.gestures.Orientation
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.component.LocalArtworkBackgroundColor
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.TrackOverflowIcon
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
internal fun SearchResultsStyle(content: @Composable () -> Unit) {
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
            LocalArtworkContentColor provides Color.White,
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
    var lastScrollResetQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var lastScrollResetRevision by rememberSaveable { mutableIntStateOf(Int.MIN_VALUE) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex > 0 }.collect(onScrollChanged)
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val searchQuery = submittedQuery.trim()
    val isUrlQuery = remember(searchQuery) { YouTubeUrlParser.parse(searchQuery) != null }
    LaunchedEffect(searchQuery, isUrlQuery) {
        if (isUrlQuery) return@LaunchedEffect
        if (searchQuery != viewModel.query) viewModel.search(searchQuery)
        viewModel.loadSmartSearch()
    }
    LaunchedEffect(viewModel.query, viewModel.searchRevision) {
        if (lastScrollResetQuery == viewModel.query &&
            lastScrollResetRevision == viewModel.searchRevision
        ) return@LaunchedEffect
        lazyListState.scrollToItem(0)
        lastScrollResetQuery = viewModel.query
        lastScrollResetRevision = viewModel.searchRevision
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
                    if (item is ArtistItem || item is AlbumItem || item is PlaylistItem) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).rotate(-45f),
                        )
                    } else {
                        IconButton(onClick = longClick) {
                            TrackOverflowIcon()
                        }
                    }
                },
                modifier =
                    Modifier.irideFolioCombinedClickable(
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
                                    is AlbumItem -> navController.navigateSearchSection("album/${item.id}", simpleMotion = false)
                                    is ArtistItem -> navController.navigateSearchSection("artist/${item.id}", simpleMotion = false)
                                    is PlaylistItem ->
                                        navController.navigateSearchSection("online_playlist/${item.id}", simpleMotion = false)
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
                        ).then(itemModifier),
            )
        }
    }

    val searchResultRow: @Composable (List<YTItem>) -> Unit = { rowItems ->
        val rowState = rememberLazyListState()
        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
        LazyRow(
            state = rowState,
            flingBehavior = rememberIrideFolioFlingBehavior(),
            overscrollEffect = null,
            modifier = Modifier.rubberBandOverscroll(Orientation.Horizontal, rowState),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
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
                                            navController.navigateSearchSection("album/${rowItem.id}", simpleMotion = false)
                                        is ArtistItem ->
                                            navController.navigateSearchSection("artist/${rowItem.id}", simpleMotion = false)
                                        is PlaylistItem ->
                                            navController.navigateSearchSection("online_playlist/${rowItem.id}", simpleMotion = false)
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
                            ),
                )
            }
        }
        }
    }

    val presentedSections = remember(viewModel.query, viewModel.searchRevision) { mutableSetOf<String>() }
    val entranceOrder = remember(viewModel.query, viewModel.searchRevision) { IrideFolioSectionEntranceOrder() }
    val resultsListContent: LazyListScope.() -> Unit = {
        val order = viewModel.smartSearchOrder
        val top = viewModel.topResult
        val revision = "${viewModel.query}:${viewModel.searchRevision}"
        if (viewModel.query == searchQuery && (order.isNotEmpty() || top != null)) {
            item(key = "smart_top") {
                SearchResultsStyle {
                    IrideFolioSection(
                        sectionKey = "$revision:top", ready = top != null,
                        presentedSections = presentedSections,
                        artwork = top?.items.orEmpty().map { it.thumbnail },
                        placeholder = { IrideFolioSectionSkeleton(148.dp, songs = true, songRows = 5) },
                    ) {
                        NavigationTitle(
                            stringResource(R.string.iride_search_top_results), useIrideStyle = useIrideStyle,
                            showDivider = false, topPadding = if (header != null) 0.dp else null,
                        )
                        top?.items.orEmpty().forEach { item ->
                            key(item.id) {
                                ytItemContent(item, Modifier.padding(start = if (item is SongItem) 6.dp else 16.dp, end = 20.dp), item is SongItem)
                            }
                        }
                    }
                }
            }
            order.filter { it != FILTER_SONG }.forEach { sectionFilter ->
                val page = viewModel.viewStateMap[sectionFilter.value]
                val discovery = viewModel.songDiscovery
                val sectionItems = viewModel.sectionItems(sectionFilter)
                if (page != null && sectionItems.isEmpty()) return@forEach
                val titleRes = when {
                    sectionFilter == FILTER_ARTIST && discovery != null -> R.string.search_similar_artists
                    sectionFilter == FILTER_ALBUM && discovery != null -> R.string.search_albums_to_explore
                    sectionFilter == FILTER_COMMUNITY_PLAYLIST && discovery != null -> R.string.search_playlists_to_explore
                    else -> filterSectionTitleRes(sectionFilter)
                }
                item(key = "smart_section_${sectionFilter.value}") {
                    SearchResultsStyle {
                        IrideFolioSection(
                            sectionKey = "$revision:${sectionFilter.value}",
                            ready = sectionItems.isNotEmpty(), presentedSections = presentedSections,
                            artwork = sectionItems.take(4).map { it.thumbnail },
                            placeholder = { IrideFolioSectionSkeleton(if (sectionFilter == FILTER_ALBUM) 180.dp else 148.dp) },
                        ) {
                            NavigationTitle(stringResource(titleRes), useIrideStyle = useIrideStyle, showDivider = false)
                            key(viewModel.query, viewModel.searchRevision) { searchResultRow(sectionItems) }
                        }
                    }
                }
            }
            if (order.isNotEmpty() && order.all { viewModel.viewStateMap[it.value] != null } &&
                top == null && order.all { viewModel.sectionItems(it).isEmpty() }) {
                item { SearchResultsStyle { EmptyPlaceholder(R.drawable.search, stringResource(R.string.no_results_found)) } }
            }
        } else {
            item { IrideFolioSectionSkeleton(148.dp, songs = true, songRows = 5) }
        }
        if (header == null) item(key = "bottom_spacer") {
            Spacer(Modifier.height(MiniPlayerHeight + MiniPlayerBottomSpacing + NavigationBarHeight))
        }
    }

    CompositionLocalProvider(
        LocalFolioSectionFastReveal provides true,
        LocalFolioSectionEntranceOrder provides entranceOrder,
    ) {
    if (header != null && searchQuery.isNotBlank()) {
        IridePageLayout(
            titleRes = R.string.search,
            state = lazyListState,
            showTopBar = true,
            showTopScrim = true,
            contentTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                if (gestureNavigationEnabled()) IrideFolio.TitleTopSpacing else IrideFolio.TitleAfterControls,
            contentHorizontalPadding = 0.dp,
            bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            modifier = modifier,
            menuSections = pageMenuSections,
        ) {
            item(key = "search_header") { header() }
            resultsListContent()
        }
    } else if (header != null) {
        OnlineSearchScreen(
            query = submittedQuery, onQueryChange = onQueryChange, navController = navController,
            onSearch = onSearch, onDismiss = onDismissSuggestions, pureBlack = pureBlack,
            isFocused = isSearchFocused, header = header, pageMenuSections = pageMenuSections,
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
}
