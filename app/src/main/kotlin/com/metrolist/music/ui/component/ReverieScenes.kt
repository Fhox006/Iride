package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import com.metrolist.music.models.reverieGenreColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.metrolist.music.R
import com.metrolist.music.models.REVERIE_CHAPTER_MILLIS
import com.metrolist.music.models.ReverieChapter
import com.metrolist.music.models.ReverieRank
import com.metrolist.music.models.ReverieRecap
import com.metrolist.music.models.ReveriePalette
import com.metrolist.music.models.reverieEdition
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sin

private val FilmInk = Color(0xFF24242A)
private val FilmBlue: Color @Composable get() = Color(LocalReveriePalette.current.accent)
private val FilmWine: Color @Composable get() = Color(LocalReveriePalette.current.secondary)
private val FilmTitleFont = FontFamily(Font(R.font.anton))
internal val LocalReveriePalette = androidx.compose.runtime.staticCompositionLocalOf { ReveriePalette(0xFFF2F0EB.toInt()) }
internal val LocalReveriePaper = androidx.compose.runtime.staticCompositionLocalOf { Color(0xFFF2F0EB) }

/** Each chapter is laid out in the measured slot: no fixed canvas, scaled child or letterbox. */
@Composable
internal fun ReverieFilm(
    recap: ReverieRecap,
    chapter: Int,
    elapsed: () -> Float,
    images: Map<String, Bitmap>,
    reducedMotion: Boolean,
    motionTime: () -> Float,
    instant: Boolean,
    modifier: Modifier = Modifier,
) {
    val selected = recap.chapters[chapter]
    val sceneElapsed: () -> Float = if (instant) ({ REVERIE_CHAPTER_MILLIS.toFloat() }) else elapsed
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val short = maxHeight < 380.dp
        val compact = maxHeight < 460.dp
        val gap = if (short) 8.dp else if (compact) 10.dp else 18.dp
        val titleSize = if (short) 30f else if (compact) 38f else 52f
        val number = remember { NumberFormat.getIntegerInstance() }
        val month = remember(recap.month) { recap.month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())) }
        fun motion(index: Int = 0, drift: Float = 1f) = Modifier.filmMotion(sceneElapsed, motionTime, reducedMotion, index, drift)
        key(selected, instant) {
            if (selected == ReverieChapter.POSTER) {
                ReveriePosterScene(recap, images, sceneElapsed, motionTime, reducedMotion)
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = if (compact) 10.dp else 18.dp)) {
                    FilmHeading(selected, titleSize, sceneElapsed, motionTime, reducedMotion, reverieEdition(recap.month).typographyVariant)
                    Spacer(Modifier.height(gap))
                    when (selected) {
                        ReverieChapter.OVERTURE -> {
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val side = minOf(maxHeight * .76f, maxWidth * .43f)
                                recap.songs.take(3).reversed().forEachIndexed { index, song ->
                                    FilmArtwork(song, images, side,
                                        Modifier.graphicsLayer {
                                            translationX = (index - 1) * side.toPx() * .56f
                                            val t = if (reducedMotion) 0f else motionTime() / 1000f
                                            translationY = sin(t * .85f + index * 1.8f) * 10.dp.toPx()
                                            rotationZ = (index - 1) * 11f + sin(t * .65f + index) * 4f
                                        }.then(motion(index + 1, 0f)), motionTime, reducedMotion)
                                }
                            }
                            FilmText(month.uppercase(Locale.getDefault()), if (compact) 34f else 43f, FilmWine, title = true,
                                modifier = motion(3), maxLines = 1)
                            Spacer(Modifier.height(gap))
                            FilmText(stringResource(R.string.reverie_overture_note), 12f, modifier = motion(4))
                        }
                        ReverieChapter.TIME -> {
                            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                                FilmCounter(recap.minutes, if (compact) 78f else 108f, sceneElapsed, motionTime, reducedMotion)
                                FilmText(stringResource(R.string.reverie_minutes), 11f, modifier = motion(1))
                            }
                            HorizontalDivider(color = FilmInk.copy(alpha = .35f), modifier = motion(2, 0f))
                            Spacer(Modifier.height(gap))
                            Row(Modifier.fillMaxWidth()) {
                                FilmStat(number.format(recap.plays), stringResource(R.string.reverie_plays), Modifier.weight(1f).then(motion(2)))
                                FilmStat(number.format(recap.songs.size), stringResource(R.string.reverie_tracks), Modifier.weight(1f).then(motion(3)))
                            }
                            Spacer(Modifier.height(gap))
                            FilmText(stringResource(R.string.reverie_time_note), 11f, modifier = motion(4))
                        }
                        ReverieChapter.RHYTHM -> {
                            ReverieDailyChart(recap, sceneElapsed, motionTime, reducedMotion, Modifier.weight(1f).fillMaxWidth())
                            Spacer(Modifier.height(gap))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                FilmStat(number.format(recap.activeDays), stringResource(R.string.reverie_active_days), Modifier.weight(1f).then(motion(2)))
                                Column(Modifier.weight(1f).then(motion(3)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilmText(stringResource(R.string.reverie_peak_day), 10f)
                                    FilmText(recap.month.atDay(recap.peakDay).format(DateTimeFormatter.ofPattern("d MMMM")), 16f)
                                }
                            }
                        }
                        ReverieChapter.ARTIST -> {
                            val artist = recap.artists.first()
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val side = minOf(maxHeight * .86f, maxWidth * .76f)
                                FilmArtwork(artist, images, side, motion(1, 6f).graphicsLayer {
                                    rotationZ = if (reducedMotion) 0f else sin(motionTime() / 4200f) * 4f
                                }, motionTime, reducedMotion, portrait = true)
                                FilmText("01", if (compact) 72f else 100f, FilmWine, title = true,
                                    modifier = Modifier.align(Alignment.BottomStart).then(motion(2, 2f)), maxLines = 1)
                            }
                            Spacer(Modifier.height(gap))
                            TypewriterText(artist.title,
                                iridePageTitleTextStyle().copy(fontSize = if (short) 24.sp else 30.sp,
                                    lineHeight = if (short) 30.sp else 37.sp, fontWeight = FontWeight.Bold),
                                FilmInk, animate = !instant, resetKey = "${recap.month}:${artist.id}", maxLines = 2,
                                modifier = motion(2).fillMaxWidth())
                            Spacer(Modifier.height(gap))
                            FilmText(stringResource(R.string.reverie_minute_value, number.format(artist.milliseconds / 60_000)), 13f, modifier = motion(3))
                        }
                        ReverieChapter.SONG -> {
                            val song = recap.songs.first()
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                FilmArtwork(song, images, minOf(maxHeight * .84f, maxWidth * .73f), motion(1, 7f).graphicsLayer {
                                    rotationZ = if (reducedMotion) 0f else -3f + sin(motionTime() / 3600f) * 5f
                                }, motionTime, reducedMotion)
                            }
                            Spacer(Modifier.height(gap))
                            FilmText(song.title, 27f, title = true, modifier = motion(2), maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            FilmText(song.subtitle, 13f, modifier = motion(3), maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            FilmText(stringResource(R.string.reverie_play_value, number.format(song.plays)), 13f, modifier = motion(4))
                        }
                        ReverieChapter.GENRES -> {
                            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                                recap.genres.take(3).forEachIndexed { index, genre ->
                                    val genreColor = Color(reverieGenreColor(genre.name))
                                    FilmText(genre.name.uppercase(Locale.getDefault()), if (short) 25f else if (index == 0) 32f else 26f,
                                        color = genreColor, title = true, modifier = motion(index + 1), maxLines = 1)
                                    Spacer(Modifier.height(8.dp))
                                    FilmBar(genre.milliseconds.toFloat() / recap.genres.first().milliseconds.coerceAtLeast(1),
                                        genreColor, index, sceneElapsed, motionTime, reducedMotion)
                                    Spacer(Modifier.height(if (short) 8.dp else if (compact) 15.dp else 24.dp))
                                }
                            }
                            FilmText(stringResource(R.string.reverie_genres_note), 11f, modifier = motion(4), maxLines = 2)
                        }
                        ReverieChapter.COLOURS -> {
                            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                                val top = (recap.categories.first().timeListened ?: 1).coerceAtLeast(1)
                                recap.categories.take(if (short) 3 else 4).forEachIndexed { index, category ->
                                    FilmText(category.name, if (short) 20f else if (compact) 23f else 27f, title = true, modifier = motion(index + 1), maxLines = 1)
                                    Spacer(Modifier.height(6.dp))
                                    val color = remember(category.colorHex, category.name) {
                                        Color(runCatching { android.graphics.Color.parseColor(category.colorHex) }
                                            .getOrDefault(reverieGenreColor(category.name)))
                                    }
                                    FilmBar((category.timeListened ?: 0).toFloat() / top, color, index, sceneElapsed, motionTime, reducedMotion)
                                    Spacer(Modifier.height(if (short) 8.dp else if (compact) 12.dp else 20.dp))
                                }
                            }
                            FilmText(stringResource(R.string.reverie_colours_note), 11f, modifier = motion(5), maxLines = 2)
                        }
                        ReverieChapter.ALBUM -> {
                            val album = recap.albums.first()
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val side = minOf(maxHeight * .84f, maxWidth * .69f)
                                Box(Modifier.size(side * 1.4f, side).then(motion(1, 5f))) {
                                    VinylPeekDisc(album.artwork, side, artwork = images[album.id],
                                        modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer {
                                            rotationZ = if (reducedMotion) 0f else motionTime() / 35f
                                        })
                                    FilmArtwork(album, images, side, Modifier.align(Alignment.CenterStart).graphicsLayer {
                                        rotationZ = if (reducedMotion) 0f else sin(motionTime() / 4100f) * 2f
                                    }, motionTime, reducedMotion)
                                }
                            }
                            Spacer(Modifier.height(gap))
                            FilmText(album.title, if (compact) 27f else 32f, title = true, modifier = motion(2), maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            FilmText(album.subtitle, 13f, modifier = motion(3), maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            FilmText(stringResource(R.string.reverie_minute_value, number.format(album.milliseconds / 60_000)), 13f, modifier = motion(4))
                        }
                        ReverieChapter.POSTER -> Unit
                    }
                }
            }
        }
    }
}

/** Short sheets keep all three positions visible by using compact rows instead of tall winner cards. */
/** Motion stays in drawing layers, so cached covers are not requested again for every frame. */
private fun Modifier.filmMotion(elapsed: () -> Float, time: () -> Float, reduced: Boolean, index: Int = 0, drift: Float = 1f) = graphicsLayer {
    val progress = if (reduced) 1f else ((elapsed() - index * 100f) / 900f).coerceIn(0f, 1f)
    val entry = 1 - (1 - progress) * (1 - progress) * (1 - progress)
    alpha = entry
    translationY = if (reduced) 0f else (1 - entry) * 22.dp.toPx() + sin(time() / 1800f + index) * drift.dp.toPx()
}

@Composable
private fun FilmHeading(chapter: ReverieChapter, size: Float, elapsed: () -> Float, time: () -> Float, reduced: Boolean, variant: Int) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (variant == 1) Alignment.End else Alignment.Start) {
        stringResource(reverieChapterTitle(chapter)).split('\n').forEachIndexed { index, line ->
            FilmText(line, if (variant == 2 && index == 0) size * .64f else size,
                title = variant != 2 || index != 0, maxLines = 1,
                modifier = Modifier.filmMotion(elapsed, time, reduced, index, 1f).drawWithContent {
                    val progress = if (reduced) 1f else ((elapsed() - index * 140) / 850).coerceIn(0f, 1f)
                    val reveal = 1 - (1 - progress) * (1 - progress) * (1 - progress)
                    clipRect(top = this.size.height * (1 - reveal)) { this@drawWithContent.drawContent() }
                })
        }
    }
}

@Composable
private fun FilmText(value: String, size: Float, color: Color = FilmInk, title: Boolean = false,
                     modifier: Modifier = Modifier, maxLines: Int = 2) {
    val paper = LocalReveriePaper.current
    val readableColor = remember(color, paper, title, size) {
        Color(com.metrolist.music.models.readableReverieColor(color.toArgb(), paper.toArgb(),
            if (title && size >= 24) 3.0 else 4.5))
    }
    Text(value, modifier, color = readableColor, fontFamily = if (title) FilmTitleFont else SpaceMonoFontFamily,
        fontSize = size.sp, lineHeight = (size * if (title) 1.12f else 1.35f).sp,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun FilmCounter(target: Long, size: Float, elapsed: () -> Float, time: () -> Float, reduced: Boolean) {
    val format = remember { NumberFormat.getIntegerInstance() }
    val progress = if (reduced) 1f else ((elapsed() - 350f) / 2000f).coerceIn(0f, 1f)
    val value = (target * (1 - (1 - progress) * (1 - progress) * (1 - progress))).toLong()
    FilmText(format.format(value), if (format.format(target).length > 5) size * .76f else size, FilmBlue, title = true,
        maxLines = 1, modifier = Modifier.filmMotion(elapsed, time, reduced, 1, 2f).graphicsLayer {
            val scale = if (reduced) 1f else 1f + sin(time() / 2100f) * .012f
            scaleX = scale; scaleY = scale
        })
}

@Composable
private fun FilmStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilmText(value, 34f, title = true, maxLines = 1)
        FilmText(label, 10f, maxLines = 2)
    }
}

@Composable
private fun FilmArtwork(item: ReverieRank, images: Map<String, Bitmap>, side: Dp, modifier: Modifier,
                        time: () -> Float, reduced: Boolean, portrait: Boolean = false) {
    val bitmap = images[item.id]
    val shape = if (portrait) CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(1.dp)
    Box(modifier.size(side.coerceAtLeast(1.dp))
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .padding(1.dp).clip(shape).background(FilmBlue).border(1.dp, FilmInk.copy(alpha = .15f), shape)) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), item.title, contentScale = ContentScale.Crop, filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val t = if (reduced) 0f else time() / 1000f
                scaleX = 1.045f + sin(t * .55f) * .025f; scaleY = scaleX
                translationX = if (reduced) 0f else sin(t * .48f) * 3.dp.toPx()
            })
        else FilmText(item.title.take(1), 30f, Color.White, title = true, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun FilmBar(fraction: Float, color: Color, index: Int, elapsed: () -> Float, time: () -> Float, reduced: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(7.dp).filmMotion(elapsed, time, reduced, index + 1, 0f)) {
        val reveal = if (reduced) 1f else ((elapsed() - 400 - index * 180) / 1400).coerceIn(0f, 1f)
        val width = size.width * fraction.coerceIn(0f, 1f) * (1 - (1 - reveal) * (1 - reveal))
        drawRect(color, size = Size(width, size.height))
        if (!reduced && width > 0) {
            val scan = (time() / 2200f + index * .27f) % 1f
            drawRect(Color.White.copy(alpha = .28f), Offset(width * scan, 0f), Size(minOf(8.dp.toPx(), width * (1 - scan)), size.height))
        }
    }
}

@Composable
private fun ReverieDailyChart(recap: ReverieRecap, elapsed: () -> Float, time: () -> Float, reduced: Boolean, modifier: Modifier) {
    val blue = FilmBlue
    val wine = FilmWine
    Canvas(modifier.padding(top = 12.dp, bottom = 12.dp)) {
        if (size.width <= 0 || size.height <= 0) return@Canvas
        val peak = recap.dailyMilliseconds.maxOrNull()?.coerceAtLeast(1) ?: 1
        val gap = 3.dp.toPx()
        val bar = (size.width - gap * (recap.dailyMilliseconds.size - 1)) / recap.dailyMilliseconds.size
        recap.dailyMilliseconds.forEachIndexed { day, value ->
            val progress = if (reduced) 1f else ((elapsed() - 350 - day * 30) / 1200).coerceIn(0f, 1f)
            val height = (size.height * .90f * value / peak * (1 - (1 - progress) * (1 - progress))).coerceAtLeast(2.dp.toPx())
            val alpha = if (reduced) 1f else .78f + .22f * (.5f + .5f * sin(time() / 850f - day * .24f))
            drawRect((if (day + 1 == recap.peakDay) wine else blue).copy(alpha = alpha),
                Offset(day * (bar + gap), size.height - height), Size(bar.coerceAtLeast(0f), height))
        }
    }
}

@Composable
private fun ReveriePosterScene(recap: ReverieRecap, images: Map<String, Bitmap>,
                               elapsed: () -> Float, time: () -> Float, reduced: Boolean) {
    val format = remember { NumberFormat.getIntegerInstance() }
    val hero = recap.albums.firstOrNull() ?: recap.songs.first()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 440.dp
        val landscape = maxWidth > maxHeight * 1.25f
        val gap = if (compact) 6.dp else 10.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_logo), null, colorFilter = ColorFilter.tint(FilmInk), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                FilmText("IRIDE / " + stringResource(R.string.reverie_name).uppercase(Locale.ROOT), 12f, maxLines = 1,
                    modifier = Modifier.weight(1f))
                FilmText(recap.month.year.toString(), 11f, maxLines = 1)
            }
            Spacer(Modifier.height(gap))
            FilmText(recap.month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())).uppercase(Locale.getDefault()),
                if (compact) 30f else 42f, title = true, maxLines = 1)
            Spacer(Modifier.height(gap))
            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PosterAlbum(hero, recap.albums.isNotEmpty(), images, time, reduced, Modifier.weight(1f))
                    PosterArtists(recap, Modifier.weight(1f).align(Alignment.CenterVertically), compact = true)
                }
            } else {
                PosterAlbum(hero, recap.albums.isNotEmpty(), images, time, reduced, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(gap))
                HorizontalDivider(color = FilmInk.copy(alpha = .3f))
                Spacer(Modifier.height(gap))
                PosterArtists(recap, Modifier.fillMaxWidth(), compact)
            }
            Spacer(Modifier.height(gap))
            HorizontalDivider(color = FilmInk.copy(alpha = .3f))
            Row(Modifier.fillMaxWidth().padding(top = gap), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1.3f)) {
                    FilmText(format.format(recap.minutes), if (compact) 25f else 32f, title = true, maxLines = 1)
                    FilmText(stringResource(R.string.reverie_minutes), 8f, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    FilmText(format.format(recap.songs.size), if (compact) 25f else 32f, title = true, maxLines = 1)
                    FilmText(stringResource(R.string.reverie_tracks), 8f, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    FilmText(format.format(recap.activeDays), if (compact) 25f else 32f, title = true, maxLines = 1)
                    FilmText(stringResource(R.string.reverie_active_days), 8f, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun PosterAlbum(item: ReverieRank, album: Boolean, images: Map<String, Bitmap>, time: () -> Float,
                        reduced: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilmArtwork(item, images, minOf(maxHeight, maxWidth * .85f), Modifier, time, reduced)
        }
        Spacer(Modifier.height(6.dp))
        FilmText(stringResource(if (album) R.string.reverie_top_album else R.string.reverie_top_song), 8f, maxLines = 1)
        FilmText(item.title, 20f, title = true, maxLines = 1)
        FilmText(item.subtitle, 10f, maxLines = 1)
    }
}

@Composable
private fun PosterArtists(recap: ReverieRecap, modifier: Modifier, compact: Boolean) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
        FilmText(stringResource(R.string.reverie_your_artists), 9f, maxLines = 1)
        recap.artists.take(5).forEachIndexed { index, artist ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilmText("0${index + 1}", 10f, Color(reverieGenreColor(artist.id)), maxLines = 1)
                FilmText(artist.title, if (compact) 12f else 14f, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}
