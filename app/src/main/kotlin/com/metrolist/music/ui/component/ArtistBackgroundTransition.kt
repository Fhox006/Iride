package com.metrolist.music.ui.component

/** Keep the initial viewport tinted; move the base in only after a deliberate scroll. */
internal fun artistBackgroundTintOpacity(
    y: Float,
    scrollOffset: Float,
    albumStart: Float,
    viewportHeight: Float,
    parallaxFactor: Float = 0.75f,
): Float {
    val fadeHeight = (viewportHeight * 0.30f).coerceAtLeast(1f)
    val initialFadeEnd = maxOf(albumStart * parallaxFactor, viewportHeight + fadeHeight)
    val travel = (scrollOffset - viewportHeight * 0.35f).coerceAtLeast(0f)
    val fadeEnd = initialFadeEnd - travel * parallaxFactor
    val t = ((y - (fadeEnd - fadeHeight)) / fadeHeight).coerceIn(0f, 1f)
    val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
    return (1f - fade).coerceIn(0f, 1f)
}
