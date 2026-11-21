package com.metrolist.music.ui.component

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.lyrics.LyricsDebugLog
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.utils.SyncStatus
import kotlin.math.roundToInt

@Composable
private fun syncStatusLabel(s: SyncStatus) = when (s) {
    SyncStatus.Idle -> stringResource(R.string.debug_sync_idle)
    SyncStatus.Syncing -> stringResource(R.string.debug_sync_in_progress)
    SyncStatus.Completed -> stringResource(R.string.debug_sync_complete)
    is SyncStatus.Error -> stringResource(R.string.debug_sync_error, s.message.take(25))
}

@Composable
fun DebugBubble(
    lyricsProvider: () -> LyricsEntity? = { null },
) {
    if (!BuildConfig.DEBUG) return
    // TODO: remove when re-enabling debug bubble
    return

    val context = LocalContext.current
    val syncUtils = LocalSyncUtils.current
    val syncState by syncUtils.syncState.collectAsState()

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(400f) }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(Float.MAX_VALUE)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .zIndex(Float.MAX_VALUE)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    onClick = { expanded = !expanded },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(8.dp, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.bug_report),
                            contentDescription = stringResource(R.string.debug_title),
                            tint = MaterialTheme.colorScheme.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Surface(
                        shape = FolioShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 280.dp)
                            .padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = stringResource(R.string.debug_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            TextButton(
                                onClick = {
                                    lyricsProvider()?.lyrics?.let { lyrics ->
                                        val plain = if (lyrics.startsWith("[")) {
                                            LyricsUtils.parseLyrics(lyrics).joinToString("\n") { it.text }
                                        } else lyrics
                                        val debugEntries = LyricsDebugLog.entries.value
                                        val debugBlock = if (debugEntries.isNotEmpty()) {
                                            "\n\n--- LYRICS API DEBUG LOG ---\n" +
                                            debugEntries.joinToString("\n") { "[${it.timeMs}ms] ${it.message}" }
                                        } else ""
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics+Debug", plain + debugBlock))
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                    }
                                    expanded = false
                                }
                            ) {
                                Icon(painterResource(R.drawable.content_copy), null, Modifier.size(16.dp))
                                Text(stringResource(R.string.debug_copy_lyrics_and_log), style = MaterialTheme.typography.labelMedium)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = stringResource(R.string.settings_section_sync_data),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            val statusLines = buildList {
                                add(stringResource(R.string.debug_sync_overall, syncStatusLabel(syncState.overallStatus)))
                                if (syncState.currentOperation.isNotEmpty())
                                    add(stringResource(R.string.debug_sync_operation, syncState.currentOperation))
                                add(stringResource(R.string.debug_sync_liked, syncStatusLabel(syncState.likedSongs)))
                                add(stringResource(R.string.debug_sync_library, syncStatusLabel(syncState.librarySongs)))
                                add(stringResource(R.string.debug_sync_uploaded, syncStatusLabel(syncState.uploadedSongs)))
                                add(stringResource(R.string.debug_sync_albums, syncStatusLabel(syncState.likedAlbums)))
                                add(stringResource(R.string.debug_sync_artists, syncStatusLabel(syncState.artists)))
                                add(stringResource(R.string.debug_sync_playlists, syncStatusLabel(syncState.playlists)))
                            }
                            statusLines.forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    syncUtils.performFullSync()
                                    expanded = false
                                }
                            ) {
                                Icon(painterResource(R.drawable.sync), null, Modifier.size(16.dp))
                                Text(stringResource(R.string.debug_force_sync), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
