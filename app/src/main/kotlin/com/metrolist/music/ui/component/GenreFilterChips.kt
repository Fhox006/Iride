/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.GenreProvider
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class GenreSongInfo(
    val id: String,
    val title: String,
    val artist: String?,
)

data class GenreFilterState(
    val genreBySongId: Map<String, List<String>>,
    val selectedGenre: String?,
    val isLoading: Boolean,
    val onSelect: (String) -> Unit,
) {
    private val genreCounts: Map<String, Int>
        get() = genreBySongId.values.flatten().groupingBy { it }.eachCount()

    // Genres with only 1 matching song aren't useful as a filter (they'd
    // narrow the list to a single item), so they're dropped. The rest are
    // ordered most → fewest songs, so the left edge is always the "biggest" pill.
    val sortedGenres: List<String>
        get() =
            genreCounts
                .filterValues { it >= 2 }
                .entries
                .sortedByDescending { it.value }
                .map { it.key }

    fun matches(songId: String): Boolean =
        selectedGenre == null || genreBySongId[songId]?.contains(selectedGenre) == true
}

// Pills only re-sort/reflow once per interval. Committing on every single
// fetch result made the row constantly jump around; ticking at a fixed pace
// keeps it calm while genres are still being resolved in the background.
// Kept short so pills actually show up as soon as they're ready instead of
// sitting resolved-but-hidden for seconds.
private const val PILL_SNAPSHOT_INTERVAL_MS = 400L

// When genres still need fetching, wait a beat before showing the shimmer
// placeholder — most navigations settle fast enough that skipping straight to
// pills (or nothing) looks calmer than a placeholder that flashes for a few
// frames. Only applies when nothing is cached yet; if all songs already have
// genres, pills render on the very next frame with no delay at all.
private const val INITIAL_PILL_RENDER_DELAY_MS = 120L

/**
 * Fetches genre/style tags for [songs] from [GenreProvider] (no genre data
 * exists anywhere else in Iride) and exposes selection/filter state for
 * [GenrePillsRow]. Resets the selected genre whenever the song list changes.
 */
@Composable
fun rememberGenreFilter(songs: List<GenreSongInfo>): GenreFilterState {
    var genres by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var stableGenres by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val ids = remember(songs) { songs.map { it.id } }

    LaunchedEffect(ids) {
        selectedGenre = null
        val missing = songs.filter { it.id !in genres }
        if (missing.isEmpty()) {
            stableGenres = genres
            isLoading = false
            return@LaunchedEffect
        }

        delay(INITIAL_PILL_RENDER_DELAY_MS)
        isLoading = true
        val semaphore = Semaphore(4)
        val fetchJob =
            launch {
                missing
                    .map { info ->
                        async {
                            semaphore.withPermit {
                                val songGenres = GenreProvider.getGenres(info.id, info.title, info.artist)
                                if (songGenres.isNotEmpty()) {
                                    genres = genres + (info.id to songGenres)
                                }
                            }
                        }
                    }.awaitAll()
            }

        while (fetchJob.isActive) {
            delay(PILL_SNAPSHOT_INTERVAL_MS)
            stableGenres = genres
        }
        stableGenres = genres
        isLoading = false
    }

    return GenreFilterState(
        genreBySongId = stableGenres,
        selectedGenre = selectedGenre,
        isLoading = isLoading,
        onSelect = { genre -> selectedGenre = if (selectedGenre == genre) null else genre },
    )
}

@Composable
fun GenrePillsRow(
    state: GenreFilterState,
    modifier: Modifier = Modifier,
) {
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    val genres = state.sortedGenres
    val showPlaceholder = genres.size <= 1 && state.isLoading

    AnimatedVisibility(
        visible = genres.size > 1 || showPlaceholder,
        // Anchor to Start so the row grows/shrinks from the right edge only —
        // the first pill's position never shifts when the row appears/resizes.
        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        modifier = modifier,
    ) {
        if (genres.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(if (topNavigationBarEnabled) 20.dp else 8.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp),
            ) {
                items(genres, key = { it }) { genre ->
                    val selected = state.selectedGenre == genre

                    if (topNavigationBarEnabled) {
                        UnderlinePill(
                            text = genre,
                            selected = selected,
                            onClick = { state.onSelect(genre) },
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                        val containerColor by animateColorAsState(
                            targetValue =
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                },
                            animationSpec = spring(stiffness = 400f),
                            label = "genrePillColor",
                        )
                        val contentColor by animateColorAsState(
                            targetValue =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            animationSpec = spring(stiffness = 400f),
                            label = "genrePillContentColor",
                        )
                        val horizontalPadding by animateDpAsState(
                            targetValue = if (selected) 16.dp else 14.dp,
                            animationSpec = spring(stiffness = 400f),
                            label = "genrePillPadding",
                        )

                        Surface(
                            onClick = { state.onSelect(genre) },
                            shape = RoundedCornerShape(percent = 50),
                            color = containerColor,
                            contentColor = contentColor,
                            modifier = Modifier.animateItem(),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        } else {
            GenrePillsPlaceholder()
        }
    }
}

/**
 * Monospace pill with an underline that grows/shrinks with the text width instead of a filled
 * background — the "sottolineato" pill look used across New Iride UI filter rows (genre pills
 * here, queue-mode pills in IrideMp3Player). Kept as a standalone composable so both call sites
 * share one visual instead of drifting apart.
 *
 * Selection changes are spring-animated (text color/weight glow and underline fade+grow) so this
 * matches the smooth feel of the other New Iride UI selector, [IrideSegmentedToggle] — previously
 * this snapped instantly with no animation at all.
 */
@Composable
fun UnderlinePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var textWidthPx by remember(text) { mutableStateOf(0) }
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = 400f),
        label = "underlinePillTextColor",
    )
    val underlineAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "underlinePillUnderlineAlpha",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = SpaceMonoFontFamily,
                letterSpacing = 0.5.sp,
            ),
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.onSizeChanged { textWidthPx = it.width },
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(with(density) { textWidthPx.toDp() })
                .background(Color.White.copy(alpha = underlineAlpha)),
        )
    }
}

/** Skeleton pills shown while genres are still being resolved, so the row doesn't look broken. */
@Composable
private fun GenrePillsPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "genrePillsShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "genrePillsShimmerAlpha",
    )

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(listOf(56.dp, 84.dp, 68.dp)) { width ->
            Box(
                modifier =
                    Modifier
                        .height(32.dp)
                        .width(width)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.7f)),
            )
        }
    }
}
