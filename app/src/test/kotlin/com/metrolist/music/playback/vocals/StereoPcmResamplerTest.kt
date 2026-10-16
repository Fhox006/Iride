package com.metrolist.music.playback.vocals

import org.junit.Assert.assertEquals
import org.junit.Test

class StereoPcmResamplerTest {
    @Test
    fun `44 point 1 kHz stereo is unchanged`() {
        val resampler = StereoPcmResampler(44100)
        val left = mutableListOf<Float>()
        val right = mutableListOf<Float>()
        val emit: (Float, Float) -> Unit = { l, r -> left.add(l); right.add(r) }
        repeat(1000) { resampler.accept(it / 1000f, -it / 1000f, emit) }
        resampler.finish(emit)
        assertEquals(1000, left.size)
        repeat(1000) {
            assertEquals(it / 1000f, left[it], 0f)
            assertEquals(-it / 1000f, right[it], 0f)
        }
    }

    @Test
    fun `resampling maintains duration across ten minutes`() {
        for (rate in listOf(22050, 48000)) {
            val resampler = StereoPcmResampler(rate)
            var frames = 0L
            val emit: (Float, Float) -> Unit = { _, _ -> frames++ }
            repeat(rate * 600) { resampler.accept(0f, 0f, emit) }
            resampler.finish(emit)
            assertEquals(44100L * 600, frames)
        }
    }

    @Test
    fun `empty input produces no audio`() {
        var frames = 0
        StereoPcmResampler(48000).finish { _, _ -> frames++ }
        assertEquals(0, frames)
    }
}
