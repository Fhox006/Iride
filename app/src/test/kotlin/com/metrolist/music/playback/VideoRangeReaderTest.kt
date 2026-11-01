package com.metrolist.music.playback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException

class VideoRangeReaderTest {
    private class FragmentedServer(val data: ByteArray, val fragmentSize: Int, val throwsEof: Boolean = false) {
        val requests = mutableListOf<Pair<Long, Long>>()
        var closes = 0
        var position = 0
        var end = 0

        fun reader(start: Long = 0, length: Long = data.size.toLong() - start, chunk: Long = 32) =
            VideoRangeReader(start, length,
                openRange = { position, length ->
                    requests += position to length
                    this.position = position.toInt()
                    end = minOf(data.size, this.position + minOf(length, fragmentSize.toLong()).toInt())
                },
                readRange = { buffer, offset, length ->
                    if (position == end) {
                        if (throwsEof) throw EOFException()
                        -1
                    } else {
                        val count = minOf(length, end - position)
                        data.copyInto(buffer, offset, position, position + count)
                        position += count
                        count
                    }
                }, closeRange = { closes++ }, chunkSize = chunk)
    }

    private fun readAll(reader: VideoRangeReader): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(7)
        reader.open()
        try {
            while (true) {
                val count = reader.read(buffer, 0, buffer.size)
                if (count == -1) break
                output.write(buffer, 0, count)
            }
        } finally { reader.close() }
        return output.toByteArray()
    }

    @Test fun continuesAfterServerReturnsOnlyAnInitialFragment() {
        val data = ByteArray(115) { it.toByte() }
        val server = FragmentedServer(data, 13)
        assertArrayEquals(data, readAll(server.reader()))
        assertEquals(listOf(0L, 13L, 26L, 39L, 52L, 65L, 78L, 91L, 104L), server.requests.map { it.first })
        assertEquals(server.requests.size, server.closes)
    }

    @Test fun chunksNeverSkipOrDuplicateBytes() {
        val data = ByteArray(100) { (it * 3).toByte() }
        val server = FragmentedServer(data, 100)
        assertArrayEquals(data, readAll(server.reader(chunk = 16)))
        assertEquals(listOf(0L, 16L, 32L, 48L, 64L, 80L, 96L), server.requests.map { it.first })
        assertEquals(4L, server.requests.last().second)
    }

    @Test fun seekStartsAtRequestedByteAndRespectsRequestedLength() {
        val data = ByteArray(100) { it.toByte() }
        val server = FragmentedServer(data, 9)
        assertArrayEquals(data.copyOfRange(75, 89), readAll(server.reader(start = 75, length = 14)))
        assertEquals(listOf(75L to 14L, 84L to 5L), server.requests)
    }

    @Test fun recoversUnexpectedEofAfterPartialRange() {
        val data = ByteArray(71) { it.toByte() }
        assertArrayEquals(data, readAll(FragmentedServer(data, 11, throwsEof = true).reader()))
    }

    @Test fun emptyKnownRangeFailsInsteadOfLooping() {
        val server = FragmentedServer(byteArrayOf(), 10)
        try {
            readAll(server.reader(length = 100))
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            assertEquals(1, server.requests.size)
            assertEquals(1, server.closes)
        }
    }

    @Test fun unknownLengthContinuesFragmentsAndStopsAtEmptyRange() {
        val data = ByteArray(65) { it.toByte() }
        assertArrayEquals(data, readAll(FragmentedServer(data, 10).reader(length = -1)))
    }

    @Test fun zeroLengthDoesNotOpenNetworkConnection() {
        val server = FragmentedServer(byteArrayOf(), 10)
        val reader = server.reader()
        reader.open()
        assertEquals(0, reader.read(ByteArray(1), 0, 0))
        assertEquals(-1, reader.read(ByteArray(1), 0, 1))
        reader.close()
        assertTrue(server.requests.isEmpty())
    }
}
