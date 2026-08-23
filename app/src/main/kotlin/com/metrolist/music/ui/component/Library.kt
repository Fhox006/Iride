/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.R
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import kotlinx.coroutines.CoroutineScope

@Composable
fun NewReleaseBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .background(
                androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                androidx.compose.foundation.shape.RoundedCornerShape(50),
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "+$count",
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = com.metrolist.music.ui.theme.SpaceMonoFontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun LibraryArtistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    newSongCount: Int = 0,
    modifier: Modifier = Modifier
) = ArtistListItem(
    artist = artist,
    showLikedIcon = false,
    badges = {
        NewReleaseBadge(
            count = newSongCount,
            modifier = Modifier.padding(end = 4.dp),
        )
    },
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = stringResource(R.string.more_options)
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate("artist/${artist.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryArtistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    newSongCount: Int = 0,
    modifier: Modifier = Modifier
) = ArtistGridItem(
    artist = artist,
    showLikedIcon = false,
    badges = {
        NewReleaseBadge(
            count = newSongCount,
            modifier = Modifier.padding(end = 4.dp),
        )
    },
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate("artist/${artist.id}")
            },
            onLongClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

@Composable
fun LibraryAlbumListItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) = AlbumListItem(
    album = album,
    showLikedIcon = false,
    isActive = isActive,
    isPlaying = isPlaying,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    AlbumMenu(
                        originalAlbum = album,
                        navController = navController,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = stringResource(R.string.more_options)
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate("album/${album.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAlbumGridItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = AlbumGridItem(
    album = album,
    showLikedIcon = false,
    isActive = isActive,
    isPlaying = isPlaying,
    coroutineScope = coroutineScope,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate("album/${album.id}")
            },
            onLongClick = {
                menuState.show {
                    AlbumMenu(
                        originalAlbum = album,
                        navController = navController,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContinueListeningAlbumItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean,
    isPlaying: Boolean,
    size: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.width(size)) {
        AlbumGridItem(
            album = album,
            showLikedIcon = false,
            isActive = isActive,
            isPlaying = isPlaying,
            coroutineScope = coroutineScope,
            size = size,
            modifier = Modifier.combinedClickable(
                onClick = { navController.navigate("album/${album.id}") },
                onLongClick = {
                    menuState.show {
                        AlbumMenu(originalAlbum = album, navController = navController, onDismiss = menuState::dismiss)
                    }
                },
            ),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.remove_from_continue_listening),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(16.dp)
                    .shadow(2.dp, CircleShape),
            )
        }
    }
}

@Composable
private fun SuggestedFollowActions(
    onFollow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(onClick = onFollow)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = stringResource(R.string.subscribe),
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(12.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .size(18.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.remove_suggested_follow_artist),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySuggestedFollowArtistItem(
    navController: NavController,
    artist: Artist,
    onFollow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ArtistGridItem(
    artist = artist,
    showLikedIcon = false,
    fillMaxWidth = true,
    badges = {
        SuggestedFollowActions(onFollow = onFollow, onDismiss = onDismiss)
    },
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { navController.navigate("artist/${artist.id}") },
            onLongClick = onDismiss,
        )
)

@Composable
fun LibrarySuggestedFollowArtistListItem(
    navController: NavController,
    artist: Artist,
    onFollow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ArtistListItem(
    artist = artist,
    showLikedIcon = false,
    badges = {
        SuggestedFollowActions(onFollow = onFollow, onDismiss = onDismiss)
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable { navController.navigate("artist/${artist.id}") },
)

@Composable
fun LibraryPlaylistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier
) = PlaylistListItem(
    playlist = playlist,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = stringResource(R.string.more_options)
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.browseId != null)
                navController.navigate("online_playlist/${playlist.playlist.browseId}")
            else
                navController.navigate("local_playlist/${playlist.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier
) = PlaylistGridItem(
    playlist = playlist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.browseId != null)
                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                else
                    navController.navigate("local_playlist/${playlist.id}")
            },
            onLongClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        )
)
