/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.WordTimestamp
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.theme.SatoshiFontFamily
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

private const val WAVE_ATTACK_MS = 350f
private const val WAVE_RELEASE_MS = 220f
private const val WAVE_MAX_AMP_DP = 2.2f
private const val WAVE_DURATION_MIN_MS = 1000f
private const val WAVE_DURATION_FULL_MS = 1600f
private const val WAVE_ATTACK_RELEASE_SPAN_FRACTION = 0.35f

private fun easeOutCubic(t: Float): Float {
    val inv = 1f - t
    return 1f - inv * inv * inv
}

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

private const val LONG_WORD_GLOW_CHARS = 9
private const val LONG_WORD_GLOW_BOOST = 0.22f

private const val NOT_STARTED_ALPHA_FACTOR = 0.82f

private const val SWEEP_FEATHER_FRACTION = 0.35f
private const val SWEEP_FEATHER_MIN_DP = 8f
private const val SWEEP_FEATHER_MAX_DP = 32f

private const val BLOOM_MIN_CHARS = 6
private const val BLOOM_MAX_CHARS = 14
private const val BLOOM_MAX_ALPHA = 0.45f
private const val BLOOM_MIN_RADIUS_DP = 4f
private const val BLOOM_MAX_RADIUS_DP = 16f

private fun String.containsRtl(): Boolean {
    for (c in this) {
        val directionality = Character.getDirectionality(c).toInt()
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt() ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt()
        ) {
            return true
        }
    }
    return false
}

private fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(substring(start, end))
        start = end
        end = it.next()
    }
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LyricsLine(
    index: Int,
    item: LyricsEntry,
    isSynced: Boolean,
    isActiveLine: Boolean,
    bgVisible: Boolean,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    currentPositionState: Long,
    lyricsOffset: Long,
    playerConnection: PlayerConnection,
    lyricsTextSize: Float,
    lyricsLineSpacing: Float,
    expressiveAccent: Color,
    lyricsTextPosition: LyricsPosition,
    respectAgentPositioning: Boolean,
    isAutoScrollEnabled: Boolean,
    displayedCurrentLineIndex: Int,
    romanizeAsMain: Boolean,
    enabledLanguages: List<String>,
    romanizeLyrics: Boolean,
    onSizeChanged: (Int) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    lyricsBlurEnabled: Boolean = true
) {

    val itemModifier = modifier
        .fillMaxWidth()
        .onSizeChanged { onSizeChanged(it.height) }
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
        .padding(
            top = if (item.isBackground) 0.dp else 8.dp,
            bottom = if (item.isBackground) 2.dp else 8.dp
        )

    val blurRadius by animateFloatAsState(
        targetValue = if (!lyricsBlurEnabled || !isSynced || isActiveLine || !isAutoScrollEnabled || displayedCurrentLineIndex < 0) {
            0f
        } else {
            val distance = abs(index - displayedCurrentLineIndex)
            when (distance) {
                0 -> 0f
                1 -> 4f
                2 -> 6f
                else -> 14f
            }
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 400f
        ),
        label = "lyricsBlurRadius"
    )

    val agentAlignment = when {
        respectAgentPositioning && item.agent == "v1" -> Alignment.Start
        respectAgentPositioning && item.agent == "v2" -> Alignment.End
        respectAgentPositioning && item.agent == "v1000" -> Alignment.CenterHorizontally
        item.isBackground -> Alignment.CenterHorizontally
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    }

    val agentTextAlign = when {
        respectAgentPositioning && item.agent == "v1" -> TextAlign.Left
        respectAgentPositioning && item.agent == "v2" -> TextAlign.Right
        respectAgentPositioning && item.agent == "v1000" -> TextAlign.Center
        item.isBackground -> TextAlign.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }
    }

    Box(modifier = itemModifier
        .then(if (blurRadius > 0.5f) Modifier.blur(blurRadius.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
        .padding(
            start = when (agentAlignment) {
                Alignment.End -> 32.dp
                Alignment.CenterHorizontally -> 20.dp
                else -> 8.dp
            },
            end = when (agentAlignment) {
                Alignment.End -> 8.dp
                Alignment.CenterHorizontally -> 20.dp
                else -> 32.dp
            }
        ),
        contentAlignment = when {
        respectAgentPositioning && item.agent == "v1" -> Alignment.CenterStart
        respectAgentPositioning && item.agent == "v2" -> Alignment.CenterEnd
        item.isBackground -> Alignment.Center
        respectAgentPositioning && item.agent == "v1000" -> Alignment.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.CenterStart
            LyricsPosition.RIGHT -> Alignment.CenterEnd
            LyricsPosition.CENTER -> Alignment.Center
        }
    }) {
        @Composable
        fun LyricContent() {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = agentAlignment) {
                val inactiveAlpha = if (item.isBackground) 0.15f else 0.38f
                val activeAlpha = 1f
                val focusedAlpha = if (item.isBackground) 0.5f else 0.3f

                val targetAlpha = if (!isSynced || item.isBackground || isActiveLine) {
                    activeAlpha
                } else if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                    when (abs(index - displayedCurrentLineIndex)) {
                        0 -> focusedAlpha
                        1 -> 0.2f; 2 -> 0.2f; 3 -> 0.15f; 4 -> 0.1f; else -> 0.08f
                    }
                } else inactiveAlpha
                val animatedAlpha by animateFloatAsState(targetAlpha, tween(500, easing = FastOutSlowInEasing), label = "lyricsLineAlpha")
                val lineColor = expressiveAccent.copy(alpha = if (item.isBackground) focusedAlpha else animatedAlpha)

                val romanizedTextState by item.romanizedTextFlow.collectAsState()
                val isRomanizedAvailable = romanizedTextState != null
                val mainTextRaw = if (romanizeAsMain && isRomanizedAvailable) romanizedTextState else item.text
                val subTextRaw = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedTextState
                val mainText = if (item.isBackground) mainTextRaw?.removePrefix("(")?.removeSuffix(")") else mainTextRaw
                val subText = if (item.isBackground) subTextRaw?.removePrefix("(")?.removeSuffix(")") else subTextRaw

                val lyricStyle = TextStyle(
                    fontFamily = SatoshiFontFamily,
                    fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal,
                    lineHeight = if (item.isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,
                    letterSpacing = TextUnit.Unspecified,
                    textAlign = agentTextAlign,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                )

                val effectiveWords = if (item.words?.isNotEmpty() == true) {
                    item.words
                } else if (mainText != null) {
                    remember(mainText, item.time) {
                        val words = mainText.split(Regex("\\s+")).filter { it.isNotBlank() }
                        val wordDurationSec = 0.18
                        val wordStaggerSec = 0.03
                        val startTimeSec = item.time / 1000.0
                        words.mapIndexed { idx, wordText ->
                            WordTimestamp(
                                text = wordText,
                                startTime = startTimeSec + (idx * wordStaggerSec),
                                endTime = startTimeSec + (idx * wordStaggerSec) + wordDurationSec,
                                hasTrailingSpace = idx < words.size - 1
                            )
                        }
                    }
                } else null

                val hasWordTimings = item.words?.isNotEmpty() == true
                if (isSynced && hasWordTimings && effectiveWords != null && (isActiveLine || abs(index - displayedCurrentLineIndex) <= 3) && mainText != null) {
                    WordLevelLyrics(
                        mainText = mainText,
                        words = effectiveWords,
                        isActiveLine = isActiveLine,
                        currentPositionState = currentPositionState,
                        lyricsOffset = lyricsOffset,
                        playerConnection = playerConnection,
                        lyricStyle = lyricStyle,
                        lineColor = lineColor,
                        expressiveAccent = expressiveAccent,
                        isBackground = item.isBackground,
                        focusedAlpha = focusedAlpha,
                        alignment = agentTextAlign
                    )
                } else if (isSynced && !hasWordTimings && (isActiveLine || abs(index - displayedCurrentLineIndex) <= 3) && mainText != null) {
                    LineFadeAnimation(
                        mainText = mainText,
                        isActiveLine = isActiveLine,
                        lyricStyle = lyricStyle,
                        lineColor = lineColor,
                        expressiveAccent = expressiveAccent,
                        focusedAlpha = focusedAlpha,
                        alignment = agentTextAlign,
                        isWordSync = false
                    )
                } else {
                    Text(
                        text = mainText ?: "",
                        style = lyricStyle.copy(color = if (isActiveLine) expressiveAccent else lineColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (romanizeLyrics && enabledLanguages.isNotEmpty()) {
                    subText?.let {
                        Text(
                            text = it,
                            fontSize = 18.sp,
                            color = expressiveAccent.copy(alpha = 0.6f),
                            textAlign = agentTextAlign,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                val transText by item.translatedTextFlow.collectAsState()
                transText?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = expressiveAccent.copy(alpha = 0.5f),
                        textAlign = agentTextAlign,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (item.isBackground) {
            AnimatedVisibility(
                visible = bgVisible,
                enter = fadeIn(tween(durationMillis = 250, delayMillis = 100)),
                exit = fadeOut(tween(250))
            ) {
                LyricContent()
            }
        } else {
            LyricContent()
        }
    }
}

@Composable
private fun WordLevelLyrics(
    mainText: String,
    words: List<WordTimestamp>,
    isActiveLine: Boolean,
    currentPositionState: Long,
    lyricsOffset: Long,
    playerConnection: PlayerConnection,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    isBackground: Boolean,
    focusedAlpha: Float,
    alignment: TextAlign
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var smoothPosition by remember { mutableLongStateOf(currentPositionState + lyricsOffset) }

    LaunchedEffect(isActiveLine) {
        if (isActiveLine) {
            var lastPlayerPos = playerConnection.player.currentPosition
            var lastUpdateTime = System.currentTimeMillis()
            while (isActive) {
                withFrameMillis {
                    val now = System.currentTimeMillis()
                    val playerPos = playerConnection.player.currentPosition
                    if (playerPos != lastPlayerPos) {
                        lastPlayerPos = playerPos
                        lastUpdateTime = now
                    }
                    val elapsed = now - lastUpdateTime
                    smoothPosition = lastPlayerPos + lyricsOffset + (if (playerConnection.player.isPlaying) elapsed else 0)
                }
            }
        }
    }

    LaunchedEffect(isActiveLine, currentPositionState) {
        if (!isActiveLine) {
            smoothPosition = currentPositionState + lyricsOffset
        }
    }

    val (effectiveWords, effectiveToOriginalIdx) = remember(words, isBackground) {
        words.flatMapIndexed { originalIdx, word ->
            val shouldSplit = word.text.contains('-') && word.text.length > 1 &&
                    (!word.hasTrailingSpace || words.size == 1)
            if (shouldSplit) {
                val segments = mutableListOf<String>()
                var start = 0
                for (i in 0 until word.text.length) {
                    if (word.text[i] == '-') {
                        segments.add(word.text.substring(start, i + 1))
                        start = i + 1
                    }
                }
                if (start < word.text.length) {
                    segments.add(word.text.substring(start))
                }

                if (segments.size > 1) {
                    val totalDuration = word.endTime - word.startTime
                    val segmentDuration = totalDuration / segments.size
                    segments.mapIndexed { index, segmentText ->
                        WordTimestamp(
                            text = segmentText,
                            startTime = word.startTime + index * segmentDuration,
                            endTime = word.startTime + (index + 1) * segmentDuration,
                            hasTrailingSpace = if (index == segments.size - 1) word.hasTrailingSpace else false
                        ) to originalIdx
                    }
                } else listOf(word to originalIdx)
            } else listOf(word to originalIdx)
        }.let { data -> data.map { it.first } to data.map { it.second } }
    }

    val graphemeClusters = remember(mainText) { mainText.toGraphemeClusters() }
    val clusterCount = graphemeClusters.size
    val clusterCharOffsets = remember(mainText) {
        IntArray(clusterCount).also { offsets ->
            var charOffset = 0
            graphemeClusters.forEachIndexed { i, cluster ->
                offsets[i] = charOffset
                charOffset += cluster.length
            }
        }
    }

    val charToWordData = remember(mainText, effectiveWords, isBackground, graphemeClusters, clusterCharOffsets) {
        val wordIdxMap = IntArray(clusterCount) { -1 }
        val charInWordMap = IntArray(clusterCount)
        val wordLenMap = IntArray(clusterCount) { 1 }
        var currentPos = 0
        var clCursor = 0
        effectiveWords.forEachIndexed { wordIdx, word ->
            val rawWordText = word.text.let {
                if (isBackground) {
                    var t = it
                    if (wordIdx == 0) t = t.removePrefix("(")
                    if (wordIdx == effectiveWords.size - 1) t = t.removeSuffix(")")
                    t
                } else it
            }
            val indexInMain = mainText.indexOf(rawWordText, currentPos)
            if (indexInMain != -1) {
                val wordEndInMain = indexInMain + rawWordText.length
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < indexInMain) clCursor++
                val wordClusterIndices = mutableListOf<Int>()
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < wordEndInMain) {
                    wordClusterIndices.add(clCursor++)
                }
                val wordClusterLen = wordClusterIndices.size
                wordClusterIndices.forEachIndexed { posInWord, clIdx ->
                    wordIdxMap[clIdx] = wordIdx
                    charInWordMap[clIdx] = posInWord
                    wordLenMap[clIdx] = wordClusterLen
                }
                if (clCursor < clusterCount && clusterCharOffsets[clCursor] == wordEndInMain &&
                    wordEndInMain < mainText.length && mainText[wordEndInMain] == ' ') {
                    wordIdxMap[clCursor] = wordIdx
                    charInWordMap[clCursor] = wordClusterLen
                    wordLenMap[clCursor] = wordClusterLen + 1
                    clCursor++
                }
                currentPos = wordEndInMain
            }
        }
        Triple(wordIdxMap, charInWordMap, wordLenMap)
    }

    val waveMaxAmpPx = with(density) { WAVE_MAX_AMP_DP.dp.toPx() }
    val descentPadPx = with(density) { lyricStyle.fontSize.toPx() * 0.12f }

    val remappedWordTimesMs = remember(effectiveWords) {
        if (effectiveWords.size <= 1) {
            effectiveWords.map { w ->
                (w.startTime * 1000).toLong() to (w.endTime * 1000).toLong()
            }
        } else {
            val durations = effectiveWords.map { it.endTime - it.startTime }
            val mean = durations.average()
            val adjusted = durations.map { d -> d + (mean - d) * 0.5 }
            val scale = durations.sum() / adjusted.sum()
            val normalized = adjusted.map { it * scale }
            var currentMs = (effectiveWords.first().startTime * 1000).toLong()
            normalized.map { dur ->
                val startMs = currentMs
                val endMs = currentMs + (dur * 1000).toLong()
                currentMs = endMs
                startMs to endMs
            }
        }
    }

    val sweepFeatherMinPx = with(density) { SWEEP_FEATHER_MIN_DP.dp.toPx() }
    val sweepFeatherMaxPx = with(density) { SWEEP_FEATHER_MAX_DP.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(mainText, maxWidthPx, lyricStyle, alignment) {
            val unconstrained = textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(maxWidth = Int.MAX_VALUE),
                softWrap = false
            )
            val naturalWidth = unconstrained.size.width
            val layoutWidth = if (alignment == TextAlign.Left) {
                minOf(naturalWidth, (maxWidthPx * 0.9f).toInt())
            } else {
                maxWidthPx
            }
            textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(minWidth = layoutWidth, maxWidth = layoutWidth),
                softWrap = true
            )
        }

        val isRtlText = remember(mainText) { mainText.containsRtl() }

        val wordBoundsPx = remember(layoutResult, charToWordData, clusterCharOffsets, effectiveWords.size) {
            val (wordIdxMap, _, _) = charToWordData
            val bounds = Array(effectiveWords.size) {
                floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            }
            for (k in clusterCharOffsets.indices) {
                val wIdx = wordIdxMap[k]
                if (wIdx == -1) continue
                val b = layoutResult.getBoundingBox(clusterCharOffsets[k])
                val bb = bounds[wIdx]
                val bLeft = minOf(b.left, b.right)
                val bRight = maxOf(b.left, b.right)
                if (bLeft < bb[0]) bb[0] = bLeft
                if (b.top < bb[1]) bb[1] = b.top
                if (bRight > bb[2]) bb[2] = bRight
                if (b.bottom > bb[3]) bb[3] = b.bottom
            }
            bounds
        }

        fun activeWordIndex(): Int = remappedWordTimesMs.indexOfFirst { (s, e) -> smoothPosition in s..e }

        fun bloomIntensity(idx: Int): Float {
            if (idx == -1) return 0f
            val originalWord = words[effectiveToOriginalIdx[idx]]
            val len = originalWord.text.length
            if (len < BLOOM_MIN_CHARS) return 0f
            val lengthFactor = ((len - BLOOM_MIN_CHARS).toFloat() / (BLOOM_MAX_CHARS - BLOOM_MIN_CHARS)).coerceIn(0f, 1f)
            val (s, e) = remappedWordTimesMs[idx]
            val progress = ((smoothPosition - s).toFloat() / (e - s).coerceAtLeast(1)).coerceIn(0f, 1f)
            val shape = sin(PI.toFloat() * progress).coerceIn(0f, 1f)
            return (0.35f + 0.65f * lengthFactor) * shape
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layoutResult.size.height.toDp() })
        ) {
            if (isActiveLine && !isRtlText && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val idx = activeWordIndex()
                            val intensity = bloomIntensity(idx)
                            alpha = (intensity * BLOOM_MAX_ALPHA).coerceIn(0f, 1f)
                            renderEffect = if (intensity > 0.01f) {
                                val radiusPx = with(density) {
                                    (BLOOM_MIN_RADIUS_DP + intensity * (BLOOM_MAX_RADIUS_DP - BLOOM_MIN_RADIUS_DP)).dp.toPx()
                                }
                                android.graphics.RenderEffect.createBlurEffect(
                                    radiusPx, radiusPx, android.graphics.Shader.TileMode.DECAL
                                ).asComposeRenderEffect()
                            } else null
                        }
                ) {
                    val idx = activeWordIndex()
                    if (idx != -1) {
                        val bb = wordBoundsPx[idx]
                        if (bb[2] > bb[0]) {
                            clipRect(left = bb[0], top = bb[1], right = bb[2], bottom = bb[3]) {
                                drawText(layoutResult, color = expressiveAccent)
                            }
                        }
                    }
                }
            }

            Canvas(modifier = Modifier
                .matchParentSize()
                .graphicsLayer(clip = false)
            ) {
                if (mainText.isEmpty()) return@Canvas
                if (!isActiveLine) {
                    drawText(layoutResult, color = lineColor)
                } else {
                    if (isRtlText) {
                        val (wordIdxMap, _, _) = charToWordData
                        val wordFactors = effectiveWords.mapIndexed { wordIdx, word ->
                            val (wStartMs, wEndMs) = remappedWordTimesMs[wordIdx]
                            val isWordSung = smoothPosition > wEndMs
                            val isWordActive = smoothPosition in wStartMs..wEndMs
                            val sungFactor = if (isWordSung) 1f
                            else if (smoothPosition >= wStartMs) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                            else 0f
                            Triple(sungFactor, isWordSung, isWordActive)
                        }

                        drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))

                        effectiveWords.indices.forEach { wIdx ->
                            val (sungFactor, isWordSung, isWordActive) = wordFactors[wIdx]

                            var left = Float.MAX_VALUE
                            var right = Float.MIN_VALUE
                            var top = Float.MAX_VALUE
                            var bottom = Float.MIN_VALUE
                            var found = false

                            for (i in 0 until clusterCount) {
                                if (wordIdxMap[i] == wIdx) {
                                    val bounds = layoutResult.getBoundingBox(clusterCharOffsets[i])
                                    left = minOf(left, bounds.left, bounds.right)
                                    right = maxOf(right, bounds.left, bounds.right)
                                    top = minOf(top, bounds.top)
                                    bottom = maxOf(bottom, bounds.bottom)
                                    found = true
                                }
                            }

                            if (found) {
                                if (isWordSung) {
                                    clipRect(left = left, top = top, right = right, bottom = bottom) {
                                        drawText(layoutResult, color = expressiveAccent)
                                    }
                                } else if (isWordActive && sungFactor > 0f) {
                                    clipRect(left = left, top = top, right = right, bottom = bottom) {
                                        drawText(layoutResult, color = expressiveAccent.copy(alpha = focusedAlpha + (1f - focusedAlpha) * sungFactor))
                                    }
                                }
                            }
                        }
                        return@Canvas
                    }

                    val (wordIdxMap, _, _) = charToWordData
                    val wordFactors = effectiveWords.mapIndexed { wordIdx, word ->
                        val (wStartMs, wEndMs) = remappedWordTimesMs[wordIdx]
                        val isWordSung = smoothPosition > wEndMs
                        val sungFactor = if (isWordSung) 1f
                        else if (smoothPosition >= wStartMs) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                        else 0f
                        Triple(sungFactor, word, isWordSung)
                    }

                    for (i in 0 until clusterCount) {
                        val charBounds = layoutResult.getBoundingBox(clusterCharOffsets[i])
                        val wordIdx = wordIdxMap[i]
                        val hasDescender = graphemeClusters[i].any { c -> c.lowercaseChar() in "gjpqy" }

                        if (wordIdx == -1) {
                            val unmappedLeft = minOf(charBounds.left, charBounds.right)
                            val unmappedRight = maxOf(charBounds.left, charBounds.right)
                            clipRect(left = unmappedLeft, top = charBounds.top, right = unmappedRight, bottom = charBounds.bottom + (if (hasDescender) descentPadPx else 0f)) {
                                drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))
                            }
                            continue
                        }

                        val (sungFactor, _, isWordSung) = wordFactors[wordIdx]
                        val originalWord = words[effectiveToOriginalIdx[wordIdx]]
                        val bb = wordBoundsPx[wordIdx]
                        val wLeft = bb[0]
                        val wRight = bb[2]
                        val wordWidthPx = (wRight - wLeft).coerceAtLeast(1f)

                        val isLongWord = originalWord.text.length >= LONG_WORD_GLOW_CHARS
                        val longWordGlow = if (isLongWord && !isWordSung) sungFactor * LONG_WORD_GLOW_BOOST else 0f
                        val dimAlpha = (focusedAlpha * NOT_STARTED_ALPHA_FACTOR + longWordGlow).coerceAtMost(1f)

                        var waveOffset = 0f
                        if (!isWordSung && sungFactor > 0f) {
                            val spanStartMs = (originalWord.startTime * 1000).toLong()
                            val spanEndMs = (originalWord.endTime * 1000).toLong()
                            val spanDurationMs = (spanEndMs - spanStartMs).toFloat().coerceAtLeast(1f)
                            if (spanDurationMs > WAVE_DURATION_MIN_MS) {
                                val timeIntoWord = (smoothPosition - spanStartMs).toFloat()
                                val timeToEnd = (spanEndMs - smoothPosition).toFloat()
                                val attackMs = minOf(WAVE_ATTACK_MS, spanDurationMs * WAVE_ATTACK_RELEASE_SPAN_FRACTION)
                                val releaseMs = minOf(WAVE_RELEASE_MS, spanDurationMs * WAVE_ATTACK_RELEASE_SPAN_FRACTION)
                                val rawAttack = (timeIntoWord / attackMs).coerceIn(0f, 1f)
                                val rawRelease = (timeToEnd / releaseMs).coerceIn(0f, 1f)
                                val attack = easeOutCubic(rawAttack)
                                val release = smoothstep(rawRelease)
                                val envelope = attack * release
                                if (envelope > 0.001f) {
                                    val durationWeight = smoothstep(
                                        ((spanDurationMs - WAVE_DURATION_MIN_MS) / (WAVE_DURATION_FULL_MS - WAVE_DURATION_MIN_MS)).coerceIn(0f, 1f)
                                    )
                                    val charCenterX = (charBounds.left + charBounds.right) / 2f
                                    val sweepXForLift = wLeft + sungFactor * wordWidthPx
                                    val sigma = wordWidthPx * 0.28f
                                    val dist = charCenterX - sweepXForLift
                                    val bump = exp(-(dist * dist) / (2f * sigma * sigma))
                                    waveOffset = -(envelope * durationWeight * bump * waveMaxAmpPx)
                                }
                            }
                        }

                        val cTop = charBounds.top + waveOffset
                        val cBottom = charBounds.bottom + (if (hasDescender) descentPadPx else 0f) + waveOffset

                        val left = minOf(charBounds.left, charBounds.right)
                        val right = maxOf(charBounds.left, charBounds.right)

                        if (isWordSung) {
                            clipRect(left = left, top = cTop, right = right, bottom = cBottom) {
                                drawText(layoutResult, topLeft = Offset(0f, waveOffset), color = expressiveAccent)
                            }
                        } else {
                            val featherPx = (wordWidthPx * SWEEP_FEATHER_FRACTION).coerceIn(sweepFeatherMinPx, sweepFeatherMaxPx)
                            val sweepX = wLeft + sungFactor * wordWidthPx
                            val solidRight = (sweepX - featherPx).coerceAtLeast(wLeft)
                            val featherRight = sweepX.coerceAtMost(wRight)

                            val solidPartRight = solidRight.coerceIn(left, right)
                            val featherPartRight = featherRight.coerceIn(left, right)

                            if (solidPartRight > left) {
                                clipRect(left = left, top = cTop, right = solidPartRight, bottom = cBottom) {
                                    drawText(layoutResult, topLeft = Offset(0f, waveOffset), color = expressiveAccent)
                                }
                            }
                            if (featherPartRight > solidPartRight) {
                                clipRect(left = solidPartRight, top = cTop, right = featherPartRight, bottom = cBottom) {
                                    drawText(
                                        layoutResult,
                                        topLeft = Offset(0f, waveOffset),
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(expressiveAccent, expressiveAccent.copy(alpha = dimAlpha)),
                                            startX = solidRight,
                                            endX = featherRight
                                        )
                                    )
                                }
                            }
                            if (right > featherPartRight) {
                                clipRect(left = featherPartRight, top = cTop, right = right, bottom = cBottom) {
                                    drawText(layoutResult, topLeft = Offset(0f, waveOffset), color = expressiveAccent.copy(alpha = dimAlpha))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineFadeAnimation(
    mainText: String,
    isActiveLine: Boolean,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    focusedAlpha: Float,
    alignment: TextAlign,
    isWordSync: Boolean = false
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isActiveLine, mainText) {
        if (isActiveLine) {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                withFrameMillis {
                    elapsedMs = System.currentTimeMillis() - startTime
                }
                if (elapsedMs > (if (isWordSync) mainText.length * 28L + 250L else 800L)) break
            }
        } else {
            elapsedMs = 0L
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(mainText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }
        val letterLayouts = remember(mainText, lyricStyle) {
            mainText.map { textMeasurer.measure(it.toString(), lyricStyle) }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layoutResult.size.height.toDp() })
                .graphicsLayer(clip = false)
        ) {
            if (!isActiveLine || elapsedMs == 0L) {
                drawText(layoutResult, color = lineColor)
                return@Canvas
            }

            if (!isWordSync) {
                val fadeDuration = 350f
                val rawProgress = (elapsedMs / fadeDuration).coerceIn(0f, 1f)
                val progress = 1f - (1f - rawProgress) * (1f - rawProgress) * (1f - rawProgress)
                val alpha = focusedAlpha + (1f - focusedAlpha) * progress
                drawText(layoutResult, color = expressiveAccent.copy(alpha = alpha))
            } else {
                val staggerPerChar = 24f
                val fadeDuration = 160f
                for (i in mainText.indices) {
                    val charBounds = layoutResult.getBoundingBox(i)
                    val charDelay = i * staggerPerChar
                    val rawProgress = ((elapsedMs - charDelay) / fadeDuration).coerceIn(0f, 1f)
                    val progress = 1f - (1f - rawProgress) * (1f - rawProgress) * (1f - rawProgress)
                    val alpha = focusedAlpha + (1f - focusedAlpha) * progress
                    withTransform({ translate(left = charBounds.left, top = charBounds.top) }) {
                        drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = alpha))
                    }
                }
            }
        }
    }
}
