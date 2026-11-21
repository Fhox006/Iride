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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
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
import com.metrolist.music.extensions.isManualQueueItem
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.extensions.withManualQueueItem
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
    val order = IrideMp3QueueOrder<Any>()
    val manualQueueOverrides = mutableStateMapOf<Any, Boolean>()
}

private const val MP3_HISTORY_DIVIDER = "history_divider"
private const val MP3_UP_NEXT_DIVIDER = "up_next_divider"

private sealed interface Mp3QueueEntry {
    val key: String

    data class Track(
        override val key: String,
        val metadata: MediaMetadata,
        val windowUid: Any? = null,
        val manualQueue: Boolean = false,
    ) : Mp3QueueEntry

    data object HistoryDivider : Mp3QueueEntry {
        override val key = MP3_HISTORY_DIVIDER
    }

    data object UpNextDivider : Mp3QueueEntry {
        override val key = MP3_UP_NEXT_DIVIDER
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
    val shuffleModeEnabled by connection.shuffleModeEnabled.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val events by remember(database) { database.recentEventsPerSong(80) }.collectAsState(initial = null)
    val history = remember(events) { events.orEmpty().map { it.song.toMediaMetadata() } }
    val displayed = remember { mutableStateListOf<Timeline.Window>() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 2)
    var positionedQueue by remember { mutableStateOf(false) }
    val queueItemHeights = remember { mutableStateMapOf<String, Int>() }
    val rows = remember { mutableStateListOf<Mp3QueueEntry>(Mp3QueueEntry.HistoryDivider) }
    val historyOrder = remember { mutableListOf<String>() }
    val promotedHistory = remember { mutableStateMapOf<String, Any>() }
    val manualQueueOverrides = controls.manualQueueOverrides.toMap()
    // ExoPlayer UIDs are opaque objects; LazyColumn keys must be saveable in an Android Bundle.
    val windowKeys = remember { mutableMapOf<Any, String>() }
    fun windowKey(uid: Any): String = windowKeys.getOrPut(uid) { "queued_${windowKeys.size}" }
    val reorder =
        rememberReorderableLazyListState(listState) { from, to ->
            val source = rows.indexOfFirst { it.key == from.key }
            val target = rows.indexOfFirst { it.key == to.key }
            val track = rows.getOrNull(source) as? Mp3QueueEntry.Track
            if (!isGuest && track != null && target >= 0 && source != target) {
                val divider = rows.indexOf(Mp3QueueEntry.HistoryDivider)
                // Queue tracks stay below History; history tracks can be promoted into the queue.
                val destination =
                    if (track.windowUid != null) target.coerceAtLeast(divider + 1)
                    else target
                if (destination >= 0 && source != destination) {
                    rows.add(destination, rows.removeAt(source))
                }
            }
        }
    fun finishDrag() {
        val afterHistory = rows.dropWhile { it != Mp3QueueEntry.HistoryDivider }.drop(1)
        val queueRows = afterHistory.filterIsInstance<Mp3QueueEntry.Track>()
        val manualKeys =
            afterHistory.takeWhile { it != Mp3QueueEntry.UpNextDivider }
                .filterIsInstance<Mp3QueueEntry.Track>()
                .map { it.key }
                .toSet()
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
            val insertedUids = mutableMapOf<String, Any>()
            var offset = 0
            queueRows.forEach { track ->
                val source = track.windowUid?.let(player::indexOfWindow)
                // Playback can advance while the list is being dragged.
                if (source != null && source <= player.currentMediaItemIndex) return@forEach
                val target =
                    (player.currentMediaItemIndex + 1 + offset).coerceAtMost(player.mediaItemCount)
                if (source == null) {
                    player.addMediaItem(
                        target,
                        track.metadata.toMediaItem().withManualQueueItem(track.key in manualKeys),
                    )
                    val uid = player.currentTimeline.getWindow(target, Timeline.Window()).uid
                    // Keep the row key stable across the history-to-queue transition.
                    windowKeys[uid] = track.key
                    promotedHistory[track.metadata.id] = uid
                    insertedUids[track.key] = uid
                } else if (source != target) {
                    player.moveMediaItem(source, target)
                }
                offset++
            }
            val liveKeys =
                (player.currentMediaItemIndex + 1 until player.mediaItemCount).map {
                    player.currentTimeline.getWindow(it, Timeline.Window()).uid
                }
            val manualUids =
                manualKeys.mapNotNull { key ->
                    queueRows.firstOrNull { it.key == key }?.windowUid ?: insertedUids[key]
                }.toSet()
            controls.manualQueueOverrides.keys.retainAll(liveKeys.toSet())
            liveKeys.forEach { uid -> controls.manualQueueOverrides[uid] = uid in manualUids }
            if (hasHistoryInsertion || queueRows.map { it.windowUid } != previousKeys) {
                controls.order.setManualOrder(liveKeys)
            }
        }
        historyOrder.clear()
        historyOrder.addAll(
            rows.takeWhile { it != Mp3QueueEntry.HistoryDivider }
                .filterIsInstance<Mp3QueueEntry.Track>()
                .map { it.metadata.id }
        )
    }
    LaunchedEffect(
        windows,
        currentIndex,
        shuffleModeEnabled,
        manualQueueOverrides,
        history,
        events != null,
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
        controls.order.synchronize(keys)
        val windowsByKey = live.associateBy { it.uid }
        val manualUids =
            live.filter { window ->
                manualQueueOverrides[window.uid] ?: window.mediaItem.isManualQueueItem
            }.map { it.uid }.toSet()
        val ordered = controls.order.ordered(keys, preferred = manualUids)
        val shufflePlaybackOrder = connection.player.shuffleModeEnabled
        if (!isGuest && !shufflePlaybackOrder && ordered != keys) {
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
        val visibleOrder = if (shufflePlaybackOrder) keys else ordered
        val orderedWindows = visibleOrder.mapNotNull(windowsByKey::get)
        if (displayed.toList() != orderedWindows) {
            displayed.clear()
            displayed.addAll(orderedWindows)
        }
        val liveUids = live.map { it.uid }.toSet()
        controls.manualQueueOverrides.keys.retainAll(liveUids)
        promotedHistory.entries.removeAll { it.value !in liveUids }
        val currentId = connection.player.currentMediaItem?.mediaId
        val historyById = history.filterNot { it.id == currentId || it.id in promotedHistory }
            .associateBy { it.id }
        val historyRows =
            queueHistoryOldestFirst(historyById.keys.toList(), historyOrder)
                .map { Mp3QueueEntry.Track("history_$it", historyById.getValue(it)) }
        val manualQueueRows =
            orderedWindows.filter { window ->
                manualQueueOverrides[window.uid] ?: window.mediaItem.isManualQueueItem
            }.mapNotNull { window ->
                window.mediaItem.metadata?.let {
                    Mp3QueueEntry.Track(windowKey(window.uid), it, window.uid, manualQueue = true)
                }
            }
        val upNextRows =
            orderedWindows.filterNot { window ->
                manualQueueOverrides[window.uid] ?: window.mediaItem.isManualQueueItem
            }.mapNotNull { window ->
                window.mediaItem.metadata?.let {
                    Mp3QueueEntry.Track(windowKey(window.uid), it, window.uid)
                }
            }
        val updated =
            historyRows + Mp3QueueEntry.HistoryDivider + manualQueueRows +
                (if (upNextRows.isNotEmpty()) listOf(Mp3QueueEntry.UpNextDivider) else emptyList()) +
                upNextRows
        if (rows.toList() != updated) {
            rows.clear()
            rows.addAll(updated)
        }
        queueItemHeights.keys.retainAll(updated.map { it.key }.toSet())
        if (!positionedQueue && events != null) {
            // Request the queue heading before measurement, without animating through history.
            // Later database emissions preserve this stable key as history is inserted above it.
            val historyPrefixCount = if (historyRows.isEmpty()) 2 else 1
            listState.requestScrollToItem(historyRows.size + historyPrefixCount)
            positionedQueue = true
        }
    }
    fun clearManualQueue() {
        if (isGuest) return
        val player = connection.player
        rows.dropWhile { it != Mp3QueueEntry.HistoryDivider }.drop(1)
            .takeWhile { it != Mp3QueueEntry.UpNextDivider }
            .filterIsInstance<Mp3QueueEntry.Track>()
            .mapNotNull { it.windowUid }
            .forEach { uid ->
                val index = player.indexOfWindow(uid)
                if (index > player.currentMediaItemIndex) player.removeMediaItem(index)
                controls.manualQueueOverrides.remove(uid)
            }
    }
    val queueRows = rows.dropWhile { it != Mp3QueueEntry.HistoryDivider }.drop(1)
    val manualQueueCount =
        queueRows.takeWhile { it != Mp3QueueEntry.UpNextDivider }.count { it is Mp3QueueEntry.Track }
    val upNextCount =
        queueRows.dropWhile { it != Mp3QueueEntry.UpNextDivider }.drop(1)
            .count { it is Mp3QueueEntry.Track }
    val hasHistory = rows.takeWhile { it != Mp3QueueEntry.HistoryDivider }
        .any { it is Mp3QueueEntry.Track }
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
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.mp3_queue_title),
                modifier = Modifier.weight(1f).semantics { heading() },
                color = Color.White,
                fontFamily = IBMPlexMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Mp3QueueToggle(
                icon = R.drawable.shuffle,
                description = stringResource(R.string.mp3_shuffle_queue),
                active = shuffleModeEnabled,
                enabled = !isGuest,
                onClick = { connection.setShuffleModeEnabled(!shuffleModeEnabled) },
            )
            Mp3QueueToggle(
                icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                description = stringResource(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one
                        Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all
                        else -> R.string.repeat_mode_off
                    }
                ),
                active = repeatMode != Player.REPEAT_MODE_OFF,
                enabled = !isGuest,
                onClick = { connection.player.toggleRepeatMode() },
            )
        }
        windows.getOrNull(currentIndex)?.mediaItem?.metadata?.let { current ->
            IrideMp3CurrentTrack(current, remaining, textScale, onClick = onNowPlaying)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val queueSection = rows.dropWhile { it != Mp3QueueEntry.HistoryDivider }
            val spacingPx = with(density) { 5.dp.roundToPx() }
            val queueHeight = queueSection.sumOf { entry ->
                queueItemHeights[entry.key] ?: if (entry is Mp3QueueEntry.Track) {
                    with(density) { 54.dp.roundToPx() }
                } else 0
            } + spacingPx * (queueSection.size - 1).coerceAtLeast(0)
            // Short queues need enough trailing space to align their heading with the viewport top.
            val bottomSpace = with(density) {
                (constraints.maxHeight - queueHeight).coerceAtLeast(0).toDp()
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = bottomSpace),
                modifier =
                    Modifier.fillMaxSize()
                        .clipToBounds()
                        .nestedScroll(scrollConnection)
                        .irideFolioGestures(listState)
                        .drawWithContent {
                            drawContent()
                            val layout = listState.layoutInfo
                            val visible = layout.visibleItemsInfo
                            if (visible.isNotEmpty() && layout.totalItemsCount > visible.size) {
                                val viewport = layout.viewportSize.height.toFloat()
                                val averageItemSize = visible.map { it.size }.average().toFloat()
                                val contentSize = averageItemSize * layout.totalItemsCount
                                if (contentSize > viewport && viewport > 0f) {
                                    val thumbHeight =
                                        (viewport * viewport / contentSize)
                                            .coerceIn(18.dp.toPx(), viewport)
                                    val scrollOffset =
                                        (listState.firstVisibleItemIndex * averageItemSize +
                                            listState.firstVisibleItemScrollOffset)
                                            .coerceAtLeast(0f)
                                    val scrollRange = (contentSize - viewport).coerceAtLeast(1f)
                                    val top =
                                        (scrollOffset / scrollRange * (viewport - thumbHeight))
                                            .coerceIn(0f, viewport - thumbHeight)
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.36f),
                                        topLeft = Offset(size.width - 2.dp.toPx(), top),
                                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), thumbHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                    )
                                }
                            }
                        }
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
                item(key = "history_title") { Mp3QueueSectionTitle(stringResource(R.string.history)) }
                if (!hasHistory) {
                    item(key = "history_empty") {
                        Mp3EmptyText(stringResource(R.string.mp3_history_empty))
                    }
                }
                rows.forEach { entry ->
                    when (entry) {
                        Mp3QueueEntry.HistoryDivider -> {
                            item(key = entry.key) {
                                Box(Modifier.onSizeChanged { queueItemHeights[entry.key] = it.height }) {
                                    ReorderableItem(reorder, key = entry.key) {
                                        when {
                                            manualQueueCount > 0 ->
                                                Mp3QueueSectionTitle(
                                                    stringResource(R.string.mp3_manual_queue),
                                                    actionText = stringResource(R.string.mp3_clear_queue),
                                                    onAction = ::clearManualQueue,
                                                )
                                            upNextCount == 0 ->
                                                Mp3EmptyText(stringResource(R.string.mp3_queue_empty))
                                            else -> Spacer(Modifier.height(2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Mp3QueueEntry.UpNextDivider -> {
                            item(key = entry.key) {
                                Box(Modifier.onSizeChanged { queueItemHeights[entry.key] = it.height }) {
                                    Mp3QueueSectionTitle(stringResource(R.string.mp3_up_next))
                                }
                            }
                        }
                        is Mp3QueueEntry.Track -> {
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
                                                .draggableHandle(onDragStopped = ::finishDrag)
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
                                            controls.manualQueueOverrides.remove(entry.windowUid)
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
                                                        // Transparent rows expose the action only in the revealed strip.
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
                                                Modifier.onSizeChanged {
                                                    if (entry.windowUid != null) queueItemHeights[entry.key] = it.height
                                                }.clip(FolioShape(8.dp))
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
private fun Mp3QueueSectionTitle(
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().semantics { heading() }
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                modifier = Modifier.weight(1f).padding(start = 6.dp, top = 8.dp, bottom = 8.dp),
                color = Color.White.copy(alpha = .65f),
                fontFamily = IBMPlexMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            if (actionText != null && onAction != null) {
                Text(
                    actionText,
                    modifier =
                        Modifier.clickable(onClickLabel = actionText, onClick = onAction)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    color = Color.White.copy(alpha = .55f),
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun Mp3QueueToggle(
    icon: Int,
    description: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.padding(horizontal = 2.dp).size(36.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = if (active) .24f else .10f))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 1f else .35f),
            modifier = Modifier.size(18.dp),
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
    val lightweightMode by rememberLightweightFolioMode()
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
        if (!reducedMotion && !lightweightMode && displayedBitmap != null && bitmap != null) {
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
                if (lyricsStyle == Mp3LyricsStyle.GRADIENT) {
                    Mp3LyricsBackground(
                        artwork = displayedBitmap,
                        lightweight = lightweightMode,
                        incomingArtwork = incomingBitmap,
                        crossfadeProgress = artworkProgress.value,
                    )
                }
            }
            if (lightweightMode && lyricsStyle != Mp3LyricsStyle.GRADIENT) {
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { alpha = actionsOpacity }
                        .background(Color.Black)
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
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = {
                                onInteraction()
                                controller.selectionAction()
                            },
                            enabled =
                                fullscreen && controlsOpacity > .5f && controller.canSelectLyrics,
                            modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
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
                            modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
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
                            modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
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
