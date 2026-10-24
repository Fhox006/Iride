/** Iride Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumReleaseType
import com.metrolist.music.viewmodels.ArtistDiscographyViewModel
import com.metrolist.music.viewmodels.DiscographyCategory

private fun DiscographyCategory.labelRes() =
    when (this) {
        DiscographyCategory.FROM_ARTIST -> R.string.discography_from_artist
        DiscographyCategory.APPEARS_ON -> R.string.discography_appears_on
    }

private fun AlbumReleaseType.labelRes() =
    when (this) {
        AlbumReleaseType.SINGLE -> R.string.discography_singles
        AlbumReleaseType.EP -> R.string.discography_eps
        AlbumReleaseType.ALBUM -> R.string.albums
    }

private val TYPE_ORDER =
    listOf(AlbumReleaseType.SINGLE, AlbumReleaseType.EP, AlbumReleaseType.ALBUM)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistDiscographyScreen(
    navController: NavController,
    viewModel: ArtistDiscographyViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val artistName by viewModel.artistName.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val buckets by viewModel.buckets.collectAsState()
    val releaseTypes by viewModel.releaseTypes.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val keyboardController = LocalSoftwareKeyboardController.current

    val lazyGridState = rememberLazyGridState()

    var selectedCategory by rememberSaveable { mutableStateOf<DiscographyCategory?>(null) }
    val activeCategory =
        selectedCategory?.takeIf { cat -> buckets.any { it.category == cat } }
            ?: buckets.firstOrNull()?.category

    val activeReleases =
        remember(buckets, activeCategory) {
            buckets.firstOrNull { it.category == activeCategory }?.releases.orEmpty()
        }
    val typeGroups =
        remember(activeReleases, releaseTypes) {
            activeReleases.groupBy { releaseTypes[it.id] ?: AlbumReleaseType.ALBUM }
        }
    var selectedType by rememberSaveable(activeCategory) { mutableStateOf<AlbumReleaseType?>(null) }
    val filteredReleases =
        selectedType?.let { type -> typeGroups[type].orEmpty() } ?: activeReleases

    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IridePageLayout(
            title = artistName.orEmpty(),
            state = lazyGridState,
            showTopBar = true,
            navigationIcon = { IridePageBackButton(navController) },
            bottomPadding =
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            columns =
                GridCells.Adaptive(
                    minSize =
                        GridThumbnailHeight +
                            if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                ),
        ) {
            item(key = "artist_page_title", span = { GridItemSpan(maxLineSpan) }) {
                if (buckets.size > 1) {
                    IrideSegmentedToggle(
                        options =
                            buckets.map { it.category to stringResource(it.category.labelRes()) },
                        selected = activeCategory,
                        onSelect = { selectedCategory = it },
                        spacing = 12.dp,
                    )
                }
            }

            if (typeGroups.size > 1) {
                item(key = "type_filter", span = { GridItemSpan(maxLineSpan) }) {
                    ChipsRow(
                        chips =
                            listOf<Pair<AlbumReleaseType?, String>>(
                                null to stringResource(R.string.filter_all)
                            ) +
                                TYPE_ORDER.filter { typeGroups.containsKey(it) }
                                    .map { it to stringResource(it.labelRes()) },
                        currentValue = selectedType,
                        onValueUpdate = { selectedType = it },
                        useIrideStyle = true,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
            }

            if (loading) {
                items(6) {
                    GridItemPlaceHolder(fillMaxWidth = true)
                }
            } else if (filteredReleases.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.album),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.discography_empty),
                            style = TextStyle(fontFamily = IBMPlexMonoFontFamily, fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    items = filteredReleases,
                    key = { "discography_${it.id}" },
                ) { album ->
                    YouTubeGridItem(
                        item = album,
                        isActive = mediaMetadata?.album?.id == album.id,
                        isPlaying = isPlaying,
                        fillMaxWidth = true,
                        coroutineScope = coroutineScope,
                        modifier =
                            Modifier.irideFolioCombinedClickable(
                                    onClick = { navController.navigate("album/${album.id}") },
                                    onLongClick = {
                                        menuState.show(anchored = true) {
                                            YouTubeAlbumMenu(
                                                albumItem = album,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                    )
                }
            }
        }
    }
}
