package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.rubberBandOverscroll

@Composable
fun SharedHomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
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
        showNavigationArrow = false,
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
    itemContent: @Composable (item: T, itemWidth: Dp) -> Unit,
) {
    if (loading) {
        ShimmerHost(modifier = modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding)
                    .height(ListItemHeight * 4)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
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
        modifier = modifier
            .fillMaxWidth()
            .rubberBandOverscroll(androidx.compose.foundation.gestures.Orientation.Horizontal, listState),
    ) {
        items(items, key = key) { item -> itemContent(item) }
    }
}
