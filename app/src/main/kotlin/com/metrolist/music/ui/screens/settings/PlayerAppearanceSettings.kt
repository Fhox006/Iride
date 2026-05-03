/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SquigglySliderKey
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.UseNewMiniPlayerDesignKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.SquigglySlider
import com.metrolist.music.ui.component.WavySlider
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAppearanceSettings(navController: NavController) {
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, defaultValue = false)
    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(UseNewPlayerDesignKey, defaultValue = true)
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.ANIMATED_GRADIENT)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(CropAlbumArtKey, defaultValue = true)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.DEFAULT)
    val (squigglySlider, onSquigglySliderChange) =
        rememberPreference(SquigglySliderKey, defaultValue = false)
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(SwipeThumbnailKey, defaultValue = true)
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(SwipeSensitivityKey, defaultValue = 0.73f)
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) =
        rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(MiniPlayerBackgroundStyleKey, defaultValue = MiniPlayerBackgroundStyle.DEFAULT)
    val (pureBlackMiniPlayer, onPureBlackMiniPlayerChange) =
        rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)

    val availableBackgroundStyles = PlayerBackgroundStyle.values().filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    val availableMiniPlayerBackgroundStyles = MiniPlayerBackgroundStyle.values().filter {
        it != MiniPlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerButtonsStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = { onPlayerBackgroundChange(it); showPlayerBackgroundDialog = false },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.ANIMATED_GRADIENT -> stringResource(R.string.animated_gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    else -> ""
                }
            }
        )
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = { onPlayerButtonsStyleChange(it); showPlayerButtonsStyleDialog = false },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                    else -> ""
                }
            }
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = { onMiniPlayerBackgroundChange(it); showMiniPlayerBackgroundDialog = false },
            title = stringResource(R.string.mini_player_background_style),
            current = miniPlayerBackground,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                when (it) {
                    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                    else -> ""
                }
            }
        )
    }

    if (showSensitivityDialog) {
        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
        DefaultDialog(
            onDismiss = { tempSensitivity = swipeSensitivity; showSensitivityDialog = false },
            buttons = {
                TextButton(onClick = { tempSensitivity = 0.73f }) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tempSensitivity = swipeSensitivity
                    showSensitivityDialog = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    onSwipeSensitivityChange(tempSensitivity)
                    showSensitivityDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.swipe_sensitivity),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(
                        R.string.sensitivity_percentage,
                        (tempSensitivity * 100).roundToInt()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = tempSensitivity,
                    onValueChange = { tempSensitivity = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showSliderOptionDialog) {
        val sliderPreviewColors = PlayerSliderColors.getSliderColors(
            MaterialTheme.colorScheme.primary,
            PlayerBackgroundStyle.DEFAULT,
            isSystemInDarkTheme()
        )
        DefaultDialog(
            buttons = {
                TextButton(onClick = { showSliderOptionDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showSliderOptionDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Default slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f).weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.DEFAULT && !squigglySlider)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.DEFAULT)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }.padding(12.dp)
                    ) {
                        Slider(
                            value = 0.35f, valueRange = 0f..1f,
                            onValueChange = {}, colors = sliderPreviewColors, enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.default_),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Wavy slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f).weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.WAVY && !squigglySlider)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.WAVY)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }.padding(12.dp)
                    ) {
                        WavySlider(
                            value = 0.5f, valueRange = 0f..1f,
                            onValueChange = {}, colors = sliderPreviewColors,
                            modifier = Modifier.weight(1f), isPlaying = true, enabled = false
                        )
                        Text(
                            text = stringResource(R.string.wavy),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Slim slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f).weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.SLIM)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.SLIM)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }.padding(12.dp)
                    ) {
                        Slider(
                            value = 0.65f, valueRange = 0f..1f,
                            onValueChange = {},
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { PlayerSliderTrack(sliderState = it, colors = sliderPreviewColors) },
                            colors = sliderPreviewColors, enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.slim),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Squiggly slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f).weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.WAVY && squigglySlider)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.WAVY)
                                onSquigglySliderChange(true)
                                showSliderOptionDialog = false
                            }.padding(12.dp)
                    ) {
                        SquigglySlider(
                            value = 0.5f, valueRange = 0f..1f,
                            onValueChange = {}, colors = sliderPreviewColors,
                            modifier = Modifier.weight(1f), isPlaying = true, enabled = false
                        )
                        Text(
                            text = stringResource(R.string.squiggly),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // ── Screen ────────────────────────────────────────────────────────────

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // ── Player ────────────────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.player_appearance),
            items = buildList {
                /* HIDDEN - new_player_design toggle
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.new_player_design)) },
                        trailingContent = {
                            Switch(
                                checked = useNewPlayerDesign,
                                onCheckedChange = onUseNewPlayerDesignChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (useNewPlayerDesign) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onUseNewPlayerDesignChange(!useNewPlayerDesign) }
                    )
                )
                END HIDDEN */
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.player_background_style)) },
                        description = {
                            Text(
                                when (playerBackground) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.ANIMATED_GRADIENT -> stringResource(R.string.animated_gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                    else -> ""
                                }
                            )
                        },
                        onClick = { showPlayerBackgroundDialog = true }
                    )
                )
                /* HIDDEN - hide_player_thumbnail toggle
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.hide_image),
                        title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                        description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hidePlayerThumbnail,
                                onCheckedChange = onHidePlayerThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (hidePlayerThumbnail) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) }
                    )
                )
                END HIDDEN */
                if (advancedMode) add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.crop),
                        title = { Text(stringResource(R.string.crop_album_art)) },
                        description = { Text(stringResource(R.string.crop_album_art_desc)) },
                        trailingContent = {
                            Switch(
                                checked = cropAlbumArt,
                                onCheckedChange = onCropAlbumArtChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (cropAlbumArt) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onCropAlbumArtChange(!cropAlbumArt) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.player_buttons_style)) },
                        description = {
                            Text(
                                when (playerButtonsStyle) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                    else -> ""
                                }
                            )
                        },
                        onClick = { showPlayerButtonsStyleDialog = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = {
                            Text(
                                when (sliderStyle) {
                                    SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                    SliderStyle.WAVY -> if (squigglySlider) stringResource(R.string.squiggly) else stringResource(R.string.wavy)
                                    SliderStyle.SLIM -> stringResource(R.string.slim)
                                    else -> ""
                                }
                            )
                        },
                        onClick = { showSliderOptionDialog = true }
                    )
                )
                if (advancedMode) add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                        trailingContent = {
                            Switch(
                                checked = swipeThumbnail,
                                onCheckedChange = onSwipeThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (swipeThumbnail) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onSwipeThumbnailChange(!swipeThumbnail) }
                    )
                )
                /* HIDDEN - swipe_sensitivity setting
                if (swipeThumbnail) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.tune),
                            title = { Text(stringResource(R.string.swipe_sensitivity)) },
                            description = {
                                Text(
                                    stringResource(
                                        R.string.sensitivity_percentage,
                                        (swipeSensitivity * 100).roundToInt()
                                    )
                                )
                            },
                            onClick = { showSensitivityDialog = true }
                        )
                    )
                }
                END HIDDEN */
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Mini Player ────────────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.mini_player),
            items = buildList {
                /* HIDDEN - new_mini_player_design + mini_player_background_style
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.new_mini_player_design)) },
                        trailingContent = {
                            Switch(
                                checked = useNewMiniPlayerDesign,
                                onCheckedChange = onUseNewMiniPlayerDesignChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (useNewMiniPlayerDesign) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onUseNewMiniPlayerDesignChange(!useNewMiniPlayerDesign) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = {
                            Text(
                                text = stringResource(R.string.mini_player_background_style),
                                color = if (!useNewMiniPlayerDesign)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        description = {
                            Text(
                                text = if (!useNewMiniPlayerDesign) {
                                    stringResource(R.string.mini_player_background_not_available)
                                } else {
                                    when (miniPlayerBackground) {
                                        MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                        MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                                        MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                        MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                        MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                                        else -> ""
                                    }
                                },
                                color = if (!useNewMiniPlayerDesign)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { if (useNewMiniPlayerDesign) showMiniPlayerBackgroundDialog = true },
                        enabled = useNewMiniPlayerDesign
                    )
                )
                END HIDDEN */
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.contrast),
                        title = { Text(stringResource(R.string.pure_black_mini_player)) },
                        trailingContent = {
                            Switch(
                                checked = pureBlackMiniPlayer,
                                onCheckedChange = onPureBlackMiniPlayerChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (pureBlackMiniPlayer) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onPureBlackMiniPlayerChange(!pureBlackMiniPlayer) }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        }
    )
}
