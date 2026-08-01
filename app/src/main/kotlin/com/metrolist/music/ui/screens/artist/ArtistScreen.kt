/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ShowArtistDescriptionKey
import com.metrolist.music.constants.ShowArtistSubscriberCountKey
import com.metrolist.music.constants.ShowMonthlyListenersKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.ExpandableText
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.LinkSegment
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.SongCarousel
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumReleaseType
import com.metrolist.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImagePainter
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.TypewriterText
import com.metrolist.music.ui.component.rememberNewlyVisibleKeys
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rubberBandOverscroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.grainOverlay
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberGrainBrush
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import sv.lib.squircleshape.SquircleShape

/**
 * Overflow sheet for the top bar's three-dot button: game / radio / shuffle / share, the four
 * buttons the bar used to hold shoulder to shoulder. Each `on*` param is null when that action
 * doesn't apply (offline branch, no endpoint, guest room) — same gating the inline buttons used to
 * do themselves, just read once here instead of duplicated per button.
 */
@Composable
private fun ColumnScope.ArtistOverflowMenu(
    onGame: () -> Unit,
    onRadio: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    fun icon(@androidx.annotation.DrawableRes res: Int): @Composable () -> Unit = {
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = Color.White.copy(alpha = 0.85f),
        )
    }

    val actions = buildList {
        add(
            NewAction(
                icon = icon(R.drawable.game_controller),
                text = stringResource(R.string.guess_game),
                onClick = { onDismiss(); onGame() },
            ),
        )
        onRadio?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.radio),
                    text = stringResource(R.string.radio),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
        onShuffle?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.shuffle),
                    text = stringResource(R.string.shuffle),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
        onShare?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.share),
                    text = stringResource(R.string.share),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
    }
    NewActionGrid(
        actions = actions,
        columns = 2,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val isChannelSubscribed by viewModel.isChannelSubscribed.collectAsState()
    val recentAlbum by viewModel.recentAlbum.collectAsState()
    val recentAlbumPreciseDate by viewModel.recentAlbumPreciseDate.collectAsState()
    val expandedTopSongs by viewModel.expandedTopSongs.collectAsState()
    val unseenAlbumIds by viewModel.unseenAlbumIds.collectAsState()
    val featuringSongs by viewModel.featuringSongs.collectAsState()
    val unseenSongIds by viewModel.unseenSongIds.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    // New Iride UI: image given a touch more height than a plain square (ratio < 1 = taller),
    // so the cover gets slightly more room before the title starts.
    val imageAspectRatio = if (topNavigationBarEnabled) 0.94f else 1f
    val irideHorizontalPadding = if (topNavigationBarEnabled) 20.dp else 16.dp

    val albumsTitles = remember(artistPage) {
        artistPage?.sections
            ?.filter { it.title.contains("Album", ignoreCase = true) }
            ?.flatMap { it.items }
            ?.filterIsInstance<AlbumItem>()
            ?.map { it.title.lowercase().trim() }
            ?.toSet() ?: emptySet()
    }

    // "Essential Albums": only for artists with a deep catalog (8+ albums) and a large audience —
    // a small-catalog artist doesn't need a curated shortcut, the regular Albums row already covers it.
    // Ranked by where each album's songs land in the artist's Top Songs shelf (YTM's own popularity
    // ranking) rather than by year — the raw Album-section order is release order, so sorting by that
    // (or leaving it as-is) always surfaces the newest album, not the most listened-to one.
    val essentialAlbums = remember(artistPage, expandedTopSongs) {
        val albumSection = artistPage?.sections?.firstOrNull { section ->
            section.title.contains("Album", ignoreCase = true) &&
                !section.title.contains("Single", ignoreCase = true) &&
                !section.title.contains("EP", ignoreCase = true)
        }
        val albums = albumSection?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
        val isFamous = hasLargeAudience(artistPage?.monthlyListenerCount) ||
            hasLargeAudience(artistPage?.subscriberCountText)
        if (albums.size < 8 || !isFamous) {
            emptyList()
        } else {
            val topSongsSection = artistPage?.sections?.firstOrNull { section ->
                (section.items.firstOrNull() as? SongItem)?.album != null
            }
            val shelfTopSongs = topSongsSection?.items?.filterIsInstance<SongItem>() ?: emptyList()
            val topSongs = expandedTopSongs?.takeIf { it.size > shelfTopSongs.size } ?: shelfTopSongs

            // Albums ranked by the earliest (= most popular) top song that belongs to them. Matched
            // by browseId first (how SongItem.album.id is normally populated); titles as a fallback
            // for the rare song whose album id didn't resolve to one of this shelf's own albums.
            val albumsById = albums.associateBy { it.id }
            val albumsByTitle = albums.associateBy { it.title.lowercase().trim() }
            val ranked = LinkedHashSet<AlbumItem>()
            topSongs.forEach { song ->
                val album = song.album?.id?.let(albumsById::get)
                    ?: song.album?.name?.lowercase()?.trim()?.let(albumsByTitle::get)
                album?.let(ranked::add)
            }
            albums.forEach(ranked::add)
            // Picked by popularity above, but displayed in release order (newest first) — a "most
            // essential albums" shelf reads as the artist's discography, not a popularity ranking.
            ranked.take(4).sortedByDescending { it.year ?: Int.MIN_VALUE }
        }
    }

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val librarySourceIndicatorOffset by animateDpAsState(
        targetValue = if (!showLocal) 2.dp else 42.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "artistSourceIndicator",
    )
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset =
        with(density) {
            -(systemBarsTopPadding + AppBarHeight).roundToPx()
        }

    // Classic (non New-Iride) app bar only ever needs the on/off read, not a continuous value.
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }
    // Declared here (ahead of its LaunchedEffect further down) so topBarRevealProgress below can
    // gate on it — the screen's own "have I finished landing" flag, already used to hold the header
    // entrance to a single playthrough.
    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    val frostBackdrop = rememberFrostBackdrop()

    // Stretch of the vertical rubber band, hoisted so the header art can answer the pull.
    val headerPull = rememberRubberBandPull()
    val grainBrush = rememberGrainBrush()

    // Sections that have already played their entrance. LazyColumn disposes items scrolled far off
    // screen, so without this a wipe would replay every time a shelf came back into view.
    val revealedSections = remember(showLocal) { mutableSetOf<String>() }

    // The whole screen arrives rather than being slapped down: covers the gap between navigation
    // and first layout.
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name
    // Everything in the header waits for the name to finish typing, so the block reads as one
    // sentence being composed rather than four things appearing at once.
    val nameTypingMs = remember(artistName) {
        val length = artistName?.length ?: 0
        if (length == 0) 0 else minOf(26 * length, 700)
    }
    // The header entrance is a landing, not a scroll effect. LazyColumn disposes item 0 once it is
    // far enough off screen, so this flag has to live out here (declared further up, alongside
    // topBarRevealProgress): kept inside the item, it died with it and the name retyped (and every
    // row re-faded) every time the header scrolled back in.
    // Window-space Y of the big header name's bottom edge and of the top bar's bottom edge — used to
    // start the top bar title's reveal exactly when the big name goes behind the bar, instead of on a
    // fixed scroll-distance heuristic that showed both titles on screen at once.
    var nameBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    // Single source for the whole bar's arrival — glass, back button and title all key off the same
    // crossing instead of the glass reacting to raw scroll offset while the title waited on the name.
    // That split is what read as the bar arriving "in anticipo": the blur was already up over the
    // photo before NOME ARTISTA had even started leaving.
    val titleCoverRangePx = with(density) { 24.dp.toPx() }
    val topBarRevealProgress by remember {
        derivedStateOf {
            if (!headerRevealed) {
                // Navigating back to an artist you'd already scrolled restores that scroll offset
                // (LazyListState is saveable) before this composition's own frostBackdrop
                // GraphicsLayer — which is NOT saveable — has recorded a single real frame. Gating on
                // `headerRevealed` (the same one-shot "screen has landed" flag the header entrance
                // already waits on) means the glass can only turn on after a real frame exists to blur.
                0f
            } else if (lazyListState.firstVisibleItemIndex > 0) {
                // Header item disposed off the top: nameBottomPx would otherwise hold its last
                // recorded (and stale) value.
                1f
            } else {
                ((topBarBottomPx + titleCoverRangePx - nameBottomPx) / titleCoverRangePx).coerceIn(0f, 1f)
            }
        }
    }
    LaunchedEffect(artistName) {
        if (artistName != null && !headerRevealed) {
            // Longest chain in the cascade: typing, then the release panel at +140 over Medium.
            delay(nameTypingMs + 140L + IrideMotion.Medium)
            headerRevealed = true
        }
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    // The Online branch's header is taller (Recent Album panel) than the Library branch's — without
    // resetting scroll, toggling away from Online while scrolled left the shorter Library layout
    // looking like it started with a big empty top padding, since the same pixel offset now landed
    // much further down its (shorter) content.
    // Guarded to skip the first run: LaunchedEffect(showLocal) fires on every fresh composition,
    // including returning from AlbumScreen (where rememberSaveable had just restored scroll) — without
    // the guard that restored position was wiped back to top on every trip back, not just on a real
    // online/local toggle.
    var showLocalInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(showLocal) {
        if (showLocalInitialized) {
            lazyListState.animateScrollToItem(0)
        } else {
            showLocalInitialized = true
        }
    }

    // Waiting on the network.
    val artistLoading = artistPage == null && !showLocal
    // New Iride UI draws no loading mockup at all: the header appears as soon as anything is known
    // and each shelf wipes itself in as it lands. A placeholder can only ever model a subset of this
    // screen — the old one had no listeners line and no latest-release panel — so it guaranteed the
    // jump it existed to prevent.
    val fullSkeleton = artistLoading && !topNavigationBarEnabled

    val featuringTitle = stringResource(R.string.featuring)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                // Fade only. A scale-down here would inset edge-to-edge content by ~1% for the
                // duration, showing a hairline of window background around every edge; the fade
                // alone already covers the gap between navigation and first layout.
                if (topNavigationBarEnabled) {
                    Modifier.graphicsLayer { alpha = screenProgress }
                } else {
                    Modifier
                },
            ),
    ) {
        LazyColumn(
            // recordFrostBackdrop must wrap the rubber band, not the other way round: the frosted
            // bar samples this layer, so it has to capture the content *after* the band's
            // translation or the glass would show un-pulled pixels while the finger is down.
            modifier = Modifier
                .recordFrostBackdrop(frostBackdrop)
                .rubberBandOverscroll(Orientation.Vertical, lazyListState, headerPull),
            state = lazyListState,
            overscrollEffect = null,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (fullSkeleton) {
                item(key = "shimmer") {
                    LegacyArtistShimmer(
                        headerOffset = headerOffset,
                        topFadePadding = systemBarsTopPadding + AppBarHeight,
                    )
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val reducedMotion = rememberReducedMotion()
                    // Panel only exists for the online branch, and only once a recent release is
                    // known — shared by the render below and the spacer that follows it, so both stay
                    // in lockstep instead of drifting into two separate conditions.
                    val showRecentAlbumPanel = !showLocal && recentAlbum != null
                    // Same 160dp as the image's bottom gradient below — the name anchors to where
                    // that legible zone starts, not to a guess at the tallest possible header.
                    val gradientHeightPx = with(density) { 160.dp.roundToPx() }

                    // No forced aspect ratio here: the photo below keeps its own fixed ratio via its
                    // own modifier, but this outer box wraps to whichever is taller — the photo, or
                    // the overlaid text (name, toggle, listeners, recent-release panel). Forcing this
                    // box to the photo's ratio let tall overlay content (the online branch's panel,
                    // or a 2-line name at a large font scale) draw past the box's bottom edge into the
                    // next shelf below — nothing here clips — which is exactly the "layout doesn't
                    // know its true height until it loads" jump between branches.
                    // animateContentSize lives on the Column below, not here: this box's photo child
                    // bleeds upward (negative offset) into the status-bar/top-bar gap, and
                    // animateContentSize clips its subject to its own measured bounds — put on this
                    // outer box, it cut that bleed off at the box's un-offset top edge, leaving the
                    // reserved gap flat black instead of showing the photo through it.
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Artist Image with offset
                        // Photo height is fully known from this box's width constraint and the
                        // fixed aspect ratio the image below uses — computed directly instead of
                        // measured via onSizeChanged, which lagged a frame behind the photo
                        // becoming known. That lag was the bug: on a cold (uncached) load the
                        // name rendered at the wrong top padding for one frame, then
                        // animateContentSize visibly slid it down once the real height arrived.
                        val imageHeightPx = if (thumbnail != null) {
                            with(density) { (maxWidth.toPx() * imageAspectRatio).roundToInt() }
                        } else {
                            0
                        }
                        if (thumbnail != null) {
                            var imageLoaded by remember(thumbnail) { mutableStateOf(false) }
                            // A memory-cache hit (any revisit to an artist already seen this
                            // session) resolves synchronously — animating it in over 520ms just
                            // replays the black `surfaceVariant` placeholder behind the transparent
                            // top bar every single time, since the tween always starts at 0
                            // regardless of how fast `imageLoaded` flips. Only genuinely new
                            // network/disk decodes need the fade.
                            var skipImageEnterAnim by remember(thumbnail) { mutableStateOf(false) }
                            val animatedImageProgress = rememberEnterProgress(
                                play = imageLoaded,
                                durationMillis = 520,
                                easing = IrideMotion.EaseOutQuart,
                            )
                            val imageProgress = if (skipImageEnterAnim) 1f else animatedImageProgress
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(imageAspectRatio)
                                        .offset {
                                            // Parallax: the photo climbs at roughly a third of the
                                            // content's speed, which is what reads as depth. Both
                                            // scroll values are read inside this lambda, so this is
                                            // a placement change, not a recomposition.
                                            val parallax = if (
                                                topNavigationBarEnabled &&
                                                lazyListState.firstVisibleItemIndex == 0
                                            ) {
                                                (lazyListState.firstVisibleItemScrollOffset * 0.35f).roundToInt()
                                            } else {
                                                0
                                            }
                                            IntOffset(x = 0, y = headerOffset + parallax)
                                        }
                                        .then(
                                            if (topNavigationBarEnabled) {
                                                Modifier.graphicsLayer {
                                                    // Settles from slightly oversized on load, and
                                                    // grows from its own bottom edge while the list
                                                    // is dragged past the top — the thing that makes
                                                    // the rubber band felt rather than merely present.
                                                    //
                                                    // Growing by exactly the band's travel (rather
                                                    // than by a fraction of a magic constant) pins
                                                    // the photo's top edge in place while the finger
                                                    // drags the list down. A smaller stretch let the
                                                    // pull open a strip of bare window above the
                                                    // image — the black band at the top.
                                                    val stretch = if (size.height > 0f) {
                                                        (headerPull.offset / size.height)
                                                            .coerceIn(0f, 0.6f)
                                                    } else {
                                                        0f
                                                    }
                                                    val s = lerp(1.06f, 1f, imageProgress) + stretch
                                                    scaleX = s
                                                    scaleY = s
                                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                                }
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        // The classic UI fades the photo out under its solid app
                                        // bar. The Iride bar is frosted glass and wants real pixels
                                        // behind it — and the fade was drawing the top of the image
                                        // straight to transparent, so dragging the list down slid
                                        // that transparency into view as a black band.
                                        .then(
                                            if (topNavigationBarEnabled) {
                                                Modifier
                                            } else {
                                                Modifier.fadingEdge(
                                                    top = systemBarsTopPadding + AppBarHeight,
                                                )
                                            },
                                        ),
                            ) {
                                AsyncImage(
                                    model = thumbnail.resize(1200, 1200),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    onState = { state ->
                                        if (state is AsyncImagePainter.State.Success) {
                                            if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                                                skipImageEnterAnim = true
                                            }
                                            imageLoaded = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        // Fading the photo rather than the whole block lets the
                                        // surfaceVariant backdrop hold the space while it decodes.
                                        .then(
                                            if (topNavigationBarEnabled) {
                                                Modifier.graphicsLayer { alpha = imageProgress }
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .grainOverlay(if (topNavigationBarEnabled) grainBrush else null),
                                )
                                // Full gradient overlay: 0% background at top → 100% at bottom
                                val bgColor = MaterialTheme.colorScheme.background
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, bgColor),
                                            )
                                        ),
                                )
                            }
                        }

                        // Artist Name and Controls Section — anchored to a fixed point near the
                        // bottom of the image (the start of its gradient) rather than bottom-aligned
                        // to its own height. Bottom-aligning made the name's position a function of
                        // whatever optional content (monthly listeners, recent-release panel) happened
                        // to be loaded below it: the panel arriving async pushed the name up, and it
                        // sank back down whenever that data was missing. Anchoring the top instead
                        // means the name never moves — the optional content simply flows in below it,
                        // with no reserved gap when it's absent.
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (thumbnail != null) {
                                            // padding, not offset: offset only shifts where this
                                            // Column draws, it doesn't grow the wrap-content outer
                                            // Box to match — the tail of a tall Column would draw
                                            // past the Box's bounds and under the next shelf. Padding
                                            // is real layout space, so the Box sizes to fit it.
                                            Modifier.padding(
                                                top = with(density) {
                                                    (imageHeightPx - gradientHeightPx).coerceAtLeast(0).toDp()
                                                },
                                            )
                                        } else {
                                            Modifier.padding(top = 16.dp)
                                        },
                                    )
                                    .then(
                                        if (reducedMotion) {
                                            Modifier
                                        } else {
                                            Modifier.animateContentSize(
                                                tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
                                            )
                                        },
                                    ),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = irideHorizontalPadding),
                            ) {
                                // Artist Name — the one typed element on the screen.
                                val irideNameStyle = TextStyle(
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = (-0.3).sp,
                                )
                                if (topNavigationBarEnabled && artistName != null) {
                                    // Shuffle-plays the artist's songs: the online shuffle endpoint
                                    // when browsing YTM, otherwise whatever is saved locally — so the
                                    // button does something on both branches of the toggle, not just
                                    // the one that already had a shuffle action in the top bar.
                                    val shuffleEndpointForPlay = artistPage?.artist?.shuffleEndpoint
                                    val canShufflePlay = shuffleEndpointForPlay != null || librarySongs.isNotEmpty()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .onGloballyPositioned { nameBottomPx = it.boundsInWindow().bottom },
                                    ) {
                                        TypewriterText(
                                            text = artistName,
                                            style = irideNameStyle,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            // Keyed on the artist, not the string: recomposing the same
                                            // screen must not retype the name.
                                            resetKey = viewModel.artistId,
                                            // Types on the first landing only — coming back to the top
                                            // of the page is navigation, not an arrival.
                                            animate = !headerRevealed,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (canShufflePlay) {
                                            val playProgress = headerEnter(
                                                revealed = headerRevealed,
                                                play = artistName != null,
                                                delayMillis = nameTypingMs + 20,
                                                durationMillis = IrideMotion.Short,
                                            )
                                            // Own chip behind the bare icon button — the header's
                                            // bottom gradient already protects the name's contrast,
                                            // but this button sits closer to the raw photo above it,
                                            // where a bare icon can wash out against a bright patch.
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 10.dp)
                                                    .irideEnterScale(playProgress, from = 0.7f)
                                                    .clip(SquircleShape(radius = 10.dp, cornerSmoothing = 0.48f))
                                                    .background(Color.Black.copy(alpha = 0.28f)),
                                            ) {
                                                IrideOutlineIconButton(
                                                    onClick = {
                                                        if (!isGuest) {
                                                            val endpoint = shuffleEndpointForPlay
                                                            if (endpoint != null) {
                                                                playerConnection.playQueue(YouTubeQueue(endpoint))
                                                            } else if (librarySongs.isNotEmpty()) {
                                                                playerConnection.playQueue(
                                                                    ListQueue(
                                                                        title = artistName,
                                                                        items = librarySongs.shuffled().map { it.toMediaItem() },
                                                                        startIndex = 0,
                                                                    ),
                                                                )
                                                            }
                                                        }
                                                    },
                                                    icon = R.drawable.play,
                                                    contentDescription = stringResource(R.string.shuffle),
                                                    tint = Color.White,
                                                    size = 38.dp,
                                                    iconSize = 18.dp,
                                                    pressEffect = IridePressEffect.Punch,
                                                )
                                            }
                                        }
                                    }
                                } else if (topNavigationBarEnabled) {
                                    // Nothing to say yet — better an empty line than "Unknown"
                                    // flashing before the real name arrives. Sized from the type
                                    // scale so the gap matches the line it is holding open at any
                                    // system font size.
                                    Spacer(
                                        modifier = Modifier.height(
                                            with(density) { (28f * 1.2f).sp.toDp() } + 6.dp,
                                        ),
                                    )
                                } else {
                                    Text(
                                        text = artistName ?: "Unknown",
                                        style = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                // Library/Online source toggle — Iride segmented pill in the New UI
                                // (same component used for the library "saved/downloaded" switch),
                                // classic dual-icon capsule otherwise. Shown whenever the artist has
                                // any local content (songs or albums) to switch to — gating on albums
                                // alone hid the toggle (and the library songs behind it) for artists
                                // saved with songs but no albums.
                                if (libraryAlbums.isNotEmpty() || librarySongs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(if (topNavigationBarEnabled) 10.dp else 12.dp))
                                    if (topNavigationBarEnabled) {
                                        val toggleProgress = headerEnter(
                                            revealed = headerRevealed,
                                            play = artistName != null,
                                            delayMillis = nameTypingMs + 40,
                                            durationMillis = IrideMotion.Short,
                                        )
                                        IrideSegmentedToggle(
                                            modifier = Modifier.irideEnter(toggleProgress, 6.dp),
                                            options = listOf(
                                                false to stringResource(R.string.online),
                                                true to stringResource(R.string.filter_library),
                                            ),
                                            selected = showLocal,
                                            onSelect = { value ->
                                                if (value != showLocal) {
                                                    showLocal = value
                                                    if (!value && artistPage == null) viewModel.fetchArtistsFromYTM()
                                                }
                                            },
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .offset {
                                                        IntOffset(
                                                            x = librarySourceIndicatorOffset.roundToPx(),
                                                            y = 2.dp.roundToPx(),
                                                        )
                                                    }
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                            )
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                        ) {
                                                            if (showLocal) {
                                                                showLocal = false
                                                                if (artistPage == null) viewModel.fetchArtistsFromYTM()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.language),
                                                        contentDescription = null,
                                                        tint = if (!showLocal)
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
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() },
                                                        ) {
                                                            if (!showLocal) showLocal = true
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.bookmark_outlined),
                                                        contentDescription = null,
                                                        tint = if (showLocal)
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

                                val monthlyListeners = artistPage?.monthlyListenerCount
                                if (showMonthlyListeners && !monthlyListeners.isNullOrEmpty()) {
                                    val cleanListeners = remember(monthlyListeners) {
                                        monthlyListeners
                                            .replace("monthly listeners", "", ignoreCase = true)
                                            .replace("ascoltatori mensili", "", ignoreCase = true)
                                            .trim()
                                    }
                                    val listenersProgress = headerEnter(
                                        revealed = headerRevealed,
                                        play = artistName != null,
                                        delayMillis = nameTypingMs + 80,
                                        durationMillis = IrideMotion.Short,
                                    )
                                    Spacer(modifier = Modifier.height(if (topNavigationBarEnabled) 10.dp else 8.dp))
                                    Text(
                                        text = "$cleanListeners listeners this month",
                                        style = if (topNavigationBarEnabled) {
                                            TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp)
                                        } else {
                                            MaterialTheme.typography.bodySmall
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = if (topNavigationBarEnabled) {
                                            Modifier.revealMask(listenersProgress)
                                        } else {
                                            Modifier
                                        },
                                    )
                                }

                                // Action buttons (subscribe/radio/shuffle/share) live in the top bar now.

                                // Recent Album Panel (YTM view only — library view already lists albums below)
                                if (showRecentAlbumPanel) {
                                    val panelProgress = headerEnter(
                                        revealed = headerRevealed,
                                        play = artistName != null,
                                        delayMillis = nameTypingMs + 140,
                                        durationMillis = IrideMotion.Medium,
                                    )
                                    Spacer(modifier = Modifier.height(if (topNavigationBarEnabled) 14.dp else 8.dp))
                                    RecentAlbumPanel(
                                        album = recentAlbum!!.album,
                                        releaseType = recentAlbum!!.type,
                                        preciseDate = recentAlbumPreciseDate,
                                        useMonospace = topNavigationBarEnabled,
                                        enterProgress = if (topNavigationBarEnabled) panelProgress else 1f,
                                        isActive = mediaMetadata?.album?.id == recentAlbum!!.album.id,
                                        isPlaying = isPlaying,
                                        onClick = { navController.navigate("album/${recentAlbum!!.album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = recentAlbum!!.album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            if (showRecentAlbumPanel) {
                                // Was a flat 16dp for both UI modes — in New Iride UI this stacked on
                                // top of NavigationTitle's own 26dp top padding for the next section
                                // ("Album"), leaving ~42dp of dead space between the two panels.
                                Spacer(modifier = Modifier.height(if (topNavigationBarEnabled) 4.dp else 16.dp))
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier
                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                    .revealMask(rememberSectionEnter("local_songs", revealedSections)),
                                useIrideStyle = topNavigationBarEnabled,
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                },
                            )
                        }

                        val filteredLibrarySongs =
                            if (hideExplicit) {
                                librarySongs.filter { !it.song.explicit }
                            } else {
                                librarySongs
                            }
                        item(key = "local_songs_carousel") {
                            SongCarousel(
                                items = filteredLibrarySongs,
                                key = { "local_song_${it.id}" },
                                modifier = Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec),
                            ) { song, itemWidth ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    // Carousel is itself a horizontal drag surface (LazyHorizontalGrid
                                    // + snap fling) — the per-row swipe-to-queue gesture fought it for
                                    // the same horizontal drag, same conflict Home's Quick Picks
                                    // carousel already had to turn this off for.
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .width(itemWidth)
                                            .padding(horizontal = if (topNavigationBarEnabled) 8.dp else 0.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (!isGuest) {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                                    items = filteredLibrarySongs.map { it.toMediaItem() },
                                                                    startIndex = filteredLibrarySongs.indexOfFirst { it.id == song.id },
                                                                ),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
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

                    if (libraryAlbums.isNotEmpty()) {
                        val filteredLibraryAlbums = libraryAlbums.filter { album ->
                            !hideExplicit || !album.album.explicit
                        }

                        if (filteredLibraryAlbums.isNotEmpty()) {
                            item(key = "local_albums_title") {
                                NavigationTitle(
                                    title = stringResource(R.string.albums),
                                    modifier = Modifier
                                        .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                        .revealMask(rememberSectionEnter("local_albums", revealedSections)),
                                    useIrideStyle = topNavigationBarEnabled,
                                    onClick = {
                                        navController.navigate("artist/${viewModel.artistId}/albums")
                                    },
                                )
                            }

                            item(key = "local_albums_list") {
                                val rowState = rememberLazyListState()
                                LazyRow(
                                    state = rowState,
                                    overscrollEffect = null,
                                    modifier = Modifier
                                        .irideEnter(
                                            rememberSectionEnter("local_albums_row", revealedSections),
                                            10.dp,
                                        )
                                        .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = if (topNavigationBarEnabled) {
                                        PaddingValues(horizontal = irideHorizontalPadding)
                                    } else {
                                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                                    },
                                ) {
                                    items(
                                        items = filteredLibraryAlbums,
                                        key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" },
                                    ) { album ->
                                        AlbumGridItem(
                                            album = album,
                                            isActive = mediaMetadata?.album?.id == album.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            showPlayButton = false,
                                            size = 180.dp,
                                            modifier =
                                                Modifier
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("album/${album.id}")
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                AlbumMenu(
                                                                    originalAlbum = album,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ).animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Guards the Essential Albums insertion below to a single shelf: some artist
                    // pages carry more than one YTM shelf shaped like "Top Songs" (e.g. both a
                    // "Songs" and a "Popular" shelf), and rendering the panel from each would reuse
                    // the same LazyColumn item keys and crash.
                    var essentialAlbumsRendered = false
                    // Guards the discography button to a single insertion the same way — some
                    // artists carry separate "Singles" and "EPs" shelves rather than one combined
                    // shelf; the button lands under whichever of those is encountered first.
                    var discographyButtonRendered = false
                    // Guards the Featuring section to a single insertion — it's injected right
                    // before the first Video/Performance-titled shelf (above videos, below
                    // EPs/Singles per the artist page's own shelf order), or after the loop if the
                    // artist page never has a video shelf at all.
                    var featuringSectionRendered = false
                    val featuringSection: LazyListScope.() -> Unit = {
                        if (featuringSongs.isNotEmpty()) {
                            item(key = "featuring_title") {
                                NavigationTitle(
                                    title = featuringTitle,
                                    modifier = Modifier
                                        .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                        .revealMask(rememberSectionEnter("featuring_section", revealedSections)),
                                    useIrideStyle = topNavigationBarEnabled,
                                )
                            }
                            item(key = "featuring_carousel") {
                                val gridState = rememberLazyGridState()
                                rememberNewlyVisibleKeys(gridState) { visibleIds ->
                                    visibleIds.forEach { viewModel.markSongSeen(it) }
                                }
                                SongCarousel(
                                    items = featuringSongs,
                                    key = { it.id },
                                    gridState = gridState,
                                    modifier = Modifier.irideEnter(
                                        rememberSectionEnter("featuring_carousel", revealedSections),
                                        10.dp,
                                    ),
                                ) { song, itemWidth ->
                                    YouTubeListItem(
                                        item = song,
                                        isActive = mediaMetadata?.id == song.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
                                        showNewMarker = song.id in unseenSongIds,
                                        newMarkerLabel = if (song.id in unseenSongIds) stringResource(R.string.artist_release_type_feat) else null,
                                        showAlbumInSubtitle = true,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .padding(horizontal = if (topNavigationBarEnabled) 8.dp else 0.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.id in unseenSongIds) viewModel.markSongSeen(song.id)
                                                    if (!isGuest) {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = featuringTitle,
                                                                    items = featuringSongs.map { it.toMediaItem() },
                                                                    startIndex = featuringSongs.indexOfFirst { it.id == song.id },
                                                                ),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = song,
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
                    }
                    artistPage?.sections?.fastForEach { section ->
                        // "From your library" is redundant with the dedicated library toggle above — skip it
                        val isFromYourLibrarySection = section.title.contains("your library", ignoreCase = true) ||
                                section.title.contains("tua libreria", ignoreCase = true)
                        if (section.items.isNotEmpty() && !isFromYourLibrarySection) {
                            val isSinglesSection = section.title.contains("Single", ignoreCase = true) || section.title.contains("EP", ignoreCase = true)
                            // Filter out recent album and duplicate Singles/EPs
                            val filteredItemsUnsorted = section.items.filter { item ->
                                val isDuplicate = isSinglesSection && item is AlbumItem && albumsTitles.contains(item.title.lowercase().trim())
                                !isDuplicate
                            }

                            val isAlbumOrSingleEpSection = section.title.contains("Album", ignoreCase = true) ||
                                    section.title.contains("Single", ignoreCase = true) ||
                                    section.title.contains("EP", ignoreCase = true)
                            // Newest-first by release year — YTM shelves are not always correctly ordered
                            val filteredItems = if (isAlbumOrSingleEpSection) {
                                filteredItemsUnsorted.sortedByDescending { (it as? AlbumItem)?.year ?: Int.MIN_VALUE }
                            } else {
                                filteredItemsUnsorted
                            }

                            if (filteredItems.isNotEmpty()) {
                                // Top Songs sits directly under the header, so it's the one shelf
                                // whose position is driven purely by the header's own async growth
                                // (Recent Album panel / monthly listeners arriving late over the
                                // network) — that resize is already smooth on its own
                                // (animateContentSize), so a placement animation here doubles up on
                                // the same movement and reads as the shelf sliding down out of
                                // nowhere. Left unanimated, it just holds its resting position.
                                val isTopSongsShelf = (filteredItems.firstOrNull() as? SongItem)?.album != null

                                val isVideoSectionForFeaturingGate = section.title.contains("Video", ignoreCase = true) ||
                                    section.title.contains("Performance", ignoreCase = true)
                                if (isVideoSectionForFeaturingGate && !featuringSectionRendered) {
                                    featuringSectionRendered = true
                                    featuringSection()
                                }

                                if (!isVideoSectionForFeaturingGate) {
                                item(key = "section_${section.title}") {
                                    NavigationTitle(
                                        title = section.title,
                                        modifier = Modifier
                                            .then(
                                                if (isTopSongsShelf) {
                                                    Modifier
                                                } else {
                                                    Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                },
                                            )
                                            .revealMask(rememberSectionEnter(section.title, revealedSections)),
                                        useIrideStyle = topNavigationBarEnabled,
                                        onClick =
                                            section.moreEndpoint?.let {
                                                {
                                                    navController.navigate(
                                                        "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                                    )
                                                }
                                            },
                                    )
                                }

                                if (isTopSongsShelf) {
                                    val shelfTopSongs = filteredItems.distinctBy { it.id }.filterIsInstance<SongItem>()
                                    // YTM's shelf itself only ever carries ~5 songs; viewModel silently
                                    // browses the shelf's own "more" endpoint to fetch the full list in
                                    // the background, and this swaps in once (and only if) it comes
                                    // back longer than what we already have.
                                    val topSongs = expandedTopSongs?.takeIf { it.size > shelfTopSongs.size } ?: shelfTopSongs
                                    item(key = "top_songs_carousel_${section.title}") {
                                        val topSongsGridState = rememberLazyGridState()
                                        rememberNewlyVisibleKeys(topSongsGridState) { visibleIds ->
                                            visibleIds.forEach { viewModel.markSongSeen(it) }
                                        }
                                        SongCarousel(
                                            items = topSongs,
                                            key = { it.id },
                                            gridState = topSongsGridState,
                                        ) { song, itemWidth ->
                                            YouTubeListItem(
                                                item = song,
                                                isActive = mediaMetadata?.id == song.id,
                                                isPlaying = isPlaying,
                                                // Same carousel/swipe gesture conflict as the local
                                                // songs carousel above.
                                                isSwipeable = false,
                                                showNewMarker = song.id in unseenSongIds,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                YouTubeSongMenu(
                                                                    song = song,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.more_vert),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                },
                                                modifier =
                                                    Modifier
                                                        .width(itemWidth)
                                                        .padding(horizontal = if (topNavigationBarEnabled) 8.dp else 0.dp)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (!isGuest) {
                                                                    if (song.id == mediaMetadata?.id) {
                                                                        playerConnection.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(
                                                                            ListQueue(
                                                                                title = section.title,
                                                                                items = topSongs.map { it.toMediaItem() },
                                                                                startIndex = topSongs.indexOfFirst { it.id == song.id },
                                                                            ),
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    YouTubeSongMenu(
                                                                        song = song,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                            )
                                        }
                                    }

                                    if (essentialAlbums.isNotEmpty() && !essentialAlbumsRendered) {
                                        essentialAlbumsRendered = true
                                        item(key = "essential_albums_title") {
                                            NavigationTitle(
                                                title = stringResource(R.string.essential_albums),
                                                modifier = Modifier
                                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                    .revealMask(rememberSectionEnter("essential_albums", revealedSections)),
                                                useIrideStyle = topNavigationBarEnabled,
                                            )
                                        }
                                        item(key = "essential_albums_row") {
                                            val rowState = rememberLazyListState()
                                            LazyRow(
                                                state = rowState,
                                                overscrollEffect = null,
                                                modifier = Modifier
                                                    .irideEnter(
                                                        rememberSectionEnter("essential_albums_row", revealedSections),
                                                        10.dp,
                                                    )
                                                    .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                                contentPadding = if (topNavigationBarEnabled) {
                                                    PaddingValues(horizontal = irideHorizontalPadding)
                                                } else {
                                                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                                                },
                                            ) {
                                                items(
                                                    items = essentialAlbums,
                                                    key = { "essential_album_${it.id}" },
                                                ) { album ->
                                                    YouTubeGridItem(
                                                        item = album,
                                                        isActive = mediaMetadata?.album?.id == album.id,
                                                        isPlaying = isPlaying,
                                                        coroutineScope = coroutineScope,
                                                        showPlayButton = false,
                                                        size = 270.dp,
                                                        thumbnailShape = RoundedCornerShape(270.dp * 0.06f),
                                                        modifier = Modifier
                                                            .combinedClickable(
                                                                onClick = {
                                                                    navController.navigate("album/${album.id}")
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    menuState.show {
                                                                        YouTubeAlbumMenu(
                                                                            albumItem = album,
                                                                            navController = navController,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }
                                                                },
                                                            )
                                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    item(key = "section_list_${section.title}") {
                                        val isAlbumSection = section.title.contains("Album", ignoreCase = true)
                                        val isSingleEpSection = section.title.contains("Single", ignoreCase = true) ||
                                                section.title.contains("EP", ignoreCase = true)
                                        val hidePlayButton = isAlbumSection || isSingleEpSection
                                        val isVideoSection = section.title.contains("Video", ignoreCase = true) ||
                                                section.title.contains("Performance", ignoreCase = true)
                                        val rowState = rememberLazyListState()
                                        LazyRow(
                                            state = rowState,
                                            overscrollEffect = null,
                                            modifier = Modifier
                                                .irideEnter(
                                                    rememberSectionEnter(
                                                        "row_${section.title}",
                                                        revealedSections,
                                                    ),
                                                    10.dp,
                                                )
                                                .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            contentPadding = if (topNavigationBarEnabled) {
                                                PaddingValues(horizontal = irideHorizontalPadding)
                                            } else {
                                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                                            },
                                        ) {
                                            items(
                                                items = filteredItems.distinctBy { it.id },
                                                key = { "youtube_album_${it.id}" },
                                            ) { item ->
                                                YouTubeGridItem(
                                                    item = item,
                                                    isActive =
                                                        when (item) {
                                                            is SongItem -> mediaMetadata?.id == item.id
                                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                            else -> false
                                                        },
                                                    isPlaying = isPlaying,
                                                    coroutineScope = coroutineScope,
                                                    thumbnailRatio = if (isVideoSection) 16f / 9f else 1f,
                                                    thumbnailCornerRadius = if (isVideoSection) 8.dp else 3.dp,
                                                    showPlayButton = !hidePlayButton,
                                                    showNewMarker = item is AlbumItem && item.id in unseenAlbumIds,
                                                    newMarkerLabel = when {
                                                        item !is AlbumItem || item.id !in unseenAlbumIds -> null
                                                        isAlbumSection -> stringResource(R.string.artist_release_type_album)
                                                        section.title.contains("EP", ignoreCase = true) -> stringResource(R.string.artist_release_type_ep)
                                                        isSingleEpSection -> stringResource(R.string.artist_release_type_single)
                                                        else -> null
                                                    },
                                                    size = when {
                                                        isAlbumSection -> 180.dp
                                                        isVideoSection -> 110.dp
                                                        else -> 148.dp
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (item is AlbumItem && item.id in unseenAlbumIds) {
                                                                        viewModel.markAlbumSeen(item.id)
                                                                    }
                                                                    when (item) {
                                                                        is SongItem -> {
                                                                            if (isVideoSection) {
                                                                                navController.navigate(
                                                                                    "video_player/${item.id}?title=${Uri.encode(item.title)}",
                                                                                )
                                                                            } else if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }

                                                                        is AlbumItem -> {
                                                                            navController.navigate("album/${item.id}")
                                                                        }

                                                                        is ArtistItem -> {
                                                                            navController.navigate("artist/${item.id}")
                                                                        }

                                                                        is PlaylistItem -> {
                                                                            navController.navigate("online_playlist/${item.id}")
                                                                        }

                                                                        is PodcastItem -> {
                                                                            navController.navigate("online_podcast/${item.id}")
                                                                        }

                                                                        is EpisodeItem -> {
                                                                            if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
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
                                                                            is SongItem -> {
                                                                                YouTubeSongMenu(
                                                                                    song = item,
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is AlbumItem -> {
                                                                                YouTubeAlbumMenu(
                                                                                    albumItem = item,
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is ArtistItem -> {
                                                                                YouTubeArtistMenu(
                                                                                    artist = item,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is PlaylistItem -> {
                                                                                YouTubePlaylistMenu(
                                                                                    playlist = item,
                                                                                    coroutineScope = coroutineScope,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is PodcastItem -> {
                                                                                YouTubePlaylistMenu(
                                                                                    playlist = item.asPlaylistItem(),
                                                                                    coroutineScope = coroutineScope,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is EpisodeItem -> {
                                                                                YouTubeSongMenu(
                                                                                    song = item.asSongItem(),
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                            ).animateItem(placementSpec = IrideMotion.PlacementSpec),
                                                )
                                            }
                                        }
                                    }
                                }
                                }

                                if (isSinglesSection && topNavigationBarEnabled && !discographyButtonRendered) {
                                    discographyButtonRendered = true
                                    item(key = "discography_button") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = irideHorizontalPadding, vertical = 4.dp)
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                .clip(SquircleShape(radius = 14.dp, cornerSmoothing = 0.48f))
                                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                                .clickable {
                                                    navController.navigate("artist/${viewModel.artistId}/discography")
                                                }
                                                .padding(vertical = 14.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.view_discography).uppercase(),
                                                style = TextStyle(
                                                    fontFamily = SpaceMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    letterSpacing = 1.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!featuringSectionRendered) {
                        featuringSectionRendered = true
                        featuringSection()
                    }

                    // About Artist Section
                    if (!showLocal && (showArtistDescription || showArtistSubscriberCount)) {
                        val description = artistPage?.description
                        val descriptionRuns = artistPage?.descriptionRuns
                        val subscriberCount = artistPage?.subscriberCountText

                        if ((showArtistDescription && !description.isNullOrEmpty()) ||
                            (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty())
                        ) {
                            item(key = "about_artist") {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = irideHorizontalPadding)
                                            .padding(vertical = 16.dp)
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                ) {
                                    Text(
                                        text = stringResource(R.string.information),
                                        style = if (topNavigationBarEnabled) {
                                            TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp, letterSpacing = (-0.1).sp)
                                        } else {
                                            MaterialTheme.typography.titleLarge
                                        },
                                        fontWeight = FontWeight.Bold,
                                        // onSurfaceVariant resolves per scheme; the old hardcoded
                                        // white at 55% was invisible in the light theme.
                                        color = if (topNavigationBarEnabled) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        },
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .revealMask(rememberSectionEnter("about", revealedSections)),
                                    )

                                    if (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) {
                                        val formattedSubscribers = formatSubscriberCount(subscriberCount)
                                        Text(
                                            text = formattedSubscribers ?: subscriberCount,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 16.dp),
                                        )
                                    }

                                    if (showArtistDescription && (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty())) {
                                        Text(
                                            text = "Wikipedia",
                                            style = if (topNavigationBarEnabled) {
                                                TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp, letterSpacing = (-0.1).sp)
                                            } else {
                                                MaterialTheme.typography.titleMedium
                                            },
                                            fontWeight = FontWeight.Bold,
                                            // onSurfaceVariant resolves per scheme; the old hardcoded
                                        // white at 55% was invisible in the light theme.
                                        color = if (topNavigationBarEnabled) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        },
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )

                                        val linkSegments = remember(descriptionRuns) {
                                            descriptionRuns?.map { run ->
                                                LinkSegment(
                                                    text = run.text,
                                                    url = run.navigationEndpoint?.urlEndpoint?.url
                                                )
                                            }
                                        }

                                        ExpandableText(
                                            text = description.orEmpty(),
                                            runs = linkSegments,
                                            collapsedMaxLines = 4,
                                        )

                                        val wikiLink = descriptionRuns?.find {
                                            it.text.contains("Wikipedia", ignoreCase = true) ||
                                                    it.navigationEndpoint?.urlEndpoint?.url?.contains("wikipedia.org") == true
                                        }?.navigationEndpoint?.urlEndpoint?.url

                                        if (wikiLink != null) {
                                            Text(
                                                text = "Read more on Wikipedia",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(top = 12.dp)
                                                    .clickable { uriHandler.openUri(wikiLink) }
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .align(Alignment.BottomCenter),
        )
    }

    if (topNavigationBarEnabled) {
        // New Iride UI: minimal shell — back + title + subscribe/radio/shuffle/share, all in one row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { topBarBottomPx = it.boundsInWindow().bottom }
                .frostedTopBarBackground(
                    progress = topBarRevealProgress,
                    barColor = MaterialTheme.colorScheme.background,
                    strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backdrop = frostBackdrop,
                )
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
            Box(modifier = Modifier.irideEnterScale(backProgress, from = 0.8f)) {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
            // Always composed and always holding its weight — title fades in via
            // topBarRevealProgress above, tracking the big header name going behind this bar.
            Text(
                text = artistPage?.artist?.title ?: libraryArtist?.artist?.name.orEmpty(),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .irideEnter(topBarRevealProgress, 6.dp)
                    .revealMask(topBarRevealProgress),
            )
            val shareLinkAction = artistPage?.artist?.shareLink
            val radioEndpointAction = artistPage?.artist?.radioEndpoint
            val shuffleEndpointAction = artistPage?.artist?.shuffleEndpoint
            // Radio/shuffle/share only exist once the page lands. animateContentSize lets the
            // survivors slide across to close the gap instead of teleporting.
            val actionsReady = artistPage != null || libraryArtist != null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize(
                    animationSpec = tween(IrideMotion.Short, easing = IrideMotion.EaseOutQuart),
                ),
            ) {
                IrideOutlineIconButton(
                    onClick = { viewModel.toggleChannelSubscription() },
                    icon = if (isChannelSubscribed) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = stringResource(R.string.subscribe),
                    // Always white: a toggle affordance, not a "liked" colour cue.
                    tint = Color.White,
                    size = 40.dp,
                    iconSize = 20.dp,
                    pressEffect = IridePressEffect.Punch,
                    modifier = Modifier.irideEnterScale(
                        rememberEnterProgress(play = actionsReady, durationMillis = IrideMotion.Short),
                    ),
                )
                // Game/radio/shuffle/share live behind this overflow now — five buttons shoulder to
                // shoulder was the actual cause of stray taps the 40dp sizing above was patching
                // around; two here removes the problem instead of shrinking the target further.
                IrideOutlineIconButton(
                    onClick = {
                        menuState.show {
                            ArtistOverflowMenu(
                                onGame = { navController.navigate("artist/${viewModel.artistId}/game") },
                                onRadio = if (!showLocal && !isGuest && radioEndpointAction != null) {
                                    { playerConnection.playQueue(YouTubeQueue(radioEndpointAction)) }
                                } else {
                                    null
                                },
                                onShuffle = if (!showLocal && !isGuest && shuffleEndpointAction != null) {
                                    { playerConnection.playQueue(YouTubeQueue(shuffleEndpointAction)) }
                                } else {
                                    null
                                },
                                onShare = shareLinkAction?.let { link -> { shareArtist(context, link) } },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    icon = R.drawable.more_vert,
                    contentDescription = null,
                    tint = Color.White,
                    size = 40.dp,
                    iconSize = 20.dp,
                    modifier = Modifier.irideEnterScale(
                        rememberEnterProgress(
                            play = actionsReady,
                            delayMillis = IrideMotion.StaggerStep,
                            durationMillis = IrideMotion.Short,
                        ),
                    ),
                )
            }
        }
    } else {
        TopAppBar(
            title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                val shareLink = artistPage?.artist?.shareLink
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleChannelSubscription() }
                    ) {
                        Icon(
                            painter = painterResource(if (isChannelSubscribed) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = null,
                            tint = if (isChannelSubscribed) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (!showLocal && !isGuest) {
                        artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                            IconButton(
                                onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                            IconButton(
                                onClick = { playerConnection.playQueue(YouTubeQueue(shuffleEndpoint)) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = "Shuffle",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { shareLink?.let { shareArtist(context, it) } }
                    ) {
                        Icon(
                            painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            colors =
                if (transparentAppBar) {
                    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAlbumPanel(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    useMonospace: Boolean = false,
    releaseType: AlbumReleaseType? = null,
    preciseDate: String? = null,
    /** 0f→1f entrance driven by the caller, so the panel lands after the artist name is typed. */
    enterProgress: Float = 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        // Eyebrow label — without it the panel used to read as an ambiguous, unlabeled
        // album card floating in the header with no indication of what it represents.
        // ALBUM/EP/SINGLE tag appended so it's clear what kind of release this actually is,
        // since a single or EP looked identical to a full album otherwise.
        val eyebrowText = stringResource(R.string.artist_latest_release).uppercase() +
            (releaseType?.let { " • ${it.name}" } ?: "")
        Text(
            text = eyebrowText,
            style = if (useMonospace) {
                TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.sp)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (useMonospace) 0.5f else 1f),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .revealMask(enterProgress),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .irideEnter(enterProgress, 6.dp)
                .pressScale(interactionSource)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemThumbnail(
                thumbnailUrl = album.thumbnailUrl?.resize(544, 544),
                isActive = isActive,
                isPlaying = isPlaying,
                // New Iride UI: same 9.dp squircle radius as every other cover in the app (album/
                // playlist list & grid rows); classic UI keeps its own smaller 6.dp rounding.
                shape = if (useMonospace) {
                    SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f)
                } else {
                    SquircleShape(radius = 6.dp, cornerSmoothing = 0.48f)
                },
                modifier = Modifier.size(if (useMonospace) 108.dp else 96.dp),
                hairlineBorder = useMonospace,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = album.album.title,
                    style = if (useMonospace) {
                        TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                val extendedDate = remember(album.album.releaseDate, preciseDate) {
                    formatExtendedDate(album.album.releaseDate ?: preciseDate)
                }
                // Fallback to year when full releaseDate is not available (e.g. from YTM API)
                val displayDate = extendedDate ?: album.album.year?.toString()

                val metaText = listOfNotNull(
                    displayDate,
                    if (album.album.songCount > 0) {
                        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount)
                    } else {
                        null
                    },
                ).joinToString(" • ")

                if (useMonospace) {
                    if (metaText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metaText,
                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Start,
                        )
                    }
                } else {
                    if (displayDate != null) {
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                    }

                    if (album.album.songCount > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

private fun shareArtist(context: Context, link: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, link)
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
}

fun formatExtendedDate(dateStr: String?): String? {
    if (dateStr == null) return null
    return try {
        val parts = dateStr.split("-")
        val date = when (parts.size) {
            3 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            2 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
            else -> java.time.LocalDate.of(parts[0].toInt(), 1, 1)
        }

        val formatter = when (parts.size) {
            3 -> DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
            2 -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
            else -> DateTimeFormatter.ofPattern("yyyy", Locale.US)
        }

        date.format(formatter)
    } catch (e: Exception) {
        null
    }
}

/** True when a YTM count string ("2.1M subscribers", "850K monthly listeners"...) reaches million+ scale. */
private fun hasLargeAudience(countText: String?): Boolean {
    if (countText == null) return false
    val unit = """([\d.,]+)\s*([KMB])""".toRegex(RegexOption.IGNORE_CASE).find(countText)
        ?.groupValues?.get(2)?.uppercase()
    return unit == "M" || unit == "B"
}

fun formatSubscriberCount(subscriberCount: String?): String? {
    if (subscriberCount == null) return null
    val regex = """([\d.,]+)\s*([KMB]?)""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(subscriberCount) ?: return null
    val (numStr, unit) = matchResult.destructured
    val num = numStr.replace(",", ".").toDoubleOrNull() ?: return null

    // Discard implausible values: no suffix and less than 1000
    if (unit.isEmpty() && num < 1000) return null

    val rounded = (num * 10.0).roundToInt() / 10.0
    val formattedNum = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()

    return when (unit.uppercase()) {
        "K" -> "${formattedNum}k subscribers on YouTube"
        "M" -> "$formattedNum million subscribers on YouTube"
        "B" -> "$formattedNum billion subscribers on YouTube"
        else -> "$formattedNum subscribers on YouTube"
    }
}

/**
 * Loading state for the classic (pre-New-Iride) artist screen.
 *
 * Lifted out of ArtistScreen unchanged: it was a hundred lines of placeholder nested five levels
 * deep inside the LazyColumn. The New Iride UI has no counterpart — it draws the real header as soon
 * as anything is known and wipes each shelf in as it lands.
 */
@Composable
private fun LegacyArtistShimmer(
    headerOffset: Int,
    topFadePadding: Dp,
) {
    ShimmerHost(
        modifier =
            Modifier
                .offset {
                    IntOffset(x = 0, y = headerOffset)
                },
    ) {
        // Artist Image Placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f),
        ) {
            Spacer(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .shimmer()
                        .background(MaterialTheme.colorScheme.onSurface)
                        .fadingEdge(
                            top = topFadePadding,
                        ),
            )
            val bgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor),
                        )
                    ),
            )
        }
        // Artist Name and Controls Section
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            // Artist Name Placeholder
            TextPlaceholder(
                height = 36.dp,
                modifier =
                    Modifier
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 16.dp),
            )

            // Buttons Row Placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Subscribe (Like) Button Placeholder
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .shimmer()
                            .background(
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(20.dp),
                            ),
                )

                // Radio Button Placeholder
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .shimmer()
                            .background(
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(20.dp),
                            ),
                )

                // Shuffle Button Placeholder
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .shimmer()
                            .background(
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(20.dp),
                            ),
                )

                // Link Button Placeholder
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .shimmer()
                            .background(
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(20.dp),
                            ),
                )
            }
        }
        // Songs List Placeholder
        repeat(6) {
            ListItemPlaceHolder()
        }
    }
}