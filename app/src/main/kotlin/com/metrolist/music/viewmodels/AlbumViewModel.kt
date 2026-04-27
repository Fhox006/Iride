/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.data.remote.MusicBrainzRepository
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    private val musicBrainzRepository: MusicBrainzRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()

            // ✅ Salva il Result in una variabile — così possiamo usarlo
            // sia per onSuccess/onFailure sia dopo, nel coroutine body
            val ytResult = YouTube.album(albumId)

            ytResult
                .onSuccess { albumPage ->
                    playlistId.value = albumPage.album.playlistId
                    otherVersions.value = albumPage.otherVersions
                    database.transaction {
                        if (album == null) {
                            insert(albumPage)
                        } else {
                            update(album.album, albumPage, album.artists)
                        }
                    }
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }

            // ✅ MusicBrainz enrichment — siamo ancora nel coroutine body,
            // quindi le suspend functions sono chiamabili normalmente
            val albumPage = ytResult.getOrNull() ?: return@launch

            val currentReleaseDate = album?.album?.releaseDate
            val regex = Regex("""\d{4}-\d{2}(-\d{2})?""")

            if (currentReleaseDate == null || !regex.matches(currentReleaseDate)) {
                val releaseDate = musicBrainzRepository.getAlbumReleaseDate(
                    albumTitle = albumPage.album.title,
                    artistName = albumPage.album.artists?.firstOrNull()?.name,
                    year = albumPage.album.year
                )
                if (releaseDate != null) {
                    // ✅ Re-fetch diretto nel coroutine body — nessun problema
                    database.album(albumId).first()?.let { currentAlbum ->
                        database.query {
                            update(currentAlbum.album.copy(releaseDate = releaseDate))
                        }
                    }
                }
            }
        }
    }
}