/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.menu.NoteRatingStars
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
) {
    val database = LocalDatabase.current
    val albums by database.albumsWithNotes().collectAsState(initial = emptyList())
    val songs by database.songsWithNotes().collectAsState(initial = emptyList())

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    val albumsListState = rememberLazyListState()
    val songsListState = rememberLazyListState()
    val frostBackdrop = rememberFrostBackdrop()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordFrostBackdrop(frostBackdrop)
        ) {
        if (mainTopGradient) {
            TopScreenGradientBackground(
                mediaMetadata = mediaMetadata,
                playerBackground = playerBackgroundStyle,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)),
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabColors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.inverseSurface,
                        activeContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SegmentedButton(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = tabColors,
                        label = {
                            Text(
                                text = stringResource(R.string.notes_tab_albums),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                    SegmentedButton(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = tabColors,
                        label = {
                            Text(
                                text = stringResource(R.string.notes_tab_tracks),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                }
            }

            if (tabIndex == 0) {
                if (albums.isEmpty()) {
                    EmptyNotesState(stringResource(R.string.notes_empty_albums))
                } else {
                    LazyColumn(
                        state = albumsListState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(albums, key = { it.id }) { album ->
                            LongPressToDeleteNoteRow(
                                onDelete = {
                                    database.query {
                                        update(album.album.copy(noteTitle = null, noteRating = null, noteText = null))
                                    }
                                },
                            ) {
                                AlbumNoteRow(album)
                            }
                        }
                    }
                }
            } else {
                if (songs.isEmpty()) {
                    EmptyNotesState(stringResource(R.string.notes_empty_tracks))
                } else {
                    LazyColumn(
                        state = songsListState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(songs, key = { it.id }) { song ->
                            LongPressToDeleteNoteRow(
                                onDelete = {
                                    database.query {
                                        update(song.song.copy(noteTitle = null, noteRating = null, noteText = null))
                                    }
                                },
                            ) {
                                SongNoteRow(song)
                            }
                        }
                    }
                }
            }
        }
        }

        SettingsBackTopBar(
            title = stringResource(R.string.notes_screen_title),
            navController = navController,
            backdrop = frostBackdrop,
            revealProgress = rememberDiscreteProgress(
                active = (if (tabIndex == 0) albumsListState else songsListState).firstVisibleItemIndex > 0,
            ),
        )
    }
}

@Composable
private fun EmptyNotesState(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontFamily = SpaceMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun LongPressToDeleteNoteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DefaultDialog(
            onDismiss = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.notes_delete_title)) },
            content = {
                Text(
                    text = stringResource(R.string.notes_delete_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
        )
    }

    Box(
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteDialog = true
            },
        ),
    ) {
        content()
    }
}

@Composable
private fun NoteRow(
    coverUrl: String?,
    title: String,
    author: String,
    noteTitle: String,
    noteRating: Float?,
    noteText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(76.dp)) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            if (noteTitle.isNotBlank()) {
                Text(
                    text = noteTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (noteRating != null) {
                NoteRatingStars(rating = noteRating, modifier = Modifier.padding(top = if (noteTitle.isNotBlank()) 4.dp else 0.dp))
            }
            if (noteText.isNotBlank()) {
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = if (noteTitle.isNotBlank() || noteRating != null) 6.dp else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun AlbumNoteRow(album: Album) {
    NoteRow(
        coverUrl = album.thumbnailUrl,
        title = album.title,
        author = album.artists.joinToString { it.name },
        noteTitle = album.album.noteTitle.orEmpty(),
        noteRating = album.album.noteRating,
        noteText = album.album.noteText.orEmpty(),
    )
}

@Composable
private fun SongNoteRow(song: Song) {
    NoteRow(
        coverUrl = song.thumbnailUrl,
        title = song.title,
        author = song.artists.joinToString { it.name },
        noteTitle = song.song.noteTitle.orEmpty(),
        noteRating = song.song.noteRating,
        noteText = song.song.noteText.orEmpty(),
    )
}
