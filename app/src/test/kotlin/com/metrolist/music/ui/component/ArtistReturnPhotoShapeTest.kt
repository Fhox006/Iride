package com.metrolist.music.ui.component

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistReturnPhotoShapeTest {
    @Test
    fun finalPhotoIsANativeCircleWithNoExtraPigmentBand() {
        val outline = ArtistReturnPhotoShape(IrideExpansionMotion.artistReturnPhotoRoundness(0f))
            .createOutline(Size(240f, 240f), LayoutDirection.Ltr, Density(1f))
        assertTrue(outline is Outline.Rounded)
        val circle = (outline as Outline.Rounded).roundRect
        assertEquals(Rect(0f, 0f, 240f, 240f), Rect(circle.left, circle.top, circle.right, circle.bottom))
        assertEquals(120f, circle.topLeftCornerRadius.x, 0f)
        assertEquals(circle.topLeftCornerRadius, circle.bottomLeftCornerRadius)
        assertEquals(circle.topLeftCornerRadius, circle.bottomRightCornerRadius)
        assertEquals(circle.topLeftCornerRadius, circle.topRightCornerRadius)
    }

    @Test
    fun everyReturnFrameKeepsTheNativeRoundRectClip() {
        for (step in 0..100) {
            val shape = ArtistReturnPhotoShape(IrideExpansionMotion.artistReturnPhotoRoundness(step / 100f))
            val outline = shape.createOutline(Size(240f, 240f), LayoutDirection.Ltr, Density(3f))
            assertTrue(outline is Outline.Rounded)
        }
    }
}
