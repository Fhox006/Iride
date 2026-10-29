package com.metrolist.innertube.pages

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist

fun List<AlbumItem>.sortedByReleaseDate(): List<AlbumItem> = sortedWith(
    compareBy<AlbumItem> { it.releaseDateSortKey() == null }
        .thenByDescending { it.releaseDateSortKey() }
        .thenBy { it.title.lowercase() },
)

private fun AlbumItem.releaseDateSortKey(): String? {
    val date = releaseDate
    val key = when {
        date?.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) == true -> date
        date?.matches(Regex("\\d{4}-\\d{2}")) == true -> "$date-01"
        date?.matches(Regex("\\d{4}")) == true -> "$date-01-01"
        year != null -> "${year.toString().padStart(4, '0')}-01-01"
        else -> null
    }
    return key
}

/** Add credited catalog releases to the artist's own shelves, leaving other shelves intact. */
fun ArtistPage.withCatalogAlbums(
    releases: List<AlbumItem>,
    albumsTitle: String,
    singlesTitle: String,
    knownShortReleases: List<AlbumItem> = emptyList(),
): ArtistPage {
    val shelves = sections.toMutableList()
    fun ArtistSection.isOwnShelf(): Boolean = items.all { it is AlbumItem } &&
        listOf("appears on", "featured on", "compilation", "compare in", "presente in", "presente su", "raccolta")
            .none { title.contains(it, ignoreCase = true) }
    val albumShelf = shelves.indexOfFirst { section ->
        section.title.contains("album", ignoreCase = true) && section.isOwnShelf()
    }
    val singlesShelf = shelves.indexOfFirst { section ->
        (section.title.contains("singl", ignoreCase = true) || section.title.contains("singol", ignoreCase = true) ||
            section.title.contains("EP")) && section.isOwnShelf()
    }
    val ownShelfIndices = setOf(albumShelf, singlesShelf).filter { it >= 0 }
    val native = ownShelfIndices.flatMap { shelves[it].items.filterIsInstance<AlbumItem>() }
        .map { release ->
            // YouTube's own artist shelves often omit credits on every album card.
            if (release.artists.isNullOrEmpty()) release.copy(artists = listOf(Artist(artist.title, artist.id))) else release
        }
    val visibleShortReleases = if (singlesShelf >= 0) {
        shelves[singlesShelf].items.filterIsInstance<AlbumItem>().map { release ->
            if (release.artists.isNullOrEmpty()) release.copy(artists = listOf(Artist(artist.title, artist.id))) else release
        }
    } else emptyList()
    val nativeShortReleases = (visibleShortReleases + knownShortReleases).distinctBy(AlbumItem::id).map { release ->
        if (release.artists.isNullOrEmpty()) release.copy(artists = listOf(Artist(artist.title, artist.id))) else release
    }
    val nativeIds = native.map(AlbumItem::id).toSet()
    val mergedOwn = YouTube.mergeArtistAlbums(native, nativeShortReleases, releases)
    val enrichedNative = mergedOwn.filter { it.id in nativeIds }.associateBy(AlbumItem::id)
    ownShelfIndices.forEach { index ->
        val section = shelves[index]
        shelves[index] = section.copy(items = section.items.map { item ->
            if (item is AlbumItem) enrichedNative[item.id] ?: item else item
        })
    }
    val additions = mergedOwn.filter { it.id !in nativeIds }
    fun addToShelf(index: Int, title: String, items: List<AlbumItem>) {
        if (items.isEmpty()) return
        if (index >= 0) {
            val section = shelves[index]
            shelves[index] = section.copy(items = YouTube.mergeArtistAlbums(
                section.items.filterIsInstance<AlbumItem>(), nativeShortReleases, items,
            )
                .sortedByReleaseDate())
        } else {
            shelves += ArtistSection(title, items.sortedByReleaseDate(), null)
        }
    }
    val (shortReleases, albums) = additions.partition { it.albumType == "Single" || it.albumType == "EP" }
    addToShelf(albumShelf, albumsTitle, albums)
    addToShelf(singlesShelf, singlesTitle, shortReleases)
    ownShelfIndices.forEach { index ->
        val section = shelves[index]
        shelves[index] = section.copy(items = section.items.filterIsInstance<AlbumItem>().sortedByReleaseDate())
    }
    return copy(sections = shelves)
}
