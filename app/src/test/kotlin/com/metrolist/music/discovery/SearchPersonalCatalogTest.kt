package com.metrolist.music.discovery

import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.ArtistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPersonalCatalogTest {
    @Test
    fun followedArtistIsAvailableEvenWithoutAnyRecentSong() = runBlocking {
        val followed = Artist(ArtistEntity("UC_nayt", "Nayt"), songCount = 0)
        val catalog = searchPersonalCatalog(flowOf(emptyList()), flowOf(emptyList()), flowOf(listOf(followed))).first()
        assertEquals(listOf("UC_nayt"), catalog.filterIsInstance<ArtistItem>().map { it.id })
    }
}
