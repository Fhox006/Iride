package com.metrolist.music.playback.vocals

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import java.io.File

/** Prepared PCM goes straight to disk rather than duplicating it in the streaming cache. */
@UnstableApi
internal class VocalDataSource(
    private val originalFactory: DataSource.Factory,
    private val prepared: (String) -> File?,
) : DataSource {
    private val listeners = mutableListOf<TransferListener>()
    private var source: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners.add(transferListener)
        source?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val file = if (dataSpec.uri.scheme == "iride-vocals") dataSpec.key?.let(prepared) else null
        val delegate = if (file != null) FileDataSource() else originalFactory.createDataSource()
        source = delegate
        listeners.forEach(delegate::addTransferListener)
        return delegate.open(if (file != null) dataSpec.withUri(Uri.fromFile(file)) else dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = checkNotNull(source).read(buffer, offset, length)
    override fun getUri(): Uri? = source?.uri
    override fun getResponseHeaders(): Map<String, List<String>> = source?.responseHeaders ?: emptyMap()
    override fun close() {
        try { source?.close() } finally { source = null }
    }
}
