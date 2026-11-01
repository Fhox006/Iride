/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.metrolist.music.ui.component.LocalArtistPortraitNavigation
import com.metrolist.music.ui.component.rememberArtistPortraitTile
import com.metrolist.music.ui.component.rememberCollectionExpansionTile
import com.metrolist.music.ui.component.CollectionExpansionKind
import com.metrolist.music.ui.component.rememberIrideArtistArtwork
import androidx.compose.ui.graphics.asImageBitmap
import com.metrolist.music.ui.component.LocalArtistPortraitTransitionActive
import com.metrolist.music.ui.component.ArtistPortraitMotion
import java.util.UUID
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import com.metrolist.music.ui.component.LocalAnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.MoodMixes
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.ForYouShelfTile
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HomeFollowedReleases
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioDisplayHeadings
import com.metrolist.music.ui.component.IrideFolioMediaShelf
import com.metrolist.music.ui.component.IrideFolioSection
import com.metrolist.music.ui.component.IrideFolioSectionSkeleton
import com.metrolist.music.ui.component.IrideFolioShelfHeader
import com.metrolist.music.ui.component.IrideFolioShelfSkeleton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.LocalHomeContentSnapshot
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.MixPosterPlaceholder
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.ReverieHomeCard
import com.metrolist.music.ui.component.SharedHomeSectionTitle
import com.metrolist.music.ui.component.SharedQuickPicksCarousel
import com.metrolist.music.ui.component.SharedYouTubePlaylistRow
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.collectDeferredHomeState
import com.metrolist.music.ui.component.currentGridThumbnailHeight
import com.metrolist.music.ui.component.folioTypewriterFontFamily
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.component.irideFolioShelfTypography
import com.metrolist.music.ui.component.rememberArtistThumbnail
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.screens.playlist.NewReleaseChartsShelf
import com.metrolist.music.ui.screens.playlist.PreloadHomeCharts
import com.metrolist.music.ui.theme.fillSelected
import com.metrolist.music.ui.theme.SatoshiFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.irideDepthEnter
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.SyncState
import com.metrolist.music.utils.SyncStatus
import com.metrolist.music.utils.appendLoadedHomeSections
import com.metrolist.music.utils.isInternetAvailable
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ChartDirectoryViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.MonthlyReverieViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

private const val SHOW_HOME_TEST_PLAYLIST = false

/**
 * New Iride UI: horizontally scrollable chip row for the Mood category selector. Reuses the same
 * "single shared indicator glides between labels" technique as IrideSegmentedToggle (the
 * Library/Downloaded switch in LibraryMixScreen.kt, see ui/component/ChipsRow.kt) â€” the underline
 * slides and resizes smoothly from one chip to the next instead of each chip independently popping
 * its own static underline on/off, which read as an un-animated flicker when switching mood
 * categories. Kept local to Home instead of changing the shared ChipsRow composable itself, since
 * that component is also used (in its plain, non-glide form) by several other screens.
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
    val labelBoundsPx = remember {
        androidx.compose.runtime.mutableStateMapOf<Int, Pair<Float, Float>>()
    }
    val selectedIndex = chips.indexOfFirst { it.first == currentValue }
    val targetBounds = if (selectedIndex >= 0) labelBoundsPx[selectedIndex] else null
    val reducedMotion = rememberReducedMotion()
    val chipScrollState = rememberScrollState()
    val indicatorAnimSpec =
        tween<Float>(
            durationMillis = if (reducedMotion) 0 else IrideMotion.Short,
            easing = IrideMotion.EaseOutQuart,
        )
    val indicatorX by
        animateFloatAsState(
            targetBounds?.first ?: 0f,
            indicatorAnimSpec,
            label = "moodChipIndicatorX",
        )
    val indicatorWidth by
        animateFloatAsState(
            targetBounds?.second ?: 0f,
            indicatorAnimSpec,
            label = "moodChipIndicatorWidth",
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .rubberBandOverscroll(Orientation.Horizontal, chipScrollState)
                .horizontalScroll(chipScrollState, overscrollEffect = null)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(horizontalPadding))
            chips.forEachIndexed { index, (value, label) ->
                val isSelected = currentValue == value
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    color =
                        if (isSelected) MaterialTheme.colorScheme.textPrimary
                        else MaterialTheme.colorScheme.textTertiary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    modifier =
                        Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                onValueUpdate(value)
                            }
                            .padding(vertical = 6.dp)
                            .onGloballyPositioned { coords ->
                                labelBoundsPx[index] =
                                    coords.positionInParent().x to coords.size.width.toFloat()
                            },
                )
                if (index != chips.lastIndex) Spacer(Modifier.width(20.dp))
            }
            Spacer(Modifier.width(horizontalPadding))
        }
        Spacer(Modifier.height(3.dp))
        if (targetBounds != null) {
            Box(
                modifier =
                    Modifier.offset { IntOffset(indicatorX.roundToInt(), 0) }
                        .width(with(density) { indicatorWidth.toDp() })
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.textPrimary)
            )
        } else {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ForYouShelfSkeleton(boxSize: Dp, contentPadding: Dp, modifier: Modifier = Modifier) {
    IrideFolioShelfSkeleton(boxSize, modifier, contentPadding, tilesPerSide = 2)
}

@Composable
private fun HomeSimilarityTitle(
    name: String,
    thumbnailUrl: String?,
    isArtist: Boolean,
    artistId: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = if (isArtist) CircleShape else FolioShape(5.dp)
    val resolvedThumbnail =
        if (isArtist && artistId != null) {
            rememberArtistThumbnail(artistId, name, thumbnailUrl)
        } else thumbnailUrl
    val portraitTile = if (isArtist && artistId != null && onClick != null) {
        rememberArtistPortraitTile(artistId, name, resolvedThumbnail, null, true)
    } else Modifier to Modifier
    IrideFolioShelfHeader(
        label = stringResource(R.string.home_similar_to),
        title = compactHomeTitle(name),
        thumbnail = {
            ItemThumbnail(
                thumbnailUrl = resolvedThumbnail,
                isActive = false,
                isPlaying = false,
                shape = shape,
                artistArtwork = isArtist,
                requestResolution = 192,
                hairlineBorder = true,
                modifier = Modifier.size(48.dp).then(portraitTile.second),
            )
        },
        onClick = onClick,
        modifier = portraitTile.first.then(modifier),
        showNavigationArrow = false,
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.contentSnapshot.setVisible(true)
                Lifecycle.Event.ON_STOP -> viewModel.contentSnapshot.setVisible(false)
                else -> Unit
            }
        }
        viewModel.contentSnapshot.setVisible(
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.contentSnapshot.setVisible(false)
        }
    }
    CompositionLocalProvider(LocalHomeContentSnapshot provides viewModel.contentSnapshot) {
        HomeScreenContent(navController, snackbarHostState, viewModel)
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun HomeScreenContent(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel,
) {
    val menuState = com.metrolist.music.ui.component.LocalMenuState.current
    val chartViewModel: ChartDirectoryViewModel = hiltViewModel()
    val reverieViewModel: MonthlyReverieViewModel = hiltViewModel()
    val reverieMonths by reverieViewModel.archiveMonths.collectAsStateWithLifecycle()
    val firstFullReverieMonth by reverieViewModel.firstFullMonth.collectAsStateWithLifecycle()
    PreloadHomeCharts(chartViewModel)
    val followedReleasesPosition = rememberSaveable { kotlin.random.Random.nextInt(1, 6) }
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val context = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlayingState =
        remember(playerConnection) {
                playerConnection?.isEffectivelyPlaying ?: MutableStateFlow(false)
            }
            .collectAsStateWithLifecycle()
    val mediaMetadataState =
        remember(playerConnection) {
                playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
            }
            .collectAsStateWithLifecycle()

    val isHomeFeedLoading by viewModel.isHomeFeedLoading.collectAsStateWithLifecycle()
    val isForYouLoading by viewModel.isForYouLoading.collectAsStateWithLifecycle()
    val isAccountPlaylistsLoading by
        viewModel.isAccountPlaylistsLoading.collectAsStateWithLifecycle()
    val dailyDiscoverComplete by viewModel.phase2DailyDiscoverDone.collectAsStateWithLifecycle()
    val communityComplete by viewModel.phase2CommunityDone.collectAsStateWithLifecycle()
    val similarComplete by viewModel.phase2SimilarDone.collectAsStateWithLifecycle()

    val communityPlaylists by
        viewModel.communityPlaylists.collectDeferredHomeState("community_playlists") {
            it.orEmpty().distinctBy { item -> item.playlist.id }.size >= 4
        }
    val dailyDiscover by
        viewModel.dailyDiscover.collectDeferredHomeState("daily_discover") {
            it.orEmpty().distinctBy { item -> item.recommendation.id }.size >= 4
        }
    val accountPlaylists by
        viewModel.accountPlaylists.collectDeferredHomeState("account_playlists") {
            it.orEmpty().distinctBy { item -> item.id }.size >= 4
        }
    val homePersonalMixes by viewModel.homePersonalMixes.collectDeferredHomeState("personal_mixes")
    val starredPlaylistThumbnails by
        viewModel.starredPlaylistThumbnails.collectDeferredHomeState("starred_thumbnails")
    val similarRecommendations by
        viewModel.similarRecommendations.collectDeferredHomeState(
            "similar_recommendations",
            mergeWhileVisible = { current, incoming ->
                appendLoadedHomeSections(current.orEmpty(), incoming.orEmpty()) { it.title.id }
            },
        )
    val quickPicks by
        viewModel.quickPicks.collectDeferredHomeState("quick_picks") {
            it.orEmpty()
                .filterNot { song -> song.song.isVideo || song.song.isEpisode }
                .distinctBy { song -> song.id }
                .size >= 4
        }
    val forYouShelves by
        viewModel.forYouShelves.collectDeferredHomeState(
            "for_you",
            mergeWhileVisible = { current, incoming ->
                appendLoadedHomeSections(current, incoming) { it.artist.id }
            },
        ) {
            it.size >= 4
        }
    val homePage by
        viewModel.homePage.collectDeferredHomeState("feed") { !it?.sections.isNullOrEmpty() }
    val phase1Complete by viewModel.phase1Complete.collectAsStateWithLifecycle()
    val isHeroCarouselEnabled by viewModel.isHeroCarouselEnabled.collectAsStateWithLifecycle()
    val isHeroCarouselLoading by viewModel.isHeroCarouselLoading.collectAsStateWithLifecycle()
    val heroCarouselItems by
        viewModel.heroCarouselItems.collectDeferredHomeState(
            "hero_carousel",
            mergeWhileVisible = { current, incoming ->
                val covers =
                    incoming.filterIsInstance<HeroCarouselItem.ArtistRadio>().associateBy {
                        it.artistId
                    }
                current.map { item ->
                    if (item is HeroCarouselItem.ArtistRadio) {
                        covers[item.artistId]?.coverUrl?.let { item.copy(coverUrl = it) } ?: item
                    } else item
                }
            },
        )

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

    val lazyListState = rememberLazyListState()
    val homeReducedMotion = rememberReducedMotion()
    val moodContent by
        viewModel.homeMoodContent.collectDeferredHomeState(
            "mood",
            identity = { it.params },
            hasContent = { !it.items.isNullOrEmpty() },
            mergeWhileVisible = { current, incoming ->
                current.copy(posters = current.posters + incoming.posters)
            },
        )
    val moodMixItems = moodContent.items
    val testPlaylist = if (SHOW_HOME_TEST_PLAYLIST) {
        moodMixItems?.firstOrNull()
            ?: homePersonalMixes.firstOrNull()
            ?: homePage?.sections
                ?.asSequence()
                ?.flatMap { it.items.asSequence() }
                ?.filterIsInstance<PlaylistItem>()
                ?.firstOrNull()
    } else {
        null
    }
    val testPlaylistTrackThumbnails by
        viewModel.testPlaylistTrackThumbnails.collectAsStateWithLifecycle()
    if (SHOW_HOME_TEST_PLAYLIST) {
        LaunchedEffect(testPlaylist?.id) {
            testPlaylist?.let { viewModel.loadTestPlaylistArtwork(it.id) }
        }
    }
    val moodPosterUrls = moodContent.posters
    val moodPosterSize =
        with(LocalDensity.current) { currentGridHeight.roundToPx() }.coerceIn(128, 768)
    val isMoodLoading by viewModel.isMoodLoading.collectAsStateWithLifecycle()
    val isMoodError by viewModel.isMoodError.collectAsStateWithLifecycle()
    val loadedMoodParams = moodContent.params
    val preferredMoodParams by viewModel.preferredMoodParams.collectAsStateWithLifecycle()
    val isMoodPreferenceReady by viewModel.isMoodPreferenceReady.collectAsStateWithLifecycle()
    var selectedMoodCategory by remember {
        mutableStateOf<com.metrolist.innertube.pages.HomePage.Chip?>(null)
    }
    val cachedMoodSnapshot by viewModel.cachedMoodSnapshot.collectAsStateWithLifecycle()
    val moodChips =
        remember(homePage?.chips, cachedMoodSnapshot) {
            val feedChips = MoodMixes.chips(homePage?.chips)
            val availableChips =
                feedChips.ifEmpty {
                    cachedMoodSnapshot
                        ?.takeIf { !it.chipParams.isNullOrBlank() }
                        ?.let { snapshot ->
                            listOf(
                                com.metrolist.innertube.pages.HomePage.Chip(
                                    title = snapshot.chipTitle,
                                    endpoint =
                                        BrowseEndpoint(
                                            browseId = "FEmusic_home",
                                            params = snapshot.chipParams,
                                        ),
                                    deselectEndPoint = null,
                                )
                            )
                        }
                        .orEmpty()
                }
            availableChips.map { it to it.title }
        }
    val moodMixesState = rememberLazyListState()
    val moodLifecycleOwner = LocalLifecycleOwner.current
    val moodContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(
        moodLifecycleOwner,
        selectedMoodCategory,
        hideExplicit,
        hideVideoSongs,
        hideYoutubeShorts,
        moodPosterSize,
    ) {
        moodLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (selectedMoodCategory != null) {
                viewModel.loadMoodPage(
                    selectedMoodCategory?.endpoint?.params,
                    selectedMoodCategory?.title,
                    hideExplicit,
                    hideVideoSongs,
                    hideYoutubeShorts,
                    moodPosterSize,
                    forceReload = false,
                )
            }
            viewModel.isMoodError.collectLatest { failed ->
                while (failed && viewModel.isMoodError.value) {
                    delay(30_000)
                    if (isInternetAvailable(moodContext)) {
                        viewModel.loadMoodPage(
                            selectedMoodCategory?.endpoint?.params,
                            selectedMoodCategory?.title,
                            hideExplicit,
                            hideVideoSongs,
                            hideYoutubeShorts,
                            moodPosterSize,
                            forceReload = true,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(moodChips, isMoodPreferenceReady) {
        if (isMoodPreferenceReady && moodChips.isNotEmpty()) {
            val current =
                moodChips
                    .firstOrNull {
                        it.first.title.equals(selectedMoodCategory?.title, ignoreCase = true)
                    }
                    ?.first
            selectedMoodCategory =
                current
                    ?: moodChips
                        .firstOrNull { it.first.endpoint?.params == preferredMoodParams }
                        ?.first
                    ?: moodChips.first().first
        }
    }
    LaunchedEffect(selectedMoodCategory) {
        moodMixesState.scrollToItem(0)
    }

    val screenProgress =
        if (IrideTabEntrance.wasRevealed("home")) {
            1f
        } else {
            rememberEnterProgress(
                    play = true,
                    durationMillis = IrideMotion.Short,
                    easing = IrideMotion.EaseOutQuart,
                )
                .also { if (it >= 1f) IrideTabEntrance.markRevealed("home") }
        }
    val scope = rememberCoroutineScope()
    val backStackEntry = LocalViewModelStoreOwner.current as? NavBackStackEntry
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (homeReducedMotion) lazyListState.scrollToItem(0)
            else lazyListState.animateScrollToItem(0)
            backStackEntry.savedStateHandle.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onSectionBecameVisible("daily_discover")
        viewModel.onSectionBecameVisible("from_the_community")
        viewModel.onSectionBecameVisible("similar_recommendation_0")
    }

    val orderedFeedSections =
        remember(homePage?.sections) {
            val sections = homePage?.sections.orEmpty()
            val selectedTrendingIndex =
                sections
                    .withIndex()
                    .filter { (_, section) ->
                        val title = section.title.lowercase()
                        val label = section.label.orEmpty().lowercase()
                        (title.contains("trending") || title.contains("top 100")) &&
                            !listOf("italia", "italy", "italiano", "italian").any {
                                it in title || it in label
                            }
                    }
                    .firstOrNull { (_, section) ->
                        section.title.contains("top 100", ignoreCase = true)
                    }
                    ?.index
                    ?: sections
                        .withIndex()
                        .firstOrNull { (_, section) ->
                            val title = section.title.lowercase()
                            val label = section.label.orEmpty().lowercase()
                            title.contains("trending") &&
                                !listOf("italia", "italy", "italiano", "italian").any {
                                    it in title || it in label
                                }
                        }
                        ?.index

            sections
                .withIndex()
                .filter { (index, section) ->
                    val title = section.title.lowercase()
                    val isTrending = title.contains("trending") || title.contains("top 100")
                    !isTrending || index == selectedTrendingIndex
                }
                .map { indexed ->
                    IndexedValue(indexed.index, indexed.value)
                }
                .sortedBy { indexed ->
                    val title = indexed.value.title.lowercase()
                    when {
                        title.contains("trending") || title.contains("top 100") -> 0
                        title.contains("new release") || title.contains("nuove uscite") -> 1
                        else -> 2
                    }
                }
        }
    val pickedSongs =
        remember(quickPicks) {
            quickPicks
                .orEmpty()
                .filterNot { it.song.isVideo || it.song.isEpisode }
                .distinctBy { it.id }
        }

    val homeColorScheme =
        MaterialTheme.colorScheme.let { colors ->
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
    MaterialTheme(colorScheme = homeColorScheme, typography = irideFolioShelfTypography()) {
        IrideFolioDisplayHeadings(menuState = menuState) {
            Scaffold(
                modifier = Modifier,
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
            ) { paddingValues ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
                        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                            YouTubeGridItem(
                                item = item,
                                isActive =
                                    item.id in
                                        listOf(
                                            mediaMetadataState.value?.album?.id,
                                            mediaMetadataState.value?.id,
                                        ),
                                isPlaying = isPlayingState.value,
                                coroutineScope = scope,
                                thumbnailRatio =
                                    if (item is SongItem && item.isVideoSong) 16f / 9f else 1f,
                                forceThumbnailRatio = item is SongItem && item.isVideoSong,
                                size = currentGridHeight,
                                modifier =
                                    Modifier.irideFolioCombinedClickable(
                                        onClick = {
                                            when (item) {
                                                is SongItem -> {
                                                    if (!isListenTogetherGuest) {
                                                        if (item.isVideoSong)
                                                            playerConnection
                                                                ?.videoPlayback
                                                                ?.playVideo(item.toMediaMetadata())
                                                        else
                                                            playerConnection?.playQueue(
                                                                YouTubeQueue(
                                                                    item.endpoint
                                                                        ?: WatchEndpoint(
                                                                            videoId = item.id
                                                                        ),
                                                                    item.toMediaMetadata(),
                                                                )
                                                            )
                                                    }
                                                }
                                                is AlbumItem ->
                                                    navController.navigate("album/${item.id}")
                                                is ArtistItem ->
                                                    navController.navigate("artist/${item.id}")
                                                is PlaylistItem ->
                                                    navController.navigate(
                                                        "online_playlist/${item.id.removePrefix("VL")}"
                                                    )
                                                is PodcastItem ->
                                                    navController.navigate(
                                                        "online_podcast/${item.id}"
                                                    )
                                                is EpisodeItem -> {
                                                    if (!isListenTogetherGuest) {
                                                        playerConnection?.playQueue(
                                                            ListQueue(
                                                                title = item.title,
                                                                items =
                                                                    listOf(
                                                                        item
                                                                            .toMediaMetadata()
                                                                            .toMediaItem()
                                                                    ),
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show(anchored = true) {
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
                                                        YouTubeArtistMenu(
                                                            artist = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    is PlaylistItem ->
                                                        YouTubePlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    is PodcastItem ->
                                                        YouTubePlaylistMenu(
                                                            playlist = item.asPlaylistItem(),
                                                            coroutineScope = scope,
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
                                        },
                                    ),
                            )
                        }
                    }

                    val ytSongItem: @Composable (SongItem, Dp) -> Unit = { song, itemWidth ->
                        val showSongMenu = {
                            menuState.show(anchored = true) {
                                YouTubeSongMenu(
                                    song = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        }
                        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                            YouTubeListItem(
                                item = song,
                                isActive = song.id == mediaMetadataState.value?.id,
                                isPlaying = isPlayingState.value,
                                isSwipeable = false,
                                showDivider = false,
                                trailingContent = {
                                    SongMenuIconButton(onClick = showSongMenu) {
                                        Icon(
                                            painterResource(R.drawable.more_vert),
                                            stringResource(R.string.more_options),
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.width(itemWidth)
                                        .irideFolioCombinedClickable(
                                            onClick = {
                                                if (!isListenTogetherGuest) {
                                                    if (song.id == mediaMetadataState.value?.id)
                                                        playerConnection?.togglePlayPause()
                                                    else
                                                        playerConnection?.playQueue(
                                                            YouTubeQueue(
                                                                song.endpoint
                                                                    ?: WatchEndpoint(
                                                                        videoId = song.id
                                                                    ),
                                                                song.toMediaMetadata(),
                                                            )
                                                        )
                                                }
                                            },
                                            onLongClick = showSongMenu,
                                        ),
                            )
                        }
                    }

                    val forYouBoxScale = 1.35f
                    val forYouCellGap = IrideFolio.ShelfMosaicGap
                    val forYouBoxSize = currentGridHeight * forYouBoxScale
                    val forYouCellSize = (forYouBoxSize - forYouCellGap) / 2
                    val forYouOnClick: (LocalItem) -> Unit = { cell ->
                        when (cell) {
                            is Song -> {
                                if (!isListenTogetherGuest) {
                                    if (cell.id == mediaMetadataState.value?.id) {
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
                        val tileShape = if (cell is Artist) CircleShape else FolioShape(6.dp)
                        val collectionTile = if (cell is Album) {
                            rememberCollectionExpansionTile(
                                CollectionExpansionKind.Album, cell.id, artworkUrl = cell.thumbnailUrl, title = cell.title,
                            )
                        } else Modifier to Modifier
                        val thumbnail =
                            if (cell is Artist) {
                                rememberArtistThumbnail(cell.id, cell.title, cell.thumbnailUrl)
                            } else cell.thumbnailUrl
                        val portraitNavigation = LocalArtistPortraitNavigation.current
                        val portraitKey = rememberSaveable { UUID.randomUUID().toString() }
                        var portraitBounds by remember { mutableStateOf(Rect.Zero) }
                        val portrait =
                            if (cell is Artist) {
                                rememberIrideArtistArtwork(
                                        artistId = cell.id,
                                        url = cell.thumbnailUrl,
                                        artistName = cell.title,
                                        channelId = cell.artist.channelId,
                                        externalEnabled =
                                            !cell.artist.isLocal &&
                                                !cell.artist.isPodcastChannel &&
                                                !cell.artist.isPrivatelyOwnedArtist,
                                        highResolution = false,
                                    )
                                    .value
                            } else null
                        val portraitScope = LocalAnimatedVisibilityScope.current
                        val portraitInFlight =
                            cell is Artist && LocalArtistPortraitTransitionActive.current &&
                                portraitScope != null && !homeReducedMotion &&
                                portraitNavigation?.seed?.sourceKey == portraitKey
                        val homePortraitProgress = if (portraitInFlight) {
                            val progress by portraitScope!!.transition.animateFloat(
                                transitionSpec = { tween(ArtistPortraitMotion.Duration, easing = ArtistPortraitMotion.Easing) },
                                label = "homePortraitVisibility",
                            ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
                            progress
                        } else 1f
                        // These tiles render at roughly half the real grid size, so the outline
                        // uses thinner weights than standard items to keep the same visual weight.
                        Box(
                            modifier =
                                Modifier.size(forYouCellSize)
                                    .then(collectionTile.first)
                                    .then(collectionTile.second)
                                    .clip(tileShape)
                                    .irideFolioCombinedClickable(
                                        onClick = {
                                            if (cell is Artist && portrait != null) {
                                                portraitNavigation?.select(
                                                    portraitKey,
                                                    cell.id,
                                                    cell.title,
                                                    portrait,
                                                    thumbnail,
                                                    portraitBounds,
                                                )
                                                navController.navigate(
                                                    "artist/${cell.id}?portraitSource=$portraitKey"
                                                )
                                            } else {
                                                forYouOnClick(cell)
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show(anchored = true) {
                                                when (cell) {
                                                    is Song ->
                                                        SongMenu(
                                                            originalSong = cell,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    is Album ->
                                                        AlbumMenu(
                                                            originalAlbum = cell,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    is Artist ->
                                                        ArtistMenu(
                                                            originalArtist = cell,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    is Playlist ->
                                                        PlaylistMenu(
                                                            playlist = cell,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                }
                                            }
                                        },
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color =
                                            if (cell is Song) MaterialTheme.colorScheme.strokeItemSoft
                                            else MaterialTheme.colorScheme.strokeCard,
                                        shape = tileShape,
                                    )
                        ) {
                            if (cell is Artist && portrait != null) {
                                val displayedPortrait = portraitNavigation?.seed?.takeIf { it.sourceKey == portraitKey }?.artwork ?: portrait
                                Image(
                                    bitmap = remember(displayedPortrait.bitmap) { displayedPortrait.bitmap.asImageBitmap() },
                                    contentDescription = cell.title,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.fillMaxSize()
                                            .onGloballyPositioned { portraitBounds = it.boundsInWindow() }
                                            .alpha(if (portraitInFlight && homePortraitProgress < 1f) 0f else 1f)
                                            .clip(tileShape),
                                )
                            } else {
                                AsyncImage(
                                    model =
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(thumbnail?.resize(300, 300))
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                    contentDescription = cell.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(tileShape),
                                )
                            }
                        }
                    }
                    val forYouRecommendedTile: @Composable (ForYouShelfTile) -> Unit = { tile ->
                        when (tile) {
                            is ForYouShelfTile.Local -> forYouMiniTile(tile.item)
                            is ForYouShelfTile.RecommendedAlbum -> {
                                val album = tile.item
                                val collectionTile = rememberCollectionExpansionTile(
                                    CollectionExpansionKind.Album, album.id, artworkUrl = album.thumbnail, title = album.title,
                                )
                                AsyncImage(
                                    model =
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(album.thumbnail?.resize(300, 300))
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.size(forYouCellSize)
                                            .then(collectionTile.first)
                                            .then(collectionTile.second)
                                            .irideArtworkOverlayBorder(
                                                IrideSoftBorderWidth,
                                                MaterialTheme.colorScheme.strokeCard,
                                                FolioShape(6.dp),
                                            )
                                            .clip(FolioShape(6.dp))
                                            .irideFolioCombinedClickable(
                                                onClick = { navController.navigate("album/${album.id}") },
                                                onLongClick = {
                                                    menuState.show(anchored = true) {
                                                        YouTubeAlbumMenu(
                                                            albumItem = album,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ),
                                )
                            }
                        }
                    }
                    val forYouBlock: @Composable (ForYouShelfItem) -> Unit = { shelf ->
                        val cells: List<Any> =
                            remember(shelf) {
                                listOf(shelf.artist) +
                                    shelf.tiles
                                        .shuffled(kotlin.random.Random(shelf.artist.id.hashCode()))
                                        .take(3)
                            }
                        Column(modifier = Modifier.width(forYouBoxSize)) {
                            Column(verticalArrangement = Arrangement.spacedBy(forYouCellGap)) {
                                cells.chunked(2).forEach { rowCells ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(forYouCellGap)
                                    ) {
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
                                style =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = folioTypewriterFontFamily()
                                    ),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    val forYouLoading = isForYouLoading && forYouShelves.isEmpty()
                    val forYouSection: LazyListScope.(HomeShelfOrder) -> Unit = { shelfOrder ->
                        if (forYouLoading || forYouShelves.isNotEmpty()) {
                            shelfOrder.added(hasFourRows = false)
                            item(key = "for_you_section") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec =
                                                if (homeReducedMotion) null
                                                else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "for_you_section",
                                    ready = !forYouLoading,
                                    artwork =
                                        forYouShelves.take(2).flatMap { shelf ->
                                            listOf(shelf.artist.thumbnailUrl?.resize(300, 300)) +
                                                shelf.tiles.map { tile ->
                                                    when (tile) {
                                                        is ForYouShelfTile.Local ->
                                                            tile.item.thumbnailUrl?.resize(300, 300)
                                                        is ForYouShelfTile.RecommendedAlbum ->
                                                            tile.item.thumbnail.resize(300, 300)
                                                    }
                                                }
                                        },
                                    placeholder = {
                                        IrideFolioSectionSkeleton(forYouBoxSize, mosaic = true)
                                    },
                                ) {
                                    IrideFolioShelfHeader(
                                        title = stringResource(R.string.for_you_artists_title),
                                        showNavigationArrow = false,
                                    )

                                    val shelves = forYouShelves
                                    val forYouState = rememberLazyListState()
                                    LaunchedEffect(forYouState, shelves.size, isForYouLoading) {
                                        snapshotFlow {
                                            forYouState.layoutInfo.visibleItemsInfo
                                                .lastOrNull()
                                                ?.index
                                        }
                                            .collect { lastVisible ->
                                                if (
                                                    lastVisible != null &&
                                                        lastVisible >= shelves.size - 3 &&
                                                        viewModel.forYouShelves.value.size <=
                                                            shelves.size
                                                ) {
                                                    viewModel.loadMoreForYouShelves()
                                                }
                                            }
                                    }
                                    LazyRow(
                                        state = forYouState,
                                        contentPadding =
                                            PaddingValues(horizontal = irideGridItemStart),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                                        overscrollEffect = null,
                                        modifier =
                                            Modifier.rubberBandOverscroll(
                                                Orientation.Horizontal,
                                                forYouState,
                                            ),
                                    ) {
                                        itemsIndexed(
                                            shelves,
                                            key = { index, shelf ->
                                                "for_you_${index}_${shelf.artist.id}"
                                            },
                                        ) { _, shelf ->
                                            forYouBlock(shelf)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    IridePageLayout(
                        title = homePageTitle(),
                        state = lazyListState,
                        reducedMotion = homeReducedMotion,
                        bottomPadding =
                            LocalPlayerAwareWindowInsets.current
                                .asPaddingValues()
                                .calculateBottomPadding(),
                        listModifier =
                            Modifier.irideDepthEnter({ screenProgress }, intensity = 0.4f),
                        overlay = {
                            if (
                                isLoggedIn &&
                                    syncState.overallStatus == SyncStatus.Syncing &&
                                    syncBannerLaunchCount < 3
                            ) {
                                Text(
                                    text = stringResource(R.string.syncing_library),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.textSecondary,
                                    modifier =
                                        Modifier.align(Alignment.TopEnd)
                                            .windowInsetsPadding(WindowInsets.statusBars)
                                            .padding(
                                                horizontal = IrideFolio.HorizontalPadding,
                                                vertical = 8.dp,
                                            )
                                            .background(
                                                MaterialTheme.colorScheme.background,
                                                FolioShape(4.dp),
                                            )
                                            .padding(4.dp),
                                )
                            }
                        },
                    ) {
                        val shelfOrder = HomeShelfOrder()
                        if (isHeroCarouselEnabled) {
                            item(key = "hero_carousel") {
                                val radioItems =
                                    heroCarouselItems.filterIsInstance<
                                        HeroCarouselItem.ArtistRadio
                                    >()
                                if (radioItems.isNotEmpty() || isHeroCarouselLoading) {
                                    IrideFolioSection(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec =
                                                    if (homeReducedMotion) null
                                                    else IrideMotion.PlacementSpec,
                                                fadeOutSpec = null,
                                            ),
                                        sectionKey = "hero_carousel",
                                        ready = radioItems.isNotEmpty(),
                                        artwork = radioItems.take(1).map { it.coverUrl },
                                        placeholder = {
                                            IrideFolioSectionSkeleton(
                                                currentGridHeight,
                                                hero = true,
                                            )
                                        },
                                    ) {
                                        HeroCarouselSection(
                                            items = radioItems,
                                            onCoverError = viewModel::repairHeroRadioCover,
                                            onArtistClick = { artistId ->
                                                navController.navigate("artist/$artistId")
                                            },
                                            onArtistLongClick = { artist ->
                                                menuState.show(anchored = true) {
                                                    YouTubeArtistMenu(
                                                        artist =
                                                            ArtistItem(
                                                                id = artist.artistId,
                                                                title = artist.artistName,
                                                                thumbnail = artist.coverUrl,
                                                                shuffleEndpoint = null,
                                                                radioEndpoint = null,
                                                            ),
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        if (quickPicks == null || pickedSongs.isNotEmpty()) {
                            shelfOrder.added(hasFourRows = true)
                            item(key = "picked_for_you_section") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec =
                                                if (homeReducedMotion) null
                                                else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "picked_for_you_section",
                                    ready = quickPicks != null,
                                    artwork =
                                        pickedSongs.take(4).map {
                                            it.song.thumbnailUrl?.resize(448, 448)
                                        },
                                    placeholder = {
                                        IrideFolioSectionSkeleton(currentGridHeight, songs = true)
                                    },
                                ) {
                                    SharedHomeSectionTitle(
                                        title = stringResource(R.string.quick_picks_section_title)
                                    )

                                    SharedQuickPicksCarousel(
                                        items = pickedSongs,
                                        key = { "picked_${it.id}" },
                                        loading = quickPicks == null,
                                        contentPadding = irideListItemStart - 4.dp,
                                        usePager = true,
                                        minimumRows = 1,
                                        modifier = Modifier,
                                    ) { originalSong, itemWidth ->
                                        val song by
                                            database
                                                .song(originalSong.id)
                                                .collectAsStateWithLifecycle(
                                                    initialValue = originalSong
                                                )
                                        val currentSong = song ?: originalSong
                                        CompositionLocalProvider(
                                            LocalItemHorizontalPadding provides false
                                        ) {
                                            SongListItem(
                                                song = currentSong,
                                                isActive =
                                                    currentSong.id == mediaMetadataState.value?.id,
                                                isPlaying = isPlayingState.value,
                                                isSwipeable = false,
                                                showDivider = false,
                                                trailingContent = {
                                                    SongMenuIconButton(
                                                        onClick = {
                                                            menuState.show(anchored = true) {
                                                                SongMenu(
                                                                    originalSong = currentSong,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter =
                                                                painterResource(
                                                                    R.drawable.more_vert
                                                                ),
                                                            contentDescription =
                                                                stringResource(
                                                                    R.string.more_options
                                                                ),
                                                        )
                                                    }
                                                },
                                                modifier =
                                                    Modifier.width(itemWidth)
                                                        .irideFolioCombinedClickable(
                                                            onClick = {
                                                                if (!isListenTogetherGuest) {
                                                                    if (
                                                                        currentSong.id ==
                                                                            mediaMetadataState.value
                                                                                ?.id
                                                                    ) {
                                                                        playerConnection
                                                                            ?.togglePlayPause()
                                                                    } else {
                                                                        playerConnection?.playQueue(
                                                                            YouTubeQueue(
                                                                                WatchEndpoint(
                                                                                    videoId =
                                                                                        currentSong
                                                                                            .id
                                                                                ),
                                                                                currentSong
                                                                                    .toMediaMetadata(),
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                menuState.show(anchored = true) {
                                                                    SongMenu(
                                                                        originalSong = currentSong,
                                                                        navController =
                                                                            navController,
                                                                        onDismiss =
                                                                            menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val knownArtistIds = forYouShelves.map { it.artist.id }.toSet()
                        val knownArtistNames =
                            forYouShelves.map { it.artist.artist.name.lowercase() }.toSet()
                        val addFeedSection:
                            LazyListScope.(
                                IndexedValue<com.metrolist.innertube.pages.HomePage.Section>
                            ) -> Unit =
                            feedSection@{ indexed ->
                                val index = indexed.index
                                val section = indexed.value
                                val title = section.title.lowercase()
                                val label = section.label.orEmpty().lowercase()
                                val isSimilar =
                                    title.contains("similar to") ||
                                        title.contains("simile a") ||
                                        label.contains("similar to") ||
                                        label.contains("simile a")
                                val isTrending =
                                    title.contains("trending") || title.contains("top 100")
                                val isNewRelease =
                                    title.contains("new release") || title.contains("nuove uscite")
                                val isVideoSection =
                                    title.contains("video") ||
                                        label.contains("video") ||
                                        section.items.isNotEmpty() &&
                                            section.items.all { it is SongItem && it.isVideoSong }
                                if (!isTrending && !isNewRelease && !isSimilar && !isVideoSection)
                                    return@feedSection
                                if (
                                    title.contains("listen again") ||
                                        label.contains("listen again") ||
                                        title.contains("long listens") ||
                                        title == accountName.lowercase() ||
                                        title.contains("picked for you") ||
                                        title.contains("picked 4u") ||
                                        title.contains("quick picks") ||
                                        label.contains("picked for you") ||
                                        (label.isBlank() && title in knownArtistNames)
                                )
                                    return@feedSection

                                val sectionItems =
                                    section.items
                                        .distinctBy { it.id }
                                        .filter { item ->
                                            val isMusicItem =
                                                item is SongItem ||
                                                    item is AlbumItem ||
                                                    item is ArtistItem
                                            isMusicItem &&
                                                (isVideoSection ||
                                                    !isNewRelease ||
                                                    item is AlbumItem &&
                                                        item.albumType?.lowercase() !in
                                                            setOf("single", "singolo") &&
                                                        item.artists.orEmpty().any { artist ->
                                                            artist.id?.let {
                                                                it in knownArtistIds
                                                            } == true
                                                        })
                                        }
                                if (
                                    label.isBlank() &&
                                        sectionItems.any { item ->
                                            when (item) {
                                                is SongItem ->
                                                    item.artists.any {
                                                        it.name.equals(
                                                            section.title,
                                                            ignoreCase = true,
                                                        )
                                                    }
                                                is AlbumItem ->
                                                    item.artists.orEmpty().any {
                                                        it.name.equals(
                                                            section.title,
                                                            ignoreCase = true,
                                                        )
                                                    }
                                                else -> false
                                            }
                                        }
                                )
                                    return@feedSection
                                val rawName =
                                    if (
                                        title.contains("similar to") || title.contains("simile a")
                                    ) {
                                        section.title
                                            .substringAfter(':', section.title)
                                            .replace(Regex("^(?i)(similar to|simile a)\\s*"), "")
                                            .trim()
                                    } else section.title
                                val name =
                                    rawName
                                        .replace(
                                            Regex("^(?i)(artist|artista|album|track|brano):\\s*"),
                                            "",
                                        )
                                        .trim()
                                val displayItems =
                                    if (isSimilar)
                                        sectionItems.filterNot { item ->
                                            when (item) {
                                                is SongItem ->
                                                    item.artists.any {
                                                        it.name.equals(name, ignoreCase = true)
                                                    }
                                                is AlbumItem ->
                                                    item.title.equals(name, ignoreCase = true) ||
                                                        item.artists.orEmpty().any {
                                                            it.name.equals(name, ignoreCase = true)
                                                        }
                                                is ArtistItem ->
                                                    item.title.equals(name, ignoreCase = true)
                                                else -> false
                                            }
                                        }
                                    else sectionItems
                                if (
                                    displayItems.isEmpty() ||
                                        !isVideoSection && displayItems.size < 4
                                )
                                    return@feedSection
                                val matchedArtist =
                                    sectionItems.filterIsInstance<ArtistItem>().firstOrNull {
                                        it.title.equals(name, ignoreCase = true)
                                    }
                                val matchedAlbum =
                                    sectionItems.filterIsInstance<AlbumItem>().firstOrNull {
                                        it.title.equals(name, ignoreCase = true)
                                    }
                                val isAlbumSource =
                                    title.contains("album:") ||
                                        label.contains("album") ||
                                        matchedAlbum != null
                                val sourceThumbnail =
                                    section.thumbnail
                                        ?: matchedArtist?.thumbnail
                                        ?: matchedAlbum?.thumbnail
                                val sourceArtistId =
                                    matchedArtist?.id
                                        ?: section.endpoint?.browseId?.takeIf {
                                            it.startsWith("UC")
                                        }
                                        ?: sectionItems
                                            .asSequence()
                                            .flatMap { item ->
                                                when (item) {
                                                    is SongItem -> item.artists.asSequence()
                                                    is AlbumItem ->
                                                        item.artists.orEmpty().asSequence()
                                                    else -> emptySequence()
                                                }
                                            }
                                            .firstOrNull { it.name.equals(name, ignoreCase = true) }
                                            ?.id
                                if (
                                    isSimilar &&
                                        (name.isBlank() ||
                                            sourceThumbnail.isNullOrBlank() &&
                                                (isAlbumSource || sourceArtistId == null))
                                )
                                    return@feedSection
                                shelfOrder.added(hasFourRows = false)
                                val onSectionClick =
                                    section.endpoint?.let { endpoint ->
                                        {
                                            when {
                                                endpoint.browseId == "FEmusic_moods_and_genres" ->
                                                    navController.navigate("mood_and_genres")
                                                endpoint.params != null ->
                                                    navController.navigate(
                                                        "youtube_browse/${endpoint.browseId}?params=${endpoint.params}"
                                                    )
                                                else ->
                                                    navController.navigate(
                                                        "browse/${endpoint.browseId}"
                                                    )
                                            }
                                            Unit
                                        }
                                    }
                                item(key = "home_section_$index") {
                                    IrideFolioSection(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec =
                                                    if (homeReducedMotion) null
                                                    else IrideMotion.PlacementSpec,
                                                fadeOutSpec = null,
                                            ),
                                        sectionKey = "home_section_$index",
                                        artwork =
                                            displayItems.take(4).map {
                                                it.thumbnail?.resize(448, 448)
                                            },
                                        placeholder = {
                                            IrideFolioSectionSkeleton(
                                                currentGridHeight,
                                                songs =
                                                    displayItems.any {
                                                        it is SongItem && !it.isVideoSong
                                                    },
                                            )
                                        },
                                    ) {
                                        if (isSimilar) {
                                            HomeSimilarityTitle(
                                                name = name,
                                                thumbnailUrl = sourceThumbnail,
                                                artistId = sourceArtistId,
                                                isArtist = !isAlbumSource,
                                                onClick = onSectionClick,
                                            )
                                        } else {
                                            IrideFolioShelfHeader(
                                                title =
                                                    if (isVideoSection) section.title
                                                    else
                                                        stringResource(
                                                            if (isTrending)
                                                                R.string.home_trending_title
                                                            else R.string.home_new_albums_title
                                                        ),
                                                onClick = onSectionClick,
                                                showNavigationArrow = false,
                                            )
                                        }

                                        IrideFolioMediaShelf(
                                            items =
                                                if (isVideoSection)
                                                    displayItems.map { item ->
                                                        if (item is SongItem && !item.isVideoSong)
                                                            item.copy(
                                                                musicVideoType =
                                                                    "MUSIC_VIDEO_TYPE_OMV"
                                                            )
                                                        else item
                                                    }
                                                else displayItems,
                                            songContent = ytSongItem,
                                            tileContent = ytGridItem,
                                        )
                                    }
                                }
                            }
                        if (isLoggedIn) {
                            shelfOrder.added(hasFourRows = false)
                            item(key = "your_mood_section") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec =
                                                if (homeReducedMotion) null
                                                else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "your_mood_section",
                                    ready = isMoodPreferenceReady,
                                    placeholder = {
                                        IrideFolioSectionSkeleton(
                                            currentGridHeight,
                                            controls = true,
                                        )
                                    },
                                ) {
                                    IrideFolioShelfHeader(
                                        title = stringResource(R.string.home_mood_youtube_music),
                                        showNavigationArrow = false,
                                    )

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (moodChips.isNotEmpty()) {
                                            IrideMoodChipsRow(
                                                chips = moodChips,
                                                currentValue = selectedMoodCategory,
                                                onValueUpdate = { chip ->
                                                    if (chip == selectedMoodCategory) {
                                                        viewModel.loadMoodPage(
                                                            chip.endpoint?.params,
                                                            chip.title,
                                                            hideExplicit,
                                                            hideVideoSongs,
                                                            hideYoutubeShorts,
                                                            moodPosterSize,
                                                            forceReload = true,
                                                        )
                                                    } else selectedMoodCategory = chip
                                                },
                                                horizontalPadding = irideStart,
                                            )

                                        }
                                        val mixItems = moodMixItems.orEmpty()
                                        Box(
                                            Modifier.fillMaxWidth()
                                                .height(currentGridHeight + 64.dp)
                                        ) {
                                            LazyRow(
                                                state = moodMixesState,
                                                contentPadding =
                                                    PaddingValues(horizontal = irideGridItemStart),
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(
                                                        IrideFolio.ShelfItemSpacing
                                                    ),
                                                overscrollEffect = null,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                                        .height(currentGridHeight + 64.dp)
                                                        .padding(bottom = 4.dp)
                                                        .rubberBandOverscroll(
                                                            Orientation.Horizontal,
                                                            moodMixesState,
                                                        ),
                                            ) {
                                                items(mixItems, key = { it.id }) { mix ->
                                                    YouTubeGridItem(
                                                        item = mix,
                                                        thumbnailUrlOverride =
                                                            moodPosterUrls[mix.id] ?: mix.thumbnail,
                                                        thumbnailImageLoader =
                                                            if (moodPosterUrls[mix.id] != null)
                                                                viewModel.moodPosterImageLoader
                                                            else null,
                                                        useThumbnailOverride = true,
                                                        thumbnailPlaceholder = {
                                                            MixPosterPlaceholder(
                                                                title = mix.title,
                                                                mood =
                                                                    MixMood.fromMixTitle(mix.title)
                                                                        ?: MixMood.fromChip(
                                                                            moodChips
                                                                                .firstOrNull {
                                                                                    it.first
                                                                                        .endpoint
                                                                                        ?.params ==
                                                                                        loadedMoodParams
                                                                                }
                                                                                ?.second
                                                                        )
                                                                        ?: MixMood.CHILL,
                                                            )
                                                        },
                                                        isActive =
                                                            mix.id ==
                                                                mediaMetadataState.value?.album?.id,
                                                        isPlaying = isPlayingState.value,
                                                        coroutineScope = scope,
                                                        thumbnailRatio = 1f,
                                                        size = currentGridHeight,
                                                        showTitle = true,
                                                        modifier =
                                                            Modifier.irideFolioCombinedClickable(
                                                                onClick = {
                                                                    navController.navigate(
                                                                        "online_playlist/${mix.id}"
                                                                    )
                                                                },
                                                                onLongClick = {
                                                                    menuState.show(
                                                                        anchored = true
                                                                    ) {
                                                                        YouTubePlaylistMenu(
                                                                            playlist = mix,
                                                                            coroutineScope = scope,
                                                                            onDismiss =
                                                                                menuState::dismiss,
                                                                        )
                                                                    }
                                                                },
                                                            ),
                                                    )
                                                }
                                            }
                                            when {
                                                mixItems.isEmpty() && moodChips.isEmpty() &&
                                                    isMoodPreferenceReady && !isHomeFeedLoading ->
                                                    Text(
                                                        text = stringResource(R.string.home_mood_empty),
                                                        color = MaterialTheme.colorScheme.textSecondary,
                                                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                                                    )
                                                mixItems.isEmpty() &&
                                                    (isMoodLoading ||
                                                        isHomeFeedLoading ||
                                                        !isMoodPreferenceReady ||
                                                        selectedMoodCategory == null) ->
                                                    IrideFolioShelfSkeleton(currentGridHeight)
                                                isMoodError && mixItems.isEmpty() ->
                                                    Column(
                                                        modifier =
                                                            Modifier.align(Alignment.Center)
                                                                .padding(horizontal = 24.dp),
                                                        horizontalAlignment =
                                                            Alignment.CenterHorizontally,
                                                    ) {
                                                        Text(
                                                            text =
                                                                stringResource(
                                                                    R.string.home_mood_load_error
                                                                ),
                                                            color =
                                                                MaterialTheme.colorScheme
                                                                    .textSecondary,
                                                        )
                                                        TextButton(
                                                            onClick = {
                                                                selectedMoodCategory?.let { chip ->
                                                                    viewModel.loadMoodPage(
                                                                        chip.endpoint?.params,
                                                                        chip.title,
                                                                        hideExplicit,
                                                                        hideVideoSongs,
                                                                        hideYoutubeShorts,
                                                                        moodPosterSize,
                                                                        forceReload = true,
                                                                    )
                                                                }
                                                            }
                                                        ) {
                                                            Text(
                                                                stringResource(
                                                                    R.string.home_mood_retry
                                                                )
                                                            )
                                                        }
                                                    }
                                                mixItems.isEmpty() ->
                                                    Text(
                                                        text =
                                                            stringResource(
                                                                R.string.home_mood_empty
                                                            ),
                                                        color =
                                                            MaterialTheme.colorScheme.textSecondary,
                                                        modifier =
                                                            Modifier.align(Alignment.Center)
                                                                .padding(horizontal = 24.dp),
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (SHOW_HOME_TEST_PLAYLIST) testPlaylist?.let { playlist ->
                                item(key = "home_test_playlist_section") {
                                    IrideFolioSection(
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null
                                                else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                        sectionKey = "home_test_playlist_section",
                                        ready = true,
                                    ) {
                                        IrideFolioShelfHeader(
                                            title = stringResource(R.string.home_test_section),
                                            showNavigationArrow = false,
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                            horizontalArrangement = Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                                            overscrollEffect = null,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            item(key = "home_test_playlist_${playlist.id}") {
                                                val artwork = testPlaylistTrackThumbnails
                                                val collectionTile = rememberCollectionExpansionTile(
                                                    CollectionExpansionKind.Playlist, playlist.id,
                                                    artworkUrl = artwork.firstOrNull() ?: playlist.thumbnail, title = playlist.title,
                                                )
                                                Column(
                                                    modifier = Modifier.width(currentGridHeight)
                                                        .then(collectionTile.first)
                                                        .irideFolioCombinedClickable(
                                                            onClick = {
                                                                navController.navigate("online_playlist/${playlist.id}")
                                                            },
                                                        ),
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(currentGridHeight)
                                                            .then(collectionTile.second)
                                                            .clip(FolioShape(8.dp)),
                                                    ) {
                                                        if (artwork.isEmpty()) {
                                                            AsyncImage(
                                                                model = playlist.thumbnail,
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize(),
                                                            )
                                                        } else {
                                                            Column(Modifier.fillMaxSize()) {
                                                                Row(Modifier.weight(1f).fillMaxWidth()) {
                                                                    AsyncImage(
                                                                        model = artwork[0],
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                                                    )
                                                                    AsyncImage(
                                                                        model = artwork.getOrElse(1) { artwork[0] },
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                                                    )
                                                                }
                                                                Row(Modifier.weight(1f).fillMaxWidth()) {
                                                                    AsyncImage(
                                                                        model = artwork.getOrElse(2) { artwork[0] },
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                                                    )
                                                                    AsyncImage(
                                                                        model = artwork.getOrElse(3) { artwork[0] },
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Box(
                                                            modifier = Modifier.fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.34f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = stringResource(R.string.home_test_mood_label),
                                                                    color = Color.White,
                                                                    fontFamily = SatoshiFontFamily,
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 24.sp * IrideFolio.SatoshiTextHeightScale,
                                                                    letterSpacing = (-0.6).sp,
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 1,
                                                                )
                                                                Text(
                                                                    text = stringResource(R.string.home_test_mood_type),
                                                                    color = Color.White.copy(alpha = 0.92f),
                                                                    fontFamily = SatoshiFontFamily,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 10.sp * IrideFolio.SatoshiTextHeightScale,
                                                                    letterSpacing = 3.sp,
                                                                    textAlign = TextAlign.Center,
                                                                )
                                                            }
                                                        }
                                                        Row(
                                                            modifier = Modifier.align(Alignment.TopEnd)
                                                                .padding(top = 4.dp, end = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        ) {
                                                            Image(
                                                                painter = painterResource(R.drawable.ic_logo),
                                                                contentDescription = null,
                                                                colorFilter = ColorFilter.tint(Color.White),
                                                                modifier = Modifier.size(14.dp),
                                                            )
                                                            Text(
                                                                text = stringResource(R.string.home_test_brand_label),
                                                                color = Color.White,
                                                                fontFamily = SatoshiFontFamily,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 10.sp * IrideFolio.SatoshiTextHeightScale,
                                                                letterSpacing = 0.2.sp,
                                                                maxLines = 1,
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = playlist.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 8.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        forYouSection(shelfOrder)
                        if (homePage?.sections.isNullOrEmpty()) {
                            item(key = "home_feed_status") {
                                if (isHomeFeedLoading) {
                                    HomeFeedLoadingShelf(currentGridHeight)
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_feed_unavailable),
                                            color = MaterialTheme.colorScheme.textSecondary,
                                        )
                                        TextButton(onClick = { viewModel.loadHomeData() }) {
                                            Text(stringResource(R.string.home_mood_retry))
                                        }
                                    }
                                }
                            }
                        }
                        if (isLoggedIn) shelfOrder.added(hasFourRows = false)
                        if (isLoggedIn)
                            item(key = "home_personal_mixes_section") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec =
                                                if (homeReducedMotion) null
                                                else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "home_personal_mixes_section",
                                    artwork =
                                        homePersonalMixes.take(2).map {
                                            it.thumbnail?.resize(448, 448)
                                        },
                                    placeholder = { IrideFolioSectionSkeleton(currentGridHeight) },
                                ) {
                                    IrideFolioShelfHeader(
                                        title =
                                            stringResource(R.string.home_fresh_find_old_favorite),
                                        showNavigationArrow = false,
                                    )

                                    val personalMixesState = rememberLazyListState()
                                    val starredPlaylistName = stringResource(R.string.starred)
                                    val starredPlaylist =
                                        remember(starredPlaylistName, starredPlaylistThumbnails) {
                                            Playlist(
                                                playlist =
                                                    PlaylistEntity(
                                                        id = PlaylistEntity.LIKED_PLAYLIST_ID,
                                                        name = starredPlaylistName,
                                                    ),
                                                songCount = 0,
                                                songThumbnails = starredPlaylistThumbnails,
                                            )
                                        }
                                    LazyRow(
                                        state = personalMixesState,
                                        contentPadding =
                                            PaddingValues(horizontal = irideGridItemStart),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                                        overscrollEffect = null,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .rubberBandOverscroll(
                                                    Orientation.Horizontal,
                                                    personalMixesState,
                                                ),
                                    ) {
                                        item(key = "home_personal_mixes_starred") {
                                            PlaylistGridItem(
                                                playlist = starredPlaylist,
                                                autoPlaylist = true,
                                                modifier =
                                                    Modifier.irideFolioCombinedClickable(
                                                        onClick = {
                                                            navController.navigate(
                                                                "auto_playlist/liked"
                                                            )
                                                        },
                                                        onLongClick = {
                                                            menuState.show(anchored = true) {
                                                                PlaylistMenu(
                                                                    playlist = starredPlaylist,
                                                                    coroutineScope = scope,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ),
                                            )
                                        }
                                        items(
                                            homePersonalMixes,
                                            key = { "home_personal_mix_${it.id}" },
                                        ) { mix ->
                                            ytGridItem(mix)
                                        }
                                    }
                                }
                            }
                        val feedSection: LazyListScope.() -> Unit = {
                            orderedFeedSections.forEach { addFeedSection(it) }
                        }

                        val dailySection: LazyListScope.() -> Unit = {
                            dailyDiscover
                                ?.distinctBy { it.recommendation.id }
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { discoverList ->
                                    shelfOrder.added(hasFourRows = false)
                                    item(key = "daily_discover_section") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec =
                                                        if (homeReducedMotion) null
                                                        else IrideMotion.PlacementSpec,
                                                    fadeOutSpec = null,
                                                ),
                                            sectionKey = "daily_discover_section",
                                            artwork =
                                                discoverList.take(4).map {
                                                    it.recommendation.thumbnail?.resize(448, 448)
                                                },
                                            placeholder = {
                                                IrideFolioSectionSkeleton(
                                                    currentGridHeight,
                                                    songs = true,
                                                )
                                            },
                                        ) {
                                            val title = stringResource(R.string.your_daily_discover)
                                            IrideFolioShelfHeader(
                                                title = title,
                                                showNavigationArrow = false,
                                            )

                                            SharedQuickPicksCarousel(
                                                items = discoverList,
                                                key = { it.recommendation.id },
                                                loading = false,
                                                contentPadding = irideListItemStart - 4.dp,
                                                usePager = true,
                                                minimumRows = 1,
                                            ) { discovery, itemWidth ->
                                                (discovery.recommendation as? SongItem)?.let { song
                                                    ->
                                                    ytSongItem(song, itemWidth)
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        val communitySection: LazyListScope.() -> Unit = {
                            communityPlaylists
                                ?.distinctBy { it.playlist.id }
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { playlists ->
                                    shelfOrder.added(hasFourRows = false)
                                    item(key = "community_playlists_section") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec =
                                                        if (homeReducedMotion) null
                                                        else IrideMotion.PlacementSpec,
                                                    fadeOutSpec = null,
                                                ),
                                            sectionKey = "community_playlists_section",
                                            artwork =
                                                playlists.take(2).map {
                                                    it.playlist.thumbnail?.resize(448, 448)
                                                },
                                            placeholder = {
                                                IrideFolioSectionSkeleton(currentGridHeight)
                                            },
                                        ) {
                                            IrideFolioShelfHeader(
                                                title = stringResource(R.string.from_the_community),
                                                showNavigationArrow = false,
                                            )

                                            val communityPlaylistsState = rememberLazyListState()
                                            LazyRow(
                                                state = communityPlaylistsState,
                                                contentPadding =
                                                    PaddingValues(horizontal = irideGridItemStart),
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(
                                                        IrideFolio.ShelfItemSpacing
                                                    ),
                                                overscrollEffect = null,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                                        .rubberBandOverscroll(
                                                            Orientation.Horizontal,
                                                            communityPlaylistsState,
                                                        ),
                                            ) {
                                                items(
                                                    playlists,
                                                    key = {
                                                        "community_playlist_${it.playlist.id}"
                                                    },
                                                ) { cpItem ->
                                                    PlaylistGridItem(
                                                        playlist =
                                                            Playlist(
                                                                playlist =
                                                                    PlaylistEntity(
                                                                        id =
                                                                            cpItem.playlist.id
                                                                                .removePrefix("VL"),
                                                                        name =
                                                                            cpItem.playlist.title,
                                                                        browseId =
                                                                            cpItem.playlist.id
                                                                                .removePrefix("VL"),
                                                                        thumbnailUrl =
                                                                            cpItem.playlist
                                                                                .thumbnail,
                                                                    ),
                                                                songCount = 0,
                                                                songThumbnails = emptyList(),
                                                            ),
                                                        autoPlaylist = false,
                                                        modifier =
                                                            Modifier.irideFolioCombinedClickable(
                                                                onClick = {
                                                                    navController.navigate(
                                                                        "online_playlist/${cpItem.playlist.id.removePrefix("VL")}"
                                                                    )
                                                                },
                                                                onLongClick = {
                                                                    menuState.show(anchored = true) {
                                                                        YouTubePlaylistMenu(
                                                                            playlist = cpItem.playlist,
                                                                            coroutineScope = scope,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }
                                                                },
                                                            ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        val similarSection: LazyListScope.() -> Unit = {
                            similarRecommendations?.forEachIndexed { index, rec ->
                                val similarItems =
                                    rec.items
                                        .distinctBy { it.id }
                                        .filter { item ->
                                            item is SongItem && !item.isVideoSong ||
                                                item is AlbumItem ||
                                                item is ArtistItem ||
                                                item is PlaylistItem
                                        }
                                        .sortedBy { item -> item is ArtistItem }
                                if (
                                    similarItems.size < 2 ||
                                        rec.title !is Artist && rec.thumbnailUrl.isNullOrBlank()
                                )
                                    return@forEachIndexed
                                shelfOrder.added(hasFourRows = false)
                                item(key = "similar_to_${rec.title.id}") {
                                    IrideFolioSection(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec =
                                                    if (homeReducedMotion) null
                                                    else IrideMotion.PlacementSpec,
                                                fadeOutSpec = null,
                                            ),
                                        sectionKey = "similar_to_${rec.title.id}",
                                        artwork =
                                            similarItems.take(4).map {
                                                it.thumbnail?.resize(448, 448)
                                            },
                                        placeholder = {
                                            IrideFolioSectionSkeleton(
                                                currentGridHeight,
                                                songs =
                                                    similarItems.any {
                                                        it is SongItem && !it.isVideoSong
                                                    },
                                            )
                                        },
                                    ) {
                                        HomeSimilarityTitle(
                                            name = rec.title.title,
                                            thumbnailUrl = rec.thumbnailUrl,
                                            artistId = (rec.title as? Artist)?.id,
                                            isArtist = rec.title is Artist,
                                            onClick = {
                                                when (rec.title) {
                                                    is Song ->
                                                        navController.navigate(
                                                            "album/${rec.title.album!!.id}"
                                                        )
                                                    is Album ->
                                                        navController.navigate(
                                                            "album/${rec.title.id}"
                                                        )
                                                    is Artist ->
                                                        navController.navigate(
                                                            "artist/${rec.title.id}"
                                                        )
                                                    is Playlist ->
                                                        rec.title.playlist.browseId?.let {
                                                            navController.navigate(
                                                                "online_playlist/${it.removePrefix("VL")}"
                                                            )
                                                        }
                                                }
                                            },
                                        )

                                        IrideFolioMediaShelf(
                                            items = similarItems,
                                            songContent = ytSongItem,
                                            tileContent = ytGridItem,
                                        )
                                    }
                                }
                            }
                        }

                        val accountSection: LazyListScope.() -> Unit = {
                            accountPlaylists
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { playlists ->
                                    shelfOrder.added(hasFourRows = false)
                                    item(key = "account_playlists_section") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec =
                                                        if (homeReducedMotion) null
                                                        else IrideMotion.PlacementSpec,
                                                    fadeOutSpec = null,
                                                ),
                                            sectionKey = "account_playlists_section",
                                            artwork =
                                                playlists.take(2).map {
                                                    it.thumbnail?.resize(448, 448)
                                                },
                                            placeholder = {
                                                IrideFolioSectionSkeleton(currentGridHeight)
                                            },
                                        ) {
                                            SharedHomeSectionTitle(
                                                title =
                                                    stringResource(
                                                        R.string.home_youtube_music_playlists
                                                    ),
                                                onClick = { navController.navigate("account") },
                                            )

                                            val shuffledPlaylists =
                                                remember(playlists) {
                                                    playlists.distinctBy { it.id }.shuffled()
                                                }
                                            SharedYouTubePlaylistRow(
                                                items = shuffledPlaylists,
                                                key = { "home_account_playlist_${it.id}" },
                                                contentPadding = irideGridItemStart,
                                                modifier = Modifier,
                                                itemContent = { playlist -> ytGridItem(playlist) },
                                            )
                                        }
                                    }
                                }
                        }

                        val addFollowedReleases: LazyListScope.() -> Unit = {
                            shelfOrder.added(hasFourRows = true)
                            item(key = "home_followed_releases") {
                                val isVisible by
                                    remember(lazyListState) {
                                        derivedStateOf {
                                            val layout = lazyListState.layoutInfo
                                            layout.visibleItemsInfo.any {
                                                it.key == "home_followed_releases" &&
                                                    it.offset + it.size >
                                                        layout.viewportStartOffset &&
                                                    it.offset < layout.viewportEndOffset
                                            }
                                        }
                                    }
                                HomeFollowedReleases(
                                    navController = navController,
                                    snackbarHostState = snackbarHostState,
                                    isVisible = isVisible && phase1Complete,
                                )
                            }
                        }
                        var followedReleasesAdded = false
                        val sectionReadiness =
                            listOf(
                                true,
                                dailyDiscoverComplete || !dailyDiscover.isNullOrEmpty(),
                                communityComplete || !communityPlaylists.isNullOrEmpty(),
                                similarComplete || !similarRecommendations.isNullOrEmpty(),
                                !isLoggedIn ||
                                    !isAccountPlaylistsLoading ||
                                    !accountPlaylists.isNullOrEmpty(),
                            )
                        for ((index, section) in
                            listOf(
                                    feedSection,
                                    dailySection,
                                    communitySection,
                                    similarSection,
                                    accountSection,
                                )
                                .withIndex()) {
                            section()
                            if (!sectionReadiness[index]) {
                                item(key = "home_pending_section_$index") {
                                    IrideFolioSection(
                                        sectionKey = "home_pending_section_$index",
                                        ready = false,
                                        placeholder = {
                                            IrideFolioSectionSkeleton(
                                                currentGridHeight,
                                                songs = index == 1 || index == 3,
                                            )
                                        },
                                    ) {}
                                }
                            }
                            if (
                                !followedReleasesAdded &&
                                    index + 1 >= followedReleasesPosition &&
                                    shelfOrder.canAdd(hasFourRows = true)
                            ) {
                                addFollowedReleases()
                                followedReleasesAdded = true
                            }
                        }

                        if (!followedReleasesAdded) {
                            shelfOrder.added(hasFourRows = false)
                            addFollowedReleases()
                        }
                        item(key = "home_charts") {
                            NewReleaseChartsShelf(navController, chartViewModel, preload = false)
                        }
                        item(key = "reverie_section") {
                            IrideFolioSection(
                                sectionKey = "reverie_section",
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = null,
                                        placementSpec =
                                            if (homeReducedMotion) null
                                            else IrideMotion.PlacementSpec,
                                        fadeOutSpec = null,
                                    ),
                            ) {
                                SharedHomeSectionTitle(
                                    title = stringResource(R.string.reverie_archive_title)
                                )

                                if (reverieMonths.isNotEmpty()) {
                                    val archiveState = rememberLazyListState()
                                    LazyRow(
                                        state = archiveState,
                                        overscrollEffect = null,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .padding(bottom = 24.dp)
                                                .rubberBandOverscroll(
                                                    Orientation.Horizontal,
                                                    archiveState,
                                                ),
                                        contentPadding =
                                            PaddingValues(
                                                horizontal = IrideFolio.HorizontalPadding
                                            ),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                                    ) {
                                        items(reverieMonths, key = { it.toString() }) { month ->
                                            ReverieHomeCard(
                                                month = month,
                                                onClick = {
                                                    navController.navigate("reverie/$month")
                                                },
                                                modifier = Modifier.width(300.dp),
                                            )
                                        }
                                    }
                                } else {
                                    com.metrolist.music.ui.component.ReverieAvailabilityCard(
                                        java.time.YearMonth.now(),
                                        firstFullReverieMonth,
                                        Modifier.padding(horizontal = IrideFolio.HorizontalPadding)
                                            .padding(bottom = 24.dp),
                                    )
                                }
                            }
                        }
                        item(key = "iride_features_section") {
                            IrideFolioSection(
                                sectionKey = "iride_features_section",
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = null,
                                        placementSpec =
                                            if (homeReducedMotion) null
                                            else IrideMotion.PlacementSpec,
                                        fadeOutSpec = null,
                                    ),
                            ) {
                                SharedHomeSectionTitle(
                                    title = stringResource(R.string.home_iride_features_title)
                                )

                                val featuresState = rememberLazyListState()
                                LazyRow(
                                    state = featuresState,
                                    overscrollEffect = null,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(bottom = 24.dp)
                                            .rubberBandOverscroll(
                                                Orientation.Horizontal,
                                                featuresState,
                                            ),
                                    contentPadding =
                                        PaddingValues(horizontal = IrideFolio.HorizontalPadding),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                                ) {
                                    items(2) { index ->
                                        if (index == 0) {
                                            IrideFeatureCard(
                                                title =
                                                    stringResource(
                                                        R.string.home_iride_feature_stats_title
                                                    ),
                                                subtitle =
                                                    stringResource(
                                                        R.string
                                                            .home_iride_feature_stats_description
                                                    ),
                                                iconRes = R.drawable.stats,
                                                onClick = { navController.navigate("stats") },
                                                modifier = Modifier.width(300.dp),
                                            )
                                        } else {
                                            IrideFeatureCard(
                                                title =
                                                    stringResource(
                                                        R.string.home_iride_feature_spotify_title
                                                    ),
                                                subtitle =
                                                    stringResource(
                                                        R.string
                                                            .home_iride_feature_spotify_description
                                                    ),
                                                iconRes = R.drawable.playlist_add,
                                                onClick = {
                                                    navController.navigate("music_import")
                                                },
                                                modifier = Modifier.width(300.dp),
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
}

@Composable
private fun HomeFeedLoadingShelf(thumbnailSize: Dp) {
    IrideFolioSection(
        sectionKey = "home_feed_loading",
        ready = false,
        placeholder = { IrideFolioSectionSkeleton(thumbnailSize, songs = true) },
    ) {}
}

@Composable
private fun IrideFeatureCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = FolioShape(6.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeCard),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(112.dp)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.strokeCard,
                            FolioShape(5.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textSecondary,
                )
            }
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SyncBanner(syncState: SyncState, useIrideStyle: Boolean = false) {
    val ops =
        listOf(
            syncState.likedSongs,
            syncState.librarySongs,
            syncState.uploadedSongs,
            syncState.likedAlbums,
            syncState.uploadedAlbums,
            syncState.artists,
            syncState.playlists,
        )
    val completedCount = ops.count { it == SyncStatus.Completed }
    val progress = completedCount / ops.size.toFloat()

    if (useIrideStyle) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.textSecondary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.syncing_library).uppercase(),
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontFamily = folioTypewriterFontFamily(),
                                fontSize = 13.sp,
                                letterSpacing = (-0.1).sp,
                            ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.network_sync_warning),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = folioTypewriterFontFamily()
                            ),
                        color = MaterialTheme.colorScheme.textTertiary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.textSecondary,
                trackColor = MaterialTheme.colorScheme.fillSelected,
            )
            if (syncState.currentOperation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.currentOperation,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontFamily = folioTypewriterFontFamily()
                        ),
                    color = MaterialTheme.colorScheme.textTertiary,
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = FolioShape(20.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
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
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
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
