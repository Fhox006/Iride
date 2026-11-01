package com.metrolist.music.ui.player


import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Stable
internal class IrideMp3QueueControls {
    var selectedMood by mutableStateOf(AUTOMIX_FILTER_ALL)
    val order = IrideMp3QueueOrder<Any>()
}

private const val MP3_HISTORY_DIVIDER = "history_divider"
private const val MP3_QUEUE_TITLE = "queue_title"

private sealed interface Mp3QueueEntry {
    val key: String

    data class Track(
        override val key: String,
        val metadata: MediaMetadata,
        val windowUid: Any? = null,
    ) : Mp3QueueEntry

    data object HistoryDivider : Mp3QueueEntry {
        override val key = MP3_HISTORY_DIVIDER
    }
}

// Resolve at the time of an action: timeline indices can change during a drag or network request.
private fun Player.indexOfWindow(uid: Any): Int {
    val window = Timeline.Window()
    for (index in 0 until currentTimeline.windowCount) {
        if (currentTimeline.getWindow(index, window).uid == uid) return index
    }
    return -1
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun IrideMp3Queue(
    controls: IrideMp3QueueControls,
    remaining: Long,
    isGuest: Boolean,
    sheetState: BottomSheetState,
    onNowPlaying: () -> Unit,
    textScale: Float = 1f,
) {
    val connection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val windows by connection.queueWindows.collectAsState()
    val currentIndex by connection.currentWindowIndex.collectAsState()
    val automaticRecommendations by connection.service.isAutomaticRecommendations.collectAsState()
    val seed by connection.service.radioSeedMetadata.collectAsState()
    val events by database.recentEventsPerSong(80).collectAsState(initial = emptyList())
    val history = remember(events) { events.map { it.song.toMediaMetadata() } }
    val upcoming = windows.drop((currentIndex + 1).coerceAtLeast(0))
    val displayed = remember { mutableStateListOf<Timeline.Window>() }
    val listState = rememberLazyListState()
    val moodsState = rememberLazyListState()
    val rows = remember { mutableStateListOf<Mp3QueueEntry>() }
    val historyOrder = remember { mutableListOf<String>() }
    val promotedHistory = remember { mutableStateMapOf<String, Any>() }
    var draggedWindowUid by remember { mutableStateOf<Any?>(null) }
    // ExoPlayer UIDs are opaque objects; LazyColumn keys must be saveable in an Android Bundle.
    val windowKeys = remember { mutableMapOf<Any, String>() }
    fun windowKey(uid: Any): String = windowKeys.getOrPut(uid) { "queued_${windowKeys.size}" }
    val genreSongs =
        remember(upcoming) {
            upcoming
                .map {
                    GenreSongInfo(
                        it.mediaItem.mediaId,
                        it.mediaItem.metadata?.title.orEmpty(),
                        it.mediaItem.metadata?.artists?.firstOrNull()?.name,
                    )
                }
                .distinctBy { it.id }
                .sortedBy { it.id }
        }
    val genres = rememberGenreFilter(genreSongs)
    val moods =
        remember(genres.sortedGenres) {
            (listOf(
                    AUTOMIX_FILTER_ALL,
                    AUTOMIX_FILTER_PARTY,
                    AUTOMIX_FILTER_WORKOUT,
                ) + genres.sortedGenres.take(2).map { it.uppercase() })
                .distinct()
        }
    val familiar =
        remember(history, seed) {
            (history.flatMap { it.artists } + seed?.artists.orEmpty()).map { it.name }.toSet()
        }
    val reorder =
        rememberReorderableLazyListState(listState) { from, to ->
            val source = rows.indexOfFirst { it.key == from.key }
            val target = rows.indexOfFirst { it.key == to.key }
            val track = rows.getOrNull(source) as? Mp3QueueEntry.Track
            if (!isGuest && track != null && target >= 0 && source != target) {
                val divider = rows.indexOf(Mp3QueueEntry.HistoryDivider)
                // Upcoming tracks stay in the queue; history tracks can cross its boundary.
                val destination =
                    if (track.windowUid != null) target.coerceAtMost(divider - 1)
                    else target
                if (destination >= 0 && source != destination) {
                    rows.add(destination, rows.removeAt(source))
                }
            }
        }
    fun finishDrag() {
        val queueRows =
            rows.takeWhile { it != Mp3QueueEntry.HistoryDivider }
                .filterIsInstance<Mp3QueueEntry.Track>()
        val previousKeys = displayed.filter { it.mediaItem.metadata != null }.map { it.uid }
        val hasHistoryInsertion = queueRows.any { it.windowUid == null }
        if (!isGuest && queueRows.map { it.windowUid } != previousKeys) {
            val player = connection.player
            if (player.shuffleModeEnabled) {
                // Preserve the visible playback order before switching to a manual order.
                val playbackOrder = connection.queueWindows.value.map { it.uid }
                playbackOrder.forEachIndexed { target, uid ->
                    val source = player.indexOfWindow(uid)
                    if (source >= 0 && source != target) player.moveMediaItem(source, target)
                }
                player.shuffleModeEnabled = false
            }
            var offset = 0
            queueRows.forEach { track ->
                val source = track.windowUid?.let(player::indexOfWindow)
                // Playback can advance while the list is being dragged.
                if (source != null && source <= player.currentMediaItemIndex) return@forEach
                val target =
                    (player.currentMediaItemIndex + 1 + offset).coerceAtMost(player.mediaItemCount)
                if (source == null) {
                    player.addMediaItem(target, track.metadata.toMediaItem())
                    val uid = player.currentTimeline.getWindow(target, Timeline.Window()).uid
                    // Keep the row key stable across the history-to-queue transition.
                    windowKeys[uid] = track.key
                    promotedHistory[track.metadata.id] = uid
                } else if (source != target) {
                    player.moveMediaItem(source, target)
                }
                offset++
            }
            val liveKeys =
                (player.currentMediaItemIndex + 1 until player.mediaItemCount).map {
                    player.currentTimeline.getWindow(it, Timeline.Window()).uid
                }
            if (hasHistoryInsertion) {
                controls.order.setManualOrder(liveKeys)
                // A history track must remain visible even when it does not match the active mood.
                controls.selectedMood = AUTOMIX_FILTER_ALL
            } else {
                // Keep the unfiltered order of untouched songs when reordering inside a mood.
                draggedWindowUid?.let { uid ->
                    val destination = queueRows.indexOfFirst { it.windowUid == uid }
                    previousKeys.getOrNull(destination)?.let { controls.order.move(uid, it) }
                }
            }
        }
        historyOrder.clear()
        historyOrder.addAll(
            rows.dropWhile { it != Mp3QueueEntry.HistoryDivider }
                .filterIsInstance<Mp3QueueEntry.Track>()
                .map { it.metadata.id }
        )
        draggedWindowUid = null
    }
    LaunchedEffect(
        windows,
        currentIndex,
        controls.selectedMood,
        genres.genreBySongId,
        familiar,
        history,
        promotedHistory.toMap(),
        reorder.isAnyItemDragging,
        isGuest,
    ) {
        if (reorder.isAnyItemDragging) return@LaunchedEffect
        val live =
            connection.queueWindows.value.drop(
                (connection.currentWindowIndex.value + 1).coerceAtLeast(0)
            )
        val keys = live.map { it.uid }
        if (controls.order.synchronize(keys) || keys.isEmpty())
            controls.selectedMood = AUTOMIX_FILTER_ALL
        val windowsByKey = live.associateBy { it.uid }
        val canonical = controls.order.ordered(keys).mapNotNull(windowsByKey::get)
        val mood = controls.selectedMood
        val matchingIds =
            filterAutomix(
                    canonical.map { it.mediaItem },
                    mood,
                    familiar,
                    genres.genreBySongId,
                    fallbackToAll = false,
                )
                .map { it.mediaId }
                .toSet()
        val preferred =
            canonical.filter { it.mediaItem.mediaId in matchingIds }.map { it.uid }.toSet()
        val ordered =
            controls.order.ordered(keys, if (mood == AUTOMIX_FILTER_ALL) null else preferred)
        // Reprioritize upcoming matches only; changing or clearing a filter never discards a song.
        if (!isGuest && ordered != keys) {
            ordered.forEachIndexed { offset, uid ->
                val source = connection.player.indexOfWindow(uid)
                val target = connection.player.currentMediaItemIndex + 1 + offset
                if (
                    source > connection.player.currentMediaItemIndex &&
                        target < connection.player.mediaItemCount &&
                        source != target
                ) {
                    connection.player.shuffleModeEnabled = false
                    connection.player.moveMediaItem(source, target)
                }
            }
        }
        val filtered =
            ordered.mapNotNull(windowsByKey::get).filter {
                mood == AUTOMIX_FILTER_ALL || it.uid in preferred
            }
        if (displayed.toList() != filtered) {
            displayed.clear()
            displayed.addAll(filtered)
        }
        val liveUids = live.map { it.uid }.toSet()
        promotedHistory.entries.removeAll { it.value !in liveUids }
        val currentId = connection.player.currentMediaItem?.mediaId
        val ranks = historyOrder.withIndex().associate { it.value to it.index }
        val historyRows =
            history.filterNot { it.id == currentId || it.id in promotedHistory }
                .sortedBy { ranks[it.id] ?: -1 }
                .map { Mp3QueueEntry.Track("history_${it.id}", it) }
        val updated =
            filtered.mapNotNull { window ->
                window.mediaItem.metadata?.let {
                    Mp3QueueEntry.Track(windowKey(window.uid), it, window.uid)
                }
            } + Mp3QueueEntry.HistoryDivider + historyRows
        if (rows.toList() != updated) {
            rows.clear()
            rows.addAll(updated)
        }
    }
    fun chooseMood(index: Int) {
        if (!isGuest) controls.selectedMood = moods[index.coerceIn(moods.indices)]
    }
    val gestureLock = LocalPlayerSheetGestureLock.current
    val density = LocalDensity.current
    val scrollConnection =
        remember(sheetState, listState, density, gestureLock, reorder) {
            Mp3QueueScrollConnection(sheetState, with(density) { 56.dp.toPx() }) {
                gestureLock.value || reorder.isAnyItemDragging
            }
        }
    DisposableEffect(scrollConnection) { onDispose { scrollConnection.cancel() } }
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        windows.getOrNull(currentIndex)?.mediaItem?.metadata?.let { current ->
            IrideMp3CurrentTrack(current, remaining, textScale, onClick = onNowPlaying)
        }
        Mp3QueueSectionTitle(stringResource(R.string.mp3_queue_and_history))
        LazyColumn(
            state = listState,
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .nestedScroll(scrollConnection)
                    .irideFolioGestures(listState)
                    .pointerInput(scrollConnection) {
                        awaitEachGesture {
                            awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            scrollConnection.reset()
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                        }
                    },
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item(key = "moods") {
                LazyRow(
                    state = moodsState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    overscrollEffect = null,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clipToBounds()
                            .rubberBandOverscroll(
                                androidx.compose.foundation.gestures.Orientation.Horizontal,
                                moodsState,
                            ),
                ) {
                    itemsIndexed(moods, key = { _, mood -> mood }) { index, mood ->
                        CompositionLocalProvider(
                            LocalArtworkContentColor provides Color.White
                        ) {
                            IrideFolioChoiceButton(
                                selected = controls.selectedMood == mood,
                                compact = true,
                                enabled = !isGuest,
                                onClick = { chooseMood(index) },
                            ) {
                                Text(mood, fontSize = 10.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                }
            }
            stickyHeader(key = MP3_QUEUE_TITLE) {
                Mp3QueueSectionTitle(
                    stringResource(
                        if (automaticRecommendations) R.string.mp3_queue_recommended
                        else R.string.mp3_up_next
                    )
                )
            }
            if (displayed.isEmpty())
                item {
                    Mp3EmptyText(
                        stringResource(
                            if (upcoming.isEmpty()) R.string.mp3_queue_empty
                            else if (
                                genres.isLoading &&
                                    controls.selectedMood in
                                        listOf(AUTOMIX_FILTER_PARTY, AUTOMIX_FILTER_WORKOUT)
                            )
                                R.string.mp3_queue_moods_loading
                            else R.string.mp3_queue_filter_empty
                        )
                    )
                }
            rows.forEach { entry ->
                if (entry == Mp3QueueEntry.HistoryDivider) {
                    stickyHeader(key = entry.key) {
                        ReorderableItem(reorder, key = entry.key) {
                            Mp3QueueSectionTitle(stringResource(R.string.history))
                        }
                    }
                    if (rows.lastOrNull() == Mp3QueueEntry.HistoryDivider) {
                        item(key = "history_empty") {
                            Mp3EmptyText(stringResource(R.string.mp3_history_empty))
                        }
                    }
                } else if (entry is Mp3QueueEntry.Track) {
                    item(key = entry.key) {
                        ReorderableItem(reorder, key = entry.key) { isDragging ->
                            val lock = LocalPlayerSheetGestureLock.current
                            val handle =
                                if (isGuest) Modifier
                                else
                                    Modifier.pointerInput(lock) {
                                            awaitEachGesture {
                                                awaitFirstDown(
                                                    requireUnconsumed = false,
                                                    pass = PointerEventPass.Initial,
                                                )
                                                lock.value = true
                                                try {
                                                    do {
                                                        val event =
                                                            awaitPointerEvent(PointerEventPass.Initial)
                                                    } while (event.changes.any { it.pressed })
                                                } finally {
                                                    lock.value = false
                                                }
                                            }
                                        }
                                        .draggableHandle(
                                            onDragStarted = { draggedWindowUid = entry.windowUid },
                                            onDragStopped = ::finishDrag,
                                        )
                            val dismiss = rememberSwipeToDismissBoxState()
                            LaunchedEffect(dismiss.settledValue, isGuest) {
                                // currentValue changes while dragging; settledValue waits for release.
                                if (
                                    dismiss.settledValue == SwipeToDismissBoxValue.StartToEnd &&
                                        !isGuest && entry.windowUid != null
                                ) {
                                    val index = connection.player.indexOfWindow(entry.windowUid)
                                    if (index > connection.player.currentMediaItemIndex)
                                        connection.player.removeMediaItem(index)
                                    dismiss.reset()
                                }
                            }
                            SwipeToDismissBox(
                                state = dismiss,
                                enableDismissFromStartToEnd =
                                    !isGuest && entry.windowUid != null && !reorder.isAnyItemDragging,
                                enableDismissFromEndToStart = false,
                                backgroundContent = {
                                    Box(
                                        Modifier.fillMaxSize()
                                            .drawWithContent {
                                                // Transparent rows must expose the action only in the
                                                // revealed strip.
                                                if (
                                                    dismiss.dismissDirection ==
                                                        SwipeToDismissBoxValue.StartToEnd
                                                ) {
                                                    val reveal =
                                                        abs(dismiss.requireOffset())
                                                            .coerceIn(0f, size.width)
                                                    if (layoutDirection == LayoutDirection.Ltr) {
                                                        clipRect(right = reveal) {
                                                            this@drawWithContent.drawContent()
                                                        }
                                                    } else {
                                                        clipRect(left = size.width - reveal) {
                                                            this@drawWithContent.drawContent()
                                                        }
                                                    }
                                                }
                                            }
                                            .clip(FolioShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.10f)),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.delete),
                                            stringResource(R.string.mp3_remove_queue),
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(start = 12.dp).size(24.dp),
                                        )
                                    }
                                },
                            ) {
                                val addToQueueLabel = stringResource(R.string.add_to_queue)
                                Mp3QueueRow(
                                    entry.metadata,
                                    modifier =
                                        Modifier.clip(FolioShape(8.dp))
                                            .background(
                                                if (isDragging) Color.White.copy(alpha = .10f)
                                                else Color.Transparent
                                            )
                                            .semantics(mergeDescendants = true) {
                                                if (!isGuest && entry.windowUid == null) {
                                                    customActions = listOf(
                                                        CustomAccessibilityAction(addToQueueLabel) {
                                                            connection.addToQueue(
                                                                entry.metadata.toMediaItem()
                                                            )
                                                            true
                                                        }
                                                    )
                                                }
                                            },
                                    handle = handle,
                                    showReorderHandle = !isGuest,
                                    enabled = !isGuest,
                                    onClick = {
                                        if (!isGuest) {
                                            if (entry.windowUid != null) {
                                                val index =
                                                    connection.player.indexOfWindow(entry.windowUid)
                                                if (index > connection.player.currentMediaItemIndex)
                                                    connection.playQueueItem(index)
                                            } else {
                                                connection.playNext(entry.metadata.toMediaItem())
                                                val player = connection.player
                                                val next = player.currentMediaItemIndex + 1
                                                val index =
                                                    if (
                                                        next < player.mediaItemCount &&
                                                            player.getMediaItemAt(next).mediaId ==
                                                                entry.metadata.id
                                                    ) next
                                                    else player.currentMediaItemIndex
                                                connection.playQueueItem(index)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private class Mp3QueueScrollConnection(
    private val sheet: BottomSheetState,
    private val threshold: Float,
    private val isLocked: () -> Boolean,
) : NestedScrollConnection {
    private var overflow = 0f
    private var direction = 0f
    private var movingSheet = false

    fun reset() {
        overflow = 0f
        direction = 0f
        movingSheet = false
    }

    fun cancel() {
        if (movingSheet && !sheet.isExpanded) sheet.expandSoft()
        reset()
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || isLocked() || !movingSheet) return Offset.Zero
        // Reversing the same drag raises the player again before returning control to the list.
        if (sheet.isExpanded && available.y.sign != direction) {
            reset()
            return Offset.Zero
        }
        sheet.dispatchRawDelta(available.y * direction)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || isLocked()) return Offset.Zero
        if (consumed.y != 0f || (available.y != 0f && available.y.sign != direction)) overflow = 0f
        if (available.y == 0f) return Offset.Zero
        direction = available.y.sign
        val previousExcess = (overflow - threshold).coerceAtLeast(0f)
        overflow += abs(available.y)
        val delta = (overflow - threshold).coerceAtLeast(0f) - previousExcess
        if (delta > 0f) {
            movingSheet = true
            sheet.dispatchRawDelta(delta)
        }
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val close = movingSheet && !sheet.isExpanded && !isLocked()
        reset()
        if (!close) return Velocity.Zero
        sheet.collapseSoft()
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        reset()
        return Velocity.Zero
    }
}

@Composable
private fun Mp3QueueSectionTitle(text: String) {
    Column(
        Modifier.fillMaxWidth().semantics { heading() }
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        Text(
            text,
            color = Color.White.copy(alpha = .65f),
            fontFamily = IBMPlexMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Mp3QueueRow(
    metadata: MediaMetadata,
    modifier: Modifier = Modifier,
    handle: Modifier = Modifier,
    showReorderHandle: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showReorderHandle) {
            Box(handle.size(width = 40.dp, height = 54.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.drag_handle),
                    stringResource(R.string.mp3_reorder),
                    tint = Color.White.copy(alpha = .4f),
                    modifier = Modifier.size(25.dp),
                )
            }
        }
        Row(
            Modifier.weight(1f).clip(FolioShape(7.dp))
                .clickable(enabled = enabled, onClick = onClick).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                irideFolioPlayerArtworkRequest(LocalContext.current, metadata.thumbnailUrl),
                null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.size(34.dp)
                        .irideArtworkOverlayBorder(
                            IrideSoftBorderWidth,
                            MaterialTheme.colorScheme.strokeItemSoft,
                            IrideMp3ArtworkShape,
                        )
                        .clip(IrideMp3ArtworkShape),
            )
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    metadata.title,
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    metadata.artists.joinToString { it.name },
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color.White.copy(alpha = .55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Mp3EmptyText(text: String) {
    Text(text, color = Color.White.copy(alpha = .5f), fontFamily = IBMPlexMonoFontFamily, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
}

@Composable
internal fun IrideMp3Lyrics(
    position: Long,
    controller: LyricsPillController,
    expansion: Float,
    controlsOpacity: Float,
    onOptions: () -> Unit,
    onKeepControlsVisible: (Boolean) -> Unit,
    onInteraction: () -> Unit,
) {
    val lyricsStyle by rememberEnumPreference(Mp3LyricsStyleKey, Mp3LyricsStyle.GRADIENT)
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val context = LocalContext.current
    val connection = LocalPlayerConnection.current
    val metadata by connection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val vocalState by
        connection?.service?.vocalSeparationState?.collectAsState()
            ?: remember {
                mutableStateOf(com.metrolist.music.playback.vocals.VocalSeparationState())
            }
    var showVocalPanel by remember(metadata?.id) { mutableStateOf(false) }
    val fullscreen = expansion == 1f
    LaunchedEffect(fullscreen) { if (!fullscreen) showVocalPanel = false }
    DisposableEffect(connection, showVocalPanel) {
        connection?.service?.setKaraokeControlsVisible(showVocalPanel)
        onDispose { connection?.service?.setKaraokeControlsVisible(false) }
    }
    val latestKeepControlsVisible by rememberUpdatedState(onKeepControlsVisible)
    SideEffect { onKeepControlsVisible(showVocalPanel || controller.isSelectionModeActive) }
    DisposableEffect(controller) {
        onDispose {
            latestKeepControlsVisible(false)
            controller.scrollAction = {}
        }
    }
    val reducedMotion = rememberReducedMotion()
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var incomingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val artworkProgress = remember { Animatable(0f) }
    LaunchedEffect(metadata?.id, metadata?.thumbnailUrl, lyricsStyle) {
        if (lyricsStyle != Mp3LyricsStyle.GRADIENT) return@LaunchedEffect
        incomingBitmap = null
        artworkProgress.snapTo(0f)
        val thumbnail = metadata?.thumbnailUrl
        val bitmap =
            if (thumbnail != null)
                withContext(Dispatchers.IO) {
                    context.imageLoader
                        .execute(
                            ImageRequest.Builder(context)
                                .data(thumbnail)
                                .size(100, 100)
                                .allowHardware(false)
                                .memoryCacheKey("gradient_${metadata?.id}")
                                .build()
                        )
                        .image
                        ?.toBitmap()
                }
            else null
        if (!reducedMotion && displayedBitmap != null && bitmap != null) {
            incomingBitmap = bitmap
            artworkProgress.animateTo(1f, tween(700))
        }
        displayedBitmap = bitmap
        incomingBitmap = null
        artworkProgress.snapTo(0f)
    }
    val backdrop = rememberFrostBackdrop(backgroundColor = IrideMp3BackgroundColor)
    val density = LocalDensity.current
    val cutoutTop = with(density) { WindowInsets.displayCutout.getTop(this).toDp() } * expansion
    val actionsOpacity = expansion * controlsOpacity
    CompositionLocalProvider(LocalFolioButtonBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize().background(IrideMp3BackgroundColor)) {
            Box(
                Modifier.fillMaxSize()
                    .recordFrostBackdrop(backdrop, enabled = (backdrop?.recordingRequests ?: 0) > 0)
            ) {
                if (lyricsStyle == Mp3LyricsStyle.GRADIENT)
                    BetterAnimatedGradientBackground(
                        thumbnail = displayedBitmap,
                        incomingThumbnail = incomingBitmap,
                        crossfadeProgress = artworkProgress.value,
                        grainOpacity = IrideFolio.LyricsGrainOpacity,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
            // Erase only the foreground text behind the actions, preserving the live background.
            Box(
                Modifier.fillMaxSize()
                    .padding(top = cutoutTop)
                    .graphicsLayer {
                        compositingStrategy =
                            androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        if (actionsOpacity > 0f)
                            drawRect(
                                brush =
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = actionsOpacity),
                                        .6f to Color.Black.copy(alpha = actionsOpacity),
                                        1f to Color.Transparent,
                                        endY = IrideFolio.PlayerOverlayHeight.toPx(),
                                    ),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstOut,
                            )
                    }
            ) {
                if (lyricsStyle == Mp3LyricsStyle.TV)
                    IrideTvLyrics(
                        position = position,
                        controller = controller,
                        viewModel = lyricsViewModel,
                        onOptions = onOptions,
                        modifier = Modifier.fillMaxSize(),
                    )
                else
                    Lyrics(
                        sliderPositionProvider = { null },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        showLyrics = true,
                        lyricsViewModel = lyricsViewModel,
                        showPills = false,
                        pillsController = controller,
                        isFullScreen = fullscreen,
                        onShowOptionsMenu = onOptions,
                        textScale = 1.15f - .15f * expansion,
                        fullness = { expansion },
                        morphing = { expansion > 0f && expansion < 1f },
                    )
            }
            if (expansion > 0f)
                Box(
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(
                            top = cutoutTop + IrideFolio.PlayerOverlayTopSpacing,
                            start = 12.dp,
                            end = 12.dp,
                        )
                ) {
                    Row(
                        Modifier.fillMaxWidth().graphicsLayer { alpha = actionsOpacity },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                onInteraction()
                                controller.selectionAction()
                            },
                            enabled =
                                fullscreen && controlsOpacity > .5f && controller.canSelectLyrics,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painterResource(R.drawable.share),
                                stringResource(R.string.share_lyrics),
                                tint =
                                    Color.White.copy(
                                        alpha = if (controller.canSelectLyrics) .75f else .25f
                                    ),
                                modifier = Modifier.size(IrideFolio.PlayerActionIconSize),
                            )
                        }
                        IconButton(
                            onClick = {
                                onInteraction()
                                controller.translateAction()
                            },
                            enabled = fullscreen && controlsOpacity > .5f,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painterResource(R.drawable.translate),
                                stringResource(R.string.mp3_translate),
                                tint = Color.White.copy(alpha = .75f),
                                modifier = Modifier.size(IrideFolio.PlayerActionIconSize),
                            )
                        }
                        IconButton(
                            onClick = {
                                onInteraction()
                                showVocalPanel = !showVocalPanel
                            },
                            enabled = fullscreen && controlsOpacity > .5f,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painterResource(R.drawable.mic),
                                stringResource(R.string.vocal_voice_level),
                                tint = Color.White.copy(alpha = .75f),
                                modifier = Modifier.size(IrideFolio.PlayerActionIconSize),
                            )
                        }
                    }
                    CompositionLocalProvider(
                        LocalArtworkContentColor provides Color.White,
                        LocalArtworkBackgroundColor provides IrideMp3BackgroundColor,
                    ) {
                        LyricsVocalPanel(
                            expanded = showVocalPanel,
                            state = vocalState,
                            onDismiss = {
                                showVocalPanel = false
                                onInteraction()
                            },
                            onLevelChange = { connection?.service?.setVoiceLevel(it) },
                            onPrepare = { connection?.service?.downloadKaraoke() },
                        )
                    }
                }
        }
    }
}
