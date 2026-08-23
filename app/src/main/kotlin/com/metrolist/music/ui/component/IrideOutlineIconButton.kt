/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * How the button answers a tap. Each one is a single 0→1 run that ends at rest — nothing here
 * loops, and nothing plays unless the finger asked for it.
 */
enum class IridePressEffect {
    /** Ripple only. */
    None,

    /** Scale punch with a soft overshoot, for a state that just turned on (favourite). */
    Punch,

    /** One full turn, so the icon lands back where it started and a second tap doesn't snap
     *  (shuffle). */
    Spin,

    /** Two rings travelling outward once, like a signal leaving (radio). */
    Pulse,

    /** Damped wobble (game). */
    Shake,

    /** A short hop upward and back (share). */
    Hop,
}

private fun effectDuration(effect: IridePressEffect) = when (effect) {
    IridePressEffect.None -> 0
    IridePressEffect.Punch -> 420
    IridePressEffect.Spin -> 320
    IridePressEffect.Pulse -> 500
    IridePressEffect.Shake -> 380
    IridePressEffect.Hop -> 300
}

/**
 * Secondary action button for the New Iride UI's album/playlist headers: a bare icon with a
 * circular ripple, no filled surface and no border — Iride's headers are meant to feel like a
 * tech console, not a Material tonal-button row.
 *
 * [pressEffect] adds a one-shot reaction on top of the ripple; it is skipped entirely under
 * reduced motion, leaving the plain ripple.
 */
@Composable
fun IrideOutlineIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    loading: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 26.dp,
    pressEffect: IridePressEffect = IridePressEffect.None,
) {
    val reducedMotion = rememberReducedMotion()
    val animated = pressEffect != IridePressEffect.None && !reducedMotion
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(size)
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = {
                    if (animated) {
                        scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    effectDuration(pressEffect),
                                    easing = IrideMotion.EaseOutExpo,
                                ),
                            )
                        }
                    }
                    onClick()
                },
            )
            .then(
                if (animated && pressEffect == IridePressEffect.Pulse) {
                    Modifier.drawBehind {
                        val t = progress.value
                        if (t <= 0f || t >= 1f) return@drawBehind
                        listOf(t, (t - 0.33f).coerceAtLeast(0f)).forEach { ring ->
                            if (ring <= 0f) return@forEach
                            drawCircle(
                                color = tint,
                                radius = this.size.minDimension / 2f * (0.45f + ring * 0.75f),
                                alpha = (1f - ring) * 0.35f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5.dp.toPx(),
                                ),
                            )
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(iconSize - 2.dp), color = tint)
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier
                    .size(iconSize)
                    .then(
                        if (!animated) {
                            Modifier
                        } else {
                            Modifier.graphicsLayer {
                                val t = progress.value
                                val arc = sin(PI * t).toFloat()
                                when (pressEffect) {
                                    IridePressEffect.Punch -> {
                                        val s = 1f + arc * 0.28f
                                        scaleX = s
                                        scaleY = s
                                    }

                                    IridePressEffect.Spin -> rotationZ = 360f * t

                                    IridePressEffect.Shake ->
                                        rotationZ = sin(t * PI.toFloat() * 3f) * 6f * (1f - t)

                                    IridePressEffect.Hop -> translationY = -arc * 5.dp.toPx()

                                    IridePressEffect.Pulse, IridePressEffect.None -> Unit
                                }
                            }
                        },
                    ),
            )
        }
    }
}
