package com.metrolist.music.ui.component

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.ui.utils.rememberReducedMotion
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.metrolist.music.R
import com.metrolist.music.ui.theme.CollectionArtworkShape
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.drawIrideArtworkBorder
import com.metrolist.music.ui.utils.resize

internal enum class CollectionExpansionKind { Album, Playlist }

private fun NavBackStackEntry.routePattern(): String? = destination.route?.substringBefore('?')

internal class CollectionExpansionSource(
    val kind: CollectionExpansionKind,
    val id: String,
    val entryId: String?,
    val alternateId: String? = null,
    val key: String = UUID.randomUUID().toString(),
    var bounds: Rect = Rect.Zero,
    var layer: GraphicsLayer? = null,
    val artworkUrl: String? = null,
    val title: String = "",
    val borderColor: Color? = null,
) {
    var visibleBounds = Rect.Zero
    var coverCoordinates: LayoutCoordinates? = null
    var pageArtworkMostlyVisible = false
    var returnWithDissolve by mutableStateOf(false)
    var returnArtwork by mutableStateOf<ImageBitmap?>(null)
    var captureRequested by mutableStateOf(false)
    var artwork by mutableStateOf<GraphicsLayer?>(null)
    var coverLayer: GraphicsLayer? = null
    var coverReady = false
    var palette: ArtworkHeaderPalette? = null
    var coverBounds = Rect.Zero
    var headerBounds = Rect.Zero
    var badgePainter by mutableStateOf<Painter?>(null)
}

internal class CollectionExpansionNavigation(
    private val navController: NavController,
    private val backdrop: IrideExpansionBackdrop,
    private val graphicsContext: GraphicsContext,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
) {
    var source by mutableStateOf<CollectionExpansionSource?>(null)
        private set
    private val flights = linkedMapOf<String, CollectionExpansionSource>()
    private val retiredEntries = mutableSetOf<String>()
    private val retiredSources = mutableListOf<CollectionExpansionSource>()
    private var pendingAt = 0L
    private var pending = false
    private var pendingSource: CollectionExpansionSource? = null
    private val visited = mutableSetOf<String>().apply { navController.currentBackStackEntry?.id?.let(::add) }
    var fallbackBounds: Rect = Rect.Zero
    var motionEnabled = true

    private fun destinationKind(entry: NavBackStackEntry): CollectionExpansionKind? = when (entry.routePattern()) {
        "album/{albumId}" -> CollectionExpansionKind.Album
        "local_playlist/{playlistId}", "online_playlist/{playlistId}", "auto_playlist/{playlist}",
        "top_playlist/{top}", "cache_playlist/{playlist}", "daily_chart/{market}",
        "new_releases_chart/{market}" -> {
            if (entry.routePattern() == "auto_playlist/{playlist}" &&
                entry.arguments?.getString("playlist") == "all_tracks"
            ) null else CollectionExpansionKind.Playlist
        }
        else -> null
    }

    private fun fallbackMatch(to: NavBackStackEntry): Boolean = motionEnabled && fallbackBounds.width > 0f &&
        to.id !in visited && destinationKind(to) != null

    fun select(tile: CollectionExpansionSource) {
        if (!motionEnabled || tile.bounds.width <= 0f || tile.bounds.height <= 0f) return
        pendingSource?.takeIf { it !in flights.values }?.let(::release)
        // Each opening owns its layers and geometry; a reused lazy tile is never a flight.
        pendingSource = CollectionExpansionSource(
            tile.kind, tile.id, tile.entryId, tile.alternateId, tile.key, tile.bounds, tile.layer,
            tile.artworkUrl, tile.title, tile.borderColor,
        ).also { flight ->
            flight.badgePainter = tile.badgePainter
            flight.visibleBounds = tile.visibleBounds
            flight.palette = ArtworkHeaderPalettes.cached(tile.artworkUrl)
            tile.layer?.takeIf { !it.isReleased && it.size.width > 0f && it.size.height > 0f }?.let { original ->
                flight.artwork = graphicsContext.createGraphicsLayer().apply {
                    record(density, layoutDirection, original.size) { drawLayer(original) }
                }
            }
        }
        tile.captureRequested = true
        pending = true
        pendingAt = SystemClock.uptimeMillis()
    }

    private fun matches(entry: NavBackStackEntry, tile: CollectionExpansionSource): Boolean = when (tile.kind) {
        CollectionExpansionKind.Album -> entry.routePattern() == "album/{albumId}" && entry.arguments?.getString("albumId") == tile.id
        CollectionExpansionKind.Playlist -> when (entry.routePattern()) {
            "local_playlist/{playlistId}", "online_playlist/{playlistId}" -> entry.arguments?.getString("playlistId")?.let {
                it.removePrefix("VL") == tile.id.removePrefix("VL") || tile.alternateId?.removePrefix("VL") == it.removePrefix("VL")
            } == true
            "auto_playlist/{playlist}", "cache_playlist/{playlist}" -> entry.arguments?.getString("playlist") == when (tile.id) {
                PlaylistEntity.LIKED_PLAYLIST_ID -> "liked"
                PlaylistEntity.STARRED_PLAYLIST_ID -> "starred"
                PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> "downloaded"
                else -> tile.id
            }
            "top_playlist/{top}" -> entry.arguments?.getString("top") == tile.id
            "daily_chart/{market}", "new_releases_chart/{market}" -> entry.arguments?.getString("market") == tile.id
            else -> false
        }
    }

    private fun pendingMatch(from: NavBackStackEntry, to: NavBackStackEntry): Boolean {
        val tile = pendingSource ?: return false
        return pending && SystemClock.uptimeMillis() - pendingAt < 1500L && tile.entryId == from.id && matches(to, tile)
    }

    fun isPair(from: NavBackStackEntry, to: NavBackStackEntry): Boolean = motionEnabled && !usesSimpleSearchSectionMotion(from, to) &&
        (pendingMatch(from, to) || flights[from.id]?.entryId == to.id || flights[to.id]?.entryId == from.id || fallbackMatch(to))

    fun transition(from: NavBackStackEntry?, to: NavBackStackEntry?): Boolean {
        if (from == null || to == null) return false
        if (usesSimpleSearchSectionMotion(from, to)) {
            pendingSource?.let(::release)
            pendingSource = null
            pending = false
            return false
        }
        val entering = motionEnabled && pendingMatch(from, to)
        val returning = motionEnabled && flights[from.id]?.entryId == to.id
        val fallback = !entering && !returning && fallbackMatch(to)
        pending = false
        if (entering) {
            source = pendingSource
            flights[to.id] = source!!
            backdrop.capture(from.id)
        } else {
            pendingSource?.let(::release)
        }
        pendingSource = null
        if (returning) {
            source = flights[from.id]?.also {
                it.returnWithDissolve = it.coverCoordinates?.isAttached != true || !it.pageArtworkMostlyVisible
            }
            retiredEntries.add(from.id)
            backdrop.show(to.id)
        }
        if (fallback) {
            source = CollectionExpansionSource(destinationKind(to)!!, "", from.id, bounds = fallbackBounds)
            flights[to.id] = source!!
            backdrop.capture(from.id)
        }
        visited.add(from.id)
        visited.add(to.id)
        while (flights.size > 8) {
            val removed = flights.remove(flights.keys.first())
            if (removed != null && removed !in flights.values) release(removed)
        }
        return entering || returning || fallback
    }

    fun destination(entry: NavBackStackEntry): CollectionExpansionSource? = flights[entry.id]
    fun record(tile: CollectionExpansionSource, layer: GraphicsLayer) {
        val flight = (pendingSource ?: source)?.takeIf { it.key == tile.key && it.artwork == null } ?: return
        flight.artwork = graphicsContext.createGraphicsLayer().apply {
            record(density, layoutDirection, layer.size) { drawLayer(layer) }
        }
    }
    fun coverLayer(source: CollectionExpansionSource): GraphicsLayer = source.coverLayer
        ?: graphicsContext.createGraphicsLayer().also { source.coverLayer = it }

    private fun release(source: CollectionExpansionSource) {
        source.artwork?.let(graphicsContext::releaseGraphicsLayer)
        source.coverLayer?.let(graphicsContext::releaseGraphicsLayer)
        source.artwork = null
        source.coverLayer = null
        source.returnArtwork = null
    }

    fun dispose() {
        (flights.values + retiredSources + listOfNotNull(pendingSource)).distinct().forEach(::release)
        flights.clear()
        retiredSources.clear()
    }
    fun popBackStack(): Boolean = navController.popBackStack()

    fun settle(entry: NavBackStackEntry?): Boolean {
        source = entry?.let { flights[it.id] }
        retiredEntries.toList().forEach { id ->
            if (id != entry?.id) {
                flights.remove(id)?.let(retiredSources::add)
                retiredEntries.remove(id)
            }
        }
        return source != null
    }

    fun releaseRetired() {
        retiredSources.forEach(::release)
        retiredSources.clear()
    }

    fun reattach(tile: CollectionExpansionSource) {
        val current = source ?: return
        if (!backdrop.inFlight && current.key == tile.key &&
            tile.entryId == navController.currentBackStackEntry?.id && tile.bounds != Rect.Zero) {
            current.bounds = tile.bounds
            current.visibleBounds = tile.visibleBounds
        }
    }
}

internal val LocalCollectionExpansionNavigation = staticCompositionLocalOf<CollectionExpansionNavigation?> { null }
internal val LocalCollectionExpansionActive = staticCompositionLocalOf { false }
internal val LocalCollectionExpansionProgress = staticCompositionLocalOf<State<Float>?> { null }
internal val LocalCollectionPanelGesture = staticCompositionLocalOf<IridePanelGestureMotion?> { null }
internal val LocalCollectionExpansionSource = staticCompositionLocalOf<CollectionExpansionSource?> { null }
internal val LocalCollectionSourceEntry = staticCompositionLocalOf<String?> { null }

/** Waits for the opening flight before starting work unrelated to its rendering. */
internal suspend fun State<Float>?.awaitSettled() {
    val progress = this ?: return
    snapshotFlow { progress.value >= 1f }.first { it }
}

private class CollectionArtworkFlight(val layer: GraphicsLayer, from: Rect, to: Rect, val scrolledBounds: Rect = to) {
    val path = CollectionArtworkPath(from, to)
    var destinationMeasured = false
}

private class CollectionExpansionTarget(val layer: GraphicsLayer, val source: CollectionExpansionSource) {
    var panelCoordinates: LayoutCoordinates? = null
    var opening: CollectionArtworkFlight? = null
    var closing: CollectionArtworkFlight? = null
    var bounds by mutableStateOf(source.coverBounds)
    var recorded by mutableStateOf(layer.size.width > 0 && layer.size.height > 0)
    var recordedReadyArtwork by mutableStateOf(false)
    var ready by mutableStateOf(source.coverReady)
    var stroke by mutableStateOf<Color?>(null)
    var badgePainter by mutableStateOf<Painter?>(source.badgePainter)
    val fallback: GraphicsLayer? get() = source.artwork
    val artwork: GraphicsLayer?
        get() = layer.takeIf { recorded && ready && !it.isReleased }
            ?: fallback?.takeUnless { it.isReleased }
            ?: layer.takeIf { recorded && !it.isReleased }
}

private val LocalCollectionExpansionTarget = staticCompositionLocalOf<CollectionExpansionTarget?> { null }

/** Press prefetch and page rendering share the decoded size and cache entry. */
@Composable
internal fun rememberCollectionArtworkRequest(url: String?, size: Dp = 240.dp): ImageRequest {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    return remember(context, url, pixels) {
        ImageRequest.Builder(context).data(url?.resize(pixels, pixels)).size(pixels, pixels)
            .precision(Precision.EXACT).crossfade(false).build()
    }
}

/** A stable key selects one occurrence even when several shelves contain the same collection. */
@Composable
internal fun rememberCollectionExpansionTile(
    kind: CollectionExpansionKind,
    id: String,
    alternateId: String? = null,
    artworkUrl: String? = null,
    title: String = "",
): Pair<Modifier, Modifier> {
    val playlist = kind == CollectionExpansionKind.Playlist
    val badgePainter = if (playlist) painterResource(R.drawable.playlist_play) else null
    val navigation = LocalCollectionExpansionNavigation.current
        ?: return Modifier to if (playlist) Modifier.playlistArtworkBadge(requireNotNull(badgePainter)) else Modifier
    val reducedMotion = rememberReducedMotion()
    val borderColor = MaterialTheme.colorScheme.strokeCard
    val entryId = LocalCollectionSourceEntry.current
    val key = rememberSaveable(kind, id, entryId) { UUID.randomUUID().toString() }
    val tile = remember(key, alternateId, artworkUrl, title, borderColor) {
        CollectionExpansionSource(
            kind, id, entryId, alternateId, key,
            artworkUrl = artworkUrl,
            title = title,
            borderColor = borderColor,
        )
    }
    SideEffect { tile.badgePainter = badgePainter }
    val layer = rememberGraphicsLayer()
    val context = LocalContext.current
    val artworkRequest = rememberCollectionArtworkRequest(artworkUrl)
    val scrolling = LocalIridePageScrolling.current
    LaunchedEffect(artworkUrl, scrolling) {
        val url = artworkUrl ?: return@LaunchedEffect
        if (ArtworkHeaderPalettes.cached(url) != null) return@LaunchedEffect
        // Decorative palettes can wait for the fling; visible thumbnail decoding has priority.
        snapshotFlow { scrolling?.value == true }.collectLatest { moving ->
            if (!moving && ArtworkHeaderPalettes.cached(url) == null) {
                delay(350)
                withContext(Dispatchers.Default) { ArtworkHeaderPalettes.load(context, url) }
            }
        }
    }
    val backdrop = LocalIrideExpansionBackdrop.current
    val selected = LocalCollectionExpansionActive.current && navigation.source?.key == key && !reducedMotion
    val visibility = LocalAnimatedVisibilityScope.current
    // Keep the outgoing source alive long enough to hide exactly the selected tile in
    // its retained scene, even though NavHost uses ExitTransition.None for this pair.
    if (selected && visibility != null) {
        visibility.transition.animateFloat(
            transitionSpec = { tween(IrideExpansionMotion.Duration, easing = IrideExpansionMotion.Easing) },
            label = "collectionTileVisibility",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
    }
    val tap = Modifier.pointerInput(tile, reducedMotion, artworkRequest) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val preload = if (artworkUrl != null) context.imageLoader.enqueue(artworkRequest) else null
            tile.captureRequested = !reducedMotion
            val up = waitForUpOrCancellation(PointerEventPass.Initial)
            if (up != null && !reducedMotion && up.uptimeMillis - down.uptimeMillis < ViewConfiguration.getLongPressTimeout()) {
                navigation.select(tile)
            } else {
                tile.captureRequested = false
                preload?.dispose()
            }
        }
    }
    val cover = Modifier.onGloballyPositioned {
        tile.bounds = it.irideUnclippedBoundsInWindow()
        tile.layer = layer
        tile.visibleBounds = it.boundsInWindow()
        navigation.reattach(tile)
    }.drawWithContent {
        val hidden = selected && backdrop?.artworkInFlight == true
        if (tile.captureRequested) {
            layer.record { this@drawWithContent.drawContent() }
            navigation.record(tile, layer)
            tile.captureRequested = false
            if (!hidden) drawLayer(layer)
        } else if (hidden) {
            // Keep the hidden painter advancing; otherwise its fade starts after landing.
            layer.record { this@drawWithContent.drawContent() }
        } else {
            drawContent()
        }
        if (!hidden && playlist) drawPlaylistArtworkBadgeForCover(Rect(Offset.Zero, size), badgePainter)
    }
    return tap to cover
}

@Composable
internal fun Modifier.playlistExpansionTarget(ready: Boolean = true): Modifier =
    collectionExpansionTarget(
        ready,
        playlist = true,
        badgePainter = painterResource(R.drawable.playlist_play),
    )

@Composable
internal fun Modifier.collectionExpansionTarget(ready: Boolean = true): Modifier =
    collectionExpansionTarget(ready, playlist = false, badgePainter = null)

@Composable
private fun Modifier.collectionExpansionTarget(ready: Boolean, playlist: Boolean, badgePainter: Painter? = null): Modifier {
    val target = LocalCollectionExpansionTarget.current
    val progress = LocalCollectionExpansionProgress.current
    val gesture = LocalCollectionPanelGesture.current
    val stroke = MaterialTheme.colorScheme.strokeCard
    if (target == null || progress == null) return drawWithContent {
        drawContent()
        drawCollectionArtworkBorder(size, stroke)
        if (playlist) drawPlaylistArtworkBadgeForCover(Rect(Offset.Zero, size), badgePainter)
    }
    target.ready = ready
    target.stroke = stroke
    target.source.coverReady = ready
    target.badgePainter = badgePainter
    var placement by remember(target) { mutableStateOf<LayoutCoordinates?>(null) }
    DisposableEffect(target) {
        onDispose {
            if (target.source.coverCoordinates === placement) {
                target.source.coverCoordinates = null
                target.source.pageArtworkMostlyVisible = false
            }
        }
    }
    return onGloballyPositioned { coordinates ->
        val panel = target.panelCoordinates
        if (panel != null && panel.isAttached && coordinates.isAttached) {
            // Measure in the panel's resting coordinate system, independent of its scale.
            val bounds = panel.localBoundingBoxOf(coordinates, clipBounds = false)
            placement = coordinates
            target.source.coverCoordinates = coordinates
            target.bounds = bounds
            target.source.coverBounds = bounds
            if (target.source.headerBounds == Rect.Zero) target.source.headerBounds = bounds
            val clipped = panel.localBoundingBoxOf(coordinates, clipBounds = true)
                .intersect(Rect(Offset.Zero, panel.size.toSize()))
            target.source.pageArtworkMostlyVisible = IrideExpansionMotion.isArtworkMostlyVisible(bounds, clipped)
        }
    }.drawWithContent {
        // Record even while the flight owns visibility. An alpha-zero parent can skip
        // this draw completely, leaving an empty layer at the final handover.
        target.layer.record { this@drawWithContent.drawContent() }
        target.recorded = true
        if (ready) target.recordedReadyArtwork = true
        if (progress.value >= 1f || target.source.returnWithDissolve) {
            val bounds = Rect(Offset.Zero, size)
            val artwork = target.artwork ?: return@drawWithContent
            if (artwork === target.layer) {
                drawLayer(artwork)
                drawCollectionArtworkBorder(size, stroke)
            } else {
                drawCollectionCover(artwork, bounds)
            }
            if (playlist) {
                val panelScale = 1f - (gesture?.shrink ?: 0f)
                val corner = if (layoutDirection == LayoutDirection.Ltr) bounds.bottomRight else bounds.bottomLeft
                scale(1f / panelScale, pivot = corner) { drawPlaylistArtworkBadgeForCover(bounds, badgePainter) }
            }
        }
    }
}

@Composable
internal fun CollectionExpansionPage(entry: NavBackStackEntry, content: @Composable () -> Unit) {
    val navigation = LocalCollectionExpansionNavigation.current
    val visibility = LocalAnimatedVisibilityScope.current
    val source = navigation?.destination(entry)
    if (source == null || visibility == null || rememberReducedMotion()) {
        if (entry.routePattern()?.startsWith("artist/{artistId}") == true) {
            // ArtistScreen records inside its own moving panel, just like collection pages.
            content()
        } else {
            Box(Modifier.fillMaxSize().then(irideExpansionBackdropModifier(entry.id))) { content() }
        }
        return
    }
    val pageActive = source === navigation.source && LocalCollectionExpansionActive.current
    val playlist = source.kind == CollectionExpansionKind.Playlist
    val stroke = MaterialTheme.colorScheme.strokeCard
    val progress = visibility.transition.animateFloat(
        transitionSpec = { tween(IrideExpansionMotion.Duration, easing = IrideExpansionMotion.Easing) },
        label = "collectionExpansion",
    ) { if (!pageActive || it == EnterExitState.Visible) 1f else 0f }
    val backdrop = LocalIrideExpansionBackdrop.current
    // An interrupted enter can target PreEnter again when the entry is popped.
    val returning = pageActive && backdrop?.returning == true
    val returnWithDissolve = returning && source.returnWithDissolve
    SideEffect { if (pageActive) backdrop?.progress = progress }
    val layer = remember(source) { navigation.coverLayer(source) }
    val target = remember(entry.id, layer) { CollectionExpansionTarget(layer, source) }
    LaunchedEffect(target) {
        if (source.returnArtwork != null) return@LaunchedEffect
        snapshotFlow { target.recordedReadyArtwork }.first { it }
        // The lazy header can discard the drawing nodes referenced by the recorded layer.
        // Keep its pixels on the page so the dissolve also works after the header is disposed.
        try {
            source.returnArtwork = layer.toImageBitmap()
        } catch (_: IllegalStateException) {
            // A rapid navigation can retire the layer during readback.
        }
    }
    var frame by remember(entry.id) { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val coverSize = with(density) { 240.dp.toPx() }
    val artworkInset = when (entry.routePattern()) {
        // AlbumScreen adds this inset inside its header Column.
        "album/{albumId}" -> 12.dp
        "local_playlist/{playlistId}" -> 0.dp
        "online_playlist/{playlistId}" -> 12.dp
        "top_playlist/{top}" -> 16.dp
        else -> IrideFolio.GeneratedPlaylistTopPadding
    }
    val topPadding = iridePageStatusBarTopPadding() + IrideFolio.TitleAfterControls + artworkInset
    val coverTop = with(density) { topPadding.toPx() }
    val gesture = rememberIridePanelGesture(
        enabled = pageActive && visibility.transition.targetState == EnterExitState.Visible,
        onDismiss = navigation::popBackStack,
    )
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        frame = it.boundsInWindow()
    }) {
        Box(Modifier.fillMaxSize()
            .then(irideExpansionOverlayModifier {
                pageActive && (visibility.transition.isRunning || backdrop?.inFlight == true || backdrop?.gesturePreview == true)
            }),
        ) {
            Box(Modifier.fillMaxSize()
                .then(irideExpansionPanelModifier(source.bounds,
                    minOf(source.bounds.width, source.bounds.height) * IrideExpansionMotion.ArtworkCornerFraction,
                    progress, gesture) { frame })
                .then(irideExpansionBackdropModifier(entry.id))
                .onPlaced { target.panelCoordinates = it }
                .background(Color.Black),
            ) {
                CompositionLocalProvider(
                    LocalCollectionExpansionTarget provides target,
                    LocalCollectionExpansionProgress provides progress,
                    LocalCollectionPanelGesture provides gesture,
                    LocalCollectionExpansionSource provides source,
                    LocalIrideExpansionContentMotion provides null,
                ) {
                    content()
                    if (!target.recorded && source.title.isNotBlank()) {
                        Column(
                            Modifier.fillMaxWidth().graphicsLayer {
                                alpha = IrideExpansionMotion.contentAlpha(progress.value)
                            }
                                .drawWithContent { if (!target.recorded) drawContent() }
                                .padding(top = topPadding + 240.dp + 20.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) { IrideCollectionTitle(title = source.title) }
                    }
                }
                if (returnWithDissolve) {
                    Box(Modifier.matchParentSize().irideExpansionReturnArtwork(source.bounds, progress, gesture, { frame }) { bounds, _ ->
                        val pageArtworkBounds = source.headerBounds.takeIf { it.width > 0f && it.height > 0f }
                            ?: target.bounds.takeIf { it.width > 0f && it.height > 0f }
                            ?: Rect(
                                (frame.width - coverSize) / 2f,
                                coverTop,
                                (frame.width + coverSize) / 2f,
                                coverTop + coverSize,
                            )
                        val sourceArtworkBounds = Rect(Offset.Zero, Size(bounds.width, bounds.width))
                        val artworkBounds = IrideExpansionMotion.cover(
                            sourceArtworkBounds,
                            pageArtworkBounds,
                            progress.value,
                        )
                        val artwork = target.fallback?.takeUnless { it.isReleased }
                            ?: target.artwork?.takeUnless { it.isReleased }
                        if (artwork != null) {
                            drawCollectionCover(artwork, artworkBounds)
                        } else {
                            val image = source.returnArtwork ?: return@irideExpansionReturnArtwork
                            drawCollectionArtworkImage(image, artworkBounds)
                        }
                        if (playlist) {
                            drawPlaylistArtworkBadgeForCover(artworkBounds, source.badgePainter)
                        }
                    })
                }
            }
            // Keep visible artwork flight above the panel.
            Box(Modifier.fillMaxSize().drawWithContent {
                val p = progress.value
                if (frame.width <= 0f || frame.height <= 0f) return@drawWithContent
                val measuredBounds = target.bounds
                val fallbackBounds = measuredBounds.takeIf { it.width > 0f } ?: Rect(
                    (frame.width - coverSize) / 2f, coverTop,
                    (frame.width + coverSize) / 2f, coverTop + coverSize,
                )
                if (p <= 0f) return@drawWithContent
                val restingFrame = Rect(Offset.Zero, frame.size)
                val draggedFrame = gesture.frame(frame).translate(-frame.topLeft)
                val sourceBounds = source.bounds.translate(-frame.topLeft)
                // Keep the tapped image visible while the lazy header is still being built.
                val shouldDrawFlight = !returnWithDissolve && (p < 1f || !target.recorded)
                if (shouldDrawFlight) {
                    val retraceOpening = returning && visibility.transition.currentState == EnterExitState.PreEnter &&
                        target.opening != null
                    val useClosingPath = returning && !retraceOpening
                    val flight = (if (useClosingPath) target.closing else target.opening) ?: run {
                        val cover = if (useClosingPath) {
                            target.artwork
                        } else {
                            target.fallback?.takeUnless { it.isReleased } ?: target.artwork
                        }
                        if (cover == null || cover.isReleased || cover.size.width <= 0 || cover.size.height <= 0) {
                            null
                        } else {
                            val destination = if (useClosingPath) {
                                source.headerBounds.takeIf { it.width > 0f } ?: fallbackBounds
                            } else fallbackBounds
                            CollectionArtworkFlight(
                                cover,
                                sourceBounds,
                                destination,
                                fallbackBounds,
                            ).also {
                                it.destinationMeasured = target.bounds.width > 0f
                                if (useClosingPath) target.closing = it else target.opening = it
                            }
                        }
                    }
                    if (flight != null) {
                        if (!returning && !flight.destinationMeasured && target.bounds.width > 0f) {
                            // Late header layout must continue from the current cover rather
                            // than jump from a guessed endpoint at the end of the flight.
                            flight.path.resolveDestination(target.bounds, p)
                            flight.destinationMeasured = true
                        }
                        val rect = if (useClosingPath) {
                            val returnBounds = IrideExpansionMotion.restoredCover(flight.path.destination, flight.scrolledBounds, p)
                            val destination = IrideExpansionMotion.mapToPanel(returnBounds, restingFrame, draggedFrame)
                            IrideExpansionMotion.cover(sourceBounds, destination, p)
                        } else {
                            IrideExpansionMotion.mapArtworkToDraggedPanel(flight.path.frame(p), sourceBounds, restingFrame, draggedFrame, p)
                        }
                        val artwork = if (useClosingPath) flight.layer else target.artwork ?: flight.layer
                        val visibleBounds = source.visibleBounds.translate(-frame.topLeft)
                        val clip = if (visibleBounds.isEmpty) sourceBounds else sourceBounds.intersect(visibleBounds)
                        val compact = 1f - p
                        clipRect(
                            left = rect.left + (clip.left - sourceBounds.left) * compact,
                            top = rect.top + (clip.top - sourceBounds.top) * compact,
                            right = rect.right - (sourceBounds.right - clip.right) * compact,
                            bottom = rect.bottom - (sourceBounds.bottom - clip.bottom) * compact,
                        ) {
                            val artworkStroke = when {
                                artwork !== target.layer -> null
                                returning -> source.borderColor ?: target.stroke ?: stroke
                                else -> target.stroke ?: stroke
                            }
                            drawCollectionCover(artwork, rect, artworkStroke)
                            if (playlist) {
                                drawPlaylistArtworkBadgeForCover(
                                    rect,
                                    if (returning) source.badgePainter else target.badgePainter,
                                )
                            }
                        }
                    }
                }
            })
        }
    }
}

private fun DrawScope.drawCollectionCover(layer: GraphicsLayer, bounds: Rect, stroke: Color? = null) {
    if (bounds.isEmpty || layer.isReleased || layer.size.width <= 0 || layer.size.height <= 0) return
    translate(bounds.left, bounds.top) {
        val outline = CollectionArtworkShape.createOutline(bounds.size, layoutDirection, this)
        clipPath(Path().apply { addOutline(outline) }) {
            drawCollectionArtworkLayer(layer, Rect(Offset.Zero, bounds.size))
        }
        if (stroke != null) drawCollectionArtworkBorder(bounds.size, stroke)
    }
}

private fun DrawScope.drawCollectionArtworkImage(image: ImageBitmap, bounds: Rect) {
    if (bounds.isEmpty) return
    translate(bounds.left, bounds.top) {
        val localBounds = Rect(Offset.Zero, bounds.size)
        val outline = CollectionArtworkShape.createOutline(bounds.size, layoutDirection, this)
        clipPath(Path().apply { addOutline(outline) }) {
            drawIrideReturnArtwork(image, localBounds)
        }
    }
}

private fun DrawScope.drawCollectionArtworkLayer(layer: GraphicsLayer, bounds: Rect) {
    if (layer.isReleased || layer.size.width <= 0 || layer.size.height <= 0) return
    val coverScale = maxOf(bounds.width / layer.size.width, bounds.height / layer.size.height)
    val x = bounds.center.x - layer.size.width * coverScale / 2f
    val y = bounds.center.y - layer.size.height * coverScale / 2f
    translate(x, y) { scale(coverScale, pivot = Offset.Zero) { drawLayer(layer) } }
}

private fun DrawScope.drawCollectionArtworkBorder(size: Size, stroke: Color) {
    drawIrideArtworkBorder(collectionArtworkBorderWidthPx(size), stroke, CollectionArtworkShape, size)
}

internal fun collectionArtworkBorderWidthPx(size: Size): Float =
    size.minDimension * (IrideBaseBorderWidth / GridThumbnailHeight)

@Composable
private fun irideExpansionOverlayModifier(renderInOverlay: () -> Boolean): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return Modifier
    return with(scope) {
        Modifier.renderInSharedTransitionScopeOverlay(renderInOverlay = renderInOverlay, zIndexInOverlay = 1f)
    }
}
