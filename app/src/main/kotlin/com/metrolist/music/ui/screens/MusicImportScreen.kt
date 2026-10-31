package com.metrolist.music.ui.screens

import androidx.compose.foundation.shape.CircleShape


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AppleMusicImportConnectedKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.SpotifyImportConnectedKey
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.utils.rememberPreference

@Composable
fun MusicImportScreen(navController: NavController) {
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val spotifyConnected by rememberPreference(SpotifyImportConnectedKey, false)
    val appleConnected by rememberPreference(AppleMusicImportConnectedKey, false)
    val bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
        .asPaddingValues().calculateBottomPadding()
    IridePageFrame(
        title = stringResource(R.string.music_import_title),
        scrollState = rememberScrollState(),
        navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.music_services_login_sync), style = MaterialTheme.typography.titleMedium)
            MusicServiceButton(
                name = stringResource(R.string.music_import_youtube),
                description = stringResource(R.string.music_services_youtube_capabilities),
                connected = "SAPISID" in parseCookieString(cookie),
                onClick = { navController.navigate("login") },
            )
            MusicServiceButton(
                name = stringResource(R.string.music_import_spotify),
                description = stringResource(R.string.music_services_spotify_capabilities),
                connected = spotifyConnected,
                onClick = { navController.navigate("spotify_import") },
            )
            MusicServiceButton(
                name = stringResource(R.string.music_import_apple),
                description = stringResource(R.string.music_services_apple_capabilities),
                connected = appleConnected,
                onClick = { navController.navigate("apple_music_import") },
            )
            Text(stringResource(R.string.music_services_connection_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.music_import_in_development),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(R.string.music_import_soundcloud, R.string.music_import_deezer, R.string.music_import_tidal).forEach { name ->
                IrideFolioSecondaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(name) + " · " + stringResource(R.string.music_import_soon))
                }
            }
        }
    }
}

@Composable
private fun MusicServiceButton(name: String, description: String, connected: Boolean, onClick: () -> Unit) {
    val status = stringResource(if (connected) R.string.music_services_connected else R.string.music_services_not_connected)
    val content: @Composable RowScope.() -> Unit = {
        if (connected) Box(Modifier.padding(end = 8.dp).size(6.dp).clip(CircleShape).background(LocalContentColor.current))
        Text(name)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val modifier = Modifier.fillMaxWidth().semantics { stateDescription = status }
        if (connected) IrideFolioPrimaryButton(onClick = onClick, modifier = modifier, content = content)
        else IrideFolioSecondaryButton(onClick = onClick, modifier = modifier, content = content)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
