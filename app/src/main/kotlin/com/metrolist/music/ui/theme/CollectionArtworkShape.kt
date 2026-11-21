package com.metrolist.music.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.metrolist.music.ui.component.IrideExpansionMotion

/** One proportional Folio contour for collection tiles, their flights and full-size covers. */
internal object CollectionArtworkShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        FolioShape(with(density) {
            (size.minDimension * IrideExpansionMotion.ArtworkCornerFraction).toDp()
        }).createOutline(size, layoutDirection, density)
}
