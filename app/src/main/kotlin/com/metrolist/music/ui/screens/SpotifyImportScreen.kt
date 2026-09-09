/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) { playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null) }.collectAsStateWithLifecycle()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT)
    val frostBackdrop = rememberFrostBackdrop()

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
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textPrimary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Import Music Library",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.1).sp,
                ),
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "BETA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpaceMonoFontFamily,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.textSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Import your saved tracks, playlists, and artists from external services directly into your Iride library.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.1).sp,
                ),
                color = MaterialTheme.colorScheme.textPrimary
            )
        }
    }

    SettingsBackTopBar(
        title = "Import Music Library",
        navController = navController,
        backdrop = frostBackdrop,
    )
}
