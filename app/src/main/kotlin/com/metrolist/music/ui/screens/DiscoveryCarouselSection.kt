/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.R
import com.metrolist.music.models.DiscoveryItem
import com.metrolist.music.models.PlaylistType
import kotlinx.coroutines.delay

private val CardWidth = 160.dp
private val CardHeight = 200.dp

@Composable
fun DiscoveryCarouselSection(
    items: List<DiscoveryItem>,
    onPlaylistClick: (String) -> Unit,
    onArtistStationClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "For You",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.stableKey() }) { index, item ->
                DiscoveryCard(
                    item = item,
                    index = index,
                    onClick = {
                        when (item) {
                            is DiscoveryItem.PlaylistCard -> onPlaylistClick(item.id)
                            is DiscoveryItem.ArtistStation -> onArtistStationClick(item.artistId)
                            is DiscoveryItem.AlbumCard -> onAlbumClick(item.albumId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    item: DiscoveryItem,
    index: Int,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * 50L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
    ) {
        Card(
            modifier = Modifier
                .width(CardWidth)
                .height(CardHeight)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val coverUrl: String? = when (item) {
                    is DiscoveryItem.PlaylistCard -> item.coverUrl
                    is DiscoveryItem.ArtistStation -> item.coverUrl
                    is DiscoveryItem.AlbumCard -> item.coverUrl
                }

                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
                    )
                }

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            )
                        ),
                )

                // Type chip
                val chipLabel: String
                val chipBg: Color
                val chipText: Color
                when (item) {
                    is DiscoveryItem.PlaylistCard -> {
                        chipLabel = when (item.type) {
                            PlaylistType.LIKED_SONGS -> "Liked"
                            PlaylistType.PERSONAL -> "Playlist"
                        }
                        chipBg = MaterialTheme.colorScheme.primary
                        chipText = MaterialTheme.colorScheme.onPrimary
                    }
                    is DiscoveryItem.ArtistStation -> {
                        chipLabel = "Station"
                        chipBg = MaterialTheme.colorScheme.secondary
                        chipText = MaterialTheme.colorScheme.onSecondary
                    }
                    is DiscoveryItem.AlbumCard -> {
                        chipLabel = if (item.isPearl) "✶ Pearl" else "Album"
                        chipBg = MaterialTheme.colorScheme.tertiary
                        chipText = MaterialTheme.colorScheme.onTertiary
                    }
                }
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(6.dp),
                    color = chipBg.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = chipText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }

                // Title + subtitle
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    val title: String
                    val subtitle: String
                    when (item) {
                        is DiscoveryItem.PlaylistCard -> {
                            title = item.title
                            subtitle = item.subtitle
                        }
                        is DiscoveryItem.ArtistStation -> {
                            title = item.artistName
                            subtitle = "Artist Station"
                        }
                        is DiscoveryItem.AlbumCard -> {
                            title = item.albumTitle
                            subtitle = item.artistName
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun DiscoveryItem.stableKey(): String = when (this) {
    is DiscoveryItem.PlaylistCard -> "discovery_playlist_$id"
    is DiscoveryItem.ArtistStation -> "discovery_artist_$artistId"
    is DiscoveryItem.AlbumCard -> "discovery_album_$albumId"
}
