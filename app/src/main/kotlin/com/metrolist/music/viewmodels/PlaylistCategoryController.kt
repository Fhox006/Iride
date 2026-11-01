/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistCategoryEntity
import com.metrolist.music.db.entities.PlaylistCategorySongMap
import com.metrolist.music.db.entities.PlaylistCategoryWithCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Playlist category CRUD shared by [LocalPlaylistViewModel] (real playlists) and
 * [AutoPlaylistViewModel] (virtual "liked"/"downloaded"/"uploaded"/"starred" ids) — same table,
 * same rules, only the playlistId differs. See [PlaylistCategoryEntity] for why there's no FK.
 */
class PlaylistCategoryController(
    private val database: MusicDatabase,
    private val playlistId: String,
    scope: CoroutineScope,
) {
    val categories: StateFlow<List<PlaylistCategoryWithCount>> =
        database
            .categoriesForPlaylist(playlistId)
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val songCategoryIds: StateFlow<Map<String, List<String>>> =
        database
            .categorySongMapsForPlaylist(playlistId)
            .map { maps -> maps.groupBy({ it.songId }, { it.categoryId }) }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun createCategory(name: String, colorHex: String? = null): PlaylistCategoryEntity {
        val category = PlaylistCategoryEntity(
            playlistId = playlistId,
            name = name,
            colorHex = colorHex,
            position = categories.value.size,
        )
        database.query { insert(category) }
        return category
    }

    /** No-op for auto (genre-derived) categories — those can only be repositioned. */
    fun removeCategory(category: PlaylistCategoryEntity) {
        if (category.isAuto) return
        database.query { delete(category) }
    }

    fun addSongsToCategories(songIds: List<String>, categoryIds: List<String>) {
        if (songIds.isEmpty() || categoryIds.isEmpty()) return
        val maps = songIds.flatMap { songId ->
            categoryIds.map { categoryId -> PlaylistCategorySongMap(categoryId = categoryId, songId = songId) }
        }
        database.query { insertCategorySongMaps(maps) }
    }

    /** Persists a full drag-reorder result — [orderedCategoryIds] is the new top-to-bottom order. */
    fun reorderCategories(orderedCategoryIds: List<String>) {
        database.transaction {
            orderedCategoryIds.forEachIndexed { index, categoryId ->
                updateCategoryPosition(categoryId, index)
            }
        }
    }

    /**
     * Upserts one auto category per genre with >=2 matching songs (mirrors [GenreFilterState]'s
     * own >=2 threshold) and syncs its song membership. Safe to call every time genre resolution
     * settles — matching by name is idempotent, and song maps are fully replaced each time.
     */
    fun syncAutoCategories(genreBySongId: Map<String, List<String>>) {
        val songIdsByGenre = mutableMapOf<String, MutableList<String>>()
        genreBySongId.forEach { (songId, genres) ->
            genres.forEach { genre -> songIdsByGenre.getOrPut(genre) { mutableListOf() }.add(songId) }
        }
        val qualifyingGenres = songIdsByGenre.filterValues { it.size >= 2 }
        if (qualifyingGenres.isEmpty()) return

        val existingAuto = categories.value.filter { it.category.isAuto }
        val existingByName = existingAuto.associateBy { it.category.name }
        var nextPosition = categories.value.size

        database.transaction {
            qualifyingGenres.forEach { (genre, songIds) ->
                val category = existingByName[genre]?.category ?: PlaylistCategoryEntity(
                    playlistId = playlistId,
                    name = genre,
                    isAuto = true,
                    position = nextPosition++,
                ).also { insert(it) }

                deleteCategorySongMaps(category.id)
                insertCategorySongMaps(songIds.map { songId -> PlaylistCategorySongMap(categoryId = category.id, songId = songId) })
            }
        }
    }
}
