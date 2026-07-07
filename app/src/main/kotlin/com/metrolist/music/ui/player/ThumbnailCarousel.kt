/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import sv.lib.squircleshape.SquircleShape
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.SeekExtraSeconds
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.ui.component.CastButton
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private val CarouselPeekPadding = 34.dp
private val CarouselPageSpacing = 20.dp

@Composable
fun ThumbnailCarousel(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT
    )
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        else -> Color.White
    }

    val mediaItemsData = remember(mediaMetadata, swipeThumbnail) {
        val player = playerConnection.player
        val timeline = player.currentTimeline
        val currentIndex = player.currentMediaItemIndex
        val shuffled = player.shuffleModeEnabled
        val current = try { player.currentMediaItem } catch (e: Exception) { null }
        val prev = if (swipeThumbnail && !timeline.isEmpty) {
            val pi = timeline.getPreviousWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffled)
            if (pi != C.INDEX_UNSET) try { player.getMediaItemAt(pi) } catch (e: Exception) { null } else null
        } else null
        val next = if (swipeThumbnail && !timeline.isEmpty) {
            val ni = timeline.getNextWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffled)
            if (ni != C.INDEX_UNSET) try { player.getMediaItemAt(ni) } catch (e: Exception) { null } else null
        } else null
        val items = listOfNotNull(prev, current, next)
        MediaItemsData(items, items.indexOf(current))
    }

    // Lags behind mediaItemsData: holds the old list during skip animation so the
    // correct artwork slides in rather than the page snapping instantly with a reload.
    var displayedMediaItemsData by remember { mutableStateOf(mediaItemsData) }
    val displayedItems = displayedMediaItemsData.items
    val displayedCurrentIndex = displayedMediaItemsData.currentIndex

    val prevItem = displayedItems.getOrNull(displayedCurrentIndex - 1)
    val nextItem = displayedItems.getOrNull(displayedCurrentIndex + 1)
    LaunchedEffect(prevItem?.mediaId) {
        val url = prevItem?.mediaMetadata?.artworkUri?.toString()?.resize(1080, 1080) ?: return@LaunchedEffect
        context.imageLoader.enqueue(
            ImageRequest.Builder(context).data(url)
                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
        )
    }
    LaunchedEffect(nextItem?.mediaId) {
        val url = nextItem?.mediaMetadata?.artworkUri?.toString()?.resize(1080, 1080) ?: return@LaunchedEffect
        context.imageLoader.enqueue(
            ImageRequest.Builder(context).data(url)
                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
        )
    }

    val pagerState = rememberPagerState(
        initialPage = displayedCurrentIndex.coerceAtLeast(0),
        pageCount = { displayedItems.size }
    )

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && displayedCurrentIndex >= 0) {
            val page = pagerState.currentPage
            if (page > displayedCurrentIndex && canSkipNext) {
                playerConnection.seekToNext()
            } else if (page < displayedCurrentIndex && canSkipPrevious) {
                playerConnection.seekToPreviousAlways()
            }
        }
    }

    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(mediaMetadata?.id) {
        val newData = mediaItemsData
        val oldData = displayedMediaItemsData

        if (isFirstComposition) {
            isFirstComposition = false
            displayedMediaItemsData = newData
            pagerState.scrollToPage(newData.currentIndex.coerceAtLeast(0))
            return@LaunchedEffect
        }

        val newCurrentId = newData.items.getOrNull(newData.currentIndex)?.mediaId
        val oldCurrentId = oldData.items.getOrNull(oldData.currentIndex)?.mediaId

        if (newCurrentId == oldCurrentId) {
            displayedMediaItemsData = newData
            return@LaunchedEffect
        }

        // Animate on OLD list in the correct direction.
        // For swipe: pager already at target page, animateScrollToPage is a no-op.
        val oldItems = oldData.items
        val oldIdx = oldData.currentIndex
        when (newCurrentId) {
            oldItems.getOrNull(oldIdx + 1)?.mediaId -> if (oldIdx + 1 < displayedItems.size) {
                pagerState.animateScrollToPage(oldIdx + 1)
            }
            oldItems.getOrNull(oldIdx - 1)?.mediaId -> if (oldIdx - 1 >= 0) {
                pagerState.animateScrollToPage(oldIdx - 1)
            }
        }

        displayedMediaItemsData = newData
        pagerState.scrollToPage(newData.currentIndex.coerceAtLeast(0))
    }

    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                if (!isLandscape) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }

                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    val playerExpanded = isPlayerExpanded()
                    val isScrollEnabled by remember(swipeThumbnail, playerExpanded) {
                        derivedStateOf { swipeThumbnail && playerExpanded }
                    }

                    val pageWidth = maxWidth - CarouselPeekPadding * 2
                    val thumbCornerRadius = pageWidth * 0.03f

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = CarouselPeekPadding),
                        pageSpacing = CarouselPageSpacing,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = isScrollEnabled,
                        key = { it },
                        modifier = if (isLandscape) {
                            val landSize = minOf(maxWidth, maxHeight) - PlayerHorizontalPadding * 2
                            Modifier.size(landSize + PlayerHorizontalPadding * 2)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) { page ->
                        val item = displayedItems.getOrNull(page) ?: return@HorizontalPager

                        var skipMultiplier by remember { mutableIntStateOf(1) }
                        var lastTapTime by remember { mutableLongStateOf(0L) }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .pointerInput(isListenTogetherGuest) {
                                    if (isListenTogetherGuest) return@pointerInput
                                    detectTapGestures(
                                        onDoubleTap = { offset ->
                                            val currentPos = playerConnection.player.currentPosition
                                            val duration = playerConnection.player.duration
                                            val now = System.currentTimeMillis()
                                            if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                                                skipMultiplier++
                                            } else {
                                                skipMultiplier = 1
                                            }
                                            lastTapTime = now
                                            val skipAmount = 5000 * skipMultiplier
                                            if (offset.x < size.width / 2) {
                                                playerConnection.player.seekTo(
                                                    (currentPos - skipAmount).coerceAtLeast(0)
                                                )
                                                seekDirection = context.getString(
                                                    R.string.seek_backward_dynamic, skipAmount / 1000
                                                )
                                            } else {
                                                playerConnection.player.seekTo(
                                                    (currentPos + skipAmount).coerceAtMost(duration)
                                                )
                                                seekDirection = context.getString(
                                                    R.string.seek_forward_dynamic, skipAmount / 1000
                                                )
                                            }
                                            showSeekEffect = true
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        val pageOffset =
                                            page.toFloat() - pagerState.currentPage - pagerState.currentPageOffsetFraction
                                        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                                        val scale = lerp(0.85f, 1.0f, 1f - absOffset)
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = lerp(0.4f, 1.0f, 1f - absOffset)
                                        val shrinkPx = size.width * (1f - scale) / 2f
                                        translationX = if (absOffset > 0.001f) {
                                            -(pageOffset / absOffset) * shrinkPx
                                        } else 0f
                                    }
                                    .clip(
                                        SquircleShape(
                                            radius = thumbCornerRadius,
                                            cornerSmoothing = 0.48f
                                        )
                                    )
                            ) {
                                if (hidePlayerThumbnail) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.small_icon),
                                            contentDescription = stringResource(R.string.hide_player_thumbnail),
                                            tint = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(120.dp)
                                        )
                                    }
                                } else {
                                    val artworkUri = item.mediaMetadata.artworkUri?.toString()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(artworkUri?.resize(1080, 1080) ?: artworkUri)
                                                .size(Size.ORIGINAL)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .networkCachePolicy(CachePolicy.ENABLED)
                                                .crossfade(300)
                                                .placeholderMemoryCacheKey(artworkUri?.resize(120, 120)?.let { MemoryCache.Key(it) })
                                                .build(),
                                            contentDescription = null,
                                            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                CastButton(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    tintColor = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CarouselSeekEffectOverlay(seekDirection = seekDirection)
        }

        // Minimal, transient error banner - never hides the artwork or swaps the screen.
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        ) {
            error?.let { PlaybackErrorBanner(error = it, retry = playerConnection.player::prepare) }
        }
    }
}

@Composable
private fun CarouselSeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}
