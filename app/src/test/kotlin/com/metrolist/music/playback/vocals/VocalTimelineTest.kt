package com.metrolist.music.playback.vocals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VocalTimelineTest {
    private fun chunk(start: Long) = VocalTimeline.Chunk(start, Array(2) { FloatArray(44100 * 2) { 0.25f } }, Array(2) { FloatArray(44100 * 2) { 0.5f } })

    @Test fun `a small ready portion is playable before the read ahead buffer is complete`() {
        val timeline = VocalTimeline()
        timeline.publish(chunk(0), 0)
        assertTrue(timeline.covers(44100))
        assertFalse(timeline.hasReadyAhead(44100, 180 * 44100L, 10 * 44100L))
    }

    @Test fun `background throttling requires continuous prepared audio ahead of playback`() {
        val rate = VocalTimeline.SAMPLE_RATE.toLong()
        val timeline = VocalTimeline()
        timeline.publish(chunk(0), 0)
        timeline.publish(chunk(4 * rate), 0)
        assertFalse(timeline.hasReadyAhead(0, 180 * rate, 6 * rate))
        timeline.publish(chunk(2 * rate), 0)
        assertTrue(timeline.hasReadyAhead(0, 180 * rate, 6 * rate))
        assertFalse(timeline.hasReadyAhead(rate, 180 * rate, 6 * rate))
        assertTrue(timeline.hasReadyAhead(5 * rate, 6 * rate, 10 * rate))
    }

    @Test fun `publication preserves an audio thread snapshot and bounds retained windows`() {
        val timeline = VocalTimeline()
        timeline.publish(chunk(0), 0)
        val snapshot = timeline.chunks
        repeat(100) { timeline.publish(chunk((it + 1) * 88200L), 0) }
        assertEquals(1, snapshot.size)
        assertEquals(0L, snapshot.first().startFrame)
        assertTrue(timeline.chunks.size <= VocalTimeline.MAX_CHUNKS)
        assertTrue(timeline.chunks.any { it.startFrame == 100 * 88200L })
    }

    @Test fun `coverage ends at gaps and includes adjoining prepared windows`() {
        val timeline = VocalTimeline()
        timeline.publish(chunk(0), 0)
        assertEquals(0.25f, VocalTimeline.sample(timeline.chunks, 0.0, 0), 0f)
        assertEquals(0f, VocalTimeline.sample(timeline.chunks, 88200.0, 0), 0f)
        assertEquals(88200L, VocalTimeline.coverageEnd(timeline.chunks, 0))
        timeline.publish(chunk(88200), 0)
        assertEquals(176400L, VocalTimeline.coverageEnd(timeline.chunks, 0))
        assertEquals(0.25f, VocalTimeline.sample(timeline.chunks, 88190.0, 0), 0f)
        assertEquals(0.25f, VocalTimeline.sample(timeline.chunks, 88200.0, 0), 0f)
        assertEquals(0f, VocalTimeline.sample(timeline.chunks, 44100.0 * 60, 0), 0f)
    }

    @Test fun `readiness follows the playhead rather than any completed window`() {
        val timeline = VocalTimeline()
        val start = 90 * 44100L
        timeline.publish(chunk(start), start)
        assertFalse(timeline.covers(0))
        assertTrue(timeline.covers(start))
        assertTrue(timeline.covers(start + 44100))
        assertFalse(timeline.covers(start + 88200))
        assertFalse(timeline.covers(120 * 44100L))
    }
}
