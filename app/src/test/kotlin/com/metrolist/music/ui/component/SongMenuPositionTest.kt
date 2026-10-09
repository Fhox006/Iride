package com.metrolist.music.ui.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMenuPositionTest {
    private val viewport = IntRect(8, 32, 392, 768)
    private val menuSize = IntSize(224, 300)

    @Test
    fun prefersAboveWhenBothSidesFit() {
        assertEquals(IntOffset(136, 44), position(IntRect(312, 352, 360, 400)))
    }

    @Test
    fun opensAboveWhenBelowDoesNotFit() {
        assertEquals(IntOffset(136, 312), position(IntRect(312, 620, 360, 668)))
    }

    @Test
    fun clampsToBothHorizontalEdges() {
        assertEquals(8, position(IntRect(0, 80, 48, 128)).x)
        assertEquals(168, position(IntRect(390, 80, 438, 128)).x)
    }

    @Test
    fun alignsToStartInRightToLeftLayouts() {
        assertEquals(
            IntOffset(100, 136),
            songMenuPosition(IntRect(100, 80, 148, 128), viewport, menuSize, 8, LayoutDirection.Rtl),
        )
    }

    @Test
    fun canUseAllAvailableSpaceBelowWhenAboveIsTooShort() {
        assertEquals(
            IntOffset(136, 468),
            songMenuPosition(IntRect(312, 412, 360, 460), IntRect(8, 200, 392, 768), menuSize, 8, LayoutDirection.Ltr),
        )
    }

    @Test
    fun tallMenuUsesRoomierSideAndRequestsScrolling() {
        val placement = songMenuPlacement(IntRect(312, 352, 360, 400), viewport, IntSize(224, viewport.height), 8, LayoutDirection.Ltr)
        assertEquals(408, placement.position.y)
        assertEquals(360, placement.height)
        assertTrue(placement.constrained)
        assertEquals(viewport.bottom, placement.position.y + placement.height)
    }

    @Test
    fun remainsVisibleAcrossPortraitLandscapeAndNarrowWindows() {
        for (bounds in listOf(viewport, IntRect(48, 8, 752, 352), IntRect(8, 32, 188, 568))) {
            for (height in listOf(42, 300, bounds.height)) {
                val size = IntSize(minOf(224, bounds.width), minOf(height, bounds.height))
                for (x in -48..bounds.right + 48 step 24) {
                    for (y in -48..bounds.bottom + 48 step 24) {
                        for (direction in LayoutDirection.entries) {
                            val placement = songMenuPlacement(IntRect(x, y, x + 48, y + 48), bounds, size, 8, direction)
                            val offset = placement.position
                            assertTrue(offset.x >= bounds.left && offset.x + size.width <= bounds.right)
                            assertTrue(offset.y >= bounds.top && offset.y + placement.height <= bounds.bottom)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun excludesTopControlsNavigationAndMiniPlayerFromViewport() {
        val bounds = songMenuViewport(IntRect(0, 24, 400, 800), controlsBottom = 76, footerTop = 640, margin = 8)
        assertEquals(IntRect(8, 84, 392, 632), bounds)
        val offset = songMenuPosition(IntRect(312, 420, 360, 468), bounds, menuSize, 8, LayoutDirection.Ltr)
        assertEquals(112, offset.y)
        assertTrue(offset.y + menuSize.height < 640)
    }

    @Test
    fun tallMenuScrollsBetweenApplicationPanelsWithoutCoveringThem() {
        val bounds = songMenuViewport(IntRect(0, 24, 400, 800), controlsBottom = 100, footerTop = 610, margin = 8)
        val size = IntSize(224, bounds.height)
        val placement = songMenuPlacement(IntRect(312, 390, 360, 438), bounds, size, 8, LayoutDirection.Ltr)
        assertEquals(108, placement.position.y)
        assertEquals(382, placement.position.y + placement.height)
        assertTrue(placement.constrained)
    }

    @Test
    fun systemInsetsRemainProtectedWhenTheyExceedPanelBounds() {
        assertEquals(
            IntRect(32, 88, 768, 472),
            songMenuViewport(IntRect(24, 80, 776, 480), controlsBottom = 64, footerTop = 520, margin = 8),
        )
    }

    @Test
    fun fullMenuBelowDoesNotRequestScrollbar() {
        val placement = songMenuPlacement(IntRect(312, 80, 360, 128), viewport, menuSize, 8, LayoutDirection.Ltr)
        assertEquals(136, placement.position.y)
        assertEquals(300, placement.height)
        org.junit.Assert.assertFalse(placement.constrained)
    }

    @Test
    fun crampedMenuAboveKeepsCanonicalButtonGap() {
        val placement = songMenuPlacement(IntRect(312, 600, 360, 648), viewport, IntSize(224, 800), 8, LayoutDirection.Ltr)
        assertEquals(32, placement.position.y)
        assertEquals(592, placement.position.y + placement.height)
        assertTrue(placement.constrained)
    }

    @Test
    fun horizontalMarginsAlignWithPageContent() {
        assertEquals(
            IntRect(20, 84, 380, 632),
            songMenuViewport(IntRect(0, 24, 400, 800), 76, 640, 8, horizontalMargin = 20),
        )
    }

    private fun position(anchor: IntRect) = songMenuPosition(anchor, viewport, menuSize, 8, LayoutDirection.Ltr)
}
