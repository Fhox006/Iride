/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.ChoiceChipsRow
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideIconTile
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TimeTransfer
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val sArtists = viewModel.selectedArtists

    val toggleArtistSelection: (Artist) -> Unit = { artist ->
        if (sArtists.any { it.id == artist.id }) {
            sArtists.removeAll { it.id == artist.id }
        } else {
            sArtists.add(artist)
        }
    }

    val clearArtistSelection: () -> Unit = {
        sArtists.clear()
    }
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<Long>, Long>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                )
        ) {
            mutableStateListOf()
        }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.filteredSongs.collectAsState()
    val mostPlayedArtists by viewModel.filteredArtists.collectAsState()
    val mostPlayedAlbums by viewModel.filteredAlbums.collectAsState()
    val allArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedCategories by viewModel.mostPlayedCategories.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val weeklyMostPlaylist by viewModel.weeklyMostPlaylist.collectAsState()
    val monthlyMostPlaylist by viewModel.monthlyMostPlaylist.collectAsState()
    val recapPlaylists by viewModel.recapPlaylists.collectAsState()
    val currentDate = LocalDateTime.now()
    val orderedMostPlayedSongs =
        remember(mostPlayedSongsStats, mostPlayedSongs) {
            val songsById = mostPlayedSongs.associateBy { it.song.id }
            mostPlayedSongsStats.mapNotNull { statsSong -> songsById[statsSong.id] }
        }
    val mostPeriodPlaylists = listOfNotNull(weeklyMostPlaylist, monthlyMostPlaylist)
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val visibleStatsPlaylists =
        remember(mostPeriodPlaylists, recapPlaylists, isLoggedIn) {
            if (isLoggedIn) {
                (mostPeriodPlaylists + recapPlaylists).distinctBy { it.id }
            } else {
                mostPeriodPlaylists
            }
        }

    val coroutineScope = rememberCoroutineScope()
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val keyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    var showTimeTransfer by rememberSaveable { mutableStateOf(false) }
    var prevOptionOrdinal by rememberSaveable { mutableStateOf<OptionStats?>(null) }
    var prevIndexChips by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(showTimeTransfer) {
        if (showTimeTransfer) {
            if (prevOptionOrdinal == null) prevOptionOrdinal = selectedOption
            if (prevIndexChips == null) prevIndexChips = indexChips
            viewModel.selectedOption.value = OptionStats.CONTINUOUS
            viewModel.indexChips.value = StatPeriod.ALL.ordinal
        }
    }

    if (showTimeTransfer) {
        TimeTransfer(
            onDismiss = {
                showTimeTransfer = false
                prevOptionOrdinal?.let { viewModel.selectedOption.value = it }
                prevIndexChips?.let { viewModel.indexChips.value = it }

                prevOptionOrdinal = null
                prevIndexChips = null
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.syncMostPlaylistsIfNeeded()
    }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year ->
                                "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }
                .toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)) {
                    it.minusMonths(1)
                }
                .takeWhile {
                    it.isAfter(firstEvent?.event?.timestamp?.withDayOfMonth(1))
                }
                .mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }
                .toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate.plusYears(1).withDayOfYear(1).minusDays(1)) {
                    it.minusYears(1)
                }
                .takeWhile {
                    it.isAfter(firstEvent?.event?.timestamp)
                }
                .mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }
                .toList()
        } else {
            emptyList()
        }

    IridePageLayout(
        titleRes = R.string.stats,
        state = lazyListState,
        showTopBar = true,
        modifier =
            Modifier.background(
                if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
            ),
        bottomPadding =
            LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        isSearching = false
                        query = TextFieldValue()
                    } else navController.navigateUp()
                },
                onLongClick = { if (!isSearching) navController.backToMain() },
            ) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back))
            }
        },
        topBarContent =
            if (isSearching)
                ({
                    LibrarySearchHeader(
                        isSearchActive = true,
                        searchQuery = query.text,
                        onSearchQueryChange = { query = TextFieldValue(it) },
                        onBack = {
                            isSearching = false
                            query = TextFieldValue()
                        },
                        keyboardController = keyboardController,
                    ) {}
                })
            else null,
        actions = {
            if (!isSearching) {
                androidx.compose.material3.IconButton(onClick = { isSearching = true }) {
                    Icon(painterResource(R.drawable.search), stringResource(R.string.search))
                }
                IconButton(
                    onClick = { showTimeTransfer = true },
                    onLongClick = { showTimeTransfer = true },
                ) {
                    Icon(painterResource(R.drawable.sync), "Time Transfer")
                }
            }
        },
        overlay = {
            if (mostPlayedSongsStats.isNotEmpty() && !isSearching) {
                HideOnScrollFAB(
                    visible = true,
                    lazyListState = lazyListState,
                    icon = R.drawable.shuffle,
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.most_played_songs),
                                items =
                                    orderedMostPlayedSongs
                                        .map { it.toMediaMetadata().toMediaItem() }
                                        .shuffled(),
                            )
                        )
                    },
                )
            }
        },
    ) {
        val filteredArtists =
            allArtists
                .map { artistWrapper ->
                    Artist(
                        id = artistWrapper.artist.id,
                        name = artistWrapper.artist.name,
                    )
                }
                .filter { artist ->
                    artist.name.contains(query.text, ignoreCase = true)
                }

        if (!isSearching && sArtists.isEmpty()) {
            val hasEnoughData =
                mostPlayedArtists.isNotEmpty() &&
                    mostPlayedAlbums.isNotEmpty() &&
                    mostPlayedSongs.isNotEmpty()

            if (!hasEnoughData) {
                item(key = "empty_stats") {
                    StatsEmptyState(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.stats_not_enough_data),
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                item(key = "choice_chips") {
                    ChoiceChipsRow(
                        modifier = Modifier.padding(top = 0.dp),
                        chips =
                            when (selectedOption) {
                                OptionStats.WEEKS -> {
                                    weeklyDates
                                }

                                OptionStats.MONTHS -> {
                                    monthlyDates
                                }

                                OptionStats.YEARS -> {
                                    yearlyDates
                                }

                                OptionStats.CONTINUOUS -> {
                                    listOf(
                                        StatPeriod.WEEK_1.ordinal to
                                            pluralStringResource(
                                                R.plurals.n_week,
                                                1,
                                                1,
                                            ),
                                        StatPeriod.MONTH_1.ordinal to
                                            pluralStringResource(
                                                R.plurals.n_month,
                                                1,
                                                1,
                                            ),
                                        StatPeriod.MONTH_3.ordinal to
                                            pluralStringResource(
                                                R.plurals.n_month,
                                                3,
                                                3,
                                            ),
                                        StatPeriod.MONTH_6.ordinal to
                                            pluralStringResource(
                                                R.plurals.n_month,
                                                6,
                                                6,
                                            ),
                                        StatPeriod.YEAR_1.ordinal to
                                            pluralStringResource(
                                                R.plurals.n_year,
                                                1,
                                                1,
                                            ),
                                        StatPeriod.ALL.ordinal to
                                            stringResource(R.string.filter_all),
                                    )
                                }
                            },
                        options =
                            listOf(
                                OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                                OptionStats.WEEKS to stringResource(R.string.weeks),
                                OptionStats.MONTHS to stringResource(R.string.months),
                                OptionStats.YEARS to stringResource(R.string.years),
                            ),
                        selectedOption = selectedOption,
                        onSelectionChange = {
                            viewModel.selectedOption.value = it
                            viewModel.indexChips.value = 0
                        },
                        currentValue = indexChips,
                        onValueUpdate = { viewModel.indexChips.value = it },
                        useIrideStyle = true,
                    )
                }

                item {
                    SectionHeader(title = stringResource(R.string.top_artists))
                }
                item {
                    TopItemsCarousel(
                        items = mostPlayedArtists,
                        content = { index, artist ->
                            TopArtistCard(
                                rank = index + 1,
                                artist = artist.artist,
                                onClick = {
                                    navController.navigate("artist/${artist.id}")
                                },
                            )
                        },
                    )
                }

                item {
                    SectionHeader(title = stringResource(R.string.top_albums))
                }
                item {
                    TopItemsCarousel(
                        items = mostPlayedAlbums,
                        content = { index, album ->
                            TopAlbumCard(
                                rank = index + 1,
                                album = album.album,
                                artists = album.artists,
                                onClick = {
                                    navController.navigate("album/${album.id}")
                                },
                            )
                        },
                    )
                }

                item {
                    SectionHeader(title = stringResource(R.string.top_songs))
                }
                item {
                    TopItemsCarousel(
                        items = mostPlayedSongs,
                        content = { index, song ->
                            TopSongCard(
                                rank = index + 1,
                                song = song.song,
                                artists = song.artists,
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                endpoint = WatchEndpoint(song.id),
                                                preloadItem = song.toMediaMetadata(),
                                            )
                                        )
                                    }
                                },
                            )
                        },
                    )
                }

                if (mostPlayedCategories.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.top_genres))
                    }
                    item {
                        TopGenresSection(categories = mostPlayedCategories)
                    }
                }

                item {
                    val totalMinutes = mostPlayedSongsStats.sumOf { it.timeListened ?: 0L } / 60000
                    val uniqueArtistsCount = mostPlayedArtists.size
                    val uniqueAlbumsCount = mostPlayedAlbums.size
                    val totalSongsPlayed = mostPlayedSongsStats.sumOf { it.songCountListened }

                    StatsDataBox(
                        totalMinutes = totalMinutes,
                        totalSongs = totalSongsPlayed,
                        totalArtists = uniqueArtistsCount,
                        totalAlbums = uniqueAlbumsCount,
                    )
                }
            }
        }

        if (isSearching) {
            items(
                items =
                    allArtists.filter { artist ->
                        artist.artist.name.contains(query.text, ignoreCase = true)
                    },
                key = { "stats_artist_${it.id}" },
                contentType = { CONTENT_TYPE_ARTIST },
            ) { artist ->
                val uiArtist = Artist(name = artist.artist.name, id = artist.id)
                val isChecked = sArtists.any { it.id == uiArtist.id }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable {
                                toggleArtistSelection(uiArtist)
                            }
                            .padding(8.dp),
                ) {
                    ArtistListItem(
                        artist = artist,
                        modifier = Modifier.weight(1f),
                    )

                    Checkbox(
                        checked =
                            sArtists.contains(Artist(name = artist.artist.name, id = artist.id)),
                        onCheckedChange = {
                            toggleArtistSelection(uiArtist)
                        },
                    )
                }
            }
        }
        if (query.text.isNotEmpty() && filteredArtists.isEmpty()) {
            item(key = "no_result") {
                StatsEmptyState(
                    icon = R.drawable.search,
                    text = stringResource(R.string.no_results_found),
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
                    fontFamily = SpaceMonoFontFamily,
                    lineHeight = 22.sp,
                    letterSpacing = 0.sp,
                ),
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Start,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        )
    }
}
