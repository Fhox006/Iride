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
import androidx.compose.runtime.movableContentOf
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
import androidx.compose.ui.text.TextRange
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
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.viewmodels.OnlineSearchViewModel

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
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var isFocused by remember { mutableStateOf(false) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

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

    val topNavBarController = com.metrolist.music.LocalTopNavBarController.current
    val irideHeaderContent = remember {
        movableContentOf {
            val controller = com.metrolist.music.LocalTopNavBarController.current
            SearchScrollableHeader(
                navigationItems = controller?.navigationItems ?: emptyList(),
                currentRoute = controller?.currentRoute,
                onItemClick = controller?.onItemClick ?: { _, _ -> },
                compact = controller?.compact ?: false,
                accountImageUrl = controller?.accountImageUrl,
                query = query,
                onQueryChange = { query = it },
                searchSource = searchSource,
                onSearchSourceToggle = {
                    searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                },
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused },
                onSearch = { handleSearch(query.text) },
                onClear = { query = TextFieldValue(""); submittedQuery = null },
                pureBlack = pureBlack,
                transparentBackground = mainTopGradient,
            )
        }
    }
    val irideHeader: (@Composable () -> Unit)? = irideHeaderContent

    Scaffold(
        topBar = {},
        modifier = Modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    when {
                        pureBlack -> Color.Black
                        mainTopGradient -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    },
                ),
        ) {
            val showInlineResults = searchSource == SearchSource.ONLINE && submittedQuery != null
            if (showInlineResults) {
                OnlineSearchResultsBody(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    viewModel = onlineSearchResultViewModel,
                    pureBlack = pureBlack,
                    useIrideStyle = true,
                    isSearchFocused = isFocused,
                    queryText = query.text,
                    onQueryChange = { query = it },
                    onSearch = { handleSearch(it) },
                    onDismissSuggestions = {
                        isFocused = false
                        focusManager.clearFocus()
                    },
                    header = irideHeader,
                )
            } else {
                when (searchSource) {
                    SearchSource.LOCAL -> {
                        LocalSearchScreen(
                            query = query.text,
                            navController = navController,
                            onDismiss = { navController.navigateUp() },
                            pureBlack = pureBlack,
                            header = irideHeader,
                        )
                    }
                    SearchSource.ONLINE -> {
                        OnlineSearchScreen(
                            query = query.text,
                            onQueryChange = { query = it },
                            navController = navController,
                            onSearch = { handleSearch(it) },
                            onDismiss = {  },
                            pureBlack = pureBlack,
                            isFocused = isFocused,
                            header = irideHeader,
                        )
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.mic,
                    onClick = { navController.navigate("recognition") },
                    useIrideStyle = true,
                )
            }
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
 * New Iride UI: flat, non-collapsing replacement for [SearchCollapsingHeader] — TopNavigationBar,
 * the online/library source toggle and the search box, all fully visible at all times (no
 * scroll-driven animation), meant to be embedded as the leading item of the scrollable list
 * instead of pinned in a Scaffold topBar. See SearchScreen's `irideHeader`.
 */
@Composable
private fun SearchScrollableHeader(
    navigationItems: List<com.metrolist.music.ui.screens.Screens>,
    currentRoute: String?,
    onItemClick: (com.metrolist.music.ui.screens.Screens, Boolean) -> Unit,
    compact: Boolean,
    accountImageUrl: String?,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchSource: SearchSource,
    onSearchSourceToggle: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    pureBlack: Boolean,
    transparentBackground: Boolean,
) {
    Column {
        TopNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            containerColor = Color.Transparent,
            compact = compact,
            accountImageUrl = accountImageUrl,
        )

        Box(modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)) {
            IrideSegmentedToggle(
                options = listOf(
                    SearchSource.ONLINE to stringResource(R.string.online),
                    SearchSource.LOCAL to stringResource(R.string.filter_library),
                ),
                selected = searchSource,
                onSelect = { value -> if (value != searchSource) onSearchSourceToggle() },
            )
        }

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
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
    }
}

/**
 * New Iride UI: shared flat monospace/white-alpha search box, matching the look of
 * [com.metrolist.music.ui.component.ChipsRow] / [com.metrolist.music.ui.component.NavigationTitle]
 * / [SuggestionItem] instead of default Material text field styling. Reused by both this screen's
 * own header and [OnlineSearchResult]'s search bar so the two look identical.
 */
@Composable
fun IrideSearchBox(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    placeholderText: String,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBoxHeightDp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.06f)),
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
                    color = Color.White.copy(alpha = 0.95f),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.text.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 15.sp,
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
                        tint = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
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
    transparentBackground: Boolean = false,
    hideTitle: Boolean = false,
) {
    val density = LocalDensity.current
    val largeTitleHeightPx = if (hideTitle) 0f else with(density) { LargeTitleHeightDp.toPx() }

    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    val fraction = if (hideTitle) 0f else scrollBehavior.state.collapsedFraction
    val totalHeightDp = SmallTitleBarHeightDp + (if (hideTitle) 0.dp else LargeTitleHeightDp) + SearchBoxHeightDp + 12.dp

    val indicatorOffset by animateDpAsState(
        targetValue = if (searchSource == SearchSource.ONLINE) 2.dp else 42.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "searchSourceIndicator",
    )

    Surface(
        color = when {
            transparentBackground -> Color.Transparent
            pureBlack -> Color.Black
            else -> MaterialTheme.colorScheme.background
        },
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(totalHeightDp + with(density) { scrollBehavior.state.heightOffset.toDp() }),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallTitleBarHeightDp)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer {
                        translationY = if (hideTitle) 0f else lerpFloat(with(density) { (LargeTitleHeightDp - 12.dp).toPx() }, 0f, fraction)
                    },
                contentAlignment = if (hideTitle) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                val pillAlpha = if (hideTitle) 1f else (1f - fraction * 2f).coerceIn(0f, 1f)
                val pillEnabled = hideTitle || fraction < 0.05f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val searchStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)

                    if (!hideTitle) {
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
                    }
                    if (hideTitle) {
                        Box(modifier = Modifier.alpha(pillAlpha).padding(bottom = 4.dp)) {
                            IrideSegmentedToggle(
                                options = listOf(
                                    SearchSource.ONLINE to stringResource(R.string.online),
                                    SearchSource.LOCAL to stringResource(R.string.filter_library),
                                ),
                                selected = searchSource,
                                enabled = pillEnabled,
                                onSelect = { value -> if (value != searchSource) onSearchSourceToggle() },
                            )
                        }
                    } else {
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
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val startOffset = with(density) {
                            (SmallTitleBarHeightDp + (if (hideTitle) 0.dp else LargeTitleHeightDp)).toPx()
                        }
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
