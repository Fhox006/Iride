/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CollapsedWidth = 14.dp
private val ExpandedWidth = 36.dp

@Composable
fun AlphabetSlider(
    selectedLetter: Char?,
    onLetterSelected: (Char?) -> Unit,
    availableLetters: Set<Char>,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val letters = remember(availableLetters) {
        buildList {
            if ('#' in availableLetters) add('#')
            ('A'..'Z').forEach { if (it in availableLetters) add(it) }
        }
    }

    fun letterAt(yPx: Float, heightPx: Int): Char? {
        if (letters.isEmpty()) return null
        val itemHeight = heightPx.toFloat() / letters.size
        val index = (yPx / itemHeight).toInt().coerceIn(0, letters.size - 1)
        return letters[index]
    }

    val width by animateDpAsState(
        targetValue = if (isExpanded) ExpandedWidth else CollapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "alphabetWidth",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.50f,
        animationSpec = tween(200),
        label = "alphabetBgAlpha",
    )

    Box(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(CollapsedWidth / 2))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = bgAlpha),
            )
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isExpanded = true
                    var current = letterAt(down.position.y, size.height)
                    current?.let {
                        if (it != selectedLetter) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(it)
                        }
                    }
                    drag(down.id) { change ->
                        change.consume()
                        val next = letterAt(change.position.y, size.height)
                        if (next != null && next != current) {
                            current = next
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(next)
                        }
                    }
                    isExpanded = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isExpanded && letters.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                letters.forEach { letter ->
                    val isSelected = letter == selectedLetter
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else if (selectedLetter != null) {
            Text(
                text = selectedLetter.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
