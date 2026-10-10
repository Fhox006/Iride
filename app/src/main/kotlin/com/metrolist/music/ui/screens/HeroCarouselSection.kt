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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.R
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.stableKey
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioShelfHeader
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.theme.textPrimary
import kotlinx.coroutines.delay
import sv.lib.squircleshape.SquircleShape

private val HeroScrimColor = Color(0xFF1C1C1E)

@Composable
fun HeroCarouselSection(
    items: List<HeroCarouselItem.ArtistRadio>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    onCoverError: (String) -> Unit = {},
) {
    if (items.isEmpty()) return
    val internalPagerState = rememberPagerState(pageCount = { items.size })
    val activePagerState = pagerState ?: internalPagerState

    Column(modifier = modifier.fillMaxWidth()) {
        RadioSectionTitle()
        HorizontalPager(
            state = activePagerState,
            key = { page -> items.getOrNull(page)?.stableKey() ?: "hero_page_$page" },
            contentPadding = PaddingValues(horizontal = IrideFolio.HorizontalPadding),
            pageSpacing = IrideFolio.ShelfItemSpacing,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            items.getOrNull(page)?.let { item ->
                HeroCard(item, onArtistClick, onCoverError)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.indices.forEach { index ->
                val selected = activePagerState.currentPage == index
                Box(
                    modifier = Modifier.padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.textPrimary
                            else MaterialTheme.colorScheme.strokeCard
                        ),
                )
            }
        }
    }
}

@Composable
private fun RadioSectionTitle() {
    IrideFolioShelfHeader(
        title = stringResource(R.string.folio_artists_for_you),
        showNavigationArrow = false,
    )
}

@Composable
fun HeroCarouselSkeleton(modifier: Modifier = Modifier) {
    val cardShape = SquircleShape(radius = 12.dp, cornerSmoothing = 0.48f)
    Column(modifier = modifier.fillMaxWidth()) {
        RadioSectionTitle()
        ShimmerHost(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(148.dp)
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun HeroCard(
    item: HeroCarouselItem.ArtistRadio,
    onArtistClick: (String) -> Unit,
    onCoverError: (String) -> Unit,
) {
    val title = item.artistName
    val subtitle = stringResource(R.string.folio_explore_artist)
    val cardShape = SquircleShape(radius = 12.dp, cornerSmoothing = 0.48f)

    Box(
        modifier = Modifier.fillMaxWidth()
            .height(148.dp)
            .clip(cardShape)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), cardShape)
            .clickable(role = Role.Button, onClickLabel = title) { onArtistClick(item.artistId) },
    ) {
        RadioCover(item, Modifier.fillMaxSize(), onCoverError)
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to HeroScrimColor.copy(alpha = 0.78f),
                    ),
                ),
        )
        Box(
            modifier = Modifier.padding(12.dp)
                .align(Alignment.TopStart),
        ) {
            Box(
                modifier = Modifier.size(30.dp)
                    .clip(CircleShape)
                    .background(HeroScrimColor.copy(alpha = 0.85f), CircleShape)
                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.55f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.artist),
                    contentDescription = stringResource(R.string.view_artist),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = IrideFolio.ShelfItemFontSize,
                    lineHeight = IrideFolio.ShelfItemFontSize * 1.4f,
                    letterSpacing = (-0.1).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = IrideFolio.ShelfDetailFontSize,
                        lineHeight = IrideFolio.ShelfDetailFontSize * 1.4f,
                        letterSpacing = 0.02.em,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RadioCover(
    item: HeroCarouselItem.ArtistRadio,
    modifier: Modifier,
    onCoverError: (String) -> Unit,
) {
    val cover = item.coverUrl?.takeIf { it.isNotBlank() }
    var attempt by remember(item.artistId, cover) { mutableIntStateOf(0) }
    var failed by remember(item.artistId, cover) { mutableStateOf(false) }
    val context = LocalContext.current
    val request = remember(context, cover, attempt) {
        ImageRequest.Builder(context)
            .data(cover)
            .crossfade(true)
            .diskCachePolicy(if (attempt == 0) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY)
            .build()
    }
    LaunchedEffect(failed, attempt) {
        if (failed && attempt == 0) {
            delay(1_000)
            failed = false
            attempt = 1
        } else if (failed) {
            onCoverError(item.artistId)
        }
    }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ),
        ),
    ) {
        if (cover != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
