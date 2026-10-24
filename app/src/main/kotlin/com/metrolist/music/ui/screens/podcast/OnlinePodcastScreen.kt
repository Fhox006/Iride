package com.metrolist.music.ui.screens.podcast

import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideLoadingIndicator
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlinePodcastViewModel
import timber.log.Timber

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun OnlinePodcastScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePodcastViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val podcast by viewModel.podcast.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val libraryPodcast by viewModel.libraryPodcast.collectAsState()

    val lazyListState = rememberLazyListState()

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = false)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )
    val headerScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                lazyListState.firstVisibleItemScrollOffset > 8
        }
    }
    val frostBackdrop = rememberFrostBackdrop()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredEpisodes =
        remember(episodes, query) {
            if (query.text.isEmpty()) episodes
            else
                episodes.filter { episode ->
                    episode.title.contains(query.text, ignoreCase = true) ||
                        episode.author?.name?.contains(query.text, ignoreCase = true) == true
                }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().recordFrostBackdrop(frostBackdrop)) {
            IridePageLayout(
                background = {
                    if (mainTopGradient) {
                        TopScreenGradientBackground(
                            mediaMetadata = mediaMetadata,
                            playerBackground = playerBackgroundStyle,
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    }
                },
                title = podcast?.title.orEmpty(),
                state = lazyListState,
                showTopBar = true,
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) navController.backToMain()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                    }
                },
                topBarContent =
                    if (isSearching)
                        ({
                            if (isSearching) {
                                TextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.search),
                                            style = LocalTextStyle.current,
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    colors =
                                        TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                        ),
                                    modifier =
                                        Modifier.fillMaxWidth().focusRequester(focusRequester),
                                )
                            }
                        })
                    else null,
            ) {
                if (podcast == null && isLoading) {
                    item(key = "loading_placeholder") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            IrideLoadingIndicator()
                        }
                    }
                } else if (error != null) {
                    item(key = "error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = error ?: stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            IrideFolioPrimaryButton(onClick = { viewModel.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    podcast?.let { podcastItem ->
                        if (!isSearching) {
                            item(key = "podcast_header") {
                                val context = LocalContext.current
                                PodcastHeader(
                                    podcast = podcastItem,
                                    episodeCount = episodes.size,
                                    inLibrary = libraryPodcast?.inLibrary == true,
                                    onLibraryClick = { viewModel.toggleLibrary() },
                                    onViewChannelClick = {
                                        val channelId =
                                            podcastItem.channelId ?: podcastItem.author?.id
                                        if (channelId != null) {
                                            navController.navigate(
                                                "artist/$channelId?isPodcastChannel=true"
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        itemsIndexed(
                            items = filteredEpisodes,
                            key = { _, episode -> episode.id },
                        ) { index, episode ->
                            YouTubeListItem(
                                item = episode,
                                isActive = mediaMetadata?.id == episode.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier.irideFolioCombinedClickable(
                                            onClick = {
                                                if (episode.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    Timber.d(
                                                        "Playing episode: ${episode.title}, index: $index, total episodes: ${filteredEpisodes.size}"
                                                    )
                                                    val mediaItems = filteredEpisodes.map {
                                                        it.toMediaMetadata().toMediaItem()
                                                    }
                                                    Timber.d(
                                                        "Created ${mediaItems.size} media items for queue"
                                                    )
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            sourceRoute = podcast?.id?.let { "online_podcast/$it" },
                                                            title = podcast?.title,
                                                            items = mediaItems,
                                                            startIndex = index,
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                menuState.show(anchored = true) {
                                                    YouTubeSongMenu(
                                                        episode.asSongItem(),
                                                        navController,
                                                        menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                trailingContent = {
                                    SongMenuIconButton(
                                        onClick = {
                                            menuState.show(anchored = true) {
                                                YouTubeSongMenu(
                                                    episode.asSongItem(),
                                                    navController,
                                                    menuState::dismiss,
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastHeader(
    podcast: PodcastItem,
    episodeCount: Int,
    inLibrary: Boolean,
    onLibraryClick: () -> Unit,
    onViewChannelClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(podcast.thumbnail).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.size(200.dp)
                    .irideArtworkOverlayBorder(
                        IrideBaseBorderWidth,
                        MaterialTheme.colorScheme.strokeCard,
                        FolioShape(8.dp),
                    )
                    .clip(FolioShape(8.dp)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        podcast.author?.name?.let { authorName ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = authorName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = podcast.episodeCountText ?: "$episodeCount episodes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IrideFolioSecondaryButton(
                onClick = onLibraryClick,
                modifier = Modifier,
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (inLibrary) R.drawable.library_add_check else R.drawable.library_add
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text =
                        stringResource(
                            if (inLibrary) R.string.iride_remove_star else R.string.iride_star
                        )
                )
            }

            IrideFolioSecondaryButton(
                onClick = onViewChannelClick,
                modifier = Modifier,
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(R.string.view_channel))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
