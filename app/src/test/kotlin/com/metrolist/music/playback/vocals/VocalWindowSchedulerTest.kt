package com.metrolist.music.playback.vocals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VocalWindowSchedulerTest {
    private val rate = VocalTimeline.SAMPLE_RATE.toLong()

    @Test fun `activation at halfway starts there without processing the song intro`() {
        val scheduler = VocalWindowScheduler(180 * rate, 90 * rate, 0)
        assertEquals(90 * rate, scheduler.select(90 * rate, 0, 0))
        scheduler.advance(90 * rate, VocalWindowScheduler.STEP_FRAMES, false)
        assertEquals(100 * rate, scheduler.select(90 * rate, 0, 100))
    }

    @Test fun `seeks forward and backward immediately supersede the next window`() {
        val scheduler = VocalWindowScheduler(180 * rate, 0, 0)
        scheduler.advance(0, VocalWindowScheduler.STEP_FRAMES, false)
        assertEquals(90 * rate, scheduler.select(90 * rate, 1, 500))
        scheduler.advance(90 * rate, VocalWindowScheduler.STEP_FRAMES, false)
        assertEquals(10 * rate, scheduler.select(10 * rate, 2, 500))
    }

    @Test fun `slow inference targets upcoming audio instead of repeating expired windows`() {
        val scheduler = VocalWindowScheduler(180 * rate, 0, 0)
        scheduler.advance(0, VocalWindowScheduler.STEP_FRAMES, false)
        assertEquals(24 * rate, scheduler.select(12 * rate, 0, 12000))
    }

    @Test fun `read ahead is bounded and seeks still work after reaching the end`() {
        val scheduler = VocalWindowScheduler(180 * rate, 32 * rate, 0)
        assertNull(scheduler.select(0, 0, 100))
        scheduler.advance(170 * rate, VocalWindowScheduler.STEP_FRAMES, true)
        assertNull(scheduler.select(179 * rate, 0, 100))
        assertEquals(10 * rate, scheduler.select(10 * rate, 1, 100))
    }
}
