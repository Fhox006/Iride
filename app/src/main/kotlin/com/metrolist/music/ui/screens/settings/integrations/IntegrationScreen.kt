/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IntegrationCard
import com.metrolist.music.ui.component.IntegrationCardItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController
) {
    val settingsScrollState = rememberScrollState()
    val frostBackdrop = rememberFrostBackdrop()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsStateWithLifecycle()

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
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .verticalScroll(settingsScrollState)
                    .padding(horizontal = 16.dp),
            ) {
                IntegrationCard(
                    title = stringResource(R.string.general),
                    items = listOf(
                        IntegrationCardItem(
                            icon = painterResource(R.drawable.discord),
                            title = { Text(stringResource(R.string.discord_integration)) },
                            onClick = {
                                navController.navigate("settings/integrations/discord")
                            }
                        ),
                        IntegrationCardItem(
                            icon = painterResource(R.drawable.music_note),
                            title = { Text(stringResource(R.string.lastfm_integration)) },
                            onClick = {
                                navController.navigate("settings/integrations/lastfm")
                            }
                        )
                    )
                )
            }
        }

        SettingsBackTopBar(
            title = stringResource(R.string.integrations),
            navController = navController,
            backdrop = frostBackdrop,
            revealProgress = rememberDiscreteProgress(active = settingsScrollState.value > 0),
        )
    }
}
