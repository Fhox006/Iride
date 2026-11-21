/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import com.metrolist.music.ui.theme.CollectionArtworkShape
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.metrolist.music.ui.component.LocalArtistPortraitNavigation
import com.metrolist.music.ui.component.LocalIrideExpansionBackdrop
import com.metrolist.music.ui.component.rememberArtistPortraitTile
import com.metrolist.music.ui.component.rememberCollectionExpansionTile
import com.metrolist.music.ui.component.CollectionExpansionKind
import com.metrolist.music.ui.component.rememberIrideArtistArtwork
import com.metrolist.music.ui.component.rememberArtistArtworkLoader
import com.metrolist.music.ui.component.ArtistArtworkRequest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
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
import com.metrolist.music.BuildConfig
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
import com.metrolist.music.discovery.MoodMixes
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.ForYouShelfTile
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HomeFollowedReleases
import com.metrolist.music.ui.component.PreloadHomeFollowedReleases
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioDisplayHeadings
import com.metrolist.music.ui.component.IrideFolioMediaShelf
import com.metrolist.music.ui.component.IrideFolioSection
import com.metrolist.music.ui.component.LocalFolioSectionFastReveal
import com.metrolist.music.ui.component.rememberLightweightFolioMode
import com.metrolist.music.ui.component.IrideFolioSectionSkeleton
import com.metrolist.music.ui.component.IrideFolioShelfHeader
import com.metrolist.music.ui.component.IrideFolioShelfSkeleton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.LocalHomeContentSnapshot
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.PlaylistArtwork
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.ReverieHomeCard
import com.metrolist.music.ui.component.SharedHomeSectionTitle
import com.metrolist.music.ui.component.SharedQuickPicksCarousel
import com.metrolist.music.ui.component.SharedYouTubePlaylistRow
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TrackOverflowIcon
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.utils.UpdateDownloadState
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
import com.metrolist.music.ui.theme.fillSelected
import com.metrolist.music.ui.theme.SatoshiFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.viewmodels.prefetchArtistPage
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.irideDepthEnter
import com.metrolist.music.ui.utils.rememberEnterProgressState
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.SyncState
import com.metrolist.music.utils.SyncStatus
import com.metrolist.music.utils.appendLoadedHomeSections
import com.metrolist.music.utils.isInternetAvailable
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.FollowedReleasesViewModel
import com.metrolist.music.viewmodels.HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED
import com.metrolist.music.viewmodels.HOME_MOOD_SECTION_ENABLED
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.MonthlyReverieViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

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
                val moodLabel =
                    when (MixMood.fromChip(label)) {
                        MixMood.CHILL -> stringResource(R.string.mood_chill)
                        MixMood.FOCUS -> stringResource(R.string.mood_focus)
                        MixMood.ENERGY -> stringResource(R.string.mood_energy)
                        MixMood.FEEL_GOOD -> stringResource(R.string.mood_feel_good)
                        MixMood.PARTY -> stringResource(R.string.mood_party)
                        MixMood.WORKOUT -> stringResource(R.string.mood_workout)
                        MixMood.ROMANTIC -> stringResource(R.string.mood_romantic)
                        MixMood.SAD -> stringResource(R.string.mood_sad)
                        MixMood.SLEEP -> stringResource(R.string.mood_sleep)
                        null -> label
                    }
                Text(
                    text = moodLabel,
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
    albumId: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = if (isArtist) CircleShape else FolioShape(5.dp)
    val resolvedThumbnail =
        if (isArtist && artistId != null) {
            rememberArtistThumbnail(artistId, name, thumbnailUrl)
        } else thumbnailUrl
    val portraitTile = if (isArtist && artistId != null && onClick != null) {
        rememberArtistPortraitTile(artistId, name, resolvedThumbnail, null, true)
    } else Modifier to Modifier
    val collectionTile = if (!isArtist && albumId != null && onClick != null) {
        rememberCollectionExpansionTile(
            CollectionExpansionKind.Album,
            albumId,
            artworkUrl = resolvedThumbnail,
            title = name,
        )
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
                modifier = Modifier.size(48.dp).then(portraitTile.second).then(collectionTile.second),
            )
        },
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = portraitTile.first.then(collectionTile.first).then(modifier),
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
    updateVersion: String?,
    updateDownloadState: UpdateDownloadState,
    onUpdateClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activityLifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
    DisposableEffect(activityLifecycle, viewModel) {
        var wasBackgrounded = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> wasBackgrounded = true
                Lifecycle.Event.ON_START -> if (wasBackgrounded) {
                    wasBackgrounded = false
                    viewModel.regenerateForYouShelves()
                }
                else -> Unit
            }
        }
        activityLifecycle?.addObserver(observer)
        onDispose { activityLifecycle?.removeObserver(observer) }
    }
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
    CompositionLocalProvider(
        LocalHomeContentSnapshot provides viewModel.contentSnapshot,
        LocalFolioSectionFastReveal provides true,
    ) {
        HomeScreenContent(
            navController,
            snackbarHostState,
            viewModel,
            updateVersion,
            updateDownloadState,
            onUpdateClick,
        )
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
    updateVersion: String?,
    updateDownloadState: UpdateDownloadState,
    onUpdateClick: () -> Unit,
) {
    val menuState = com.metrolist.music.ui.component.LocalMenuState.current
    val reverieViewModel: MonthlyReverieViewModel = hiltViewModel()
    val reverieMonths by reverieViewModel.archiveMonths.collectAsStateWithLifecycle()
    val firstFullReverieMonth by reverieViewModel.firstFullMonth.collectAsStateWithLifecycle()
    val followedReleasesViewModel: FollowedReleasesViewModel =
        hiltViewModel()
    PreloadHomeFollowedReleases(followedReleasesViewModel)
    val followedReleaseTracks by followedReleasesViewModel.tracks.collectAsStateWithLifecycle()
    val followedReleasesPosition = rememberSaveable { kotlin.random.Random.nextInt(4, 6) }
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
        viewModel.communityPlaylists.collectDeferredHomeState(
            key = "community_playlists",
            applyPendingWhenHidden = false,
        ) {
            it.orEmpty().distinctBy { item -> item.playlist.id }.size >= 4
        }
    val dailyDiscover by
        viewModel.dailyDiscover.collectDeferredHomeState(
            key = "daily_discover",
            applyPendingWhenHidden = false,
        ) {
            it.orEmpty().distinctBy { item -> item.recommendation.id }.size >= 4
        }
    val accountPlaylists by
        viewModel.accountPlaylists.collectDeferredHomeState("account_playlists") {
            it.orEmpty().distinctBy { item -> item.id }.size >= 4
        }
    val homePersonalMixesState =
        if (HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED) {
            viewModel.homePersonalMixes.collectDeferredHomeState("personal_mixes")
        } else {
            remember { mutableStateOf(emptyList<PlaylistItem>()) }
        }
    val homePersonalMixes by homePersonalMixesState
    val similarRecommendations by
        viewModel.similarRecommendations.collectDeferredHomeState(
            key = "similar_recommendations",
            mergeWhileVisible = { current, incoming ->
                appendLoadedHomeSections(current.orEmpty(), incoming.orEmpty()) { it.title.id }
            },
        )
    val quickPicks by
        viewModel.quickPicks.collectDeferredHomeState(
            key = "quick_picks",
            mergeWhileVisible = { current, _ -> current },
            applyPendingWhenHidden = false,
        ) {
            it.orEmpty()
                .filterNot { song -> song.song.isVideo || song.song.isEpisode }
                .distinctBy { song -> song.id }
                .size >= 4
        }
    val forYouShelves by viewModel.forYouShelves.collectAsStateWithLifecycle()
    val homePage by
        viewModel.homePage.collectDeferredHomeState("feed") { !it?.sections.isNullOrEmpty() }
    val isHeroCarouselEnabled by viewModel.isHeroCarouselEnabled.collectAsStateWithLifecycle()
    val isHeroCarouselLoading by viewModel.isHeroCarouselLoading.collectAsStateWithLifecycle()
    val heroCarouselItems by
        viewModel.heroCarouselItems.collectDeferredHomeState(
            "hero_carousel",
            mergeWhileVisible = { _, incoming -> incoming },
        )
    val heroVideos = remember(heroCarouselItems) {
        heroCarouselItems.filterIsInstance<HeroCarouselItem.MusicVideo>()
    }

    val accountNameFlow by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncBannerLaunchCount by viewModel.syncBannerLaunchCount.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val accountNamePref by rememberPreference(AccountNameKey, "")
    val accountName = if (accountNameFlow != "Guest") accountNameFlow else accountNamePref
    val normalizedAccountName = remember(accountName) { accountName.lowercase() }
    val irideStart = 20.dp
    val irideListItemStart = 20.dp
    val irideGridItemStart = 20.dp
    val hideExplicit by rememberPreference(HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)
    val hideYoutubeShorts by rememberPreference(HideYoutubeShortsKey, defaultValue = false)
    val currentGridHeight = currentGridThumbnailHeight()
    val monthlyDrop = rememberCurrentMonthlyDrop()
    val showMonthlyDrop = monthlyDrop != null

    val lazyListState = rememberLazyListState()
    val similarShelves = remember(similarRecommendations) {
        similarRecommendations.orEmpty().mapNotNull { recommendation ->
            val items =
                recommendation.items
                    .distinctBy { it.id }
                    .filter { item ->
                        item is SongItem && !item.isVideoSong ||
                            item is AlbumItem ||
                            item is ArtistItem ||
                            item is PlaylistItem
                    }
                    .sortedBy { item -> item is ArtistItem }
            if (
                items.size < 2 ||
                    recommendation.title !is Artist && recommendation.thumbnailUrl.isNullOrBlank()
            ) {
                null
            } else {
                recommendation to items
            }
        }
    }
    val lightweightFolioMode by rememberLightweightFolioMode()
    val homeReducedMotion = rememberReducedMotion() || lightweightFolioMode
    val moodContentState =
        if (HOME_MOOD_SECTION_ENABLED) {
            viewModel.homeMoodContent.collectDeferredHomeState(
                "mood",
                identity = { it.params },
                hasContent = { !it.items.isNullOrEmpty() },
            )
        } else {
            remember {
                mutableStateOf(HomeViewModel.HomeMoodContent(null, null))
            }
        }
    val moodContent by moodContentState
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
    val isMoodLoading by viewModel.isMoodLoading.collectAsStateWithLifecycle()
    val isMoodError by viewModel.isMoodError.collectAsStateWithLifecycle()
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
    if (HOME_MOOD_SECTION_ENABLED) {
        LaunchedEffect(
            moodLifecycleOwner,
            selectedMoodCategory,
            hideExplicit,
            hideVideoSongs,
            hideYoutubeShorts,
        ) {
            moodLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (selectedMoodCategory != null) {
                    viewModel.loadMoodPage(
                        selectedMoodCategory?.endpoint?.params,
                        selectedMoodCategory?.title,
                        hideExplicit,
                        hideVideoSongs,
                        hideYoutubeShorts,
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
    }

    val homeAlreadyRevealed = remember { IrideTabEntrance.wasRevealed("home") }
    val screenProgress =
        if (homeAlreadyRevealed || lightweightFolioMode) {
            rememberUpdatedState(1f)
        } else {
            rememberEnterProgressState(
                    play = true,
                    durationMillis = IrideMotion.Short,
                    easing = IrideMotion.EaseOutQuart,
                )
        }
    LaunchedEffect(screenProgress) {
        snapshotFlow { screenProgress.value >= 1f }.first { it }
        IrideTabEntrance.markRevealed("home")
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
            val sections = homePage?.sections.orEmpty().filterNot { section ->
                val title = section.title.lowercase()
                val label = section.label.orEmpty().lowercase()
                title.contains("top 100") && (title.contains("video") || label.contains("video"))
            }
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
                    val label = indexed.value.label.orEmpty().lowercase()
                    when {
                        title.contains("trending") || title.contains("top 100") -> 0
                        title.contains("similar to") || title.contains("simile a") ||
                            label.contains("similar to") || label.contains("simile a") -> 1
                        title.contains("new release") || title.contains("nuove uscite") -> 2
                        else -> 3
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

    val colors = MaterialTheme.colorScheme
    val homeColorScheme = remember(colors) {
        colors.let { colors ->
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
    }
    val homeTypography = irideFolioShelfTypography()
    MaterialTheme(colorScheme = homeColorScheme, typography = homeTypography) {
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
                                showStarredAlbumInTitle = true,
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
                                        TrackOverflowIcon()
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

                    val forYouCellGap = IrideFolio.ShelfMosaicGap
                    // Match regular album and artist shelf artwork dimensions.
                    val forYouCellSize = currentGridHeight
                    val forYouBoxSize = forYouCellSize * 2 + forYouCellGap
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
                        val tileShape = if (cell is Artist) CircleShape else if (cell is Album || cell is Playlist) CollectionArtworkShape else FolioShape(6.dp)
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
                        val portraitLoader = rememberArtistArtworkLoader()
                        val portraitBackdrop = LocalIrideExpansionBackdrop.current
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
                        val portraitBorderColor = MaterialTheme.colorScheme.strokeCard
                        val portraitInFlight =
                            cell is Artist && LocalArtistPortraitTransitionActive.current &&
                                portraitScope != null && !homeReducedMotion &&
                                portraitNavigation?.seed?.sourceKey == portraitKey
                        if (portraitInFlight) {
                            portraitScope!!.transition.animateFloat(
                                transitionSpec = { tween(ArtistPortraitMotion.Duration, easing = ArtistPortraitMotion.Easing) },
                                label = "homePortraitVisibility",
                            ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
                        }
                        Box(
                            modifier =
                                Modifier.size(forYouCellSize)
                                    .then(collectionTile.first)
                                    .clip(tileShape)
                                    .irideFolioCombinedClickable(
                                        onClick = {
                                            if (cell is Artist && portrait != null) {
                                                prefetchArtistPage(cell.id)
                                                portraitLoader.prefetchHighResolution(ArtistArtworkRequest(
                                                    cell.id, cell.title, thumbnail, cell.artist.channelId,
                                                    !cell.artist.isLocal && !cell.artist.isPodcastChannel && !cell.artist.isPrivatelyOwnedArtist,
                                                ))
                                                portraitNavigation?.select(
                                                    portraitKey,
                                                    cell.id,
                                                    cell.title,
                                                    portrait,
                                                    thumbnail,
                                                    portraitBounds,
                                                    portraitBorderColor,
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
                        ) {
                            // Keep the cover recording inside the press layer, as on artist and library cards.
                            Box(Modifier.fillMaxSize().then(collectionTile.second)) {
                                val border = Modifier.border(
                                    width = 0.5.dp,
                                    color =
                                        if (cell is Song) MaterialTheme.colorScheme.strokeItemSoft
                                        else MaterialTheme.colorScheme.strokeCard,
                                    shape = tileShape,
                                )
                                if (cell is Artist && portrait != null) {
                                    val displayedPortrait = portraitNavigation?.seed?.takeIf { it.sourceKey == portraitKey }?.artwork ?: portrait
                                    Image(
                                        bitmap = remember(displayedPortrait.bitmap) { displayedPortrait.bitmap.asImageBitmap() },
                                        contentDescription = cell.title,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .then(border)
                                                .onGloballyPositioned { portraitBounds = it.boundsInWindow() }
                                                .drawWithContent {
                                                    if (!portraitInFlight || portraitBackdrop?.artworkInFlight != true) drawContent()
                                                }
                                                .clip(tileShape),
                                    )
                                } else if (cell is Artist) {
                                    ItemThumbnail(
                                        thumbnailUrl = thumbnail,
                                        isActive = false,
                                        isPlaying = false,
                                        shape = tileShape,
                                        artistArtwork = true,
                                        modifier = Modifier.fillMaxSize().then(border),
                                    )
                                } else {
                                    val context = LocalContext.current
                                    val thumbnailRequest = remember(context, thumbnail) {
                                        ImageRequest.Builder(context)
                                            .data(thumbnail?.resize(300, 300))
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = thumbnailRequest,
                                        contentDescription = cell.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().then(border).clip(tileShape),
                                    )
                                }
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
                                val context = LocalContext.current
                                val thumbnailRequest = remember(context, album.thumbnail) {
                                    ImageRequest.Builder(context)
                                        .data(album.thumbnail?.resize(300, 300))
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .build()
                                }
                                Box(
                                    modifier =
                                        Modifier.size(forYouCellSize)
                                            .then(collectionTile.first)
                                            .clip(CollectionArtworkShape)
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
                                ) {
                                    Box(Modifier.fillMaxSize().then(collectionTile.second)) {
                                        AsyncImage(
                                            model = thumbnailRequest,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().irideArtworkOverlayBorder(
                                                IrideSoftBorderWidth,
                                                MaterialTheme.colorScheme.strokeCard,
                                                CollectionArtworkShape,
                                            ),
                                        )
                                    }
                                }
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
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    val forYouLoading = isForYouLoading && forYouShelves.isEmpty()
                    val forYouSection: LazyListScope.(HomeShelfOrder) -> Unit = { shelfOrder ->
                        if (forYouLoading || forYouShelves.isNotEmpty() || showMonthlyDrop) {
                            shelfOrder.added(hasFourRows = false)
                            item(key = "for_you_section", contentType = "home_mosaic_shelf") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "for_you_section",
                                    ready = !forYouLoading || showMonthlyDrop,
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
                                        title =
                                            stringResource(
                                                if (showMonthlyDrop && forYouShelves.isEmpty()) {
                                                    R.string.iride_monthly_drop_home_title
                                                } else {
                                                    R.string.for_you_artists_title
                                                },
                                            ),
                                        showNavigationArrow = false,
                                    )

                                    val shelves = forYouShelves
                                    val firstShelf = shelves.firstOrNull()
                                    val forYouState = key(firstShelf?.artist?.id, firstShelf?.tiles?.map { it.id }) {
                                        rememberLazyListState()
                                    }
                                    val forYouContext = LocalContext.current
                                    val prefetchedForYouArtwork = remember(forYouState) { mutableSetOf<String>() }
                                    LaunchedEffect(forYouState, shelves, isForYouLoading, showMonthlyDrop) {
                                        snapshotFlow {
                                            forYouState.layoutInfo.visibleItemsInfo
                                                .lastOrNull()
                                                ?.index
                                        }
                                            .collect { lastVisible ->
                                                if (lastVisible != null) {
                                                    val lastVisibleShelfIndex =
                                                        lastVisible - if (showMonthlyDrop) 1 else 0
                                                    val upcomingShelves =
                                                        shelves
                                                            .drop((lastVisibleShelfIndex + 1).coerceAtLeast(0))
                                                            .take(2)
                                                    val upcomingArtwork = upcomingShelves.flatMap { shelf ->
                                                        buildList {
                                                            shelf.artist.thumbnailUrl?.resize(300, 300)?.let(::add)
                                                            shelf.tiles.forEach { tile ->
                                                                val thumbnail = when (tile) {
                                                                    is ForYouShelfTile.Local -> tile.item.thumbnailUrl
                                                                    is ForYouShelfTile.RecommendedAlbum -> tile.item.thumbnail
                                                                }
                                                                thumbnail?.resize(300, 300)?.let(::add)
                                                            }
                                                        }
                                                    }
                                                    upcomingArtwork.distinct().forEach { url ->
                                                        if (prefetchedForYouArtwork.add(url)) {
                                                            forYouContext.imageLoader.enqueue(
                                                                ImageRequest.Builder(forYouContext)
                                                                    .data(url)
                                                                    .size(300, 300)
                                                                    .precision(Precision.INEXACT)
                                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                                    .networkCachePolicy(CachePolicy.ENABLED)
                                                                    .build(),
                                                            )
                                                        }
                                                    }
                                                }
                                                if (
                                                    lastVisible != null &&
                                                        shelves.isNotEmpty() &&
                                                        (lastVisible - (if (showMonthlyDrop) 1 else 0)) >=
                                                            shelves.size - 3 &&
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
                                        if (monthlyDrop != null) {
                                            item(
                                                key = "iride_monthly_drop_${monthlyDrop.month}",
                                                contentType = "iride_monthly_drop",
                                            ) {
                                                IrideMonthlyDropHomeCard(
                                                    drop = monthlyDrop,
                                                    artworkSize = forYouBoxSize,
                                                    onClick = {
                                                        navController.navigate(IrideMonthlyDropRoute)
                                                    },
                                                )
                                            }
                                        }
                                            items(
                                            shelves,
                                                key = { shelf -> "for_you_${shelf.artist.id}" },
                                            ) { shelf ->
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
                        showTopBar = true,
                        actions = {
                            IconButton(onClick = { navController.navigate(Screens.Account.route) }) {
                                if (!accountImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = accountImageUrl,
                                        contentDescription = stringResource(Screens.Account.titleId),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(Screens.Account.iconIdInactive),
                                        contentDescription = stringResource(Screens.Account.titleId),
                                    )
                                }
                            }
                        },
                        bottomPadding =
                            LocalPlayerAwareWindowInsets.current
                                .asPaddingValues()
                                .calculateBottomPadding(),
                        listModifier =
                            Modifier.irideDepthEnter({ screenProgress.value }, intensity = 0.4f),
                         frostBlurScale = if (lightweightFolioMode) 0f else 0.5f,
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
                        if (updateVersion != null) {
                            item(key = "home_update_banner", contentType = "home_update_banner") {
                                HomeUpdateBanner(
                                    version = updateVersion,
                                    downloadState = updateDownloadState,
                                    onUpdateClick = onUpdateClick,
                                )
                            }
                        }
                        val shelfOrder = HomeShelfOrder()
                        if (isHeroCarouselEnabled && (heroVideos.isNotEmpty() || isHeroCarouselLoading)) {
                            item(key = "hero_carousel", contentType = "home_hero_carousel") {
                                val radioItems = heroVideos
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
                                            fadeOutSpec = null,
                                        ),
                                    sectionKey = "hero_carousel",
                                    ready = radioItems.isNotEmpty(),
                                    artwork = radioItems.take(1).map { it.coverUrl },
                                    placeholder = { HeroCarouselSkeleton() },
                                ) {
                                    HeroCarouselSection(
                                        items = radioItems,
                                        lowPowerMode = lightweightFolioMode,
                                        onCoverError = viewModel::removeHeroVideo,
                                        onVideoClick = { video ->
                                            navController.navigate("artist/${video.artistId}")
                                        },
                                        onVideoLongClick = { video ->
                                            menuState.show(anchored = true) {
                                                YouTubeSongMenu(
                                                    song = video.song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        shelfOrder.added(hasFourRows = true)
                        item(key = "picked_for_you_section", contentType = "home_song_shelf") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                        minimumRows = 4,
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
                                                        TrackOverflowIcon()
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
                                    listOf(
                                        "new release",
                                        "new album",
                                        "nuove uscite",
                                        "nuova uscita",
                                        "nuovi album",
                                        "nuovo album",
                                    ).any { it in title || it in label }
                                val isVideoSection =
                                    title.contains("video") ||
                                        label.contains("video") ||
                                        section.items.isNotEmpty() &&
                                            section.items.all { it is SongItem && it.isVideoSong }
                                if (isNewRelease || !isTrending && !isSimilar && !isVideoSection)
                                    return@feedSection
                                if (
                                    title.contains("listen again") ||
                                        label.contains("listen again") ||
                                        title.contains("long listens") ||
                                        title == normalizedAccountName ||
                                        title.contains("picked for you") ||
                                        title.contains("picked 4u") ||
                                        title.contains("quick picks") ||
                                        label.contains("picked for you")
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
                                            isMusicItem
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
                                val similarArtistItem =
                                    if (isSimilar && !isAlbumSource) {
                                        matchedArtist
                                            ?: sourceArtistId?.let { artistId ->
                                                ArtistItem(
                                                    id = artistId,
                                                    title = name,
                                                    thumbnail = sourceThumbnail,
                                                    channelId = artistId.takeIf { it.startsWith("UC") },
                                                    shuffleEndpoint = null,
                                                    radioEndpoint = null,
                                                )
                                            }
                                    } else null
                                val similarAlbumItem =
                                    if (isSimilar && isAlbumSource) {
                                        matchedAlbum
                                            ?: sectionItems.asSequence()
                                                .filterIsInstance<SongItem>()
                                                .firstNotNullOfOrNull { item ->
                                                    val album = item.album
                                                        ?.takeIf { it.name.equals(name, ignoreCase = true) }
                                                        ?: return@firstNotNullOfOrNull null
                                                    AlbumItem(
                                                        browseId = album.id,
                                                        playlistId = album.id,
                                                        title = album.name,
                                                        artists = item.artists,
                                                        thumbnail = sourceThumbnail ?: item.thumbnail,
                                                    )
                                                }
                                    } else null
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
                                item(key = "home_section_$index", contentType = "home_feed_shelf") {
                                    IrideFolioSection(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                                albumId = similarAlbumItem?.id,
                                                isArtist = !isAlbumSource,
                                                onClick = when {
                                                    similarArtistItem != null -> {
                                                        { navController.navigate("artist/${similarArtistItem.id}") }
                                                    }
                                                    similarAlbumItem != null -> {
                                                        { navController.navigate("album/${similarAlbumItem.id}") }
                                                    }
                                                    else -> onSectionClick
                                                },
                                                onLongClick = when {
                                                    similarArtistItem != null -> {
                                                        {
                                                            menuState.show(anchored = true) {
                                                                YouTubeArtistMenu(
                                                                    artist = similarArtistItem,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        }
                                                    }
                                                    similarAlbumItem != null -> {
                                                        {
                                                            menuState.show(anchored = true) {
                                                                YouTubeAlbumMenu(
                                                                    albumItem = similarAlbumItem,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        }
                                                    }
                                                    else -> null
                                                },
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
                        if (HOME_MOOD_SECTION_ENABLED) {
                            shelfOrder.added(hasFourRows = false)
                            item(key = "your_mood_section", contentType = "home_mood_shelf") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                item(key = "home_test_playlist_section", contentType = "home_artwork_shelf") {
                                    IrideFolioSection(
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                                val collectionTile = rememberCollectionExpansionTile(
                                                    CollectionExpansionKind.Playlist, playlist.id,
                                                    artworkUrl = playlist.thumbnail, title = playlist.title,
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
                                                    PlaylistArtwork(
                                                        title = playlist.title,
                                                        thumbnailUrl = playlist.thumbnail,
                                                        playlistId = playlist.id,
                                                        modifier = Modifier.size(currentGridHeight).then(collectionTile.second),
                                                        showBadge = false,
                                                    )
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
                            item(key = "home_feed_status", contentType = "home_feed_status") {
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
                        if (HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED) {
                            shelfOrder.added(hasFourRows = false)
                            item(key = "home_personal_mixes_section", contentType = "home_artwork_shelf") {
                                IrideFolioSection(
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                        remember(starredPlaylistName) {
                                            Playlist(
                                                playlist =
                                                    PlaylistEntity(
                                                        id = PlaylistEntity.LIKED_PLAYLIST_ID,
                                                        name = starredPlaylistName,
                                                    ),
                                                songCount = 0,
                                                songThumbnails = emptyList(),
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
                                    item(key = "daily_discover_section", contentType = "home_song_shelf") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                    item(key = "community_playlists_section", contentType = "home_artwork_shelf") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                            similarShelves.forEachIndexed { index, shelf ->
                                val (rec, similarItems) = shelf
                                val similarArtistItem = (rec.title as? Artist)?.let { artist ->
                                    ArtistItem(
                                        id = artist.id,
                                        title = artist.title,
                                        thumbnail = rec.thumbnailUrl ?: artist.thumbnailUrl,
                                        channelId = artist.artist.channelId,
                                        shuffleEndpoint = null,
                                        radioEndpoint = null,
                                    )
                                }
                                val similarAlbumItem = when (val title = rec.title) {
                                    is Album -> AlbumItem(
                                        browseId = title.id,
                                        playlistId = title.album.playlistId ?: title.id,
                                        title = title.title,
                                        artists = title.artists.map {
                                            com.metrolist.innertube.models.Artist(it.name, it.id)
                                        },
                                        year = title.album.year,
                                        thumbnail = rec.thumbnailUrl ?: title.thumbnailUrl.orEmpty(),
                                    )
                                    is Song -> title.album?.let { album ->
                                        AlbumItem(
                                            browseId = album.id,
                                            playlistId = album.playlistId ?: album.id,
                                            title = album.title,
                                            artists = title.orderedArtists.map {
                                                com.metrolist.innertube.models.Artist(it.name, it.id)
                                            },
                                            year = album.year,
                                            thumbnail = rec.thumbnailUrl ?: album.thumbnailUrl ?: title.thumbnailUrl.orEmpty(),
                                        )
                                    }
                                    else -> null
                                }
                                shelfOrder.added(hasFourRows = false)
                                item(key = "similar_to_${rec.title.id}", contentType = "home_song_shelf") {
                                    IrideFolioSection(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                                            albumId = similarAlbumItem?.id,
                                            isArtist = rec.title is Artist,
                                            onLongClick = when {
                                                similarArtistItem != null -> {
                                                    {
                                                        menuState.show(anchored = true) {
                                                            YouTubeArtistMenu(
                                                                artist = similarArtistItem,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    }
                                                }
                                                similarAlbumItem != null -> {
                                                    {
                                                        menuState.show(anchored = true) {
                                                            YouTubeAlbumMenu(
                                                                albumItem = similarAlbumItem,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    }
                                                }
                                                else -> null
                                            },
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
                                    item(key = "account_playlists_section", contentType = "home_artwork_shelf") {
                                        IrideFolioSection(
                                            modifier =
                                                Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                            item(key = "home_followed_releases", contentType = "home_release_shelf") {
                                HomeFollowedReleases(
                                    navController = navController,
                                    snackbarHostState = snackbarHostState,
                                    viewModel = followedReleasesViewModel,
                                )
                            }
                        }
                        var followedReleasesAdded = false
                        val sectionReadiness =
                            listOf(
                                true,
                                similarComplete || !similarRecommendations.isNullOrEmpty(),
                                dailyDiscoverComplete || !dailyDiscover.isNullOrEmpty(),
                                communityComplete || !communityPlaylists.isNullOrEmpty(),
                                !isLoggedIn ||
                                    !isAccountPlaylistsLoading ||
                                    !accountPlaylists.isNullOrEmpty(),
                            )
                        for ((index, section) in
                            listOf(
                                    feedSection,
                                    similarSection,
                                    dailySection,
                                    communitySection,
                                    accountSection,
                                )
                                .withIndex()) {
                            section()
                            if (!sectionReadiness[index]) {
                                item(key = "home_pending_section_$index", contentType = "home_pending_section") {
                                    IrideFolioSection(
                                        sectionKey = "home_pending_section_$index",
                                        ready = false,
                                        placeholder = {
                                            IrideFolioSectionSkeleton(
                                                currentGridHeight,
                                                songs = index == 1 || index == 2,
                                            )
                                        },
                                    ) {}
                                }
                            }
                            if (
                                !followedReleasesAdded &&
                                    followedReleaseTracks.isNotEmpty() &&
                                    index + 1 >= followedReleasesPosition &&
                                    shelfOrder.canAdd(hasFourRows = true)
                            ) {
                                addFollowedReleases()
                                followedReleasesAdded = true
                            }
                        }

                        if (!followedReleasesAdded && followedReleaseTracks.isNotEmpty()) {
                            shelfOrder.added(hasFourRows = false)
                            addFollowedReleases()
                        }
                        if (false) item(key = "reverie_section", contentType = "home_reverie_shelf") {
                            IrideFolioSection(
                                sectionKey = "reverie_section",
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = null,
                                        placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                        if (false) item(key = "iride_features_section", contentType = "home_features_shelf") {
                            IrideFolioSection(
                                sectionKey = "iride_features_section",
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = null,
                                        placementSpec = if (homeReducedMotion) null else IrideMotion.PlacementSpec,
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
                        item(key = "home_branding_footer", contentType = "home_branding_footer") {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = 20.dp, bottom = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.iride_wordmark),
                                    contentDescription = stringResource(R.string.app_name),
                                    contentScale = ContentScale.Fit,
                                    colorFilter =
                                        ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.width(108.dp).height(24.dp),
                                )
                                Text(
                                    text =
                                        stringResource(
                                            R.string.home_app_version,
                                            BuildConfig.VERSION_NAME,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeUpdateBanner(
    version: String,
    downloadState: UpdateDownloadState,
    onUpdateClick: () -> Unit,
) {
    val isReady = downloadState is UpdateDownloadState.ReadyToInstall
    val isDownloading = downloadState is UpdateDownloadState.Downloading
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding, vertical = 8.dp),
        shape = FolioShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.update_home_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.update_home_version, version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isDownloading) {
                val percent = (downloadState as UpdateDownloadState.Downloading).progressPercent
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
            IrideFolioPrimaryButton(
                onClick = onUpdateClick,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        when {
                            isReady -> R.string.update_action_install
                            isDownloading -> R.string.update_home_downloading
                            downloadState is UpdateDownloadState.Failed -> R.string.update_action_retry
                            else -> R.string.update_action_download
                        },
                    ),
                )
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
fun IrideFeatureCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
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
                    tint = iconTint ?: MaterialTheme.colorScheme.textPrimary,
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
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.size(16.dp),
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
