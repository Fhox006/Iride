package com.metrolist.music.ui.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrideExpansionMotionTest {
    private val frame = Rect(0f, 24f, 412f, 916f)
    private val source = Rect(280f, 540f, 340f, 600f)
    private val header = Rect(86f, 126f, 326f, 366f)

    @Test
    fun classicReturnRequiresAtLeast95PercentOfTheArtworkArea() {
        assertTrue(IrideExpansionMotion.isArtworkMostlyVisible(header, header))
        assertTrue(IrideExpansionMotion.isArtworkMostlyVisible(header, header.translate(0f, 12f)))
        assertFalse(IrideExpansionMotion.isArtworkMostlyVisible(header, header.translate(0f, 13f)))
        assertFalse(IrideExpansionMotion.isArtworkMostlyVisible(header, header.translate(7f, 7f)))
        assertFalse(IrideExpansionMotion.isArtworkMostlyVisible(header, Rect.Zero))
    }

    @Test
    fun artworkDissolveStartsAfterThreeQuartersOfThePanelsTravel() {
        assertEquals(0f, IrideExpansionMotion.returnArtworkAlpha(1f), 0f)
        assertEquals(0f, IrideExpansionMotion.returnArtworkAlpha(0.5f), 0f)
        assertEquals(0f, IrideExpansionMotion.returnArtworkAlpha(0.25f), 0f)
        assertEquals(0.5f, IrideExpansionMotion.returnArtworkAlpha(0.125f), 0f)
        assertEquals(1f, IrideExpansionMotion.returnArtworkAlpha(0f), 0f)
    }

    @Test
    fun lateHeaderMeasurementDoesNotJumpOrMissTheFinalCover() {
        for (size in listOf(32f, 48f, 60f, 160f)) {
            val thumbnail = Rect(20f, 500f, 20f + size, 500f + size)
            for (measuredAt in listOf(0f, 0.08f, 0.4f, 0.85f)) {
                val predictedHeader = header.translate(0f, 28f)
                val flight = CollectionArtworkPath(thumbnail, predictedHeader)
                assertEquals(thumbnail, flight.frame(0f))
                val before = flight.frame(measuredAt)
                flight.resolveDestination(header, measuredAt)
                assertEquals(before, flight.frame(measuredAt))
                assertEquals(header, flight.frame(1f))
                var previousSize = before.width
                for (step in 0..100) {
                    val p = measuredAt + (1f - measuredAt) * step / 100f
                    val cover = flight.frame(p)
                    assertEquals(cover.width, cover.height, 0.001f)
                    assertTrue(cover.width >= previousSize - 0.001f)
                    previousSize = cover.width
                }
            }
        }
    }

    @Test
    fun interruptedOpeningWithLateHeaderMeasurementReturnsToTheTileDuringALargeSwipe() {
        val flight = CollectionArtworkPath(source, header.translate(0f, 28f))
        val measuredAt = 0.6f
        val beforeMeasurement = flight.frame(measuredAt)
        flight.resolveDestination(header, measuredAt)

        for (edge in listOf(-1f, 1f)) {
            val dragged = IrideExpansionMotion.gestureFrame(frame, 0.35f, edge * 0.35f, edge * 1000f)
            fun artworkAt(progress: Float) =
                IrideExpansionMotion.mapArtworkToDraggedPanel(flight.frame(progress), source, frame, dragged, progress)

            val expectedAtMeasurement = IrideExpansionMotion.cover(
                source, IrideExpansionMotion.mapToPanel(header.translate(0f, 28f), frame, dragged), measuredAt,
            )
            assertEquals(beforeMeasurement, flight.frame(measuredAt))
            assertEquals(expectedAtMeasurement.center.x, artworkAt(measuredAt).center.x, 0.001f)
            assertEquals(expectedAtMeasurement.center.y, artworkAt(measuredAt).center.y, 0.001f)
            assertEquals(source, flight.frame(0f))
            assertEquals(source, artworkAt(0f))
            assertEquals(source.center.x, artworkAt(0.00001f).center.x, 0.01f)
            assertEquals(source.center.y, artworkAt(0.00001f).center.y, 0.01f)
        }
    }

    @Test
    fun rectangularPlaylistKeepsItsCornerAtBothHandoffs() {
        val thumbnail = Rect(24f, 500f, 152f, 572f)
        val draggedHeader = header.translate(40f, 24f)
        val flight = CollectionArtworkPath(thumbnail, draggedHeader)

        assertEquals(thumbnail, flight.frame(0f))
        assertEquals(draggedHeader, flight.frame(1f))
        assertEquals(Rect(75f, 325f, 259f, 481f), flight.frame(0.5f))
    }

    @Test
    fun returningPortraitBecomesAnExactCircleAfterItsPigmentIsGone() {
        assertEquals(0f, IrideExpansionMotion.artistReturnPhotoRoundness(1f), 0f)
        assertEquals(0.5f, IrideExpansionMotion.artistReturnPhotoRoundness(0f), 0f)
        for (step in 0..1000) {
            val p = step / 1000f
            val radius = IrideExpansionMotion.artistReturnPhotoRoundness(p)
            assertTrue(radius in 0f..0.5f)
            if (p <= 0.35f) assertEquals(0f, IrideExpansionMotion.contentAlpha(p), 0f)
        }
    }

    @Test
    fun releaseStartsAtTheExactDraggedPanelAndEndsAtTheOriginalTile() {
        for (edge in listOf(-1f, 1f)) {
            for (travel in listOf(-1000f, 0f, 1000f)) {
                val dragged = IrideExpansionMotion.gestureFrame(frame, 0.3f, edge * 0.3f, travel)
                assertEquals(dragged, IrideExpansionMotion.aperture(source, dragged, 1f))
                assertEquals(source, IrideExpansionMotion.aperture(source, dragged, 0f))
                assertEquals(frame.width * 0.7f, dragged.width, 0.001f)
                assertEquals(frame.height * 0.7f, dragged.height, 0.001f)
            }
        }
    }

    @Test
    fun draggedArtworkStaysSquareAndInsideTheSamePanelAtEverySample() {
        for (edge in listOf(-1f, 1f)) {
            val dragged = IrideExpansionMotion.gestureFrame(frame, 0.3f, edge * 0.3f, 150f)
            val destination = IrideExpansionMotion.mapToPanel(header, frame, dragged)
            val releasedArtwork = IrideExpansionMotion.cover(source, destination, 1f)
            assertEquals(destination.left, releasedArtwork.left, 0.001f)
            assertEquals(destination.top, releasedArtwork.top, 0.001f)
            assertEquals(destination.right, releasedArtwork.right, 0.001f)
            assertEquals(destination.bottom, releasedArtwork.bottom, 0.001f)
            for (step in 0..1000) {
                val p = step / 1000f
                val panel = IrideExpansionMotion.aperture(source, dragged, p)
                val artwork = IrideExpansionMotion.cover(source, destination, p)
                assertEquals(artwork.width, artwork.height, 0.001f)
                assertTrue(artwork.left >= panel.left - 0.001f)
                assertTrue(artwork.top >= panel.top - 0.001f)
                assertTrue(artwork.right <= panel.right + 0.001f)
                assertTrue(artwork.bottom <= panel.bottom + 0.001f)
            }
        }
    }

    @Test
    fun cornersRetainTheirProportionsThroughoutTheAspectRatioChange() {
        val sourceRadius = source.width * IrideExpansionMotion.ArtworkCornerFraction
        val deviceRadius = 24f
        for (step in 0..1000) {
            val p = step / 1000f
            val panel = IrideExpansionMotion.aperture(source, frame, p)
            val radius = IrideExpansionMotion.cornerRadius(source, frame, panel, sourceRadius, deviceRadius, p)
            val fraction = radius / minOf(panel.width, panel.height)
            val expected = IrideExpansionMotion.ArtworkCornerFraction * (1f - p) + deviceRadius / frame.width * p
            assertEquals(expected, fraction, 0.00001f)
        }
        assertEquals(sourceRadius, IrideExpansionMotion.cornerRadius(source, frame, source, sourceRadius, deviceRadius, 0f), 0f)
        assertEquals(deviceRadius, IrideExpansionMotion.cornerRadius(source, frame, frame, sourceRadius, deviceRadius, 1f), 0.0001f)
    }

    @Test
    fun cancelledDragRestoresBothPanelAndArtworkWithoutAResidualTransform() {
        val restored = IrideExpansionMotion.gestureFrame(frame, 0f, 0f, 0f)
        assertEquals(frame, restored)
        assertEquals(header, IrideExpansionMotion.mapToPanel(header, frame, restored))
    }

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
    fun scrolledCoverReturnsContinuouslyToTheSourceThumbnail() {
        for (scroll in listOf(0f, 100f, 1500f, 5000f)) {
            val scrolled = header.translate(0f, -scroll)
            fun closing(progress: Float) = IrideExpansionMotion.cover(source,
                IrideExpansionMotion.restoredCover(header, scrolled, progress), progress)
            assertEquals(scrolled, closing(1f))
            assertEquals(source, closing(0f))
            var previous = closing(1f)
            for (step in 999 downTo 0) {
                val cover = closing(step / 1000f)
                assertEquals(cover.width, cover.height, 0.001f)
                assertTrue(kotlin.math.abs(cover.top - previous.top) < 16f)
                assertTrue(kotlin.math.abs(cover.width - previous.width) < 1f)
                previous = cover
            }
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
