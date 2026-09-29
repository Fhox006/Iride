package com.metrolist.music.ui.component

import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.metrolist.music.R
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.MixPosterRenderer
import kotlin.random.Random

@Composable
fun MixPosterPlaceholder(title: String, mood: MixMood, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.anton) ?: Typeface.DEFAULT_BOLD
    }
    val seed = remember(title) { title.hashCode().toLong() }
    val grain = remember(seed) {
        val random = Random(seed)
        Bitmap.createBitmap(IntArray(64 * 64) {
            val light = random.nextBoolean()
            (random.nextInt(20, 61) shl 24) or if (light) 0x00FFFFFF else 0
        }, 64, 64, Bitmap.Config.ARGB_8888)
    }
    DisposableEffect(grain) { onDispose { grain.recycle() } }
    Canvas(modifier.fillMaxSize()) {
        val side = size.minDimension.toInt()
        val canvas = drawContext.canvas.nativeCanvas
        MixPosterRenderer.drawGraphicBackground(canvas, side, mood, seed)
        MixPosterRenderer.drawTitle(canvas, title, side, mood.color, typeface)
        canvas.drawBitmap(grain, null, RectF(0f, 0f, side.toFloat(), side.toFloat()), null)
    }
}
