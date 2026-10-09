package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.ui.component.shimmer.ShimmerHost

@Composable
fun SharedHomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    showNavigationArrow: Boolean = false,
    titleMaxLines: Int = 1,
) {
    NavigationTitle(
        title = title,
        modifier = modifier,
        onClick = onClick,
        onRefreshClick = onRefreshClick,
        isRefreshing = isRefreshing,
        useIrideStyle = true,
        prominentTitle = true,
        sentenceCaseTitle = true,
        showDivider = false,
        showNavigationArrow = showNavigationArrow,
        titleMaxLines = titleMaxLines,
        topPadding = 24.dp,
        bottomPadding = 12.dp,
    )
}

@Composable
fun <T> SharedQuickPicksCarousel(
    items: List<T>,
    key: (T) -> Any,
    loading: Boolean,
    contentPadding: Dp,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    usePager: Boolean = false,
    itemContent: @Composable (item: T, itemWidth: Dp) -> Unit,
) {
    if (loading) {
        ShimmerHost(modifier = modifier.fillMaxWidth()) {
            Spacer(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = contentPadding)
                        .height(ListItemHeight * 4)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }
    } else if (items.isNotEmpty() && usePager) {
        QuickPicksPager(
            items = items,
            itemKey = key,
            contentPadding = contentPadding,
            modifier = modifier,
            itemContent = itemContent,
        )
    } else if (items.isNotEmpty()) {
        SongCarousel(
            items = items,
            key = key,
            contentPadding = PaddingValues(horizontal = contentPadding),
            gridState = gridState,
            modifier = modifier,
            itemContent = itemContent,
        )
    }
}

@Composable
private fun <T> QuickPicksPager(
    items: List<T>,
    itemKey: (T) -> Any,
    contentPadding: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, itemWidth: Dp) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rows = minOf(4, items.size)
        val columnSpacing = 12.dp
        val contentWidth = (maxWidth - contentPadding * 2).coerceAtLeast(0.dp)
        val columns = if ((contentWidth - columnSpacing) / 2 >= 320.dp) 2 else 1
        val itemsPerPage = rows * columns
        val pageCount = (items.size + itemsPerPage - 1) / itemsPerPage
        val pagerState = rememberPagerState(pageCount = { pageCount })

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = contentPadding),
                pageSpacing = columnSpacing,
                overscrollEffect = null,
                modifier =
                    Modifier.fillMaxWidth()
                        .height(ListItemHeight * rows)
                        .rubberBandOverscroll(
                            androidx.compose.foundation.gestures.Orientation.Horizontal,
                            pagerState,
                        ),
            ) { page ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                ) {
                    repeat(columns) { column ->
                        BoxWithConstraints(modifier = Modifier.weight(1f)) {
                            val itemWidth = maxWidth
                            Column(modifier = Modifier.fillMaxWidth()) {
                                repeat(rows) { row ->
                                    Box(modifier = Modifier.fillMaxWidth().height(ListItemHeight)) {
                                        // Keep the grid's column-first ordering, including partial
                                        // last pages.
                                        val index = page * itemsPerPage + column * rows + row
                                        items.getOrNull(index)?.let { item ->
                                            key(itemKey(item)) {
                                                itemContent(item, itemWidth)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CarouselPageIndicator(
                pageCount = pageCount,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
fun <T> SharedYouTubePlaylistRow(
    items: List<T>,
    key: (T) -> Any,
    contentPadding: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = null,
        modifier =
            modifier
                .fillMaxWidth()
                .rubberBandOverscroll(
                    androidx.compose.foundation.gestures.Orientation.Horizontal,
                    listState,
                ),
    ) {
        items(items, key = key) { item -> itemContent(item) }
    }
}
