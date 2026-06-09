/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.SongEntity
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.pow

/**
 * A superellipse ("squircle") shape implemented without external libraries.
 * n controls how square-like the curve is: higher = more square, lower = more circle.
 * For Material Expressive "squircle" look, use smoothing between 0.60 and 0.75.
 * cornerSmoothing maps to n: 0.60 â†’ nâ‰ˆ5, 0.72 â†’ nâ‰ˆ8
 */
private class SquircleShapeImpl(
    private val cornerRadiusFraction: Float = 0.25f, // fraction of min(width, height)
    private val n: Double = 6.0                       // superellipse exponent; 5â€“8 for squircle
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

    // Build 4 corner arcs
    val corners = listOf(
        Triple(r, r, Math.PI),          // top-left,     start angle = Ï€
        Triple(w - r, r, 3 * Math.PI / 2),  // top-right
        Triple(w - r, h - r, 0.0),      // bottom-right
        Triple(r, h - r, Math.PI / 2)   // bottom-left
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

@Composable
fun TopArtistCard(rank: Int, artist: ArtistEntity, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = CardSquircle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .width(160.dp)
            .wrapContentHeight()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(CardSquircle)
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = BadgeSquircle
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopAlbumCard(rank: Int, album: AlbumEntity, artists: List<ArtistEntity>, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = CardSquircle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .width(200.dp)
            .wrapContentHeight()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = album.thumbnailUrl,
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(CardSquircle)
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f),
                        shape = BadgeSquircle
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopSongCard(rank: Int, song: SongEntity, artists: List<ArtistEntity>, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = CardSquircle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .width(160.dp)
            .wrapContentHeight()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(CardSquircle)
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                        shape = BadgeSquircle
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
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

    Card(
        shape = CardSquircle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.listening_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                    label = stringResource(R.string.listened),
                    iconRes = R.drawable.history
                )
                StatItem(
                    value = totalSongs.toString(),
                    label = stringResource(R.string.songs_played),
                    iconRes = R.drawable.music_note
                )
                StatItem(
                    value = totalArtists.toString(),
                    label = stringResource(R.string.artists),
                    iconRes = R.drawable.artist
                )
                StatItem(
                    value = totalAlbums.toString(),
                    label = stringResource(R.string.albums),
                    iconRes = R.drawable.album
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, iconRes: Int) {
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
