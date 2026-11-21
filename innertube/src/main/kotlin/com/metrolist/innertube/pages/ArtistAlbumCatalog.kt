package com.metrolist.innertube.pages

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.utils.albumReleaseDate
import java.time.LocalDate
import java.util.Locale

fun List<AlbumItem>.sortedByReleaseDate(): List<AlbumItem> = sortedWith(
    compareBy<AlbumItem> { it.releaseDateSortKey() == null }
        .thenByDescending { it.releaseDateSortKey() }
        .thenBy { it.title.lowercase(Locale.ROOT) }
        .thenBy { it.id },
)

private fun AlbumItem.releaseDateSortKey() = albumReleaseDate(releaseDate, year)

/** Select the latest release from YouTube's artist shelves without treating catalog-only matches as confirmed. */
fun ArtistPage.latestNativeRelease(): Pair<ArtistSection, AlbumItem>? = sections.asSequence()
    .filter { section ->
        val title = section.title
        val isReleaseShelf = listOf("album", "singl", "singol", "EP", "latest", "uscita", "release")
            .any { title.contains(it, ignoreCase = true) }
        val isOtherArtistsShelf = listOf(
            "appears on", "featured on", "compilation", "compare in", "presente in", "presente su", "raccolta",
        ).any { title.contains(it, ignoreCase = true) }
        isReleaseShelf && !isOtherArtistsShelf
    }
    .flatMap { section ->
        section.items.asSequence()
            .filterIsInstance<AlbumItem>()
            .filterNot { it.id.startsWith("itunes:") || it.browseId.startsWith("itunes:") }
            .map { section to it }
    }
    .maxByOrNull { (_, album) -> albumReleaseDate(album.releaseDate, album.year) ?: LocalDate.MIN }

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
