/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.text.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AiProviderKey
import com.metrolist.music.constants.AiSystemPromptKey
import com.metrolist.music.constants.DeeplApiKey
import com.metrolist.music.constants.DeeplFormalityKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsRomanizeAsMainKey
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.constants.LyricsRomanizeList
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.OpenRouterApiKey
import com.metrolist.music.constants.OpenRouterBaseUrlKey
import com.metrolist.music.constants.OpenRouterDefaultBaseUrl
import com.metrolist.music.constants.OpenRouterDefaultModel
import com.metrolist.music.constants.OpenRouterModelKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.RespectAgentPositioningKey
import com.metrolist.music.constants.ShowIntervalIndicatorKey
import com.metrolist.music.constants.GeminiSetupCompletedKey
import com.metrolist.music.constants.TranslateLanguageKey
import com.metrolist.music.constants.TranslateModeKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.lyrics.LyricsUtils.findActiveLineIndices
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.screens.settings.defaultList
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.SideEffect

class LyricsPillController {
    var hasTranslations by mutableStateOf(false)
    var isSelectionModeActive by mutableStateOf(false)
    var translateAction: () -> Unit = {}
    var selectionAction: () -> Unit = {}
}

@Composable
private fun LyricsPill(
    icon: Int,
    isActive: Boolean,
    enabled: Boolean = true,
    textButtonColor: Color,
    iconButtonColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isActive) textButtonColor else Color.Transparent
    val iconTint = if (isActive) iconButtonColor else textButtonColor.copy(alpha = if (enabled) 0.8f else 0.4f)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, textButtonColor.copy(alpha = if (enabled) 0.35f else 0.2f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(17.dp),
        )
    }
}

private const val LYRICS_ANCHOR_RATIO = 0.42f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 260.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private const val LYRICS_PREVIEW_TIME = 8000L

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun ExperimentalLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    onShowOptionsMenu: () -> Unit = {},
    isFullScreen: Boolean = false,
    onExitFullScreen: () -> Unit = {},
    showPills: Boolean = true,
    pillsController: LyricsPillController? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val romanizeLyricsList = rememberPreference(LyricsRomanizeList, "")
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    val showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)

    var openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, OpenRouterDefaultBaseUrl)
    val openRouterModel by rememberPreference(OpenRouterModelKey, OpenRouterDefaultModel)
    var translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    val aiSystemPrompt by rememberPreference(AiSystemPromptKey, "")

    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    val currentLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    var lastValidLyricsEntity by remember { mutableStateOf<com.metrolist.music.db.entities.LyricsEntity?>(null) }

    LaunchedEffect(currentLyricsEntity) {
        if (currentLyricsEntity != null) {
            lastValidLyricsEntity = currentLyricsEntity
        }
    }

    val lyricsEntity = remember(currentLyricsEntity, translationStatus) {
        if (currentLyricsEntity != null) {
            currentLyricsEntity
        } else if (translationStatus is LyricsTranslationHelper.TranslationStatus.Translating || translationStatus is LyricsTranslationHelper.TranslationStatus.Success) {
            lastValidLyricsEntity
        } else {
            null
        }
    }
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.ANIMATED_GRADIENT
    )

    val enabledLanguages = remember(romanizeLyricsList.value) {
        if (romanizeLyricsList.value.isEmpty()) {
            defaultList
        } else {
            romanizeLyricsList.value.split(",").map { entry ->
                val (lang, checked) = entry.split(":")
                Pair(lang, checked.toBoolean())
            }
        }.filter { it.second }.map { it.first }
    }

    val lines by lyricsViewModel.lines.collectAsState()
    val mergedLyricsList by lyricsViewModel.mergedLyricsList.collectAsState()

    LaunchedEffect(mediaMetadata?.id) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        lyricsViewModel.loadProgressiveLyrics(
            metadata,
            enabledLanguages,
            romanizeCyrillicByLine,
            showIntervalIndicator,
        )
    }

    LaunchedEffect(lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator) {
        lyricsViewModel.processLyrics(lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator)
    }

    val isSynced = remember(lyrics) { !lyrics.isNullOrEmpty() && lyrics.startsWith("[") }
    val hasWordTimings = remember(lines) { lines.any { it.words?.isNotEmpty() == true } }

    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }

    LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode) {
        if (lines.isNotEmpty() && lyricsEntity != null) {
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = lyricsEntity,
                targetLanguage = translateLanguage,
                mode = translateMode
            )
        }
    }

    var showApiSetupDialog by remember { mutableStateOf(false) }
    var showLanguagePickerDialog by remember { mutableStateOf(false) }
    var geminiSetupCompleted by rememberPreference(GeminiSetupCompletedKey, false)

    LaunchedEffect(
        lines,
        aiProvider,
        openRouterApiKey,
        deeplApiKey,
        openRouterBaseUrl,
        openRouterModel,
        translateLanguage,
        translateMode,
        deeplFormality,
        aiSystemPrompt,
        currentSong,
        database
    ) {
        LyricsTranslationHelper.manualTrigger.collectLatest {
            val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
            if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                LyricsTranslationHelper.translateLyrics(
                    lyrics = lines,
                    targetLanguage = translateLanguage,
                    apiKey = openRouterApiKey,
                    baseUrl = openRouterBaseUrl,
                    model = openRouterModel,
                    mode = translateMode,
                    scope = scope,
                    context = context,
                    provider = aiProvider,
                    deeplApiKey = deeplApiKey,
                    deeplFormality = deeplFormality,
                    useStreaming = true,
                    songId = currentSong?.id ?: "",
                    database = database,
                    systemPrompt = aiSystemPrompt,
                )
            } else if (effectiveApiKey.isBlank()) {
                showApiSetupDialog = true
            }
        }
    }

    LaunchedEffect(lines) {
        LyricsTranslationHelper.clearTranslationsTrigger.collectLatest {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        else -> Color.White
    }

    var activeLineIndices by remember { mutableStateOf(emptySet<Int>()) }
    var scrollTargetIndex by rememberSaveable { mutableIntStateOf(-1) }
    var previousScrollActiveIndices by remember { mutableStateOf(emptySet<Int>()) }

    var currentPositionState by remember { mutableLongStateOf(0L) }
    var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastPreviewTime by rememberSaveable { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val isLyricsProviderShown = lyricsEntity != null && lyricsEntity.provider != "Unknown" && lyricsEntity.provider != "Manual" && !isSelectionModeActive
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    var pillsVisible by remember { mutableStateOf(true) }
    val effectivePillsVisible by remember {
        derivedStateOf {
            if (!showPills && isSelectionModeActive) true else pillsVisible
        }
    }

    val hasTranslations by remember(lines) {
        derivedStateOf {
            lines.any { it.translatedTextFlow.value != null }
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    SideEffect {
        if (pillsController != null) {
            pillsController.hasTranslations = hasTranslations
            pillsController.isSelectionModeActive = isSelectionModeActive
            pillsController.translateAction = {
                if (hasTranslations) {
                    lyricsEntity?.let { entity ->
                        val clearedLyrics = LyricsTranslationHelper.clearTranslations(entity)
                        database.query { upsert(clearedLyrics) }
                        LyricsTranslationHelper.triggerClearTranslations()
                    }
                } else {
                    val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
                    if (effectiveApiKey.isBlank()) {
                        showApiSetupDialog = true
                    } else {
                        LyricsTranslationHelper.triggerManualTranslation()
                    }
                }
            }
            pillsController.selectionAction = {
                if (isSelectionModeActive) {
                    isSelectionModeActive = false
                    selectedIndices.clear()
                } else if (lines.isNotEmpty()) {
                    isSelectionModeActive = true
                    selectedIndices.clear()
                    val currentLine = deferredCurrentLineIndex
                    if (currentLine >= 0 && currentLine < lines.size) {
                        selectedIndices.add(currentLine)
                    }
                }
            }
        }
    }

    val maxSelectionLimit = 5
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
            showMaxSelectionToast = false
        }
    }

    var lastMainMaxSeen by remember(lyrics, lines) { mutableIntStateOf(-1) }
    var smoothPositionForSync by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lyrics, lines) {
        if (lyrics.isNullOrEmpty() || lines.isEmpty()) {
            activeLineIndices = emptySet()
            return@LaunchedEffect
        }

        var lastPlayerPos = playerConnection.player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()

        while (isActive) {
            delay(16)
            val now = System.currentTimeMillis()
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null

            val position = if (isSeeking) {
                sliderPosition!!
            } else {
                val playerPos = playerConnection.player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                val elapsed = now - lastUpdateTime
                lastPlayerPos + (if (playerConnection.player.isPlaying) elapsed else 0)
            }

            currentPositionState = position
            smoothPositionForSync = position

            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            val effectivePosition = position + lyricsOffset

            val initialActiveIndices = findActiveLineIndices(lines, effectivePosition)
            val scrollActiveIndicesRaw = findActiveLineIndices(lines, effectivePosition + (if (hasWordTimings) 0L else 250L))

            val scrollActiveIndices = scrollActiveIndicesRaw.toMutableSet()
            for (i in scrollActiveIndicesRaw) {
                if (lines.getOrNull(i)?.isBackground == true) {
                    for (j in i - 1 downTo 0) {
                        if (lines.getOrNull(j)?.isBackground == false) {
                            scrollActiveIndices.add(j)
                            break
                        }
                    }
                }
            }

            val newActiveIndices = initialActiveIndices.toMutableSet()
            for (i in initialActiveIndices) {
                if (lines.getOrNull(i)?.isBackground == true) {
                    for (j in i - 1 downTo 0) {
                        if (lines.getOrNull(j)?.isBackground == false) {
                            newActiveIndices.add(j)
                            break
                        }
                    }
                }
            }

            val scrollMax = scrollActiveIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (scrollActiveIndices.maxOrNull() ?: -1)

            val isCurrentTargetStillActive = scrollTargetIndex in scrollActiveIndices
            val anyStillActive = newActiveIndices.isNotEmpty()

            val isInGap = !anyStillActive && previousScrollActiveIndices.isNotEmpty()

            val shouldScroll = when {
                isSeeking -> true
                isInGap -> false  // silence gap: freeze everything, do not scroll ahead
                !isCurrentTargetStillActive && anyStillActive && scrollMax > scrollTargetIndex -> true
                scrollTargetIndex == -1 && anyStillActive -> true
                previousScrollActiveIndices.isEmpty() && anyStillActive && scrollMax > scrollTargetIndex -> true
                else -> false
            }

            if (shouldScroll) {
                val targetToScroll = when {
                    isSeeking -> scrollMax
                    !isCurrentTargetStillActive && anyStillActive -> scrollMax
                    else -> scrollMax
                }
                if (targetToScroll != -1 && (isSeeking || targetToScroll > scrollTargetIndex)) {
                    scrollTargetIndex = targetToScroll
                }
            }

            if (scrollMax > lastMainMaxSeen && scrollMax != -1) {
                lastMainMaxSeen = scrollMax
            }

            activeLineIndices = if (isInGap) previousScrollActiveIndices else newActiveIndices
            if (!isInGap) previousScrollActiveIndices = newActiveIndices
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LYRICS_PREVIEW_TIME)
            lastPreviewTime = 0L
        }
    }

    LaunchedEffect(scrollTargetIndex, isAutoScrollEnabled) {
        if (scrollTargetIndex != -1 && isAutoScrollEnabled) {
            deferredCurrentLineIndex = scrollTargetIndex
        }
    }

    var userManualOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isAutoScrollEnabled) {
        if (!isAutoScrollEnabled) {
            snapshotFlow { activeLineIndices }
                .collectLatest { newActiveIndices ->
                    if (newActiveIndices != previousScrollActiveIndices && newActiveIndices.isNotEmpty()) {
                        val elapsed = System.currentTimeMillis() - lastPreviewTime
                        if (elapsed >= 1500L) {
                            userManualOffset = 0f
                            isAutoScrollEnabled = true
                            lastPreviewTime = 0L
                        }
                        previousScrollActiveIndices = newActiveIndices
                    }
                }
        }
    }

    LaunchedEffect(lyrics) {
        isAutoScrollEnabled = true
        userManualOffset = 0f
        scrollTargetIndex = -1
        deferredCurrentLineIndex = 0
        isSelectionModeActive = false
        selectedIndices.clear()
        previousScrollActiveIndices = emptySet()
        pillsVisible = true
    }

    LaunchedEffect(isAutoScrollEnabled) {
        if (isAutoScrollEnabled) pillsVisible = true
    }

    LaunchedEffect(isFullScreen) {
        if (isFullScreen) pillsVisible = false
        else pillsVisible = true
    }

    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, mergedLyricsList) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, mergedLyricsList) { mutableStateOf(true) }

    val activeListIndex by remember(mergedLyricsList, deferredCurrentLineIndex) {
        derivedStateOf {
            mergedLyricsList.indexOfFirst {
                (it is LyricsListItem.Line && it.index == deferredCurrentLineIndex) ||
                        (it is LyricsListItem.Indicator && it.afterLineIndex == deferredCurrentLineIndex)
            }.coerceAtLeast(0)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = if (isFullScreen) with(density) { 192.dp.toPx() } else maxHeightPx * LYRICS_ANCHOR_RATIO
        val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }
        val indicatorHeightPx = with(density) { 72.dp.toPx() }
        val constraintLineHeightPx = with(density) { 120.dp.toPx() }

        val positions = remember(itemHeights.toMap(), activeListIndex, mergedLyricsList) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex == -1 || mergedLyricsList.isEmpty()) return@remember map

            map[activeListIndex] = 0f
            var currentY = 0f
            for (i in activeListIndex - 1 downTo 0) {
                val item = mergedLyricsList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                currentY -= (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i] = currentY
            }
            currentY = 0f
            for (i in activeListIndex until mergedLyricsList.size - 1) {
                val currentItem = mergedLyricsList[i]
                val nextItem = mergedLyricsList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                currentY += (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i + 1] = currentY
            }
            map
        }

        val minOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, anchorY) {
            if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
            val totalBelow = (activeListIndex until mergedLyricsList.size - 1).sumOf { i ->
                val currentItem = mergedLyricsList[i]
                val nextItem = mergedLyricsList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
            }.toFloat()
            val lastItem = mergedLyricsList.last()
            val lastHeight = itemHeights[mergedLyricsList.size - 1]?.toFloat() ?: (if (lastItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
            with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
        }

        val maxOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, maxHeightPx, anchorY) {
            if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
            val totalAbove = (0 until activeListIndex).sumOf { i ->
                val item = mergedLyricsList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
            }.toFloat()
            maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
        }

        val constraintBuffer = with(density) { 1000.dp.toPx() }
        val safeMinOffset = minOf(minOffset, maxOffset - maxHeightPx) - constraintBuffer
        val safeMaxOffset = maxOf(maxOffset, 0f) + constraintBuffer

        LaunchedEffect(isAutoScrollEnabled, lines) {
            if (isAutoScrollEnabled) {
                val start = userManualOffset
                if (abs(start) < 1f) {
                    userManualOffset = 0f
                    return@LaunchedEffect
                }
                val anim = Animatable(start)
                var lastValue = start
                anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                    userManualOffset += (value - lastValue)
                    lastValue = value
                }
                userManualOffset = 0f
            }
        }

        LaunchedEffect(showLyrics, lyrics, mergedLyricsList.size) {
            if (showLyrics && mergedLyricsList.isNotEmpty()) {
                isInitialLayout = true
                snapshotFlow {
                    val h = itemHeights.toMap()
                    val windowStart = (activeListIndex - 2).coerceAtLeast(0)
                    val windowEnd = (activeListIndex + 4).coerceAtMost(mergedLyricsList.size - 1)
                    (windowStart..windowEnd).all { h.containsKey(it) }
                }.first { it }
                isInitialLayout = false
            }
        }

        LaunchedEffect(activeListIndex) {
            if (mergedLyricsList.isNotEmpty()) {
                val h = itemHeights.toMap()
                val windowStart = (activeListIndex - 3).coerceAtLeast(0)
                val windowEnd = (activeListIndex + 3).coerceAtMost(mergedLyricsList.size - 1)
                val needsMeasurement = (windowStart..windowEnd).any { !h.containsKey(it) }
                if (needsMeasurement) {
                    isInitialLayout = true
                    snapshotFlow {
                        val hh = itemHeights.toMap()
                        (windowStart..windowEnd).all { hh.containsKey(it) }
                    }.first { it }
                    isInitialLayout = false
                }
            }
        }

        LyricsTranslationHeader(
            status = translationStatus,
            modifier = Modifier.align(Alignment.TopCenter).zIndex(5f).padding(top = 56.dp)
        )

        val iconButtonColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onPrimary
            else -> Color.Black
        }


        AnimatedVisibility(
            visible = effectivePillsVisible && !isFullScreen,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
            ) {
                Spacer(Modifier.fillMaxHeight(0.10f))

                if (showPills) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        LyricsPill(
                            icon = R.drawable.more_vert,
                            isActive = false,
                            textButtonColor = expressiveAccent,
                            iconButtonColor = iconButtonColor,
                            modifier = Modifier.weight(1f),
                            onClick = onShowOptionsMenu,
                        )
                        LyricsPill(
                            icon = R.drawable.translate,
                            isActive = hasTranslations,
                            textButtonColor = expressiveAccent,
                            iconButtonColor = iconButtonColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (hasTranslations) {
                                    lyricsEntity?.let { entity ->
                                        val clearedLyrics = LyricsTranslationHelper.clearTranslations(entity)
                                        database.query { upsert(clearedLyrics) }
                                        LyricsTranslationHelper.triggerClearTranslations()
                                    }
                                } else {
                                    val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
                                    if (effectiveApiKey.isBlank()) {
                                        showApiSetupDialog = true
                                    } else {
                                        LyricsTranslationHelper.triggerManualTranslation()
                                    }
                                }
                            },
                        )
                        LyricsPill(
                            icon = R.drawable.lyrics,
                            isActive = isSelectionModeActive,
                            textButtonColor = expressiveAccent,
                            iconButtonColor = iconButtonColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isSelectionModeActive) {
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                } else if (lines.isNotEmpty()) {
                                    isSelectionModeActive = true
                                    selectedIndices.clear()
                                    val currentLine = deferredCurrentLineIndex
                                    if (currentLine >= 0 && currentLine < lines.size) {
                                        selectedIndices.add(currentLine)
                                    }
                                }
                            },
                        )
                    }
                } // end if(showPills)

                // Action buttons below pills
                val anySelected = selectedIndices.isNotEmpty()
                if (showPills) {
                    AnimatedVisibility(
                        visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Transparent)
                                    .border(1.dp, expressiveAccent.copy(alpha = 0.35f), RoundedCornerShape(50))
                                    .clickable {
                                        flingJob?.cancel()
                                        var target = scrollTargetIndex
                                        if (target == -1) {
                                            target = findActiveLineIndices(lines, currentPositionState + (currentSong?.song?.lyricsOffset ?: 0)).maxOrNull() ?: -1
                                        }
                                        if (target != -1) {
                                            userManualOffset = 0f
                                            deferredCurrentLineIndex = target
                                            scrollTargetIndex = target
                                            isAutoScrollEnabled = true
                                            lastPreviewTime = 0L
                                        }
                                    }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.sync),
                                        contentDescription = null,
                                        tint = expressiveAccent.copy(alpha = 0.8f),
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.auto_scroll),
                                        color = expressiveAccent.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                } // end if(showPills)

                AnimatedVisibility(
                    visible = isSelectionModeActive,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (anySelected) expressiveAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, expressiveAccent.copy(alpha = if (anySelected) 0.6f else 0.25f), RoundedCornerShape(50))
                                .clickable(enabled = anySelected) {
                                    val text = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
                                    if (text.isNotBlank()) {
                                        shareDialogData = Triple(text, mediaMetadata?.title ?: "", mediaMetadata?.artists?.joinToString { it.name } ?: "")
                                        showShareDialog = true
                                    }
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_upward),
                                    contentDescription = null,
                                    tint = expressiveAccent.copy(alpha = if (anySelected) 0.9f else 0.4f),
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = stringResource(R.string.share_as_image),
                                    color = expressiveAccent.copy(alpha = if (anySelected) 0.9f else 0.4f),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else if (lyrics == null && (translationStatus is LyricsTranslationHelper.TranslationStatus.Idle || translationStatus is LyricsTranslationHelper.TranslationStatus.Error)) {
            Column(modifier = Modifier.padding(top = 160.dp)) {
                ShimmerHost {
                    repeat(10) {
                        Box(
                            contentAlignment = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> Alignment.CenterStart
                                LyricsPosition.CENTER -> Alignment.Center
                                else -> Alignment.CenterEnd
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                        ) { TextPlaceholder() }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(top = if (showPills && pillsVisible && !isFullScreen) 200.dp else 80.dp, bottom = 40.dp)
                    .clipToBounds()
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source == NestedScrollSource.UserInput) isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                                return super.onPostScroll(consumed, available, source)
                            }
                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (isInitialLayout) continue
                                flingJob?.cancel()
                                velocityTracker.resetTracking()
                                isAutoScrollEnabled = false
                                lastPreviewTime = System.currentTimeMillis()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                verticalDrag(down.id) { change ->
                                    val dy = change.positionChange().y
                                    if (dy < -8f) pillsVisible = false
                                    else if (dy > 8f) {
                                        pillsVisible = true
                                        if (isFullScreen) onExitFullScreen()
                                    }
                                    userManualOffset = (userManualOffset + dy).coerceIn(safeMinOffset, safeMaxOffset)
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }
                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                        val clamped = value.coerceIn(safeMinOffset, safeMaxOffset)
                                        userManualOffset = clamped
                                        if (value != clamped) cancelAnimation()
                                    }
                                }
                            }
                        }
                    }
            ) {
                val lyricsOffsetVal = (currentSong?.song?.lyricsOffset ?: 0).toLong()
                val currentEffectivePosition = currentPositionState + lyricsOffsetVal

                mergedLyricsList.forEachIndexed { listIndex, listItem ->
                    key(listItem) {
                        val distance = abs(listIndex - activeListIndex)
                        val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * lineHeightPx)
                        val frozenOffset = remember { mutableFloatStateOf(targetOffset) }
                        LaunchedEffect(isAutoScrollEnabled, targetOffset, isInitialLayout) {
                            if (isAutoScrollEnabled || isInitialLayout) frozenOffset.floatValue = targetOffset
                        }
                        val animatedOffset by animateFloatAsState(
                            targetValue = if (isAutoScrollEnabled) targetOffset else frozenOffset.floatValue,
                            animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap()
                            else tween(750, (distance * LYRICS_STAGGER_DELAY_PER_DISTANCE).coerceAtMost(LYRICS_STAGGER_DELAY_MAX_MS), FastOutSlowInEasing),
                            label = "lyricStaggeredOffset_$listIndex"
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().layout { m, c ->
                                val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                                layout(p.width, 0) { p.place(0, 0) }
                            }.offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                        ) {
                            when (listItem) {
                                is LyricsListItem.Indicator -> {
                                    val visible = currentPositionState > listItem.gapStartMs && currentPositionState < listItem.gapEndMs - 650L
                                    IntervalIndicator(
                                        listItem.gapStartMs,
                                        listItem.gapEndMs - 650L,
                                        currentPositionState,
                                        visible,
                                        expressiveAccent,
                                        Modifier.fillMaxWidth()
                                            .onSizeChanged { itemHeights[listIndex] = it.height }
                                            .padding(horizontal = 32.dp)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    )
                                }
                                is LyricsListItem.Line -> {
                                    val index = listItem.index
                                    val item = listItem.entry
                                    val isActiveLine = activeLineIndices.contains(index)
                                    val pairedMainLineIndex = if (item.isBackground) {
                                        (index - 1 downTo 0).firstOrNull { lines.getOrNull(it)?.isBackground == false } ?: -1
                                    } else -1
                                    val isInGapWithMain = if (item.isBackground && pairedMainLineIndex != -1) {
                                        val pairedMainLine = lines[pairedMainLineIndex]
                                        currentEffectivePosition > pairedMainLine.time && currentEffectivePosition < item.time
                                    } else false
                                    val bgVisible = item.isBackground && (activeLineIndices.contains(pairedMainLineIndex) || activeLineIndices.contains(index) || isInGapWithMain)
                                    LyricsLine(
                                        index = index,
                                        item = item,
                                        isSynced = isSynced,
                                        isActiveLine = isActiveLine,
                                        bgVisible = bgVisible,
                                        isSelected = selectedIndices.contains(index),
                                        isSelectionModeActive = isSelectionModeActive,
                                        currentPositionState = currentPositionState,
                                        lyricsOffset = (currentSong?.song?.lyricsOffset ?: 0).toLong(),
                                        playerConnection = playerConnection,
                                        lyricsTextSize = 40f,
                                        lyricsLineSpacing = 1.08f,
                                        expressiveAccent = expressiveAccent,
                                        lyricsTextPosition = lyricsTextPosition,
                                        respectAgentPositioning = respectAgentPositioning,
                                        isAutoScrollEnabled = isAutoScrollEnabled,
                                        displayedCurrentLineIndex = deferredCurrentLineIndex,
                                        romanizeAsMain = romanizeAsMain,
                                        enabledLanguages = enabledLanguages,
                                        romanizeLyrics = currentSong?.romanizeLyrics ?: true,
                                        lyricsBlurEnabled = true,
                                        onSizeChanged = { itemHeights[listIndex] = it },
                                        onClick = {
                                            if (isSelectionModeActive) {
                                                if (selectedIndices.contains(index)) {
                                                    selectedIndices.remove(index)
                                                    if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                                } else if (selectedIndices.size < maxSelectionLimit) {
                                                    selectedIndices.add(index)
                                                } else {
                                                    showMaxSelectionToast = true
                                                }
                                            } else if (changeLyrics && !isGuest) {
                                                if (item.time < playerConnection.player.duration - 30000L) {
                                                    playerConnection.seekTo(item.time - (currentSong?.song?.lyricsOffset ?: 0).coerceAtLeast(0))
                                                } else {
                                                    scrollTargetIndex = index
                                                    deferredCurrentLineIndex = index
                                                    isAutoScrollEnabled = true
                                                    userManualOffset = 0f
                                                    lastPreviewTime = 0L
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionModeActive) {
                                                isSelectionModeActive = true
                                                selectedIndices.add(index)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLyricsProviderShown && mergedLyricsList.isNotEmpty()) {
                    val lastIdx = mergedLyricsList.size - 1
                    val lastItemHeight = itemHeights[lastIdx]?.toFloat() ?: lineHeightPx
                    val lastItemPos = positions.getOrDefault(lastIdx, (lastIdx - activeListIndex) * lineHeightPx)
                    val targetProviderBase = anchorY + lastItemPos + lastItemHeight + with(density) { 16.dp.toPx() }
                    val frozenProviderBase = remember { mutableFloatStateOf(targetProviderBase) }
                    LaunchedEffect(isAutoScrollEnabled, targetProviderBase, isInitialLayout) {
                        if (isAutoScrollEnabled || isInitialLayout) frozenProviderBase.floatValue = targetProviderBase
                    }
                    val animatedProviderBase by animateFloatAsState(
                        targetValue = if (isAutoScrollEnabled) targetProviderBase else frozenProviderBase.floatValue,
                        animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap()
                        else tween(750, 0, FastOutSlowInEasing),
                        label = "lyricsProviderOffset"
                    )
                    Text(
                        text = stringResource(R.string.lyrics_from_provider, lyricsEntity.provider),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, (animatedProviderBase + userManualOffset).roundToInt()) }
                            .padding(horizontal = 32.dp, vertical = 4.dp)
                    )
                }

            }
        }

        if (showProgressDialog) {
            BasicAlertDialog(onDismissRequest = {}) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(Modifier.padding(32.dp)) {
                        Text("Generating image..." + "\n" + "Please wait")
                    }
                }
            }
        }

        if (showShareDialog && shareDialogData != null) {
            val (txt, title, arts) = shareDialogData!!
            LyricsShareDialog(
                txt = txt,
                title = title,
                arts = arts,
                songId = mediaMetadata?.id ?: "",
                onDismiss = { showShareDialog = false },
                onShareAsImage = {
                    showShareDialog = false
                    showColorPickerDialog = true
                }
            )
        }

        if (showColorPickerDialog && shareDialogData != null) {
            val (txt, title, arts) = shareDialogData!!
            LyricsColorPickerDialog(
                txt = txt,
                title = title,
                arts = arts,
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                lyricsTextPosition = lyricsTextPosition,
                onDismiss = { showColorPickerDialog = false },
                onShare = { bgColor, textColor, secTextColor, style ->
                    showColorPickerDialog = false
                    showProgressDialog = true
                    scope.launch {
                        try {
                            val image = ComposeToImage.createLyricsImage(
                                context,
                                mediaMetadata?.thumbnailUrl,
                                title,
                                arts,
                                txt,
                                configuration.screenWidthDp * density.density.toInt(),
                                configuration.screenHeightDp * density.density.toInt(),
                                bgColor.toArgb(),
                                when (style) {
                                    LyricsBackgroundStyle.SOLID -> LyricsBackgroundStyle.SOLID
                                    LyricsBackgroundStyle.BLUR -> LyricsBackgroundStyle.BLUR
                                    LyricsBackgroundStyle.GRADIENT -> LyricsBackgroundStyle.GRADIENT
                                },
                                textColor.toArgb(),
                                secTextColor.toArgb(),
                                when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                    LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                    else -> Layout.Alignment.ALIGN_OPPOSITE
                                }
                            )
                            val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics${System.currentTimeMillis()}")
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Share lyrics"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to create image: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            showProgressDialog = false
                        }
                    }
                }
            )
        }

        if (showApiSetupDialog) {
            if (aiProvider == "Gemini") {
                GeminiSetupDialog(
                    currentApiKey = openRouterApiKey,
                    currentLanguage = translateLanguage,
                    onDismiss = {
                        showApiSetupDialog = false
                        LyricsTranslationHelper.clearErrorStatus()
                    },
                    onSave = { key: String ->
                        openRouterApiKey = key
                    },
                    onLanguageSelected = { lang: String ->
                        translateLanguage = lang
                    },
                    onSetupCompleted = {
                        geminiSetupCompleted = true
                        showApiSetupDialog = false
                        LyricsTranslationHelper.clearErrorStatus()
                        LyricsTranslationHelper.triggerManualTranslation()
                    },
                )
            } else {
                LyricsApiSetupDialog(
                    provider = aiProvider,
                    currentApiKey = openRouterApiKey,
                    currentLanguage = translateLanguage,
                    onDismiss = {
                        showApiSetupDialog = false
                        LyricsTranslationHelper.clearErrorStatus()
                    },
                    onSave = { key ->
                        openRouterApiKey = key
                    },
                    onLanguageSelected = { lang ->
                        translateLanguage = lang
                    },
                    onSetupCompleted = {
                        showApiSetupDialog = false
                        LyricsTranslationHelper.clearErrorStatus()
                        LyricsTranslationHelper.triggerManualTranslation()
                    },
                )
            }
        }

        if (showLanguagePickerDialog) {
            LyricsLanguagePickerDialog(
                currentLanguage = translateLanguage,
                onDismiss = { showLanguagePickerDialog = false },
                onConfirm = { selectedLanguage ->
                    translateLanguage = selectedLanguage
                    showLanguagePickerDialog = false
                    LyricsTranslationHelper.triggerManualTranslation()
                }
            )
        }
    }
}