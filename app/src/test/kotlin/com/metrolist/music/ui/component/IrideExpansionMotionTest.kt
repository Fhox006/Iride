package com.metrolist.music.ui.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IrideExpansionMotionTest {
    private val frame = Rect(0f, 24f, 412f, 916f)
    private val source = Rect(280f, 540f, 340f, 600f)
    private val header = Rect(86f, 126f, 326f, 366f)

    @Test
    fun openingAndClosingMeetTheExactSourceAndHeader() {
        assertEquals(source, IrideExpansionMotion.cover(source, header, 0f))
        assertEquals(header, IrideExpansionMotion.cover(source, header, 1f))
        assertEquals(source, IrideExpansionMotion.aperture(source, frame, 0f))
        assertEquals(frame, IrideExpansionMotion.aperture(source, frame, 1f))
    }

    @Test
    fun coverRemainsSquareAndInsideThePanelThroughoutTheFlight() {
        for (step in 0..1000) {
            val p = step / 1000f
            val panel = IrideExpansionMotion.aperture(source, frame, p)
            val cover = IrideExpansionMotion.cover(source, header, p)
            assertEquals(cover.width, cover.height, 0.001f)
            assertTrue(cover.left >= panel.left - 0.001f)
            assertTrue(cover.right <= panel.right + 0.001f)
            assertTrue(cover.top >= panel.top - 0.001f)
            assertTrue(cover.bottom <= panel.bottom + 0.001f)
        }
    }

    @Test
    fun compensatingForThePanelKeepsArtworkAtTheSameWindowPosition() {
        for (step in 0..1000) {
            val p = step / 1000f
            val panel = IrideExpansionMotion.aperture(source, frame, p)
            val scale = panel.width / frame.width
            val cover = IrideExpansionMotion.cover(source, header, p)
            val localX = (cover.left - panel.left) / scale
            val localY = (cover.top - panel.top) / scale
            val localSize = cover.width / scale
            assertEquals(cover.left, panel.left + localX * scale, 0.001f)
            assertEquals(cover.top, panel.top + localY * scale, 0.001f)
            assertEquals(cover.width, localSize * scale, 0.001f)
            assertEquals(cover.height, localSize * scale, 0.001f)
        }
    }

    @Test
    fun scrolledOffscreenArtworkRecentersBeforeThePanelFinishesClosing() {
        val scrolled = header.translate(0f, -1500f)
        assertEquals(scrolled, IrideExpansionMotion.restoredCover(header, scrolled, 1f))
        assertEquals(header, IrideExpansionMotion.restoredCover(header, scrolled, 0.30f))
        assertEquals(header, IrideExpansionMotion.restoredCover(header, scrolled, 0f))
        var previous = scrolled.top
        for (step in 1000 downTo 0) {
            val restored = IrideExpansionMotion.restoredCover(header, scrolled, step / 1000f)
            assertTrue(restored.top >= previous - 0.001f)
            assertEquals(240f, restored.width, 0.001f)
            assertEquals(240f, restored.height, 0.001f)
            previous = restored.top
        }
    }

    @Test
    fun sharedTimingAndOpacityAreContinuousMonotonicAndBounded() {
        var previous = 0f
        for (step in 0..1000) {
            val p = step / 1000f
            val eased = IrideExpansionMotion.Easing.transform(p)
            assertTrue(eased in previous..1f)
            assertTrue(IrideExpansionMotion.contentAlpha(p) in 0f..1f)
            assertEquals(IrideExpansionMotion.contentAlpha(p), IrideExpansionMotion.controlsAlpha(p), 0f)
            previous = eased
        }
        assertEquals(0f, IrideExpansionMotion.Easing.transform(0f), 0f)
        assertEquals(1f, IrideExpansionMotion.Easing.transform(1f), 0f)
        assertEquals(0f, IrideExpansionMotion.contentAlpha(0f), 0f)
        assertEquals(1f, IrideExpansionMotion.contentAlpha(1f), 0f)
    }
}
