package com.metrolist.music.ui.component

/** Blur begins with the title in the upper part of the page and finishes just before the controls. */
internal fun artistPhotoBlurProgress(
    titleTop: Float,
    initialTitleTop: Float,
    controlsBottom: Float,
    lead: Float,
    finishGap: Float,
): Float {
    require(lead > finishGap && finishGap >= 0f)
    if (titleTop == Float.POSITIVE_INFINITY) return 0f
    if (titleTop == Float.NEGATIVE_INFINITY) return 1f
    val start = minOf(initialTitleTop, controlsBottom + lead)
    val end = minOf(controlsBottom + finishGap, start - 1f)
    val t = ((start - titleTop) / (start - end)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
