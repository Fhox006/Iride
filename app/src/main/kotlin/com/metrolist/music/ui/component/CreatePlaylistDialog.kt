/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.isSyncEnabled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import timber.log.Timber

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isCreating by remember { mutableStateOf(false) }

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        autoDismiss = false,
        isInputValid = { it.isNotBlank() && !isCreating },
        onDone = { playlistName ->
            if (isCreating || playlistName.isBlank()) return@TextFieldDialog
            isCreating = true
            coroutineScope.launch(Dispatchers.IO) {
                val browseId =
                    if (allowSyncing && context.isSyncEnabled()) {
                        try {
                            YouTube.createPlaylist(playlistName)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            Timber.e(error, "Failed to create synced playlist; saving locally")
                            null
                        }
                    } else {
                        null
                    }

                val playlistEntity =
                    PlaylistEntity(
                        name = playlistName,
                        browseId = browseId,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true,
                        isAutoSync = browseId != null,
                    )

                database.insert(playlistEntity)

                withContext(Dispatchers.Main) {
                    onPlaylistCreated?.invoke(playlistEntity.id)
                    onDismiss()
                }
            }
        },
    )
}
