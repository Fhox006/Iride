package com.metrolist.music.ui.component

import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.metrolist.music.discovery.moodRosettePath
import com.metrolist.music.models.REVERIE_RING_COUNT

/** Large, cropped rosette, with the original number of nested hairline contours. */
internal fun DrawScope.drawReveriePrintFigure(primary: Int, variant: Int) {
    val path = Path()
    val center = Offset(size.width * (.62f + variant % 3 * .06f), size.height * .58f)
    val radius = size.width * .69f
    for (ring in 1..REVERIE_RING_COUNT) {
        moodRosettePath(center.x, center.y, radius * ring / REVERIE_RING_COUNT,
            petals = 6 + variant % 4 * 2, sharpness = .35f,
            rotation = variant * .17f, result = path)
        drawPath(path.asComposePath(), Color(primary), style = Stroke(.8.dp.toPx()))
    }
}
