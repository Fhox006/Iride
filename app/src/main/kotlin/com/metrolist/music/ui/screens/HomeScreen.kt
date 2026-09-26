/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.fillSelected
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.ui.utils.resize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.isMixtape
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedAnimationKey

import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.SmartBootKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.currentGridThumbnailHeight
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.IridePageTitle
import com.metrolist.music.ui.component.FrostedPageTitleBar
import com.metrolist.music.ui.component.HomeFilmViewfinderOverlay
import com.metrolist.music.ui.component.homeGlobeField
import com.metrolist.music.ui.component.rememberHomeScrollVelocity
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongCarousel
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import androidx.compose.ui.draw.clipToBounds
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.SyncStatus
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import com.metrolist.music.LocalDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.ForYouShelfTile
import com.metrolist.music.utils.SyncState
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.rememberSectionEnter

/**
 * New Iride UI: horizontally scrollable chip row for the Mood category selector. Reuses the same
 * "single shared indicator glides between labels" technique as IrideSegmentedToggle (the
 * Library/Downloaded switch in LibraryMixScreen.kt, see ui/component/ChipsRow.kt) — the underline
 * slides and resizes smoothly from one chip to the next instead of each chip independently
 * popping its own static underline on/off, which read as an un-animated flicker when switching
 * mood categories. Kept local to Home instead of changing the shared ChipsRow composable itself,
 * since that component is also used (in its plain, non-glide form) by several other screens.
 */
@Composable
private fun <E> IrideMoodChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E?,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
) {
    val density = LocalDensity.current
    val labelBoundsPx = remember { androidx.compose.runtime.mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = chips.indexOfFirst { it.first == currentValue }
    val targetBounds = if (selectedIndex >= 0) labelBoundsPx[selectedIndex] else null
    val indicatorAnimSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val indicatorX by animateFloatAsState(targetBounds?.first ?: 0f, indicatorAnimSpec, label = "moodChipIndicatorX")
    val indicatorWidth by animateFloatAsState(targetBounds?.second ?: 0f, indicatorAnimSpec, label = "moodChipIndicatorWidth")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(horizontalPadding))
            chips.forEachIndexed { index, (value, label) ->
                val isSelected = currentValue == value
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.textTertiary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onValueUpdate(value) }
                        .padding(vertical = 6.dp)
                        .onGloballyPositioned { coords ->
                            labelBoundsPx[index] = coords.positionInParent().x to coords.size.width.toFloat()
                        },
                )
                if (index != chips.lastIndex) Spacer(Modifier.width(20.dp))
            }
            Spacer(Modifier.width(horizontalPadding))
        }
        Spacer(Modifier.height(3.dp))
        if (targetBounds != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX.roundToInt(), 0) }
                    .width(with(density) { indicatorWidth.toDp() })
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.textPrimary),
            )
        } else {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun QuickPicksSkeleton(contentPadding: Dp, modifier: Modifier = Modifier) {
    ShimmerHost(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding)
                .height(ListItemHeight * 4)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
private fun ForYouShelfSkeleton(boxSize: Dp, contentPadding: Dp, modifier: Modifier = Modifier) {
    ShimmerHost(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .padding(start = contentPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(boxSize)
                        .height(boxSize + 30.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

@Composable
private fun HomeSimilarityTitle(
    name: String,
    thumbnailUrl: String,
    isArtist: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
) {
    val shape = if (isArtist) CircleShape else RoundedCornerShape(5.dp)
    NavigationTitle(
        label = stringResource(R.string.home_similar_to),
        title = name,
        thumbnail = {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .irideArtworkOverlayBorder(IrideBaseBorderWidth, MaterialTheme.colorScheme.strokeCard, shape)
                    .clip(shape),
            )
        },
        onClick = onClick,
        onRefreshClick = onRefreshClick,
        isRefreshing = isRefreshing,
        modifier = modifier,
        useIrideStyle = true,
        prominentTitle = true,
        showDivider = false,
        topPadding = 24.dp,
        bottomPadding = 12.dp,
        showNavigationArrow = false,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val radioUnavailableMessage = stringResource(R.string.home_radio_unavailable)

    val isPlaying by remember(playerConnection) {
        playerConnection?.isEffectivelyPlaying ?: MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val pinnedIds: Set<String> by remember(pinnedSpeedDialItems) {
        derivedStateOf { pinnedSpeedDialItems.map { it.id }.toSet() }
    }
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val regeneratingSections by viewModel.regeneratingSections.collectAsStateWithLifecycle()

    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forYouShelves by viewModel.forYouShelves.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val phase1Complete by viewModel.phase1Complete.collectAsStateWithLifecycle()
    val isHeroCarouselEnabled by viewModel.isHeroCarouselEnabled.collectAsStateWithLifecycle()
    val heroCarouselItems by viewModel.heroCarouselItems.collectAsStateWithLifecycle()
    val smartBootEnabled by rememberPreference(SmartBootKey, defaultValue = true)

    val accountNameFlow by viewModel.accountName.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncBannerLaunchCount by viewModel.syncBannerLaunchCount.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val accountNamePref by rememberPreference(AccountNameKey, "")
    val accountName = if (accountNameFlow != "Guest") accountNameFlow else accountNamePref

    val irideStart = 20.dp
    val irideListItemStart = 20.dp
    val irideGridItemStart = 20.dp
    val hideExplicit by rememberPreference(HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)
    val hideYoutubeShorts by rememberPreference(HideYoutubeShortsKey, defaultValue = false)
    val currentGridHeight = currentGridThumbnailHeight()

    // Declared up here: the globe state, the shelf counter and the item helpers below need them.
    val lazyListState = rememberLazyListState()
    val homeBarHeight = 40.dp
    val homeStatusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val homeBarBottomPx = with(LocalDensity.current) { (homeStatusBarHeight + homeBarHeight).toPx() }
    var homeTitleHeightPx by remember { mutableStateOf(0) }
    val compactTitleProgress by remember(lazyListState, homeBarBottomPx) {
        derivedStateOf {
            val layout = lazyListState.layoutInfo
            val titleItem = layout.visibleItemsInfo.firstOrNull { it.key == "home_page_title" }
            when {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0 -> 0f
                titleItem == null -> if (lazyListState.firstVisibleItemIndex > 0) 1f else 0f
                homeTitleHeightPx <= 0 -> 0f
                else -> {
                    val titleTop = titleItem.offset + layout.beforeContentPadding
                    ((homeBarBottomPx - titleTop) / homeTitleHeightPx).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Ball effect (Home only, default ON): master switch + reduced motion.
    // When off, the velocity tracker / shader never
    // start, so the cost is zero and the feed renders exactly like before.
    val advancedAnimation by rememberPreference(AdvancedAnimationKey, defaultValue = true)
    val homeReducedMotion = rememberReducedMotion()
    val globeOn = advancedAnimation && !homeReducedMotion
    val homeSignedVelocity = if (advancedAnimation) rememberHomeScrollVelocity(lazyListState) else 0f
    // Single animated driver: signed lean stretches the feed vertically on a
    // soft Apple-like spring; |lean| doubles as the motion-blur intensity
    // above the threshold. One animation total.
    val homeLean by animateFloatAsState(
        targetValue = if (globeOn) (homeSignedVelocity / 7000f).coerceIn(-1f, 1f) else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 180f),
        label = "homeLean",
    )
    val moodMixItems by viewModel.moodMixItems.collectAsStateWithLifecycle()
    val isMoodLoading by viewModel.isMoodLoading.collectAsStateWithLifecycle()
    var selectedMoodCategory by remember { mutableStateOf<com.metrolist.innertube.pages.HomePage.Chip?>(null) }
    val moodChips = remember(homePage?.chips) {
        homePage?.chips?.map { it to it.title } ?: emptyList()
    }
    val moodMixesState = rememberLazyListState()

    LaunchedEffect(moodChips) {
        if (moodChips.isNotEmpty() && moodChips.none { it.first == selectedMoodCategory }) {
            selectedMoodCategory = moodChips.first().first
        }
    }
    LaunchedEffect(selectedMoodCategory, hideExplicit, hideVideoSongs, hideYoutubeShorts) {
        moodMixesState.scrollToItem(0)
        if (selectedMoodCategory != null) {
            viewModel.loadMoodPage(
                selectedMoodCategory?.endpoint?.params,
                selectedMoodCategory?.title,
                hideExplicit, hideVideoSongs, hideYoutubeShorts,
            )
        }
    }

    val revealedSections = remember { IrideTabEntrance.sectionsFor("home") }
    val screenProgress = if (IrideTabEntrance.wasRevealed("home")) {
        1f
    } else {
        rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)
            .also { if (it >= 1f) IrideTabEntrance.markRevealed("home") }
    }

    @Composable
    fun LazyItemScope.homeTitleMotion(key: String): Modifier = Modifier
        .animateItem(placementSpec = IrideMotion.PlacementSpec)
        .revealMask(rememberSectionEnter(key, revealedSections))

    @Composable
    fun LazyItemScope.homeRowMotion(key: String, offsetY: Dp = 10.dp): Modifier = Modifier
        .irideEnter(rememberSectionEnter(key, revealedSections), offsetY)
        .animateItem(placementSpec = IrideMotion.PlacementSpec)

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onSectionBecameVisible("daily_discover")
        viewModel.onSectionBecameVisible("from_the_community")
        viewModel.onSectionBecameVisible("similar_recommendation_0")
    }

    val shuffledFeedSections = remember(homePage?.sections) {
        homePage?.sections.orEmpty().withIndex()
            .map { indexed ->
                IndexedValue(indexed.index, indexed.value.copy(items = indexed.value.items.shuffled()))
            }
            .shuffled()
            .sortedBy { indexed ->
                val title = indexed.value.title.lowercase()
                when {
                    title.contains("trending") -> 0
                    title.contains("new release") || title.contains("nuove uscite") -> 1
                    else -> 2
                }
            }
    }
    val pickedSongs = remember(quickPicks) {
        quickPicks.orEmpty().filterNot { it.song.isVideo || it.song.isEpisode }
            .distinctBy { it.id }
            .shuffled()
    }

    val homeColorScheme = MaterialTheme.colorScheme.let { colors ->
        colors.copy(
            primary = colors.onSurface,
            onPrimary = colors.surface,
            primaryContainer = colors.surfaceContainerHighest,
            onPrimaryContainer = colors.onSurface,
            inversePrimary = colors.onSurface,
            secondary = colors.onSurfaceVariant,
            onSecondary = colors.surface,
            secondaryContainer = colors.surfaceContainerHighest,
            onSecondaryContainer = colors.onSurface,
            tertiary = colors.onSurfaceVariant,
            onTertiary = colors.surface,
            tertiaryContainer = colors.surfaceContainerHighest,
            onTertiaryContainer = colors.onSurface,
        )
    }
    MaterialTheme(colorScheme = homeColorScheme) {
    Scaffold(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidthDp = maxWidth
            val horizontalLazyGridItemWidthFactor =
                if (containerWidthDp * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = containerWidthDp * horizontalLazyGridItemWidthFactor

            val localGridItem: @Composable (LocalItem) -> Unit = { item ->
                when (item) {
                    is Song -> SongGridItem(
                        song = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!isListenTogetherGuest) {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection?.togglePlayPause()
                                        } else {
                                            playerConnection?.startRadioForSong(item.toMediaMetadata())
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                    )
                    is Album -> AlbumGridItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        coroutineScope = scope,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                    is Artist -> ArtistGridItem(
                        artist = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("artist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = item,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                    is Playlist -> {}
                }
            }

            val ytGridItem: @Composable (YTItem) -> Unit = { item ->
                YouTubeGridItem(
                    item = item,
                    isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    thumbnailRatio = 1f,
                    size = currentGridHeight,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection?.playQueue(
                                            YouTubeQueue(
                                                item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata(),
                                            ),
                                        )
                                    }
                                }
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                is EpisodeItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection?.playQueue(
                                            ListQueue(
                                                title = item.title,
                                                items = listOf(item.toMediaMetadata().toMediaItem()),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is PodcastItem -> YouTubePlaylistMenu(
                                        playlist = item.asPlaylistItem(),
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is EpisodeItem -> YouTubeSongMenu(
                                        song = item.asSongItem(),
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        },
                    ),
                )
            }

            val forYouBoxScale = 1.35f
            val forYouCellGap = 3.dp * forYouBoxScale
            val forYouBoxSize = currentGridHeight * forYouBoxScale
            val forYouCellSize = (forYouBoxSize - forYouCellGap) / 2
            val forYouOnClick: (LocalItem) -> Unit = { cell ->
                when (cell) {
                    is Song -> {
                        if (!isListenTogetherGuest) {
                            if (cell.id == mediaMetadata?.id) {
                                playerConnection?.togglePlayPause()
                            } else {
                                playerConnection?.startRadioForSong(cell.toMediaMetadata())
                            }
                        }
                    }
                    is Album -> navController.navigate("album/${cell.id}")
                    is Artist -> navController.navigate("artist/${cell.id}")
                    is Playlist -> {}
                }
            }
            val forYouMiniTile: @Composable (LocalItem) -> Unit = { cell ->
                val tileShape = if (cell is Artist) CircleShape else RoundedCornerShape(6.dp)
                // These tiles render at roughly half the real grid size, so the outline
                // uses thinner weights than standard items to keep the same visual weight.
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cell.thumbnailUrl?.resize(300, 300))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = cell.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(forYouCellSize)
                        .irideArtworkOverlayBorder(
                            if (cell is Song) 0.5.dp else IrideSoftBorderWidth,
                            if (cell is Song) MaterialTheme.colorScheme.strokeItemSoft else MaterialTheme.colorScheme.strokeCard,
                            tileShape,
                        )
                        .clip(tileShape)
                        .clickable { forYouOnClick(cell) },
                )
            }
            val forYouRecommendedTile: @Composable (ForYouShelfTile) -> Unit = { tile ->
                when (tile) {
                    is ForYouShelfTile.Local -> forYouMiniTile(tile.item)
                    is ForYouShelfTile.RecommendedAlbum -> {
                        val album = tile.item
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.thumbnail?.resize(300, 300))
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(forYouCellSize)
                                .irideArtworkOverlayBorder(
                                    IrideSoftBorderWidth,
                                    MaterialTheme.colorScheme.strokeCard,
                                    RoundedCornerShape(6.dp),
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { navController.navigate("album/${album.id}") },
                        )
                    }
                }
            }
            val forYouBlock: @Composable (ForYouShelfItem) -> Unit = { shelf ->
                val cells: List<Any> = remember(shelf) { (listOf(shelf.artist) + shelf.tiles).shuffled().take(4) }
                Column(modifier = Modifier.width(forYouBoxSize)) {
                    Column(verticalArrangement = Arrangement.spacedBy(forYouCellGap)) {
                        cells.chunked(2).forEach { rowCells ->
                            Row(horizontalArrangement = Arrangement.spacedBy(forYouCellGap)) {
                                rowCells.forEach { cell ->
                                    when (cell) {
                                        is Artist -> forYouMiniTile(cell)
                                        is ForYouShelfTile -> forYouRecommendedTile(cell)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shelf.artist.artist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val forYouLoading = !phase1Complete && forYouShelves.isEmpty()
            val forYouSection: LazyListScope.() -> Unit = {
                if (forYouLoading || forYouShelves.size >= 4) {
                    item(key = "for_you_title") {
                        NavigationTitle(
                            title = stringResource(R.string.for_you_artists_title),
                            modifier = homeTitleMotion("for_you"),
                            onRefreshClick = { viewModel.regenerateForYouShelves() },
                            isRefreshing = "for_you_shelf" in regeneratingSections,
                            useIrideStyle = true,
                            showDivider = false,
                            topPadding = 24.dp,
                            bottomPadding = 12.dp,
                            sentenceCaseTitle = true,
                            showNavigationArrow = false,
                        )
                    }
                    item(key = "for_you_row") {
                        if (forYouLoading) {
                            ForYouShelfSkeleton(
                                boxSize = forYouBoxSize,
                                contentPadding = irideGridItemStart,
                                modifier = homeRowMotion("for_you_carousel"),
                            )
                        } else {
                            val shelves = forYouShelves
                            val forYouState = rememberLazyListState()
                            LaunchedEffect(forYouState, shelves.size) {
                                snapshotFlow { forYouState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                    .collect { lastVisible ->
                                        if (lastVisible != null && lastVisible >= shelves.size - 3) {
                                            viewModel.loadMoreForYouShelves()
                                        }
                                    }
                            }
                            LazyRow(
                                state = forYouState,
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                overscrollEffect = null,
                                modifier = homeRowMotion("for_you_carousel")
                                    .rubberBandOverscroll(Orientation.Horizontal, forYouState)
                                    ) {
                                itemsIndexed(shelves, key = { index, shelf -> "for_you_${index}_${shelf.artist.id}" }) { _, shelf -> forYouBlock(shelf) }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = lazyListState,
                    overscrollEffect = null,
                    contentPadding = PaddingValues(
                        top = homeStatusBarHeight + homeBarHeight + 8.dp,
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .homeGlobeField(
                            lean = homeLean,
                            contentAlpha = screenProgress,
                            enabled = globeOn,
                        )
                        .rubberBandOverscroll(Orientation.Vertical, lazyListState)
                ) {
                item(key = "home_page_title") {
                    IridePageTitle(
                        titleRes = R.string.home,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                            .onSizeChanged { homeTitleHeightPx = it.height }
                            .drawWithContent {
                                // Cut the page title at the bar edge even while its background is translucent.
                                clipRect(top = size.height * compactTitleProgress) {
                                    this@drawWithContent.drawContent()
                                }
                            },
                        monospace = true,
                    )
                }

                if (isLoading) {
                    item(key = "loading_indicator") {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = MaterialTheme.colorScheme.textSecondary,
                            trackColor = MaterialTheme.colorScheme.fillSelected,
                        )
                    }
                }

                item(key = "sync_banner") {
                    AnimatedVisibility(
                        visible = isLoggedIn && syncState.overallStatus == SyncStatus.Syncing && syncBannerLaunchCount < 3,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SyncBanner(syncState = syncState, useIrideStyle = true)
                    }
                }

                if (isHeroCarouselEnabled) {
                    item(key = "hero_carousel") {
                        val radioItems = remember(heroCarouselItems) {
                            heroCarouselItems.filterIsInstance<HeroCarouselItem.ArtistRadio>()
                        }
                        if (radioItems.isNotEmpty()) {
                            val heroPagerState = rememberPagerState(pageCount = { radioItems.size })
                            HeroCarouselSection(
                                items = radioItems,
                                pagerState = heroPagerState,
                                onCoverError = viewModel::repairHeroRadioCover,
                                onRadioClick = { artistId ->
                                    scope.launch(Dispatchers.IO) {
                                        val endpoint = viewModel.fetchArtistRadioEndpoint(artistId)
                                        withContext(Dispatchers.Main) {
                                            if (endpoint != null) {
                                                playerConnection?.playQueue(YouTubeQueue(endpoint))
                                            } else {
                                                snackbarHostState.showSnackbar(radioUnavailableMessage)
                                            }
                                        }
                                    }
                                    Unit
                                },
                                modifier = homeRowMotion("hero_carousel"),
                            )
                        } else if (!phase1Complete) {
                            HeroCarouselSkeleton(
                                modifier = Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec),
                            )
                        }
                    }
                }

                val speedDialContent: @Composable LazyItemScope.() -> Unit = {
                        val items = speedDialItems
                        val targetItemSize = 160.dp
                        val columns = (containerWidthDp / targetItemSize).toInt().coerceAtLeast(3)
                        val rows = if (columns >= 6) 1 else if (columns >= 4) 2 else 2
                        val itemsPerPage = columns * rows
                        val peekPadding = 12.dp
                        val itemWidth = (containerWidthDp - peekPadding * 2) / columns

                        val realPageCount = (items.size + 1 + itemsPerPage - 1) / itemsPerPage
                        val virtualPageCount = if (realPageCount > 1) realPageCount * 1000 else 1
                        val initialPage = if (realPageCount > 1) realPageCount * 500 else 0
                        val pagerState = rememberPagerState(
                            initialPage = initialPage,
                            pageCount = { virtualPageCount },
                        )

                        Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = peekPadding),
                                pageSpacing = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemWidth * rows)
                                    ) { page ->
                                val realPage = if (realPageCount > 1) page % realPageCount else 0
                                val isFirstPage = realPage == 0
                                val centerIndex = if (rows >= 2 && columns >= 2) columns * 2 - 1 else itemsPerPage - 1

                                val pageStartIndex = if (isFirstPage) 0 else realPage * itemsPerPage - 1
                                val pageItems = items.drop(pageStartIndex).take(if (isFirstPage) itemsPerPage - 1 else itemsPerPage)

                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (row in 0 until rows) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (col in 0 until columns) {
                                                val itemIndex = row * columns + col
                                                val isRandomizeSlot = isFirstPage && itemIndex == centerIndex

                                                if (isRandomizeSlot) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(itemWidth)
                                                            .height(itemWidth)
                                                            .padding(4.dp),
                                                    ) {
                                                        RandomizeGridItem(
                                                            isLoading = isRandomizing,
                                                            onClick = {
                                                                if (isRandomizing) {
                                                                    randomizeJob?.cancel()
                                                                } else if (!isListenTogetherGuest) {
                                                                    randomizeJob = scope.launch {
                                                                        val randomItem = viewModel.getRandomItem()
                                                                        if (randomItem != null) {
                                                                            when (randomItem) {
                                                                                is SongItem -> playerConnection?.playQueue(
                                                                                    YouTubeQueue(
                                                                                        randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                                        randomItem.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                                is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                                is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                                is PlaylistItem -> navController.navigate("online_playlist/${randomItem.id}")
                                                                                is PodcastItem -> navController.navigate("online_podcast/${randomItem.id}")
                                                                                is EpisodeItem -> playerConnection?.playQueue(
                                                                                    ListQueue(
                                                                                        title = randomItem.title,
                                                                                        items = listOf(randomItem.toMediaMetadata().toMediaItem()),
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                                } else {
                                                    val actualItemIndex = if (isFirstPage && itemIndex > centerIndex) itemIndex - 1 else itemIndex
                                                    if (actualItemIndex < pageItems.size) {
                                                        val sdItem = pageItems[actualItemIndex]
                                                        val isPinned = sdItem.id in pinnedIds
                                                        Box(
                                                            modifier = Modifier
                                                                .width(itemWidth)
                                                                .height(itemWidth)
                                                                .padding(4.dp),
                                                        ) {
                                                            SpeedDialGridItem(
                                                                item = sdItem,
                                                                isPinned = isPinned,
                                                                isActive = sdItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                                isPlaying = isPlaying,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .combinedClickable(
                                                                        onClick = {
                                                                            when (sdItem) {
                                                                                is SongItem -> {
                                                                                    if (!isListenTogetherGuest) {
                                                                                        playerConnection?.playQueue(
                                                                                            YouTubeQueue(
                                                                                                sdItem.endpoint ?: WatchEndpoint(videoId = sdItem.id),
                                                                                                sdItem.toMediaMetadata(),
                                                                                            ),
                                                                                        )
                                                                                    }
                                                                                }
                                                                                is AlbumItem -> navController.navigate("album/${sdItem.id}")
                                                                                is ArtistItem -> navController.navigate("artist/${sdItem.id}")
                                                                                is PlaylistItem -> {
                                                                                    val rawType = pinnedSpeedDialItems.find { it.id == sdItem.id }?.type
                                                                                    if (rawType == "LOCAL_PLAYLIST") {
                                                                                        navController.navigate("local_playlist/${sdItem.id}")
                                                                                    } else {
                                                                                        navController.navigate("online_playlist/${sdItem.id}")
                                                                                    }
                                                                                }
                                                                                is PodcastItem -> navController.navigate("online_podcast/${sdItem.id}")
                                                                                is EpisodeItem -> {
                                                                                    if (!isListenTogetherGuest) {
                                                                                        playerConnection?.playQueue(
                                                                                            ListQueue(
                                                                                                title = sdItem.title,
                                                                                                items = listOf(sdItem.toMediaMetadata().toMediaItem()),
                                                                                            ),
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                        },
                                                                        onLongClick = {
                                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            menuState.show {
                                                                                when (sdItem) {
                                                                                    is SongItem -> YouTubeSongMenu(
                                                                                        song = sdItem,
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                                                        albumItem = sdItem,
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is ArtistItem -> YouTubeArtistMenu(
                                                                                        artist = sdItem,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                                                        playlist = sdItem,
                                                                                        coroutineScope = scope,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is PodcastItem -> YouTubePlaylistMenu(
                                                                                        playlist = sdItem.asPlaylistItem(),
                                                                                        coroutineScope = scope,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is EpisodeItem -> YouTubeSongMenu(
                                                                                        song = sdItem.asSongItem(),
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                }
                                                                            }
                                                                        },
                                                                    ),
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.width(itemWidth))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (realPageCount > 1) {
                                val currentRealPage by remember(realPageCount) {
                                    derivedStateOf { pagerState.currentPage % realPageCount }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    repeat(realPageCount) { index ->
                                        val isSelected = currentRealPage == index
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (isSelected) 5.dp else 4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.textPrimary
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                }

                if (quickPicks == null || pickedSongs.size >= 4) {
                    item(key = "picked_for_you_title") {
                        NavigationTitle(
                            title = stringResource(R.string.quick_picks_section_title),
                            modifier = homeTitleMotion("picked_for_you"),
                            onRefreshClick = { viewModel.regenerateQuickPicks() },
                            isRefreshing = "quick_picks" in regeneratingSections,
                            useIrideStyle = true,
                            sentenceCaseTitle = true,
                            showDivider = false,
                            topPadding = 24.dp,
                            bottomPadding = 12.dp,
                            showNavigationArrow = false,
                        )
                    }
                    item(key = "picked_for_you_row") {
                        if (quickPicks == null) {
                            QuickPicksSkeleton(
                                contentPadding = irideListItemStart,
                                modifier = homeRowMotion("picked_for_you_row"),
                            )
                        } else {
                            SongCarousel(
                                items = pickedSongs,
                                key = { "picked_${it.id}" },
                                contentPadding = PaddingValues(horizontal = irideListItemStart - 4.dp),
                                gridState = rememberLazyGridState(),
                                modifier = homeRowMotion("picked_for_you_row"),
                            ) { originalSong, itemWidth ->
                                val song by database.song(originalSong.id)
                                    .collectAsStateWithLifecycle(initialValue = originalSong)
                                val currentSong = song ?: originalSong
                                CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                                    SongListItem(
                                        song = currentSong,
                                        isActive = currentSong.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
                                        trailingContent = {
                                            IconButton(onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = currentSong,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = stringResource(R.string.more_options),
                                                )
                                            }
                                        },
                                        modifier = Modifier.width(itemWidth).combinedClickable(
                                            onClick = {
                                                if (!isListenTogetherGuest) {
                                                    if (currentSong.id == mediaMetadata?.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(
                                                            YouTubeQueue(
                                                                WatchEndpoint(videoId = currentSong.id),
                                                                currentSong.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = currentSong,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                val knownArtistIds = forYouShelves.map { it.artist.id }.toSet()
                val knownArtistNames = forYouShelves.map { it.artist.artist.name.lowercase() }.toSet()
                val addFeedSection: LazyListScope.(IndexedValue<com.metrolist.innertube.pages.HomePage.Section>) -> Unit = feedSection@{ indexed ->
                    val index = indexed.index
                    val section = indexed.value
                    val title = section.title.lowercase()
                    val label = section.label.orEmpty().lowercase()
                    val isSimilar = title.contains("similar to") || title.contains("simile a") ||
                        label.contains("similar to") || label.contains("simile a")
                    val isTrending = title.contains("trending")
                    val isNewRelease = title.contains("new release") || title.contains("nuove uscite")
                    if ((!isTrending && !isNewRelease && !isSimilar) ||
                        title.contains("video") || label.contains("video")
                    ) return@feedSection
                    if (title.contains("listen again") || label.contains("listen again") ||
                        title.contains("long listens") || title == accountName.lowercase() ||
                        title.contains("picked for you") || title.contains("picked 4u") ||
                        title.contains("quick picks") || label.contains("picked for you") ||
                        (label.isBlank() && title in knownArtistNames)
                    ) return@feedSection

                    val sectionItems = section.items.distinctBy { it.id }.filter { item ->
                        val isMusicItem = item is SongItem && !item.isVideoSong ||
                            item is AlbumItem || item is ArtistItem
                        isMusicItem && (!isNewRelease || item is AlbumItem &&
                            item.albumType?.lowercase() !in setOf("single", "singolo") &&
                            item.artists.orEmpty().any { artist ->
                                artist.id?.let { it in knownArtistIds } == true
                            })
                    }
                    if (label.isBlank() && sectionItems.any { item ->
                            when (item) {
                                is SongItem -> item.artists.any { it.name.equals(section.title, ignoreCase = true) }
                                is AlbumItem -> item.artists.orEmpty().any { it.name.equals(section.title, ignoreCase = true) }
                                else -> false
                            }
                        }
                    ) return@feedSection
                    val rawName = if (title.contains("similar to") || title.contains("simile a")) {
                        section.title.substringAfter(':', section.title)
                            .replace(Regex("^(?i)(similar to|simile a)\\s*"), "")
                            .trim()
                    } else section.title
                    val name = rawName.replace(Regex("^(?i)(artist|artista|album|track|brano):\\s*"), "").trim()
                    val displayItems = if (isSimilar) sectionItems.filterNot { item ->
                        when (item) {
                            is SongItem -> item.artists.any { it.name.equals(name, ignoreCase = true) }
                            is AlbumItem -> item.title.equals(name, ignoreCase = true) ||
                                item.artists.orEmpty().any { it.name.equals(name, ignoreCase = true) }
                            is ArtistItem -> item.title.equals(name, ignoreCase = true)
                            else -> false
                        }
                    } else sectionItems
                    if (displayItems.size < 4) return@feedSection
                    val matchedArtist = sectionItems.filterIsInstance<ArtistItem>()
                        .firstOrNull { it.title.equals(name, ignoreCase = true) }
                    val matchedAlbum = sectionItems.filterIsInstance<AlbumItem>()
                        .firstOrNull { it.title.equals(name, ignoreCase = true) }
                    val isAlbumSource = title.contains("album:") || label.contains("album") || matchedAlbum != null
                    val sourceThumbnail = section.thumbnail ?: matchedArtist?.thumbnail ?: matchedAlbum?.thumbnail
                    if (isSimilar && (name.isBlank() || sourceThumbnail.isNullOrBlank())) return@feedSection
                    val onSectionClick = section.endpoint?.let { endpoint ->
                        {
                            when {
                                endpoint.browseId == "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                endpoint.params != null -> navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                else -> navController.navigate("browse/${endpoint.browseId}")
                            }
                            Unit
                        }
                    }
                    item(key = "home_section_title_$index") {
                        if (isSimilar) {
                            HomeSimilarityTitle(
                                name = name,
                                thumbnailUrl = sourceThumbnail!!,
                                isArtist = !isAlbumSource,
                                modifier = homeTitleMotion("home_section_$index"),
                                onClick = onSectionClick,
                            )
                        } else {
                            NavigationTitle(
                                title = section.title,
                                label = section.label,
                                sentenceCaseTitle = true,
                                onClick = onSectionClick,
                                modifier = homeTitleMotion("home_section_$index"),
                                useIrideStyle = true,
                                showDivider = false,
                                topPadding = 24.dp,
                                bottomPadding = 12.dp,
                                showNavigationArrow = false,
                            )
                        }
                    }
                    item(key = "home_section_list_$index") {
                        val sectionItemsState = rememberLazyListState()
                        LazyRow(
                            state = sectionItemsState,
                            contentPadding = PaddingValues(horizontal = irideGridItemStart),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            overscrollEffect = null,
                            modifier = homeRowMotion("home_section_${index}_row")
                                .rubberBandOverscroll(Orientation.Horizontal, sectionItemsState),
                        ) {
                            items(displayItems, key = { "home_section_${index}_item_${it.id}" }) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
                if (moodChips.isNotEmpty() && (moodMixItems?.size ?: 4) >= 4) {
                    item(key = "your_mood_title") {
                        NavigationTitle(
                            title = stringResource(R.string.home_mood_youtube_music),
                            modifier = homeTitleMotion("your_mood"),
                            useIrideStyle = true,
                            showDivider = false,
                            topPadding = 24.dp,
                            bottomPadding = 12.dp,
                            sentenceCaseTitle = true,
                            showNavigationArrow = false,
                        )
                    }

                    item(key = "your_mood_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(homeRowMotion("your_mood_row")),
                        ) {
                        if (moodChips.isNotEmpty()) {
                            IrideMoodChipsRow(
                                chips = moodChips,
                                currentValue = selectedMoodCategory,
                                onValueUpdate = { selectedMoodCategory = it },
                                horizontalPadding = irideStart,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentGridHeight + 64.dp),
                        ) {
                            val mixItems = remember(moodMixItems) { moodMixItems?.shuffled() }
                            if (mixItems.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isMoodLoading || mixItems == null) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.textSecondary,
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.home_no_mood_playlists),
                                            color = MaterialTheme.colorScheme.textSecondary,
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    state = moodMixesState,
                                    contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    overscrollEffect = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 4.dp)
                                        .alpha(if (isMoodLoading) 0.4f else 1f)
                                        .rubberBandOverscroll(Orientation.Horizontal, moodMixesState),
                                ) {
                                    items(mixItems, key = { it.id }) { mix ->
                                        YouTubeGridItem(
                                            item = mix,
                                            isActive = mix.id == mediaMetadata?.album?.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = scope,
                                            thumbnailRatio = 1f,
                                            size = currentGridHeight,
                                            showTitle = true,
                                            modifier = Modifier
                                                .animateItem()
                                                .combinedClickable(
                                                    onClick = {
                                                        navController.navigate("online_playlist/${mix.id}")
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubePlaylistMenu(
                                                                playlist = mix,
                                                                coroutineScope = scope,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                ),
                                        )
                                    }
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isMoodLoading,
                                    modifier = Modifier.align(Alignment.Center),
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.textSecondary,
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                forYouSection()
                val feedSection: LazyListScope.() -> Unit = {
                    shuffledFeedSections.forEach { addFeedSection(it) }
                }

                val dailySection: LazyListScope.() -> Unit = {
                    dailyDiscover?.distinctBy { it.recommendation.id }?.takeIf { it.size >= 4 }?.let { discoverList ->
                        item(key = "daily_discover_title") {
                            val title = stringResource(R.string.your_daily_discover)
                            NavigationTitle(
                                title = title,
                                onRefreshClick = { viewModel.regenerateDailyDiscover() },
                                isRefreshing = "daily_discover" in regeneratingSections,
                                useIrideStyle = true,
                                showDivider = false,
                                topPadding = 24.dp,
                                bottomPadding = 12.dp,
                                sentenceCaseTitle = true,
                                showNavigationArrow = false,
                                modifier = homeTitleMotion("daily_discover"),
                            )
                        }
                        item(key = "daily_discover_content") {
                            val dailyDiscoverState = rememberLazyListState()
                            LazyRow(
                                state = dailyDiscoverState,
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                overscrollEffect = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(homeRowMotion("daily_discover_row"))
                                    .rubberBandOverscroll(Orientation.Horizontal, dailyDiscoverState)
                                    ) {
                                items(discoverList, key = { "daily_discover_${it.recommendation.id}" }) { ddItem ->
                                    IrideDailyDiscoverCard(
                                        dailyDiscover = ddItem,
                                        size = currentGridHeight,
                                        onClick = {
                                            if (!isListenTogetherGuest) {
                                                val song = ddItem.recommendation as? SongItem
                                                val meta = song?.toMediaMetadata()
                                                if (meta != null) playerConnection?.playQueue(
                                                    YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), meta)
                                                )
                                            }
                                        },
                                        navController = navController,
                                    )
                                }
                            }
                        }
                    }

                }

                val communitySection: LazyListScope.() -> Unit = {
                    communityPlaylists?.distinctBy { it.playlist.id }?.takeIf { it.size >= 4 }?.let { playlists ->
                        item(key = "community_playlists_title") {
                            NavigationTitle(
                                title = stringResource(R.string.from_the_community),
                                modifier = homeTitleMotion("community_playlists"),
                                onRefreshClick = { viewModel.regenerateCommunityPlaylists() },
                                isRefreshing = "community_playlists" in regeneratingSections,
                                useIrideStyle = true,
                                showDivider = false,
                                topPadding = 24.dp,
                                bottomPadding = 12.dp,
                                sentenceCaseTitle = true,
                                showNavigationArrow = false,
                            )
                        }
                        item(key = "community_playlists_content") {
                            val communityPlaylistsState = rememberLazyListState()
                            LazyRow(
                                state = communityPlaylistsState,
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                overscrollEffect = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(homeRowMotion("community_playlists_row"))
                                    .rubberBandOverscroll(Orientation.Horizontal, communityPlaylistsState)
                                    ) {
                                items(playlists, key = { "community_playlist_${it.playlist.id}" }) { cpItem ->
                                    PlaylistGridItem(
                                        playlist = Playlist(
                                            playlist = PlaylistEntity(
                                                id = cpItem.playlist.id.removePrefix("VL"),
                                                name = cpItem.playlist.title,
                                                browseId = cpItem.playlist.id.removePrefix("VL"),
                                                thumbnailUrl = cpItem.playlist.thumbnail,
                                            ),
                                            songCount = 0,
                                            songThumbnails = emptyList(),
                                        ),
                                        autoPlaylist = false,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${cpItem.playlist.id.removePrefix("VL")}")
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }

                }

                val similarSection: LazyListScope.() -> Unit = {
                    similarRecommendations?.forEachIndexed { index, rec ->
                    val similarItems = rec.items
                        .distinctBy { it.id }
                        .filter { item -> item is SongItem && !item.isVideoSong || item is AlbumItem || item is ArtistItem }
                        .sortedBy { item -> item is ArtistItem }
                    if (similarItems.size < 4 || similarItems.none { it !is ArtistItem } ||
                            rec.thumbnailUrl.isNullOrBlank()
                        ) return@forEachIndexed
                        item(key = "similar_to_title_$index") {
                            HomeSimilarityTitle(
                                name = rec.title.title,
                                thumbnailUrl = rec.thumbnailUrl!!,
                                isArtist = rec.title is Artist,
                                onClick = {
                                    when (rec.title) {
                                        is Song -> navController.navigate("album/${rec.title.album!!.id}")
                                        is Album -> navController.navigate("album/${rec.title.id}")
                                        is Artist -> navController.navigate("artist/${rec.title.id}")
                                        is Playlist -> {}
                                    }
                                },
                                onRefreshClick = { viewModel.regenerateSimilarRecommendations() },
                                isRefreshing = "similar_recommendations" in regeneratingSections,
                                modifier = homeTitleMotion("similar_to_$index"),
                            )
                        }
                        item(key = "similar_to_list_$index") {
                            val similarToState = rememberLazyListState()
                            LazyRow(
                                state = similarToState,
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                overscrollEffect = null,
                                    modifier = homeRowMotion("similar_to_row_$index")
                                        .rubberBandOverscroll(Orientation.Horizontal, similarToState)
                                        ) {
                                items(similarItems, key = { it.id }) { recItem -> ytGridItem(recItem) }
                            }
                        }
                    }

                }

                val accountSection: LazyListScope.() -> Unit = {
                    accountPlaylists?.takeIf { it.distinctBy { playlist -> playlist.id }.size >= 4 }?.let { playlists ->
                        item(key = "account_playlists_title") {
                            NavigationTitle(
                                title = stringResource(R.string.home_youtube_music_playlists),
                                onClick = { navController.navigate("account") },
                                modifier = homeTitleMotion("account_playlists"),
                                useIrideStyle = true,
                                sentenceCaseTitle = true,
                                showDivider = false,
                                topPadding = 24.dp,
                                bottomPadding = 12.dp,
                                showNavigationArrow = false,
                            )
                        }
                        item(key = "account_playlists_list") {
                            val accountPlaylistsState = rememberLazyListState()
                            val shuffledPlaylists = remember(playlists) { playlists.distinctBy { it.id }.shuffled() }
                            LazyRow(
                                state = accountPlaylistsState,
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                overscrollEffect = null,
                                modifier = homeRowMotion("account_playlists_row")
                                    .rubberBandOverscroll(Orientation.Horizontal, accountPlaylistsState),
                            ) {
                                items(shuffledPlaylists, key = { "home_account_playlist_${it.id}" }) { playlist ->
                                    ytGridItem(playlist)
                                }
                            }
                        }
                    }

                }

                feedSection()
                dailySection()
                communitySection()
                similarSection()
                accountSection()
            }
            }

            if (globeOn) {
                HomeFilmViewfinderOverlay(
                )
            }

            FrostedPageTitleBar(
                titleRes = R.string.home,
                progress = compactTitleProgress,
                backdrop = null,
                fullWidth = true,
                titleProgress = compactTitleProgress,
                barHeight = homeBarHeight,
                centerTitleVertically = true,
                titleEnterFromBottom = !homeReducedMotion,
                solidBackgroundColor = Color(0xFF070707),
                fadeTitle = false,
            )

        }
    }
    }
}

@Composable
fun SyncBanner(syncState: SyncState, useIrideStyle: Boolean = false) {
    val ops = listOf(
        syncState.likedSongs, syncState.librarySongs, syncState.uploadedSongs,
        syncState.likedAlbums, syncState.uploadedAlbums, syncState.artists, syncState.playlists,
    )
    val completedCount = ops.count { it == SyncStatus.Completed }
    val progress = completedCount / ops.size.toFloat()

    if (useIrideStyle) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.textSecondary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.syncing_library).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 13.sp,
                            letterSpacing = (-0.1).sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.network_sync_warning),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily),
                        color = MaterialTheme.colorScheme.textTertiary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.textSecondary,
                trackColor = MaterialTheme.colorScheme.fillSelected,
            )
            if (syncState.currentOperation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.currentOperation,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpaceMonoFontFamily),
                    color = MaterialTheme.colorScheme.textTertiary,
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.syncing_your_library),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.network_sync_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
            )
            if (syncState.currentOperation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.currentOperation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
                )
            }
        }
    }
}

private val DailyDiscoverSeedMessages = listOf(
    R.string.daily_discover_sounds_like,
    R.string.daily_discover_because_you_listen_to,
    R.string.daily_discover_based_on,
    R.string.daily_discover_for_fans_of,
)

/**
 * Compact, flat, monochrome Daily Discover card for the New Iride UI — replaces the classic
 * UI's large Material3 carousel card with a small squared thumbnail + single-line text stack,
 * matching the bordered-tile look used elsewhere (e.g. [NewActionButton]).
 */
@Composable
private fun IrideDailyDiscoverCard(
    dailyDiscover: com.metrolist.music.viewmodels.DailyDiscoverItem,
    size: Dp,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val song = dailyDiscover.recommendation as? SongItem
    val messageRes = remember(dailyDiscover.seed.id) {
        DailyDiscoverSeedMessages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % DailyDiscoverSeedMessages.size]
    }

    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline), RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (song != null) {
                        menuState.show {
                            YouTubeSongMenu(song = song, navController = navController, onDismiss = { menuState.dismiss() })
                        }
                    }
                },
            )
            .padding(8.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(dailyDiscover.recommendation.thumbnail?.resize(300, 300))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .irideArtworkOverlayBorder(IrideSoftBorderWidth, MaterialTheme.colorScheme.strokeItemSoft, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dailyDiscover.recommendation.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song?.artists?.joinToString(", ") { it.name } ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                messageRes,
                "${dailyDiscover.seed.title} • ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

