package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.gestures.Orientation
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
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.R
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.discovery.NewReleaseKind
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary
import sv.lib.squircleshape.SquircleShape
import kotlinx.coroutines.flow.MutableStateFlow

/** Only songs use Home's four-row quick picks; albums keep individual cover cards. */
@Composable
fun IrideFolioReleaseShelf(
    releases: List<NewRelease>,
    resolvingId: String?,
    onClick: (NewRelease) -> Unit,
    onMenuClick: (NewRelease) -> Unit,
    resolvedSongIds: Map<String, String> = emptyMap(),
    minimumRows: Int = 1,
) {
    if (releases.isNotEmpty() && releases.all { !it.isTrack }) {
        IrideFolioReleaseAlbumShelf(releases, resolvingId, onClick, onMenuClick)
        return
    }
    val player = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val mediaMetadata by remember(player) {
        player?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsStateWithLifecycle()
    val isPlaying by remember(player) {
        player?.isEffectivelyPlaying ?: MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    SharedQuickPicksCarousel(
        items = releases,
        key = { it.id },
        loading = false,
        contentPadding = IrideFolio.HorizontalPadding - 4.dp,
        usePager = true,
        minimumRows = minimumRows,
    ) { release, itemWidth ->
        val songId = resolvedSongIds[release.id]
        val librarySong by remember(database, songId) {
            songId?.let(database::song) ?: MutableStateFlow<Song?>(null)
        }.collectAsStateWithLifecycle(initialValue = null)
        val isActive = songId?.let { it == mediaMetadata?.id } == true
        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
            ListItem(
                title = release.title,
                subtitle = release.artistName,
                subtitleColor = MaterialTheme.colorScheme.textSecondary,
                badges = {
                    if (librarySong?.song?.liked == true) com.metrolist.music.ui.component.Icon.Starred()
                    if (release.explicit) com.metrolist.music.ui.component.Icon.Explicit()
                    if (songId != null) {
                        val download by LocalDownloadUtil.current.getDownload(songId)
                            .collectAsStateWithLifecycle(initialValue = null)
                        com.metrolist.music.ui.component.Icon.Download(download?.state)
                    }
                },
                showDivider = false,
                isActive = isActive,
                thumbnailContent = {
                    Box {
                        ItemThumbnail(
                            thumbnailUrl = release.artworkUrl,
                            isActive = isActive,
                            isPlaying = isPlaying,
                            shape = SquircleShape(ThumbnailCornerRadius, cornerSmoothing = .5f),
                            modifier = Modifier.size(ListThumbnailSize),
                            softBorder = true,
                            showLikedStar = librarySong?.song?.liked == true,
                        )
                        if (resolvingId == release.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                },
                trailingContent = {
                    SongMenuIconButton(onClick = { onMenuClick(release) }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = stringResource(R.string.more_options),
                        )
                    }
                },
                modifier = Modifier.width(itemWidth).irideFolioCombinedClickable(
                    onClick = { onClick(release) },
                    onLongClick = {
                        onMenuClick(release)
                    },
                ),
            )
        }
    }
}

@Composable
private fun IrideFolioReleaseAlbumShelf(
    releases: List<NewRelease>,
    resolvingId: String?,
    onClick: (NewRelease) -> Unit,
    onMenuClick: (NewRelease) -> Unit,
) {
    SharedYouTubePlaylistRow(releases, key = { it.id }, contentPadding = IrideFolio.HorizontalPadding) { release ->
        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
            Column(Modifier.width(156.dp)) {
                GridItem(
                    title = release.title,
                    subtitle = release.artistName,
                    badges = { if (release.explicit) com.metrolist.music.ui.component.Icon.Explicit() },
                    size = 156.dp,
                    thumbnailContent = {
                        ItemThumbnail(
                            thumbnailUrl = release.artworkUrl,
                            isActive = false,
                            isPlaying = false,
                            shape = FolioShape(7.dp),
                            hairlineBorder = true,
                        )
                        if (resolvingId == release.id) {
                            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .35f)),
                                contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                        }
                    },
                    modifier = Modifier.irideFolioCombinedClickable(
                        onClick = { onClick(release) },
                        onLongClick = {
                            onMenuClick(release)
                        },
                    ),
                )
                Text(
                    stringResource(if (release.kind == NewReleaseKind.EP) R.string.new_releases_ep else R.string.new_releases_album),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IBMPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun IrideFolioReleaseSpotlight(
    releases: List<NewRelease>,
    resolvingId: String?,
    artworkEpoch: Long,
    artwork: suspend (NewRelease) -> String?,
    details: @Composable (NewRelease) -> String,
    onClick: (NewRelease) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { releases.size })
    val pagerPull = rememberRubberBandPull()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val coverSize = IrideFolio.ReleaseSpotlightCoverSize * fontScale
    Column {
        SharedHomeSectionTitle(stringResource(R.string.new_releases_spotlight))
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = IrideFolio.HorizontalPadding),
            pageSpacing = 12.dp,
            key = { releases[it].id },
            overscrollEffect = null,
            modifier =
                Modifier.fillMaxWidth()
                    .rubberBandOverscroll(Orientation.Horizontal, pager, pagerPull),
        ) { index ->
            val release = releases[index]
            val photo by produceState<String?>(null, release.id, release.artistName, artworkEpoch) {
                value = artwork(release)
            }
            var photoFailed by remember(photo) { mutableStateOf(false) }
            val context = LocalContext.current
            val imageUrl = if (photoFailed) release.artworkUrl else photo ?: release.artworkUrl
            val request = remember(context, imageUrl) {
                ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(maxOf(IrideFolio.ReleaseSpotlightHeight, 108.dp * fontScale + IrideFolio.ReleaseSpotlightPadding * 2))
                    .clip(SquircleShape(IrideFolio.ReleaseSpotlightCornerRadius, cornerSmoothing = .48f))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onClick(release) },
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { photoFailed = true },
                    modifier = Modifier.matchParentSize(),
                )
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .12f),
                    .35f to Color.Black.copy(alpha = .16f),
                    1f to Color.Black.copy(alpha = .94f),
                )))
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .padding(IrideFolio.ReleaseSpotlightPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(coverSize).clip(FolioShape(6.dp))) {
                        AsyncImage(
                            model = release.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (resolvingId == release.id) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .4f)),
                                contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            stringResource(R.string.new_releases_just_released),
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = IrideFolio.ReleaseSpotlightMetadataSize,
                            lineHeight = IrideFolio.ReleaseSpotlightMetadataSize * 1.25f,
                            color = Color.White.copy(alpha = .75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            release.title,
                            fontFamily = IBMPlexMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = IrideFolio.ReleaseSpotlightTitleSize,
                            lineHeight = IrideFolio.ReleaseSpotlightTitleLineHeight,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            release.artistName,
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = IrideFolio.ReleaseSpotlightArtistSize,
                            lineHeight = IrideFolio.ReleaseSpotlightArtistSize * 1.25f,
                            color = Color.White.copy(alpha = .85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            details(release),
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = IrideFolio.ReleaseSpotlightMetadataSize,
                            lineHeight = IrideFolio.ReleaseSpotlightMetadataSize * 1.25f,
                            color = Color.White.copy(alpha = .7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        CarouselPageIndicator(
            releases.size,
            pager.currentPage,
            overscrollOffset = pagerPull.offset,
        )
    }
}
