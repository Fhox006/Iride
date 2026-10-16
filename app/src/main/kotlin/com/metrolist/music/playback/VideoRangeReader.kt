package com.metrolist.music.playback

import java.io.EOFException
import java.io.IOException

/** Presents successive HTTP byte ranges as one seekable progressive stream. */
internal class VideoRangeReader(
    initialPosition: Long,
    length: Long,
    private val openRange: (position: Long, length: Long) -> Unit,
    private val readRange: (ByteArray, Int, Int) -> Int,
    private val closeRange: () -> Unit,
    private val chunkSize: Long = 1024L * 1024L,
) {
    var remaining = length
        private set
    var position = initialPosition
        private set
    private var chunkRemaining = 0L
    private var chunkStart = initialPosition
    private var opened = false

    fun open() {
        if (remaining != 0L) nextRange()
    }

    private fun nextRange() {
        if (opened) closeRange()
        opened = false
        chunkStart = position
        chunkRemaining = if (remaining >= 0) minOf(chunkSize, remaining) else chunkSize
        opened = true
        openRange(position, chunkRemaining)
    }

    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return -1
        while (true) {
            if (chunkRemaining == 0L) nextRange()
            val count = try {
                readRange(buffer, offset, minOf(length.toLong(), chunkRemaining).toInt())
            } catch (_: EOFException) { -1 }
            if (count == -1) {
                if (position == chunkStart) {
                    if (remaining < 0) return -1
                    throw IOException("Empty YouTube video range at $position")
                }
                nextRange()
                continue
            }
            position += count
            chunkRemaining -= count
            if (remaining > 0) remaining -= count
            return count
        }
    }

    fun close() {
        if (opened) {
            opened = false
            closeRange()
        }
    }
}
