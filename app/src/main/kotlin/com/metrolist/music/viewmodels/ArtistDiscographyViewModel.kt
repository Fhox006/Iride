/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiscographyCategory { FROM_ARTIST, APPEARS_ON }

data class DiscographyBucket(
    val category: DiscographyCategory,
    val releases: List<AlbumItem>,
)

// Per-item albumType only survives on the shelf's own first page (ArtistPage's parser reads it
// off the subtitle) — the "load more" continuation parser (ArtistItemsPage) never sets it, so any
// artist with more than a handful of releases would have every expanded item silently default to
// ALBUM. The shelf title itself is the fallback signal for those, same coarse Album vs Single/EP
// split ArtistScreen already keys its own rendering off of.
fun AlbumItem.releaseType(shelfTitle: String): AlbumReleaseType = when {
    albumType?.contains("EP", ignoreCase = true) == true -> AlbumReleaseType.EP
    albumType?.contains("Single", ignoreCase = true) == true -> AlbumReleaseType.SINGLE
    albumType?.contains("Album", ignoreCase = true) == true -> AlbumReleaseType.ALBUM
    shelfTitle.contains("EP", ignoreCase = true) -> AlbumReleaseType.EP
    shelfTitle.contains("Single", ignoreCase = true) -> AlbumReleaseType.SINGLE
    else -> AlbumReleaseType.ALBUM
}

private val APPEARS_ON_TITLE_HINTS = listOf(
    "appears on", "featured on", "compilation",
    "compare in", "presente in", "presente su", "raccolta",
)

@HiltViewModel
class ArtistDiscographyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!

    private val _artistName = MutableStateFlow<String?>(null)
    val artistName = _artistName.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _buckets = MutableStateFlow<List<DiscographyBucket>>(emptyList())
    val buckets = _buckets.asStateFlow()

    // Type of every release, resolved once up front (needs the source shelf's title, which the
    // AlbumItem itself doesn't carry) — screen reads this instead of recomputing from a bare
    // AlbumItem, which is all it's handed once the release lands in a bucket.
    private val _releaseTypes = MutableStateFlow<Map<String, AlbumReleaseType>>(emptyMap())
    val releaseTypes = _releaseTypes.asStateFlow()

    init {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess { page ->
                    _artistName.value = page.artist.title

                    // Each Albums/Singles/EPs shelf only ever carries a handful of items on the
                    // artist page itself (same reason ArtistViewModel expands Top Songs via its own
                    // "more" endpoint) — done here for every album-shaped shelf so the full picture
                    // is gathered before splitting it into categories below.
                    val albumSections = page.sections.filter { section -> section.items.any { it is AlbumItem } }
                    val perSection = coroutineScope {
                        albumSections
                            .map { section ->
                                async {
                                    val moreEndpoint = section.moreEndpoint
                                    val items = if (moreEndpoint != null) {
                                        YouTube.artistItems(moreEndpoint).getOrNull()?.items
                                            ?.filterIsInstance<AlbumItem>()
                                            ?: section.items.filterIsInstance<AlbumItem>()
                                    } else {
                                        section.items.filterIsInstance<AlbumItem>()
                                    }
                                    section.title to items
                                }
                            }
                            .map { it.await() }
                    }

                    // First shelf that surfaces a given release wins both its category and its
                    // type — later duplicates of the same id (e.g. a single also echoed in an
                    // "Essential"-style shelf) are dropped rather than overwriting it.
                    val seenIds = HashSet<String>()
                    val own = mutableListOf<AlbumItem>()
                    val appearsOn = mutableListOf<AlbumItem>()
                    val types = HashMap<String, AlbumReleaseType>()

                    perSection.forEach { (title, items) ->
                        val isAppearsOnShelf = APPEARS_ON_TITLE_HINTS.any { title.contains(it, ignoreCase = true) }
                        items.forEach { release ->
                            if (seenIds.add(release.id)) {
                                types[release.id] = release.releaseType(title)
                                if (isAppearsOnShelf) appearsOn.add(release) else own.add(release)
                            }
                        }
                    }

                    _releaseTypes.value = types
                    _buckets.value = listOfNotNull(
                        own.takeIf { it.isNotEmpty() }
                            ?.let { DiscographyBucket(DiscographyCategory.FROM_ARTIST, it.sortedByDescending(AlbumItem::year)) },
                        appearsOn.takeIf { it.isNotEmpty() }
                            ?.let { DiscographyBucket(DiscographyCategory.APPEARS_ON, it.sortedByDescending(AlbumItem::year)) },
                    )
                    _loading.value = false
                }
                .onFailure {
                    _loading.value = false
                }
        }
    }
}
