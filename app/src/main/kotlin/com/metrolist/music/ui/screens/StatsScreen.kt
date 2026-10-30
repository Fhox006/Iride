/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IrideIconTile
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.ReverieArchiveDialog
import com.metrolist.music.ui.component.ReverieMonthSelector
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val months by viewModel.listeningMonths.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val reverieMonths by viewModel.reverieMonths.collectAsStateWithLifecycle()
    val firstFullMonth by viewModel.firstFullReverieMonth.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsStateWithLifecycle()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val lazyListState = rememberLazyListState()
    var showReverieArchive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.syncMostPlaylistsIfNeeded() }

    if (showReverieArchive) {
        ReverieArchiveDialog(
            months = reverieMonths,
            preferredMonth = selectedMonth,
            onDismiss = { showReverieArchive = false },
            onPlay = { month ->
                showReverieArchive = false
                navController.navigate("reverie/$month")
            },
        )
    }

    IridePageLayout(
        titleRes = R.string.stats,
        state = lazyListState,
        showTopBar = true,
        modifier = Modifier,
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        navigationIcon = { IridePageBackButton(navController) },
    ) {
        item(key = "monthly_selector") {
            ReverieMonthSelector(
                months = months,
                selectedMonth = selectedMonth,
                onSelect = viewModel::selectMonth,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (selectedMonth in reverieMonths) {
            item(key = "watch_monthly_reverie") {
                IrideFolioPrimaryButton(
                    onClick = { navController.navigate("reverie/$selectedMonth") },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.reverie_watch_selected,
                            selectedMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault()))),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
        if (selectedMonth !in reverieMonths) {
            item(key = "reverie_availability") {
                com.metrolist.music.ui.component.ReverieAvailabilityCard(
                    selectedMonth, firstFullMonth, Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        if (mostPlayedSongs.isEmpty()) {
            item(key = "empty_stats") {
                StatsEmptyState(
                    icon = R.drawable.music_note,
                    text = stringResource(R.string.stats_not_enough_data),
                )
            }
        } else {
            if (mostPlayedArtists.isNotEmpty()) {
                item { SectionHeader(title = stringResource(R.string.top_artists)) }
                item {
                    TopItemsCarousel(items = mostPlayedArtists) { index, artist ->
                        TopArtistCard(rank = index + 1, artist = artist.artist) {
                            navController.navigate("artist/${artist.id}")
                        }
                    }
                }
            }
            if (mostPlayedAlbums.isNotEmpty()) {
                item { SectionHeader(title = stringResource(R.string.top_albums)) }
                item {
                    TopItemsCarousel(items = mostPlayedAlbums) { index, album ->
                        TopAlbumCard(rank = index + 1, album = album.album, artists = album.artists) {
                            navController.navigate("album/${album.id}")
                        }
                    }
                }
            }
            item { SectionHeader(title = stringResource(R.string.top_songs)) }
            item {
                TopItemsCarousel(items = mostPlayedSongs) { index, song ->
                    TopSongCard(rank = index + 1, song = song.song, artists = song.artists) {
                        if (song.id == playerConnection?.mediaMetadata?.value?.id) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection?.playQueue(
                                YouTubeQueue(
                                    endpoint = WatchEndpoint(song.id),
                                    preloadItem = song.toMediaMetadata(),
                                ),
                            )
                        }
                    }
                }
            }
            item {
                StatsDataBox(
                    totalMinutes = mostPlayedSongsStats.sumOf { it.timeListened ?: 0L } / 60000,
                    totalSongs = mostPlayedSongsStats.sumOf { it.songCountListened },
                    totalArtists = mostPlayedArtists.size,
                    totalAlbums = mostPlayedAlbums.size,
                )
            }
        }
    }
}

enum class OptionStats {
    WEEKS,
    MONTHS,
    YEARS,
    CONTINUOUS,
}

@Composable
private fun StatsEmptyState(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        IrideIconTile(size = 48.dp, color = MaterialTheme.colorScheme.fillSubtle) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = IBMPlexMonoFontFamily,
                    lineHeight = 22.sp,
                    letterSpacing = 0.sp,
                ),
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Start,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        )
    }
}
