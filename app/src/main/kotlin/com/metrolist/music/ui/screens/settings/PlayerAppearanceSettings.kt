/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings
import com.metrolist.music.ui.component.IrideSlider
import com.metrolist.music.ui.component.IrideSwitch

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.BetterGradientSmoothTransitionKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAppearanceSettings(navController: NavController) {
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, defaultValue = false)
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(CropAlbumArtKey, defaultValue = true)
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(SwipeSensitivityKey, defaultValue = 0.73f)
    val (betterGradientSmoothTransition, onBetterGradientSmoothTransitionChange) =
        rememberPreference(BetterGradientSmoothTransitionKey, defaultValue = true)

    val availableBackgroundStyles = PlayerBackgroundStyle.values().filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }


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
                    PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT -> stringResource(R.string.better_animated_gradient)
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
                IrideSlider(
                    value = tempSensitivity,
                    onValueChange = { tempSensitivity = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }


    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal
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

        Material3SettingsGroup(
            title = stringResource(R.string.player_appearance),
            items = buildList {
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
                                    PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT -> stringResource(R.string.better_animated_gradient)
                                }
                            )
                        },
                        onClick = { showPlayerBackgroundDialog = true }
                    )
                )
                if (playerBackground == PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT) add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.better_gradient_smooth_transition)) },
                        description = { Text(stringResource(R.string.better_gradient_smooth_transition_desc)) },
                        trailingContent = {
                            IrideSwitch(
                                checked = betterGradientSmoothTransition,
                                onCheckedChange = onBetterGradientSmoothTransitionChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (betterGradientSmoothTransition) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onBetterGradientSmoothTransitionChange(!betterGradientSmoothTransition) }
                    )
                )
                if (advancedMode) add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.crop),
                        title = { Text(stringResource(R.string.crop_album_art)) },
                        description = { Text(stringResource(R.string.crop_album_art_desc)) },
                        trailingContent = {
                            IrideSwitch(
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
            }
        )

    }

    SettingsBackTopBar(
        title = stringResource(R.string.player_appearance),
        navController = navController,
    )
}
