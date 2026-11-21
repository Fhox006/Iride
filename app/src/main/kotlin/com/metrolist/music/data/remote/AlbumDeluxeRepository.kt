package com.metrolist.music.data.remote

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.innertube.utils.morePreciseAlbumReleaseDate
import com.metrolist.music.utils.AlbumDeluxeBlock
import com.metrolist.music.utils.detectAlbumDeluxe
import com.metrolist.music.utils.sharedAlbumEdge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class AlbumDeluxeRepository @Inject constructor() {
    suspend fun find(page: AlbumPage, onMatch: (AlbumDeluxeBlock) -> Unit) {
        var best: AlbumDeluxeBlock? = null
        val visited = mutableSetOf(page.album.id)
        var datedPage: AlbumPage? = null

        suspend fun withDate(value: AlbumPage): AlbumPage {
            if ((value.releaseDate ?: value.album.releaseDate)?.length == 10) return value
            val artist = value.album.artists?.firstOrNull()?.name ?: return value
            val date = morePreciseAlbumReleaseDate(
                value.releaseDate ?: value.album.releaseDate,
                YouTube.appleMusicAlbumReleaseDate(value.album.title, artist, value.album.year),
                value.album.year,
            )
            return value.copy(releaseDate = date ?: value.releaseDate ?: value.album.releaseDate)
        }

        suspend fun inspect(items: List<AlbumItem>) {
            for (item in items) {
                if (!visited.add(item.id)) continue
                val candidateYear = item.year
                val currentYear = page.album.year
                if (candidateYear != null && currentYear != null && candidateYear > currentYear) continue
                try {
                    withTimeoutOrNull(8_000) {
                        val previous = YouTube.album(item.id).getOrNull() ?: return@withTimeoutOrNull
                        if (sharedAlbumEdge(page.songs, previous.songs) == null) return@withTimeoutOrNull
                        val needsDates = page.album.year == null || previous.album.year == null ||
                            page.album.year == previous.album.year
                        val current = if (needsDates) {
                            datedPage ?: withDate(page).also { datedPage = it }
                        } else page
                        val match = detectAlbumDeluxe(current, if (needsDates) withDate(previous) else previous)
                            ?: return@withTimeoutOrNull
                        if (match.songIds.size > (best?.songIds?.size ?: 0)) {
                            best = match
                            onMatch(match)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Missing catalog metadata must not interrupt the album screen.
                }
            }
        }

        withTimeoutOrNull(60_000) {
            inspect(page.otherVersions + page.similarAlbums)
            for (artist in page.album.artists.orEmpty()) {
                val id = artist.id ?: continue
                val artistPage = YouTube.artist(id).getOrNull() ?: continue
                for (section in artistPage.sections.filter { it.items.any { item -> item is AlbumItem } }) {
                    inspect(section.items.filterIsInstance<AlbumItem>())
                    val endpoint = section.moreEndpoint ?: continue
                    val albums = YouTube.artistItems(endpoint).getOrNull() ?: continue
                    inspect(albums.items.filterIsInstance<AlbumItem>())
                    var continuation = albums.continuation
                    val seen = mutableSetOf<String>()
                    while (continuation != null && seen.add(continuation)) {
                        val next = YouTube.artistItemsContinuation(continuation).getOrNull() ?: break
                        inspect(next.items.filterIsInstance<AlbumItem>())
                        continuation = next.continuation
                    }
                }
            }
        }
    }
}
