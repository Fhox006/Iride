/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.util.lerp as lerpFloat

private val LargeTitleHeightDp = 80.dp
private val SmallTitleBarHeightDp = 56.dp
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
        snapAnimationSpec = tween(durationMillis = 200),
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
    val totalHeightDp = SmallTitleBarHeightDp + LargeTitleHeightDp + SearchBoxHeightDp + 12.dp

    val indicatorOffset by animateDpAsState(
        targetValue = if (searchSource == SearchSource.ONLINE) 2.dp else 42.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "searchSourceIndicator",
    )

    Surface(
        color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(totalHeightDp + with(density) { scrollBehavior.state.heightOffset.toDp() }),
    ) {
        Box {
            // Single Title Transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallTitleBarHeightDp)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer {
                        // Move from Large position to Small position
                        // Expanded: below SmallTitleBar. Collapsed: at 0.
                        translationY = lerpFloat(with(density) { (LargeTitleHeightDp - 12.dp).toPx() }, 0f, fraction)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val pillAlpha = (1f - fraction * 2f).coerceIn(0f, 1f)
                val pillEnabled = fraction < 0.05f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val searchStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)

                    Text(
                        text = stringResource(R.string.search),
                        style = searchStyle,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val targetScale = 0.61f
                            val scale = lerpFloat(1f, targetScale, fraction)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            },

                    )
                    Box(
                        modifier = Modifier
                            .alpha(pillAlpha)
                            .padding(bottom = 4.dp)
                            .width(80.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset, y = 2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(
                                        enabled = pillEnabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { if (searchSource != SearchSource.ONLINE) onSearchSourceToggle() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.language),
                                    contentDescription = null,
                                    tint = if (searchSource == SearchSource.ONLINE)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(
                                        enabled = pillEnabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { if (searchSource != SearchSource.LOCAL) onSearchSourceToggle() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bookmark_outlined),
                                    contentDescription = null,
                                    tint = if (searchSource == SearchSource.LOCAL)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Search Box Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { 
                        val startOffset = with(density) { (SmallTitleBarHeightDp + LargeTitleHeightDp).toPx() }
                        translationY = startOffset + scrollBehavior.state.heightOffset
                    }
                    .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 12.dp)
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
                }
            }
        }
    }
}
