/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.QueueEditLockKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.component.UnderlinePill
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.menu.QueueMenu
import com.metrolist.music.ui.menu.SelectionMediaMetadataMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import kotlin.math.roundToInt
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.flow.first
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.Button


// A single reorderable list backs the "recently played", queue, and autoplay sections of
// InlineQueuePanel, so a history song can be dragged down into the real queue, and an
// autoplay suggestion can be dragged up into it, dropped straight at the position it lands on.
private sealed class QueueSlot(val key: Any) {
    class History(val metadata: MediaMetadata) : QueueSlot("inline_history_${metadata.id}")
    class QueueEntry(val window: Timeline.Window) : QueueSlot(window.uid.hashCode())
    class Automix(val item: androidx.media3.common.MediaItem) : QueueSlot("inline_automix_${item.mediaId}")
}

// Auto-Mix filter chips. ALL/POPULAR pass the list through unfiltered (the API already
// returns it in relevance order). DISCOVER/FAMILIAR check the artist against what's already
// in this listening session (history + queue), no network needed. PARTY/WORKOUT/the trailing
// genre chip match against real tags from GenreProvider (Last.fm/iTunes) - Iride has no
// genre data anywhere else, so this is the only honest source; there is no "workout" tag,
// so it's approximated from adjacent high-energy genre tags rather than faked.
// Not private: IrideMp3Player.kt's queue preview reuses the exact same filter set/logic
// instead of duplicating it, so the two UP NEXT surfaces can't drift.
const val AUTOMIX_FILTER_ALL = "ALL"
const val AUTOMIX_FILTER_POPULAR = "POPULAR"
const val AUTOMIX_FILTER_DISCOVER = "DISCOVER"
const val AUTOMIX_FILTER_FAMILIAR = "FAMILIAR"
const val AUTOMIX_FILTER_PARTY = "PARTY"
const val AUTOMIX_FILTER_WORKOUT = "WORKOUT"
const val AUTOMIX_FILTER_DEEP_CUTS = "DEEP CUTS"
val AUTOMIX_STATIC_FILTERS = listOf(
    AUTOMIX_FILTER_ALL, AUTOMIX_FILTER_POPULAR, AUTOMIX_FILTER_DISCOVER, AUTOMIX_FILTER_FAMILIAR,
    AUTOMIX_FILTER_PARTY, AUTOMIX_FILTER_WORKOUT, AUTOMIX_FILTER_DEEP_CUTS,
)
private val PARTY_TAG_KEYWORDS = listOf("dance", "electro", "edm", "house", "pop")
private val WORKOUT_TAG_KEYWORDS = listOf("rock", "metal", "hip", "rap", "edm")

fun filterAutomix(
    items: List<androidx.media3.common.MediaItem>,
    filter: String,
    familiarArtists: Set<String>,
    genreBySongId: Map<String, List<String>>,
): List<androidx.media3.common.MediaItem> {
    if (items.isEmpty() || filter == AUTOMIX_FILTER_ALL || filter == AUTOMIX_FILTER_POPULAR) return items
    val filtered = when (filter) {
        AUTOMIX_FILTER_DISCOVER -> items.filter { item ->
            item.metadata?.artists?.none { it.name in familiarArtists } == true
        }
        AUTOMIX_FILTER_FAMILIAR -> items.filter { item ->
            item.metadata?.artists?.any { it.name in familiarArtists } == true
        }
        AUTOMIX_FILTER_DEEP_CUTS -> items.drop(items.size / 2)
        AUTOMIX_FILTER_PARTY -> items.filter { item ->
            genreBySongId[item.mediaId]?.any { tag -> PARTY_TAG_KEYWORDS.any { tag.contains(it, true) } } == true
        }
        AUTOMIX_FILTER_WORKOUT -> items.filter { item ->
            genreBySongId[item.mediaId]?.any { tag -> WORKOUT_TAG_KEYWORDS.any { tag.contains(it, true) } } == true
        }
        // Trailing dynamic chip: exact match against a real detected genre tag.
        else -> items.filter { item -> genreBySongId[item.mediaId]?.any { it.equals(filter, true) } == true }
    }
    return filtered.ifEmpty { items }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InlineQueuePanel(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    openNonce: Int = 0,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val isRadio by playerConnection.service.isAutoMixQueueActive.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val castHandler = remember(playerConnection) {
        try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember { derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() } }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd,
    ) { playerConnection.service.sleepTimer.isActive }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val lazyListState = rememberLazyListState()

    // History shown inline above queue, draggable like a real queue item —
    // dropping one past the divider promotes it into the real player queue.
    var historyItems by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }
    var historyPageOffset by remember { mutableStateOf(0) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var hasMoreHistory by remember { mutableStateOf(true) }
    val historyPageSize = 20

    fun triggerHistoryLoad() {
        if (isLoadingHistory || !hasMoreHistory) return
        isLoadingHistory = true
        coroutineScope.launch {
            val currentSongId = playerConnection.mediaMetadata.value?.id
            val page = playerConnection.service.database.events().first()
                .distinctBy { it.song.id }
                .filter { it.song.id != currentSongId }
                .drop(historyPageOffset)
                .take(historyPageSize)
            if (page.size < historyPageSize) hasMoreHistory = false
            if (page.isNotEmpty()) {
                // Capture scroll position before prepending so we can restore it
                val firstIdx = lazyListState.firstVisibleItemIndex
                val firstOffset = lazyListState.firstVisibleItemScrollOffset
                val oldSize = historyItems.size
                // page is DESC (newest first); reverse so oldest is at top of list
                historyItems = page.reversed().map { it.song.toMediaMetadata() } + historyItems
                val added = historyItems.size - oldSize
                historyPageOffset += page.size
                // Compensate scroll so visible item doesn't jump
                if (added > 0 && firstIdx > 0) {
                    lazyListState.scrollToItem(firstIdx + added, firstOffset)
                }
            }
            isLoadingHistory = false
        }
    }

    // The moment the playing song changes (skip, auto-advance, jump-ahead...), drop the
    // one that just finished into the history list immediately rather than waiting for
    // the next DB reload — the new current song is always the first item below it.
    val currentMediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var lastKnownMetadata by remember { mutableStateOf(currentMediaMetadata) }
    LaunchedEffect(currentMediaMetadata?.id) {
        val previous = lastKnownMetadata
        if (previous != null && previous.id != currentMediaMetadata?.id && historyItems.none { it.id == previous.id }) {
            historyItems = historyItems + previous
        }
        lastKnownMetadata = currentMediaMetadata
    }

    // Auto-Mix filter chips. FAMILIAR = artist already heard this session (history + queue,
    // no network needed). PARTY/WORKOUT/the trailing chip match real genre tags resolved by
    // GenreProvider (Last.fm/iTunes) — the trailing chip's label IS the top detected genre
    // for the current Auto-Mix batch, so it genuinely reflects what's playing.
    val familiarArtistNames = remember(historyItems, queueWindows) {
        buildSet {
            historyItems.forEach { md -> md.artists.forEach { add(it.name) } }
            queueWindows.forEach { w -> w.mediaItem.metadata?.artists?.forEach { add(it.name) } }
        }
    }
    val automixGenreSongs = remember(automix) {
        automix.map {
            GenreSongInfo(id = it.mediaId, title = it.metadata?.title.orEmpty(), artist = it.metadata?.artists?.firstOrNull()?.name)
        }
    }
    // No cacheKey: automix batches change per song, so a persisted pill order would go
    // stale immediately — live-resorting for the current batch only is what's wanted here.
    val automixGenreFilter = rememberGenreFilter(automixGenreSongs)
    val dynamicGenreFilter = automixGenreFilter.sortedGenres.firstOrNull()?.uppercase()
    val automixFilters = remember(dynamicGenreFilter) {
        if (dynamicGenreFilter != null) AUTOMIX_STATIC_FILTERS + dynamicGenreFilter else AUTOMIX_STATIC_FILTERS
    }
    var selectedAutomixFilter by remember { mutableStateOf(AUTOMIX_FILTER_ALL) }
    val filteredAutomix = remember(automix, selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId) {
        filterAutomix(automix, selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId)
    }

    val dragScrollZone = LocalConfiguration.current.screenHeightDp.dp * 0.15f
    val dragAutoScroller = rememberScroller(
        scrollableState = lazyListState,
        // Auto-scroll while dragging a queue item near the edges was ~4x too fast;
        // slow it to ~25% of the library default (75% slower).
        pixelAmountProvider = { lazyListState.layoutInfo.viewportSize.height * 0.0125f },
    )
    // Single reorderable list backing both sections. History slots may move freely,
    // including past the divider into the queue zone (that's how a recent song gets
    // promoted); queue slots are clamped so they can never move above the divider.
    val combinedList = remember { mutableStateListOf<QueueSlot>() }
    var draggedSlotKey by remember { mutableStateOf<Any?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = WindowInsets.systemBars
            .add(WindowInsets(top = dragScrollZone, bottom = dragScrollZone))
            .asPaddingValues(),
        scroller = dragAutoScroller,
    ) { from, to ->
        draggedSlotKey = from.key
        val fromIdx = combinedList.indexOfFirst { it.key == from.key }
        val toIdxRaw = combinedList.indexOfFirst { it.key == to.key }
        if (fromIdx != -1 && toIdxRaw != -1) {
            val moving = combinedList[fromIdx]
            val firstQueueIdx = combinedList.indexOfFirst { it is QueueSlot.QueueEntry }
                .let { if (it == -1) combinedList.size else it }
            val automixStartIdx = combinedList.indexOfFirst { it is QueueSlot.Automix }
                .let { if (it == -1) combinedList.size else it }
            val toIdx = when (moving) {
                // Can move within history, or down into the queue zone (promotion) —
                // never past the autoplay divider.
                is QueueSlot.History -> toIdxRaw.coerceAtMost((automixStartIdx - 1).coerceAtLeast(0))
                // Clamped to the queue zone — can't cross into history above or autoplay below.
                is QueueSlot.QueueEntry -> toIdxRaw.coerceIn(firstQueueIdx, (automixStartIdx - 1).coerceAtLeast(firstQueueIdx))
                // Can move within autoplay, or up into the queue zone (promotion) —
                // never above the first queue entry.
                is QueueSlot.Automix -> toIdxRaw.coerceAtLeast(firstQueueIdx)
            }
            if (fromIdx != toIdx) combinedList.move(fromIdx, toIdx)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            val key = draggedSlotKey
            draggedSlotKey = null
            val idx = key?.let { k -> combinedList.indexOfFirst { it.key == k } } ?: -1
            if (idx != -1) {
                // Already-played windows before currentWindowIndex are hidden from the
                // visible queue section, so visible/relative positions need this offset
                // added back to land on the right absolute index in the real timeline.
                val hiddenBefore = currentWindowIndex.coerceAtLeast(0)
                when (val slot = combinedList[idx]) {
                    is QueueSlot.History -> {
                        val firstQueueIdx = combinedList.indexOfFirst { it is QueueSlot.QueueEntry }
                            .let { if (it == -1) combinedList.size else it }
                        if (idx < firstQueueIdx) {
                            // Still above the divider — cosmetic reorder of the history list.
                            historyItems = combinedList.filterIsInstance<QueueSlot.History>().map { it.metadata }
                        } else {
                            // Dropped into the queue zone — promote it to a real queue item.
                            val insertionIndex = combinedList.take(idx).count { it is QueueSlot.QueueEntry }
                            playerConnection.player.addMediaItem(insertionIndex + hiddenBefore, slot.metadata.toMediaItem())
                            historyItems = historyItems.filterNot { it.id == slot.metadata.id }
                        }
                    }

                    is QueueSlot.QueueEntry -> {
                        val safeFrom = queueWindows.indexOfFirst { it.uid == slot.window.uid }
                        val safeTo = combinedList
                            .filterIsInstance<QueueSlot.QueueEntry>()
                            .indexOfFirst { it.window.uid == slot.window.uid } + hiddenBefore
                        if (safeFrom != -1 && safeFrom != safeTo) {
                            if (!playerConnection.player.shuffleModeEnabled) {
                                playerConnection.player.moveMediaItem(safeFrom, safeTo)
                            } else {
                                playerConnection.player.setShuffleOrder(
                                    DefaultShuffleOrder(
                                        queueWindows.map { it.firstPeriodIndex }.toMutableList()
                                            .move(safeFrom, safeTo).toIntArray(),
                                        System.currentTimeMillis(),
                                    ),
                                )
                            }
                        }
                    }

                    is QueueSlot.Automix -> {
                        val automixStartIdxNow = combinedList.indexOfFirst { it is QueueSlot.Automix }
                            .let { if (it == -1) combinedList.size else it }
                        if (idx < automixStartIdxNow) {
                            // Dropped above the autoplay divider — promote it into the real queue.
                            val insertionIndex = combinedList.take(idx).count { it is QueueSlot.QueueEntry }
                            playerConnection.player.addMediaItem(insertionIndex + hiddenBefore, slot.item)
                            playerConnection.service.automixItems.value =
                                playerConnection.service.automixItems.value.filterNot { it.mediaId == slot.item.mediaId }
                        }
                        // Still within the autoplay section — order isn't persisted; combinedList
                        // resyncs from automixItems on the next recomposition anyway.
                    }
                }
            }
        }
    }

    LaunchedEffect(queueWindows, currentWindowIndex, historyItems, filteredAutomix) {
        if (!reorderableState.isAnyItemDragging) {
            // Hide already-played windows from the queue section — the current song is
            // always the first entry below the divider, never buried mid-list.
            val visibleQueueWindows = if (currentWindowIndex in queueWindows.indices) {
                queueWindows.drop(currentWindowIndex)
            } else {
                queueWindows
            }
            combinedList.apply {
                clear()
                addAll(historyItems.map { QueueSlot.History(it) })
                addAll(visibleQueueWindows.map { QueueSlot.QueueEntry(it) })
                addAll(filteredAutomix.map { QueueSlot.Automix(it) })
            }
        }
    }

    // Center the currently playing song every time the queue is (re-)opened — keyed on
    // openNonce rather than Unit so this fires again even if the panel stayed composed.
    // History is intentionally NOT eagerly preloaded here anymore: doing so used to prepend
    // items above the current song right after the initial scroll landed, shoving the
    // playing song down out of view instead of keeping it centered at the top.
    LaunchedEffect(openNonce) {
        // Layout: index 0 = spacer, then history items (none loaded yet on a fresh open),
        // then the divider row, then the CONTINUE LISTENING header, then the current song
        // as the first queue entry.
        val targetIndex = historyItems.size + 3
        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (itemInfo == null) {
            lazyListState.scrollToItem(maxOf(0, targetIndex - 3))
        }
        val updatedItem = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == targetIndex }
        val viewportCenter = lazyListState.layoutInfo.viewportSize.height / 2
        val scrollTarget = ((updatedItem?.offset ?: 0) - viewportCenter
            + (updatedItem?.size ?: 0) / 2).toFloat()
        lazyListState.animateScrollBy(
            value = scrollTarget,
            animationSpec = tween(
                durationMillis = 400,
                easing = EaseInOut,
            ),
        )
    }

    // Load history when user scrolls near top of list
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { index ->
                if (index <= 3) triggerHistoryLoad()
            }
    }

    InlinePlayerPageFrame(
        modifier = modifier,
        pills = {
            PlayerPill(
                icon = R.drawable.radio,
                isActive = isRadio,
                enabled = !isListenTogetherGuest,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    // Radio is a shortcut into Auto-Mix, not a full queue replace: it must
                    // never restart or change the song that's currently playing.
                    val currentIndex = playerConnection.player.currentMediaItemIndex
                    val currentMetadata = playerConnection.player.getMediaItemAt(currentIndex).metadata
                    if (currentMetadata != null) {
                        selectedAutomixFilter = AUTOMIX_FILTER_ALL
                        coroutineScope.launch {
                            playerConnection.regenerateAutomix(currentMetadata)
                            delay(80)
                            val automixIdx = combinedList.indexOfFirst { it is QueueSlot.Automix }
                            if (automixIdx != -1) {
                                lazyListState.animateScrollToItem(automixIdx)
                            }
                        }
                    }
                },
            )
            PlayerPill(
                icon = R.drawable.shuffle,
                isActive = shuffleModeEnabled,
                enabled = !isListenTogetherGuest,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                        )
                    }.invokeOnCompletion {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    }
                },
            )
            PlayerPill(
                icon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                    else -> R.drawable.repeat
                },
                isActive = repeatMode != Player.REPEAT_MODE_OFF,
                enabled = !isListenTogetherGuest,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                modifier = Modifier.weight(1f),
                onClick = { playerConnection.player.toggleRepeatMode() },
            )
        },
        content = {
        // Persistent title, stays put while History/Continue Listening/Auto-Mix scroll
        // underneath it — dragging down toward History never scrolls the panel's own name away.
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.queue).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = textButtonColor,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            Box(modifier = Modifier.weight(1f)) {

        if (showSleepTimerDialog) {
            ActionPromptDialog(
                titleBar = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.sleep_timer),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                },
                onDismiss = { showSleepTimerDialog = false },
                onConfirm = {
                    showSleepTimerDialog = false
                    playerConnection.service.sleepTimer.start(
                        minute = sleepTimerValue.roundToInt(),
                        stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                        fadeOut = sleepTimerFadeOut,
                    )
                },
                onCancel = { showSleepTimerDialog = false },
                onReset = { sleepTimerValue = sleepTimerDefault },
                content = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt(),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isAtDefault) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            context.dataStore.edit { settings ->
                                                settings[SleepTimerDefaultKey] = sleepTimerValue
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) { Text(stringResource(R.string.set_as_default)) }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            context.dataStore.edit { settings ->
                                                settings[SleepTimerDefaultKey] = sleepTimerValue
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                ) { Text(stringResource(R.string.set_as_default)) }
                            }
                            OutlinedButton(
                                onClick = {
                                    showSleepTimerDialog = false
                                    playerConnection.service.sleepTimer.start(minute = -1)
                                },
                            ) { Text(stringResource(R.string.end_of_song)) }
                        }
                    }
                },
            )
        }

        // Queue list
        val defaultFling = ScrollableDefaults.flingBehavior()
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .add(WindowInsets(bottom = 72.dp))
                    .asPaddingValues(),
                flingBehavior = remember(defaultFling) {
                    object : androidx.compose.foundation.gestures.FlingBehavior {
                        override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(initialVelocity: Float): Float {
                            return with(defaultFling) {
                                performFling(initialVelocity * 0.38f)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "inline_spacer_top") { Spacer(Modifier.height(4.dp)) }

                // Recently played + queue — one reorderable list. The divider marks the
                // boundary and is (re-)rendered at whatever the current split is.
                val dividerRow: @Composable () -> Unit = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = textButtonColor.copy(alpha = 0.15f),
                        )
                        Text(
                            text = when {
                                isLoadingHistory -> "• • •"
                                historyItems.isEmpty() -> stringResource(R.string.queue_scroll_for_history)
                                hasMoreHistory -> stringResource(R.string.queue_recently_played_more)
                                else -> stringResource(R.string.queue_recently_played)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = textButtonColor.copy(alpha = 0.38f),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = textButtonColor.copy(alpha = 0.15f),
                        )
                    }
                }

                val firstQueueSlotIndex = combinedList.indexOfFirst { it is QueueSlot.QueueEntry }
                    .let { if (it == -1) combinedList.size else it }
                val automixStartIndex = combinedList.indexOfFirst { it is QueueSlot.Automix }
                    .let { if (it == -1) combinedList.size else it }

                combinedList.forEachIndexed { slotIdx, slot ->
                    if (slotIdx == firstQueueSlotIndex) {
                        item(key = "queue_section_divider") { dividerRow() }
                        item(key = "continue_listening_header") {
                            Text(
                                text = stringResource(R.string.queue_continue_listening).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = textButtonColor.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                                    .animateItem(),
                            )
                        }
                    }
                    if (slotIdx == automixStartIndex) {
                        if (automix.isNotEmpty()) {
                            item(key = "automix_filters") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .animateItem(),
                                ) {
                                    automixFilters.forEach { filter ->
                                        UnderlinePill(
                                            text = filter,
                                            selected = selectedAutomixFilter == filter,
                                            onClick = { selectedAutomixFilter = filter },
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "inline_automix_divider") {
                            Column(modifier = Modifier.animateItem()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                )
                                Text(
                                    text = stringResource(R.string.queue_autoplay).uppercase(),
                                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = textButtonColor.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }

                    item(key = slot.key) {
                        ReorderableItem(
                            state = reorderableState,
                            key = slot.key,
                        ) {
                            when (slot) {
                                is QueueSlot.History -> {
                                    val historyItem = slot.metadata
                                    MediaMetadataListItem(
                                        mediaMetadata = historyItem,
                                        isActive = false,
                                        isPlaying = false,
                                        trailingContent = {
                                            if (!isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            QueueMenu(
                                                                mediaMetadata = historyItem,
                                                                navController = navController,
                                                                playerBottomSheetState = playerBottomSheetState,
                                                                onShowDetailsDialog = {
                                                                    bottomSheetPageState.show { ShowMediaInfo(historyItem.id) }
                                                                },
                                                                onShowSleepTimerDialog = { showSleepTimerDialog = true },
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .combinedClickable(
                                                onClick = {
                                                    menuState.show {
                                                        QueueMenu(
                                                            mediaMetadata = historyItem,
                                                            navController = navController,
                                                            playerBottomSheetState = playerBottomSheetState,
                                                            onShowDetailsDialog = {
                                                                bottomSheetPageState.show { ShowMediaInfo(historyItem.id) }
                                                            },
                                                            onShowSleepTimerDialog = { showSleepTimerDialog = true },
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            ),
                                    )
                                }

                                is QueueSlot.QueueEntry -> {
                                    val window = slot.window
                                    val currentItem by rememberUpdatedState(window)
                                    val isActive = window.uid == currentPlayingUid
                                    val dismissBoxState = rememberSwipeToDismissBoxState(
                                        positionalThreshold = { totalDistance -> totalDistance },
                                    )
                                    var processedDismiss by remember { mutableStateOf(false) }
                                    val removedSongMsg = stringResource(
                                        R.string.removed_song_from_playlist,
                                        currentItem.mediaItem.metadata?.title ?: "",
                                    )
                                    val undoStr = stringResource(R.string.undo)

                                    LaunchedEffect(dismissBoxState.currentValue) {
                                        val dv = dismissBoxState.currentValue
                                        if (!processedDismiss && !isListenTogetherGuest && (
                                                    dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart
                                                    )) {
                                            processedDismiss = true
                                            playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                            dismissJob?.cancel()
                                            dismissJob = coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = removedSongMsg,
                                                    actionLabel = undoStr,
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    playerConnection.player.addMediaItem(currentItem.mediaItem)
                                                    playerConnection.player.moveMediaItem(
                                                        playerConnection.player.mediaItemCount - 1,
                                                        currentItem.firstPeriodIndex,
                                                    )
                                                }
                                            }
                                        }
                                        if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                                    }

                                    SwipeToDismissBox(
                                        state = dismissBoxState,
                                        backgroundContent = {},
                                    ) {
                                        MediaMetadataListItem(
                                            mediaMetadata = window.mediaItem.metadata!!,
                                            isActive = isActive,
                                            isPlaying = isPlaying && isActive,
                                            trailingContent = {
                                                if (!isListenTogetherGuest) {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                QueueMenu(
                                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                                    navController = navController,
                                                                    playerBottomSheetState = playerBottomSheetState,
                                                                    onShowDetailsDialog = {
                                                                        window.mediaItem.mediaId.let {
                                                                            bottomSheetPageState.show { ShowMediaInfo(it) }
                                                                        }
                                                                    },
                                                                    onShowSleepTimerDialog = { showSleepTimerDialog = true },
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.more_vert),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier.draggableHandle(),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.drag_handle),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem()
                                                .combinedClickable(
                                                    onClick = {
                                                        if (!isListenTogetherGuest) {
                                                            if (isActive) {
                                                                if (isCasting) {
                                                                    if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                                                } else {
                                                                    playerConnection.togglePlayPause()
                                                                }
                                                            } else {
                                                                if (isCasting) {
                                                                    val navigated = castHandler?.navigateToMediaIfInQueue(window.mediaItem.mediaId) ?: false
                                                                    if (!navigated) playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                } else {
                                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                    playerConnection.player.playWhenReady = true
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                ),
                                        )
                                    }
                                }

                                is QueueSlot.Automix -> {
                                    val item = slot.item
                                    MediaMetadataListItem(
                                        mediaMetadata = item.metadata!!,
                                        isActive = false,
                                        isPlaying = false,
                                        trailingContent = {
                                            if (!isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            QueueMenu(
                                                                mediaMetadata = item.metadata!!,
                                                                navController = navController,
                                                                playerBottomSheetState = playerBottomSheetState,
                                                                onShowDetailsDialog = {
                                                                    item.mediaId.let {
                                                                        bottomSheetPageState.show { ShowMediaInfo(it) }
                                                                    }
                                                                },
                                                                onShowSleepTimerDialog = { showSleepTimerDialog = true },
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                if (firstQueueSlotIndex == combinedList.size) {
                    item(key = "queue_section_divider") { dividerRow() }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            )
            }
        }
        },
    )
}

