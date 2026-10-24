/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.search

import com.metrolist.music.ui.theme.drawFolioRoundRect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.R
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.ui.component.IridePageLayoutDefaults
import com.metrolist.music.ui.component.IridePageTopBar
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideArtistArtwork
import com.metrolist.music.ui.component.ArtistPhotoBlurEffects
import com.metrolist.music.ui.component.RubberBandPull
import com.metrolist.music.ui.component.IrideTypewriterCursorBlinkMillis
import com.metrolist.music.ui.component.artistPhotoBlurProgress
import com.metrolist.music.ui.component.LocalIrideFolioScrimColor
import com.metrolist.music.ui.component.LocalIridePageOverscrollPull
import com.metrolist.music.ui.component.LocalIridePageTitleVisible
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rememberArtistArtworkLoader
import com.metrolist.music.ui.component.ArtistArtworkRequest
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** One persistent field: its glyphs become the result title without replacing the focused editor. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SearchScene(
    query: TextFieldValue,
    submittedQuery: String,
    event: SearchSceneEvent,
    keyboardRequestRevision: Int,
    onMotionComplete: (Int) -> Unit,
    landingColor: Color,
    landingInk: Color,
    landingSecondary: Color,
    landingFieldColor: Color,
    resultsReady: Boolean,
    textState: TextFieldState,
    searchSource: SearchSource,
    focusRequester: FocusRequester,
    listState: LazyListState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onComplete: (String, Boolean) -> Unit,
    artistArtwork: IrideArtistArtwork? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    content: @Composable (header: @Composable () -> Unit) -> Unit,
) {
    val editing = event.mode == SearchSceneMode.Editing
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotion()
    val titleStyle = iridePageTitleTextStyle()
    val title = stringResource(R.string.search).lowercase().replaceFirstChar { it.titlecase() }
    val artworkInk = MaterialTheme.colorScheme.textPrimary
    val artworkSecondary = MaterialTheme.colorScheme.textSecondary
    val artworkFieldColor = MaterialTheme.colorScheme.fillSubtle
    val result = searchSceneHasResult(event, submittedQuery)
    val pagePull = rememberRubberBandPull()
    val interactions = remember { MutableInteractionSource() }
    val currentOpen by rememberUpdatedState(onOpen)
    val currentEditing by rememberUpdatedState(editing)
    val touchToOpen = Modifier.pointerInput(interactions) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!currentEditing) currentOpen()
            waitForUpOrCancellation()
        }
    }
    var confirmationCursorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(event.revision, reducedMotion) {
        confirmationCursorVisible = false
        if (event.confirmsSearch && !reducedMotion) {
            repeat(3) {
                confirmationCursorVisible = true
                delay(IrideTypewriterCursorBlinkMillis)
                confirmationCursorVisible = false
                delay(IrideTypewriterCursorBlinkMillis)
            }
        }
    }
    var sceneSize by remember { mutableStateOf(Size.Zero) }
    var sceneTop by remember { mutableFloatStateOf(0f) }
    val imeTargetBottom = WindowInsets.imeAnimationTarget.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget
    val ime = WindowInsets.ime
    val imeVisibleBottom = ime.getBottom(density)
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentMotionComplete by rememberUpdatedState(onMotionComplete)
    LaunchedEffect(event.revision, editing, keyboardRequestRevision) {
        if (!editing || keyboardRequestRevision != event.revision) return@LaunchedEffect
        val keyboardSession = SearchKeyboardSession()
        // The IME can consume Android Back before the activity's BackHandler sees it.
        snapshotFlow { ime.getBottom(density) to imeTarget.getBottom(density) }
            .collect { (visibleBottom, targetBottom) ->
                if (keyboardSession.dismissesEditor(visibleBottom, targetBottom)) currentDismiss()
            }
    }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val visibleHeader = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "search_header" }
    val headerVisible = visibleHeader != null
    val headerStart = listState.layoutInfo.beforeContentPadding.takeIf { it > 0 }?.toFloat()
        ?: with(density) {
            (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + IrideFolio.TitleAfterControls).toPx()
        }
    // Page motion is immediate and never changes the animation's destination.
    val pageOffset = searchArtworkPageOffset(
        visibleHeader?.offset,
        maxOf((windowHeight - sceneTop).coerceAtLeast(0f), sceneSize.width),
        listState.firstVisibleItemIndex,
    )
    val fullHeight = (windowHeight - sceneTop).coerceAtLeast(0f)
    val titleHeight = with(density) { titleStyle.lineHeight.toDp() }
    val titleGap = titleHeight + IridePageLayoutDefaults.TitleBottomSpacing
    val resultHeight = titleHeight.coerceAtLeast(52.dp) + 24.dp
    val resultHeightPx = with(density) { resultHeight.toPx() }
    val marginPx = with(density) { 20.dp.toPx() }
    val titleGapPx = with(density) { titleGap.toPx() }
    val editorHeightPx = with(density) { 52.dp.toPx() }
    val keyboardGapPx = with(density) { 24.dp.toPx() }
    var knownKeyboardBottom by remember { mutableIntStateOf(0) }
    SideEffect {
        if (imeTargetBottom > 0) knownKeyboardBottom = imeTargetBottom
    }
    // Use the IME's destination, never its changing animation frames. The field has one motion.
    val placement = searchScenePlacement(
        viewportHeight = fullHeight,
        initialFieldTop = headerStart + titleGapPx,
        editing = editing,
        hasResult = result,
        keyboardTargetBottom = imeTargetBottom.takeIf { it > 0 } ?: knownKeyboardBottom,
        keyboardFallbackReady = true,
        fieldHeight = with(density) { 52.dp.toPx() },
        resultHeight = resultHeightPx,
        gap = with(density) { 24.dp.toPx() },
    )
    val restingTop = if (result) placement.resultTop else headerStart + titleGapPx
    val restingHeightPx = if (result) resultHeightPx else editorHeightPx
    val targetPose = SearchScenePose(
        centerY = if (editing) placement.fieldTop + editorHeightPx * 0.5f else restingTop + restingHeightPx * 0.5f,
        lift = if (editing) 1f else 0f,
        result = if (result) 1f else 0f,
        funnel = if (editing || result) 1f else 0f,
    )
    val rendered = remember {
        SearchSceneSnapshot(targetPose.copy(centerY = restingTop + restingHeightPx * 0.5f + pageOffset + pagePull.offset, lift = 0f))
    }
    val confirming = event.confirmsSearch
    val motionFrom = remember(event.revision) { rendered.pose }
    val motion = remember(event.revision) {
        Animatable(
            SearchSceneFrame(motionFrom.centerY, if (event.revision == 0) 1f else 0f),
            TwoWayConverter<SearchSceneFrame, AnimationVector2D>(
                convertToVector = { AnimationVector2D(it.centerY, it.progress) },
                convertFromVector = { SearchSceneFrame(it.v1, it.v2) },
            ),
            visibilityThreshold = SearchSceneFrame(0.5f, 0.002f),
        )
    }
    var settledRevision by remember { mutableIntStateOf(-1) }
    val pageReveal = remember(event.revision) {
        Animatable(if (confirming && !resultsReady && !reducedMotion) 0f else 1f)
    }
    LaunchedEffect(pageReveal, resultsReady, reducedMotion, event.returnsToRoot) {
        if (!confirming || resultsReady) {
            if (reducedMotion) pageReveal.snapTo(1f) else pageReveal.animateTo(
                1f, tween(240, easing = LinearOutSlowInEasing),
            )
        }
    }
    val restingCenter = targetPose.centerY + pageOffset + pagePull.offset
    val currentRestingCenter = rememberUpdatedState(restingCenter)
    val destinationCenter = when {
        !editing && settledRevision == event.revision -> motion.value.centerY
        editing -> targetPose.centerY
        else -> searchSceneKeyboardSafeTop(
            restingCenter - restingHeightPx * 0.5f, imeVisibleBottom > 0,
            fullHeight, fullHeight, imeVisibleBottom, restingHeightPx, 1f, 0f,
        ) + restingHeightPx * 0.5f
    }
    val redirectVelocity = remember(motion, destinationCenter) { motion.velocity }
    LaunchedEffect(motion, reducedMotion, destinationCenter) {
        if (!editing && settledRevision == event.revision) return@LaunchedEffect
        val destination = SearchSceneFrame(destinationCenter, 1f)
        if (reducedMotion) {
            motion.snapTo(destination)
            return@LaunchedEffect
        }
        // The same spring owns position and visual progress. A late first IME target redirects
        // it with its current velocity, without a timeout, held phase, or second eased path.
        motion.animateTo(
            destination,
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = SearchMotionStiffness,
                visibilityThreshold = SearchSceneFrame(0.5f, 0.002f),
            ),
            initialVelocity = redirectVelocity,
        )
    }
    val progress = motion.value.progress.coerceIn(0f, 1f)
    val pose = searchScenePose(motionFrom, targetPose, progress, editing, confirming)
    val lift = pose.lift
    val frost = lift
    val resultProgress = pose.result
    val renderedArtwork = remember { SearchSceneArtworkSnapshot(artistArtwork) }
    val previousArtwork = remember(event.revision) { renderedArtwork.artwork }
    val sceneArtwork = artistArtwork ?: previousArtwork?.takeIf { targetPose.result == 0f && resultProgress > 0f }
    SideEffect { renderedArtwork.artwork = sceneArtwork }
    val primary = lerpColor(landingInk, artworkInk, resultProgress)
    val secondary = lerpColor(landingSecondary, artworkSecondary, resultProgress)
    val fieldColor = lerpColor(landingFieldColor, artworkFieldColor, resultProgress)
    val funnel = pose.funnel
    val collapsed = resultProgress * (1f - lift)
    val fieldHeightPx = with(density) { lerp(52.dp.toPx(), resultHeight.toPx(), collapsed) }
    val anchorTop = headerStart + (pageOffset + pagePull.offset) * (1f - lift)
    val resultRowsTop = placement.resultsTop
    val controlsBottom = with(density) {
        (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + IrideFolio.ControlsTopSpacing + IrideFolio.ControlsHeight).toPx()
    }
    val artworkBlur = with(density) {
        artistPhotoBlurProgress(
            if (headerVisible) placement.resultTop + pageOffset else if (listState.firstVisibleItemIndex > 0) Float.NEGATIVE_INFINITY else placement.resultTop,
            placement.resultTop,
            controlsBottom,
            IrideFolio.ArtistPhotoBlurLead.toPx(),
            IrideFolio.TopScrimFadeHeight.toPx(),
        )
    }
    val initialHeaderHeight = titleGap + 52.dp + IridePageLayoutDefaults.TitleBottomSpacing
    val headerHeight = with(density) {
        lerp(initialHeaderHeight.toPx(), (resultRowsTop - headerStart).coerceAtLeast(0f), resultProgress).toDp()
    }
    val currentHeaderHeight = rememberUpdatedState(headerHeight)
    val currentContent = rememberUpdatedState(content)
    val centerY = if (!editing && settledRevision == event.revision) {
        restingTop + restingHeightPx * 0.5f + pageOffset + pagePull.offset
    } else motion.value.centerY
    val proposedFieldTop = centerY - fieldHeightPx * 0.5f
    val fieldInset = marginPx
    val fieldWidth = (sceneSize.width - fieldInset * 2f).coerceAtLeast(1f)
    val iconAlpha = ((funnel - 0.48f) / 0.52f).coerceIn(0f, 1f)
    val fieldInk = lerpColor(primary, Color.White, lift)
    val fieldSecondary = lerpColor(secondary, Color.White.copy(alpha = 0.72f), lift)
    val textMeasurer = rememberTextMeasurer()
    val titleQueryStyle = remember(titleStyle.fontSize) {
        TextStyle(fontFamily = IBMPlexMonoFontFamily, fontSize = titleStyle.fontSize, fontWeight = FontWeight.Bold)
    }
    val queryWidth = remember(query.text, titleQueryStyle, textMeasurer) {
        textMeasurer.measure(query.text.ifEmpty { " " }, titleQueryStyle, maxLines = 1, softWrap = false).size.width.toFloat()
    }
    // Reserve the leading icon, trailing gutter and block cursor before fitting the title.
    val titleWidth = (fieldWidth - with(density) { 76.dp.toPx() } -
        with(density) { titleStyle.fontSize.toPx() }).coerceAtLeast(1f)
    val fittedTitleSize = (titleStyle.fontSize.value * (titleWidth / queryWidth.coerceAtLeast(1f)))
        .coerceIn(15f, titleStyle.fontSize.value.coerceAtLeast(15f))
    val fontSize = lerp(15f, fittedTitleSize, collapsed)
    val queryStyle = TextStyle(
        color = fieldInk.copy(alpha = fieldInk.alpha * if (event.returnsToRoot) 1f - progress else 1f),
        fontFamily = IBMPlexMonoFontFamily,
        fontSize = fontSize.sp,
        fontWeight = FontWeight(lerp(400f, 700f, collapsed).roundToInt()),
        textAlign = TextAlign.Start,
    )
    val fittedQueryWidth = queryWidth * fittedTitleSize / titleStyle.fontSize.value
    val titleTextLeft = ((fieldWidth - fittedQueryWidth) * 0.5f).coerceAtLeast(with(density) { 52.dp.toPx() })
    val textStart = lerp(with(density) { (16.dp + 36.dp * iconAlpha).toPx() }, titleTextLeft, collapsed)
    val impact = if (confirming) searchScenePressure(progress) else 0f
    val fieldScale = pose.scale
    // Insets are read at placement time, not from the previous composition frame.
    fun safeFieldTop(): Float = searchSceneKeyboardSafeTop(
        proposedFieldTop,
        editing || ime.getBottom(density) > 0,
        fullHeight,
        if (editing) sceneSize.height else fullHeight,
        ime.getBottom(density),
        fieldHeightPx,
        fieldScale,
        keyboardGapPx * lift * lift * lift * lift,
    )
    val fieldTop = safeFieldTop()
    SideEffect {
        rendered.pose = pose.copy(centerY = fieldTop + fieldHeightPx * 0.5f)
    }
    LaunchedEffect(event.revision, editing) {
        if (editing) return@LaunchedEffect
        snapshotFlow {
            searchSceneCanAttach(
                motion.value.progress, motion.isRunning, ime.getBottom(density),
                motion.value.centerY, currentRestingCenter.value,
            )
        }.first { it }
        settledRevision = event.revision
        currentMotionComplete(event.revision)
    }
    val iconSizePx = with(density) { 48.dp.toPx() }
    val iconTop = fieldTop + (fieldHeightPx - iconSizePx) * 0.5f
    val iconLeft = fieldInset + with(density) { 4.dp.toPx() }
    val titleConvergence = ((funnel - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val titleScale = lerp(1f, 0.12f, titleConvergence)

    val suggestionsModel: OnlineSearchSuggestionViewModel =
        hiltViewModel(key = "searchSceneSuggestions")
    val suggestions by suggestionsModel.viewState.collectAsState()
    val artworkLoader = rememberArtistArtworkLoader()
    val predictedArtist = remember(query.text, suggestions.items, editing) {
        if (!editing || query.text.isBlank() || searchGenreArtworkName(query.text) != null) null else suggestions.items.filterIsInstance<ArtistItem>()
            .mapNotNull { artist -> searchPredictionScore(query.text, artist.title)?.let { it to artist } }
            .minByOrNull { it.first }?.second
    }
    LaunchedEffect(predictedArtist?.id) {
        predictedArtist?.let {
            artworkLoader.prefetch(
                ArtistArtworkRequest(
                    it.id, it.title, it.thumbnail?.resize(1200, 1200), it.channelId,
                    it.id.startsWith("UC") && !it.isProfile,
                ),
            )
        }
    }
    LaunchedEffect(query.text, editing, searchSource) {
        suggestionsModel.previewResultsEnabled = editing && searchSource == SearchSource.ONLINE
        suggestionsModel.query.value = if (suggestionsModel.previewResultsEnabled) query.text else ""
    }

    Box(
        Modifier.fillMaxSize().background(landingColor)
            .onSizeChanged { sceneSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { sceneTop = it.positionInWindow().y },
    ) {
        SearchScenePage(
            sceneArtwork, pageOffset, artworkBlur, pagePull, editing, motion,
            motionFrom.lift, targetPose.lift, motionFrom.result, targetPose.result,
            event.returnsToRoot && submittedQuery.isNotBlank(), pageReveal, currentHeaderHeight, currentContent,
        )
        if (lift > 0f || editing || frost > 0f) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f * frost))
                    .clickable(
                        enabled = editing,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        if (navigationIcon != null) {
            Box(Modifier.graphicsLayer { alpha = resultProgress }) {
                IridePageTopBar(title = title, progress = 0f, navigationIcon = navigationIcon)
            }
        }
        // Both resting headers follow the page; only the open editor detaches from it.
        // Keep the native field and FocusRequester attached even when its lazy header is recycled.
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = if (headerVisible || editing || lift > 0f || motion.isRunning || progress < 1f) 1f else 0f
        }) {
            Text(
                text = title,
                style = titleStyle,
                color = primary,
                modifier = Modifier.offset {
                    IntOffset(
                        lerp(marginPx, iconLeft, titleConvergence).roundToInt(),
                        lerp(anchorTop, iconTop + with(density) { 4.dp.toPx() }, titleConvergence)
                            .roundToInt(),
                    )
                }.blur((3f * titleConvergence).dp).graphicsLayer {
                    transformOrigin = TransformOrigin(0.12f, 0.5f)
                    scaleX = titleScale
                    scaleY = titleScale * (1f - 0.3f * titleConvergence)
                    alpha = 1f - titleConvergence
                }.drawWithContent {
                    val throat = size.width * (1f - 0.88f * titleConvergence)
                    val funnelPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(throat, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    clipPath(funnelPath) { this@drawWithContent.drawContent() }
                }.then(touchToOpen).clickable(
                    enabled = !editing && !result,
                    interactionSource = interactions,
                    indication = null,
                    onClick = onOpen,
                ).then(
                    if (editing || result) Modifier.clearAndSetSemantics {}
                    else Modifier.semantics { heading() },
                ),
            )
            Box(
                Modifier.offset {
                    val top = safeFieldTop()
                    rendered.pose = pose.copy(centerY = top + fieldHeightPx * 0.5f)
                    IntOffset(fieldInset.roundToInt(), top.roundToInt())
                }
                    .width(with(density) { fieldWidth.toDp() })
                    .height(with(density) { fieldHeightPx.toDp() })
                    .then(touchToOpen)
                    .graphicsLayer {
                        translationY = safeFieldTop().let { it - it.roundToInt() }
                        scaleX = fieldScale
                        scaleY = fieldScale
                        shadowElevation = with(density) { 38.dp.toPx() } * lift
                        ambientShadowColor = Color.Black.copy(alpha = 0.7f)
                        spotShadowColor = Color.Black.copy(alpha = 0.8f)
                        shape = FolioShape(18.dp)
                        clip = false
                    }.background(Color.Black.copy(alpha = 0.72f * lift), FolioShape(18.dp))
                    .background(fieldColor, FolioShape(18.dp))
                    .drawWithContent {
                        drawContent()
                        if (lift > 0f) {
                            drawFolioRoundRect(
                                color = primary.copy(alpha = 0.06f * lift),
                                cornerRadius = CornerRadius(18.dp.toPx()),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                        if (impact > 0f) {
                            drawFolioRoundRect(
                                color = Color.Black.copy(alpha = 0.05f * impact),
                                cornerRadius = CornerRadius(18.dp.toPx()),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                SearchTextField(
                    state = textState,
                    editing = editing,
                    confirmationCursorVisible = confirmationCursorVisible,
                    textStyle = queryStyle,
                    placeholder = if (collapsed < 0.5f) stringResource(
                        if (searchSource == SearchSource.LOCAL) R.string.search_library
                        else R.string.iride_search_online_hint,
                    ) else "",
                    placeholderStyle = TextStyle(
                        color = fieldSecondary, fontFamily = IBMPlexMonoFontFamily, fontSize = 15.sp,
                    ),
                    onSubmit = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                        .padding(
                            start = with(density) { textStart.toDp() },
                            end = 52.dp - 28.dp * collapsed,
                        )
                        .focusRequester(focusRequester)
                        .focusProperties { canFocus = editing && keyboardRequestRevision == event.revision }
                        .then(if (result && !editing) Modifier.semantics { heading() } else Modifier),
                )
                if (!editing) {
                    Box(Modifier.matchParentSize().clickable(
                        interactionSource = interactions, indication = null, onClick = onOpen,
                    ))
                }
                val arrowAlpha = searchSceneArrowAlpha(lift) * (1f - collapsed)
                if (arrowAlpha > 0.001f) {
                    Box(
                        Modifier.align(Alignment.CenterEnd).size(48.dp)
                            .graphicsLayer { alpha = arrowAlpha }.clickable(
                            enabled = editing,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onSubmit(textState.text.toString()) },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.arrow_forward), title, tint = fieldInk)
                    }
                }
            }
            Box(
                modifier = Modifier.offset {
                    IntOffset(iconLeft.roundToInt(), (safeFieldTop() + (fieldHeightPx - iconSizePx) * 0.5f).roundToInt())
                }.size(48.dp).then(touchToOpen).graphicsLayer {
                    translationY = (safeFieldTop() + (fieldHeightPx - iconSizePx) * 0.5f).let { it - it.roundToInt() }
                    alpha = iconAlpha
                    translationX = (iconLeft + iconSizePx * 0.5f - fieldInset - fieldWidth * 0.5f) * (fieldScale - 1f)
                    scaleX = lerp(0.78f, 1f, iconAlpha) * fieldScale
                    scaleY = scaleX
                }.clickable(
                    enabled = iconAlpha > 0.8f,
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpen,
                ).then(if (iconAlpha < 0.8f) Modifier.clearAndSetSemantics {} else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.search),
                    contentDescription = title,
                    tint = fieldSecondary.copy(alpha = fieldSecondary.alpha * lerp(0.95f, 0.72f, collapsed)),
                    modifier = Modifier.size(24.dp),
                )
            }
            if (editing && searchSource == SearchSource.ONLINE && lift > 0.65f) {
                val recallGapPx = with(density) { 12.dp.toPx() }
                val availableRecallHeight = (fieldTop - headerStart - recallGapPx * 2f).coerceAtLeast(0f)
                Box(
                    Modifier.offset {
                        IntOffset(0, (fieldTop - recallGapPx - availableRecallHeight).roundToInt())
                    }.fillMaxWidth().height(with(density) { availableRecallHeight.toDp() })
                        .graphicsLayer { alpha = ((lift - 0.65f) / 0.35f).coerceIn(0f, 1f) },
                ) {
                    SearchRecallPanel(
                        query = query.text,
                        submittedQuery = submittedQuery,
                        suggestions = suggestions.suggestions,
                        predictionItems = suggestions.items,
                        availableHeight = availableRecallHeight,
                        onComplete = onComplete,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/** Reopening during a return must keep fading the photo already visible, even after data clears. */
private class SearchSceneArtworkSnapshot(var artwork: IrideArtistArtwork?)

/** Changing foreground frames invalidates only the page's render layer and header spacer. */
@Composable
private fun SearchScenePage(
    artwork: IrideArtistArtwork?,
    pageOffset: Float,
    artworkBlur: Float,
    pagePull: RubberBandPull,
    editing: Boolean,
    motion: Animatable<SearchSceneFrame, AnimationVector2D>,
    fromLift: Float,
    toLift: Float,
    fromResult: Float,
    toResult: Float,
    returningToRoot: Boolean,
    pageReveal: Animatable<Float, *>,
    headerHeight: State<Dp>,
    content: State<@Composable (header: @Composable () -> Unit) -> Unit>,
) {
    val density = LocalDensity.current
    val effects = remember(density) { ArtistPhotoBlurEffects(with(density) { 28.dp.toPx() }) }
    val header = remember(headerHeight) {
        @Composable { Spacer(Modifier.fillMaxWidth().height(headerHeight.value)) }
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val lift = lerp(fromLift, toLift, motion.value.progress.coerceIn(0f, 1f))
            renderEffect = effects.at(searchSceneBlurRadius(editing, lift) / 28f)
            alpha = 1f - 0.28f * lift
            scaleX = 1f - 0.025f * lift
            scaleY = scaleX
        }.then(if (editing) Modifier.clearAndSetSemantics {} else Modifier),
    ) {
        artwork?.let {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = lerp(fromResult, toResult, motion.value.progress.coerceIn(0f, 1f))
            }) { SearchArtistBackdrop(it, pageOffset, artworkBlur, pagePull) }
        }
        CompositionLocalProvider(
            LocalIridePageTitleVisible provides false,
            LocalIridePageOverscrollPull provides pagePull,
            LocalIrideFolioScrimColor provides (artwork?.tint ?: Color.Black),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = pageReveal.value * if (returningToRoot) {
                    1f - motion.value.progress.coerceIn(0f, 1f)
                } else 1f
                translationY = with(density) { 10.dp.toPx() } * (1f - pageReveal.value)
            }) {
                content.value(header)
            }
        }
    }
}
