/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

private val LargeTitleHeightDp = 72.dp
private val SmallTitleBarHeightDp = 52.dp
private val SearchBoxHeightDp = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lazyListState = rememberLazyListState()
    var isHandlingScrollToTop by remember { mutableStateOf(false) }

    val scrollToTopCount by savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsState(initial = 0)
    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true
            kotlinx.coroutines.delay(100)
            if (!isPlayerExpanded) {
                focusManager.clearFocus(force = true)
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

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) return
        focusManager.clearFocus()
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
                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
            }
        }
        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query { insert(SearchHistory(query = searchQuery)) }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Scaffold(
        topBar = {
            SearchCollapsingHeader(
                scrollBehavior = scrollBehavior,
                query = query,
                onQueryChange = { query = it },
                searchSource = searchSource,
                onSearchSourceToggle = {
                    searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                },
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused },
                onSearch = { handleSearch(query.text) },
                onClear = { query = TextFieldValue("") },
                pureBlack = pureBlack,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background),
        ) {
            when (searchSource) {
                SearchSource.LOCAL -> {
                    LocalSearchScreen(
                        query = query.text,
                        navController = navController,
                        onDismiss = { navController.navigateUp() },
                        pureBlack = pureBlack,
                    )
                }
                SearchSource.ONLINE -> {
                    OnlineSearchScreen(
                        query = query.text,
                        onQueryChange = { query = it },
                        navController = navController,
                        onSearch = { handleSearch(it) },
                        onDismiss = { /* stay on page */ },
                        pureBlack = pureBlack,
                        isFocused = isFocused,
                    )
                }
            }

            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.mic,
                onClick = { navController.navigate("recognition") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchCollapsingHeader(
    scrollBehavior: TopAppBarScrollBehavior,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchSource: SearchSource,
    onSearchSourceToggle: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    pureBlack: Boolean,
) {
    val density = LocalDensity.current
    val largeTitleHeightPx = with(density) { LargeTitleHeightDp.toPx() }

    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    val fraction = scrollBehavior.state.collapsedFraction
    val largeTitleCurrentHeight = with(density) {
        (largeTitleHeightPx + scrollBehavior.state.heightOffset).toDp().coerceAtLeast(0.dp)
    }

    Surface(
        color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Small title — fixed height, fades in on scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallTitleBarHeightDp)
                    .alpha(fraction)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.search),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Large title — collapses on scroll
            if (largeTitleCurrentHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(largeTitleCurrentHeight)
                        .alpha(1f - fraction)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = stringResource(R.string.search),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            }

            // Rounded search box — always visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, top = 8.dp, bottom = 12.dp)
                    .height(SearchBoxHeightDp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged(onFocusChanged),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.text.isEmpty()) {
                                Text(
                                    text = stringResource(
                                        when (searchSource) {
                                            SearchSource.LOCAL -> R.string.search_library
                                            SearchSource.ONLINE -> R.string.search_yt_music
                                        },
                                    ),
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )

                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    FilledTonalIconButton(onClick = onSearchSourceToggle) {
                        Icon(
                            painter = painterResource(
                                when (searchSource) {
                                    SearchSource.LOCAL -> R.drawable.library_music
                                    SearchSource.ONLINE -> R.drawable.language
                                },
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
