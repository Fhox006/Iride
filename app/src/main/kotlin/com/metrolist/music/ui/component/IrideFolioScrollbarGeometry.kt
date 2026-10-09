package com.metrolist.music.ui.component

internal data class FolioScrollbarThumb(
    val height: Float,
    val offset: Float,
    val travel: Float,
    val scrollRange: Float,
)

internal fun folioScrollbarThumb(
    trackHeight: Float,
    contentHeight: Int,
    viewportHeight: Int,
    scrollOffset: Int,
    minimumThumbHeight: Float,
): FolioScrollbarThumb? {
    if (trackHeight <= 0f || viewportHeight <= 0 || contentHeight <= viewportHeight ||
        contentHeight == Int.MAX_VALUE || viewportHeight == Int.MAX_VALUE || scrollOffset == Int.MAX_VALUE
    ) return null
    val range = (contentHeight - viewportHeight).toFloat()
    val height = (trackHeight * viewportHeight / contentHeight)
        .coerceIn(minimumThumbHeight.coerceAtMost(trackHeight), trackHeight)
    val travel = trackHeight - height
    return FolioScrollbarThumb(height, travel * (scrollOffset / range).coerceIn(0f, 1f), travel, range)
}
