package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.discovery.NewReleaseKind
import com.metrolist.music.discovery.NewReleaseSection
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioReleaseShelf
import com.metrolist.music.ui.component.IrideFolioReleaseSpotlight
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.component.SharedHomeSectionTitle
import com.metrolist.music.ui.component.SharedYouTubePlaylistRow
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.screens.playlist.NewReleaseChartsShelf
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.viewmodels.NewReleasesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sv.lib.squircleshape.SquircleShape

@Composable
fun NewReleasesScreen(navController: NavController, viewModel: NewReleasesViewModel = hiltViewModel()) {
    val textColor = MaterialTheme.colorScheme.onSurface
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        onSurfaceVariant = textColor.copy(alpha = .7f),
        secondary = textColor.copy(alpha = .7f),
    )) {
        CompositionLocalProvider(LocalContentColor provides textColor, LocalLibraryTextColor provides textColor.copy(alpha = .7f)) {
            NewReleasesContent(navController, viewModel)
        }
    }
}

@Composable
private fun NewReleasesContent(navController: NavController, viewModel: NewReleasesViewModel) {
    val page by viewModel.page.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val resolvingId by viewModel.resolvingId.collectAsStateWithLifecycle()
    val resolvedSongIds by viewModel.resolvedSongIds.collectAsStateWithLifecycle()
    val player = LocalPlayerConnection.current
    val listenTogether = LocalListenTogetherManager.current
    val isGuest = listenTogether?.isInRoom == true && !listenTogether.isHost
    val menuState = LocalMenuState.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listState = rememberLazyListState()
    val backStackEntry = LocalViewModelStoreOwner.current as? NavBackStackEntry
    val reducedMotion = rememberReducedMotion()
    val scrollToTop = backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var openJob by remember { mutableStateOf<Job?>(null) }
    val unavailable = stringResource(R.string.new_releases_unavailable)
    val connectionError = stringResource(R.string.new_releases_connection_error)
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    val resolveRelease: (NewRelease, Boolean) -> Unit = { release, showMenu ->
        val previousJob = openJob
        previousJob?.cancel()
        openJob = scope.launch {
            previousJob?.join()
            try {
                when (val item = viewModel.openRelease(release)) {
                    is AlbumItem -> if (showMenu) {
                        menuState.show(anchored = true) {
                            YouTubeAlbumMenu(item, navController, menuState::dismiss)
                        }
                    } else navController.navigate("album/${item.id}")
                    is SongItem -> if (showMenu) {
                        menuState.show(anchored = true) {
                            YouTubeSongMenu(item, navController, menuState::dismiss)
                        }
                    } else if (!isGuest) {
                        if (player != null) {
                            if (player.mediaMetadata.value?.id == item.id) {
                                player.togglePlayPause()
                            } else {
                                player.playQueue(YouTubeQueue(
                                    endpoint = WatchEndpoint(videoId = item.id),
                                    preloadItem = item.toMediaMetadata(),
                                ))
                            }
                        } else snackbar.showSnackbar(connectionError)
                    }
                    else -> snackbar.showSnackbar(unavailable)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                snackbar.showSnackbar(connectionError)
            }
        }
    }
    val openRelease: (NewRelease) -> Unit = { resolveRelease(it, false) }
    val openMenu: (NewRelease) -> Unit = { resolveRelease(it, true) }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (reducedMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
            backStackEntry.savedStateHandle.set("scrollToTop", false)
        }
    }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.onPageOpened()
            while (true) {
                delay(com.metrolist.music.discovery.NewReleaseSchedule.nextCheckDelayMillis())
                viewModel.onPageOpened()
            }
        }
    }

    IridePageLayout(
        title = stringResource(R.string.new_releases_title),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        bottomPadding = bottomPadding + 24.dp,
        overlay = {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding))
        },
    ) {
        if (page.hero.isNotEmpty()) {
            item(key = "release_spotlight") {
                IrideFolioReleaseSpotlight(
                    releases = page.hero,
                    resolvingId = resolvingId,
                    artworkEpoch = page.checkedAt / (12 * 60 * 60_000L),
                    artwork = viewModel::heroArtwork,
                    details = { releaseDetails(it) },
                    onClick = openRelease,
                )
            }
        } else if (loading) {
            item(key = "release_hero_placeholder") {
                Box(Modifier.fillMaxWidth().padding(IrideFolio.HorizontalPadding)
                    .height(IrideFolio.ReleaseSpotlightHeight)
                    .clip(SquircleShape(IrideFolio.ReleaseSpotlightCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainer))
            }
        }
        releaseShelf("followed_albums", R.string.new_releases_followed_albums,
            page.followedAlbums, resolvingId, openRelease, openMenu, resolvedSongIds)
        releaseShelf("followed_songs", R.string.new_releases_followed_singles,
            page.followedSingles, resolvingId, openRelease, openMenu, resolvedSongIds)
        releaseShelf("release_albums", R.string.new_releases_for_you,
            page.albums, resolvingId, openRelease, openMenu, resolvedSongIds)
        if (page.newAlbums.isNotEmpty()) {
            item(key = "release_youtube_albums") {
                ReleaseHeading(stringResource(R.string.new_releases_youtube_albums))
                ReleaseDiscoveryShelf(page.newAlbums, navController)
            }
        }
        if (page.playlists.isNotEmpty()) {
            item(key = "release_playlists") {
                ReleaseHeading(stringResource(R.string.new_releases_playlists))
                ReleaseDiscoveryShelf(page.playlists, navController)
            }
        }
        releaseShelf("release_songs", R.string.new_releases_making_waves,
            page.songs, resolvingId, openRelease, openMenu, resolvedSongIds)
        if (page.artists.isNotEmpty()) {
            item(key = "release_artists") {
                ReleaseHeading(stringResource(R.string.new_releases_artists))
                ReleaseDiscoveryShelf(page.artists, navController)
            }
        }
        item(key = "release_charts") { NewReleaseChartsShelf(navController) }
        page.sections.take(8).filter { it.releases.isNotEmpty() }.forEach { section ->
            item(key = "release_section_${section.id}") {
                ReleaseHeading(releaseSectionTitle(section))
                IrideFolioReleaseShelf(section.releases, resolvingId, openRelease, openMenu, resolvedSongIds)
            }
        }
    }
}

@Composable
private fun ReleaseDiscoveryShelf(items: List<YTItem>, navController: NavController) {
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    SharedYouTubePlaylistRow(items, key = { it.id }, contentPadding = IrideFolio.HorizontalPadding - 4.dp) { item ->
        YouTubeGridItem(
            item = item,
            size = when (item) { is ArtistItem -> 120.dp; is AlbumItem -> 156.dp; else -> 172.dp },
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        is AlbumItem -> navController.navigate("album/${item.id}")
                        else -> Unit
                    }
                },
                onLongClick = {
                    menuState.show(anchored = true) {
                        when (item) {
                            is ArtistItem -> YouTubeArtistMenu(item, menuState::dismiss)
                            is PlaylistItem -> YouTubePlaylistMenu(item, coroutineScope = scope, onDismiss = menuState::dismiss)
                            is AlbumItem -> YouTubeAlbumMenu(item, navController, menuState::dismiss)
                            else -> Unit
                        }
                    }
                },
            ),
        )
    }
}

private fun LazyListScope.releaseShelf(
    key: String,
    title: Int,
    releases: List<NewRelease>,
    resolvingId: String?,
    onClick: (NewRelease) -> Unit,
    onMenuClick: (NewRelease) -> Unit,
    resolvedSongIds: Map<String, String>,
) {
    if (releases.isEmpty()) return
    item(key = key) {
        ReleaseHeading(stringResource(title))
        IrideFolioReleaseShelf(releases, resolvingId, onClick, onMenuClick, resolvedSongIds)
    }
}

@Composable
internal fun ReleaseHeading(title: String, onClick: (() -> Unit)? = null) {
    SharedHomeSectionTitle(title, onClick = onClick, showNavigationArrow = onClick != null, titleMaxLines = 2)
}

@Composable
private fun releaseSectionTitle(section: NewReleaseSection): String = when (section.id) {
    "local" -> stringResource(R.string.new_releases_country, section.subject)
    "world" -> stringResource(R.string.new_releases_world)
    "eps" -> stringResource(R.string.new_releases_latest_eps)
    "discovery" -> stringResource(R.string.new_releases_next_discovery)
    else -> {
        val genreResource = when (section.id.substringAfter('_')) {
            "rap" -> R.string.new_releases_genre_rap
            "soul" -> R.string.new_releases_genre_soul
            "electronic" -> R.string.new_releases_genre_electronic
            "rock" -> R.string.new_releases_genre_rock
            "ambient" -> R.string.new_releases_genre_ambient
            "classical" -> R.string.new_releases_genre_classical
            "folk" -> R.string.new_releases_genre_folk
            "pop" -> R.string.new_releases_genre_pop
            "jazz" -> R.string.new_releases_genre_jazz
            "latin" -> R.string.new_releases_genre_latin
            else -> 0
        }
        val genre = if (genreResource == 0) section.subject else stringResource(genreResource)
        stringResource(if (section.id.startsWith("songs_")) R.string.new_releases_genre_tracks
            else R.string.new_releases_genre_albums, genre)
    }
}

@Composable
private fun releaseDetails(release: NewRelease): String {
    val kind = stringResource(when (release.kind) {
        NewReleaseKind.ALBUM -> R.string.new_releases_album
        NewReleaseKind.EP -> R.string.new_releases_ep
        NewReleaseKind.SINGLE -> R.string.new_releases_single
    })
    val date = remember(release.releaseDate) {
        runCatching { LocalDate.parse(release.releaseDate).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) }
            .getOrDefault(release.releaseDate)
    }
    return stringResource(R.string.new_releases_details, kind, date)
}
