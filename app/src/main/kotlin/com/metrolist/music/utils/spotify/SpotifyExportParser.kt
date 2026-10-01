package com.metrolist.music.utils.spotify

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackReader
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream

data class SpotifyExportTrack(
    val title: String,
    val artists: List<String>,
    val durationSeconds: Int?,
    val spotifyUri: String,
)

data class SpotifyExportPlaylist(
    val name: String,
    val tracks: List<SpotifyExportTrack>,
    val fingerprint: String,
    val invalid: Boolean = false,
)

data class SpotifyExportResult(
    val playlists: List<SpotifyExportPlaylist>,
)

class SpotifyExportTooLargeException : IllegalArgumentException()

/** Reads exports in memory without extracting ZIP paths onto the filesystem. */
object SpotifyExportParser {
    const val MAX_BYTES = 128L * 1024 * 1024
    private const val MAX_ENTRIES = 2048
    private const val MAX_TRACKS = 100_000
    private const val MAX_FIELD_LENGTH = 1_048_576

    fun parse(input: InputStream, fileName: String): SpotifyExportResult {
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val signature = IntArray(4) { buffered.read() }
        buffered.reset()
        val isZip = signature[0] == 0x50 && signature[1] == 0x4b &&
            ((signature[2] == 3 && signature[3] == 4) || (signature[2] == 5 && signature[3] == 6))
        if (!isZip) {
            require(fileName.endsWith(".csv", true))
            return SpotifyExportResult(listOf(parseCsv(BoundedInputStream(buffered), fileName)))
        }
        val playlists = mutableListOf<SpotifyExportPlaylist>()
        var entries = 0
        // One budget spans every entry, including CSV entries rejected as malformed.
        val budget = ReadBudget()
        ZipInputStream(buffered).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > MAX_ENTRIES) throw SpotifyExportTooLargeException()
                if (!entry.isDirectory && entry.name.endsWith(".csv", true)) {
                    if (entry.size > MAX_BYTES) throw SpotifyExportTooLargeException()
                    try {
                        playlists += parseCsv(BoundedInputStream(zip, budget), entry.name)
                    } catch (error: SpotifyExportTooLargeException) {
                        throw error
                    } catch (_: IllegalArgumentException) {
                        playlists += SpotifyExportPlaylist(
                            name = playlistName(entry.name),
                            tracks = emptyList(),
                            fingerprint = fingerprint(entry.name, emptyList()),
                            invalid = true,
                        )
                    }
                }
                // Drain through the same budget; closeEntry alone would bypass size limits.
                val remaining = BoundedInputStream(zip, budget)
                val buffer = ByteArray(8192)
                while (remaining.read(buffer) != -1) Unit
                zip.closeEntry()
            }
        }
        require(playlists.isNotEmpty())
        return SpotifyExportResult(playlists)
    }

    private fun parseCsv(input: InputStream, fileName: String): SpotifyExportPlaylist {
        val rows = CsvRows(PushbackReader(InputStreamReader(input, Charsets.UTF_8), 1))
        val header = rows.next()?.map { it.trim().removePrefix("\uFEFF").lowercase(Locale.ROOT) }
            ?: throw IllegalArgumentException()
        val titleIndex = header.indexOfFirst { it in SpotifyExportColumns.titles }
        val artistIndex = header.indexOfFirst { it in SpotifyExportColumns.artists }
        require(titleIndex >= 0 && artistIndex >= 0)
        val uriIndex = header.indexOfFirst { it in SpotifyExportColumns.uris }
        val durationIndex = header.indexOfFirst { it in SpotifyExportColumns.durations }
        val legacyArtists = header.getOrNull(durationIndex) == "duration (ms)"
        val tracks = mutableListOf<SpotifyExportTrack>()
        while (true) {
            val row = rows.next() ?: break
            if (row.all(String::isBlank)) continue
            if (tracks.size >= MAX_TRACKS) throw SpotifyExportTooLargeException()
            tracks += SpotifyExportTrack(
                title = row.getOrNull(titleIndex).orEmpty().trim(),
                artists = parseArtists(row.getOrNull(artistIndex).orEmpty(), legacyArtists),
                durationSeconds = row.getOrNull(durationIndex)?.trim()?.toLongOrNull()
                    ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.let { (it / 1000).toInt() },
                spotifyUri = row.getOrNull(uriIndex).orEmpty().trim(),
            )
        }
        val name = playlistName(fileName)
        return SpotifyExportPlaylist(name, tracks, fingerprint(name, tracks))
    }

    private fun playlistName(fileName: String) = fileName.substringAfterLast('/').substringAfterLast('\\')
        .substringBeforeLast('.').replace('_', ' ').trim().ifEmpty { "Spotify" }

    private fun parseArtists(value: String, legacy: Boolean): List<String> {
        val names = if (legacy || ';' in value) value.split(';') else value.split(UNESCAPED_COMMA)
        return names.map { it.replace("\\,", ",").trim() }.filter(String::isNotEmpty)
    }

    private val UNESCAPED_COMMA = Regex("(?<!\\\\),\\s*")

    private fun fingerprint(name: String, tracks: List<SpotifyExportTrack>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun add(value: String) {
            digest.update(value.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
        }
        add(name)
        tracks.forEach { track ->
            add(track.spotifyUri)
            add(track.title)
            track.artists.forEach(::add)
            add(track.durationSeconds.toString())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class ReadBudget(var count: Long = 0)

    private class BoundedInputStream(
        private val delegate: InputStream,
        private val budget: ReadBudget = ReadBudget(),
    ) : InputStream() {
        override fun read(): Int = delegate.read().also { if (it != -1) record(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length).also { if (it > 0) record(it) }

        private fun record(count: Int) {
            budget.count += count
            if (budget.count > MAX_BYTES) throw SpotifyExportTooLargeException()
        }
    }

    /** Quoting is tracked across physical lines; doubled quotes remain inside one field. */
    private class CsvRows(private val reader: PushbackReader) {
        fun next(): List<String>? {
            val fields = mutableListOf<String>()
            val field = StringBuilder()
            var quoted = false
            var closedQuote = false
            var seenCharacter = false
            while (true) {
                val code = reader.read()
                if (code == -1) {
                    require(!quoted)
                    if (!seenCharacter && fields.isEmpty()) return null
                    fields += field.toString()
                    return fields
                }
                seenCharacter = true
                val character = code.toChar()
                when {
                    quoted && character == '"' -> {
                        val next = reader.read()
                        if (next == '"'.code) field.append('"') else {
                            quoted = false
                            closedQuote = true
                            if (next != -1) reader.unread(next)
                        }
                    }
                    quoted -> field.append(character)
                    character == '"' -> {
                        require(field.isEmpty() && !closedQuote)
                        quoted = true
                    }
                    character == ',' -> {
                        fields += field.toString()
                        field.setLength(0)
                        closedQuote = false
                    }
                    character == '\n' || character == '\r' -> {
                        if (character == '\r') {
                            val next = reader.read()
                            if (next != '\n'.code && next != -1) reader.unread(next)
                        }
                        fields += field.toString()
                        return fields
                    }
                    closedQuote -> require(character.isWhitespace())
                    character == '\uFEFF' && fields.isEmpty() && field.isEmpty() -> Unit
                    else -> field.append(character)
                }
                if (field.length > MAX_FIELD_LENGTH || fields.size > 256) throw SpotifyExportTooLargeException()
            }
        }
    }
}
