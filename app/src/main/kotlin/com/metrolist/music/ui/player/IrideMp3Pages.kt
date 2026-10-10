package com.metrolist.music.ui.player

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Stable
internal class IrideMp3QueueControls {
    var showingHistory by mutableStateOf(false)
    var selectedMood by mutableStateOf(AUTOMIX_FILTER_ALL)
}

// Resolve at the time of an action: timeline indices can change during a drag or network request.
private fun Player.indexOfWindow(uid: Any): Int {
    val window = Timeline.Window()
    for (index in 0 until currentTimeline.windowCount) {
        if (currentTimeline.getWindow(index, window).uid == uid) return index
    }
    return -1
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val radio by connection.service.isAutoMixQueueActive.collectAsState()
    val seed by connection.service.radioSeedMetadata.collectAsState()
    val events by database.recentEventsPerSong(80).collectAsState(initial = emptyList())
    val history = remember(events) { events.map { it.song.toMediaMetadata() } }
    val upcoming = windows.drop((currentIndex + 1).coerceAtLeast(0))
    val displayed = remember { mutableStateListOf<Timeline.Window>() }
    val listState = key(controls.showingHistory) { rememberLazyListState() }
    val moodsState = rememberLazyListState()
    // ExoPlayer UIDs are opaque objects; LazyColumn keys must be saveable in an Android Bundle.
    val windowKeys = remember { mutableMapOf<Any, String>() }
    fun windowKey(uid: Any): String = windowKeys.getOrPut(uid) { "queued_${windowKeys.size}" }
    val genreSongs = remember(upcoming, radio) {
        if (radio) upcoming.map { GenreSongInfo(it.mediaItem.mediaId, it.mediaItem.metadata?.title.orEmpty(), it.mediaItem.metadata?.artists?.firstOrNull()?.name) }
        else emptyList()
    }
    val genres = rememberGenreFilter(genreSongs)
    val moods = remember(genres.sortedGenres) {
        (listOf(
            AUTOMIX_FILTER_ALL, AUTOMIX_FILTER_FAMILIAR, AUTOMIX_FILTER_DISCOVER,
            AUTOMIX_FILTER_PARTY, AUTOMIX_FILTER_WORKOUT,
        ) + genres.sortedGenres.take(2).map { it.uppercase() }).distinct()
    }
    val familiar = remember(history, seed) {
        (history.flatMap { it.artists } + seed?.artists.orEmpty()).map { it.name }.toSet()
    }
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val source = displayed.indexOfFirst { windowKey(it.uid) == from.key }
        val target = displayed.indexOfFirst { windowKey(it.uid) == to.key }
        if (!isGuest && source >= 0 && target >= 0 && source != target) {
            val fromIndex = connection.player.indexOfWindow(displayed[source].uid)
            val toIndex = connection.player.indexOfWindow(displayed[target].uid)
            if (fromIndex > connection.player.currentMediaItemIndex && toIndex > connection.player.currentMediaItemIndex) {
                displayed.add(target, displayed.removeAt(source))
                // A deliberate order replaces random playback order.
                connection.player.shuffleModeEnabled = false
                connection.player.moveMediaItem(fromIndex, toIndex)
            }
        }
    }
    LaunchedEffect(windows, currentIndex, reorder.isAnyItemDragging) {
        if (!reorder.isAnyItemDragging) {
            displayed.clear()
            displayed.addAll(upcoming)
        }
    }
    LaunchedEffect(seed?.id, radio) {
        controls.selectedMood = AUTOMIX_FILTER_ALL
    }

    fun chooseMood(index: Int) {
        if (isGuest || !radio) return
        val mood = moods[index.coerceIn(moods.indices)]
        controls.selectedMood = mood
        val latest = connection.queueWindows.value.drop((connection.currentWindowIndex.value + 1).coerceAtLeast(0))
        val preferred = filterAutomix(latest.map { it.mediaItem }, mood, familiar, genres.genreBySongId)
            .map { it.mediaId }.toSet()
        // Prioritize matching tracks without deleting other recommendations or interrupting playback.
        val ordered = latest.sortedBy { if (it.mediaItem.mediaId in preferred) 0 else 1 }
        connection.player.shuffleModeEnabled = false
        ordered.forEachIndexed { offset, window ->
            val source = connection.player.indexOfWindow(window.uid)
            val target = connection.player.currentMediaItemIndex + 1 + offset
            if (source > connection.player.currentMediaItemIndex && target < connection.player.mediaItemCount && source != target) {
                connection.player.moveMediaItem(source, target)
            }
        }
    }
    val gestureLock = LocalPlayerSheetGestureLock.current
    val density = LocalDensity.current
    val scrollConnection = remember(sheetState, listState, density, gestureLock, reorder) {
        Mp3QueueScrollConnection(sheetState, with(density) { 56.dp.toPx() }) {
            gestureLock.value || reorder.isAnyItemDragging
        }
    }
    DisposableEffect(scrollConnection) {
        onDispose { scrollConnection.cancel() }
    }
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (controls.showingHistory) R.string.history else R.string.mp3_manage_queue),
                color = Color.White, fontFamily = SpaceMonoFontFamily, fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Mp3PageAction(
                if (controls.showingHistory) R.drawable.queue_music else R.drawable.history,
                stringResource(if (controls.showingHistory) R.string.mp3_manage_queue else R.string.history),
                { controls.showingHistory = !controls.showingHistory },
            )
        }
        if (!controls.showingHistory) {
            windows.getOrNull(currentIndex)?.mediaItem?.metadata?.let { current ->
                IrideMp3CurrentTrack(current, remaining, textScale, onClick = onNowPlaying)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
                .nestedScroll(scrollConnection)
                .pointerInput(scrollConnection) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        scrollConnection.reset()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                    }
                },
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (controls.showingHistory) {
                itemsIndexed(history, key = { _, item -> "history_${item.id}" }) { index, item ->
                    Mp3QueueRow(item, onClick = {
                        if (!isGuest) connection.playQueue(ListQueue(items = history.map { it.toMediaItem() }, startIndex = index))
                    })
                }
                if (history.isEmpty()) item { Mp3EmptyText(stringResource(R.string.mp3_history_empty)) }
            } else {
                if (radio) item(key = "moods") {
                    LazyRow(state = moodsState, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        itemsIndexed(moods, key = { _, mood -> mood }) { index, mood ->
                            Text(mood, color = Color.White, fontFamily = SpaceMonoFontFamily, fontSize = 10.sp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF1E1E1E))
                                    .border(1.dp, if (controls.selectedMood == mood) Color.White.copy(alpha = .4f) else Color.Transparent, RoundedCornerShape(50))
                                    .clickable(enabled = !isGuest) { chooseMood(index) }.padding(horizontal = 13.dp, vertical = 9.dp))
                        }
                    }
                }
                if (displayed.isEmpty()) item { Mp3EmptyText(stringResource(R.string.mp3_queue_empty)) }
                items(displayed, key = { windowKey(it.uid) }) { window ->
                    ReorderableItem(reorder, key = windowKey(window.uid)) {
                        val lock = LocalPlayerSheetGestureLock.current
                        val handle = if (isGuest) Modifier else Modifier
                            .pointerInput(lock) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    lock.value = true
                                    try {
                                        do {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                        } while (event.changes.any { it.pressed })
                                    } finally {
                                        lock.value = false
                                    }
                                }
                            }.draggableHandle()
                        val dismiss = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismiss.settledValue, isGuest) {
                            // currentValue changes while dragging; settledValue waits for release.
                            if (dismiss.settledValue == SwipeToDismissBoxValue.StartToEnd && !isGuest) {
                                val index = connection.player.indexOfWindow(window.uid)
                                if (index > connection.player.currentMediaItemIndex) connection.player.removeMediaItem(index)
                                dismiss.reset()
                            }
                        }
                        SwipeToDismissBox(state = dismiss, enableDismissFromStartToEnd = !isGuest && !reorder.isAnyItemDragging,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .drawWithContent {
                                            // Transparent rows must expose the action only in the revealed strip.
                                            if (dismiss.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                                val reveal = abs(dismiss.requireOffset()).coerceIn(0f, size.width)
                                                if (layoutDirection == LayoutDirection.Ltr) {
                                                    clipRect(right = reveal) { this@drawWithContent.drawContent() }
                                                } else {
                                                    clipRect(left = size.width - reveal) { this@drawWithContent.drawContent() }
                                                }
                                            }
                                        }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.delete), stringResource(R.string.mp3_remove_queue),
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(start = 12.dp).size(24.dp),
                                    )
                                }
                            }) {
                            window.mediaItem.metadata?.let { item ->
                                Mp3QueueRow(item, handle = handle, showReorderHandle = !isGuest, onClick = {
                                    if (!isGuest) {
                                        val index = connection.player.indexOfWindow(window.uid)
                                        if (index >= 0) { connection.player.seekToDefaultPosition(index); connection.player.play() }
                                    }
                                })
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
private fun Mp3QueueRow(metadata: MediaMetadata,
    handle: Modifier = Modifier, showReorderHandle: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showReorderHandle) {
            Box(handle.size(width = 40.dp, height = 54.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.drag_handle), stringResource(R.string.mp3_reorder),
                    tint = Color.White.copy(alpha = .4f), modifier = Modifier.size(25.dp))
            }
        }
        Row(Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick).padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(metadata.thumbnailUrl, null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(metadata.title, fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, lineHeight = 16.sp,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(metadata.artists.joinToString { it.name }, fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp, lineHeight = 14.sp, color = Color.White.copy(alpha = .55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Mp3EmptyText(text: String) {
    Text(text, color = Color.White.copy(alpha = .5f), fontFamily = SpaceMonoFontFamily, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
}

@Composable
internal fun IrideMp3Lyrics(
    position: Long,
    controller: LyricsPillController,
    onOptions: () -> Unit,
    onFullscreen: (Boolean) -> Unit,
) {
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val lyricsStyle by rememberEnumPreference(Mp3LyricsStyleKey, Mp3LyricsStyle.GRADIENT)
    val latestPosition by rememberUpdatedState(position)
    val latestOptions by rememberUpdatedState(onOptions)
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val context = LocalContext.current
    val connection = LocalPlayerConnection.current
    val metadata by connection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val fullscreenProgress = remember { Animatable(0f) }
    var inlineBounds by remember { mutableStateOf<Rect?>(null) }
    var closing by remember { mutableStateOf(false) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var incomingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val artworkProgress = remember { Animatable(0f) }
    LaunchedEffect(metadata?.id, metadata?.thumbnailUrl, lyricsStyle) {
        if (lyricsStyle != Mp3LyricsStyle.GRADIENT) return@LaunchedEffect
        incomingBitmap = null
        artworkProgress.snapTo(0f)
        val thumbnail = metadata?.thumbnailUrl
        val bitmap = if (thumbnail != null) withContext(Dispatchers.IO) {
            context.imageLoader.execute(
                ImageRequest.Builder(context).data(thumbnail).size(100, 100).allowHardware(false)
                    .memoryCacheKey("gradient_${metadata?.id}").build(),
            ).image?.toBitmap()
        } else null
        if (!reducedMotion && displayedBitmap != null && bitmap != null) {
            incomingBitmap = bitmap
            artworkProgress.animateTo(1f, tween(700))
        }
        displayedBitmap = bitmap
        incomingBitmap = null
        artworkProgress.snapTo(0f)
    }
    LaunchedEffect(fullscreen) {
        fullscreenProgress.snapTo(0f)
        if (fullscreen) {
            fullscreenProgress.animateTo(
                1f, tween(if (reducedMotion) 0 else IrideMotion.Fullscreen, easing = FastOutSlowInEasing),
            )
        }
    }
    fun closeFullscreen(afterClose: () -> Unit = {}) {
        if (closing) return
        closing = true
        if (controller.isSelectionModeActive) controller.selectionAction()
        scope.launch {
            fullscreenProgress.animateTo(
                0f, tween(if (reducedMotion) 0 else IrideMotion.Fullscreen, easing = FastOutSlowInEasing),
            )
            fullscreen = false
            closing = false
            afterClose()
        }
    }
    SideEffect { onFullscreen(fullscreen) }
    DisposableEffect(Unit) {
        onDispose {
            onFullscreen(false)
            controller.scrollAction = {}
        }
    }

    // Move the same composition between windows: keep scroll, translation and renderer state.
    val content = remember(controller, lyricsViewModel) {
        movableContentOf<Boolean> { full ->
            val backdrop = rememberFrostBackdrop().takeIf { lyricsStyle == Mp3LyricsStyle.GRADIENT }
            CompositionLocalProvider(LocalFolioButtonBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize().background(IrideMp3BackgroundColor)) {
                    // Glass controls must sample a sibling, never a layer containing themselves.
                    if (lyricsStyle == Mp3LyricsStyle.GRADIENT) Box(
                        Modifier.fillMaxSize().background(IrideMp3BackgroundColor).recordFrostBackdrop(backdrop),
                    ) {
                        BetterAnimatedGradientBackground(
                            thumbnail = displayedBitmap,
                            incomingThumbnail = incomingBitmap,
                            crossfadeProgress = artworkProgress.value,
                            grainOpacity = IrideFolio.LyricsGrainOpacity,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(Modifier.fillMaxSize()) {
                        if (lyricsStyle == Mp3LyricsStyle.TV) {
                            IrideTvLyrics(
                                position = latestPosition,
                                controller = controller,
                                viewModel = lyricsViewModel,
                                onOptions = { if (full) closeFullscreen { latestOptions() } else latestOptions() },
                                modifier = Modifier.fillMaxSize().padding(
                                    top = if (full) lerp(68f, 90f, fullscreenProgress.value).dp else 68.dp,
                                ),
                            )
                        }
                        if (lyricsStyle == Mp3LyricsStyle.GRADIENT) Lyrics(
                            sliderPositionProvider = { latestPosition },
                            modifier = Modifier.fillMaxSize().padding(
                                start = 12.dp, top = if (full) lerp(68f, 90f, fullscreenProgress.value).dp else 68.dp,
                            ),
                            showLyrics = true,
                            lyricsViewModel = lyricsViewModel,
                            showPills = false,
                            pillsController = controller,
                            isFullScreen = full,
                            onExitFullScreen = { closeFullscreen() },
                            onShowOptionsMenu = { if (full) closeFullscreen { latestOptions() } else latestOptions() },
                            // Enlarge the compact view while preserving the final fullscreen scale.
                            textScale = if (full) lerp(1.3f, 1.6f, fullscreenProgress.value) else 1.3f,
                            morphing = { fullscreen && fullscreenProgress.value > 0f && fullscreenProgress.value < 1f },
                        )
                    }
                    Row(
                        Modifier.align(Alignment.TopEnd)
                            .windowInsetsPadding(if (full) WindowInsets.displayCutout else WindowInsets(0))
                            .padding(
                                top = if (full) lerp(16f, 26f, fullscreenProgress.value).dp else 16.dp,
                                end = if (full) lerp(8f, 20f, fullscreenProgress.value).dp else 8.dp,
                            )
                            .irideFolioGlassSurface(backdrop)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IrideFolioActionReveal(visible = full && !closing && fullscreenProgress.value >= 0.5f) {
                            Row {
                                Mp3PageAction(
                                    R.drawable.translate, stringResource(R.string.mp3_translate),
                                    { controller.translateAction() },
                                )
                                if (lyricsStyle == Mp3LyricsStyle.GRADIENT) Mp3PageAction(
                                    R.drawable.share, stringResource(R.string.share_lyrics),
                                    { controller.selectionAction() }, enabled = controller.canSelectLyrics,
                                )
                            }
                        }
                        Mp3PageAction(
                            if (full) R.drawable.ic_iride_fullscreen_exit else R.drawable.fullscreen,
                            stringResource(if (full) R.string.mp3_exit_fullscreen else R.string.mp3_fullscreen),
                            { if (full) closeFullscreen() else fullscreen = true },
                        )
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        if (!fullscreen) inlineBounds = it.boundsInWindow()
    }) {
        if (!fullscreen) content(false)
    }
    if (fullscreen) Dialog(onDismissRequest = { closeFullscreen() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false,
        )) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                it.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                it.setWindowAnimations(0)
            }
            val insets = window?.let { WindowCompat.getInsetsController(it, view) }
            insets?.hide(WindowInsetsCompat.Type.systemBars())
            insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose {
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
                insets?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        var dialogOrigin by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { dialogOrigin = it.positionInWindow() }) {
            val progress = fullscreenProgress.value
            val start = inlineBounds
            val startWidth = with(density) { start?.width?.toDp() } ?: maxWidth
            val startHeight = with(density) { start?.height?.toDp() } ?: maxHeight
            // Resize the viewport rather than stretching glyphs in two different directions.
            Box(
                Modifier.offset {
                    IntOffset(
                        lerp((start?.left ?: dialogOrigin.x) - dialogOrigin.x, 0f, progress).roundToInt(),
                        lerp((start?.top ?: dialogOrigin.y) - dialogOrigin.y, 0f, progress).roundToInt(),
                    )
                }.size(
                    width = startWidth + (maxWidth - startWidth) * progress,
                    height = startHeight + (maxHeight - startHeight) * progress,
                ).clip(RoundedCornerShape(12.dp * (1f - progress))),
            ) { content(true) }
        }
    }
}

@Composable
private fun Mp3PageAction(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    IrideFolioFrostedButton(
        onClick = onClick,
        enabled = enabled,
        grouped = true,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
    ) {
        Icon(painterResource(icon), description, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
    }
}
