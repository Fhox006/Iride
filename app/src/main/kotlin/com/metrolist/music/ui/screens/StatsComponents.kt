/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.CategoryStats
import com.metrolist.music.db.entities.SongEntity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.metrolist.music.ui.component.parseCategoryColor
import com.metrolist.music.utils.makeReadableTimeString
import com.metrolist.music.utils.rememberPreference
import kotlin.math.min
import kotlin.math.pow

/**
 * A superellipse ("squircle") shape implemented without external libraries.
 * n controls how square-like the curve is: higher = more square, lower = more circle.
 * For Material Expressive "squircle" look, use smoothing between 0.60 and 0.75.
 * cornerSmoothing maps to n: 0.60 â†’ nâ‰ˆ5, 0.72 â†’ nâ‰ˆ8
 */
private class SquircleShapeImpl(
    private val cornerRadiusFraction: Float = 0.25f,
    private val n: Double = 6.0
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = squirclePath(size, cornerRadiusFraction, n)
        return Outline.Generic(path)
    }
}

private fun squirclePath(size: androidx.compose.ui.geometry.Size, cornerFraction: Float, n: Double): Path {
    val w = size.width
    val h = size.height
    val r = min(w, h) * cornerFraction.coerceIn(0.05f, 0.5f)
    val path = Path()
    val steps = 72

    fun superellipsePoint(t: Double, a: Double, b: Double): Pair<Double, Double> {
        val cos = kotlin.math.cos(t)
        val sin = kotlin.math.sin(t)
        val x = a * signOf(cos) * kotlin.math.abs(cos).pow(2.0 / n)
        val y = b * signOf(sin) * kotlin.math.abs(sin).pow(2.0 / n)
        return Pair(x, y)
    }

    val corners = listOf(
        Triple(r, r, Math.PI),
        Triple(w - r, r, 3 * Math.PI / 2),
        Triple(w - r, h - r, 0.0),
        Triple(r, h - r, Math.PI / 2)
    )

    var first = true
    for ((cx, cy, startAngle) in corners) {
        for (i in 0..steps / 4) {
            val t = startAngle + (Math.PI / 2) * (i.toDouble() / (steps / 4))
            val (dx, dy) = superellipsePoint(t, r.toDouble(), r.toDouble())
            val x = (cx + dx).toFloat()
            val y = (cy + dy).toFloat()
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun signOf(x: Double): Double = if (x >= 0.0) 1.0 else -1.0

private val SoftSquircle = SquircleShapeImpl(cornerRadiusFraction = 0.22f, n = 6.0)
private val CardSquircle = SquircleShapeImpl(cornerRadiusFraction = 0.25f, n = 7.0)
private val BadgeSquircle = SquircleShapeImpl(cornerRadiusFraction = 0.35f, n = 6.0)

@Composable
fun <T> TopItemsCarousel(
    items: List<T>,
    content: @Composable (index: Int, item: T) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            content(index, item)
        }
    }
}

/**
 * New Iride UI flat variant shared by [TopArtistCard]/[TopAlbumCard]/[TopSongCard]: no Card
 * surface, transparent background, squircle-clipped thumbnail (still [CardSquircle], per the
 * app-wide New Iride UI squircle language), monospace title/subtitle, and a minimal "#N" scrim
 * badge instead of a filled Material colored chip so it reads as flat chrome rather than
 * "still classic mode with a different color."
 */
@Composable
private fun IrideTopItemCard(
    rank: Int,
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    width: Dp,
    imageHeight: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .irideArtworkOverlayBorder(1.dp, MaterialTheme.colorScheme.strokeItemSoft, CardSquircle)
                    .clip(CardSquircle)
            )
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = BadgeSquircle
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$rank",
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = (-0.1).sp,
                    ),
                    color = Color.White,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = (-0.1).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TopArtistCard(rank: Int, artist: ArtistEntity, onClick: () -> Unit = {}) {
    IrideTopItemCard(
        rank = rank,
        title = artist.name,
        subtitle = null,
        thumbnailUrl = artist.thumbnailUrl,
        width = 160.dp,
        imageHeight = 160.dp,
        onClick = onClick,
    )
}

@Composable
fun TopAlbumCard(rank: Int, album: AlbumEntity, artists: List<ArtistEntity>, onClick: () -> Unit = {}) {
    IrideTopItemCard(
        rank = rank,
        title = album.title,
        subtitle = artists.joinToString(", ") { it.name },
        thumbnailUrl = album.thumbnailUrl,
        width = 200.dp,
        imageHeight = 200.dp,
        onClick = onClick,
    )
}

@Composable
fun TopSongCard(rank: Int, song: SongEntity, artists: List<ArtistEntity>, onClick: () -> Unit = {}) {
    IrideTopItemCard(
        rank = rank,
        title = song.title,
        subtitle = artists.joinToString(", ") { it.name },
        thumbnailUrl = song.thumbnailUrl,
        width = 160.dp,
        imageHeight = 160.dp,
        onClick = onClick,
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp, letterSpacing = (-0.1).sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.textSecondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp, end = 16.dp)
    )
}

@Composable
fun StatsDataBox(
    totalMinutes: Long,
    totalSongs: Int,
    totalArtists: Int,
    totalAlbums: Int
) {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.listening_stats),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = (-0.1).sp),
            color = MaterialTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        val items = listOf(
            Triple(if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m", stringResource(R.string.listened), R.drawable.history),
            Triple(totalSongs.toString(), stringResource(R.string.songs_played), R.drawable.music_note),
            Triple(totalArtists.toString(), stringResource(R.string.artists), R.drawable.artist),
            Triple(totalAlbums.toString(), stringResource(R.string.albums), R.drawable.album),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (value, label, iconRes) ->
                StatItem(
                    value = value,
                    label = label,
                    iconRes = iconRes,
                    useIrideStyle = true,
                    modifier = Modifier.weight(1f)
                )
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.strokeHairline)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    iconRes: Int,
    useIrideStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (useIrideStyle) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(bottom = 6.dp)
            )
            Text(
                text = value,
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = label.uppercase(),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 10.sp, letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(28.dp)
                .padding(bottom = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}

private val GenreChartPalette = listOf(
    Color(0xFF3987E5),
    Color(0xFFD95926),
    Color(0xFF199E70),
    Color(0xFFC98500),
    Color(0xFFD55181),
    Color(0xFF008300),
    Color(0xFF9085E9),
    Color(0xFFE66767),
)

/**
 * Ring chart + legend breakdown of listening time by playlist category ("genre" tag).
 * A category keeps the color it was given in AddToCategorySheet/CategoryPills so it reads
 * the same everywhere; uncolored ones fall back to [GenreChartPalette] in rank order.
 */
@Composable
fun TopGenresSection(categories: List<CategoryStats>) {
    if (categories.isEmpty()) return

    val totalTime = categories.sumOf { it.timeListened ?: 0L }.coerceAtLeast(1L)
    val slices = remember(categories) {
        categories.mapIndexed { index, stat ->
            val color = parseCategoryColor(stat.colorHex) ?: GenreChartPalette[index % GenreChartPalette.size]
            val fraction = (stat.timeListened ?: 0L).toFloat() / totalTime.toFloat()
            Triple(stat, color, fraction)
        }
    }
    val ringDescription = slices.joinToString(", ") { (stat, _, fraction) ->
        "${stat.name} ${(fraction * 100).toInt()}%"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        val density = LocalDensity.current
        val strokeWidth = 14.dp
        val gapDegrees = if (slices.size > 1) 3f else 0f

        Box(
            modifier = Modifier
                .size(132.dp)
                .semantics { contentDescription = ringDescription },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = with(density) { strokeWidth.toPx() }
                val diameter = size.minDimension - strokePx
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f
                slices.forEach { (_, color, fraction) ->
                    val sweep = (fraction * 360f - gapDegrees).coerceAtLeast(0f)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    startAngle += fraction * 360f
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = makeReadableTimeString(totalTime),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.top_genres).uppercase(),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 9.sp, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            slices.take(6).forEach { (stat, color, fraction) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stat.name,
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
