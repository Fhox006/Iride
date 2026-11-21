/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.search

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalIsPlayerExpanded
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.R
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.navigateSearchSection
import com.metrolist.music.utils.recordSearchHistoryOpen
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlineSearchViewModel
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean,
    savedStateHandle: SavedStateHandle,
    initialQuery: String = "",
    onlineSearchResultViewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val searchSuggestionsViewModel: OnlineSearchSuggestionViewModel =
        hiltViewModel(key = "searchSceneSuggestions")
    val searchSuggestions by searchSuggestionsViewModel.viewState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var keyboardClosing by remember { mutableStateOf(false) }
    var keyboardRequestRevision by remember { mutableIntStateOf(-1) }
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isHandlingScrollToTop by remember { mutableStateOf(false) }
    var searchSession by rememberSaveable(
        stateSaver = Saver<SearchSessionState, String>(save = { it.restoredQuery }, restore = { SearchSessionState(it) }),
    ) {
        mutableStateOf(SearchSessionState(initialQuery))
    }
    val submittedQuery = searchSession.submittedQuery
    val motionEvent = searchSession.event
    val isFocused = motionEvent.mode == SearchSceneMode.Editing
    val scrollToTopCount by
        savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsState(initial = 0)
    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var savedQuery by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(initialQuery, TextRange(initialQuery.length)))
        }
    val textState = remember { TextFieldState(savedQuery.text, savedQuery.selection) }
    val query = TextFieldValue(textState.text.toString(), textState.selection)
    fun setQuery(value: TextFieldValue) {
        savedQuery = value
        textState.edit {
            if (toString() != value.text) replace(0, length, value.text)
            selection = value.selection
        }
    }
    LaunchedEffect(textState) {
        snapshotFlow { TextFieldValue(textState.text.toString(), textState.selection) }.collect { value ->
            savedQuery = value
        }
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) searchSource = SearchSource.ONLINE
        if (!isFocused) setQuery(TextFieldValue(submittedQuery))
    }

    LaunchedEffect(isFocused, motionEvent.revision, isPlayerExpanded) {
        keyboardRequestRevision = -1
        if (isFocused && !isPlayerExpanded) {
            val requestRevision = motionEvent.revision
            // A new show must not race the previous hide. The visual lift starts immediately;
            // focus waits for the old IME session, rather than interpreting its close as Back.
            if (keyboardClosing) {
                snapshotFlow { ime.getBottom(density) }.first { it == 0 }
                keyboardClosing = false
            }
            if (searchSession.event.revision != requestRevision ||
                searchSession.event.mode != SearchSceneMode.Editing) return@LaunchedEffect
            keyboardRequestRevision = requestRevision
            withFrameNanos {}
            if (searchSession.event.revision != requestRevision ||
                searchSession.event.mode != SearchSceneMode.Editing) return@LaunchedEffect
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun hideKeyboard() {
        keyboardRequestRevision = -1
        keyboardClosing = ime.getBottom(density) > 0
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun dismissEditor() {
        if (searchSession.event.mode != SearchSceneMode.Editing) return
        searchSession = searchSession.dismiss()
        setQuery(TextFieldValue(searchSession.submittedQuery, TextRange(searchSession.submittedQuery.length)))
        hideKeyboard()
    }

    fun openEditor() {
        if (isPlayerExpanded || searchSession.event.mode == SearchSceneMode.Editing) return
        if (searchSession.event.returnsToRoot) {
            setQuery(TextFieldValue())
            onlineSearchResultViewModel.search("")
        }
        val currentText = textState.text.toString()
        setQuery(TextFieldValue(currentText, TextRange(currentText.length)))
        searchSession = searchSession.open()
    }

    val latestOpenEditor by rememberUpdatedState(::openEditor)
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount <= lastHandledCount) return@LaunchedEffect
        lastHandledCount = scrollToTopCount
        isHandlingScrollToTop = true
        try {
            lazyListState.scrollToItem(0)
            withFrameNanos {}
            latestOpenEditor()
        } finally {
            isHandlingScrollToTop = false
        }
    }

    fun openSection(route: String) {
        hideKeyboard()
        if (isFocused) dismissEditor()
        navController.navigateSearchSection(route, simpleMotion = false)
    }

    fun handleSearch(rawQuery: String, selectedSong: SongItem? = null) {
        val searchQuery = rawQuery.trim()
        if (searchQuery.isEmpty()) {
            if (searchSession.event.returnsToRoot) return
            hideKeyboard()
            searchSession = searchSession.reset()
            // Keep the outgoing result and cached artwork alive until the reverse motion ends.
            coroutineScope.launch { lazyListState.scrollToItem(0) }
            return
        }
        val parsedUrl = YouTubeUrlParser.parse(searchQuery)
        if (parsedUrl == null) {
            searchSession = searchSession.submit(searchQuery)
            hideKeyboard()
        } else {
            dismissEditor()
        }
        when (parsedUrl) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = parsedUrl.id)))
            }
            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                openSection("online_playlist/${parsedUrl.id}")
            }
            is YouTubeUrlParser.ParsedUrl.Album -> {
                openSection("album/MPREb_${parsedUrl.id}")
            }
            is YouTubeUrlParser.ParsedUrl.Artist -> {
                openSection("artist/${parsedUrl.id}")
            }
            null -> {
                if (searchSource == SearchSource.ONLINE) {
                    val suggestionsMatchDraft = searchSuggestions.query.trim().equals(rawQuery.trim(), ignoreCase = true)
                    val pinnedSuggestions = if (suggestionsMatchDraft && selectedSong == null) {
                        searchSuggestions.recommendedItems.take(5)
                    } else emptyList()
                    onlineSearchResultViewModel.search(
                        searchQuery,
                        resetCarousels = true,
                        pinnedSuggestions = (listOfNotNull(selectedSong) + pinnedSuggestions).distinctBy { it.id }.take(5),
                        selectedSong = selectedSong,
                    )
                    onlineSearchResultViewModel.loadSmartSearch()
                }
                coroutineScope.launch { lazyListState.scrollToItem(0) }
            }
        }
    }

    fun selectSong(song: SongItem) {
        val title = com.metrolist.music.discovery.SearchDiscovery.cleanTitle(song.title)
        if (onlineSearchResultViewModel.query == title && onlineSearchResultViewModel.songDiscovery?.seed?.id == song.id) return
        setQuery(TextFieldValue(title, TextRange(title.length)))
        handleSearch(title, song)
    }

    val playerSearchQuery by savedStateHandle.getStateFlow<String?>("playerSearchQuery", null).collectAsState()
    LaunchedEffect(playerSearchQuery) {
        playerSearchQuery?.takeIf { it.isNotBlank() }?.let { requestedQuery ->
            searchSource = SearchSource.ONLINE
            setQuery(TextFieldValue(requestedQuery, TextRange(requestedQuery.length)))
            handleSearch(requestedQuery)
            lazyListState.scrollToItem(0)
            savedStateHandle["playerSearchQuery"] = null
        }
    }

    PredictiveBackHandler(enabled = !isPlayerExpanded && (isFocused || submittedQuery.isNotBlank())) { progress ->
        val start = searchSession.event
        // Commit only after the gesture completes. Cancellation leaves the draft, artwork and
        // active motion intact; an IME close or a new submission invalidates this gesture.
        progress.collect {}
        when (searchBackAction(start, searchSession.event)) {
            SearchBackAction.DismissEditor -> dismissEditor()
            SearchBackAction.ReturnToRoot -> handleSearch("")
            SearchBackAction.None -> Unit
        }
    }

    fun acceptCompletion(suggested: String, submit: Boolean) {
        if (!isFocused) openEditor()
        setQuery(TextFieldValue(suggested, TextRange(suggested.length)))
        if (submit) handleSearch(suggested)
    }

    val pageMenuSections =
        listOf(
            listOf(
                LibraryPageMenuAction(
                    label = stringResource(R.string.online),
                    icon = R.drawable.language,
                    selected = searchSource == SearchSource.ONLINE,
                    onClick = {
                        searchSource = SearchSource.ONLINE
                    },
                ),
                LibraryPageMenuAction(
                    label = stringResource(R.string.filter_library),
                    icon = if (searchSource == SearchSource.LOCAL) R.drawable.bookmark_filled else R.drawable.bookmark_outlined,
                    selected = searchSource == SearchSource.LOCAL,
                    onClick = {
                        searchSource = SearchSource.LOCAL
                    },
                ),
            ),
        )
    val landingColor = when {
        mainTopGradient -> Color.Transparent
        else -> MaterialTheme.colorScheme.background
    }
    val landingInk = MaterialTheme.colorScheme.textPrimary
    val landingSecondary = MaterialTheme.colorScheme.textSecondary
        val searchBack: (@Composable () -> Unit)? = if (submittedQuery.isBlank() || gestureNavigationEnabled()) null else ({
            Box(
                Modifier.size(48.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    role = Role.Button, onClick = { handleSearch("") },
                ), contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back),
                    tint = Color.White)
            }
        })
        SearchScene(
            query = query,
            submittedQuery = submittedQuery,
            event = motionEvent,
            keyboardRequestRevision = keyboardRequestRevision,
            onMotionComplete = { completedRevision ->
                val completed = searchSession.finish(completedRevision)
                if (completed != searchSession) {
                    searchSession = completed
                    setQuery(TextFieldValue())
                    onlineSearchResultViewModel.search("")
                }
            },
            landingColor = landingColor,
            landingInk = landingInk,
            landingSecondary = landingSecondary,
            resultsReady = searchSource != SearchSource.ONLINE ||
                (onlineSearchResultViewModel.query == submittedQuery &&
                    (onlineSearchResultViewModel.summaryPage != null || onlineSearchResultViewModel.viewStateMap.values.any { it != null })),
            textState = textState,
            searchSource = searchSource,
            focusRequester = focusRequester,
            listState = lazyListState,
            onOpen = ::openEditor,
            onDismiss = ::dismissEditor,
            onSubmit = { handleSearch(it) },
            onComplete = ::acceptCompletion,
            onOpenArtist = { artistId -> openSection("artist/$artistId") },
            onOpenAlbum = { albumId -> openSection("album/$albumId") },
            onOpenItem = { item ->
                if (!pauseSearchHistory) database.recordSearchHistoryOpen(query.text, item)
                when (item) {
                    is ArtistItem -> openSection("artist/${item.id}")
                    is AlbumItem -> openSection("album/${item.id}")
                    is PlaylistItem -> openSection("online_playlist/${item.id}")
                    is SongItem -> {
                        selectSong(item)
                        if (playerConnection?.mediaMetadata?.value?.id == item.id) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection?.playQueue(
                                YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()),
                            )
                        }
                    }
                    else -> Unit
                }
            },
            navController = navController,
            navigationIcon = searchBack,
            landingContent = { landingHeader ->
                if (searchSource == SearchSource.ONLINE) {
                    OnlineSearchScreen(
                        query = "", onQueryChange = ::setQuery, navController = navController,
                        onSearch = { handleSearch(it) }, onDismiss = ::dismissEditor, pureBlack = pureBlack,
                        header = landingHeader, pageMenuSections = pageMenuSections,
                    )
                } else {
                    LocalSearchScreen(
                        query = "", navController = navController, onDismiss = { navController.navigateUp() },
                        pureBlack = pureBlack, header = landingHeader, pageMenuSections = pageMenuSections,
                    )
                }
            },
        ) { irideHeader ->
            if (searchSource == SearchSource.ONLINE) {
                OnlineSearchResultsBody(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    viewModel = onlineSearchResultViewModel,
                    pureBlack = pureBlack,
                    useIrideStyle = true,
                    isSearchFocused = false,
                    queryText = query.text,
                    submittedQuery = submittedQuery,
                    onQueryChange = { setQuery(it) },
                    onSearch = { handleSearch(it) },
                    onDismissSuggestions = {
                        dismissEditor()
                    },
                    header = irideHeader,
                    pageMenuSections = pageMenuSections,
                    lazyListState = lazyListState,
                )
            } else {
                LocalSearchScreen(
                    query = submittedQuery,
                    navController = navController,
                    onDismiss = { navController.navigateUp() },
                    pureBlack = pureBlack,
                    header = irideHeader,
                    pageMenuSections = pageMenuSections,
                    lazyListState = lazyListState,
                )
            }
        }

    val latestDismissEditor by rememberUpdatedState(::dismissEditor)
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isHandlingScrollToTop) return@LifecycleEventObserver
                    if (isPlayerExpanded) {
                        latestDismissEditor()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (isHandlingScrollToTop) return@LifecycleEventObserver
                    latestDismissEditor()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (isPlayerExpanded) {
            latestDismissEditor()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
