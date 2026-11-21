package com.metrolist.music.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource

/** Keeps a progressive YouTube stream continuous across bounded HTTP range requests. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class VideoRangeDataSource(
    private val upstream: HttpDataSource,
    private val resolve: (DataSpec) -> Stream?,
) : BaseDataSource(true) {
    data class Stream(val url: String, val length: Long?, val userAgent: String? = null)

    private lateinit var original: DataSpec
    private lateinit var stream: Stream
    private var reader: VideoRangeReader? = null
    private var opened = false
    private var upstreamOpen = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val resolved = resolve(dataSpec)
        if (resolved == null) {
            upstreamOpen = true
            val length = upstream.open(dataSpec)
            opened = true
            transferStarted(dataSpec)
            return length
        }
        original = dataSpec
        stream = resolved
        val available = stream.length?.let { (it - dataSpec.position).coerceAtLeast(0) } ?: C.LENGTH_UNSET.toLong()
        val remaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() && available >= 0 -> minOf(dataSpec.length, available)
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            else -> available
        }
        opened = true
        transferStarted(dataSpec)
        reader = VideoRangeReader(dataSpec.position, remaining,
            openRange = { position, size ->
                upstreamOpen = true
                upstream.open(original.buildUpon().setUri(stream.url).setPosition(position).setLength(size)
                    .setHttpRequestHeaders(stream.userAgent?.let { original.httpRequestHeaders + ("User-Agent" to it) }
                        ?: original.httpRequestHeaders).build())
            },
            readRange = upstream::read,
            closeRange = { upstreamOpen = false; upstream.close() },
        )
        reader?.open()
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = reader?.read(buffer, offset, length) ?: upstream.read(buffer, offset, length)
        if (count > 0) bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri? = if (upstreamOpen) upstream.uri else null
    override fun getResponseHeaders(): Map<String, List<String>> = if (upstreamOpen) upstream.responseHeaders else emptyMap()

    override fun close() {
        try {
            val activeReader = reader
            if (activeReader != null) activeReader.close()
            else if (upstreamOpen) upstream.close()
        } finally {
            upstreamOpen = false
            reader = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }
}
