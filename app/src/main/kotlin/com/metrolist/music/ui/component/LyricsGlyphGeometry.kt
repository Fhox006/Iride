package com.metrolist.music.ui.component

internal data class LyricsGlyphCell(
    val clusterIndex: Int,
    val left: Float,
    val right: Float,
    val isWhitespace: Boolean = false,
)

internal fun lyricsGlyphClips(
    cells: List<LyricsGlyphCell>,
    maskLeft: Float,
    maskRight: Float,
): List<LyricsGlyphCell> {
    // Spaces must not repaint a neighbor's ink; outer cells also own any glyph overhang.
    val visibleCells = cells.filter { !it.isWhitespace && it.right > it.left }.sortedBy { it.left }
    return visibleCells.mapIndexed { index, cell ->
        cell.copy(
            left = if (index == 0) maskLeft else (visibleCells[index - 1].right + cell.left) / 2f,
            right = if (index == visibleCells.lastIndex) maskRight else (cell.right + visibleCells[index + 1].left) / 2f,
        )
    }
}
