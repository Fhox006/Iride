/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
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

@Composable
fun HeroCarouselSection(
    items: List<HeroCarouselItem>,
    onNewReleaseClick: (String) -> Unit,
    onForYouClick: (String, Boolean) -> Unit,
    onMoodClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onArtistRadioClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Featured for you",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalPager(
            state = pagerState,
            key = { page -> items[page].stableKey() },
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            HeroCard(
                item = items[page],
                onNewReleaseClick = onNewReleaseClick,
                onForYouClick = onForYouClick,
                onMoodClick = onMoodClick,
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
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                        ),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    item: HeroCarouselItem,
    onNewReleaseClick: (String) -> Unit,
    onForYouClick: (String, Boolean) -> Unit,
    onMoodClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onArtistRadioClick: (String, String) -> Unit,
) {
    val badgeLabel: String
    // Null for the Mood badge only — it used to reuse the "favorite" (heart) glyph, which reads
    // as a stray/mismatched icon next to the word "MOOD" (hearts mean "liked", not "mood"). Every
    // other badge keeps its icon.
    val badgeIcon: Int?
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
            onClick = { onNewReleaseClick(item.albumId) }
        }
        is HeroCarouselItem.ForYou -> {
            badgeLabel = "FOR YOU"
            badgeIcon = R.drawable.star
            title = item.title
            subtitle = item.subtitle
            coverUrl = item.coverUrl
            onClick = { onForYouClick(item.playlistId, item.isLocal) }
        }
        is HeroCarouselItem.Mood -> {
            badgeLabel = "MOOD"
            badgeIcon = null
            title = item.moodName
            subtitle = "A playlist matching the mood"
            coverUrl = item.coverUrl
            onClick = { onMoodClick(item.playlistId) }
        }
        is HeroCarouselItem.MoreFromArtist -> {
            badgeLabel = "MORE FROM"
            badgeIcon = R.drawable.person
            title = item.artistName
            subtitle = "Explore the rest of their catalog"
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
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                        )
                    ),
            )

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
                    if (badgeIcon != null) {
                        Icon(
                            painter = painterResource(badgeIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
