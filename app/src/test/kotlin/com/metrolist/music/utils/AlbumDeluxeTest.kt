package com.metrolist.music.utils

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.AlbumPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumDeluxeTest {
    private fun song(id: String) = SongItem(
        id = id, title = id, artists = listOf(Artist("Artist", "artist")),
        duration = 180, thumbnail = "",
    )

    private val original = listOf("a", "b", "c").map(::song)
    private val bonus = listOf("x", "y").map(::song)

    private fun page(id: String, year: Int, songs: List<SongItem>, date: String? = null) = AlbumPage(
        AlbumItem(id, "playlist", title = id, artists = listOf(Artist("Artist", "artist")),
            year = year, thumbnail = ""), songs, emptyList(), releaseDate = date,
    )

    @Test fun `shared prefix keeps original track order`() {
        val current = original.reversed() + bonus
        assertEquals(listOf("c", "b", "a"), sharedAlbumEdge(current, original))
    }

    @Test fun `shared suffix is accepted`() {
        assertEquals(listOf("a", "b", "c"), sharedAlbumEdge(bonus + original, original))
    }

    @Test fun `scattered shared tracks are rejected even with a three track prefix`() {
        val previous = original + song("d")
        assertNull(sharedAlbumEdge(original + bonus + song("d"), previous))
    }

    @Test fun `interior blocks and fewer than three tracks are rejected`() {
        assertNull(sharedAlbumEdge(listOf(bonus[0]) + original + bonus[1], original))
        assertNull(sharedAlbumEdge(original.take(2) + bonus, original))
    }

    @Test fun `different video ids can identify same recording`() {
        val reissued = original.map { it.copy(id = "new-${it.id}", title = " ${it.title.uppercase()} ", duration = 181) }
        assertEquals(reissued.map { it.id }, sharedAlbumEdge(reissued + bonus, original))
        assertNull(sharedAlbumEdge(reissued.map { it.copy(artists = listOf(Artist("Other", "other"))) } + bonus, original))
        assertNull(sharedAlbumEdge(reissued.map { it.copy(duration = 240) } + bonus, original))
    }

    @Test fun `three copies of one track do not qualify`() {
        assertNull(sharedAlbumEdge(List(3) { song("a") } + bonus, original))
    }

    @Test fun `source must be an earlier different album`() {
        val current = page("deluxe", 2025, original + bonus)
        assertNotNull(detectAlbumDeluxe(current, page("original", 2024, original)))
        assertNull(detectAlbumDeluxe(current, page("future", 2026, original)))
        assertNull(detectAlbumDeluxe(current, page("deluxe", 2024, original)))
        assertNull(detectAlbumDeluxe(current, page("unknown", 2025, original)))
    }

    @Test fun `same year requires precise earlier release dates`() {
        val current = page("deluxe", 2025, original + bonus, "2025-06-01")
        assertNotNull(detectAlbumDeluxe(current, page("original", 2025, original, "2025-02-01")))
        assertNull(detectAlbumDeluxe(current, page("original", 2025, original)))
        assertNull(detectAlbumDeluxe(current, page("original", 2025, original, "2025-06-01")))
    }
}
