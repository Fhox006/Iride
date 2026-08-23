/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.MediaInfo
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val (advancedMode) = rememberPreference(AdvancedModeKey, defaultValue = false)

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }

    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }

    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(
                windowInsets
                    .asPaddingValues()
            )
            .fillMaxSize()
    ) {
        if (info != null && song != null) {
            item(contentType = "MediaDetails") {
                Column {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    @Composable
                    fun copyable(label: String, text: String?, icon: Int): Material3SettingsItem {
                        val displayText = text ?: stringResource(R.string.unknown)
                        return Material3SettingsItem(
                            title = { Text(label) },
                            description = { Text(displayText) },
                            icon = painterResource(icon),
                            onClick = {
                                cm.setPrimaryClip(ClipData.newPlainText("text", displayText))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    val generalItems = listOf(
                        copyable(stringResource(R.string.song_title), song?.title, R.drawable.music_note),
                        copyable(stringResource(R.string.song_artists), song?.artists?.joinToString { it.name }, R.drawable.person),
                        copyable(stringResource(R.string.album), song?.song?.albumName, R.drawable.album),
                        copyable(stringResource(R.string.duration), song?.song?.duration?.let { makeTimeString(it * 1000L) }, R.drawable.timer),
                    )

                    Material3SettingsGroup(
                        title = stringResource(R.string.general),
                        items = generalItems
                    )

                    val fileSize = currentFormat?.contentLength?.let { Formatter.formatShortFileSize(context, it) }
                    val bitrate = currentFormat?.bitrate?.let { "${it / 1000} Kbps" }
                    if (fileSize != null || bitrate != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(bitrate, fileSize).joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 0.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val descriptionText = info?.description ?: stringResource(R.string.unknown)
                    Material3SettingsGroup(
                        title = stringResource(R.string.description),
                        items = listOf(
                            Material3SettingsItem(
                                title = {},
                                description = { Text(descriptionText) },
                                onClick = {
                                    cm.setPrimaryClip(ClipData.newPlainText("text", descriptionText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    )

                    if (advancedMode && currentFormat != null) {
                        Spacer(Modifier.height(8.dp))

                        val advancedItems = listOf(
                            copyable(stringResource(R.string.media_id), song?.id, R.drawable.media3_icon_bookmark_filled),
                            copyable(stringResource(R.string.views), info?.viewCount?.let(::numberFormatter), R.drawable.media3_icon_feed),
                            copyable(stringResource(R.string.likes), info?.like?.let(::numberFormatter), R.drawable.media3_icon_thumb_up_unfilled),
                            copyable(stringResource(R.string.dislikes), info?.dislike?.let(::numberFormatter), R.drawable.media3_icon_thumb_down_unfilled),
                            copyable("Itag", currentFormat?.itag?.toString(), R.drawable.key),
                            copyable(stringResource(R.string.mime_type), currentFormat?.mimeType, R.drawable.info),
                            copyable(stringResource(R.string.codecs), currentFormat?.codecs, R.drawable.radio),
                            copyable(stringResource(R.string.bitrate), bitrate, R.drawable.gradient),
                            copyable(stringResource(R.string.sample_rate), currentFormat?.sampleRate?.let { "$it Hz" }, R.drawable.contrast),
                            copyable(stringResource(R.string.loudness), currentFormat?.loudnessDb?.let { "$it dB" }, R.drawable.volume_up),
                            copyable(stringResource(R.string.volume), if (playerConnection != null) "${(playerConnection.player.volume * 100).toInt()}%" else null, R.drawable.volume_mute),
                            copyable(stringResource(R.string.file_size), fileSize, R.drawable.content_copy),
                        )

                        Material3SettingsGroup(
                            title = stringResource(R.string.information),
                            items = advancedItems
                        )
                    }
                }
            }
        } else {
            item(contentType = "MediaInfoLoader") {
                ShimmerHost {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        TextPlaceholder()
                    }
                }
            }
        }
    }
}
