package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import com.metrolist.music.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ChartBackground = Color(0xFF19191D)
private val ChartInk = android.graphics.Color.rgb(242, 240, 235)

fun chartCountryName(code: String): String = when (code) {
    "global" -> "Global"
    "gb" -> "United Kingdom"
    "kr" -> "South Korea"
    "cz" -> "Czech Republic"
    else -> Locale.Builder().setRegion(code.uppercase(Locale.ROOT)).build().getDisplayCountry(Locale.ENGLISH)
}

private val chartArtworkCodes = ("global us gb ad ar au at by be bo br bg ca cl co cr cy cz dk do ec eg sv ee fi fr " +
    "de gr gt hn hk hu is in id ie il it jp kz lv lt lu my mt mx ma nl nz ni ng no pk pa py pe ph pl pt ro ru sa " +
    "sg sk za kr es se ch tw th tr ua ae uy ve vn").split(' ').toSet()

fun hasChartArtwork(code: String): Boolean = code in chartArtworkCodes

/** The persisted Library artwork and the live cover use exactly the same renderer. */
fun renderChartPoster(context: Context, code: String, count: Int, size: Int = 768): Bitmap {
    require(hasChartArtwork(code))
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    try {
        CanvasDrawScope().draw(
            density = Density(context.resources.displayMetrics.density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bitmap)),
            size = Size(size.toFloat(), size.toFloat()),
        ) {
            drawChartPoster(code, chartCountryName(code), context.getString(R.string.new_charts_top_count, count),
                Typeface.create(ResourcesCompat.getFont(context, R.font.satoshi_variable), Typeface.BOLD),
                checkNotNull(ResourcesCompat.getFont(context, R.font.ibm_plex_mono_bold)),
                irideGrainBrush(density = 1f, soft = false))
        }
        return bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}
/** Dark covers pair Folio's Satoshi Bold with the artist carousel's IBM Plex Mono Bold. */
@Composable
fun IrideChartPoster(code: String, name: String, topLabel: String, modifier: Modifier) {
    val context = LocalContext.current
    // Keep specks at one physical pixel rather than enlarging them with screen density.
    val grain = remember { irideGrainBrush(density = 1f, soft = false) }
    val titleTypeface = remember(context) {
        Typeface.create(ResourcesCompat.getFont(context, R.font.satoshi_variable), Typeface.BOLD)
    }
    val monoTypeface = remember(context) {
        checkNotNull(ResourcesCompat.getFont(context, R.font.ibm_plex_mono_bold))
    }

    Canvas(modifier) { drawChartPoster(code, name, topLabel, titleTypeface, monoTypeface, grain) }
}

private fun DrawScope.drawChartPoster(
    code: String, name: String, topLabel: String,
    titleTypeface: Typeface, monoTypeface: Typeface,
    grain: androidx.compose.ui.graphics.Brush,
) {
    val side = size.minDimension
    drawRect(ChartBackground)
    translate(side * .105f, side * .58f) {
        drawFlagMotif(code, side * .79f, side * .31f)
    }
    drawRect(grain, alpha = .09f)
    val canvas = drawContext.canvas.nativeCanvas
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ChartInk
        typeface = monoTypeface
        textSize = side * .066f
    }
    canvas.drawText(topLabel, side * .08f, side * .125f, labelPaint)
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ChartInk
        typeface = titleTypeface
        textSize = side * .235f
        textScaleX = .9f
    }
    val width = (side * .84f).toInt().coerceAtLeast(1)
    fun layout() = StaticLayout.Builder.obtain(name, 0, name.length, titlePaint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
        .setLineSpacing(0f, .98f).build()
    var title = layout()
    while ((title.height > side * .355f || title.lineCount > 3) && titlePaint.textSize > side * .10f) {
        titlePaint.textSize *= .94f
        title = layout()
    }
    canvas.save()
    canvas.translate(side * .08f, side * .195f)
    title.draw(canvas)
    canvas.restore()
}

private val Red = Color(0xFFDF493F)
private val Blue = Color(0xFF3464C8)
private val Green = Color(0xFF32876B)
private val Yellow = Color(0xFFEEC84E)
private val White = Color(0xFFFFFCF3)
private val Black = Color(0xFF24242A)
private val Orange = Color(0xFFE58C45)
private val Burgundy = Color(0xFF9A3B51)
private val Cyan = Color(0xFF58AFCA)

private fun DrawScope.drawFlagMotif(code: String, width: Float, height: Float) {
    fun rectangle(color: Color, x: Float, y: Float, w: Float, h: Float) =
        drawRect(color, Offset(x * width, y * height), Size(w * width, h * height))
    fun polygon(color: Color, vararg coordinates: Float) {
        drawPath(Path().apply {
            moveTo(coordinates[0] * width, coordinates[1] * height)
            for (index in 2 until coordinates.size step 2) lineTo(coordinates[index] * width, coordinates[index + 1] * height)
            close()
        }, color)
    }
    fun bands(colors: List<Color>) {
        colors.forEachIndexed { index, color -> rectangle(color, 0f, index.toFloat() / colors.size, 1f, 1f / colors.size) }
    }
    fun bars(colors: List<Color>) {
        colors.forEachIndexed { index, color ->
            val left = index * .29f
            polygon(color, left + .12f, 0f, left + .36f, 0f, left + .24f, 1f, left, 1f)
        }
    }
    fun disc(color: Color, x: Float = .5f, y: Float = .5f, radius: Float = .32f) =
        drawCircle(color, height * radius, Offset(width * x, height * y))
    fun cross(field: Color, ink: Color, border: Color? = null) {
        rectangle(field, 0f, 0f, 1f, 1f)
        if (border != null) {
            rectangle(border, .245f, 0f, .16f, 1f)
            rectangle(border, 0f, .36f, 1f, .28f)
        }
        rectangle(ink, .285f, 0f, .08f, 1f)
        rectangle(ink, 0f, .425f, 1f, .15f)
    }
    fun star(color: Color, x: Float, y: Float, radius: Float = .22f, points: Int = 5) {
        val center = Offset(width * x, height * y)
        val outer = height * radius
        drawPath(Path().apply {
            repeat(points * 2) { index ->
                val angle = -PI / 2 + index * PI / points
                val distance = if (index % 2 == 0) outer else outer * .42f
                val px = center.x + (cos(angle) * distance).toFloat()
                val py = center.y + (sin(angle) * distance).toFloat()
                if (index == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }, color)
    }
    fun crescent(field: Color, ink: Color, x: Float = .53f, y: Float = .5f) {
        disc(ink, x, y, .34f)
        disc(field, x + .07f, y - .06f, .29f)
    }
    fun unionJack() {
        rectangle(Blue, 0f, 0f, 1f, 1f)
        polygon(White, 0f, 0f, .12f, 0f, 1f, .88f, 1f, 1f, .88f, 1f, 0f, .12f)
        polygon(White, 1f, 0f, .88f, 0f, 0f, .88f, 0f, 1f, .12f, 1f, 1f, .12f)
        rectangle(White, .4f, 0f, .2f, 1f)
        rectangle(White, 0f, .35f, 1f, .3f)
        rectangle(Red, .445f, 0f, .11f, 1f)
        rectangle(Red, 0f, .415f, 1f, .17f)
    }
    when (code) {
        "global" -> {
            disc(Blue, radius = .48f)
            drawCircle(White, height * .48f, Offset(width * .5f, height * .5f), style = Stroke(height * .025f))
            drawOval(White, Offset(width * .5f - height * .24f, height * .02f), Size(height * .48f, height * .96f), style = Stroke(height * .025f))
            drawLine(White, Offset(width * .5f - height * .48f, height * .5f), Offset(width * .5f + height * .48f, height * .5f), strokeWidth = height * .025f)
        }
        "it" -> bars(listOf(Green, White, Red))
        "fr" -> bars(listOf(Blue, White, Red))
        "ie" -> bars(listOf(Green, White, Orange))
        "be" -> bars(listOf(Black, Yellow, Red))
        "ro" -> bars(listOf(Blue, Yellow, Red))
        "ad" -> { bars(listOf(Blue, Yellow, Red)); disc(Burgundy, radius = .10f) }
        "ng" -> bars(listOf(Green, White, Green))
        "pe" -> bars(listOf(Red, White, Red))
        "mx" -> { bars(listOf(Green, White, Red)); disc(Green, radius = .11f) }
        "gt" -> { bars(listOf(Cyan, White, Cyan)); disc(Green, radius = .10f) }
        "ca" -> { bars(listOf(Red, White, Red)); star(Red, .49f, .5f, .25f, 7) }
        "us" -> {
            repeat(7) { index -> rectangle(if (index % 2 == 0) Red else White, 0f, index / 7f, 1f, 1f / 7f) }
            rectangle(Blue, 0f, 0f, .43f, .57f)
            repeat(3) { index -> disc(White, .10f + index * .115f, .27f, .035f) }
        }
        "gb" -> unionJack()
        "au", "nz" -> { rectangle(Blue, 0f, 0f, 1f, 1f); polygon(White, 0f, .2f, .37f, .2f, .37f, .3f, 0f, .3f); rectangle(Red, .13f, 0f, .08f, .52f); star(if (code == "au") White else Red, .7f, .5f, .18f); star(White, .87f, .76f, .11f) }
        "br" -> {
            rectangle(Green, 0f, 0f, 1f, 1f)
            polygon(Yellow, .5f, .08f, .91f, .5f, .5f, .92f, .09f, .5f)
            disc(Blue, radius = .26f)
            polygon(White, .365f, .37f, .66f, .51f, .65f, .57f, .35f, .43f)
        }
        "jp" -> { rectangle(White, 0f, 0f, 1f, 1f); disc(Red) }
        "kr" -> {
            rectangle(White, 0f, 0f, 1f, 1f)
            disc(Red, radius = .28f)
            clipRect(0f, height * .5f, width, height) { disc(Blue, radius = .28f) }
            rectangle(Black, .09f, .15f, .12f, .065f); rectangle(Black, .78f, .79f, .12f, .065f)
        }
        "de" -> bands(listOf(Black, Red, Yellow))
        "nl", "lu" -> bands(listOf(Red, White, if (code == "lu") Cyan else Blue))
        "ru", "sk" -> { bands(listOf(White, Blue, Red)); if (code == "sk") rectangle(Red, .20f, .2f, .13f, .5f) }
        "by" -> { bands(listOf(Red, Red, Green)); rectangle(White, 0f, 0f, .12f, 1f) }
        "ua" -> bands(listOf(Blue, Yellow))
        "pl", "id" -> if (code == "pl") bands(listOf(White, Red)) else bands(listOf(Red, White))
        "at" -> bands(listOf(Red, White, Red))
        "hu" -> bands(listOf(Red, White, Green))
        "bg" -> bands(listOf(White, Green, Red))
        "lt" -> bands(listOf(Yellow, Green, Red))
        "lv" -> bands(listOf(Burgundy, White, Burgundy))
        "ee" -> bands(listOf(Blue, Black, White))
        "es" -> { bands(listOf(Red, Yellow, Yellow, Red)); rectangle(Burgundy, .22f, .36f, .085f, .28f) }
        "bo" -> bands(listOf(Red, Yellow, Green))
        "co", "ec", "ve" -> { bands(listOf(Yellow, Yellow, Blue, Red)); if (code != "co") disc(White, radius = .055f) }
        "py" -> { bands(listOf(Red, White, Blue)); disc(Green, radius = .10f) }
        "ar", "uy" -> { bands(listOf(Cyan, White, Cyan)); disc(Yellow, if (code == "uy") .18f else .5f, .5f, .12f) }
        "cr" -> bands(listOf(Blue, White, Red, Red, White, Blue))
        "hn", "ni", "sv" -> { bands(listOf(Blue, White, Blue)); disc(if (code == "hn") Blue else Yellow, radius = .085f) }
        "dk" -> cross(Red, White)
        "no" -> cross(Red, Blue, White)
        "se" -> cross(Blue, Yellow)
        "fi" -> cross(White, Blue)
        "is" -> cross(Blue, Red, White)
        "ch" -> { rectangle(Red, 0f, 0f, 1f, 1f); rectangle(White, .43f, .2f, .14f, .6f); rectangle(White, .31f, .38f, .38f, .24f) }
        "gr" -> { repeat(7) { index -> rectangle(if (index % 2 == 0) Blue else White, 0f, index / 7f, 1f, 1f / 7f) }; rectangle(Blue, 0f, 0f, .4f, .58f); rectangle(White, .16f, 0f, .08f, .58f); rectangle(White, 0f, .23f, .4f, .12f) }
        "cz" -> { bands(listOf(White, Red)); polygon(Blue, 0f, 0f, .42f, .5f, 0f, 1f) }
        "pt" -> { rectangle(Green, 0f, 0f, .38f, 1f); rectangle(Red, .38f, 0f, .62f, 1f); disc(Yellow, .38f, .5f, .19f) }
        "in" -> { bands(listOf(Orange, White, Green)); drawCircle(Blue, height * .11f, Offset(width * .5f, height * .5f), style = Stroke(height * .035f)) }
        "pk" -> { rectangle(Green, 0f, 0f, 1f, 1f); rectangle(White, 0f, 0f, .19f, 1f); crescent(Green, White, .58f); star(White, .74f, .3f, .08f) }
        "tr" -> { rectangle(Red, 0f, 0f, 1f, 1f); crescent(Red, White, .42f); star(White, .67f, .5f, .18f) }
        "sa" -> { rectangle(Green, 0f, 0f, 1f, 1f); rectangle(White, .22f, .3f, .56f, .09f); rectangle(White, .26f, .66f, .49f, .05f) }
        "ma" -> { rectangle(Red, 0f, 0f, 1f, 1f); star(Green, .5f, .5f, .29f) }
        "eg" -> { bands(listOf(Red, White, Black)); disc(Yellow, radius = .10f) }
        "ae" -> { bands(listOf(Green, White, Black)); rectangle(Red, 0f, 0f, .22f, 1f) }
        "il" -> { bands(listOf(White, Blue, White, White, White, Blue, White)); star(Blue, .5f, .5f, .20f, 6) }
        "cy" -> { rectangle(White, 0f, 0f, 1f, 1f); polygon(Orange, .29f, .48f, .65f, .3f, .73f, .41f, .52f, .65f, .3f, .6f); rectangle(Green, .36f, .76f, .32f, .06f) }
        "mt" -> { rectangle(White, 0f, 0f, .5f, 1f); rectangle(Red, .5f, 0f, .5f, 1f); rectangle(Black, .08f, .14f, .03f, .23f); rectangle(Black, .04f, .23f, .11f, .04f) }
        "kz" -> { rectangle(Cyan, 0f, 0f, 1f, 1f); rectangle(Yellow, .08f, 0f, .04f, 1f); disc(Yellow, radius = .24f); polygon(Yellow, .35f, .78f, .65f, .78f, .5f, .88f) }
        "my" -> { repeat(7) { index -> rectangle(if (index % 2 == 0) Red else White, 0f, index / 7f, 1f, 1f / 7f) }; rectangle(Blue, 0f, 0f, .46f, .6f); crescent(Blue, Yellow, .23f, .3f) }
        "sg" -> { bands(listOf(Red, White)); crescent(Red, White, .25f, .24f); star(White, .42f, .24f, .07f) }
        "th" -> bands(listOf(Red, White, Blue, Blue, White, Red))
        "vn" -> { rectangle(Red, 0f, 0f, 1f, 1f); star(Yellow, .5f, .5f, .34f) }
        "ph" -> { bands(listOf(Blue, Red)); polygon(White, 0f, 0f, .45f, .5f, 0f, 1f); disc(Yellow, .15f, .5f, .14f) }
        "tw" -> { rectangle(Red, 0f, 0f, 1f, 1f); rectangle(Blue, 0f, 0f, .48f, .58f); disc(White, .24f, .29f, .17f) }
        "hk" -> { rectangle(Red, 0f, 0f, 1f, 1f); polygon(White, .42f, .20f, .68f, .47f, .54f, .51f, .40f, .79f, .38f, .54f, .27f, .36f) }
        "za" -> { bands(listOf(Red, Blue)); polygon(Green, 0f, 0f, .47f, .35f, 1f, .35f, 1f, .65f, .47f, .65f, 0f, 1f); polygon(Yellow, 0f, .06f, .38f, .5f, 0f, .94f); polygon(Black, 0f, .18f, .27f, .5f, 0f, .82f) }
        "pa" -> { rectangle(White, 0f, 0f, 1f, 1f); rectangle(Red, .5f, 0f, .5f, .5f); rectangle(Blue, 0f, .5f, .5f, .5f); star(Blue, .25f, .25f, .15f); star(Red, .75f, .75f, .15f) }
        "do" -> { rectangle(Blue, 0f, 0f, 1f, 1f); rectangle(Red, .5f, 0f, .5f, .5f); rectangle(Red, 0f, .5f, .5f, .5f); rectangle(White, .44f, 0f, .12f, 1f); rectangle(White, 0f, .42f, 1f, .16f) }
        "cl" -> { bands(listOf(White, Red)); rectangle(Blue, 0f, 0f, .33f, .5f); star(White, .165f, .25f, .15f) }
        else -> bars(listOf(Blue, White, Green))
    }
}
