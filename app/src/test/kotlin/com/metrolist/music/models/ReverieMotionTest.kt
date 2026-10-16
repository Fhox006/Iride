package com.metrolist.music.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class ReverieMotionTest {
    @Test fun aChapterOwnsAFixedPoseRatherThanAnElapsedTimeAnimation() {
        for (chapter in 0..8) {
            val pose = reverieFigurePose(chapter)
            repeat(100) { assertEquals(pose, reverieFigurePose(chapter)) }
            assertEquals(.59f, pose.radius, 0f)
        }
        assertEquals(12, REVERIE_RING_COUNT)
    }

    @Test fun chapterChangesMoveTheSingleFigureWithinABoundedArea() {
        val poses = (0..8).map(::reverieFigurePose)
        assertTrue(poses.map { it.x }.distinct().size > 3)
        assertTrue(poses.map { it.y }.distinct().size > 2)
        for (from in 0..8) for (to in 0..8) {
            val first = reverieFigurePose(from)
            val next = reverieFigurePose(to)
            assertTrue(abs(first.x - next.x) <= .12f)
            assertTrue(abs(first.y - next.y) <= .06f)
            assertTrue(abs(first.rotation - next.rotation) <= .3f)
        }
    }

    @Test fun allOutlinesKeepTheSingleFamilyOfRingsNestedDuringEveryTransition() {
        for (from in 0..8) for (to in 0..8) for (step in 0..20) for (point in 0 until 160) {
            val angle = point * (2 * PI / 160).toFloat()
            val radius = radiusBetween(from, to, step / 20f, angle)
            assertTrue(radius.isFinite() && radius in .82f..1.19f)
            var previous = 0f
            for (ring in 1..REVERIE_RING_COUNT) {
                val current = radius * ring / REVERIE_RING_COUNT
                assertTrue(current > previous)
                previous = current
            }
        }
    }

    @Test fun changingOutlineInterpolatesWithoutACutOrExtraCyclesOnLongJumps() {
        for (point in 0 until 160) {
            val angle = point * (2 * PI / 160).toFloat()
            assertEquals(1f, reverieMorphRadius(angle, 0f, 0f, 0f), .0001f)
            for (from in 0..8) for (to in 0..8) for (step in 0 until 100) {
                val before = radiusBetween(from, to, step / 100f, angle)
                val after = radiusBetween(from, to, (step + 1) / 100f, angle)
                assertTrue(abs(before - after) < .004f)
            }
        }
    }

    @Test fun rosetteHasSixPetalsAndIsDistinctFromCircleAndSquare() {
        val lobe = reverieMorphRadius(0f, 0f, 1f, 0f)
        val valley = reverieMorphRadius((PI / 6).toFloat(), 0f, 1f, 0f)
        assertTrue(lobe - valley > .25f)
        assertEquals(lobe, reverieMorphRadius((PI / 3).toFloat(), 0f, 1f, 0f), .0001f)
        assertTrue((0..8).map { reverieFigurePose(it).contour }.distinct().size == 4)
    }

    private fun radiusBetween(from: Int, to: Int, fraction: Float, angle: Float): Float {
        val a = reverieFigurePose(from).contour
        val b = reverieFigurePose(to).contour
        fun blend(a: Float, b: Float) = a + (b - a) * fraction
        return reverieMorphRadius(angle, blend(a.square, b.square), blend(a.rosette, b.rosette), blend(a.diamond, b.diamond))
    }
}
