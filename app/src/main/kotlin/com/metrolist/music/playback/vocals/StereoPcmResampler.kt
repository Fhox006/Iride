package com.metrolist.music.playback.vocals

/** An integer timebase prevents drift between the prepared audio and timed lyrics. */
internal class StereoPcmResampler(val sampleRate: Int) {
    var inputFrames: Long = 0
        private set
    private var outputFrames = 0L
    private var previousLeft = 0f
    private var previousRight = 0f

    init { require(sampleRate > 0) }

    fun accept(left: Float, right: Float, emit: (Float, Float) -> Unit) {
        val frame = inputFrames++
        if (sampleRate == 44100) {
            emit(left, right)
            outputFrames++
            return
        }
        if (frame == 0L) {
            emit(left, right)
            outputFrames++
        } else {
            while (outputFrames * sampleRate <= frame * 44100) {
                val fraction = (outputFrames * sampleRate - (frame - 1) * 44100) / 44100f
                emit(previousLeft + (left - previousLeft) * fraction, previousRight + (right - previousRight) * fraction)
                outputFrames++
            }
        }
        previousLeft = left
        previousRight = right
    }

    fun finish(emit: (Float, Float) -> Unit) {
        val expected = (inputFrames * 44100 + sampleRate / 2) / sampleRate
        while (outputFrames < expected) { emit(previousLeft, previousRight); outputFrames++ }
    }
}
