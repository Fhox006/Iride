package com.metrolist.music.ui.component

import android.os.Build
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Per-entry snapshots also support album → playlist → album return gestures. */
internal class IrideExpansionBackdrop(private val graphicsContext: GraphicsContext, private val scope: CoroutineScope) {
    var returnBlur by mutableFloatStateOf(0f)
    var gesturePreview by mutableStateOf(false)
    var inFlight by mutableStateOf(false)
    var returning by mutableStateOf(false)
    var progress by mutableStateOf<State<Float>?>(null)
    var entryId by mutableStateOf<String?>(null)
        private set
    var layer by mutableStateOf<GraphicsLayer?>(null)
        private set
    private val layers = linkedMapOf<String, GraphicsLayer>()
    private val snapshots = mutableStateMapOf<String, ImageBitmap>()
    val snapshot: ImageBitmap? get() = snapshots[entryId]

    suspend fun freeze() {
        val id = entryId ?: return
        val scene = layers[id] ?: return
        if (!scene.isReleased && scene.size.width > 0 && scene.size.height > 0) {
            try {
                snapshots[id] = scene.toImageBitmap()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                // A rapid second navigation can retire a scene while its GPU readback runs.
            }
        }
    }
    private val composedEntries = mutableStateMapOf<String, Int>()

    fun attach(id: String) { composedEntries[id] = (composedEntries[id] ?: 0) + 1 }

    fun detach(id: String) {
        val count = (composedEntries[id] ?: 1) - 1
        if (count > 0) composedEntries[id] = count else composedEntries.remove(id)
    }

    val sourceIsComposed: Boolean get() = entryId in composedEntries

    fun capture(id: String?) {
        show(id)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { freeze() }
    }

    fun show(id: String?) {
        entryId = id
        layer = layers[id]
    }

    fun recordingLayer(id: String): GraphicsLayer = layers.getOrPut(id) {
        graphicsContext.createGraphicsLayer().also { if (entryId == id) layer = it }
    }

    val visible: Boolean get() = inFlight || gesturePreview
    val blurProgress: Float get() = if (gesturePreview) 1f else progress?.value ?: returnBlur

    fun frozen(id: String): Boolean = entryId == id && visible

    fun retain(ids: Set<String>) {
        layers.keys.filter { it !in ids && it != entryId }.forEach { id ->
            layers.remove(id)?.let(graphicsContext::releaseGraphicsLayer)
            snapshots.remove(id)
        }
    }

    fun dispose() {
        layer = null
        layers.values.forEach(graphicsContext::releaseGraphicsLayer)
        layers.clear()
        snapshots.clear()
        composedEntries.clear()
    }
}

internal val LocalIrideExpansionBackdrop = staticCompositionLocalOf<IrideExpansionBackdrop?> { null }

@Composable
internal fun rememberExpansionBackdropBlurs(): List<RenderEffect?> {
    val radius = with(LocalDensity.current) { 5.dp.toPx() }
    return remember(radius) {
        List(21) { step ->
            if (step == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null
            else BlurEffect(radius * step / 20f, radius * step / 20f, TileMode.Clamp)
        }
    }
}

@Composable
internal fun irideExpansionBackdropModifier(entryId: String): Modifier {
    val backdrop = LocalIrideExpansionBackdrop.current ?: return Modifier
    val background = LocalScreenFrostBackground.current
    val layer = remember(backdrop, entryId) { backdrop.recordingLayer(entryId) }
    val effects = rememberExpansionBackdropBlurs()
    DisposableEffect(backdrop, entryId) {
        backdrop.attach(entryId)
        onDispose { backdrop.detach(entryId) }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return Modifier.onGloballyPositioned { origin = it.positionOnScreen() }.graphicsLayer {
        // Once pop restores the real source page, it has the same blur as its preview.
        // Keep this effect outside the recording so the retained scene stays sharp.
        renderEffect = if (backdrop.entryId == entryId && backdrop.visible) {
            effects[(backdrop.blurProgress.coerceIn(0f, 1f) * 20f).toInt()]
        } else null
    }.drawWithContent {
        // Record while the source is still visible. Waiting for navigation's next draw can
        // miss it entirely when NavHost removes an entry with ExitTransition.None.
        if (!backdrop.frozen(entryId) || layer.size.width == 0) {
            layer.record {
                drawRect(Color.Black)
                if (background != null && background.content.size.width > 0) {
                    val offset = background.positionOnScreen - origin
                    translate(offset.x, offset.y) { drawLayer(background.content) }
                }
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        } else {
            // Show the real source as soon as NavHost restores it during the closing flight.
            drawContent()
        }
    }
}
