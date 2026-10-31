package com.metrolist.music.ui.component

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.ui.utils.rememberReducedMotion
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal enum class CollectionExpansionKind { Album, Playlist }

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
) {
    var captureRequested by mutableStateOf(false)
    var artwork by mutableStateOf<GraphicsLayer?>(null)
    var coverLayer: GraphicsLayer? = null
    var coverReady = false
    var palette: ArtworkHeaderPalette? = null
    var coverBounds = Rect.Zero
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

    private fun destinationKind(entry: NavBackStackEntry): CollectionExpansionKind? = when (entry.destination.route) {
        "album/{albumId}" -> CollectionExpansionKind.Album
        "local_playlist/{playlistId}", "online_playlist/{playlistId}", "auto_playlist/{playlist}",
        "top_playlist/{top}", "cache_playlist/{playlist}", "daily_chart/{market}",
        "new_releases_chart/{market}" -> CollectionExpansionKind.Playlist
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
            tile.artworkUrl, tile.title,
        ).also { flight ->
            flight.palette = ArtworkHeaderPalettes.cached(tile.artworkUrl)
            tile.layer?.takeIf { !it.isReleased && it.size.width > 0 && it.size.height > 0 }?.let { original ->
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
        CollectionExpansionKind.Album -> entry.destination.route == "album/{albumId}" && entry.arguments?.getString("albumId") == tile.id
        CollectionExpansionKind.Playlist -> when (entry.destination.route) {
            "local_playlist/{playlistId}", "online_playlist/{playlistId}" -> entry.arguments?.getString("playlistId")?.let {
                it.removePrefix("VL") == tile.id.removePrefix("VL") || tile.alternateId?.removePrefix("VL") == it.removePrefix("VL")
            } == true
            "auto_playlist/{playlist}", "cache_playlist/{playlist}" -> entry.arguments?.getString("playlist") == when (tile.id) {
                PlaylistEntity.LIKED_PLAYLIST_ID, PlaylistEntity.STARRED_PLAYLIST_ID -> "liked"
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

    fun isPair(from: NavBackStackEntry, to: NavBackStackEntry): Boolean = motionEnabled &&
        (pendingMatch(from, to) || flights[from.id]?.entryId == to.id || flights[to.id]?.entryId == from.id || fallbackMatch(to))

    fun transition(from: NavBackStackEntry?, to: NavBackStackEntry?): Boolean {
        if (from == null || to == null) return false
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
            source = flights[from.id]
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
            current.layer = tile.layer
            current.bounds = tile.bounds
        }
    }
}

internal val LocalCollectionExpansionNavigation = staticCompositionLocalOf<CollectionExpansionNavigation?> { null }
internal val LocalCollectionExpansionActive = staticCompositionLocalOf { false }
internal val LocalCollectionExpansionProgress = staticCompositionLocalOf<State<Float>?> { null }
internal val LocalCollectionExpansionSource = staticCompositionLocalOf<CollectionExpansionSource?> { null }
internal val LocalCollectionSourceEntry = staticCompositionLocalOf<String?> { null }

/** Waits for the opening flight before starting work unrelated to its rendering. */
internal suspend fun State<Float>?.awaitSettled() {
    val progress = this ?: return
    snapshotFlow { progress.value >= 1f }.first { it }
}

private class CollectionArtworkFlight(val layer: GraphicsLayer, val from: Rect, val to: Rect)

private class CollectionExpansionTarget(val layer: GraphicsLayer, val source: CollectionExpansionSource) {
    val handoff = Animatable(0f)
    var panelCoordinates: LayoutCoordinates? = null
    var opening: CollectionArtworkFlight? = null
    var closing: CollectionArtworkFlight? = null
    var bounds by mutableStateOf(source.coverBounds)
    var recorded by mutableStateOf(layer.size.width > 0 && layer.size.height > 0)
    var ready by mutableStateOf(source.coverReady)
    val fallback: GraphicsLayer? get() = source.artwork
}

private val LocalCollectionExpansionTarget = staticCompositionLocalOf<CollectionExpansionTarget?> { null }

/** A stable key selects one occurrence even when several shelves contain the same collection. */
@Composable
internal fun rememberCollectionExpansionTile(
    kind: CollectionExpansionKind,
    id: String,
    alternateId: String? = null,
    artworkUrl: String? = null,
    title: String = "",
): Pair<Modifier, Modifier> {
    val navigation = LocalCollectionExpansionNavigation.current ?: return Modifier to Modifier
    val reducedMotion = rememberReducedMotion()
    val entryId = LocalCollectionSourceEntry.current
    val key = rememberSaveable(kind, id, entryId) { UUID.randomUUID().toString() }
    val tile = remember(key, alternateId, artworkUrl, title) {
        CollectionExpansionSource(kind, id, entryId, alternateId, key, artworkUrl = artworkUrl, title = title)
    }
    val context = LocalContext.current
    LaunchedEffect(artworkUrl) {
        val url = artworkUrl ?: return@LaunchedEffect
        delay(350)
        withContext(Dispatchers.Default) { ArtworkHeaderPalettes.load(context, url) }
    }
    val layer = rememberGraphicsLayer()
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
    val tap = Modifier.pointerInput(tile, reducedMotion) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            tile.captureRequested = !reducedMotion
            val up = waitForUpOrCancellation(PointerEventPass.Initial)
            if (up != null && !reducedMotion && up.uptimeMillis - down.uptimeMillis < ViewConfiguration.getLongPressTimeout()) {
                navigation.select(tile)
            } else tile.captureRequested = false
        }
    }
    val cover = Modifier.onGloballyPositioned {
        tile.bounds = it.irideUnclippedBoundsInWindow()
        tile.layer = layer
        navigation.reattach(tile)
    }.graphicsLayer { alpha = if (selected) 0f else 1f }.drawWithContent {
        if (tile.captureRequested) {
            layer.record { this@drawWithContent.drawContent() }
            navigation.record(tile, layer)
            drawLayer(layer)
            tile.captureRequested = false
        } else drawContent()
    }
    return tap to cover
}

/** The page owns the layer: scrolling the lazy header away preserves the return artwork. */
@Composable
internal fun Modifier.collectionExpansionTarget(ready: Boolean = true): Modifier {
    val target = LocalCollectionExpansionTarget.current ?: return this
    val progress = LocalCollectionExpansionProgress.current ?: return this
    target.ready = ready
    target.source.coverReady = ready
    return onGloballyPositioned { coordinates ->
        val panel = target.panelCoordinates
        if (panel != null && panel.isAttached && coordinates.isAttached) {
            // Measure in the panel's resting coordinate system, independent of its scale.
            val bounds = panel.localBoundingBoxOf(coordinates, clipBounds = false)
            target.bounds = bounds
            target.source.coverBounds = bounds
        }
    }.graphicsLayer {
        alpha = if (progress.value >= 1f) {
            if (target.ready || target.fallback == null) 1f else 0f
        } else 0f
    }.drawWithContent {
        target.layer.record { this@drawWithContent.drawContent() }
        target.recorded = true
        drawLayer(target.layer)
    }
}

@Composable
internal fun CollectionExpansionPage(entry: NavBackStackEntry, content: @Composable () -> Unit) {
    val navigation = LocalCollectionExpansionNavigation.current
    val visibility = LocalAnimatedVisibilityScope.current
    val source = navigation?.destination(entry)
    if (source == null || visibility == null || rememberReducedMotion()) {
        if (entry.destination.route?.startsWith("artist/{artistId}") == true) {
            // ArtistScreen records inside its own moving panel, just like collection pages.
            content()
        } else {
            Box(Modifier.fillMaxSize().then(irideExpansionBackdropModifier(entry.id))) { content() }
        }
        return
    }
    val pageActive = source === navigation.source && LocalCollectionExpansionActive.current
    val progress = visibility.transition.animateFloat(
        transitionSpec = { tween(IrideExpansionMotion.Duration, easing = IrideExpansionMotion.Easing) },
        label = "collectionExpansion",
    ) { if (!pageActive || it == EnterExitState.Visible) 1f else 0f }
    val backdrop = LocalIrideExpansionBackdrop.current
    SideEffect { if (pageActive) backdrop?.progress = progress }
    val layer = remember(source) { navigation.coverLayer(source) }
    val target = remember(entry.id, layer) { CollectionExpansionTarget(layer, source) }
    LaunchedEffect(target, progress) {
        snapshotFlow { progress.value >= 1f && target.ready }.collectLatest { settled ->
            if (settled) target.handoff.animateTo(1f, tween(150))
            else target.handoff.snapTo(0f)
        }
    }
    var frame by remember(entry.id) { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val coverSize = with(density) { 240.dp.toPx() }
    val artworkInset = when (entry.destination.route) {
        "local_playlist/{playlistId}" -> 0.dp
        "top_playlist/{top}" -> 16.dp
        else -> IrideFolio.GeneratedPlaylistTopPadding
    }
    val topPadding = iridePageStatusBarTopPadding() + IrideFolio.TitleAfterControls + artworkInset
    val coverTop = with(density) { topPadding.toPx() }
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        frame = it.boundsInWindow()
    }) {
        Box(Modifier.fillMaxSize()
            .then(irideExpansionOverlayModifier {
                pageActive && (visibility.transition.isRunning || backdrop?.inFlight == true || backdrop?.gesturePreview == true)
            })
            .then(iridePanelGestureModifier(
                progress = progress,
                enabled = pageActive && visibility.transition.currentState == EnterExitState.Visible && visibility.transition.targetState == EnterExitState.Visible,
                onDismiss = navigation::popBackStack,
            )),
        ) {
            Box(Modifier.fillMaxSize()
                .then(irideExpansionPanelModifier(source.bounds, with(density) { 5.dp.toPx() }, progress) { frame })
                .then(irideExpansionBackdropModifier(entry.id))
                .onPlaced { target.panelCoordinates = it }
                .background(Color.Black),
            ) {
                CompositionLocalProvider(
                    LocalCollectionExpansionTarget provides target,
                    LocalCollectionExpansionProgress provides progress,
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
            }
            // Artwork has one window-space transform. The panel never scales or translates it.
            Box(Modifier.fillMaxSize().drawWithContent {
                val p = progress.value
                if ((p >= 1f && (target.handoff.value >= 1f || target.fallback == null)) || frame.width <= 0f || frame.height <= 0f) return@drawWithContent
                val returning = visibility.transition.targetState == EnterExitState.PostExit
                val flight = (if (returning) target.closing else target.opening) ?: run {
                    val cover = if (returning) {
                        target.layer.takeIf { target.recorded } ?: target.fallback
                    } else {
                        target.fallback ?: target.layer.takeIf { target.recorded }
                    }
                    if (cover == null || cover.isReleased || cover.size.width <= 0 || cover.size.height <= 0) return@drawWithContent
                    val destination = target.bounds.takeIf { it.width > 0f } ?: Rect(
                        (frame.width - coverSize) / 2f, coverTop,
                        (frame.width + coverSize) / 2f, coverTop + coverSize,
                    )
                    // Freeze both endpoints and the artwork once. Loading cannot retarget a flight.
                    CollectionArtworkFlight(cover, source.bounds.translate(-frame.topLeft), destination).also {
                        if (returning) target.closing = it else target.opening = it
                    }
                }
                val rect = IrideExpansionMotion.cover(flight.from, flight.to, p)
                flight.layer.alpha = if (p >= 1f) 1f - target.handoff.value else 1f
                translate(rect.left, rect.top) {
                    val coverScale = rect.width / maxOf(flight.layer.size.width, flight.layer.size.height)
                    scale(coverScale, coverScale, pivot = Offset.Zero) { drawLayer(flight.layer) }
                }
            })
        }
    }
}

@Composable
private fun irideExpansionOverlayModifier(renderInOverlay: () -> Boolean): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return Modifier
    return with(scope) {
        Modifier.renderInSharedTransitionScopeOverlay(renderInOverlay = renderInOverlay, zIndexInOverlay = 1f)
    }
}
