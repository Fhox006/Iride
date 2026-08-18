/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Optimized for minimal recomposition during navigation
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.music.ui.component

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AutoLinkFeaturedArtistsKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.HideDurationForStandardSongsKey
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ShowExplicitBadgeKey
import com.metrolist.music.constants.ShowFeaturedArtistsInTopSongsKey
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.constants.SquareVideoThumbnailKey
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.audio.AudioBandLevels
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.TitleFeaturingParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableCollectionItemScope

const val ActiveBoxAlpha = 0.6f

/**
 * 0f→1f smoothed toggle for the "selected / loading" thumbnail outline (thumbnail shrink + row
 * border). Only for that in-between state — once the track is actually sounding, [NowPlayingOverlay]
 * takes over so the two states read as visually distinct (see [ItemThumbnail]).
 */
@Composable
fun rememberSelectionProgress(isActive: Boolean): Float {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return if (isActive) 1f else 0f
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
        label = "selectionOutline",
    )
    return progress
}

// Width and gap stay proportional to bar height (≈3:14 and ≈2:14) so the whole mark scales as
// one unit instead of thin bars stretching tall on a big grid tile.
private const val VisualizerWidthToHeight = 3f / 14f
private const val VisualizerGapToHeight = 2f / 14f
private val VisualizerBarMinHeight = 10.dp
private val VisualizerBarDefaultHeight = 14.dp
private val VisualizerBarMaxHeightCap = 22.dp
private const val VisualizerBarMinFraction = 0.28f

/**
 * Squared, monospace-flavored equalizer bars — the "this one is actually sounding" tell.
 * Deliberately not a stock play/sound icon: three flat-topped rectangles (no rounding), matching
 * the app's geometric styling instead of borrowing a system glyph. Each bar tracks a real band
 * (bass/mid/treble) of the currently playing track via [AudioVisualizerAnalyzer], smoothed so it
 * reads as a gentle pulse rather than a jumpy spectrum analyzer.
 */
@Composable
private fun AudioVisualizerBars(
    modifier: Modifier = Modifier,
    barHeight: Dp = VisualizerBarDefaultHeight,
    color: Color = Color.White,
) {
    val reducedMotion = rememberReducedMotion()
    val playerConnection = LocalPlayerConnection.current
    val bandLevels by (playerConnection?.audioBandLevels
        ?: remember { MutableStateFlow(AudioBandLevels()) }).collectAsState()

    val smoothSpec = tween<Float>(280, easing = LinearOutSlowInEasing)
    fun smoothed(target: Float): Float {
        val fraction = VisualizerBarMinFraction + target * (1f - VisualizerBarMinFraction)
        return fraction
    }
    val bar1 by animateFloatAsState(smoothed(bandLevels.bass), smoothSpec, label = "audioVisualizerBar1")
    val bar2 by animateFloatAsState(smoothed(bandLevels.mid), smoothSpec, label = "audioVisualizerBar2")
    val bar3 by animateFloatAsState(smoothed(bandLevels.treble), smoothSpec, label = "audioVisualizerBar3")
    val fractions = if (reducedMotion) listOf(0.45f, 1f, 0.7f) else listOf(bar1, bar2, bar3)

    Row(
        modifier = modifier.height(barHeight),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(barHeight * VisualizerGapToHeight),
    ) {
        fractions.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barHeight * VisualizerWidthToHeight)
                    .fillMaxHeight(fraction)
                    .background(color)
            )
        }
    }
}

/**
 * Drawn over a thumbnail whose track is the one actually making sound — a dark scrim plus
 * [AudioVisualizerBars], distinct from the lighter "selected / loading" border+shrink so the two
 * states can't be confused (see [ItemThumbnail], [LocalThumbnail]).
 */
@Composable
private fun NowPlayingOverlay(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        // Scales with the thumbnail — a fixed size would shrink to an unreadable speck on a
        // 128dp grid tile, or crowd a 48dp list row.
        val barHeight = (minOf(maxWidth, maxHeight) * 0.3f).coerceIn(VisualizerBarMinHeight, VisualizerBarMaxHeightCap)
        AudioVisualizerBars(barHeight = barHeight)
    }
}

/** How long the turntable needle-drop SFX plays before the first track actually starts. */
const val NeedleDropLeadInMs = 350L

val LocalItemHorizontalPadding = compositionLocalOf { true }

/**
 * Edge length asked of the artwork CDN for feed thumbnails.
 *
 * Sized for the largest surface [ItemThumbnail] draws (grid tiles top out around 128dp, plus the
 * wider Home carousel cards) and deliberately kept under 480: [resize] treats anything from there
 * up as a request for i.ytimg.com's `maxresdefault`, which is both larger than any tile needs and
 * missing entirely for a good number of videos.
 */
private const val ThumbnailRequestSize = 448

/**
 * A vinyl record, printed with the album's own cover art. Used behind the cover on the Album
 * screen (New Iride UI) while the album is playing — peeking out to the side and spinning; see
 * [com.metrolist.music.ui.screens.AlbumScreen]. Draw this *before* the square album
 * [ItemThumbnail] in the same Box so the square covers the hidden portion of the disc.
 */
val VinylPeekDiscBaseBottom = Color(0xFF060607)

// Real-LP proportions: a 12" label sits at ~38-40% of the disc's diameter.
private const val VinylPeekDiscLabelFraction = 0.40f
private const val VinylPeekDiscSpindleFraction = 0.035f
private const val VinylPeekDiscGrooveCount = 6

@Composable
fun VinylPeekDisc(
    thumbnailUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl?.resize(160, 160))
            .crossfade(true)
            .build(),
    )
    val painterState by painter.state.collectAsState()
    // The disc only makes sense once we actually have art to print on its label — don't show
    // an empty/placeholder record while the thumbnail is still loading or missing.
    val isLoaded = painterState is AsyncImagePainter.State.Success

    AnimatedVisibility(
        visible = isLoaded,
        enter = fadeIn(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotationDegrees }
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(VinylPeekDiscBaseBottom)
                    val outerRadius = this.size.minDimension / 2f * 0.97f
                    val labelRadius = this.size.minDimension / 2f * VinylPeekDiscLabelFraction
                    repeat(VinylPeekDiscGrooveCount) { i ->
                        val t = i / (VinylPeekDiscGrooveCount - 1f)
                        val radius = outerRadius - (outerRadius - labelRadius - 4.dp.toPx()) * t
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 0.6.dp.toPx()),
                        )
                    }
                }
                // Faint rim so the disc's silhouette stays visible even against a black cover.
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.14f), shape = CircleShape)
        ) {
            // Center label — the only place album artwork appears on the disc.
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(VinylPeekDiscLabelFraction)
                    .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.22f), shape = CircleShape)
            )
            // Spindle hole.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(VinylPeekDiscSpindleFraction)
                    .clip(CircleShape)
                    .background(VinylPeekDiscBaseBottom)
                    .border(width = 0.5.dp, color = Color.Black.copy(alpha = 0.6f), shape = CircleShape)
            )
        }
    }
}

@Composable
fun currentGridThumbnailHeight(): Dp {
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    return if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
}

// Basic list item - optimized with inline to reduce recomposition
@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    activeBackgroundColor: Color? = null,
    selectedBackgroundColor: Color? = null,
    isAvailable: Boolean = true,
    showDivider: Boolean = true,
) {
    val applyHPad = LocalItemHorizontalPadding.current
    val highlightShape = RoundedCornerShape(ThumbnailCornerRadius)
    val hPad = if (applyHPad) 12.dp else 0.dp
    val plain = !isActive && isSelected != true && showDivider

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = when {
                isActive -> {
                    // No row-level frame: the cover thumbnail's own active border already signals
                    // what's playing, a second border around the whole row was redundant.
                    Modifier
                        .padding(horizontal = hPad, vertical = 2.dp)
                        .height(ListItemHeight)
                }
                isSelected == true -> {
                    Modifier
                        .padding(horizontal = hPad, vertical = 2.dp)
                        .clip(highlightShape)
                        .background(selectedBackgroundColor ?: MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.22f))
                        .height(ListItemHeight)
                }
                else -> {
                    Modifier
                        .padding(horizontal = hPad, vertical = 0.dp)
                        .height(ListItemHeight)
                }
            }
        ) {
            Box(
                modifier = Modifier.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                thumbnailContent()
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .align(Alignment.Center)
                            .background(
                                Color.Black.copy(alpha = 0.25f),
                                RoundedCornerShape(ThumbnailCornerRadius)
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(ListThumbnailSize / 2)
                                .align(Alignment.Center)
                                .graphicsLayer { alpha = 1f }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        subtitle()
                    }
                }
            }

            trailingContent()
        }

        if (plain) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = hPad + 4.dp + ListThumbnailSize + 6.dp, end = hPad),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: AnnotatedString?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    activeBackgroundColor: Color? = null,
    selectedBackgroundColor: Color? = null,
    showDivider: Boolean = true,
) = ListItem(
    title = title,
    subtitle = {
        badges()
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isSelected = isSelected,
    isActive = isActive,
    activeBackgroundColor = activeBackgroundColor,
    selectedBackgroundColor = selectedBackgroundColor,
    showDivider = showDivider,
)

// merge badges and subtitle text and pass to basic list item
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    // Lets callers (Album/Playlist screens, New Iride UI only) render the subtitle in the same
    // color as the title instead of the default muted secondary — used so a "feat. Artist" credit
    // doesn't read as visually washed-out compared to the rest of the row. Defaults to the
    // existing secondary color everywhere else so this is a no-op unless explicitly overridden.
    subtitleColor: Color = MaterialTheme.colorScheme.secondary,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    activeBackgroundColor: Color? = null,
    selectedBackgroundColor: Color? = null,
    showDivider: Boolean = true,
) = ListItem(
    title = title,
    subtitle = {
        badges()

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = subtitleColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isSelected = isSelected,
    isActive = isActive,
    activeBackgroundColor = activeBackgroundColor,
    selectedBackgroundColor = selectedBackgroundColor,
    showDivider = showDivider,
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
) {
    val applyHPad = LocalItemHorizontalPadding.current
    // New Iride UI: tiles sit closer together laterally than the classic UI's 8dp gap.
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val hPad = if (applyHPad) (if (topNavigationBarEnabled) 4.dp else 8.dp) else 0.dp
    Column(
        modifier = if (fillMaxWidth) {
            modifier
                .padding(horizontal = hPad, vertical = 4.dp)
                .fillMaxWidth()
        } else {
            modifier
                .padding(horizontal = hPad, vertical = 4.dp)
                .width(size * thumbnailRatio)
        }
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(size)
            }
                .aspectRatio(thumbnailRatio)
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        title()

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            subtitle()
        }
    }
}

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
) = GridItem(
    modifier = modifier,
    title = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        Text(
            text = title,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    },
    subtitle = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        Text(
            text = subtitle,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
    badges = badges,
    thumbnailContent = thumbnailContent,
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth,
    size = size,
)

private fun shouldHideDuration(durationSeconds: Int, hideDurationForStandard: Boolean): Boolean {
    if (!hideDurationForStandard) return false
    return durationSeconds in (1 * 60)..(5 * 60)
}

/**
 * Lazily strips a "feat./featuring/ft." credit out of [song]'s title and links the
 * collaborator as a real artist, wherever a song title is first rendered. Idempotent:
 * a quick substring check skips songs whose titles were already cleaned up.
 */
@Composable
private fun AutoLinkFeaturedArtistEffect(song: Song) {
    val autoLink by rememberPreference(AutoLinkFeaturedArtistsKey, defaultValue = true)
    val database = LocalDatabase.current
    val looksFeatured = remember(song.song.title) { TitleFeaturingParser.looksFeatured(song.song.title) }
    LaunchedEffect(song.id, song.song.title, autoLink, looksFeatured) {
        if (autoLink && looksFeatured) {
            database.query { linkFeaturedArtist(song) }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showDownloadIcon: Boolean = true,
    subtitleOverride: String? = null,
    // See ListItem's subtitleColor doc — null keeps the existing muted secondary color.
    subtitleColor: Color? = null,
    // Unseen-song marker (Featuring section / Top Songs / album rows), cleared by viewport visibility.
    showNewMarker: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showNewMarker) Icon.New()
        if (showLikedIcon && song.song.liked && albumIndex == null) {
            Icon.Starred()
        }
        if (song.song.explicit) {
            Icon.Explicit()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    activeBackgroundColor: Color? = null,
    selectedBackgroundColor: Color? = null,
    showInLibraryIcon: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    hairlineBorder: Boolean = false,
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = true)
    AutoLinkFeaturedArtistEffect(song)

    val content: @Composable () -> Unit = {
        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
        ListItem(
            showDivider = showDivider,
            title = song.song.title,
            subtitle = subtitleOverride ?: if (shouldHideDuration(song.song.duration, hideDurationForStandard)) {
                song.orderedArtists.joinToString { it.name }
            } else {
                joinByBullet(
                    song.orderedArtists.joinToString { it.name },
                    makeTimeString(song.song.duration * 1000L)
                )
            },
            subtitleColor = subtitleColor ?: MaterialTheme.colorScheme.secondary,
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = song.song.thumbnailUrl,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = SquircleShape(radius = ThumbnailCornerRadius, cornerSmoothing = 0.5f),
                    modifier = Modifier.size(ListThumbnailSize),
                    showLikedStar = showLikedIcon && song.song.liked,
                    hairlineBorder = hairlineBorder,
                )
            },
            trailingContent = {
                if (showInLibraryIcon) {
                    Icon(
                        painter = painterResource(R.drawable.library_add_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                trailingContent()
            },
            modifier = modifier,
            isSelected = isSelected,
            isActive = isActive,
            activeBackgroundColor = activeBackgroundColor,
            selectedBackgroundColor = selectedBackgroundColor,
        )
    }

    if (isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

/**
 * The single trailing button a song row shows for its overflow menu / drag handle. Reorder mode
 * never adds a second button next to it — this one morphs in place: three dots crossfade into
 * three lines, and the same slot becomes the [sh.calvin.reorderable] drag handle.
 */
@Composable
fun ReorderableCollectionItemScope.SongRowReorderButton(
    reordering: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val morph by animateFloatAsState(
        targetValue = if (reordering) 1f else 0f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(IrideMotion.Quick, easing = IrideMotion.EaseOutQuart)
        },
        label = "reorderButtonMorph",
    )

    androidx.compose.material3.IconButton(
        onClick = { if (!reordering) onMenuClick() },
        modifier = modifier.then(if (reordering) Modifier.draggableHandle() else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = if (reordering) null else stringResource(R.string.menu),
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - morph
                    val scale = 1f - morph * 0.4f
                    scaleX = scale
                    scaleY = scale
                },
            )
            Icon(
                painter = painterResource(R.drawable.drag_handle),
                contentDescription = if (reordering) stringResource(R.string.reorder) else null,
                modifier = Modifier.graphicsLayer {
                    alpha = morph
                    val scale = 0.6f + morph * 0.4f
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon.Starred()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        AutoLinkFeaturedArtistEffect(song)
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        Text(
            text = song.song.title,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
        val subtitleText = if (shouldHideDuration(song.song.duration, hideDurationForStandard)) {
            song.orderedArtists.joinToString { it.name }
        } else {
            joinByBullet(
                song.orderedArtists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            )
        }
        Text(
            text = subtitleText,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
    badges = badges,
    thumbnailContent = {
        val gridHeight = currentGridThumbnailHeight()
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        ItemThumbnail(
            thumbnailUrl = song.song.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (topNavigationBarEnabled) RoundedCornerShape(5.dp) else SquircleShape(radius = squircleRadius, cornerSmoothing = 0.5f),
            modifier = Modifier.size(gridHeight)
        )
        if (!isActive) {
            OverlayPlayButton(
                visible = true
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = if (artist.songCount > 0) pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount) else null,
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                // 900px of avatar for a 48dp circle was ~18x the pixels this ever draws.
                .data(artist.artist.thumbnailUrl?.resize(192, 192))
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ListThumbnailSize)
                .clip(CircleShape)
                .border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && artist.artist.bookmarkedAt != null) {
            Icon.Starred()
        }
    },
    fillMaxWidth: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
) = GridItem(
    title = artist.artist.name,
    subtitle = if (artist.songCount > 0) pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount) else "",
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artist.artist.thumbnailUrl?.resize(ThumbnailRequestSize, ThumbnailRequestSize))
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), CircleShape)
        )
    },
    fillMaxWidth = fillMaxWidth,
    size = size,
    modifier = modifier
)

/**
 * "New from artists you follow" row item. A thin ring around the avatar carries the unread-release
 * signal instead of a floating numeral badge, so the indicator and the circular photo share one
 * shape language (concentric circles) rather than a shape clashing against a shape.
 */
@Composable
fun ArtistNewReleaseRingItem(
    artist: Artist,
    newSongCount: Int,
    modifier: Modifier = Modifier,
) {
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val avatarSize = currentGridThumbnailHeight()
    Column(
        modifier = modifier.width(avatarSize),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(avatarSize),
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .border(avatarSize * 0.012f, MaterialTheme.colorScheme.onBackground, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artist.artist.thumbnailUrl?.resize(300, 300))
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
                    .background(NotificationDotGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (newSongCount > 9) "9+" else newSongCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.artist.name,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.labelMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.labelMedium
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), album.id) {
            withContext(Dispatchers.IO) {
                value = database.albumSongs(album.id).first()
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon.Starred()
        }
        if (album.album.explicit) {
            Icon.Explicit()
        }
        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = album.album.title,
    subtitle = joinByBullet(
        album.artists.joinToString { it.name },
        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        album.album.year?.toString()
    ),
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f),
            modifier = Modifier.size(ListThumbnailSize),
            hairlineBorder = true
        )
    },
    trailingContent = trailingContent,
    modifier = modifier
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), album.id) {
            withContext(Dispatchers.IO) {
                value = database.albumSongs(album.id).first()
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon.Starred()
        }
        if (album.album.explicit) {
            Icon.Explicit()
        }
        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
) = GridItem(
    title = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        Text(
            text = album.album.title,
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        Text(
            text = joinByBullet(album.artists.joinToString { it.name }, album.album.year?.toString()),
            style = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (topNavigationBarEnabled) RoundedCornerShape(5.dp) else SquircleShape(radius = squircleRadius, cornerSmoothing = 0.5f),
            hairlineBorder = true
        )
    },
    fillMaxWidth = fillMaxWidth,
    size = size,
    modifier = modifier
)

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), playlist.id) {
            withContext(Dispatchers.IO) {
                value = database.playlistSongs(playlist.id).first().map { it.song }
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        Icon.Download(downloadState)
    },
    trailingContent: @Composable RowScope.() -> Unit = {}
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(
                R.plurals.n_song,
                playlist.songCount,
                playlist.songCount
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        // Matched by stable playlist id (not the localized display name) so this keeps showing
        // the glass star cover even when the Liked Songs entry is renamed to "Starred" for New
        // Iride UI — a name-string match would silently fall through the moment the texts differ.
        val isLikedPlaylist = playlist.playlist.id == PlaylistEntity.LIKED_PLAYLIST_ID
        if (isLikedPlaylist) {
            // Same frosted-glass mosaic + badge as the playlist's own screen (AutoPlaylistScreen)
            // so this entry looks identical whether seen from inside or from the Library list.
            GlassPlaylistCover(
                thumbnails = playlist.thumbnails,
                icon = R.drawable.star,
                size = ListThumbnailSize,
                shape = SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f),
                iconSizeFraction = 0.65f,
            )
        } else {
            PlaylistThumbnail(
                thumbnails = playlist.thumbnails,
                size = ListThumbnailSize,
                placeHolder = {
                    val painter = when {
                        playlist.playlist.name == stringResource(R.string.offline) -> R.drawable.offline
                        playlist.playlist.name == stringResource(R.string.cached_playlist) -> R.drawable.cached
                        // R.drawable.backup as placeholder
                        playlist.playlist.name == stringResource(R.string.uploaded_playlist) -> R.drawable.backup
                        else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                    }
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.8f),
                        modifier = Modifier.size(ListThumbnailSize / 2)
                    )
                },
                shape = SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f),
                hairlineBorder = true
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), playlist.id) {
            withContext(Dispatchers.IO) {
                value = database.playlistSongs(playlist.id).first().map { it.song }
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        Icon.Download(downloadState)
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val subtitle = if (autoPlaylist) {
            ""
        } else {
            if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.playlist.remoteSongCount,
                    playlist.playlist.remoteSongCount
                )
            } else {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.songCount,
                    playlist.songCount
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        val width = maxWidth
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        val shape = if (topNavigationBarEnabled) RoundedCornerShape(5.dp) else SquircleShape(radius = squircleRadius, cornerSmoothing = 0.5f)
        // See PlaylistListItem's placeholder for why this matches by id, not name.
        val isLikedPlaylist = playlist.playlist.id == PlaylistEntity.LIKED_PLAYLIST_ID
        if (isLikedPlaylist) {
            // Same frosted-glass mosaic + badge as the playlist's own screen (AutoPlaylistScreen)
            // so this entry looks identical whether seen from inside or from the Library grid.
            GlassPlaylistCover(
                thumbnails = playlist.thumbnails,
                icon = R.drawable.star,
                size = width,
                shape = shape,
                iconSizeFraction = 0.65f,
            )
        } else {
            PlaylistThumbnail(
                thumbnails = playlist.thumbnails,
                size = width,
                placeHolder = {
                    val painter = when {
                        playlist.playlist.name == stringResource(R.string.offline) -> R.drawable.offline
                        playlist.playlist.name == stringResource(R.string.cached_playlist) -> R.drawable.cached
                        // R.drawable.backup as placeholder
                        playlist.playlist.name == stringResource(R.string.uploaded_playlist) -> R.drawable.backup
                        else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(painter),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier = Modifier.size(width / 2)
                        )
                    }
                },
                shape = shape,
                hairlineBorder = true
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
    ListItem(
        title = mediaMetadata.title,
        subtitle = if (mediaMetadata.suggestedBy != null) {
            buildAnnotatedString {
                append(mediaMetadata.artists.joinToString { it.name })
                append(" • ")
                append(makeTimeString(mediaMetadata.duration * 1000L))
                append(" • ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(mediaMetadata.suggestedBy)
                }
            }
        } else {
            if (shouldHideDuration(mediaMetadata.duration, hideDurationForStandard)) {
                AnnotatedString(mediaMetadata.artists.joinToString { it.name })
            } else {
                AnnotatedString(
                    joinByBullet(
                        mediaMetadata.artists.joinToString { it.name },
                        makeTimeString(mediaMetadata.duration * 1000L)
                    )
                )
            }
        },
        badges = { if (mediaMetadata.explicit) Icon.Explicit()},
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumIndex = null,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                modifier = Modifier.size(ListThumbnailSize)
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    // Unseen-song marker — Featuring section / Top Songs / album rows, cleared by viewport visibility.
    showNewMarker: Boolean = false,
    // What's new, spelled out next to the dot (e.g. "FEAT" in the Featuring carousel) — a bare dot
    // didn't say why the row was marked new.
    newMarkerLabel: String? = null,
    // Featuring section only: appends " — Album Name" to the subtitle for tracks that live on
    // someone else's album, so "ArtistA, ArtistB feat. You" reads as "... feat. You — Album Name".
    showAlbumInSubtitle: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

        if (showNewMarker) Icon.New(label = newMarkerLabel)
        if ((item is SongItem && song?.song?.liked == true) ||
            (item is AlbumItem && album?.album?.bookmarkedAt != null)
        ) {
            Icon.Starred()
        }
        if (item.explicit) Icon.Explicit()
        if (item is SongItem) {
            val download by LocalDownloadUtil.current.getDownload(item.id).collectAsState(null)
            Icon.Download(download?.state)
        }
    },
    hairlineBorder: Boolean = item is AlbumItem,
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = true)

    val content: @Composable () -> Unit = {
        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
        ListItem(
            title = item.title,
            subtitle = when (item) {
                is SongItem -> {
                    val database = LocalDatabase.current
                    val showFeaturedArtists by rememberPreference(ShowFeaturedArtistsInTopSongsKey, defaultValue = true)
                    val localArtists by produceState<List<com.metrolist.music.db.entities.ArtistEntity>?>(initialValue = null, item.id) {
                        value = database.song(item.id).firstOrNull()?.orderedArtists
                    }
                    // The API's own artists list sometimes truncates collaborators; when a local
                    // record picked up more (e.g. via TitleFeaturingParser), prefer it.
                    val artistNames = if (showFeaturedArtists && (localArtists?.size ?: 0) > item.artists.size) {
                        localArtists!!.joinToString { it.name }
                    } else {
                        item.artists.joinToString { it.name }
                    }
                    val durationSec = item.duration
                    val base = if (durationSec != null && shouldHideDuration(durationSec, hideDurationForStandard)) {
                        artistNames
                    } else {
                        joinByBullet(artistNames, makeTimeString(durationSec?.times(1000L)))
                    }
                    val albumName = item.album?.name
                    if (showAlbumInSubtitle && !albumName.isNullOrBlank()) "$base — $albumName" else base
                }
                is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                is ArtistItem -> null
                is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                is PodcastItem -> joinByBullet(item.author?.name, item.episodeCountText)
                is EpisodeItem -> joinByBullet(item.author?.name, item.publishDateText, makeTimeString(item.duration?.times(1000L)))
            },
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = item.thumbnail,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
                    modifier = Modifier.size(ListThumbnailSize),
                    hairlineBorder = hairlineBorder,
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive
        )
    }

    if (item is SongItem && isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = item.copy(thumbnail = item.thumbnail.resize(544,544)).toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    // Unseen-release marker (artist page Album/Single/EP rows) — plain dot until the item is opened.
    showNewMarker: Boolean = false,
    // What's new, spelled out next to the dot (ALBUM/EP/SINGLE) — a bare dot didn't say what kind
    // of release just came out.
    newMarkerLabel: String? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

        if (showNewMarker) Icon.New(label = newMarkerLabel)
        if ((item is SongItem && song?.song?.liked == true) ||
            (item is AlbumItem && album?.album?.bookmarkedAt != null)
        ) {
            Icon.Starred()
        }
        if (item.explicit) Icon.Explicit()
        if (item is SongItem) {
            val download by LocalDownloadUtil.current.getDownload(item.id).collectAsState(null)
            Icon.Download(download?.state)
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    thumbnailCornerRadius: Dp = ThumbnailCornerRadius,
    // Overrides the shape computed below entirely. Square (ratio 1f) tiles in the New Iride UI
    // otherwise always draw a fixed 5.dp RoundedCornerShape regardless of `thumbnailCornerRadius` or
    // `size` — fine at the ~150-180dp this was tuned for, but at a much larger tile the same 5.dp
    // reads as almost square. Rather than change that fixed value for every existing square tile in
    // the app, callers that intentionally render an oversized tile can pass their own shape here.
    thumbnailShape: Shape? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
    showTitle: Boolean = true,
    // Used when item.artists is null/empty (always true for albums parsed off
    // an artist's own page) so the subtitle isn't left artist-less.
    fallbackArtistName: String? = null,
    hairlineBorder: Boolean = item is AlbumItem,
) {
    val squareVideoThumbnail by rememberPreference(SquareVideoThumbnailKey, defaultValue = true)
    val defaultRatio = if (item is SongItem) 16f / 9 else 1f
    val effectiveThumbnailRatio = when {
        thumbnailRatio != defaultRatio -> thumbnailRatio
        item is SongItem && squareVideoThumbnail -> 1f
        else -> thumbnailRatio
    }
    GridItem(
    title = {
        if (showTitle) {
            val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
            Text(
                text = item.title,
                style = if (topNavigationBarEnabled) {
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.basicMarquee().fillMaxWidth()
            )
        }
    },
    subtitle = {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
        val subtitle = when (item) {
            is SongItem -> {
                val durationSec = item.duration
                if (durationSec != null && shouldHideDuration(durationSec, hideDurationForStandard)) {
                    item.artists.joinToString { it.name }
                } else {
                    joinByBullet(item.artists.joinToString { it.name }, makeTimeString(durationSec?.times(1000L)))
                }
            }
            is AlbumItem -> {
                val artistName = item.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }
                    ?: fallbackArtistName
                joinByBullet(artistName, item.year?.toString())
            }
            is ArtistItem -> null
            is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
            is PodcastItem -> joinByBullet(item.author?.name, item.episodeCountText)
            is EpisodeItem -> joinByBullet(item.author?.name, makeTimeString(item.duration?.times(1000L)))
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = if (topNavigationBarEnabled) {
                    MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = thumbnailShape ?: when {
                item is ArtistItem -> CircleShape
                // Non-square thumbnails (e.g. 16:9 videos) don't suit a squircle
                effectiveThumbnailRatio != 1f -> RoundedCornerShape(thumbnailCornerRadius)
                topNavigationBarEnabled -> RoundedCornerShape(5.dp)
                else -> SquircleShape(radius = squircleRadius, cornerSmoothing = 0.5f)
            },
            hairlineBorder = hairlineBorder,
        )

        if (item is SongItem && !isActive) {
            OverlayPlayButton(
                visible = true
            )
        }
    },
    thumbnailRatio = effectiveThumbnailRatio,
    fillMaxWidth = fillMaxWidth,
    size = size,
    modifier = modifier
)
}

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    subtitle = {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(
                iterations = 3,
                initialDelayMillis = 1000,
                velocity = 30.dp
            )
        )
    },
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = true,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    subtitle = {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(
                iterations = 3,
                initialDelayMillis = 1000,
                velocity = 30.dp
            )
        )
    },
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = false,
            isPlaying = false,
            shape = CircleShape,
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    subtitle = {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(
                iterations = 3,
                initialDelayMillis = 1000,
                velocity = 30.dp
            )
        )
    },
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = true
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    thumbnailRatio: Float = 1f,
    showLikedStar: Boolean = false,
    hairlineBorder: Boolean = false,
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val selectionBorderColor = if (topNavigationBarEnabled) {
        Color.White.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    // Shrink/border reads as "selected or still loading" — once the track is actually sounding,
    // NowPlayingOverlay below takes over so the two states can't be mistaken for each other.
    val selectionProgress = rememberSelectionProgress(isActive && !isPlaying)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(thumbnailRatio)
            .clip(shape)
            .then(
                if (selectionProgress > 0f) {
                    Modifier
                        .border(
                            BorderStroke(1.5.dp, selectionBorderColor.copy(alpha = selectionBorderColor.alpha * selectionProgress)),
                            shape,
                        )
                        .padding(4.dp * selectionProgress)
                } else if (hairlineBorder) {
                    Modifier.border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), shape)
                } else {
                    Modifier
                }
            )
    ) {
        if (albumIndex != null && showLikedStar) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(12.dp)
            )
        }

        if (albumIndex == null) {
            // Every call site hands this its raw thumbnail URL, and rememberAsyncImagePainter —
            // unlike AsyncImage — installs no size resolver, so Coil defaults to SizeResolver
            // .ORIGINAL: each row of every feed was downloading and decoding full-resolution
            // artwork (lh3 originals run 1000-2400px) to draw a 48-128dp tile. Asking the CDN for
            // ThumbnailRequestSize cuts the bytes, and the constraints resolver decodes to the
            // size actually on screen so a 48dp row doesn't hold a 448px bitmap in the cache.
            val sizeResolver = rememberConstraintsSizeResolver()
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl?.resize(ThumbnailRequestSize, ThumbnailRequestSize))
                    .size(sizeResolver)
                    .crossfade(200)
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .build(),
            )
            val painterState by painter.state.collectAsState()
            val isLoaded = painterState is AsyncImagePainter.State.Success

            AnimatedVisibility(
                visible = !isLoaded,
                enter = fadeIn(),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(0.4f),
                    )
                }
            }

            Image(
                painter = painter,
                contentDescription = null,
                contentScale = if (shape == CircleShape || cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .then(sizeResolver)
                    .clip(shape)
            )
        }

        if (albumIndex != null) {
            Text(
                text = albumIndex.toString(),
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (isActive && isPlaying && !isSelected) {
            NowPlayingOverlay()
        }

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun LocalThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    showCenterPlay: Boolean = false,
    playButtonVisible: Boolean = false,
    thumbnailRatio: Float = 1f
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val selectionBorderColor = if (topNavigationBarEnabled) {
        Color.White.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val selectionProgress = rememberSelectionProgress(isActive && !isPlaying)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(thumbnailRatio)
            .clip(shape)
            .border(
                BorderStroke(1.5.dp, selectionBorderColor.copy(alpha = selectionBorderColor.alpha * selectionProgress)),
                shape,
            )
            .padding(4.dp * selectionProgress)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (shape == CircleShape || cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        )

        if (isActive && isPlaying) {
            NowPlayingOverlay()
        }

        if (showCenterPlay) {
            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        if (playButtonVisible) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistThumbnail(
    thumbnails: List<String>,
    size: Dp,
    placeHolder: @Composable () -> Unit,
    shape: Shape,
    cacheKey: String? = null,
    hairlineBorder: Boolean = false,
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val hairlineModifier = if (hairlineBorder) {
        Modifier.border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), shape)
    } else {
        Modifier
    }

    when (thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .then(hairlineModifier)
        ) {
            placeHolder()
        }
        1 -> AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnails[0])
                .apply { /* Removed cache key extensions due to unresolved in env */ }
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
            placeholder = painterResource(R.drawable.queue_music),
            error = painterResource(R.drawable.queue_music),
            modifier = Modifier
                .size(size)
                .clip(shape)
                .then(hairlineModifier)
        )
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .then(hairlineModifier)
        ) {
            listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd
            ).fastForEachIndexed { index, alignment ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnails.getOrNull(index))
                        .apply { /* Removed cache key extensions due to unresolved in env */ }
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                    placeholder = painterResource(R.drawable.queue_music),
                    error = painterResource(R.drawable.queue_music),
                    modifier = Modifier
                        .align(alignment)
                        .size(size / 2)
                )
            }
        }
    }
}

@Composable
fun BoxScope.OverlayPlayButton(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.Center)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BoxScope.OverlayEditButton(
    visible: Boolean,
    onClick: () -> Unit,
    alignment: Alignment = Alignment.Center,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(alignment)
            .then(if (alignment == Alignment.BottomEnd) Modifier.padding(8.dp) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                .padding(0.dp)
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// Same horizontally-snapping carousel mechanism as Home's "Picked for you" (LazyHorizontalGrid
// + snap fling, `rows` tall pages), factored out so every song carousel in the app (Picked for
// you, Artist "Top Songs", Artist library songs) stays visually identical. Row spacing/shape is
// untouched here — callers render their own row composable (SongListItem/YouTubeListItem) inside
// [itemContent], so the existing look of those rows carries over as-is.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun <T> SongCarousel(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    rows: Int = 4,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridState: LazyGridState = rememberLazyGridState(),
    itemContent: @Composable (item: T, itemWidth: Dp) -> Unit,
) {
    // Reserving the full `rows` height regardless of item count left dead space below short lists
    // (e.g. an artist with only 1-2 featured tracks still got a 4-row-tall carousel). Shrink to fit
    // the actual content instead — callers with enough items to fill every row see no change.
    val effectiveRows = minOf(rows, items.size).coerceAtLeast(1)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val itemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * itemWidthFactor
        val snapLayoutInfoProvider = remember(gridState) {
            SnapLayoutInfoProvider(
                lazyGridState = gridState,
                positionInLayout = { layoutSize, itemSize ->
                    layoutSize * itemWidthFactor / 2f - itemSize / 2f
                },
            )
        }
        LazyHorizontalGrid(
            state = gridState,
            rows = GridCells.Fixed(effectiveRows),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
            contentPadding = contentPadding,
            overscrollEffect = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * effectiveRows)
                .rubberBandOverscroll(Orientation.Horizontal, gridState),
        ) {
            gridItems(items = items, key = { key(it) }) { item ->
                itemContent(item, itemWidth)
            }
        }
    }
}

@Composable
fun SwipeToSongBox(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    content: @Composable BoxScope.() -> Unit
) {
    val ctx = LocalContext.current
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val offset = remember { mutableFloatStateOf(0f) }
    val threshold = 300f
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    // One-shot white pulse drawn on confirmed swipe only (threshold reached at release).
    // Never fires on a cancelled drag (offset springs back below threshold), per design ask.
    val confirmFlash = remember { Animatable(0f) }

    val dragState = rememberDraggableState { delta ->
        offset.floatValue = (offset.floatValue + delta).coerceIn(-threshold, threshold)
    }

    // Every offset read below is deferred to layout/draw (Modifier.offset{} lambda,
    // graphicsLayer{} lambda, drawBehind{}) instead of read directly in this function's body.
    // Reading offset.floatValue directly here — as this used to do, in an `if` gating the
    // reveal panel's presence and in a `then(if (...))` on content's own Modifier chain — makes
    // this whole composable (and, since it isn't a separate skip scope, the heavy `content()`
    // it hosts: thumbnail, badges, download-state flows) recompose on every single drag pixel.
    // That recomposition storm is what read as a jittery/stuttering row while dragging; sizes
    // and colors below never change mid-drag, only visibility (alpha) and position do, so they
    // can be computed once and animated purely at draw time.
    val nextBg = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.secondary
    val nextTint = if (topNavigationBarEnabled) Color.Black else MaterialTheme.colorScheme.onSecondary
    val queueBg = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.primary
    val queueTint = if (topNavigationBarEnabled) Color.Black else MaterialTheme.colorScheme.onPrimary
    val labelStyle = MaterialTheme.typography.labelLarge
    val labelFontFamily = if (topNavigationBarEnabled) SpaceMonoFontFamily else FontFamily.Default
    val labelLetterSpacing = if (topNavigationBarEnabled) 0.5.sp else 0.sp
    val nextLabel = stringResource(R.string.swipe_label_next)
    val queueLabel = stringResource(R.string.swipe_label_queue)
    val contentBg = if (topNavigationBarEnabled) Color.Black else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStarted = {
                    // Guarantees a fresh gesture never inherits a still-fading flash from a prior
                    // confirmed swipe on this row — a cancelled drag can never show any flash.
                    confirmFlash.snapTo(0f)
                },
                onDragStopped = {
                    when {
                        offset.floatValue >= threshold -> {
                            player?.playNext(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.play_next, Toast.LENGTH_SHORT).show()
                            confirmSwipe(scope, confirmFlash, player?.isPlaying?.value == true)
                            reset(offset, scope)
                        }

                        offset.floatValue <= -threshold -> {
                            player?.addToQueue(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.add_to_queue, Toast.LENGTH_SHORT).show()
                            confirmSwipe(scope, confirmFlash, player?.isPlaying?.value == true)
                            reset(offset, scope)
                        }

                        else -> reset(offset, scope)
                    }
                }
            )
    ) {
        // Fixed-size panels — span the full row at all times instead of growing with drag
        // distance, and both always stay in composition (only their alpha animates) so
        // dragging never adds/removes a subtree. The sliding content Box (drawn after these,
        // so it sits on top in z-order) is what actually reveals/covers them.
        //
        // The row itself has a transparent background in New Iride UI, so this panel alone
        // would show through behind the cover/title/artist too and clash with their white
        // text. The content Box below carries its own black backing sized to its own bounds
        // (only while swiping), so NEXT/QUEUE keeps a plain white backdrop while the song
        // info stays readable on black.
        //
        // Visibility is exposed as derivedStateOf booleans instead of reading offset.floatValue
        // directly here — the boolean only flips twice per gesture (at each sign change), so
        // animateFloatAsState below recomposes on that rare flip, never per drag pixel. That
        // animated float replaces the old instant graphicsLayer{alpha=0/1} snap with a short
        // dissolve, and in New Iride UI drives a tech-HUD frame + typewriter reveal so the flash
        // reads as a designed transition instead of a glitch.
        val nextVisible by remember { derivedStateOf { offset.floatValue > 0f } }
        val queueVisible by remember { derivedStateOf { offset.floatValue < 0f } }

        SwipeRevealPanel(
            modifier = Modifier.matchParentSize(),
            visible = nextVisible,
            alignment = Alignment.CenterStart,
            background = nextBg,
            tint = nextTint,
            label = nextLabel,
            style = labelStyle,
            fontFamily = labelFontFamily,
            letterSpacing = labelLetterSpacing,
            techStyled = topNavigationBarEnabled,
            offset = offset,
            threshold = threshold,
        )
        SwipeRevealPanel(
            modifier = Modifier.matchParentSize(),
            visible = queueVisible,
            alignment = Alignment.CenterEnd,
            background = queueBg,
            tint = queueTint,
            label = queueLabel,
            style = labelStyle,
            fontFamily = labelFontFamily,
            letterSpacing = labelLetterSpacing,
            techStyled = topNavigationBarEnabled,
            offset = offset,
            threshold = threshold,
            reverse = true,
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.floatValue.roundToInt(), 0) }
                .fillMaxWidth()
                .drawBehind {
                    if (contentBg != null && offset.floatValue != 0f) {
                        drawRect(contentBg)
                    }
                },
            content = content
        )

        // Confirm-only white pulse — never touched by a cancelled drag, only by confirmSwipe().
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = confirmFlash.value }
                .background(Color.White)
        )
    }
}

// Fires the confirm-only white pulse and, when nothing is currently playing, a short
// monospace beep via ToneGenerator (mirrors the ArtistGameViewModel tone pattern) so a
// silent app still gives audible confirmation that the swipe committed.
private fun confirmSwipe(
    scope: CoroutineScope,
    flash: Animatable<Float, AnimationVector1D>,
    isPlaying: Boolean,
) {
    scope.launch {
        // Fully opaque — anything less lets the NEXT/QUEUE label (still mid-scramble from
        // reset()'s offset animation below) show through and read as a glitch. Held slightly
        // longer than reset()'s 300ms so the row is fully closed again before the flash clears,
        // instead of uncovering the panel for its last few frames.
        flash.snapTo(1f)
        flash.animateTo(0f, animationSpec = tween(durationMillis = 320))
    }
    if (!isPlaying) {
        scope.launch {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 40)
            delay(60)
            toneGenerator.release()
        }
    }
}

// Helper to animate reset of swipe offset
private fun reset(offset: MutableState<Float>, scope: CoroutineScope) {
    scope.launch {
        animate(
            initialValue = offset.value,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        ) { value, _ -> offset.value = value }
    }
}

// NEXT/QUEUE reveal panel behind a swiped song row. `techStyled` (New Iride UI) adds a thin
// HUD hairline frame and a passcode-style scramble reveal driven by drag distance (not time).
// NEXT (drag right) locks letters left-to-right; QUEUE (drag left) passes `reverse = true` so
// the reveal mirrors — letters lock from the trailing edge inward, growing leftward from the
// panel's right edge in step with the swipe direction. Classic Material UI keeps a plain
// fade with no framing or reveal.
@Composable
private fun SwipeRevealPanel(
    modifier: Modifier,
    visible: Boolean,
    alignment: Alignment,
    background: Color,
    tint: Color,
    label: String,
    style: TextStyle,
    fontFamily: FontFamily,
    letterSpacing: TextUnit,
    techStyled: Boolean,
    offset: State<Float>,
    threshold: Float,
    reverse: Boolean = false,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "swipePanelAlpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .background(background)
            .then(
                if (techStyled) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val lineColor = tint.copy(alpha = 0.35f)
                        drawLine(lineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                        drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = alignment
    ) {
        if (techStyled) {
            PasscodeSwipeLabel(
                label = label,
                visible = visible,
                offset = offset,
                threshold = threshold,
                style = style,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                color = tint,
                modifier = Modifier.padding(horizontal = 24.dp),
                reverse = reverse,
            )
        } else {
            Text(
                text = label,
                style = style,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing,
                color = tint,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

private const val SCRAMBLE_GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val SCRAMBLE_TICK_MS = 45L
private const val REVEAL_BOOST = 1.25f

// Passcode-style reveal: how many letters are locked follows drag progress (offset/threshold),
// not a timer, so it reads as "decoding" the further you swipe. Locked letters show the real
// label; the rest keep re-rolling random glyphs on a fixed tick until they lock too.
//
// [reverse] mirrors the reveal for swipe-left gestures (QUEUE): letters lock from the trailing
// edge of the string inward and the rendered text is right-aligned, so the locked block grows
// leftward from the panel's right edge in step with the drag direction instead of growing
// rightward inside a right-anchored widget (which would shift letters around as they lock).
@Composable
private fun PasscodeSwipeLabel(
    label: String,
    visible: Boolean,
    offset: State<Float>,
    threshold: Float,
    style: TextStyle,
    fontFamily: FontFamily,
    letterSpacing: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    reverse: Boolean = false,
) {
    val reducedMotion = rememberReducedMotion()
    // Boosted so the full word locks in before the drag reaches the actual commit threshold —
    // reading "NEXT"/"QUEUE" complete gives the reveal breathing room before release fires.
    val progress by remember {
        derivedStateOf { (abs(offset.value) / threshold * REVEAL_BOOST).coerceIn(0f, 1f) }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(visible, reducedMotion) {
        if (visible && !reducedMotion) {
            while (true) {
                delay(SCRAMBLE_TICK_MS)
                tick++
            }
        } else {
            tick = 0
        }
    }
    // Reduced motion: no scramble, label reveals instantly at any drag rather than decoding
    // letter-by-letter — the "crossfade or instant" alternative platform guidelines call for.
    val lockedCount = if (reducedMotion) {
        if (progress > 0f) label.length else 0
    } else {
        (progress * label.length).toInt().coerceIn(0, label.length)
    }
    val displayed = remember(label, lockedCount, tick, reverse) {
        if (reverse) {
            // Lock from the trailing edge of the string inward (E → U → U → Q for "QUEUE"),
            // with scrambled glyphs on the leading side. Combined with TextAlign.End below
            // and the caller's right-anchored panel, this makes the locked block grow
            // leftward from the row's right edge as the user drags left.
            buildString {
                val unlocked = label.length - lockedCount
                for (i in 0 until unlocked) {
                    append(SCRAMBLE_GLYPHS[Random(tick * 31 + i).nextInt(SCRAMBLE_GLYPHS.length)])
                }
                append(label.takeLast(lockedCount))
            }
        } else {
            buildString {
                append(label.take(lockedCount))
                for (i in lockedCount until label.length) {
                    append(SCRAMBLE_GLYPHS[Random(tick * 31 + i).nextInt(SCRAMBLE_GLYPHS.length)])
                }
            }
        }
    }
    Text(
        text = displayed,
        textAlign = if (reverse) TextAlign.End else TextAlign.Start,
        style = style,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Black,
        letterSpacing = letterSpacing,
        color = color,
        modifier = modifier,
    )
}

// Fixed rather than theme-derived: needs ~4:1+ contrast against both the app's near-black dark
// surfaces and light-theme white, independent of the seed/dynamic accent color.
val NotificationDotGreen = Color(0xFF2E7D32)

object Icon {
    @Composable
    fun Starred() {
        Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Download(state: Int?) {
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
        when (state) {
            STATE_COMPLETED -> if (topNavigationBarEnabled) {
                // New Iride UI: flat monochrome badge, no colored pill — matches Starred()/Explicit() above.
                Icon(
                    painter = painterResource(R.drawable.arrow_downward),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_downward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
            STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.6f) else ProgressIndicatorDefaults.circularColor,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 2.dp)
            )
            else -> { /* no icon */ }
        }
    }

    @Composable
    fun New(label: String? = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = if (label != null) 3.dp else 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NotificationDotGreen),
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }

    @Composable
    fun Explicit() {
        val showExplicitBadge by rememberPreference(ShowExplicitBadgeKey, defaultValue = false)
        if (showExplicitBadge) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
        }
    }
}

// New Iride UI: flat monochrome spinner instead of the Material Expressive blob shape,
// matching the tinting already used for HomeScreen's mood-mix loaders.
@Composable
fun IrideLoadingIndicator(modifier: Modifier = Modifier) {
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    if (topNavigationBarEnabled) {
        CircularProgressIndicator(
            modifier = modifier.size(28.dp),
            strokeWidth = 2.dp,
            color = Color.White.copy(alpha = 0.6f),
        )
    } else {
        ContainedLoadingIndicator(modifier = modifier)
    }
}
