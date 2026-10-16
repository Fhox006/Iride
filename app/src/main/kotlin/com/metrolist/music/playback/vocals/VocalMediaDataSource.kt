package com.metrolist.music.playback.vocals

import android.media.MediaDataSource
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import kotlinx.coroutines.Job

/** Random access over the playback cache, without downloading the complete song first. */
@UnstableApi
internal class VocalMediaDataSource(
    private val source: DataSource,
    private val spec: DataSpec,
    private val job: Job,
) : MediaDataSource() {
    private var position = -1L
    private var length = C.LENGTH_UNSET.toLong()
    private var opened = false
    private var closed = false

    init {
        try {
            length = source.open(spec)
            position = 0
            opened = true
        } catch (error: Exception) {
            source.close()
            throw error
        }
    }

    @Synchronized override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (closed || !job.isActive) throw IOException("Karaoke source closed")
        if (size == 0) return 0
        if (length >= 0 && position >= length) return -1
        if (!opened || position != this.position) {
            if (opened) { opened = false; source.close() }
            val remaining = source.open(spec.subrange(position))
            opened = true
            this.position = position
            if (remaining >= 0) length = position + remaining
        }
        val read = source.read(buffer, offset, minOf(size, 65536))
        if (read > 0) this.position += read
        return read
    }

    @Synchronized override fun getSize(): Long = length

    @Synchronized override fun close() {
        closed = true
        if (opened) { opened = false; source.close() }
    }
}
