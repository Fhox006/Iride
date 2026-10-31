/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import android.content.res.Resources
import android.graphics.Point
import android.view.DisplayShape
import android.view.RoundedCorner
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2

/** Detected device screen corner info needed for panel shape computation. */
data class DeviceCornerInfo(
    val bottomLeftRadiusPx: Float,
    val bottomRightRadiusPx: Float,
    val bottomInsetPx: Float,
    val bottomLeftCenterOnScreen: Offset? = null,
    val bottomRightCenterOnScreen: Offset? = null,
    val displayShape: DisplayShape? = null,
    val bottomLeftProfile: List<Offset> = emptyList(),
    val bottomRightProfile: List<Offset> = emptyList(),
    val bottomLeftPhysicalRadius: CornerRadius? = null,
    val bottomRightPhysicalRadius: CornerRadius? = null,
)

private data class PhysicalCornerProfile(
    val center: Offset,
    val radius: CornerRadius,
    val points: List<Offset>,
)

private fun physicalCornerProfile(path: android.graphics.Path, isLeft: Boolean): PhysicalCornerProfile? {
    val bounds = android.graphics.RectF()
    path.computeBounds(bounds, true)
    val approximation = path.approximate(0.25f)
    val points = approximation.indices.step(3).map { Offset(approximation[it + 1], approximation[it + 2]) }
    val sideX = if (isLeft) bounds.left else bounds.right
    val sideY = points.asSequence()
        .filter { abs(it.x - sideX) < 0.01f }
        .maxOfOrNull { it.y } ?: return null
    val bottomPoints = points.filter { abs(it.y - bounds.bottom) < 0.01f }
    val bottomX = if (isLeft) bottomPoints.minOfOrNull { it.x } else bottomPoints.maxOfOrNull { it.x }
    bottomX ?: return null
    val radius = CornerRadius(abs(bottomX - sideX), bounds.bottom - sideY)
    // Many devices expose a default rectangular DisplayShape. Keep RoundedCorner in that case.
    if (radius.x <= 1f || radius.y <= 1f) return null
    val center = Offset(bottomX, sideY)
    val profile = (points.filter {
        it.x >= minOf(sideX, bottomX) && it.x <= maxOf(sideX, bottomX) && it.y >= sideY
    } + Offset(sideX, sideY) + Offset(bottomX, bounds.bottom))
        .distinct()
        .sortedBy { atan2(it.y - center.y, abs(it.x - center.x)) }
        .map { Offset((it.x - center.x) / radius.x, (it.y - center.y) / radius.y) }
    return PhysicalCornerProfile(center, radius, profile)
}

@Suppress("DEPRECATION")
private fun readDeviceCorners(view: View): DeviceCornerInfo {
    val root = view.rootView
    val rootLocation = IntArray(2)
    root.getLocationOnScreen(rootLocation)
    val displaySize by lazy(LazyThreadSafetyMode.NONE) {
        Point().also { view.display?.getRealSize(it) }
    }
    fun frameworkRadius(name: String): Int {
        val id = view.resources.getIdentifier(name, "dimen", "android")
        return if (id == 0) 0 else try {
            view.resources.getDimensionPixelSize(id)
        } catch (_: Resources.NotFoundException) {
            0
        }
    }
    val resourceRadius by lazy(LazyThreadSafetyMode.NONE) {
        frameworkRadius("rounded_corner_radius_bottom").takeIf { it > 0 }
            ?: frameworkRadius("rounded_corner_radius")
    }
    fun corner(position: Int): Pair<Float, Offset?> {
        val displayCorner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.display?.getRoundedCorner(position)?.takeIf { it.radius > 0 }
        } else null
        val windowCorner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            root.rootWindowInsets?.getRoundedCorner(position)?.takeIf { it.radius > 0 }
        } else null
        val corner = displayCorner ?: windowCorner
        if (corner == null) {
            // These framework resources receive the manufacturer's runtime overlays. This is
            // a best-effort fallback for missing public data, not a catalogue of guessed models.
            if (resourceRadius <= 0 || displaySize.x <= 0 || displaySize.y <= 0) return 0f to null
            val radius = resourceRadius.toFloat().coerceAtMost(minOf(displaySize.x, displaySize.y) / 2f)
            val x = if (position == RoundedCorner.POSITION_BOTTOM_LEFT) radius else displaySize.x - radius
            return radius to Offset(x, displaySize.y - radius)
        }
        val origin = if (displayCorner != null) Offset.Zero else {
            Offset(rootLocation[0].toFloat(), rootLocation[1].toFloat())
        }
        return corner.radius.toFloat() to (origin + Offset(corner.center.x.toFloat(), corner.center.y.toFloat()))
    }
    val (leftRadius, leftCenter) = corner(RoundedCorner.POSITION_BOTTOM_LEFT)
    val (rightRadius, rightCenter) = corner(RoundedCorner.POSITION_BOTTOM_RIGHT)
    return DeviceCornerInfo(
        leftRadius, rightRadius, 0f, leftCenter, rightCenter,
        displayShape = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) view.display?.shape else null,
    )
}

/**
 * Reads bottom rounded-corner radii from the current window via [android.view.RoundedCorner]
 * (API 31+). Falls back to zero on older APIs or when corners are unavailable (emulator, split
 * window, etc.).
 *
 * [bottomInsetPx] is the navigation bar inset, read from [WindowInsets.systemBars].
 */
@Composable
fun rememberDeviceCornerInfo(): DeviceCornerInfo {
    val density = LocalDensity.current
    val view = LocalView.current
    val bottomInsetPx = WindowInsets.systemBars.getBottom(density).toFloat()

    var corners by remember(view) { mutableStateOf(readDeviceCorners(view)) }
    DisposableEffect(view) {
        // Insets may be absent during the first composition. Observe layout without replacing
        // the host view's window-insets listener, which Compose also needs.
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnGlobalLayoutListener { corners = readDeviceCorners(view) }
        observer.addOnGlobalLayoutListener(listener)
        corners = readDeviceCorners(view)
        onDispose {
            if (observer.isAlive) observer.removeOnGlobalLayoutListener(listener)
        }
    }
    return remember(corners, bottomInsetPx) {
        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) corners.displayShape?.path else null
        val left = path?.let { physicalCornerProfile(it, true) }
        val right = path?.let { physicalCornerProfile(it, false) }
        corners.copy(
            bottomInsetPx = bottomInsetPx,
            bottomLeftCenterOnScreen = left?.center ?: corners.bottomLeftCenterOnScreen,
            bottomRightCenterOnScreen = right?.center ?: corners.bottomRightCenterOnScreen,
            bottomLeftProfile = left?.points.orEmpty(),
            bottomRightProfile = right?.points.orEmpty(),
            bottomLeftPhysicalRadius = left?.radius,
            bottomRightPhysicalRadius = right?.radius,
        )
    }
}

/**
 * Returns a [Shape] whose top corners are fixed at [topRadius] and whose bottom corners are
 * derived from the device's physical screen rounded corners, reduced by the panel's distance from
 * the screen edges.
 *
 * Measured bounds locate the panel inside the display. Its outline is a parallel inset of the
 * physical contour, with equal side and bottom gaps supplied by the dock layout.
 * DisplayShape supplies the physical curve when available; RoundedCorner supplies a circular
 * fallback. Top corners retain the Folio squircle.
 *
 * On API < 31 or when corner info is unavailable, bottom radii use [fallbackBottomRadius].
 * [bottomSpacing] overrides the total bottom gap when the caller already knows its laid-out inset.
 *
 */
@Composable
fun rememberDeviceMatchedBottomPanelShape(
    horizontalPadding: Dp,
    additionalBottomSpacing: Dp,
    topRadius: Dp = 28.dp,
    bottomSpacing: Dp? = null,
    panelBoundsOnScreen: Rect? = null,
    fallbackBottomRadius: Dp = topRadius,
    simpleOutline: Boolean = false,
    cornerInfo: DeviceCornerInfo = rememberDeviceCornerInfo(),
): Shape {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemBars = WindowInsets.systemBars
    val leftInset = systemBars.getLeft(density, layoutDirection)
    val rightInset = systemBars.getRight(density, layoutDirection)

    return remember(
        density, cornerInfo, horizontalPadding, additionalBottomSpacing, topRadius,
        bottomSpacing, leftInset, rightInset, layoutDirection, panelBoundsOnScreen, fallbackBottomRadius, simpleOutline,
    ) {
        val hPx = with(density) { horizontalPadding.toPx() }
        val bPx = with(density) {
            bottomSpacing?.toPx() ?: (cornerInfo.bottomInsetPx + additionalBottomSpacing.toPx())
        }
        val fallbackPx = with(density) { fallbackBottomRadius.toPx() }
        val leftCenter = cornerInfo.bottomLeftCenterOnScreen
        val rightCenter = cornerInfo.bottomRightCenterOnScreen
        if (panelBoundsOnScreen != null && leftCenter != null && rightCenter != null && !simpleOutline) {
            return@remember ConcentricDevicePanelShape(
                topRadius = topRadius,
                panelBounds = panelBoundsOnScreen,
                leftCenter = leftCenter,
                rightCenter = rightCenter,
                leftRadius = cornerInfo.bottomLeftPhysicalRadius ?: CornerRadius(cornerInfo.bottomLeftRadiusPx),
                rightRadius = cornerInfo.bottomRightPhysicalRadius ?: CornerRadius(cornerInfo.bottomRightRadiusPx),
                leftProfile = cornerInfo.bottomLeftProfile,
                rightProfile = cornerInfo.bottomRightProfile,
            )
        }
        fun effectiveRadius(radius: Float, center: Offset?, sideInset: Int, isLeft: Boolean): CornerRadius {
            if (radius <= 0f && center == null) return CornerRadius(fallbackPx)
            val x: Float
            val y: Float
            if (panelBoundsOnScreen != null && center != null) {
                x = if (isLeft) center.x - panelBoundsOnScreen.left else panelBoundsOnScreen.right - center.x
                y = panelBoundsOnScreen.bottom - center.y
            } else {
                x = radius - hPx - sideInset
                y = radius - bPx
            }
            return if (x > 0f && y > 0f) CornerRadius(x, y) else CornerRadius(fallbackPx)
        }
        val leftRadius = effectiveRadius(
            cornerInfo.bottomLeftRadiusPx, cornerInfo.bottomLeftCenterOnScreen, leftInset, true,
        )
        val rightRadius = effectiveRadius(
            cornerInfo.bottomRightRadiusPx, cornerInfo.bottomRightCenterOnScreen, rightInset, false,
        )
        if (simpleOutline) {
            object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                    val top = CornerRadius(with(density) { topRadius.toPx() })
                    return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, top, top, rightRadius, leftRadius))
                }
            }
        } else {
            DeviceMatchedBottomPanelShape(
                topRadius, leftRadius, rightRadius, cornerInfo.bottomLeftProfile, cornerInfo.bottomRightProfile,
            )
        }
    }
}

/** A true inward offset; scaling corner axes does not preserve the distance between contours. */
internal fun Path.parallelInset(distance: Float): Path {
    if (distance <= 0f) return Path().apply { addPath(this@parallelInset) }
    val stroke = android.graphics.Path()
    android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = distance * 2
        strokeJoin = android.graphics.Paint.Join.ROUND
    }.getFillPath(asAndroidPath(), stroke)
    return Path.combine(PathOperation.Difference, this, stroke.asComposePath())
}

private fun Outline.toPath(): Path = Path().apply {
    when (val outline = this@toPath) {
        is Outline.Generic -> addPath(outline.path)
        is Outline.Rounded -> addRoundRect(outline.roundRect)
        is Outline.Rectangle -> addRect(outline.rect)
    }
}

private class ConcentricDevicePanelShape(
    private val topRadius: Dp,
    private val panelBounds: Rect,
    private val leftCenter: Offset,
    private val rightCenter: Offset,
    private val leftRadius: CornerRadius,
    private val rightRadius: CornerRadius,
    private val leftProfile: List<Offset>,
    private val rightProfile: List<Offset>,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val displayLeft = leftCenter.x - leftRadius.x
        val displayRight = rightCenter.x + rightRadius.x
        val displayBottom = maxOf(leftCenter.y + leftRadius.y, rightCenter.y + rightRadius.y)
        val gap = minOf(panelBounds.left - displayLeft, displayRight - panelBounds.right,
            displayBottom - panelBounds.bottom).coerceAtLeast(0f)
        // Only the display's lower band is needed. Keep its artificial top outside the panel.
        val displayTop = minOf(panelBounds.top - gap - 1f, leftCenter.y, rightCenter.y)
        return try {
            val physicalOutline = DeviceMatchedBottomPanelShape(
                0.dp, leftRadius, rightRadius, leftProfile, rightProfile,
            ).createOutline(Size(displayRight - displayLeft, displayBottom - displayTop), layoutDirection, density)
            val insetPath = physicalOutline.toPath().parallelInset(gap).apply {
                translate(Offset(displayLeft - panelBounds.left, displayTop - panelBounds.top))
            }
            val topPath = FolioShape(topStart = topRadius, topEnd = topRadius)
                .createOutline(size, layoutDirection, density).toPath()
            Outline.Generic(Path.combine(PathOperation.Intersect, topPath, insetPath))
        } catch (_: IllegalArgumentException) {
            // Some device contours cannot be combined by Skia; keep a proportional rounded fallback.
            val left = CornerRadius((leftRadius.x - gap).coerceAtLeast(0f), (leftRadius.y - gap).coerceAtLeast(0f))
            val right = CornerRadius((rightRadius.x - gap).coerceAtLeast(0f), (rightRadius.y - gap).coerceAtLeast(0f))
            val top = CornerRadius(with(density) { topRadius.toPx() })
            Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, top, top, right, left))
        }
    }
}

private class DeviceMatchedBottomPanelShape(
    private val topRadius: Dp,
    private val bottomLeft: CornerRadius,
    private val bottomRight: CornerRadius,
    private val leftProfile: List<Offset>,
    private val rightProfile: List<Offset>,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val topOutline = FolioShape(topStart = topRadius, topEnd = topRadius)
            .createOutline(size, layoutDirection, density)
        val topPath = Path().apply {
            when (topOutline) {
                is Outline.Generic -> addPath(topOutline.path)
                is Outline.Rounded -> addRoundRect(topOutline.roundRect)
                is Outline.Rectangle -> addRect(topOutline.rect)
            }
        }
        val bottomPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - bottomRight.y)
            if (bottomRight.x > 0f && bottomRight.y > 0f) {
                if (rightProfile.isEmpty()) {
                    arcTo(Rect(size.width - bottomRight.x * 2, size.height - bottomRight.y * 2,
                        size.width, size.height), 0f, 90f, false)
                } else {
                    rightProfile.forEach { point ->
                        lineTo(size.width - bottomRight.x + point.x * bottomRight.x,
                            size.height - bottomRight.y + point.y * bottomRight.y)
                    }
                }
            }
            lineTo(bottomLeft.x, size.height)
            if (bottomLeft.x > 0f && bottomLeft.y > 0f) {
                if (leftProfile.isEmpty()) {
                    arcTo(Rect(0f, size.height - bottomLeft.y * 2, bottomLeft.x * 2, size.height), 90f, 90f, false)
                } else {
                    leftProfile.asReversed().forEach { point ->
                        lineTo(bottomLeft.x + point.x * bottomLeft.x,
                            size.height - bottomLeft.y + point.y * bottomLeft.y)
                    }
                }
            }
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Intersect, topPath, bottomPath))
    }
}


@Composable
fun DeviceCornerDebugPanel(
    modifier: Modifier = Modifier,
) {
    var hPadSlider by remember { mutableFloatStateOf(12f) }
    var bSpaceSlider by remember { mutableFloatStateOf(12f) }

    val hPad = hPadSlider.dp
    val bSpace = bSpaceSlider.dp

    val cornerInfo = rememberDeviceCornerInfo()
    val shape = rememberDeviceMatchedBottomPanelShape(hPad, bSpace)
    val density = LocalDensity.current

    val hPx = with(density) { hPad.toPx() }
    val bPx = cornerInfo.bottomInsetPx + with(density) { bSpace.toPx() }
    val edge = maxOf(hPx, bPx)
    val blEff = maxOf(0f, cornerInfo.bottomLeftRadiusPx - edge)
    val brEff = maxOf(0f, cornerInfo.bottomRightRadiusPx - edge)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = hPad, vertical = bSpace)
                .clip(shape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "device BL: ${cornerInfo.bottomLeftRadiusPx.toInt()}px  BR: ${cornerInfo.bottomRightRadiusPx.toInt()}px",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "effective BL: ${blEff.toInt()}px  BR: ${brEff.toInt()}px",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text("H padding: ${hPadSlider.toInt()}dp", style = MaterialTheme.typography.labelSmall)
        Slider(value = hPadSlider, onValueChange = { hPadSlider = it }, valueRange = 0f..64f)
        Text("B spacing: ${bSpaceSlider.toInt()}dp", style = MaterialTheme.typography.labelSmall)
        Slider(value = bSpaceSlider, onValueChange = { bSpaceSlider = it }, valueRange = 0f..64f)
    }
}
