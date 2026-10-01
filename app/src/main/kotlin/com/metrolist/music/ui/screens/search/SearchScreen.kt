/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalIsPlayerExpanded
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LibraryPageMenuAction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.viewmodels.OnlineSearchViewModel

@Composable
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean,
    savedStateHandle: SavedStateHandle,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isHandlingScrollToTop by remember { mutableStateOf(false) }
    val scrollToTopCount by savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsState(initial = 0)
    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true
            lazyListState.scrollToItem(0)
            withFrameNanos { }
            kotlinx.coroutines.delay(100)
            if (!isPlayerExpanded) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var isFocused by remember { mutableStateOf(false) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = false)

    var submittedQuery by rememberSaveable { mutableStateOf<String?>(null) }
    val onlineSearchResultViewModel: OnlineSearchViewModel = hiltViewModel()

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) return
        focusManager.clearFocus()
        isFocused = false
        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = parsedUrl.id)))
            }
            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                navController.navigate("online_playlist/${parsedUrl.id}")
            }
            is YouTubeUrlParser.ParsedUrl.Album -> {
                navController.navigate("album/MPREb_${parsedUrl.id}")
            }
            is YouTubeUrlParser.ParsedUrl.Artist -> {
                navController.navigate("artist/${parsedUrl.id}")
            }
            null -> {
                query = TextFieldValue(searchQuery, TextRange(searchQuery.length))
                submittedQuery = searchQuery
            }
        }
        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query { insert(SearchHistory(query = searchQuery)) }
            }
        }
    }

    LaunchedEffect(submittedQuery) {
        submittedQuery?.let(onlineSearchResultViewModel::search)
    }

    BackHandler(enabled = submittedQuery != null || isFocused) {
        if (submittedQuery != null) {
            submittedQuery = null
        }
        if (isFocused) {
            isFocused = false
            focusManager.clearFocus()
        }
    }

    val irideHeaderContent = remember {
        movableContentOf {
            SearchScrollableHeader(
                query = query,
                onQueryChange = {
                    query = it
                },
                searchSource = searchSource,
                focusRequester = focusRequester,
                onFocusChanged = { if (it.isFocused) isFocused = true },
                onSearch = { handleSearch(query.text) },
                onClear = { query = TextFieldValue(""); submittedQuery = null },
            )
        }
    }
    val irideHeader: (@Composable () -> Unit)? = irideHeaderContent

    val pageMenuSections = listOf(
        listOf(
            LibraryPageMenuAction(
                label = stringResource(R.string.online),
                selected = searchSource == SearchSource.ONLINE,
                onClick = {
                    searchSource = SearchSource.ONLINE
                    submittedQuery = query.text.trim().takeIf(String::isNotEmpty)
                },
            ),
            LibraryPageMenuAction(
                label = stringResource(R.string.filter_library),
                selected = searchSource == SearchSource.LOCAL,
                onClick = {
                    searchSource = SearchSource.LOCAL
                    submittedQuery = null
                },
            ),
        ),
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            when {
                pureBlack -> Color.Black
                mainTopGradient -> Color.Transparent
                else -> MaterialTheme.colorScheme.background
            },
        ),
    ) {
        if (searchSource == SearchSource.ONLINE) {
            OnlineSearchResultsBody(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = onlineSearchResultViewModel,
                pureBlack = pureBlack,
                useIrideStyle = true,
                isSearchFocused = isFocused,
                queryText = query.text,
                onQueryChange = {
                    query = it
                },
                onSearch = { handleSearch(it) },
                onDismissSuggestions = {
                    isFocused = false
                    focusManager.clearFocus()
                },
                header = irideHeader,
                pageMenuSections = pageMenuSections,
                lazyListState = lazyListState,
            )
        } else {
            LocalSearchScreen(
                query = query.text,
                navController = navController,
                onDismiss = { navController.navigateUp() },
                pureBlack = pureBlack,
                header = irideHeader,
                pageMenuSections = pageMenuSections,
                lazyListState = lazyListState,
            )
        }
    }

    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isHandlingScrollToTop) return@LifecycleEventObserver
                    if (isPlayerExpanded) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (isHandlingScrollToTop) return@LifecycleEventObserver
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Search controls embedded at the start of the scrollable list.
 */
@Composable
private fun SearchScrollableHeader(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchSource: SearchSource,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        IrideSearchBox(
            query = query,
            onQueryChange = onQueryChange,
            placeholderText = stringResource(
                when (searchSource) {
                    SearchSource.LOCAL -> R.string.search_library
                    SearchSource.ONLINE -> R.string.search_yt_music
                },
            ),
            focusRequester = focusRequester,
            onFocusChanged = onFocusChanged,
            onSearch = onSearch,
            onClear = onClear,
            showBorder = false,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}
