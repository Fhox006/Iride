package com.metrolist.music.ui.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.metrolist.music.ui.theme.CollectionArtworkShape

/** Exact existing outline in print coordinates; no second radius or corner algorithm. */
fun albumSleeveContour(): android.graphics.Path = Path().apply {
    addOutline(CollectionArtworkShape.createOutline(Size(1024f, 1024f), LayoutDirection.Ltr, Density(1f)))
}.asAndroidPath()
