package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary
import sv.lib.squircleshape.SquircleShape

/** One quiet destination picker for both library tracks and imported tracks. */
@Composable
fun IrideFolioPlaylistPicker(
    playlists: List<Playlist>,
    containingSong: Set<String>,
    busy: Boolean = false,
    failed: Boolean = false,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (Playlist) -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val editable = remember(playlists) {
        playlists.filter { it.playlist.isEditable }.sortedWith(
            compareByDescending<Playlist> { it.playlist.lastUpdateTime ?: it.playlist.createdAt }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.playlist.name },
        )
    }
    val visible = remember(editable, query.text) {
        editable.filter { it.playlist.name.contains(query.text.trim(), ignoreCase = true) }
    }
    IrideFolioDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            Modifier.heightIn(max = IrideFolio.PlaylistPickerMaxHeight)
                .imePadding()
                .padding(top = IrideFolio.DialogContentPadding),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.playlist_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IrideFolio.ShelfSelectorIconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
                IrideOutlineIconButton(
                    onClick = onDismiss,
                    icon = R.drawable.close,
                    contentDescription = stringResource(R.string.playlist_picker_close),
                    iconSize = IrideFolio.CollectionActionIconSize,
                    enabled = !busy,
                )
            }
            IrideFolioSecondaryButton(
                onClick = onCreate,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = IrideFolio.HorizontalPadding,
                    vertical = IrideFolio.DialogSectionSpacing,
                ),
            ) {
                Icon(painterResource(R.drawable.add), null, Modifier.size(IrideFolio.CollectionActionIconSize))
                Spacer(Modifier.size(IrideFolio.DialogActionSpacing))
                Text(stringResource(R.string.playlist_draft_title))
            }
            if (editable.size > 6) {
                IrideFolioCollectionSearchBox(
                    query = query,
                    onQueryChange = { if (!busy) query = it },
                    placeholderText = stringResource(R.string.playlist_picker_search),
                    focusRequester = focus,
                    onFocusChanged = {},
                    onSearch = {},
                    onClear = { if (!busy) query = TextFieldValue() },
                    modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding),
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().irideFolioGestures(listState),
                overscrollEffect = null,
                contentPadding = PaddingValues(vertical = IrideFolio.DialogActionSpacing),
            ) {
                if (failed) {
                    item(key = "playlist_add_error") {
                        Text(
                            stringResource(R.string.playlist_picker_add_error),
                            modifier = Modifier.padding(IrideFolio.HorizontalPadding),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                        )
                    }
                }
                items(visible, key = { it.id }) { playlist ->
                    ListItem(
                        title = playlist.playlist.name,
                        subtitle = {
                            Text(
                                pluralStringResource(R.plurals.playlist_draft_tracks, playlist.songCount, playlist.songCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textSecondary,
                            )
                        },
                        thumbnailContent = {
                            PlaylistThumbnail(
                                thumbnails = playlist.thumbnails,
                                size = ListThumbnailSize,
                                shape = SquircleShape(ThumbnailCornerRadius, cornerSmoothing = 0.5f),
                                softBorder = true,
                                placeHolder = {
                                    Icon(painterResource(R.drawable.queue_music), null, Modifier.size(ListThumbnailSize / 2))
                                },
                            )
                        },
                        trailingContent = {
                            Box(Modifier.size(IrideFolio.CollectionActionTouchTargetSize), contentAlignment = Alignment.Center) {
                                if (playlist.id in containingSong) {
                                    Icon(painterResource(R.drawable.check), stringResource(R.string.playlist_picker_contains),
                                        Modifier.size(IrideFolio.CollectionActionIconSize))
                                }
                            }
                        },
                        modifier = Modifier.clickable(enabled = !busy) { onSelect(playlist) },
                    )
                }
                if (visible.isEmpty() && query.text.isNotBlank()) {
                    item {
                        Text(
                            stringResource(R.string.playlist_picker_no_results),
                            modifier = Modifier.padding(IrideFolio.HorizontalPadding),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
