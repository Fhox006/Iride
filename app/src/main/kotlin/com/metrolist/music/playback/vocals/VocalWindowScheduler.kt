package com.metrolist.music.playback.vocals

/** Prioritizes audible and upcoming samples, including seeks after reaching the track's end. */
internal class VocalWindowScheduler(private val durationFrames: Long, positionFrame: Long, revision: Long) {
    var nextFrame = positionFrame.coerceAtLeast(0)
        private set
    private var revision = revision

    fun select(positionFrame: Long, seekRevision: Long, processingMs: Long): Long? {
        val position = positionFrame.coerceAtLeast(0)
        if (revision != seekRevision) {
            revision = seekRevision
            nextFrame = position
        } else if (nextFrame + STEP_FRAMES <= position ||
            (processingMs > STEP_FRAMES * 1000L / VocalTimeline.SAMPLE_RATE && nextFrame < position + processingMs * VocalTimeline.SAMPLE_RATE / 1000)) {
            nextFrame = position + processingMs * VocalTimeline.SAMPLE_RATE / 1000
        }
        return nextFrame.takeIf { it < durationFrames && it <= position + VocalTimeline.SAMPLE_RATE * 30 }
    }

    fun advance(startFrame: Long, emittedFrames: Int, ended: Boolean) {
        nextFrame = if (ended) durationFrames else startFrame + emittedFrames
    }

    companion object {
        // Spleeter pads every call to 512 STFT hops. Smaller windows cost the same inference.
        const val STEP_FRAMES = VocalTimeline.SAMPLE_RATE * 10
        const val OVERLAP_FRAMES = VocalTimeline.SAMPLE_RATE
    }
}
