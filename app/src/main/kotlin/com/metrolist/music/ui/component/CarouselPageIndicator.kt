package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.textPrimary

@Composable
fun CarouselPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    // Large release catalogs keep the same dots without expanding beyond the pager's width.
    val visibleCount = minOf(pageCount, 7)
    val firstVisible = (currentPage - visibleCount / 2).coerceIn(0, pageCount - visibleCount)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(visibleCount) { offset ->
            val index = firstVisible + offset
            val isSelected = currentPage == index
            val clippedEdge = (offset == 0 && firstVisible > 0) ||
                (offset == visibleCount - 1 && firstVisible + visibleCount < pageCount)
            val dotSize by animateDpAsState(
                targetValue = if (isSelected) 5.dp else if (clippedEdge) 3.dp else 4.dp,
                animationSpec = tween(durationMillis = 180),
                label = "radioPageIndicatorSize",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.textPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    ),
            )
        }
    }
}
