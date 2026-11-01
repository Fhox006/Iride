package com.metrolist.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.REVERIE_CHAPTER_MILLIS
import com.metrolist.music.models.REVERIE_PAPER
import com.metrolist.music.models.ReverieChapter
import com.metrolist.music.models.ReverieCursor
import com.metrolist.music.models.seekReverieChapter
import com.metrolist.music.models.reverieEdition
import com.metrolist.music.models.reveriePalette
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioCompactControls
import com.metrolist.music.ui.component.IrideFolioFrostedButton
import com.metrolist.music.ui.component.LocalFolioButtonBackdrop
import com.metrolist.music.ui.component.LocalReveriePaper
import com.metrolist.music.ui.component.LocalReveriePalette
import com.metrolist.music.ui.component.ReverieBackdrop
import com.metrolist.music.ui.component.ReverieFilm
import com.metrolist.music.ui.component.irideFolioGlassSurface
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.rememberReverieMotionTime
import com.metrolist.music.ui.component.reverieChapterTitle
import com.metrolist.music.ui.component.reverieGestures
import com.metrolist.music.ui.component.reveriePrintFinish
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.shareReverieImage
import com.metrolist.music.viewmodels.MonthlyReverieViewModel
import com.metrolist.music.viewmodels.ReverieFilmState
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReverieScreen(navController: NavController, month: YearMonth, viewModel: MonthlyReverieViewModel = hiltViewModel()) {
    val state by viewModel.film.collectAsStateWithLifecycle()
    val edition = remember(month) { reverieEdition(month) }
    val defaultPalette = remember(edition) { reveriePalette(edition.primary) }
    val connection = LocalPlayerConnection.current
    val cursorSaver = remember {
        androidx.compose.runtime.saveable.listSaver<ReverieCursor, Int>(
            save = { listOf(it.chapter, it.revision, if (it.instant) 1 else 0) },
            restore = { ReverieCursor(it[0], it[1], it[2] == 1) },
        )
    }
    var cursor by rememberSaveable(month.toString(), stateSaver = cursorSaver) { mutableStateOf(ReverieCursor()) }
    var closing by remember { mutableStateOf(false) }
    var resumeMain by remember { mutableStateOf(true) }
    var volume by remember { mutableFloatStateOf(1f) }
    var motionRunning by remember { mutableStateOf(true) }
    val reducedMotion = rememberReducedMotion()
    val motionTime = rememberReverieMotionTime(motionRunning && !closing && !reducedMotion)
    DisposableEffect(connection) {
        val lease = connection?.let { runCatching { it.service.suspendForReverie() }.getOrNull() }
        volume = lease?.volume ?: 1f
        onDispose { lease?.release(resumeMain) }
    }
    LaunchedEffect(month) { viewModel.markHomeSeen(month); viewModel.load(month) }
    val ready = state as? ReverieFilmState.Ready
    val chapter = ready?.recap?.chapters?.getOrNull(cursor.chapter)
    val palette = ready?.backgrounds?.get(chapter) ?: defaultPalette
    ReverieSheet(Color(palette.background), onClosing = { closing = true }, onDismiss = { navController.navigateUp() }) { close ->
        val backdrop = rememberFrostBackdrop(backgroundColor = Color(palette.background), stableBlur = true)
        CompositionLocalProvider(LocalFolioButtonBackdrop provides backdrop, LocalReveriePaper provides Color(palette.background),
            LocalReveriePalette provides palette) {
            Box(Modifier.fillMaxSize().reveriePrintFinish()) {
                ReverieBackdrop(cursor.chapter, palette, reducedMotion,
                    Modifier.recordFrostBackdrop(backdrop), variant = edition.figureVariant)
                Box(Modifier.fillMaxSize().windowInsetsPadding(
                    androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )) {
                    when (val film = state) {
                        is ReverieFilmState.Ready -> {
                            val shown = cursor
                            ReveriePlayback(film, viewModel, volume, closing, motionTime, shown,
                                onSeek = { cursor = seekReverieChapter(cursor, it, film.recap.chapters.size) },
                                onStep = { cursor = seekReverieChapter(cursor, cursor.chapter + it, film.recap.chapters.size) },
                                onAdvance = { if (cursor == shown) cursor = shown.copy(chapter = it, instant = false) },
                                onMotionRunningChange = { motionRunning = it },
                                onFocusLost = { resumeMain = false }, onClose = close)
                        }
                        else -> {
                            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                if (film == ReverieFilmState.Loading) CircularProgressIndicator(color = Color(0xFF3464C8))
                                Text(stringResource(when (film) {
                                    ReverieFilmState.Loading -> R.string.reverie_loading
                                    ReverieFilmState.Empty -> R.string.reverie_empty
                                    else -> R.string.reverie_error
                                }), color = Color(0xFF24242A), fontFamily = IBMPlexMonoFontFamily, fontSize = 14.sp)
                                if (film == ReverieFilmState.Error) IrideFolioFrostedButton(onClick = { viewModel.load(month, retry = true) }) {
                                    Text(stringResource(R.string.reverie_retry), color = Color(0xFF24242A))
                                }
                            }
                            ReverieFolioControls(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                                IconButton(onClick = close, modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize)) {
                                    Icon(painterResource(R.drawable.close), stringResource(R.string.close), tint = Color(0xFF24242A))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The MP3 player's curtain, in a navigation dialog that keeps the previous page underneath. */
@Composable
private fun ReverieSheet(background: Color, onClosing: () -> Unit, onDismiss: () -> Unit, content: @Composable (() -> Unit) -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        window?.apply {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(this, false)
            setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            attributes = attributes.apply {
                layoutInDisplayCutoutMode = if (android.os.Build.VERSION.SDK_INT >= 30)
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            setWindowAnimations(0)
            @Suppress("DEPRECATION")
            run { navigationBarColor = android.graphics.Color.TRANSPARENT; statusBarColor = android.graphics.Color.TRANSPARENT }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                isNavigationBarContrastEnforced = false
                isStatusBarContrastEnforced = false
            }
            androidx.core.view.WindowCompat.getInsetsController(this, view).isAppearanceLightNavigationBars = true
        }
        onDispose { }
    }
    com.metrolist.music.ui.component.IrideFolioDragOverlay(background, onClosing, onDismiss, content)
}

@Composable
private fun ReveriePlayback(
    film: ReverieFilmState.Ready, viewModel: MonthlyReverieViewModel, volume: Float,
    closing: Boolean, motionTime: State<Float>, cursor: ReverieCursor,
    onSeek: (Int) -> Unit, onStep: (Int) -> Unit, onAdvance: (Int) -> Unit,
    onMotionRunningChange: (Boolean) -> Unit,
    onFocusLost: () -> Unit, onClose: () -> Unit,
) {
    val recap = film.recap
    val chapters = remember(recap) { recap.chapters }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()
    // Chapter duration is editorial time, independent of Android's animation-speed preference.
    val timelineScale = remember { object : MotionDurationScale { override val scaleFactor = 1f } }
    var held by remember { mutableStateOf(false) }
    var playing by rememberSaveable(recap.month.toString()) { mutableStateOf(true) }
    var muted by rememberSaveable { mutableStateOf(false) }
    val current = cursor.chapter.coerceIn(chapters.indices)
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var audioAvailable by remember { mutableStateOf(true) }
    var chapterReady by remember(recap, current, cursor.revision) { mutableStateOf(false) }
    var audioPrepared by remember(recap) { mutableStateOf(false) }
    var audioPreparationFailed by remember(recap) { mutableStateOf(false) }
    var audioProgress by remember(recap) { mutableStateOf(0 to 0) }
    var preparationAttempt by remember { mutableStateOf(0) }
    var exporting by remember { mutableStateOf(false) }
    var finished by rememberSaveable(recap.month.toString()) { mutableStateOf(false) }
    val elapsed = rememberSaveable(recap.month.toString(), current, cursor.revision, saver = Saver<Animatable<Float, AnimationVector1D>, Float>(
        save = { it.value }, restore = { Animatable(it) },
    )) { Animatable(0f) }
    val audio = remember(recap) { viewModel.audioFactory.create { playing = false; onFocusLost() } }
    val images = film.images

    DisposableEffect(audio) { onDispose { audio.release() } }
    LaunchedEffect(audio, preparationAttempt) {
        audioPreparationFailed = false
        chapterReady = false
        try {
            audio.prepareAll(chapters.mapNotNull(recap::soundtrack)) { done, total -> audioProgress = done to total }
            audio.prime(recap.soundtrack(chapters[current]), chapters.getOrNull(current + 1)?.let(recap::soundtrack))
            audioPrepared = true
        } catch (error: CancellationException) { throw error
        } catch (error: Exception) {
            reportException(error)
            audioPreparationFailed = true
        }
    }
    LaunchedEffect(audio, volume) { audio.setMasterVolume(volume) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(view, playing, resumed, finished, held, closing, audioPrepared) {
        val previous = view.keepScreenOn
        view.keepScreenOn = playing && resumed && !finished && !closing
        onDispose { view.keepScreenOn = previous }
    }
    LaunchedEffect(muted) { audio.setMuted(muted) }
    DisposableEffect(playing, finished, resumed, exporting, held, closing, audioPrepared) {
        onMotionRunningChange(audioPrepared && (playing || finished) && resumed && !exporting && !held && !closing)
        onDispose { onMotionRunningChange(true) }
    }
    DisposableEffect(audio, playing, resumed, exporting, held, closing, audioPrepared) {
        audio.setPlaying(audioPrepared && playing && resumed && !exporting && !held && !closing)
        onDispose { audio.setPlaying(false) }
    }
    // All excerpts are buffered before editorial time begins.
    LaunchedEffect(recap, current, cursor.revision, playing, resumed, exporting, held, closing, audioPrepared, chapterReady) {
        if (!audioPrepared || !chapterReady || !playing || !resumed || exporting || held || closing) return@LaunchedEffect
        withContext(timelineScale) {
            elapsed.animateTo(REVERIE_CHAPTER_MILLIS.toFloat(),
                tween((REVERIE_CHAPTER_MILLIS - elapsed.value).toInt().coerceAtLeast(1), easing = LinearEasing))
            if (current == chapters.lastIndex) {
                finished = true
                playing = false
            } else onAdvance(current + 1)
        }
    }
    LaunchedEffect(audio, current, cursor.revision, playing, resumed, exporting, held, closing, audioPrepared) {
        if (!audioPrepared || !playing || !resumed || exporting || held || closing) return@LaunchedEffect
        val next = chapters.getOrNull(current + 1)
        val dissolveAt = (REVERIE_CHAPTER_MILLIS - audio.dissolveMillis).toFloat()
        if (elapsed.value < dissolveAt) {
            audioAvailable = audio.transitionTo(recap.soundtrack(chapters[current]),
                audio.dissolveMillis)
            if (!audioAvailable) {
                audioPrepared = false
                audioPreparationFailed = true
                return@LaunchedEffect
            }
            chapterReady = true
            audio.preload(next?.let(recap::soundtrack))
        }
        snapshotFlow { elapsed.value }.first { it >= dissolveAt }
        if (next == null) audio.finish((REVERIE_CHAPTER_MILLIS - elapsed.value).toLong().coerceAtLeast(1))
        else {
            audioAvailable = audio.transitionTo(recap.soundtrack(next), audio.dissolveMillis)
            if (!audioAvailable) {
                audioPrepared = false
                audioPreparationFailed = true
            }
        }
    }
    fun seek(index: Int) {
        if (index !in chapters.indices) return
        onSeek(index)
        finished = false
        playing = true
    }
    fun export() {
        if (exporting) return
        exporting = true
        scope.launch {
            try {
                val uri = viewModel.exportImage()
                shareReverieImage(context, uri)
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                reportException(error)
                Toast.makeText(context, R.string.reverie_export_error, Toast.LENGTH_SHORT).show()
            } finally { exporting = false }
        }
    }
    val ink = Color(0xFF24242A)
    fun step(direction: Int) {
        if (direction != 0 && current + direction in chapters.indices) {
            onStep(direction)
            finished = false
            playing = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (!audioPrepared) {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (!audioPreparationFailed) CircularProgressIndicator(color = ink)
                Text(stringResource(if (audioPreparationFailed) R.string.reverie_audio_prepare_error else R.string.reverie_audio_preparing_all,
                    audioProgress.first, audioProgress.second), color = ink, fontFamily = IBMPlexMonoFontFamily)
                if (audioPreparationFailed) IrideFolioFrostedButton(onClick = { preparationAttempt++ }) {
                    Text(stringResource(R.string.reverie_retry), color = ink)
                }
            }
            ReverieFolioControls(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                IconButton(onClick = onClose) { Icon(painterResource(R.drawable.close), stringResource(R.string.close), tint = ink) }
            }
            return@Box
        }
        // Controls and share action occupy measured slots; the film only receives the remaining height.
        Column(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            ReverieTimeline(chapters, current, { elapsed.value }, ::seek)
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reverie_edition, recap.month.format(DateTimeFormatter.ofPattern("MM / yyyy"))),
                    color = ink, fontFamily = IBMPlexMonoFontFamily, maxLines = 1,
                    fontSize = 12.sp, letterSpacing = .5.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                ReverieFolioControls {
                    if (!finished) IconButton(onClick = { playing = !playing }, modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize)) {
                        Icon(painterResource(if (playing) R.drawable.pause else R.drawable.play),
                            stringResource(if (playing) R.string.reverie_pause else R.string.reverie_resume), tint = ink, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { muted = !muted }, modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize)) {
                        Icon(painterResource(if (muted) R.drawable.volume_off else R.drawable.volume_up),
                            stringResource(if (!audioAvailable) R.string.reverie_audio_unavailable else if (muted) R.string.reverie_unmute else R.string.reverie_mute),
                            tint = ink, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize)) {
                        Icon(painterResource(R.drawable.close), stringResource(R.string.close), tint = ink, modifier = Modifier.size(22.dp))
                    }
                }
            }
            val gestures = stringResource(R.string.reverie_gestures)
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()
                .semantics {
                    contentDescription = gestures
                    customActions = listOf(
                        androidx.compose.ui.semantics.CustomAccessibilityAction(context.getString(R.string.reverie_previous)) { step(-1); true },
                        androidx.compose.ui.semantics.CustomAccessibilityAction(context.getString(R.string.reverie_next)) { step(1); true },
                    )
                }
                .reverieGestures(recap.month, onHold = { held = it }, onTap = { step(if (it < .35f) -1 else 1) },
                    onStep = ::step)) {
                CompositionLocalProvider(LocalReveriePaper provides Color(
                    film.backgrounds[chapters[current]]?.background ?: REVERIE_PAPER,
                )) {
                    ReverieFilm(recap, current, { elapsed.value }, images, reducedMotion,
                        motionTime = { motionTime.value }, instant = cursor.instant)
                }
            }
            if (chapters[current] == ReverieChapter.POSTER) {
                Box(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    IrideFolioFrostedButton(onClick = ::export,
                        enabled = !exporting, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.share), null, tint = ink, modifier = Modifier.size(20.dp))
                        Text(stringResource(if (exporting) R.string.reverie_exporting else R.string.reverie_share_image),
                            color = ink, modifier = Modifier.padding(start = 12.dp), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReverieFolioControls(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Box(modifier) {
        IrideFolioCompactControls {
            Row(Modifier.irideFolioGlassSurface(LocalFolioButtonBackdrop.current).padding(2.dp),
                verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
private fun ReverieTimeline(chapters: List<ReverieChapter>, current: Int, elapsed: () -> Float, onSelect: (Int) -> Unit) {
    val description = stringResource(R.string.reverie_chapter, current + 1, chapters.size,
        stringResource(reverieChapterTitle(chapters[current])).replace('\n', ' '))
    Canvas(Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 20.dp).semantics { contentDescription = description }
        .pointerInput(chapters) { detectTapGestures { point ->
            onSelect((point.x / size.width * chapters.size).toInt().coerceIn(chapters.indices))
        } }) {
        val y = size.height / 2
        val progress = (current + elapsed() / REVERIE_CHAPTER_MILLIS) / chapters.size
        drawLine(Color(0xFF24242A).copy(alpha = .22f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
        drawLine(Color.Black, Offset(0f, y), Offset(size.width * progress, y), 2.dp.toPx())
        for (i in 1 until chapters.size) drawCircle(Color(0xFF24242A).copy(alpha = .4f), 2.dp.toPx(), Offset(size.width * i / chapters.size, y))
    }
}
