/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.AutoPlaylistSongSortType
import com.metrolist.music.constants.AutoPlaylistSongSortTypeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[AutoPlaylistSongSortTypeKey].toEnum(AutoPlaylistSongSortType.CREATE_DATE) to (it[SongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit, hideVideoSongs) ->
                val (sortType, descending) = sortDesc
                // CUSTOM has no DB-level sort of its own — it's a reorder cache layered on top of
                // the natural (create-date) list, so new/removed songs fall in/out for free.
                val naturalSortType = when (sortType) {
                    AutoPlaylistSongSortType.CUSTOM, AutoPlaylistSongSortType.CREATE_DATE -> SongSortType.CREATE_DATE
                    AutoPlaylistSongSortType.NAME -> SongSortType.NAME
                    AutoPlaylistSongSortType.ARTIST -> SongSortType.ARTIST
                    AutoPlaylistSongSortType.PLAY_TIME -> SongSortType.PLAY_TIME
                }
                val naturalDescending = sortType == AutoPlaylistSongSortType.CUSTOM || descending

                val naturalFlow = when (playlist) {
                    "liked" -> database.likedSongs(naturalSortType, naturalDescending)
                    "downloaded" -> database.downloadedSongs(naturalSortType, naturalDescending)
                    "uploaded" -> database.uploadedSongs(naturalSortType, naturalDescending)
                    "starred" -> database.starredSongs(naturalSortType, naturalDescending)
                    else -> flowOf(emptyList())
                }.map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                if (sortType == AutoPlaylistSongSortType.CUSTOM) {
                    naturalFlow.combine(database.autoPlaylistSongOrder(playlist)) { natural, order ->
                        val bySongId = natural.associateBy { it.id }
                        val ordered = order.mapNotNull { bySongId[it] }
                        val orderedIds = ordered.mapTo(HashSet()) { it.id }
                        ordered + natural.filter { it.id !in orderedIds }
                    }
                } else {
                    naturalFlow
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun saveCustomOrder(songIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            database.query { saveAutoPlaylistSongOrder(playlist, songIds) }
        }
    }

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            when (playlist) {
                "liked" -> syncUtils.syncLikedSongsSuspend()
                "uploaded" -> syncUtils.syncUploadedSongsSuspend()
            }
            _isRefreshing.value = false
        }
    }
}
