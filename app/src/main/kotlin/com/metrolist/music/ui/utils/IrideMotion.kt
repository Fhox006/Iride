/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metrolist.music.constants.IrideAnimationsKey
import com.metrolist.music.utils.rememberPreference
import kotlin.random.Random

/**
 * Motion vocabulary for the New Iride UI.
 *
 * The register is monospace/terminal: things are *composed*, not faded in. A line reveals left to
 * right like a printed row; a value counts up instead of appearing; a control answers the finger.
 * Two rules hold the whole thing together:
 *
 *  1. **Nothing loops on a loaded screen.** Every effect here is one-shot — it runs on first mount
 *     or on touch, then stops. The only repeating animation in the app's Iride surfaces is the
 *     loading skeleton, which dies when the data lands.
 *  2. **Reduced motion is a hard off, not a speed-up.** [rememberReducedMotion] folds the user's
 *     own preference and the system animator scale into one boolean; every helper below checks it
 *     and jumps straight to the end state.
 */
object IrideMotion {
    /** Entrances: fast start, long soft landing. The house easing. */
    val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Movement of something already on screen (height, scroll, container resize). */
    val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

    const val Quick = 160
    const val Short = 240
    const val Medium = 300
    const val Long = 420

    /** Gap between siblings in a cascade. Short enough to read as one gesture, not a queue. */
    const val StaggerStep = 40

    /** Press feedback for cards and rows. */
    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /**
     * Shared `animateItem` spec for every Iride header screen (artist/album). Without a shared spec,
     * shelves repositioning after the header resizes race the header's own `animateContentSize` tween
     * against LazyColumn's default spring — two different motion curves settling the same event, which
     * reads as the layout "recalculating" rather than moving once.
     */
    val PlacementSpec = tween<IntOffset>(Medium, easing = EaseOutQuart)
}

/**
 * Landing-entrance state for the bottom-nav tab roots (Home/Library), kept outside any
 * composable's `remember` scope so it survives a tab switch.
 *
 * NavHost disposes a tab's composable the moment you switch away and recomposes it from scratch
 * when you switch back — a plain `remember` resets with it, so the "plays once" entrance in
 * [rememberSectionEnter] replayed on every revisit instead of just the first-ever landing. This
 * is a process-lifetime singleton, so "once" actually means once per app session, not once per
 * mount.
 */
object IrideTabEntrance {
    private val revealedTabs = mutableSetOf<String>()
    private val sections = mutableMapOf<String, MutableSet<String>>()

    fun wasRevealed(tab: String) = tab in revealedTabs
    fun markRevealed(tab: String) { revealedTabs += tab }
    fun sectionsFor(tab: String): MutableSet<String> = sections.getOrPut(tab) { mutableSetOf() }
}

/**
 * True when animations must not play: the user turned Iride animations off in Settings, or the
 * system animator scale is 0 ("Remove animations" / developer options).
 *
 * The system value is read once per composition entry — changing it restarts the activity anyway.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val animationsEnabled by rememberPreference(IrideAnimationsKey, defaultValue = true)
    val systemDisabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    return !animationsEnabled || systemDisabled
}

/**
 * 1f normally; after the app has been backgrounded (`ON_STOP`) and comes back (`ON_START`), drops
 * to 0f and fades back up over [fadeMillis].
 *
 * Compose's `rememberInfiniteTransition` phases its animations off the system frame clock, which
 * keeps advancing while the app is stopped. A continuously-animated surface (the animated gradient
 * backgrounds) is drawn mid-cycle when the app resumes, at a position far from where it was left —
 * a hard visual cut. Wrapping that surface's alpha in this masks the cut as a soft fade to the new
 * position instead of a jump.
 */
@Composable
fun rememberResumeFadeAlpha(fadeMillis: Int = 500): Float {
    val reducedMotion = rememberReducedMotion()
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasStopped by remember { mutableStateOf(false) }
    var fadeKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> wasStopped = true
                Lifecycle.Event.ON_START -> if (wasStopped) {
                    wasStopped = false
                    fadeKey++
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (reducedMotion) return 1f
    val alpha = remember(fadeKey) { Animatable(if (fadeKey == 0) 1f else 0f) }
    LaunchedEffect(fadeKey) {
        if (fadeKey > 0) alpha.animateTo(1f, tween(fadeMillis, easing = IrideMotion.EaseOutExpo))
    }
    return alpha.value
}

/**
 * 0f→1f once, [delayMillis] after [play] turns true, then holds at 1f. The building block for every
 * entrance here: hand the result to [irideEnter] or [revealMask].
 *
 * Returns 1f immediately under reduced motion, and stays at 0f while [play] is false — so a caller
 * can gate a cascade on "the data arrived" without the content flashing in first.
 */
@Composable
fun rememberEnterProgress(
    play: Boolean,
    delayMillis: Int = 0,
    durationMillis: Int = IrideMotion.Medium,
    easing: Easing = IrideMotion.EaseOutExpo,
): Float {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return if (play) 1f else 0f
    // Kept out of the animation's own recomposition: `play` flipping is the trigger, and the
    // start-at-0 frame must exist before the tween runs or the first frame lands fully drawn.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(play) { if (play) started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis, delayMillis, easing),
        label = "irideEnter",
    )
    return progress
}

/**
 * 0f or 1f based on [active], animated between the two with a fixed [durationMillis]. The trigger
 * fires once per [active] transition, so show/hide reads as one motion independent of whatever is
 * flipping the boolean (scroll, layout, focus, …).
 *
 * Used by top-bar frosted overlays that should pop in/out in a constant time even when the
 * triggering signal moves quickly or slowly.
 */
@Composable
fun rememberDiscreteProgress(
    active: Boolean,
    durationMillis: Int = IrideMotion.Short,
    easing: Easing = FastOutSlowInEasing,
): Float {
    val reducedMotion = rememberReducedMotion()
    val target = if (active) 1f else 0f
    if (reducedMotion) return target
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = easing),
        label = "irideDiscreteProgress",
    )
    return progress
}

/**
 * The standard Iride entrance: fade up from [offsetY] below its resting place.
 *
 * [progress] comes from [rememberEnterProgress]. Pure graphicsLayer — no layout pass, no
 * recomposition per frame.
 */
fun Modifier.irideEnter(
    progress: Float,
    offsetY: Dp = 8.dp,
): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * offsetY.toPx()
}

/**
 * Entrance for small round controls: grows in from [from] while fading. Icons read better scaling up
 * than sliding, which on a 40dp target just looks like a glitch.
 */
fun Modifier.irideEnterScale(
    progress: Float,
    from: Float = 0.7f,
): Modifier = graphicsLayer {
    alpha = progress
    val s = from + (1f - from) * progress
    scaleX = s
    scaleY = s
}

/**
 * Wipes content in from the left edge, like a line being printed. The Iride signature for text that
 * shouldn't be spelled out letter-by-letter (section titles, eyebrows, the top bar title) — the
 * typewriter is reserved for the one hero moment per screen.
 */
fun Modifier.revealMask(progress: Float): Modifier = drawWithContent {
    when {
        progress >= 1f -> drawContent()
        progress <= 0f -> Unit
        else -> clipRect(right = size.width * progress) { this@drawWithContent.drawContent() }
    }
}

/**
 * Shrinks slightly while held. Physical answer to the finger for whole rows and cards, where a
 * ripple reads as nothing.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val reducedMotion = rememberReducedMotion()
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            pressed = when (interaction) {
                is PressInteraction.Press -> true
                is PressInteraction.Release, is PressInteraction.Cancel -> false
                else -> pressed
            }
        }
    }
    if (reducedMotion) return this
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = IrideMotion.PressSpring,
        label = "irideTapScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private const val GRAIN_TILE = 128

/**
 * Static monochrome film grain, tiled. Deliberately *not* animated: a shimmering grain layer is a
 * permanent loop on a loaded screen, which is exactly what this UI must not have. Fixed noise still
 * breaks the flatness of a large photographic surface and keeps the print/CRT register.
 *
 * Null under reduced motion (grain is decoration, and decoration is the first thing to drop).
 */
@Composable
fun rememberGrainBrush(): Brush? {
    if (rememberReducedMotion()) return null
    return remember {
        val pixels = IntArray(GRAIN_TILE * GRAIN_TILE) {
            val v = Random.nextInt(0, 256)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bitmap = Bitmap.createBitmap(pixels, GRAIN_TILE, GRAIN_TILE, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}

/** Lays [brush] over the content at a whisper. No-op when the brush is null. */
fun Modifier.grainOverlay(
    brush: Brush?,
    alpha: Float = 0.035f,
): Modifier = if (brush == null) this else drawWithContent {
    drawContent()
    drawRect(brush = brush, alpha = alpha)
}

/**
 * One step of a header cascade (name → subtitle → actions…), played only until [revealed] turns
 * true. Header items on these screens live in item 0 of a LazyColumn, which gets disposed once
 * scrolled far enough off screen — an ungated entrance would replay every time it scrolled back.
 * [revealed] is the screen's own one-shot "have I finished landing" flag.
 */
@Composable
fun headerEnter(
    revealed: Boolean,
    play: Boolean,
    delayMillis: Int,
    durationMillis: Int,
): Float = if (revealed) {
    1f
} else {
    rememberEnterProgress(play = play, delayMillis = delayMillis, durationMillis = durationMillis)
}

/**
 * Entrance for a shelf, played **once** per [key] for the lifetime of the screen. Without [seen],
 * a shelf scrolled far off screen and disposed by LazyColumn would replay its wipe-in every time it
 * came back into view.
 */
@Composable
fun rememberSectionEnter(key: String, seen: MutableSet<String>): Float {
    val firstTime = remember(key) { seen.add(key) }
    return if (firstTime) {
        rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
    } else {
        1f
    }
}
