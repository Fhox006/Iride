package com.metrolist.music.ui.screens.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class SprayHalo(
    val x: Float,
    val y: Float,
    val radiusX: Float,
    val radiusY: Float,
    val angle: Float = 0f,
    val feather: Float = 0.45f,
    val strength: Float = 1f,
)

data class SpraySettings(
    val edgeBlur: Float = 0.03f,
    val edgePasses: Float = 2f,
    val edgeParticles: Float = 1.1f,
    val mistBlur: Float = 0.095f,
    val mistPasses: Float = 2f,
    val mistStrength: Float = 0.32f,
    val mistOffsetX: Float = -0.10f,
    val mistOffsetY: Float = 0.05f,
    val particleDensity: Float = 1f,
    val particleSize: Int = 1,
    val snapToLetters: Boolean = false,
    val leftHalo: SprayHalo = SprayHalo(x = 0.08f, y = 0.5f, radiusX = 0.75f, radiusY = 0.75f),
    val rightHalo: SprayHalo = SprayHalo(x = 0.92f, y = 0.5f, radiusX = 0.75f, radiusY = 0.75f),
)

/** Draws soft spray lettering with a gentler blur across the middle of the word. */
class SprayTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var text: String = "AVER"
        set(value) {
            if (field != value) {
                field = value
                invalidateCache()
            }
        }

    var settings: SpraySettings = SpraySettings()
        set(value) {
            if (field != value) {
                field = value
                invalidateCache()
            }
        }

    var typeface: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        set(value) {
            if (field != value) {
                field = value
                invalidateCache()
            }
        }

    var showHaloGuides: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var cachedBitmap: Bitmap? = null
    private var textBounds: Rect? = null
    private var outlinePixels = IntArray(0)
    private var noiseSeed = 0x4952494445L
    private var drag: HaloDrag? = null
    private var editingSettings: SpraySettings? = null

    var onSettingsEdited: ((SpraySettings) -> Unit)? = null

    private enum class DragPart { CENTER, ORBIT }

    private data class HaloDrag(
        val side: Int,
        val part: DragPart,
        val startX: Float,
        val startY: Float,
        val initial: SpraySettings,
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateCache()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cachedBitmap?.recycle()
        cachedBitmap = null
    }

    fun regenerateGrain() {
        noiseSeed = System.nanoTime()
        invalidateCache()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val bitmap = cachedBitmap ?: buildSprayBitmap(width, height).also { cachedBitmap = it }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        if (showHaloGuides) {
            textBounds?.let { bounds ->
                val guides = editingSettings ?: settings
                drawHaloGuide(canvas, bounds, guides.leftHalo, Color.CYAN)
                drawHaloGuide(canvas, bounds, guides.rightHalo, Color.MAGENTA)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bounds = textBounds ?: return false
        if (!showHaloGuides) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drag = findDragTarget(event.x, event.y, bounds) ?: return false
                editingSettings = settings
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val gesture = drag ?: return false
                val original = if (gesture.side == 0) gesture.initial.leftHalo else gesture.initial.rightHalo
                val updated = when (gesture.part) {
                    DragPart.CENTER -> {
                        var centerX = bounds.left + bounds.width() * original.x + event.x - gesture.startX
                        var centerY = bounds.top + bounds.height() * original.y + event.y - gesture.startY
                        if (gesture.initial.snapToLetters) {
                            nearestOutline(centerX, centerY)?.let {
                                centerX = it.first
                                centerY = it.second
                            }
                        }
                        original.copy(
                            x = ((centerX - bounds.left) / bounds.width().coerceAtLeast(1)).coerceIn(-0.5f, 1.5f),
                            y = ((centerY - bounds.top) / bounds.height().coerceAtLeast(1)).coerceIn(-0.5f, 1.5f),
                        )
                    }
                    DragPart.ORBIT -> {
                        val geometry = HaloGeometry(bounds, original)
                        val startAngle = atan2(gesture.startY - geometry.centerY, gesture.startX - geometry.centerX)
                        val currentAngle = atan2(event.y - geometry.centerY, event.x - geometry.centerX)
                        val startDistance = hypot(
                            gesture.startX - geometry.centerX, gesture.startY - geometry.centerY,
                        ).coerceAtLeast(1f)
                        val scale = hypot(event.x - geometry.centerX, event.y - geometry.centerY) /
                            startDistance
                        val angle = original.angle + (currentAngle - startAngle) * 180f / PI.toFloat()
                        original.copy(
                            radiusX = (original.radiusX * scale).coerceIn(0.1f, 2.5f),
                            radiusY = (original.radiusY * scale).coerceIn(0.1f, 2.5f),
                            angle = ((angle + 180f) % 360f + 360f) % 360f - 180f,
                        )
                    }
                }
                editingSettings = if (gesture.side == 0) {
                    gesture.initial.copy(leftHalo = updated)
                } else {
                    gesture.initial.copy(rightHalo = updated)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (drag == null) return false
                drag = null
                val edited = editingSettings
                editingSettings = null
                parent?.requestDisallowInterceptTouchEvent(false)
                if (edited != null && edited != settings) {
                    settings = edited
                    onSettingsEdited?.invoke(edited)
                } else {
                    invalidate()
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                drag = null
                editingSettings = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findDragTarget(x: Float, y: Float, bounds: Rect): HaloDrag? {
        val candidates = listOf(settings.leftHalo, settings.rightHalo)
        val centerTolerance = resources.displayMetrics.density * 32f
        val orbitTolerance = resources.displayMetrics.density * 28f
        val centers = candidates.mapIndexed { index, halo ->
            val geometry = HaloGeometry(bounds, halo)
            Triple(index, hypot(x - geometry.centerX, y - geometry.centerY), geometry)
        }
        centers.minByOrNull { it.second }?.let { nearest ->
            if (nearest.second <= centerTolerance) {
                return HaloDrag(nearest.first, DragPart.CENTER, x, y, settings)
            }
        }
        val orbit = centers.minByOrNull { (_, _, geometry) ->
            abs(geometry.distance(x, y) - 1f) * min(geometry.radiusX, geometry.radiusY)
        } ?: return null
        val edgeDistance = abs(orbit.third.distance(x, y) - 1f) *
            min(orbit.third.radiusX, orbit.third.radiusY)
        return if (edgeDistance <= orbitTolerance) {
            HaloDrag(orbit.first, DragPart.ORBIT, x, y, settings)
        } else {
            null
        }
    }

    private fun nearestOutline(x: Float, y: Float): Pair<Float, Float>? {
        val threshold = resources.displayMetrics.density * 28f
        var closest = threshold * threshold
        var result: Pair<Float, Float>? = null
        for (index in outlinePixels) {
            val px = (index % width).toFloat()
            val py = (index / width).toFloat()
            val distance = (px - x) * (px - x) + (py - y) * (py - y)
            if (distance < closest) {
                closest = distance
                result = px to py
            }
        }
        return result
    }

    private fun invalidateCache() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        invalidate()
    }

    private fun buildSprayBitmap(w: Int, h: Int): Bitmap {
        val config = settings
        val random = Random(noiseSeed)
        val textSizePx = min(h * 0.70f, w * 0.45f)
        val raw = renderTextAlphaMask(w, h, textSizePx)
        val bounds = alphaBounds(raw, w, h)
        textBounds = bounds
        outlinePixels = collectOutline(raw, w, h)
        val softEdge = boxBlurGaussianApprox(
            raw, w, h, (textSizePx * config.edgeBlur).toInt(), passes = config.edgePasses,
        )
        val shifted = shiftMask(
            raw, w, h, textSizePx * config.mistOffsetX, textSizePx * config.mistOffsetY,
        )
        val mist = boxBlurGaussianApprox(
            shifted, w, h, (textSizePx * config.mistBlur).toInt(), passes = config.mistPasses,
        )
        val haloWeights = FloatArray(w * h)
        val leftGeometry = HaloGeometry(bounds, config.leftHalo)
        val rightGeometry = HaloGeometry(bounds, config.rightHalo)
        val pixels = IntArray(w * h) { Color.BLACK }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val leftWeight = leftGeometry.weight(x.toFloat(), y.toFloat())
                val rightWeight = rightGeometry.weight(x.toFloat(), y.toFloat())
                val haloWeight = 1f - (1f - leftWeight) * (1f - rightWeight)
                haloWeights[i] = haloWeight
                val mainProbability = (
                    raw[i] + (softEdge[i] - raw[i]) * config.edgeParticles * haloWeight
                    ).coerceIn(0f, 255f) / 255f
                if (random.nextFloat() < mainProbability) pixels[i] = Color.WHITE
            }
        }

        // Sample fewer mist centers for larger particles so size and count can be tuned separately.
        val radius = config.particleSize / 2
        val footprint = if (radius == 0) 1 else 1 + radius * radius * 3
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val mistProbability = mist[i] / 255f * config.mistStrength *
                    config.particleDensity * haloWeights[i]
                if (random.nextFloat() < (mistProbability / footprint).coerceIn(0f, 1f)) {
                    for (dy in -radius..radius) {
                        for (dx in -radius..radius) {
                            val px = x + dx
                            val py = y + dy
                            if (dx * dx + dy * dy <= radius * radius &&
                                px in 0 until w && py in 0 until h
                            ) {
                                pixels[py * w + px] = Color.WHITE
                            }
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private class HaloGeometry(bounds: Rect, halo: SprayHalo) {
        val centerX = bounds.left + bounds.width() * halo.x
        val centerY = bounds.top + bounds.height() * halo.y
        val radiusX = (bounds.height() * halo.radiusX).coerceAtLeast(1f)
        val radiusY = (bounds.height() * halo.radiusY).coerceAtLeast(1f)
        private val radians = halo.angle * PI.toFloat() / 180f
        private val cosine = cos(radians)
        private val sine = sin(radians)
        private val feather = halo.feather.coerceIn(0f, 1f)
        private val strength = halo.strength.coerceIn(0f, 1f)

        fun distance(x: Float, y: Float): Float {
            val dx = x - centerX
            val dy = y - centerY
            val localX = cosine * dx + sine * dy
            val localY = -sine * dx + cosine * dy
            return hypot(localX / radiusX, localY / radiusY)
        }

        fun weight(x: Float, y: Float): Float {
            val distance = distance(x, y)
            if (distance >= 1f) return 0f
            if (feather == 0f || distance <= 1f - feather) return strength
            val progress = ((distance - (1f - feather)) / feather).coerceIn(0f, 1f)
            val eased = progress * progress * (3f - 2f * progress)
            return (1f - eased) * strength
        }
    }

    private fun drawHaloGuide(canvas: Canvas, bounds: Rect, halo: SprayHalo, color: Int) {
        val geometry = HaloGeometry(bounds, halo)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 200
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 1.5f
        }
        val checkpoint = canvas.save()
        canvas.translate(geometry.centerX, geometry.centerY)
        canvas.rotate(halo.angle)
        canvas.drawOval(
            RectF(-geometry.radiusX, -geometry.radiusY, geometry.radiusX, geometry.radiusY),
            paint,
        )
        paint.alpha = 100
        val innerX = geometry.radiusX * (1f - halo.feather)
        val innerY = geometry.radiusY * (1f - halo.feather)
        canvas.drawOval(RectF(-innerX, -innerY, innerX, innerY), paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, paint.strokeWidth * 3f, paint)
        canvas.restoreToCount(checkpoint)
    }

    private fun collectOutline(raw: IntArray, w: Int, h: Int): IntArray {
        val points = ArrayList<Int>()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val index = y * w + x
                if (raw[index] > 128 &&
                    (raw[index - 1] <= 128 || raw[index + 1] <= 128 ||
                        raw[index - w] <= 128 || raw[index + w] <= 128)
                ) {
                    points.add(index)
                }
            }
        }
        return points.toIntArray()
    }

    private fun smoothStep(start: Float, end: Float, value: Float): Float {
        val progress = ((value - start) / (end - start)).coerceIn(0f, 1f)
        return progress * progress * (3f - 2f * progress)
    }

    private fun renderTextAlphaMask(w: Int, h: Int, textSizePx: Float): IntArray {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = textSizePx
            typeface = this@SprayTextView.typeface
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = paint.measureText(text)
        val availableWidth = w * 0.84f
        if (textWidth > availableWidth && textWidth > 0f) {
            paint.textScaleX = availableWidth / textWidth
            paint.getTextBounds(text, 0, text.length, bounds)
        }
        val x = (w - bounds.width()) / 2f - bounds.left
        val y = (h - bounds.height()) / 2f - bounds.top
        canvas.drawText(text, x, y, paint)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        return IntArray(pixels.size) { Color.alpha(pixels[it]) }
    }

    private fun alphaBounds(src: IntArray, w: Int, h: Int): Rect {
        var left = w
        var top = h
        var right = 0
        var bottom = 0
        var found = false

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (src[y * w + x] > 12) {
                    found = true
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        return if (found) Rect(left, top, right + 1, bottom + 1)
        else Rect(0, 0, w, h)
    }

    private fun boxBlurGaussianApprox(
        src: IntArray,
        w: Int,
        h: Int,
        radius: Int,
        passes: Float,
    ): IntArray {
        if (radius <= 0) return src.copyOf()
        var result = src
        val completePasses = passes.toInt().coerceAtLeast(1)
        repeat(completePasses) {
            result = boxBlurHorizontal(result, w, h, radius)
            result = boxBlurVertical(result, w, h, radius)
        }
        val remainder = (passes - completePasses).coerceIn(0f, 1f)
        if (remainder > 0f) {
            val next = boxBlurVertical(boxBlurHorizontal(result, w, h, radius), w, h, radius)
            result = IntArray(result.size) { i ->
                (result[i] + (next[i] - result[i]) * remainder).roundToInt()
            }
        }
        return result
    }

    private fun boxBlurHorizontal(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val out = IntArray(w * h)
        val windowSize = radius * 2 + 1
        for (y in 0 until h) {
            val row = y * w
            var sum = 0
            for (x in -radius..radius) sum += src[row + x.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                out[row + x] = sum / windowSize
                sum += src[row + (x + radius + 1).coerceIn(0, w - 1)] -
                    src[row + (x - radius).coerceIn(0, w - 1)]
            }
        }
        return out
    }

    private fun boxBlurVertical(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val out = IntArray(w * h)
        val windowSize = radius * 2 + 1
        for (x in 0 until w) {
            var sum = 0
            for (y in -radius..radius) sum += src[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = sum / windowSize
                sum += src[(y + radius + 1).coerceIn(0, h - 1) * w + x] -
                    src[(y - radius).coerceIn(0, h - 1) * w + x]
            }
        }
        return out
    }

    private fun shiftMask(src: IntArray, w: Int, h: Int, dx: Float, dy: Float): IntArray {
        val out = IntArray(w * h)
        val offsetX = dx.toInt()
        val offsetY = dy.toInt()
        for (y in 0 until h) {
            val sourceY = y - offsetY
            if (sourceY !in 0 until h) continue
            for (x in 0 until w) {
                val sourceX = x - offsetX
                if (sourceX in 0 until w) {
                    out[y * w + x] = src[sourceY * w + sourceX]
                }
            }
        }
        return out
    }
}
