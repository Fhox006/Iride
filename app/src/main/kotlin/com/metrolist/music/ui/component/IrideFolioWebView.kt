package com.metrolist.music.ui.component

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.webkit.WebView
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.roundToInt

/** Native scrolling must publish its offset as Compose state so Folio's title can fade. */
class IrideFolioWebState internal constructor() {
    private var view: WebView? = null
    var scrollOffset by mutableIntStateOf(0)
        private set

    fun attach(webView: WebView) {
        view = webView
        scrollOffset = webView.scrollY
        webView.setOnScrollChangeListener { _, _, y, _, _ -> scrollOffset = y }
    }

    internal fun scrollBy(delta: Float): Float {
        val current = view ?: return 0f
        val previous = current.scrollY
        current.scrollBy(0, delta.roundToInt())
        return (current.scrollY - previous).toFloat()
    }
}

@Composable fun rememberIrideFolioWebState(): IrideFolioWebState = remember { IrideFolioWebState() }

@Composable
fun IrideFolioWebState.rememberScrollState(): ScrollableState = rememberScrollableState(::scrollBy)

/**
 * WebView does not normally dispatch nested scroll. Without this bridge a login page consumes every
 * touch itself, and Folio never receives the distance left over at either document edge. Only
 * unconsumed vertical movement is forwarded to the enclosing Compose frame; web links, selection,
 * horizontal movement and the browser's own scrolling remain native. There is no second spring or
 * glow here: AndroidView forwards these events to Folio's spring.
 */
class IrideFolioWebView(context: Context) : WebView(context), NestedScrollingChild3 {
    private val nestedChild = NestedScrollingChildHelper(this)
    private val consumed = IntArray(2)
    private val parentConsumed = IntArray(2)
    private var previousRawY = 0f
    private var touchOffset = 0f
    private var velocityTracker: VelocityTracker? = null

    init {
        isNestedScrollingEnabled = true
        overScrollMode = OVER_SCROLL_NEVER
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousRawY = event.rawY
                touchOffset = 0f
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = (previousRawY - event.rawY).roundToInt()
                previousRawY = event.rawY
                consumed.fill(0)
                dispatchNestedPreScroll(0, delta, consumed, null, ViewCompat.TYPE_TOUCH)
                touchOffset += consumed[1]
                val remaining = delta - consumed[1]
                val previousScroll = scrollY
                val adjusted = MotionEvent.obtain(event)
                adjusted.offsetLocation(0f, touchOffset)
                val handled = super.onTouchEvent(adjusted)
                adjusted.recycle()
                // Do not mistake WebView's touch slop for an edge. Publish leftover movement
                // only when the document really cannot continue in the requested direction.
                val childConsumed = scrollY - previousScroll
                val leftover =
                    if (remaining != 0 && !canScrollVertically(remaining.sign())) {
                        remaining - childConsumed
                    } else 0
                parentConsumed.fill(0)
                dispatchNestedScroll(
                    0,
                    childConsumed,
                    0,
                    leftover,
                    null,
                    ViewCompat.TYPE_TOUCH,
                    parentConsumed,
                )
                touchOffset += parentConsumed[1]
                velocityTracker?.addMovement(event)
                return handled
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val velocity =
                    if (event.actionMasked == MotionEvent.ACTION_CANCEL) 0f
                    else -(velocityTracker?.yVelocity ?: 0f)
                val parentHandled = dispatchNestedPreFling(0f, velocity)
                val adjusted = MotionEvent.obtain(event)
                adjusted.offsetLocation(0f, touchOffset)
                if (parentHandled) adjusted.action = MotionEvent.ACTION_CANCEL
                val handled = super.onTouchEvent(adjusted)
                adjusted.recycle()
                if (!parentHandled)
                    dispatchNestedFling(0f, velocity, canScrollVertically(velocity.sign()))
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
                velocityTracker?.recycle()
                velocityTracker = null
                return handled
            }
        }
        velocityTracker?.addMovement(event)
        return super.onTouchEvent(event)
    }

    private fun Int.sign(): Int = compareTo(0)

    private fun Float.sign(): Int = compareTo(0f)

    override fun onDetachedFromWindow() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
        velocityTracker?.recycle()
        velocityTracker = null
        super.onDetachedFromWindow()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) =
        nestedChild.setNestedScrollingEnabled(enabled)

    override fun isNestedScrollingEnabled(): Boolean = nestedChild.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = nestedChild.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int): Boolean =
        nestedChild.startNestedScroll(axes, type)

    override fun stopNestedScroll() = nestedChild.stopNestedScroll()

    override fun stopNestedScroll(type: Int) = nestedChild.stopNestedScroll(type)

    override fun hasNestedScrollingParent(): Boolean = nestedChild.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int): Boolean =
        nestedChild.hasNestedScrollingParent(type)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
    ): Boolean = nestedChild.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int,
    ): Boolean = nestedChild.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
    ): Boolean =
        nestedChild.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
        )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
    ): Boolean =
        nestedChild.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
        )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray,
    ) =
        nestedChild.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed,
        )

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
        nestedChild.dispatchNestedPreFling(velocityX, velocityY)

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean,
    ): Boolean = nestedChild.dispatchNestedFling(velocityX, velocityY, consumed)
}
