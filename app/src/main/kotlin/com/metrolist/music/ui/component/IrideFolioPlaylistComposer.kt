package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.viewmodels.PlaylistDraftError
import com.metrolist.music.viewmodels.PlaylistDraftState
import sv.lib.squircleshape.SquircleShape

/** The entire draft presentation lives in Folio; entry points supply only state and actions. */
@Composable
fun IrideFolioPlaylistComposer(
    draft: PlaylistDraftState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    onToggleSong: (MediaMetadata) -> Unit,
    onRefresh: () -> Unit,
    onRetrySeeds: () -> Unit,
    onCreate: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val searchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf(TextFieldValue(draft.query)) }
    val selectedIds = remember(draft.songs) { draft.songs.map { it.id }.toSet() }
    val tracks = if (draft.showingSelection) draft.songs else draft.suggestions
    val selectedLabel = pluralStringResource(R.plurals.playlist_draft_tracks, draft.songs.size, draft.songs.size)
    val allSongs = remember(draft.existingSongs, draft.songs) { draft.existingSongs + draft.songs }
    val thumbnails = remember(allSongs) { allSongs.asSequence().mapNotNull { it.thumbnailUrl }.distinct().take(4).toList() }
    val duration = remember(allSongs) { allSongs.sumOf { it.duration.coerceAtLeast(0).toLong() } }
    val totalLabel = pluralStringResource(R.plurals.playlist_draft_tracks, allSongs.size, allSongs.size)

    IridePageLayout(
        title = stringResource(if (draft.addingToPlaylist) R.string.playlist_draft_add_music else R.string.playlist_draft_title),
        state = listState,
        showTopBar = true,
        bottomPadding = IrideFolio.PlaylistDraftSectionGap,
        navigationIcon = {
            IrideOutlineIconButton(
                onClick = onDismiss,
                icon = R.drawable.close,
                contentDescription = stringResource(R.string.playlist_draft_close),
                enabled = !draft.saving,
            )
        },
        actions = {
            val saveLabel = stringResource(when {
                draft.saving -> R.string.playlist_draft_saving
                draft.addingToPlaylist -> R.string.playlist_draft_add
                else -> R.string.playlist_draft_create
            })
            IrideFolioPrimaryIconButton(
                onClick = { focus.clearFocus(); onCreate() },
                size = IrideFolio.ControlSurfaceSize,
                enabled = !draft.preparing && !draft.saving && draft.error != PlaylistDraftError.INITIAL_SONGS &&
                    (!draft.addingToPlaylist || draft.songs.isNotEmpty()),
            ) {
                if (draft.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IrideFolio.CollectionActionIconSize).semantics { contentDescription = saveLabel },
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.background,
                    )
                } else {
                    Icon(painterResource(R.drawable.check), saveLabel, Modifier.size(IrideFolio.CollectionActionIconSize))
                }
            }
        },
    ) {
        item(key = "draft_identity") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = IrideFolio.HorizontalPadding,
                    vertical = IrideFolio.PlaylistDraftSectionGap,
                ),
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.PlaylistDraftSectionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaylistThumbnail(
                    thumbnails = thumbnails,
                    size = IrideFolio.PlaylistDraftArtworkSize,
                    shape = IrideFolio.PlaylistDraftArtworkShape,
                    placeHolder = {
                        Icon(
                            painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            modifier = Modifier.size(IrideFolio.CollectionPlayIconSize),
                            tint = MaterialTheme.colorScheme.textSecondary,
                        )
                    },
                    softBorder = true,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
                ) {
                    val nameLabel = stringResource(R.string.playlist_draft_name)
                    val nameStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = IBMPlexMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = IrideFolio.PlaylistDraftNameFontSize,
                        lineHeight = IrideFolio.PlaylistDraftNameLineHeight,
                        color = MaterialTheme.colorScheme.textPrimary,
                    )
                    BasicTextField(
                        value = draft.name,
                        onValueChange = onNameChange,
                        enabled = !draft.saving && !draft.preparing,
                        readOnly = draft.addingToPlaylist,
                        singleLine = true,
                        textStyle = nameStyle,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameLabel },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { searchFocus.requestFocus() }),
                        decorationBox = { field ->
                            Box {
                                if (draft.name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.playlist_draft_untitled),
                                        style = nameStyle,
                                        color = MaterialTheme.colorScheme.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                field()
                            }
                        },
                    )
                    Text(
                        text = if (duration > 0) "$totalLabel · ${makeTimeString(duration * 1000)}" else totalLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = IBMPlexMonoFontFamily),
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }
        }
        item(key = "draft_search") {
            IrideFolioCollectionSearchBox(
                query = query,
                onQueryChange = {
                    if (!draft.saving) {
                        query = it
                        onQueryChange(it.text)
                    }
                },
                placeholderText = stringResource(R.string.playlist_draft_search),
                focusRequester = searchFocus,
                onFocusChanged = {},
                onSearch = { focus.clearFocus() },
                onClear = {
                    if (!draft.saving) {
                        query = TextFieldValue()
                        onQueryChange("")
                    }
                },
                modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding),
            )
        }
        item(key = "draft_sections") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = IrideFolio.HorizontalPadding,
                    vertical = IrideFolio.DialogSectionSpacing,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
                    verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
                ) {
                    IrideFolioChoiceButton(
                        selected = !draft.showingSelection,
                        onClick = { onSelectionChange(false) },
                        enabled = !draft.saving,
                        compact = true,
                    ) { Text(stringResource(R.string.playlist_draft_discover)) }
                    IrideFolioChoiceButton(
                        selected = draft.showingSelection,
                        onClick = { onSelectionChange(true) },
                        enabled = !draft.saving,
                        compact = true,
                    ) { Text(selectedLabel) }
                }
                if (draft.loading || draft.preparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IrideFolio.ShelfSelectorIconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                } else if (!draft.showingSelection && draft.query.isBlank()) {
                    IrideOutlineIconButton(
                        onClick = onRefresh,
                        icon = R.drawable.refresh,
                        contentDescription = stringResource(R.string.playlist_draft_refresh),
                        enabled = !draft.saving,
                        iconSize = IrideFolio.ShelfSelectorIconSize,
                    )
                }
            }
        }
        if (draft.error != null) {
            item(key = "draft_error") {
                IrideFolioDraftMessage(
                    text = stringResource(
                        if (draft.error == PlaylistDraftError.INITIAL_SONGS) R.string.playlist_draft_seed_error
                        else R.string.playlist_draft_save_error,
                    ),
                    onRetry = if (draft.error == PlaylistDraftError.INITIAL_SONGS) onRetrySeeds else onCreate,
                )
            }
        }
        if (!draft.showingSelection && draft.unavailable) {
            item(key = "draft_network") {
                IrideFolioDraftMessage(stringResource(R.string.playlist_draft_offline), onRefresh)
            }
        }
        items(tracks, key = { "draft_song_${it.id}" }) { song ->
            IrideFolioDraftSong(
                song = song,
                selected = song.id in selectedIds,
                enabled = !draft.saving && !draft.preparing,
                onToggle = { onToggleSong(song) },
            )
        }
        if (tracks.isEmpty() && !draft.loading && !draft.preparing && draft.error == null) {
            item(key = "draft_empty") {
                IrideFolioDraftMessage(
                    stringResource(
                        when {
                            draft.showingSelection -> R.string.playlist_draft_empty_selection
                            draft.query.isNotBlank() -> R.string.playlist_draft_no_results
                            else -> R.string.playlist_draft_start
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun IrideFolioDraftSong(song: MediaMetadata, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    ListItem(
        title = song.title,
        subtitle = {
            Text(
                song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = song.thumbnailUrl,
                isActive = false,
                isPlaying = false,
                modifier = Modifier.size(ListThumbnailSize),
                shape = SquircleShape(radius = ThumbnailCornerRadius, cornerSmoothing = 0.5f),
                softBorder = true,
            )
        },
        trailingContent = {
            Box(
                Modifier.size(IrideFolio.CollectionActionTouchTargetSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (selected) R.drawable.check else R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier.size(IrideFolio.CollectionActionIconSize),
                    tint = if (selected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.textSecondary,
                )
            }
        },
        modifier = Modifier.toggleable(
            value = selected,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = { onToggle() },
        ),
    )
}

@Composable
private fun IrideFolioDraftMessage(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = IrideFolio.HorizontalPadding,
            vertical = IrideFolio.PlaylistDraftSectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogSectionSpacing),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = IBMPlexMonoFontFamily),
            color = MaterialTheme.colorScheme.textSecondary,
        )
        if (onRetry != null) {
            IrideFolioSecondaryButton(onClick = onRetry) {
                Text(stringResource(R.string.playlist_draft_retry))
            }
        }
    }
}
