package com.metrolist.music.playback.vocals

/** Immutable windows published by the worker; the audio thread never waits for inference or I/O. */
internal class VocalTimeline {
    data class Chunk(
        val startFrame: Long,
        val channels: Array<FloatArray>,
        val original: Array<FloatArray>,
    ) {
        val endFrame: Long get() = startFrame + channels[0].size
        init {
            require(channels.size == 2 && channels[0].isNotEmpty() && channels[0].size == channels[1].size)
            require(original.size == 2 && original.all { it.size == channels[0].size })
        }
    }

    @Volatile var chunks: List<Chunk> = emptyList()
        private set

    fun publish(chunk: Chunk, positionFrame: Long) {
        chunks = (chunks.filter { it.endFrame > positionFrame - SAMPLE_RATE * 5 &&
            it.startFrame < positionFrame + SAMPLE_RATE * 40 &&
            (it.endFrame <= chunk.startFrame || it.startFrame >= chunk.endFrame) }
            .sortedBy { kotlin.math.abs(it.startFrame - positionFrame) }.take(MAX_CHUNKS - 1) + chunk)
            .sortedBy { it.startFrame }
    }

    fun clear() { chunks = emptyList() }

    fun covers(positionFrame: Long): Boolean = chunks.any { positionFrame >= it.startFrame && positionFrame < it.endFrame }

    fun hasReadyAhead(positionFrame: Long, durationFrames: Long, aheadFrames: Long): Boolean {
        val snapshot = chunks
        val index = snapshot.indexOfFirst { positionFrame >= it.startFrame && positionFrame < it.endFrame }
        if (index < 0) return false
        val target = (positionFrame + aheadFrames).let { if (durationFrames > 0) minOf(it, durationFrames) else it }
        return coverageEnd(snapshot, index) >= target
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val FADE_FRAMES = SAMPLE_RATE / 50
        const val MAX_CHUNKS = 6

        fun sample(chunks: List<Chunk>, frame: Double, channel: Int): Float {
            return sample(chunks, chunks.indexOfFirst { frame < it.endFrame }, frame, channel)
        }

        fun sample(chunks: List<Chunk>, chunkIndex: Int, frame: Double, channel: Int): Float {
            val chunk = chunks.getOrNull(chunkIndex) ?: return 0f
            if (frame < chunk.startFrame || frame >= chunk.endFrame) return 0f
            return sample(chunk.channels[channel], frame - chunk.startFrame)
        }

        fun sample(samples: FloatArray, frame: Double): Float {
            val index = frame.toInt()
            val next = samples[minOf(index + 1, samples.lastIndex)]
            return samples[index] + (next - samples[index]) * (frame - index).toFloat()
        }

        fun coverageEnd(chunks: List<Chunk>, chunkIndex: Int): Long {
            var end = chunks.getOrNull(chunkIndex)?.endFrame ?: return -1
            var next = chunkIndex + 1
            while (next < chunks.size && chunks[next].startFrame == end) end = chunks[next++].endFrame
            return end
        }
    }
}
