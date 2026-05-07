/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.ExperimentalLyricsKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.LyricsAnimationStyle
import com.metrolist.music.constants.LyricsAnimationStyleKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsGlowEffectKey
import com.metrolist.music.constants.LyricsLineSpacingKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.LyricsTextSizeKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.ExpandableSettingsSection
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettings(navController: NavController) {
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, defaultValue = false)
    val (experimentalLyrics, onExperimentalLyricsChange) =
        rememberPreference(ExperimentalLyricsKey, defaultValue = true)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) =
        rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.FADE)
    val (lyricsTextSize, onLyricsTextSizeChange) =
        rememberPreference(LyricsTextSizeKey, defaultValue = 28f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) =
        rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(LyricsTextPositionKey, defaultValue = LyricsPosition.LEFT)
    val (respectAgentPositioning, onRespectAgentPositioningChange) =
        rememberPreference(com.metrolist.music.constants.RespectAgentPositioningKey, defaultValue = true)
    val (lyricsClick, onLyricsClickChange) =
        rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(LyricsScrollKey, defaultValue = true)
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(HideStatusBarOnFullscreenKey, defaultValue = false)

    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by remember { mutableStateOf(false) }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showExperimentalLyricsBetaDialog) {
        DefaultDialog(
            onDismiss = { showExperimentalLyricsBetaDialog = false },
            title = { Text(stringResource(R.string.experimental_lyrics_beta_title)) },
            buttons = {
                TextButton(onClick = { showExperimentalLyricsBetaDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    showExperimentalLyricsBetaDialog = false
                    onExperimentalLyricsChange(true)
                }) {
                    Text(stringResource(R.string.enable))
                }
            }
        ) {
            Text(stringResource(R.string.experimental_lyrics_beta_message))
        }
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                }
            }
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        DefaultDialog(
            onDismiss = {
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false
            },
            buttons = {
                TextButton(onClick = { tempTextSize = 28f }) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tempTextSize = lyricsTextSize
                    showLyricsTextSizeDialog = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    onLyricsTextSizeChange(tempTextSize)
                    showLyricsTextSizeDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        DefaultDialog(
            onDismiss = {
                tempSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false
            },
            buttons = {
                TextButton(onClick = { tempSpacing = 1.2f }) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tempSpacing = lyricsLineSpacing
                    showLyricsLineSpacingDialog = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    onLyricsLineSpacingChange(tempSpacing)
                    showLyricsLineSpacingDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.1f", tempSpacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = tempSpacing,
                    onValueChange = { tempSpacing = it },
                    valueRange = 0.8f..2.5f,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
    }

    // ── Screen ────────────────────────────────────────────────────────────

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

        /* HIDDEN - experimental_lyrics group (always ON via default, toggle hidden from user)
        // ── Experimental lyrics ────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.experimental_lyrics),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.experimental_lyrics)) },
                        description = { Text(stringResource(R.string.experimental_lyrics_desc)) },
                        showBadge = true,
                        trailingContent = {
                            Switch(
                                checked = experimentalLyrics,
                                onCheckedChange = {
                                    if (!experimentalLyrics) showExperimentalLyricsBetaDialog = true
                                    else onExperimentalLyricsChange(false)
                                },
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (experimentalLyrics) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = {
                            if (!experimentalLyrics) showExperimentalLyricsBetaDialog = true
                            else onExperimentalLyricsChange(false)
                        }
                    )
                )
                if (!experimentalLyrics) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                            description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsGlowEffect,
                                    onCheckedChange = onLyricsGlowEffectChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                if (lyricsGlowEffect) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
                            description = {
                                Text(
                                    when (lyricsAnimationStyle) {
                                        LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                                        LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                                        LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                                        LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                                        LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                                        LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                                    }
                                )
                            },
                            onClick = { showLyricsAnimationStyleDialog = true }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_text_size)) },
                            description = { Text("${lyricsTextSize.roundToInt()} sp") },
                            onClick = { showLyricsTextSizeDialog = true }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                            description = { Text(String.format(Locale.US, "%.1f", lyricsLineSpacing)) },
                            onClick = { showLyricsLineSpacingDialog = true }
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        END HIDDEN */

        // ── Display ────────────────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_interface),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    description = {
                        Text(
                            when (lyricsPosition) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            }
                        )
                    },
                    onClick = { showLyricsPositionDialog = true }
                ),
                if (advancedMode) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.respect_agent_positioning)) },
                    description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                    trailingContent = {
                        Switch(
                            checked = respectAgentPositioning,
                            onCheckedChange = onRespectAgentPositioningChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        if (respectAgentPositioning) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onRespectAgentPositioningChange(!respectAgentPositioning) }
                ) else null,
                if (advancedMode) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsClick,
                            onCheckedChange = onLyricsClickChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        if (lyricsClick) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsClickChange(!lyricsClick) }
                ) else null,
                if (advancedMode) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsScroll,
                            onCheckedChange = onLyricsScrollChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        if (lyricsScroll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsScrollChange(!lyricsScroll) }
                ) else null,
                if (advancedMode) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.hide_status_bar_fullscreen)) },
                    description = { Text(stringResource(R.string.hide_status_bar_fullscreen_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hideStatusBarOnFullscreen,
                            onCheckedChange = onHideStatusBarOnFullscreenChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        if (hideStatusBarOnFullscreen) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) }
                ) else null
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lyrics)) },
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
