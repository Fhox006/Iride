/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HeroCarouselEnabledKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.HideDurationForStandardSongsKey
import com.metrolist.music.constants.ShowFeaturedArtistsInTopSongsKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.SmartBootKey
import com.metrolist.music.constants.SwipeToRemoveSongKey
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IrideSwitch
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettings(
    navController: NavController,
    activity: Activity
) {
    val scrollState = rememberScrollState()
    val frostBackdrop = rememberFrostBackdrop()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT)
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) { playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null) }.collectAsStateWithLifecycle()

    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(SwipeToSongKey, defaultValue = true)
    val (swipeToRemoveSong, onSwipeToRemoveSongChange) =
        rememberPreference(SwipeToRemoveSongKey, defaultValue = true)
    val (hideDurationForStandard, onHideDurationForStandardChange) =
        rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
    val (showFeaturedArtistsInTopSongs, onShowFeaturedArtistsInTopSongsChange) =
        rememberPreference(ShowFeaturedArtistsInTopSongsKey, defaultValue = true)
    val (heroCarouselEnabled, onHeroCarouselEnabledChange) =
        rememberPreference(HeroCarouselEnabledKey, defaultValue = false)
    val (smartBootEnabled, onSmartBootEnabledChange) =
        rememberPreference(SmartBootKey, defaultValue = true)
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) =
        rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

    var showDefaultOpenTabDialog by remember { mutableStateOf(false) }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = { onDefaultOpenTabChange(it); showDefaultOpenTabDialog = false },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordFrostBackdrop(frostBackdrop)
        ) {
            if (mainTopGradient) {
                TopScreenGradientBackground(
                    mediaMetadata = mediaMetadata,
                    playerBackground = playerBackgroundStyle,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            }
            Column(
                Modifier
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(
                    Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                    )
                )

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_navigation),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.default_open_tab)) },
                            description = {
                                Text(
                                    when (defaultOpenTab) {
                                        NavigationTab.HOME -> stringResource(R.string.home)
                                        NavigationTab.SEARCH -> stringResource(R.string.search)
                                        NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                    }
                                )
                            },
                            onClick = { showDefaultOpenTabDialog = true }
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Material3SettingsGroup(
                    title = stringResource(R.string.home),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.explore_outlined),
                            title = { Text(stringResource(R.string.hero_carousel_title)) },
                            description = { Text(stringResource(R.string.hero_carousel_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = heroCarouselEnabled,
                                    onCheckedChange = onHeroCarouselEnabledChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                if (heroCarouselEnabled) R.drawable.check else R.drawable.close
                                            ),
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            contentDescription = null,
                                        )
                                    },
                                )
                            },
                            onClick = { onHeroCarouselEnabledChange(!heroCarouselEnabled) },
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Material3SettingsGroup(
                    title = stringResource(R.string.smart_boot),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.smart_boot)) },
                            description = { Text(stringResource(R.string.smart_boot_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = smartBootEnabled,
                                    onCheckedChange = onSmartBootEnabledChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                if (smartBootEnabled) R.drawable.check else R.drawable.close
                                            ),
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            contentDescription = null,
                                        )
                                    },
                                )
                            },
                            onClick = { onSmartBootEnabledChange(!smartBootEnabled) },
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_behavior),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.swipe_song_to_add_title)) },
                            description = { Text(stringResource(R.string.swipe_song_to_add)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = swipeToSong, onCheckedChange = onSwipeToSongChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (swipeToSong) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onSwipeToSongChange(!swipeToSong) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.swipe_song_to_remove_title)) },
                            description = { Text(stringResource(R.string.swipe_song_to_remove)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = swipeToRemoveSong, onCheckedChange = onSwipeToRemoveSongChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (swipeToRemoveSong) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.timer),
                            title = { Text(stringResource(R.string.hide_duration_standard_songs)) },
                            description = { Text(stringResource(R.string.hide_duration_standard_songs_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = hideDurationForStandard, onCheckedChange = onHideDurationForStandardChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (hideDurationForStandard) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onHideDurationForStandardChange(!hideDurationForStandard) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.group),
                            title = { Text(stringResource(R.string.show_featured_artists_in_top_songs)) },
                            description = { Text(stringResource(R.string.show_featured_artists_in_top_songs_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = showFeaturedArtistsInTopSongs, onCheckedChange = onShowFeaturedArtistsInTopSongsChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (showFeaturedArtistsInTopSongs) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onShowFeaturedArtistsInTopSongsChange(!showFeaturedArtistsInTopSongs) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = enableHighRefreshRate, onCheckedChange = onEnableHighRefreshRateChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (enableHighRefreshRate) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) }
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SettingsBackTopBar(
            title = stringResource(R.string.interface_settings),
            navController = navController,
            backdrop = frostBackdrop,
            revealProgress = rememberDiscreteProgress(active = scrollState.value > 0),
        )
    }
}
