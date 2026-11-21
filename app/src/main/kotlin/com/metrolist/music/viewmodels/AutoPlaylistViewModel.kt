/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AutoPlaylistSongSortDescendingKey
import com.metrolist.music.constants.AutoPlaylistSongSortType
import com.metrolist.music.constants.AutoPlaylistSongSortTypeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator
import java.util.Locale
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!
    private val isOffline = savedStateHandle.get<Boolean>("offline") ?: false

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val artistRecoveryMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs: StateFlow<List<Song>?> =
        context.dataStore.data
            .map {
                Triple(
                    it[AutoPlaylistSongSortTypeKey].toEnum(AutoPlaylistSongSortType.CREATE_DATE) to (it[AutoPlaylistSongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit, hideVideoSongs) ->
                val (sortType, descending) = sortDesc
                val sortTypeMapped = when (sortType) {
                    AutoPlaylistSongSortType.CREATE_DATE -> SongSortType.CREATE_DATE
                    AutoPlaylistSongSortType.NAME -> SongSortType.NAME
                    AutoPlaylistSongSortType.ARTIST -> SongSortType.ARTIST
                    AutoPlaylistSongSortType.PLAY_TIME -> SongSortType.PLAY_TIME
                }

                when (playlist) {
                    "liked" -> database.likedSongs(sortTypeMapped, descending)
                    "downloaded" -> database.downloadedSongs(sortTypeMapped, descending)
                    "uploaded" -> database.uploadedSongs(sortTypeMapped, descending)
                    "starred" -> database.starredSongs(sortTypeMapped, descending)
                    "all_tracks" -> if (isOffline) {
                        combine(
                            database.downloadedSongs(sortTypeMapped, descending),
                            database.localSongs(),
                        ) { downloaded, local -> (downloaded + local).distinctBy { it.id } }
                    } else {
                        database.allSongs()
                    }
                    "local_songs" -> database.localSongs()
                    else -> flowOf(emptyList())
                }.map {
                    val visible = it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs)
                    if (playlist == "all_tracks" || playlist == "local_songs") {
                        sortCollectionSongs(visible, sortType, descending)
                    } else {
                        visible
                    }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    val playlistDuration: StateFlow<Int?> = likedSongs
        .filterNotNull()
        .map { songs -> songs.sumOf { it.song.duration } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        if (!isOffline && (playlist == "liked" || playlist == "starred")) {
            viewModelScope.launch(Dispatchers.IO) {
                likedSongs.first { it != null }
                restoreFavoriteArtists()
            }
        }
    }

    private suspend fun restoreFavoriteArtists() = artistRecoveryMutex.withLock {
        val missingIds = likedSongs.value.orEmpty()
            .filter { song ->
                !song.song.isLocal && !song.song.isUploaded && !song.song.isEpisode &&
                    song.artists.none { it.name.isNotBlank() }
            }
            .map { it.id }

        for (batch in missingIds.chunked(50)) {
            val ids = batch.toHashSet()
            val songs = YouTube.queue(batch).getOrNull().orEmpty()
                .filter { it.id in ids && it.artists.any { artist -> artist.name.isNotBlank() } }
            if (songs.isEmpty()) continue
            database.withTransaction {
                songs.forEach { restoreMissingSongArtists(it.toMediaMetadata()) }
            }
        }
    }

    val playlistSongCount: StateFlow<Int?> =
        when (playlist) {
            "liked" -> database.likedSongsCount()
            "starred" -> database.starredSongsCount()
            else -> likedSongs.filterNotNull().map { it.size }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    val favoriteCoverThumbnails: StateFlow<List<String>?> =
        when (playlist) {
            "liked" -> database.lastLikedSongThumbnails()
            "starred" -> database.starredSongThumbnails()
            else -> flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                when (playlist) {
                    "liked" -> syncUtils.syncLikedSongsSuspend()
                    "uploaded" -> syncUtils.syncUploadedSongsSuspend()
                }
                if (!isOffline && (playlist == "liked" || playlist == "starred")) {
                    restoreFavoriteArtists()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val categoryController = PlaylistCategoryController(database, playlist, viewModelScope)
    val categories get() = categoryController.categories
    val songCategoryIds get() = categoryController.songCategoryIds

    fun createCategory(name: String, colorHex: String? = null) = categoryController.createCategory(name, colorHex)
    fun removeCategory(category: com.metrolist.music.db.entities.PlaylistCategoryEntity) = categoryController.removeCategory(category)
    fun addSongsToCategories(songIds: List<String>, categoryIds: List<String>) =
        categoryController.addSongsToCategories(songIds, categoryIds)
    fun reorderCategories(orderedCategoryIds: List<String>) = categoryController.reorderCategories(orderedCategoryIds)
    fun syncAutoCategories(genreBySongId: Map<String, List<String>>) = categoryController.syncAutoCategories(genreBySongId)
}

private fun sortCollectionSongs(
    songs: List<Song>,
    sortType: AutoPlaylistSongSortType,
    descending: Boolean,
): List<Song> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    val sorted = when (sortType) {
        AutoPlaylistSongSortType.CREATE_DATE -> songs.sortedBy {
            it.song.inLibrary ?: it.song.dateDownload ?: it.song.date ?: LocalDateTime.MIN
        }
        AutoPlaylistSongSortType.NAME -> songs.sortedWith(compareBy(collator) { it.song.title })
        AutoPlaylistSongSortType.ARTIST -> songs.sortedWith(
            compareBy(collator) { song -> song.orderedArtists.joinToString("") { it.name } },
        )
        AutoPlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
    }
    return if (descending) sorted.asReversed() else sorted
}
