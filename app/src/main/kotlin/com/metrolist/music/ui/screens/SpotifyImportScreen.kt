package com.metrolist.music.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AppleMusicImportConnectedKey
import com.metrolist.music.constants.SpotifyImportConnectedKey
import com.metrolist.music.ui.component.ExportifyPageError
import com.metrolist.music.ui.component.ExportifyWebView
import com.metrolist.music.ui.component.IrideFolioMenuAction
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.ui.component.PlaylistExportService
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.SpotifyImportError
import com.metrolist.music.viewmodels.SpotifyImportPhase
import com.metrolist.music.viewmodels.SpotifyImportViewModel

@Composable
fun SpotifyImportScreen(navController: NavController) = ServicePlaylistImportScreen(navController, PlaylistExportService.SPOTIFY)

@Composable
fun AppleMusicImportScreen(navController: NavController) = ServicePlaylistImportScreen(navController, PlaylistExportService.APPLE_MUSIC)

@Composable
private fun ServicePlaylistImportScreen(
    navController: NavController,
    service: PlaylistExportService,
    viewModel: SpotifyImportViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current.findImportActivity() ?: error("Playlist import requires an activity"),
        key = "playlist_import_$service",
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spotify = service == PlaylistExportService.SPOTIFY
    var connected by rememberPreference(if (spotify) SpotifyImportConnectedKey else AppleMusicImportConnectedKey, false)
    var loading by remember { mutableStateOf(true) }
    var receiving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ExportifyPageError?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var changeAccount by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importUri) }
    val chooseFile: () -> Unit = {
        picker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "application/zip", "application/x-zip-compressed", "application/octet-stream"))
    }
    val bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val browserHeight = (LocalConfiguration.current.screenHeightDp * 0.65f).dp.coerceIn(380.dp, 640.dp)
    IridePageFrame(
        title = stringResource(if (spotify) R.string.music_import_spotify else R.string.music_import_apple),
        scrollState = rememberScrollState(),
        navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
        menuSections = listOf(buildList {
            add(IrideFolioMenuAction(stringResource(R.string.spotify_exportify_import_csv), R.drawable.download, chooseFile))
            add(IrideFolioMenuAction(stringResource(R.string.spotify_exportify_reload), R.drawable.refresh, { reload++ }))
            if (spotify) add(IrideFolioMenuAction(stringResource(R.string.spotify_exportify_change_account), R.drawable.person, {
                connected = false
                changeAccount++
            }))
            add(IrideFolioMenuAction(stringResource(R.string.music_import_open_browser), R.drawable.link, { context.openPlaylistExporter(service) }))
        }),
    ) {
        // Keep the browser composed while reviewing results, including multi-file exports.
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(if (spotify) R.string.spotify_exportify_instructions else R.string.music_services_apple_instructions),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().height(browserHeight)) {
                ExportifyWebView(
                    modifier = Modifier.fillMaxSize(), service = service, handleBackNavigation = false,
                    reloadRequest = reload, changeAccountRequest = changeAccount,
                    onConnected = { if (!connected) connected = true }, connectionKnown = connected,
                    onExport = { file ->
                        receiving = false
                        viewModel.importFile(file)
                    },
                    onError = { failure = ExportifyPageError.Connection; loading = false; receiving = false },
                    onLoading = { active -> loading = active; if (active) failure = null },
                    onPageError = { error -> failure = error; loading = false; receiving = false },
                    onDownloadStarted = { receiving = true },
                )
                if (loading || receiving) CircularProgressIndicator(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                failure?.let { error ->
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) {
                        Text(if (error is ExportifyPageError.Http) stringResource(R.string.spotify_exportify_http_error, error.statusCode)
                            else stringResource(R.string.spotify_exportify_connection_error))
                        IrideFolioSecondaryButton(onClick = { reload++ }) { Text(stringResource(R.string.spotify_exportify_reload)) }
                        IrideFolioSecondaryButton(onClick = { context.openPlaylistExporter(service) }) { Text(stringResource(R.string.music_import_open_browser)) }
                        IrideFolioSecondaryButton(onClick = chooseFile) { Text(stringResource(R.string.spotify_exportify_choose_file)) }
                    }
                }
            }
            if (state.reading || receiving) Text(stringResource(R.string.music_import_reading), style = MaterialTheme.typography.bodySmall)
            state.error?.let { error ->
                Text(stringResource(when (error) {
                    SpotifyImportError.INVALID_FILE -> R.string.music_services_invalid_file
                    SpotifyImportError.FILE_TOO_LARGE -> R.string.spotify_exportify_file_too_large
                    SpotifyImportError.READ_FAILED -> R.string.spotify_exportify_read_failed
                    SpotifyImportError.SAVE_FAILED -> R.string.spotify_exportify_save_failed
                }), color = MaterialTheme.colorScheme.error)
            }
            state.playlists.forEach { playlist ->
                val resolving = playlist.phase in setOf(SpotifyImportPhase.PENDING, SpotifyImportPhase.RESOLVING)
                PlaylistImportRow(
                    title = playlist.name,
                    subtitle = when {
                        playlist.phase == SpotifyImportPhase.SAVED -> stringResource(R.string.music_import_saved)
                        resolving -> stringResource(R.string.spotify_exportify_progress, playlist.processed, playlist.total)
                        playlist.phase == SpotifyImportPhase.FAILED -> stringResource(R.string.spotify_exportify_invalid_csv)
                        else -> stringResource(R.string.music_import_match_count, playlist.matched, playlist.unmatched + playlist.failed)
                    },
                    busy = resolving, saved = playlist.phase == SpotifyImportPhase.SAVED,
                    progress = if (resolving && playlist.total > 0) playlist.processed.toFloat() / playlist.total else null,
                    onOpen = playlist.savedPlaylistId?.let { id -> { navController.navigate("local_playlist/$id") } },
                )
                if (playlist.phase == SpotifyImportPhase.READY && (playlist.unmatched > 0 || playlist.failed > 0)) {
                    IrideFolioSecondaryButton(onClick = { viewModel.retryFailures(playlist.id) }, enabled = !state.saving) {
                        Text(stringResource(R.string.spotify_exportify_retry))
                    }
                }
            }
            if (state.playlists.any { it.phase != SpotifyImportPhase.SAVED }) {
                IrideFolioPrimaryButton(onClick = viewModel::saveReadyPlaylists, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.saving) R.string.music_import_saving else R.string.spotify_exportify_save))
                }
            }
        }
    }
}

private fun Context.openPlaylistExporter(service: PlaylistExportService) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(service.url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.music_import_no_browser, Toast.LENGTH_LONG).show()
    }
}

internal tailrec fun Context.findImportActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findImportActivity()
    else -> null
}
