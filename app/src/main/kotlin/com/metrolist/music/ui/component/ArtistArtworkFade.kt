package com.metrolist.music.ui.component

/** A flat-ended dissolve reaches the page pigment before the overlapping title begins. */
internal fun artistArtworkPhotoOpacity(heightFraction: Float): Float {
    val t = ((heightFraction - 0.60f) / 0.36f).coerceIn(0f, 1f)
    val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
    return (1f - fade).coerceIn(0f, 1f)
}
