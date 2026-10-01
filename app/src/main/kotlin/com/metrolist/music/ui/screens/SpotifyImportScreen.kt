package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.ExportifyWebView
import com.metrolist.music.ui.component.ExportifyPageError
import com.metrolist.music.ui.component.GridItem
import com.metrolist.music.ui.component.IridePageHeaderLayout
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.viewmodels.SpotifyImportError
import com.metrolist.music.viewmodels.SpotifyImportPhase
import com.metrolist.music.viewmodels.SpotifyImportPlaylist
import com.metrolist.music.viewmodels.SpotifyImportViewModel

@Composable
fun SpotifyImportScreen(navController: NavController, viewModel: SpotifyImportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var browserLoading by remember { mutableStateOf(true) }
    var browserError by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<ExportifyPageError?>(null) }
    var reloadRequest by remember { mutableIntStateOf(0) }
    var changeAccountRequest by remember { mutableIntStateOf(0) }
    var changingAccount by remember { mutableStateOf(false) }
    var spotifyLogin by remember { mutableStateOf(false) }
    var receiving by remember { mutableStateOf(false) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            receiving = false
            viewModel.importUri(uri)
        }
    }
    val pending = state.reading || state.saving || receiving || state.playlists.any { it.phase != SpotifyImportPhase.SAVED }
    val leave: () -> Unit = { if (pending) confirmLeave = true else navController.navigateUp() }
    val bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    IridePageHeaderLayout(
        title = stringResource(R.string.spotify_exportify_title),
        navigationIcon = { IconButton(onClick = leave) { Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back)) } },
    ) {
        // Preserve the browser while scrolling: an export can take several minutes.
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(when {
                state.saving -> R.string.spotify_exportify_saving
                state.reading || receiving -> R.string.spotify_exportify_receiving
                state.savedCount > 0 && state.playlists.all { it.phase == SpotifyImportPhase.SAVED } -> R.string.spotify_exportify_done
                state.playlists.isNotEmpty() -> R.string.spotify_exportify_received
                else -> R.string.spotify_exportify_instructions
            }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (spotifyLogin) R.string.spotify_exportify_login_service else R.string.spotify_exportify_service),
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (browserLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    TextButton(onClick = { changeAccountRequest++ }, enabled = !changingAccount && !receiving && !state.saving) {
                        Text(stringResource(R.string.spotify_exportify_change_account))
                    }
                }
                Box(Modifier.fillMaxWidth().height(440.dp)) {
                    ExportifyWebView(
                        modifier = Modifier.fillMaxSize(),
                        handleBackNavigation = !pending,
                        reloadRequest = reloadRequest,
                        changeAccountRequest = changeAccountRequest,
                        onAccountChangeInProgress = { changingAccount = it },
                        onPageHostChanged = { spotifyLogin = it == "accounts.spotify.com" },
                        onPageError = { error ->
                            pageError = error
                            browserLoading = false
                            receiving = false
                        },
                        onExport = { file -> receiving = false; browserError = false; viewModel.importFile(file) },
                        onError = { receiving = false; browserLoading = false; browserError = true },
                        onLoading = { loading ->
                            browserLoading = loading
                            if (loading) {
                                receiving = false
                                pageError = null
                                browserError = false
                            }
                        },
                        onDownloadStarted = { receiving = true; browserError = false },
                    )
                    if (browserLoading && pageError == null) {
                        Column(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            Text(stringResource(if (changingAccount) R.string.spotify_exportify_changing_account else R.string.spotify_exportify_loading_page),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    pageError?.let { error ->
                        Column(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {
                            Text(
                                text = if (error is ExportifyPageError.Http)
                                    stringResource(R.string.spotify_exportify_http_error, error.statusCode)
                                else stringResource(R.string.spotify_exportify_connection_error),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Button(onClick = { reloadRequest++ }) {
                                Text(stringResource(R.string.spotify_exportify_reload))
                            }
                        }
                    }
                }
            }
            if (browserError) Text(stringResource(R.string.spotify_exportify_browser_error), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { picker.launch(arrayOf("text/*", "application/zip", "application/x-zip-compressed", "application/octet-stream")) }, enabled = !state.saving) {
                    Text(stringResource(R.string.spotify_exportify_choose_file))
                }
                TextButton(onClick = { uriHandler.openUri("https://exportify.app") }) {
                    Icon(painterResource(R.drawable.open_in_new), stringResource(R.string.spotify_exportify_open_browser), Modifier.size(18.dp))
                    Text(stringResource(R.string.spotify_exportify_browser_label), Modifier.padding(start = 6.dp))
                }
            }
            if (state.playlists.isEmpty()) Text(stringResource(R.string.spotify_exportify_login_help),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error?.let { error ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(when (error) {
                        SpotifyImportError.INVALID_FILE -> R.string.spotify_exportify_invalid_file
                        SpotifyImportError.FILE_TOO_LARGE -> R.string.spotify_exportify_file_too_large
                        SpotifyImportError.READ_FAILED -> R.string.spotify_exportify_read_failed
                        SpotifyImportError.SAVE_FAILED -> R.string.spotify_exportify_save_failed
                    }), Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = viewModel::clearError) {
                        Icon(painterResource(R.drawable.close), stringResource(R.string.spotify_exportify_dismiss))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.spotify_exportify_loaded), style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold))
                if (state.reading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            state.playlists.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    row.forEach { playlist ->
                        key(playlist.id) {
                            ImportPlaylistCard(playlist, Modifier.weight(1f), !state.saving,
                                onRemove = { viewModel.remove(playlist.id) }, onRetry = { viewModel.retryFailures(playlist.id) },
                                onOpen = { playlist.savedPlaylistId?.let { navController.navigate("local_playlist/$it") } })
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (state.playlists.isNotEmpty()) Button(onClick = { confirmSave = true }, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.spotify_exportify_save))
            }
        }
    }
    // This guard also covers the toolbar back button; leaving would discard the staged import.
    BackHandler(enabled = pending, onBack = leave)
    if (confirmSave) {
        val unsaved = state.playlists.filter { it.phase == SpotifyImportPhase.READY }
        AlertDialog(onDismissRequest = { confirmSave = false },
            title = { Text(stringResource(R.string.spotify_exportify_save)) },
            text = { Text(stringResource(R.string.spotify_exportify_confirm_save,
                unsaved.size, unsaved.sumOf { it.matched }, unsaved.sumOf { it.unmatched + it.failed })) },
            confirmButton = { TextButton(onClick = { confirmSave = false; viewModel.saveReadyPlaylists() }, enabled = state.canSave) { Text(stringResource(R.string.spotify_exportify_save)) } },
            dismissButton = { TextButton(onClick = { confirmSave = false }) { Text(stringResource(R.string.spotify_exportify_cancel)) } })
    }
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false },
        title = { Text(stringResource(R.string.spotify_exportify_leave_title)) },
        text = { Text(stringResource(R.string.spotify_exportify_leave_message)) },
        confirmButton = { TextButton(onClick = { confirmLeave = false; navController.navigateUp() }, enabled = !state.saving) { Text(stringResource(R.string.spotify_exportify_leave)) } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.spotify_exportify_cancel)) } })
}

@Composable
private fun ImportPlaylistCard(playlist: SpotifyImportPlaylist, modifier: Modifier, enabled: Boolean,
    onRemove: () -> Unit, onRetry: () -> Unit, onOpen: () -> Unit) {
    val resolving = playlist.phase == SpotifyImportPhase.PENDING || playlist.phase == SpotifyImportPhase.RESOLVING
    val shape = RoundedCornerShape(5.dp)
    Column(modifier) {
        GridItem(fillMaxWidth = true,
            title = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)) },
            subtitle = {
                Column {
                    Text(if (playlist.phase == SpotifyImportPhase.FAILED) stringResource(R.string.spotify_exportify_invalid_csv)
                        else stringResource(R.string.spotify_exportify_match_count, playlist.matched, playlist.unmatched),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (playlist.failed > 0) Text(stringResource(R.string.spotify_exportify_failed_count, playlist.failed),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            },
            thumbnailContent = {
                if (resolving) Box(Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh), Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp)
                    if (playlist.total > 0) Text(stringResource(R.string.spotify_exportify_progress, playlist.processed, playlist.total),
                        Modifier.align(Alignment.BottomCenter).padding(12.dp), style = MaterialTheme.typography.labelSmall)
                } else ItemThumbnail(playlist.thumbnail, false, false, shape, hairlineBorder = true,
                    placeholder = { Icon(painterResource(R.drawable.playlist_play), null, Modifier.size(48.dp)) })
                if (playlist.phase != SpotifyImportPhase.SAVED) IconButton(onClick = onRemove, enabled = enabled,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) {
                    Icon(painterResource(R.drawable.close), stringResource(R.string.spotify_exportify_remove), Modifier.size(16.dp))
                }
            })
        if (resolving && playlist.total > 0) LinearProgressIndicator(progress = { playlist.processed.toFloat() / playlist.total },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        if ((playlist.failed > 0 || playlist.unmatched > 0) && playlist.phase == SpotifyImportPhase.READY) TextButton(onClick = onRetry, enabled = enabled) { Text(stringResource(R.string.spotify_exportify_retry)) }
        if (playlist.phase == SpotifyImportPhase.SAVED) TextButton(onClick = onOpen) {
            Icon(painterResource(R.drawable.check), null, Modifier.size(16.dp))
            Text(stringResource(R.string.spotify_exportify_saved), Modifier.padding(start = 4.dp))
        }
    }
}
