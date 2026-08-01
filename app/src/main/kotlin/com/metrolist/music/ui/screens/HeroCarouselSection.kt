/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.stableKey
import com.metrolist.music.ui.component.IrideCollapsibleSection
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import sv.lib.squircleshape.SquircleShape
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Dark charcoal used for the text-legibility scrim and badge pill, in place of pure
// black — same token as IrideMp3Player's surface / AnimatedAlbumGradientBackground's
// DarkGrayBlack, so the carousel reads as part of the same dark surface family instead
// of a flat black box. Verified >=8:1 contrast for white text even over a white cover.
private val HeroScrimColor = Color(0xFF1C1C1E)

@Composable
fun HeroCarouselSection(
    items: List<HeroCarouselItem>,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onArtistRadioClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    newIrideUi: Boolean = false,
    collapsed: Boolean = false,
    onCollapseToggle: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(modifier = modifier.fillMaxWidth()) {
        NavigationTitle(
            title = "Featured for you",
            useIrideStyle = newIrideUi,
            collapsed = collapsed,
            onCollapseToggle = onCollapseToggle,
        )

        IrideCollapsibleSection(collapsed = collapsed) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                key = { page -> items[page].stableKey() },
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                HeroCard(
                    item = items[page],
                    newIrideUi = newIrideUi,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onArtistRadioClick = onArtistRadioClick,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                items.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    newIrideUi && selected -> Color.White.copy(alpha = 0.8f)
                                    newIrideUi -> Color.White.copy(alpha = 0.22f)
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                                }
                            ),
                    )
                }
            }
        }
        }
    }
}

// Reserves Hero Carousel's exact final footprint (title + card + dot row) before its data has
// loaded, so it never pops in above sections that were ready sooner (e.g. Picked for you, which
// loads from the DB and is typically ready before the network-backed hero items are) and pushes
// them down. Height must stay pixel-identical to the real content below, or swapping skeleton for
// content still reflows everything under it.
@Composable
fun HeroCarouselSkeleton(newIrideUi: Boolean, modifier: Modifier = Modifier) {
    val cardHeight = if (newIrideUi) 148.dp else 190.dp
    val cardShape = if (newIrideUi) SquircleShape(radius = 12.dp, cornerSmoothing = 0.48f) else RoundedCornerShape(20.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        NavigationTitle(title = "Featured for you", useIrideStyle = newIrideUi)
        ShimmerHost(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(cardHeight)
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
        // Matches the real dot-indicator row's footprint (10dp top padding + 8dp dot size).
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun HeroCard(
    item: HeroCarouselItem,
    newIrideUi: Boolean,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onArtistRadioClick: (String, String) -> Unit,
) {
    val badgeLabel: String
    val badgeIcon: Int
    val title: String
    val subtitle: String
    val coverUrl: String?
    val onClick: () -> Unit

    when (item) {
        is HeroCarouselItem.NewRelease -> {
            badgeLabel = "NEW"
            badgeIcon = R.drawable.trending_up
            title = item.title
            subtitle = item.artistName
            coverUrl = item.coverUrl
            onClick = { onAlbumClick(item.albumId) }
        }
        is HeroCarouselItem.InRotation -> {
            badgeLabel = "IN ROTATION"
            badgeIcon = R.drawable.album
            title = item.title
            subtitle = item.artistName
            coverUrl = item.coverUrl
            onClick = { onAlbumClick(item.albumId) }
        }
        is HeroCarouselItem.RecommendedAlbum -> {
            badgeLabel = "FOR YOU"
            badgeIcon = R.drawable.star
            title = item.title
            subtitle = item.artistName
            coverUrl = item.coverUrl
            onClick = { onAlbumClick(item.albumId) }
        }
        is HeroCarouselItem.TrendingArtist -> {
            badgeLabel = "TRENDING"
            badgeIcon = R.drawable.trending_up
            title = item.artistName
            subtitle = "Rising on your feed"
            coverUrl = item.coverUrl
            onClick = { onArtistClick(item.artistId) }
        }
        is HeroCarouselItem.ArtistRadio -> {
            badgeLabel = "RADIO"
            badgeIcon = R.drawable.radio
            title = "${item.artistName} Radio"
            subtitle = "Endless mix inspired by this artist"
            coverUrl = item.coverUrl
            onClick = { onArtistRadioClick(item.artistId, item.artistName) }
        }
        is HeroCarouselItem.GenreNewRelease -> {
            badgeLabel = item.genreLabel
            badgeIcon = R.drawable.palette
            title = item.title
            subtitle = item.artistName
            coverUrl = item.coverUrl
            onClick = { onAlbumClick(item.albumId) }
        }
    }

    val cardHeight = if (newIrideUi) 148.dp else 190.dp
    // Radius/smoothing matched to the rest of New Iride UI's big content cards (AlbumScreen,
    // Player, OnlinePlaylistScreen all use 12dp/0.45-0.48) — the old 20dp/0.55 read noticeably
    // rounder than every other box on the screen.
    val cardShape = if (newIrideUi) SquircleShape(radius = 12.dp, cornerSmoothing = 0.48f) else RoundedCornerShape(20.dp)

    val cardModifier = Modifier
        .fillMaxWidth()
        .height(cardHeight)
        .clip(cardShape)
        .let {
            if (newIrideUi) it.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), cardShape) else it
        }
        .clickable(onClick = onClick)

    // New Iride UI has no Card/elevation anywhere else — a flat, bordered Box replaces the
    // shadowed Material Card so this shelf stops standing out from its neighbors.
    if (newIrideUi) {
        Box(modifier = cardModifier) {
            HeroCardContent(coverUrl, badgeIcon, badgeLabel, title, subtitle, newIrideUi = true)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = cardShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            HeroCardContent(coverUrl, badgeIcon, badgeLabel, title, subtitle, newIrideUi = false)
        }
    }
}

@Composable
private fun HeroCardContent(
    coverUrl: String?,
    badgeIcon: Int,
    badgeLabel: String,
    title: String,
    subtitle: String,
    newIrideUi: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        )
                    ),
            )
        }

        // Iride: darken only the lower half where the title sits, capped at a lighter alpha,
        // so the cover art stays visible instead of the old full-height 0.78 black wash.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (newIrideUi) {
                        // Same peak darkness as classic (0.78) so white title/subtitle text keeps
                        // its contrast ratio on bright covers — only the darkened *area* shrinks,
                        // confined to the lower half instead of washing the whole card.
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to HeroScrimColor.copy(alpha = 0.78f),
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, HeroScrimColor.copy(alpha = 0.78f)),
                        )
                    }
                ),
        )

        if (newIrideUi) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    // Flat charcoal backing so the label stays legible over light/white covers —
                    // the border-only pill let bright art wash out the white text underneath.
                    // 0.85 alpha: the minimum that keeps the white label at >=4.5:1 contrast
                    // even against a pure-white cover (worst case).
                    .background(HeroScrimColor.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                    .border(
                        BorderStroke(0.8.dp, Color.White.copy(alpha = 0.55f)),
                        RoundedCornerShape(3.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = badgeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 9.sp,
                        letterSpacing = 0.10.em,
                    ),
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(
                        painter = painterResource(badgeIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = if (newIrideUi) {
                    MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 15.sp,
                        letterSpacing = (-0.1).sp,
                    )
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = if (newIrideUi) {
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            letterSpacing = 0.02.em,
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
