/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

private data class HyphenGroupWord(
    val pos: Int,
    val size: Int,
    val isLast: Boolean,
    val groupStartMs: Long,
    val groupEndMs: Long
)

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
        .then(if (blurRadius > 0.5f) Modifier.blur(blurRadius.dp) else Modifier)
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
                        modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false)
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
    val glowPaint = remember { android.graphics.Paint().apply { isAntiAlias = true } }

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

    val hyphenGroupData = remember(effectiveWords) {
        val map = mutableMapOf<Int, HyphenGroupWord>()
        var currentGroup = mutableListOf<Int>()
        effectiveWords.forEachIndexed { wordIdx, word ->
            currentGroup.add(wordIdx)
            if (!word.text.endsWith("-")) {
                if (currentGroup.size > 1) {
                    val groupSize = currentGroup.size
                    val groupStartMs = (effectiveWords[currentGroup.first()].startTime * 1000).toLong()
                    val groupEndMs = (word.endTime * 1000).toLong()
                    currentGroup.forEachIndexed { pos, idx ->
                        map[idx] = HyphenGroupWord(pos, groupSize, pos == groupSize - 1, groupStartMs, groupEndMs)
                    }
                }
                currentGroup = mutableListOf()
            }
        }
        map
    }

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

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { layoutResult.size.height.toDp() })
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
                                left = minOf(left, bounds.left)
                                right = maxOf(right, bounds.right)
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

                val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData
                val wordFactors = effectiveWords.mapIndexed { wordIdx, word ->
                    val (wStartMs, wEndMs) = remappedWordTimesMs[wordIdx]
                    val isWordSung = smoothPosition > wEndMs
                    val sungFactor = if (isWordSung) 1f
                    else if (smoothPosition >= wStartMs) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                    else 0f
                    Triple(sungFactor, word, isWordSung)
                }

                // Pixel bounds per word — used for pixel-proportional sweep timing
                val wordPixelLeft = FloatArray(effectiveWords.size) { Float.MAX_VALUE }
                val wordPixelRight = FloatArray(effectiveWords.size) { -Float.MAX_VALUE }
                for (k in 0 until clusterCount) {
                    val wIdx = wordIdxMap[k]
                    if (wIdx == -1) continue
                    val b = layoutResult.getBoundingBox(clusterCharOffsets[k])
                    if (b.left < wordPixelLeft[wIdx]) wordPixelLeft[wIdx] = b.left
                    if (b.right > wordPixelRight[wIdx]) wordPixelRight[wIdx] = b.right
                }

                // Main drawing pass
                for (i in 0 until clusterCount) {
                    val charOffset = clusterCharOffsets[i]
                    val charBounds = layoutResult.getBoundingBox(charOffset)
                    val wordIdx = wordIdxMap[i]

                    val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)

                    val charLp = if (wordItem != null) {
                        val (rStartMs, rEndMs) = remappedWordTimesMs[wordIdx]
                        val wLeft = wordPixelLeft[wordIdx]
                        val wRight = wordPixelRight[wordIdx]
                        val wWidth = (wRight - wLeft).coerceAtLeast(1f)
                        val wordProgress = ((smoothPosition - rStartMs).toFloat() / (rEndMs - rStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                        val sweepX = wLeft + wordProgress * wWidth
                        ((sweepX - charBounds.left) / charBounds.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    } else 0f

                    val shouldGlow = wordItem != null && !isWordSung && sungFactor > 0.001f

                    val groupWord = if (wordIdx != -1) hyphenGroupData[wordIdx] else null
                    var waveOffset = 0f
                    if (groupWord != null) {
                        val wallTime = System.currentTimeMillis()
                        val timeInGroup = (smoothPosition - groupWord.groupStartMs).toFloat()
                        val timeToGroupEnd = (groupWord.groupEndMs - smoothPosition).toFloat()
                        val waveFade = (timeInGroup / 200f).coerceIn(0f, 1f) * (timeToGroupEnd / 200f).coerceIn(0f, 1f)
                        if (waveFade > 0.01f) {
                            waveOffset = sin(wallTime * 0.006f + i * 0.4f) * 3.24f * waveFade
                        }
                    }

                    val lineIdx = layoutResult.getLineForOffset(charOffset)
                    val lineBottom = layoutResult.getLineBottom(lineIdx)
                    val cTop = charBounds.top + waveOffset
                    val cBottom = maxOf(charBounds.bottom, lineBottom) + waveOffset

                    if (shouldGlow && wordItem != null) {
                        val sMs = wordItem.startTime * 1000; val eMs = wordItem.endTime * 1000; val dur = eMs - sMs
                        val impactRatio = dur.toFloat() / wordItem.text.length.coerceAtLeast(1)
                        val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                        val impactFactor = (((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fadeFactor
                        if (impactFactor > 0.01f) {
                            val lineBaseline = layoutResult.getLineBaseline(layoutResult.getLineForOffset(charOffset)).toFloat()
                            drawIntoCanvas { canvas ->
                                glowPaint.maskFilter = BlurMaskFilter(12.dp.toPx() * impactFactor, BlurMaskFilter.Blur.NORMAL)
                                glowPaint.color = expressiveAccent.copy(alpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)).toArgb()
                                glowPaint.textSize = lyricStyle.fontSize.toPx()
                                glowPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                canvas.nativeCanvas.drawText(graphemeClusters[i], charBounds.left, lineBaseline + waveOffset, glowPaint)
                            }
                        }
                    }

                    val baseAlpha = if (isWordSung || charLp > 0.99f) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                    clipRect(left = charBounds.left, top = cTop, right = charBounds.right, bottom = cBottom) {
                        drawText(layoutResult, topLeft = androidx.compose.ui.geometry.Offset(0f, waveOffset), color = expressiveAccent.copy(alpha = if (wordIdx == -1) focusedAlpha else baseAlpha))
                    }
                    if (!isWordSung && charLp > 0f && charLp < 1f) {
                        val fXL = charBounds.width * charLp
                        val eW = (charBounds.width * 0.45f).coerceAtLeast(1f)
                        val sWL = (fXL - eW).coerceAtLeast(0f)
                        if (sWL > 0f) {
                            clipRect(left = charBounds.left, top = cTop, right = charBounds.left + sWL, bottom = cBottom) {
                                drawText(layoutResult, topLeft = androidx.compose.ui.geometry.Offset(0f, waveOffset), color = expressiveAccent)
                            }
                        }
                        for (j in 0 until 12) {
                            val start = charBounds.left + sWL + j * eW / 12f
                            val end = (charBounds.left + sWL + (j + 1) * eW / 12f + 0.5f).coerceAtMost(charBounds.left + fXL)
                            if (end > start) {
                                clipRect(left = start, top = cTop, right = end, bottom = cBottom) {
                                    drawText(layoutResult, topLeft = androidx.compose.ui.geometry.Offset(0f, waveOffset), color = expressiveAccent.copy(alpha = 1f - (j + 0.5f) / 12f))
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
