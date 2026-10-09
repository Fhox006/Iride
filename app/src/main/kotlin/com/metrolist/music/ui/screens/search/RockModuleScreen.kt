package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.SharedHomeSectionTitle
import com.metrolist.music.ui.component.SharedQuickPicksCarousel
import com.metrolist.music.ui.component.SharedYouTubePlaylistRow
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.viewmodels.RockModuleViewModel

@Composable
fun RockModuleScreen(
    navController: NavController,
    viewModel: RockModuleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val isPlaying =
        playerConnection?.isEffectivelyPlaying?.collectAsStateWithLifecycle()?.value ?: false
    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value

    IridePageLayout(
        titleRes = R.string.rock_module_title,
        showTopBar = true,
        bottomPadding =
            LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 20.dp,
        navigationIcon = {
            IridePageBackButton(navController)
        },
    ) {
        item(key = "rock_trending_title") {
            SharedHomeSectionTitle(
                title = stringResource(R.string.rock_trending_songs),
                onRefreshClick = viewModel::refresh,
                isRefreshing = state.songsLoading,
            )
        }
        item(key = "rock_trending_songs") {
            SharedQuickPicksCarousel(
                items = state.songs.take(20),
                key = { "rock_song_${it.id}" },
                loading = state.songsLoading,
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { song, itemWidth ->
                RockSongItem(
                    song = song,
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    modifier =
                        Modifier.padding(end = 4.dp).clickable {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection?.togglePlayPause()
                            } else {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        song.endpoint ?: WatchEndpoint(videoId = song.id),
                                        song.toMediaMetadata(),
                                    )
                                )
                            }
                        },
                    itemWidth = itemWidth,
                )
            }
        }
        if (!state.songsLoading && state.songs.isEmpty()) {
            item(key = "rock_songs_empty") {
                RockSectionMessage(
                    text =
                        stringResource(
                            if (state.songsFailed) R.string.rock_module_load_error
                            else R.string.rock_module_empty_songs
                        )
                )
            }
        }

        item(key = "rock_community_title") {
            SharedHomeSectionTitle(
                title = stringResource(R.string.rock_community_playlists),
                onRefreshClick = viewModel::refresh,
                isRefreshing = state.playlistsLoading,
            )
        }
        if (state.playlistsLoading) {
            item(key = "rock_community_loading") {
                RockSectionMessage(text = stringResource(R.string.rock_module_loading))
            }
        } else if (state.playlists.isEmpty()) {
            item(key = "rock_community_empty") {
                RockSectionMessage(
                    text =
                        stringResource(
                            if (state.playlistsFailed) R.string.rock_module_load_error
                            else R.string.rock_module_empty_playlists
                        )
                )
            }
        } else {
            item(key = "rock_community_row") {
                SharedYouTubePlaylistRow(
                    items = state.playlists,
                    key = { "rock_playlist_${it.id}" },
                    contentPadding = 20.dp,
                ) { playlist ->
                    RockPlaylistItem(playlist = playlist) {
                        navController.navigate("online_playlist/${playlist.id.removePrefix("VL")}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RockSongItem(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    itemWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    YouTubeListItem(
        item = song,
        isActive = isActive,
        isPlaying = isPlaying,
        isSwipeable = false,
        modifier = modifier.width(itemWidth),
    )
}

@Composable
private fun RockPlaylistItem(
    playlist: PlaylistItem,
    onClick: () -> Unit,
) {
    YouTubeGridItem(
        item = playlist,
        modifier = Modifier.clickable(onClick = onClick),
        coroutineScope = null,
    )
}

@Composable
private fun RockSectionMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    )
}
