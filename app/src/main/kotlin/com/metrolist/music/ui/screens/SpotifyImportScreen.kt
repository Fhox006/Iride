package com.metrolist.music.ui.screens

import com.metrolist.music.ui.component.IrideFolioAlertDialog
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.ExportifyPageError
import com.metrolist.music.ui.component.ExportifyWebView
import com.metrolist.music.ui.component.GridItem
import com.metrolist.music.ui.component.IrideFolioMenuAction
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.viewmodels.SpotifyImportError
import com.metrolist.music.viewmodels.SpotifyImportPhase
import com.metrolist.music.viewmodels.SpotifyImportPlaylist
import com.metrolist.music.viewmodels.SpotifyImportViewModel

@Composable
fun SpotifyImportScreen(
    navController: NavController,
    viewModel: SpotifyImportViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current.findActivity()
            ?: error("Spotify import requires an activity-scoped owner"),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var browserLoading by remember { mutableStateOf(true) }
    var browserError by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<ExportifyPageError?>(null) }
    var reloadRequest by remember { mutableIntStateOf(0) }
    var spotifyLogin by remember { mutableStateOf(false) }
    var receiving by remember { mutableStateOf(false) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                receiving = false
                viewModel.importUri(uri)
            }
        }
    val leave: () -> Unit = { navController.navigateUp() }
    val bottom =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
    val pageScrollState = rememberScrollState()
    IridePageFrame(
        scrollState = pageScrollState,
        title = stringResource(R.string.spotify_exportify_title),
        navigationIcon = {
            IconButton(onClick = leave) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back))
            }
        },
        menuSections = listOf(
            listOf(
                IrideFolioMenuAction(
                    label = stringResource(R.string.spotify_exportify_import_csv),
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/*",
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                            )
                        )
                    },
                ),
            ),
        ),
    ) {
        // Preserve the browser while scrolling: an export can take several minutes.
        Column(
            Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(
                    when {
                        state.saving -> R.string.spotify_exportify_saving
                        state.reading || receiving -> R.string.spotify_exportify_receiving
                        state.savedCount > 0 &&
                            state.playlists.all { it.phase == SpotifyImportPhase.SAVED } ->
                            R.string.spotify_exportify_done
                        state.playlists.isNotEmpty() -> R.string.spotify_exportify_received
                        else -> R.string.spotify_exportify_instructions
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp),
                    )
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(
                            if (spotifyLogin) R.string.spotify_exportify_login_service
                            else R.string.spotify_exportify_service
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (browserLoading)
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                Box(Modifier.fillMaxWidth().height(380.dp)) {
                    ExportifyWebView(
                        modifier = Modifier.fillMaxSize(),
                        handleBackNavigation = false,
                        reloadRequest = reloadRequest,
                        changeAccountRequest = 0,
                        onAccountChangeInProgress = {},
                        onPageHostChanged = { spotifyLogin = it == "accounts.spotify.com" },
                        onPageError = { error ->
                            pageError = error
                            browserLoading = false
                            receiving = false
                        },
                        onExport = { file ->
                            receiving = false
                            browserError = false
                            viewModel.importFile(file)
                        },
                        onError = {
                            receiving = false
                            browserLoading = false
                            browserError = true
                        },
                        onLoading = { loading ->
                            browserLoading = loading
                            if (loading) {
                                receiving = false
                                pageError = null
                                browserError = false
                            }
                        },
                        onDownloadStarted = {
                            receiving = true
                            browserError = false
                        },
                    )
                    if (browserLoading && pageError == null) {
                        Column(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            Text(
                                stringResource(
                                    R.string.spotify_exportify_loading_page
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    pageError?.let { error ->
                        Column(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {
                            Text(
                                text =
                                    if (error is ExportifyPageError.Http)
                                        stringResource(
                                            R.string.spotify_exportify_http_error,
                                            error.statusCode,
                                        )
                                    else
                                        stringResource(R.string.spotify_exportify_connection_error),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            IrideFolioPrimaryButton(onClick = { reloadRequest++ }) {
                                Text(stringResource(R.string.spotify_exportify_reload))
                            }
                        }
                    }
                }
            }
            if (browserError)
                Text(
                    stringResource(R.string.spotify_exportify_browser_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            if (state.playlists.isEmpty())
                Text(
                    stringResource(R.string.spotify_exportify_login_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            state.error?.let { error ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            when (error) {
                                SpotifyImportError.INVALID_FILE ->
                                    R.string.spotify_exportify_invalid_file
                                SpotifyImportError.FILE_TOO_LARGE ->
                                    R.string.spotify_exportify_file_too_large
                                SpotifyImportError.READ_FAILED ->
                                    R.string.spotify_exportify_read_failed
                                SpotifyImportError.SAVE_FAILED ->
                                    R.string.spotify_exportify_save_failed
                            }
                        ),
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    IconButton(onClick = viewModel::clearError) {
                        Icon(
                            painterResource(R.drawable.close),
                            stringResource(R.string.spotify_exportify_dismiss),
                        )
                    }
                }
            }
            if (state.playlists.isNotEmpty()) {
                Text(
                    stringResource(R.string.spotify_exportify_loaded),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    stringResource(R.string.spotify_exportify_background_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IrideFolioPrimaryButton(
                    onClick = { confirmSave = true },
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(
                        stringResource(R.string.spotify_exportify_save),
                    )
                }
            }
            state.playlists.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { playlist ->
                        key(playlist.id) {
                            ImportPlaylistCard(
                                playlist,
                                Modifier.weight(1f),
                                !state.saving,
                                onRetry = { viewModel.retryFailures(playlist.id) },
                                onOpen = {
                                    playlist.savedPlaylistId?.let {
                                        navController.navigate("local_playlist/$it")
                                    }
                                },
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (confirmSave) {
        val unsaved = state.playlists.filter { it.phase == SpotifyImportPhase.READY }
        IrideFolioAlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text(stringResource(R.string.spotify_exportify_save)) },
            text = {
                Text(
                    stringResource(
                        R.string.spotify_exportify_confirm_save,
                        unsaved.size,
                        unsaved.sumOf { it.matched },
                        unsaved.sumOf { it.unmatched + it.failed },
                    )
                )
            },
            confirmButton = {
                IrideFolioPrimaryButton(
                    onClick = {
                        confirmSave = false
                        viewModel.saveReadyPlaylists()
                    },
                    enabled = state.canSave,
                ) {
                    Text(stringResource(R.string.spotify_exportify_save))
                }
            },
            dismissButton = {
                IrideFolioSecondaryButton(onClick = { confirmSave = false }) {
                    Text(stringResource(R.string.spotify_exportify_cancel))
                }
            },
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun ImportPlaylistCard(
    playlist: SpotifyImportPlaylist,
    modifier: Modifier,
    enabled: Boolean,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    val resolving =
        playlist.phase == SpotifyImportPhase.PENDING ||
            playlist.phase == SpotifyImportPhase.RESOLVING
    val shape = RoundedCornerShape(5.dp)
    Column(modifier) {
        GridItem(
            fillMaxWidth = true,
            title = {
                Text(
                    playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            },
            subtitle = {
                Column {
                    Text(
                        if (playlist.phase == SpotifyImportPhase.FAILED)
                            stringResource(R.string.spotify_exportify_invalid_csv)
                        else
                            stringResource(
                                R.string.spotify_exportify_match_count,
                                playlist.matched,
                                playlist.unmatched,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (playlist.failed > 0)
                        Text(
                            stringResource(
                                R.string.spotify_exportify_failed_count,
                                playlist.failed,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            },
            thumbnailContent = {
                if (playlist.thumbnails.size >= 4)
                    Column(Modifier.fillMaxSize().clip(shape)) {
                        playlist.thumbnails.take(4).chunked(2).forEach { row ->
                            Row(Modifier.weight(1f)) {
                                row.forEach { thumbnail ->
                                    AsyncImage(
                                        model = thumbnail,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                else if (resolving)
                    Box(
                        Modifier.fillMaxSize()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp,
                        )
                    }
                else
                    ItemThumbnail(
                        playlist.thumbnail,
                        false,
                        false,
                        shape,
                        hairlineBorder = true,
                        placeholder = {
                            Icon(
                                painterResource(R.drawable.playlist_play),
                                null,
                                Modifier.size(48.dp),
                            )
                        },
                    )
            },
        )
        if (resolving && playlist.total > 0)
            LinearProgressIndicator(
                progress = { playlist.processed.toFloat() / playlist.total },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        if (
            (playlist.failed > 0 || playlist.unmatched > 0) &&
                playlist.phase == SpotifyImportPhase.READY
        )
            TextButton(onClick = onRetry, enabled = enabled) {
                Text(stringResource(R.string.spotify_exportify_retry))
            }
        if (playlist.phase == SpotifyImportPhase.SAVED)
            TextButton(onClick = onOpen) {
                Icon(painterResource(R.drawable.check), null, Modifier.size(16.dp))
                Text(
                    stringResource(R.string.spotify_exportify_saved),
                    Modifier.padding(start = 4.dp),
                )
            }
    }
}
