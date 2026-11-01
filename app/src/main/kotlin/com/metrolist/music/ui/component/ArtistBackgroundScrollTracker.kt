package com.metrolist.music.ui.component

internal data class ArtistScrollAnchor(val offset: Int, val size: Int, val key: Any)

/** Retain traveled distance after the artwork/header item leaves the lazy viewport. */
internal class ArtistBackgroundScrollTracker {
    private var previous = emptyList<ArtistScrollAnchor>()
    private var previousIndex = 0
    private var previousOffset = 0
    private var distance = 0f

    fun update(index: Int, offset: Int, visible: List<ArtistScrollAnchor>): Float {
        if (visible.isEmpty()) return distance
        distance = when {
            index == 0 -> offset.toFloat()
            index == previousIndex && offset == previousOffset -> distance
            else -> {
                val anchor = previous.firstNotNullOfOrNull { old ->
                    visible.firstOrNull { it.key == old.key }?.let { old.offset - it.offset }
                }
                val delta = anchor?.toFloat() ?: run {
                    // A jump may skip every measured anchor. Estimate only the skipped gap;
                    // ordinary gestures resume exact displacement using shared stable keys.
                    val averageSize = previous.map { it.size }.average().takeIf { it.isFinite() }
                        ?: visible.map { it.size }.average()
                    ((index - previousIndex) * averageSize + offset - previousOffset).toFloat()
                }
                (distance + delta).coerceAtLeast(0f)
            }
        }
        previous = visible
        previousIndex = index
        previousOffset = offset
        return distance
    }
}
