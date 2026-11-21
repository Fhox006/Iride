package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Per-entry snapshots also support album → playlist → album return gestures. */
internal class IrideExpansionBackdrop(
    private val graphicsContext: GraphicsContext,
    private val scope: CoroutineScope,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    val lightweightMode: Boolean = false,
) {
    var returnBlur by mutableFloatStateOf(0f)
    var previewGesture by mutableStateOf<IridePanelGestureMotion?>(null)
    val gesturePreview: Boolean get() = previewGesture != null
    var inFlight by mutableStateOf(false)
    var returning by mutableStateOf(false)
    var progress by mutableStateOf<State<Float>?>(null)
    var entryId by mutableStateOf<String?>(null)
        private set
    var layer by mutableStateOf<GraphicsLayer?>(null)
        private set
    private val layers = linkedMapOf<String, GraphicsLayer>()
    private val snapshots = mutableStateMapOf<String, ImageBitmap>()
    private val pendingCaptures = mutableStateMapOf<String, Boolean>()
    private val captureVersions = mutableMapOf<String, Long>()
    private var frozenScene by mutableStateOf<IrideFrozenBackdrop?>(null)
    private val retiredScenes = mutableListOf<IrideFrozenBackdrop>()
    val hasFrozenScene: Boolean get() = frozenScene != null

    private fun replaceFrozenScene(image: ImageBitmap?) {
        frozenScene?.let(retiredScenes::add)
        frozenScene = image?.let { IrideFrozenBackdrop(it, graphicsContext, density, layoutDirection) }
    }

    fun drawFrozenScene(scope: DrawScope, alpha: Float = 1f): Boolean {
        val scene = frozenScene ?: return false
        if (alpha >= 1f) {
            scene.draw(scope, blurProgress)
        } else if (alpha > 0f) {
            val canvas = scope.drawContext.canvas
            canvas.saveLayer(Rect(Offset.Zero, scope.size), Paint().apply { this.alpha = alpha })
            try {
                scene.draw(scope, blurProgress)
            } finally {
                canvas.restore()
            }
        }
        return true
    }

    fun releaseRetiredScenes() {
        retiredScenes.forEach(IrideFrozenBackdrop::dispose)
        retiredScenes.clear()
    }

    private suspend fun freeze(id: String, version: Long) {
        val scene = layers[id] ?: return
        if (!scene.isReleased && scene.size.width > 0 && scene.size.height > 0) {
            try {
                val image = scene.toImageBitmap()
                if (captureVersions[id] == version && layers[id] === scene && !scene.isReleased) {
                    snapshots[id] = image
                    if (entryId == id) replaceFrozenScene(image)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                // A rapid second navigation can retire a scene while its GPU readback runs.
            }
        }
    }
    private val composedEntries = mutableStateMapOf<String, Int>()

    fun endGesturePreview(gesture: IridePanelGestureMotion) {
        if (previewGesture === gesture) previewGesture = null
    }

    fun attach(id: String) { composedEntries[id] = (composedEntries[id] ?: 0) + 1 }

    fun detach(id: String) {
        val count = (composedEntries[id] ?: 1) - 1
        if (count > 0) composedEntries[id] = count else composedEntries.remove(id)
    }

    private val sourceIsComposed: Boolean get() = entryId in composedEntries

    // During a fast reversal the source can be composed but still have no visible content.
    // Keep one copy of the retained scene behind NavHost until the artwork lands.
    val useRetainedScene: Boolean
        get() = visible &&
            (!sourceIsComposed || gesturePreview || (returning && (progress?.value ?: 1f) > 0f)) &&
            (hasFrozenScene || layer?.let { !it.isReleased && it.size.width > 0 && it.size.height > 0 } == true)

    fun capture(id: String?) {
        show(id)
        if (id == null) return
        if (lightweightMode) {
            pendingCaptures.remove(id)
            snapshots.remove(id)
            replaceFrozenScene(null)
            return
        }
        val version = (captureVersions[id] ?: 0L) + 1L
        captureVersions[id] = version
        // Always refresh after selection: a prior capture may predate a long scroll.
        snapshots.remove(id)
        replaceFrozenScene(null)
        pendingCaptures[id] = true
    }

    fun capturePending(id: String): Boolean = pendingCaptures[id] == true

    fun recorded(id: String) {
        // Capture only after the moving artwork owns the selected thumbnail's pixels.
        if (visible && !artworkInFlight) return
        if (pendingCaptures.remove(id) == true) {
            // Start after the draw pass so child layer visibility has also been applied.
            val version = captureVersions[id] ?: return
            scope.launch { freeze(id, version) }
        }
    }

    fun show(id: String?) {
        if (entryId != id) replaceFrozenScene(snapshots[id])
        entryId = id
        layer = layers[id]
    }

    fun recordingLayer(id: String): GraphicsLayer = layers.getOrPut(id) {
        graphicsContext.createGraphicsLayer().also { if (entryId == id) layer = it }
    }

    val visible: Boolean get() = inFlight || gesturePreview
    val artworkInFlight: Boolean get() = visible && (progress?.value ?: 0f) > 0f
    val blurProgress: Float get() = if (gesturePreview) 1f else progress?.value ?: returnBlur

    fun frozen(id: String): Boolean = entryId == id && visible

    fun retain(ids: Set<String>) {
        layers.keys.filter { it !in ids && it != entryId }.forEach { id ->
            layers.remove(id)?.let(graphicsContext::releaseGraphicsLayer)
            snapshots.remove(id)
            pendingCaptures.remove(id)
            captureVersions.remove(id)
        }
    }

    fun dispose() {
        frozenScene?.dispose()
        frozenScene = null
        releaseRetiredScenes()
        layer = null
        layers.values.forEach(graphicsContext::releaseGraphicsLayer)
        layers.clear()
        snapshots.clear()
        pendingCaptures.clear()
        captureVersions.clear()
        composedEntries.clear()
    }
}

internal val LocalIrideExpansionBackdrop = staticCompositionLocalOf<IrideExpansionBackdrop?> { null }

@Composable
internal fun rememberExpansionBackdropBlurs(enabled: Boolean = true): List<RenderEffect?> {
    val radius = with(LocalDensity.current) { 3.dp.toPx() }
    return remember(radius, enabled) {
        List(13) { step ->
            if (!enabled || step == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null
            else BlurEffect(radius * step / 12f, radius * step / 12f, TileMode.Clamp)
        }
    }
}

@Composable
internal fun irideExpansionBackdropModifier(entryId: String, backgroundColor: Color? = null): Modifier {
    val backdrop = LocalIrideExpansionBackdrop.current ?: return Modifier
    if (backdrop.lightweightMode) return Modifier
    val ambientBackground = LocalScreenFrostBackground.current.takeIf { backgroundColor == null }
    val layer = remember(backdrop, entryId) { backdrop.recordingLayer(entryId) }
    val effects = rememberExpansionBackdropBlurs()
    val visibility = LocalAnimatedVisibilityScope.current
    if (backdrop.entryId == entryId && backdrop.visible && visibility != null) {
        // Also retain sources opened without a selected tile (deep links and headers).
        // ExitTransition.None must not remove them before the requested capture draws.
        visibility.transition.animateFloat(
            transitionSpec = { tween(IrideExpansionMotion.Duration, easing = IrideExpansionMotion.Easing) },
            label = "expansionSourceLifetime",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
    }
    DisposableEffect(backdrop, entryId) {
        backdrop.attach(entryId)
        onDispose { backdrop.detach(entryId) }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return Modifier.onGloballyPositioned { origin = it.positionOnScreen() }.graphicsLayer {
        // Once pop restores the real source page, it has the same blur as its preview.
        // Keep this effect outside the recording so the retained scene stays sharp.
        renderEffect = if (backdrop.entryId == entryId && backdrop.visible && !backdrop.useRetainedScene &&
            (!backdrop.hasFrozenScene || backdrop.returning || backdrop.gesturePreview)
        ) {
            effects[(backdrop.blurProgress.coerceIn(0f, 1f) * effects.lastIndex).toInt()]
        } else null
    }.drawWithContent {
        // The moving destination is never sampled by this flight. Re-recording it
        // feeds changing window coordinates back into drawing on every shape update.
        if (backdrop.visible && backdrop.entryId != entryId) {
            drawContent()
            return@drawWithContent
        }
        // Capture only a selected source. Ordinary scrolling draws the live page directly,
        // without recording a second full-page display list for a hypothetical navigation.
        if (backdrop.capturePending(entryId)) {
            layer.record {
                drawRect(backgroundColor ?: Color.Black)
                if (ambientBackground != null && ambientBackground.content.size.width > 0) {
                    val offset = ambientBackground.positionOnScreen - origin
                    translate(offset.x, offset.y) { drawLayer(ambientBackground.content) }
                }
                this@drawWithContent.drawContent()
            }
            backdrop.recorded(entryId)
            if (!backdrop.useRetainedScene) drawLayer(layer)
        } else if (!backdrop.useRetainedScene &&
            (!backdrop.frozen(entryId) || backdrop.returning || !backdrop.drawFrozenScene(this))
        ) {
            drawContent()
        }
    }
}
