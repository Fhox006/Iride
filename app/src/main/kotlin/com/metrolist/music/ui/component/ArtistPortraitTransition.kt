package com.metrolist.music.ui.component

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import java.util.UUID
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.viewmodels.prefetchArtistPage
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.metrolist.music.ui.utils.rememberReducedMotion
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.flow.first

internal data class ArtistPortraitSeed(
    val sourceKey: String,
    val artistId: String,
    val name: String,
    val artwork: IrideArtistArtwork,
    val thumbnailUrl: String?,
    val bounds: Rect,
    val borderColor: Color,
    val sourceEntryId: String?,
    val sourceControls: Map<String, Rect>,
)

internal class ArtistPortraitNavigation(
    private val navController: NavController,
    private val backdrop: IrideExpansionBackdrop,
) {
    var seed: ArtistPortraitSeed? by mutableStateOf(null)
        private set
    private val flights = mutableMapOf<String, ArtistPortraitSeed>()
    private val controls = mutableMapOf<Any, Pair<String, Rect>>()
    fun updateControl(key: Any, kind: String, bounds: Rect) { controls[key] = kind to bounds }
    fun removeControl(key: Any) { controls.remove(key) }
    private var pending = false
    private var selectedAtMillis = 0L
    private fun hasPendingSelection(): Boolean = pending &&
        android.os.SystemClock.uptimeMillis() - selectedAtMillis < 1500L

    fun upgrade(artistId: String, artwork: IrideArtistArtwork) {
        val current = seed ?: return
        if (current.artistId == artistId && current.artwork != artwork) {
            seed = current.copy(artwork = artwork)
            flights.entries.filter { it.value.sourceKey == current.sourceKey }.forEach { it.setValue(seed!!) }
        }
    }

    fun select(
        sourceKey: String,
        artistId: String,
        name: String,
        artwork: IrideArtistArtwork,
        thumbnailUrl: String?,
        bounds: Rect,
        borderColor: Color,
    ) {
        if (backdrop.lightweightMode) return
        backdrop.capture(navController.currentBackStackEntry?.id)
        seed = ArtistPortraitSeed(sourceKey, artistId, name, artwork, thumbnailUrl, bounds, borderColor, navController.currentBackStackEntry?.id,
            controls.values.associate { it.first to it.second })
        pending = true
        selectedAtMillis = android.os.SystemClock.uptimeMillis()
    }

    fun transition(from: NavBackStackEntry?, to: NavBackStackEntry?): Boolean {
        if (usesSimpleSearchSectionMotion(from, to)) {
            pending = false
            return false
        }
        if (backdrop.lightweightMode) {
            pending = false
            return false
        }
        if (from == null || to == null) return false
        val entering = hasPendingSelection() && to.destination.route?.startsWith("artist/{artistId}?") == true &&
            seed?.sourceEntryId == from.id && to.arguments?.getString("artistId") == seed?.artistId
        val exiting = flights[from.id]?.sourceEntryId == to.id
        pending = false
        if (entering) {
            flights[to.id] = seed!!
            return true
        }
        if (exiting) {
            seed = flights[from.id]
            backdrop.show(to.id)
            return true
        }
        return false
    }

    fun settle(entry: NavBackStackEntry?): Boolean {
        val current = entry?.let { flights[it.id] } ?: return false
        seed = current
        return true
    }

    fun retain(entryIds: Set<String>) {
        flights.keys.retainAll(entryIds)
        if (flights.values.none { it.sourceKey == seed?.sourceKey }) seed = null
    }

    fun isPair(from: NavBackStackEntry, to: NavBackStackEntry): Boolean = !backdrop.lightweightMode && !usesSimpleSearchSectionMotion(from, to) && (
        (hasPendingSelection() && to.destination.route?.startsWith("artist/{artistId}?") == true &&
            seed?.sourceEntryId == from.id && to.arguments?.getString("artistId") == seed?.artistId) ||
            flights[from.id]?.sourceEntryId == to.id ||
            flights[to.id]?.sourceEntryId == from.id
        )
}

internal val LocalArtistPortraitTransitionActive = staticCompositionLocalOf { false }

internal val LocalArtistPortraitNavigation =
    staticCompositionLocalOf<ArtistPortraitNavigation?> { null }

@Composable
internal fun IrideArtistNavigationTransitions(
    navController: NavController,
    onTransitionLockChanged: (touchLocked: Boolean, backLocked: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val graphicsContext = LocalGraphicsContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val lightweightMode by rememberLightweightFolioMode()
    if (lightweightMode) {
        content()
        return
    }
    val backdrop = remember(navController, graphicsContext, density, layoutDirection, lightweightMode) {
        IrideExpansionBackdrop(graphicsContext, scope, density, layoutDirection, lightweightMode)
    }
    val portraits = remember(navController, backdrop) { ArtistPortraitNavigation(navController, backdrop) }
    val collections = remember(navController, backdrop, graphicsContext, density, layoutDirection) {
        CollectionExpansionNavigation(navController, backdrop, graphicsContext, density, layoutDirection)
    }
    val backdropBlurs = rememberExpansionBackdropBlurs(enabled = !backdrop.lightweightMode)
    DisposableEffect(backdrop) { onDispose { backdrop.dispose() } }
    DisposableEffect(collections) { onDispose { collections.dispose() } }

    var active by remember(navController) { mutableStateOf(false) }
    var collectionActive by remember(navController) { mutableStateOf(false) }
    var returning by remember(navController) { mutableStateOf(false) }
    var flying by remember(navController) { mutableStateOf(false) }
    var flight by remember(navController) { mutableStateOf(0) }
    var outgoingEntryId by remember(navController) { mutableStateOf<String?>(null) }
    val reducedMotion = rememberReducedMotion()
    val fallbackWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val fallbackSize = with(LocalDensity.current) { 40.dp.toPx() }
    val fallbackTop = with(LocalDensity.current) { 24.dp.toPx() }
    collections.fallbackBounds = Rect(fallbackWidth / 2f - fallbackSize / 2f, fallbackTop,
        fallbackWidth / 2f + fallbackSize / 2f, fallbackTop + fallbackSize)
    collections.motionEnabled = !reducedMotion && !lightweightMode
    LaunchedEffect(flight) {
        if (flight > 0) {
            val runningFlight = flight
            val outgoing = outgoingEntryId
            try {
                if (flying && !backdrop.lightweightMode) {
                    Animatable(0f).animateTo(1f, tween(IrideExpansionMotion.Duration, easing = IrideExpansionMotion.Easing)) {
                        backdrop.returnBlur = if (returning) 1f - value else value
                    }
                }
                // NavHost starts its animation after composition; keep the final artwork until
                // its outgoing entry actually finishes, including the first layout frame.
                navController.visibleEntries.first { entries -> entries.none { it.id == outgoing } }
                flying = false
                backdrop.inFlight = false
                backdrop.returnBlur = 0f
                backdrop.progress = null
                backdrop.returning = false
                active = portraits.settle(navController.currentBackStackEntry)
                collectionActive = collections.settle(navController.currentBackStackEntry)
                // The visible page's parent snapshot is used by its next predictive gesture.
                backdrop.show(if (collectionActive) collections.source?.entryId else if (active) portraits.seed?.sourceEntryId else null)
                val retainedEntries = navController.currentBackStack.value.map { it.id }.toSet()
                backdrop.retain(retainedEntries)
                portraits.retain(retainedEntries)
                // Retire GPU resources after the final visible artwork has left the display list.
                repeat(2) { withFrameNanos { } }
                collections.releaseRetired()
                backdrop.releaseRetiredScenes()
            } finally {
                if (flight == runningFlight) onTransitionLockChanged(false, false)
            }
        }
    }
    DisposableEffect(navController, portraits, collections, reducedMotion) {
        var previous = navController.currentBackStackEntry
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            val next = controller.currentBackStackEntry
            if (next?.id != previous?.id) {
                outgoingEntryId = previous?.id
                active = portraits.transition(previous, next)
                collectionActive = collections.transition(previous, next)
                returning = !reducedMotion && ((active && next?.id == portraits.seed?.sourceEntryId) ||
                    (collectionActive && next?.id == collections.source?.entryId))
                backdrop.returning = returning
                if ((active || collectionActive) && !reducedMotion) {
                    backdrop.returnBlur = if (returning) 1f else 0f
                    backdrop.progress = null
                    backdrop.inFlight = true
                    backdrop.previewGesture = null
                    flying = true
                    onTransitionLockChanged(true, active && returning)
                } else {
                    flying = false
                    backdrop.inFlight = false
                    backdrop.previewGesture = null
                    backdrop.returnBlur = 0f
                    onTransitionLockChanged(false, false)
                }
                flight++
                previous = next
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
            onTransitionLockChanged(false, false)
        }
    }
    Box {
        // Isolate the retained scene from NavHost. Only the layer's effect changes on
        // animation frames; its full-resolution display list is reused by the renderer.
        Box(Modifier.matchParentSize().graphicsLayer {
            renderEffect = if (backdrop.useRetainedScene && !backdrop.hasFrozenScene) {
                backdropBlurs[(backdrop.blurProgress.coerceIn(0f, 1f) * backdropBlurs.lastIndex).toInt()]
            } else null
        }.drawWithContent {
            if (backdrop.useRetainedScene && !backdrop.drawFrozenScene(this)) {
                backdrop.layer?.takeUnless { it.isReleased }?.let { drawLayer(it) }
            }
        })
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this,
                LocalArtistPortraitNavigation provides portraits,
                LocalArtistPortraitTransitionActive provides active,
                LocalCollectionExpansionNavigation provides collections,
                LocalCollectionExpansionActive provides collectionActive,
                LocalIrideExpansionBackdrop provides backdrop,
            ) { content() }
        }
    }
}

/** Only the tapped tile participates, even when the same artist occurs in several shelves. */
@Composable
internal fun artistPortraitSharedModifier(artistId: String, sourceKey: String? = null): Modifier {
    val seed = LocalArtistPortraitNavigation.current?.seed
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalAnimatedVisibilityScope.current
    val reducedMotion = rememberReducedMotion()
    if (
        !LocalArtistPortraitTransitionActive.current ||
            reducedMotion ||
            seed?.artistId != artistId ||
            sharedScope == null ||
            visibilityScope == null ||
            (sourceKey != null && sourceKey != seed.sourceKey)
    )
        return Modifier

    return with(sharedScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState("artist-portrait-${seed.sourceKey}"),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ ->
                tween(ArtistPortraitMotion.Duration, easing = ArtistPortraitMotion.Easing)
            },
        )
    }
}

internal object ArtistPortraitMotion {
    const val Duration = IrideExpansionMotion.Duration
    const val Dissolve = IrideExpansionMotion.Dissolve
    val Easing = IrideExpansionMotion.Easing
}

/** Clips every layer of the artist page to the same shrinking device-shaped panel. */
@Composable
internal fun artistPortraitPanelModifier(progress: State<Float>, gesture: IridePanelGestureMotion, frame: () -> Rect): Modifier {
    val source = LocalArtistPortraitNavigation.current?.seed?.bounds ?: return Modifier
    if (!LocalArtistPortraitTransitionActive.current || rememberReducedMotion()) return Modifier
    return irideExpansionPanelModifier(source, source.width / 2f, progress, gesture, continuousCorners = false, frame = frame)
}

/** Keep source artwork edge visible while the full artist header returns to its tile. */
@Composable
internal fun Modifier.artistPortraitReturnBorder(
    progress: State<Float>,
    frame: Rect,
    color: Color,
): Modifier {
    val seed = LocalArtistPortraitNavigation.current?.seed ?: return this
    if (frame.width <= 0f) {
        return this
    }
    val baseStroke = with(LocalDensity.current) { IrideBaseBorderWidth.toPx() }
    return drawWithContent {
        drawContent()
        val p = progress.value.coerceIn(0f, 1f)
        val aperture = IrideExpansionMotion.aperture(seed.bounds, frame, p)
        val panelScale = (aperture.width / frame.width).coerceAtLeast(0.001f)
        val strokeWidth = (baseStroke / panelScale).coerceAtMost(size.minDimension * 0.2f)
        val shape = ArtistReturnPhotoShape(IrideExpansionMotion.artistReturnPhotoRoundness(p))
        val outline = shape.createOutline(
            Size(
                (size.width - strokeWidth).coerceAtLeast(0f),
                (size.height - strokeWidth).coerceAtLeast(0f),
            ),
            layoutDirection,
            this,
        )
        translate(strokeWidth / 2f, strokeWidth / 2f) {
            drawPath(Path().apply { addOutline(outline) }, color, style = Stroke(strokeWidth))
        }
    }
}

/** Ink stays at its final size; the moving aperture reveals it without stretching it. */
@Composable
internal fun artistPortraitRevealModifier(
    progress: State<Float>,
    frame: Rect,
    border: Color? = null,
): Modifier {
    val source = LocalArtistPortraitNavigation.current?.seed?.bounds ?: return Modifier
    if (!LocalArtistPortraitTransitionActive.current || rememberReducedMotion()) return Modifier
    return irideExpansionRevealModifier(source, minOf(source.width, source.height) / 2f, progress, frame, border)
}

@Composable
internal fun irideExpansionRevealModifier(
    source: Rect,
    sourceRadius: Float,
    progress: State<Float>,
    frame: Rect,
    border: Color? = null,
): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    return Modifier.onGloballyPositioned { origin = it.positionInWindow() }.drawWithContent {
        val progress = progress.value
        if (progress >= 1f) {
            drawContent()
        } else if (frame.width > 0f && frame.height > 0f) {
            val aperture = IrideExpansionMotion.aperture(source, frame, progress)
                .translate(-origin.x, -origin.y)
            val radius = sourceRadius * (1f - progress)
            val path = Path().apply { addRoundRect(RoundRect(aperture, CornerRadius(radius))) }
            clipPath(path) { this@drawWithContent.drawContent() }
            if (border != null) {
                val borderProgress = (1f - progress).coerceIn(0f, 1f)
                drawPath(path, border.copy(alpha = border.alpha * borderProgress), style = Stroke(0.5.dp.toPx()))
            }
        }
    }
}

@Composable
internal fun artistPortraitOverlayModifier(zIndex: Float, renderInOverlay: () -> Boolean = { true }): Modifier {
    val scope = LocalSharedTransitionScope.current
    if (!LocalArtistPortraitTransitionActive.current || scope == null || rememberReducedMotion()) return Modifier
    return with(scope) {
        Modifier.renderInSharedTransitionScopeOverlay(
            renderInOverlay = renderInOverlay,
            zIndexInOverlay = zIndex,
        )
    }
}

internal data class ArtistControlMotion(val source: Rect, val progress: State<Float>, val sourceControls: Map<String, Rect> = emptyMap())

internal val LocalArtistControlMotion = compositionLocalOf<ArtistControlMotion?> { null }

/** Each control follows the tapped portrait independently, with a small depth offset. */
@Composable
internal fun Modifier.artistControlMotion(kind: String): Modifier {
    val motion = LocalArtistControlMotion.current ?: return this
    val maxTravel = with(LocalDensity.current) { 10.dp.toPx() }
    var center by remember { mutableStateOf<Offset?>(null) }
    return onGloballyPositioned { coordinates ->
        // Position callbacks include layer translation; remove it to retain the resting center.
        val current = coordinates.boundsInWindow().center
        val previous = center
        if (previous == null || motion.progress.value >= 0.999f) center = current
    }.graphicsLayer {
        val target = center ?: motion.source.center
        val progress = motion.progress.value
        val matching = motion.sourceControls[kind]?.let {
            kotlin.math.abs(it.center.x - target.x) < maxTravel && kotlin.math.abs(it.center.y - target.y) < maxTravel
        } == true
        val factor = if (matching) 0f else (1f - progress).coerceIn(0f, 1f) * 0.10f
        translationX = ((motion.source.center.x - target.x) * factor).coerceIn(-maxTravel, maxTravel)
        translationY = ((motion.source.center.y - target.y) * factor).coerceIn(-maxTravel, maxTravel)
        alpha = IrideExpansionMotion.controlsAlpha(progress)
    }
}

internal class ArtistPortraitTileState(
    val key: String,
    var bounds: Rect = Rect.Zero,
)

@Composable
internal fun rememberArtistPortraitTile(
    artistId: String,
    name: String,
    url: String?,
    channelId: String?,
    externalEnabled: Boolean,
): Pair<Modifier, Modifier> {
    val navigation = LocalArtistPortraitNavigation.current ?: return Modifier to Modifier
    val reducedMotion = rememberReducedMotion()
    val entryId = LocalCollectionSourceEntry.current
    val key = rememberSaveable(artistId, entryId) { UUID.randomUUID().toString() }
    val tile = remember(key) { ArtistPortraitTileState(key) }
    val borderColor = MaterialTheme.colorScheme.strokeCard
    val hiddenPortrait = rememberGraphicsLayer()
    val artwork by rememberIrideArtistArtwork(artistId, url, name, channelId, externalEnabled, highResolution = false)
    val currentArtwork by rememberUpdatedState(artwork)
    val artworkLoader = rememberArtistArtworkLoader()
    val artworkRequest = remember(artistId, name, url, channelId, externalEnabled) {
        ArtistArtworkRequest(artistId, name, url, channelId, externalEnabled)
    }
    val scope = LocalAnimatedVisibilityScope.current
    val backdrop = LocalIrideExpansionBackdrop.current
    val inFlight = !reducedMotion && LocalArtistPortraitTransitionActive.current &&
        navigation.seed?.sourceKey == tile.key && scope != null
    if (inFlight) {
        scope.transition.animateFloat(
            transitionSpec = { tween(ArtistPortraitMotion.Duration, easing = ArtistPortraitMotion.Easing) },
            label = "artistTileVisibility",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
    }
    val tap = Modifier.pointerInput(tile, artworkRequest, reducedMotion) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val detailPreload = artworkLoader.prefetchHighResolution(artworkRequest)
            if (externalEnabled && artistId.startsWith("UC")) prefetchArtistPage(artistId)
            val up = waitForUpOrCancellation(PointerEventPass.Initial)
            if (up != null && up.uptimeMillis - down.uptimeMillis < android.view.ViewConfiguration.getLongPressTimeout() &&
                !reducedMotion && currentArtwork != null && tile.bounds.width > 0f && tile.bounds.height > 0f) {
                navigation.select(tile.key, artistId, name, currentArtwork!!, url, tile.bounds, borderColor)
            } else detailPreload?.cancel()
        }
    }
    val portrait = Modifier.onGloballyPositioned { tile.bounds = it.irideUnclippedBoundsInWindow() }
        .drawWithContent {
            if (inFlight && backdrop?.artworkInFlight == true) {
                hiddenPortrait.record { this@drawWithContent.drawContent() }
            } else drawContent()
        }
    return tap to portrait
}
