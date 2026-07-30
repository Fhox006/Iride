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
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
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
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ShowExplicitBadgeKey
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
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.TitleFeaturingParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableCollectionItemScope

const val ActiveBoxAlpha = 0.6f

val LocalItemHorizontalPadding = compositionLocalOf { true }

/**
 * Fraction of the album thumbnail's width that the vinyl disc behind it (see [VinylPeekDisc])
 * is allowed to peek out on the right. Callers that add extra spacing between grid items to
 * host the effect (e.g. the "dischi per te" row) must reserve at least this much width, or the
 * peeking disc will overlap the next card's cover.
 */
const val VinylPeekFraction = 0.28f

/**
 * A vinyl record, printed with the album's own cover art, sitting behind an album square and
 * peeking out on its right edge — used to visually tell albums apart from playlists in the
 * Iride New UI "dischi per te" row. Draw this *before* the square album [ItemThumbnail] in the
 * same Box so the square covers the hidden portion of the disc.
 */
private val VinylPeekDiscBaseTop = Color(0xFF141416)
private val VinylPeekDiscBaseBottom = Color(0xFF060607)

@Composable
private fun VinylPeekDisc(
    thumbnailUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl?.resize(120, 120))
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
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(VinylPeekDiscBaseTop, VinylPeekDiscBaseBottom)))
                // Faint rim so the disc's silhouette stays visible even against a black cover.
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = CircleShape)
        ) {
            // Concentric grooves for a realistic record texture.
            for (ring in 1..4) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(1f - ring * 0.16f)
                        .border(width = 0.6.dp, color = Color.White.copy(alpha = 0.06f), shape = CircleShape)
                )
            }
            // Center label — the only place album artwork appears on the disc.
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.24f)
                    .clip(CircleShape)
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.18f), shape = CircleShape)
            )
            // Spindle hole.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.045f)
                    .clip(CircleShape)
                    .background(VinylPeekDiscBaseBottom)
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
                isActive && isSelected == true -> {
                    Modifier
                        .padding(horizontal = hPad, vertical = 2.dp)
                        .clip(highlightShape)
                        .background(activeBackgroundColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))
                        .height(ListItemHeight)
                }
                isActive -> {
                    Modifier
                        .padding(horizontal = hPad, vertical = 2.dp)
                        .clip(highlightShape)
                        .background(activeBackgroundColor ?: Color.White.copy(alpha = 0.10f))
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
    fillMaxWidth = fillMaxWidth
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
    badges: @Composable RowScope.() -> Unit = {
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
                    shape = SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f),
                    modifier = Modifier.size(ListThumbnailSize),
                    showLikedStar = showLikedIcon && song.song.liked,
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
                .data(artist.artist.thumbnailUrl?.resize(900, 900))
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ListThumbnailSize)
                .clip(CircleShape),
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
) = GridItem(
    title = artist.artist.name,
    subtitle = if (artist.songCount > 0) pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount) else "",
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artist.artist.thumbnailUrl?.resize(900, 900))
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    },
    fillMaxWidth = fillMaxWidth,
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
    val avatarSize = 72.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(avatarSize + 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(avatarSize + 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
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
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (newSongCount > 9) "9+" else newSongCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
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
            textAlign = TextAlign.Center,
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
            modifier = Modifier.size(ListThumbnailSize)
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
    showPlayButton: Boolean = true,
    showVinylEffect: Boolean = false,
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
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem
        val scope = rememberCoroutineScope()
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

        if (showVinylEffect) {
            VinylPeekDisc(
                thumbnailUrl = album.album.thumbnailUrl,
                size = maxWidth,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * VinylPeekFraction)
            )
        }

        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (topNavigationBarEnabled) RoundedCornerShape(5.dp) else SquircleShape(radius = squircleRadius, cornerSmoothing = 0.5f),
        )

        if (showPlayButton) {
            AlbumPlayButton(
                visible = !isActive,
                onClick = {
                    scope.launch {
                        val albumWithSongs = withContext(Dispatchers.IO) {
                            database.albumWithSongs(album.id).firstOrNull()
                        }
                        albumWithSongs?.let {
                            playerConnection.playQueue(LocalAlbumRadio(it))
                        }
                    }
                }
            )
        }
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
                shape = SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f)
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
                shape = shape
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
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

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
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = true)

    val content: @Composable () -> Unit = {
        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
        ListItem(
            title = item.title,
            subtitle = when (item) {
                is SongItem -> {
                    val durationSec = item.duration
                    if (durationSec != null && shouldHideDuration(durationSec, hideDurationForStandard)) {
                        item.artists.joinToString { it.name }
                    } else {
                        joinByBullet(item.artists.joinToString { it.name }, makeTimeString(durationSec?.times(1000L)))
                    }
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
                    modifier = Modifier.size(ListThumbnailSize)
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
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

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
    showPlayButton: Boolean = true,
    showVinylEffect: Boolean = false,
    size: Dp = currentGridThumbnailHeight(),
    showTitle: Boolean = true,
    // Used when item.artists is null/empty (always true for albums parsed off
    // an artist's own page) so the subtitle isn't left artist-less.
    fallbackArtistName: String? = null,
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
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem
        val scope = rememberCoroutineScope()
        val squircleRadius = maxWidth * 0.06f
        val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

        if (showVinylEffect && item is AlbumItem) {
            VinylPeekDisc(
                thumbnailUrl = item.thumbnail,
                size = maxWidth,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * VinylPeekFraction)
            )
        }

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
        )

        if (item is SongItem && !isActive) {
            OverlayPlayButton(
                visible = true
            )
        }

        if (showPlayButton) {
            AlbumPlayButton(
                visible = item is AlbumItem && !isActive,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        var albumWithSongs = database.albumWithSongs(item.id).first()
                        if (albumWithSongs?.songs.isNullOrEmpty()) {
                            YouTube.album(item.id).onSuccess { albumPage ->
                                database.transaction { insert(albumPage) }
                                albumWithSongs = database.albumWithSongs(item.id).first()
                            }.onFailure { reportException(it) }
                        }
                        albumWithSongs?.let {
                            withContext(Dispatchers.Main) {
                                playerConnection.playQueue(LocalAlbumRadio(it))
                            }
                        }
                    }
                }
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
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(thumbnailRatio)
            .clip(shape)
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
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
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
                    .clip(shape)
            )
        }

        if (albumIndex != null) {
            AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                Text(
                    text = albumIndex.toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
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

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (albumIndex != null)
                        Color.Transparent
                    else
                        Color.Black.copy(alpha = ActiveBoxAlpha),
                    shape = shape
                )
        )
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
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(thumbnailRatio)
            .clip(shape)
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
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f), shape)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }        }

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
    cacheKey: String? = null
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    
    when (thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
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
        )
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
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

@Composable
fun BoxScope.AlbumPlayButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
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
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White
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
            rows = GridCells.Fixed(rows),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
            contentPadding = contentPadding,
            overscrollEffect = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * rows)
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
        flash.snapTo(0.55f)
        flash.animateTo(0f, animationSpec = tween(durationMillis = 220))
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
// HUD hairline frame and a passcode-style scramble reveal driven by drag distance (not time):
// letters lock in left-to-right as `offset` approaches `threshold`, unrevealed letters cycle
// through random glyphs. Classic Material UI keeps a plain fade with no framing or reveal.
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
                modifier = Modifier.padding(horizontal = 24.dp)
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
    val displayed = remember(label, lockedCount, tick) {
        buildString {
            append(label.take(lockedCount))
            for (i in lockedCount until label.length) {
                append(SCRAMBLE_GLYPHS[Random(tick * 31 + i).nextInt(SCRAMBLE_GLYPHS.length)])
            }
        }
    }
    Text(
        text = displayed,
        style = style,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Black,
        letterSpacing = letterSpacing,
        color = color,
        modifier = modifier,
    )
}

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
