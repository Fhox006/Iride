/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.utils.DiagnosticLog
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

private const val LEAD_DEVELOPER_GITHUB = "https://github.com/Fhox006"

@Composable
fun AboutScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, false)
    val windowInsets =
        LocalPlayerAwareWindowInsets.current.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    val scrollState = rememberScrollState()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by
        remember(playerConnection) {
                playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
            }
            .collectAsStateWithLifecycle()

    IridePageFrame(
        title = stringResource(R.string.about),
        scrollState = scrollState,
        navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
        modifier = Modifier.fillMaxSize().background(Color.Transparent),
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
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = FolioShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.ic_logo),
                                contentDescription = stringResource(R.string.about_iride_name),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.about_iride_name),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = IBMPlexMonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.about_iride_release, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.about_developer_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Material3SettingsGroup(
                    title = stringResource(R.string.credits_lead_developer),
                    items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.github),
                                title = { Text(stringResource(R.string.about_developer_github)) },
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.navigate_next),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = { uriHandler.openUri(LEAD_DEVELOPER_GITHUB) },
                            )
                    ),
                )

                Spacer(Modifier.height(12.dp))

                Material3SettingsGroup(
                    title = stringResource(R.string.credits_special_thanks),
                    items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.github),
                                title = { Text(stringResource(R.string.credits_metrolist_author)) },
                                description = {
                                    Text(stringResource(R.string.credits_metrolist_base))
                                },
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.navigate_next),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    uriHandler.openUri("https://github.com/mostafaalagamy")
                                },
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.github),
                                title = { Text("SimpMusic · GPL-3.0") },
                                description = { Text(stringResource(R.string.credits_simpmusic_adaptations)) },
                                onClick = { uriHandler.openUri("https://github.com/maxrave-dev/SimpMusic") },
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.github),
                                title = { Text("ArchiveTune · GPL-3.0") },
                                description = { Text(stringResource(R.string.credits_archivetune_adaptations)) },
                                onClick = { uriHandler.openUri("https://github.com/rukamori/ArchiveTune") },
                            ),
                        ),
                )

                Spacer(Modifier.height(12.dp))

                Material3SettingsGroup(
                    items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.info),
                                title = { Text(stringResource(R.string.credits_license_name)) },
                                description = {
                                    Text(stringResource(R.string.credits_license_desc))
                                },
                                onClick = {
                                    uriHandler.openUri(
                                        "https://github.com/Fhox006/Iride/blob/main/LICENSE"
                                    )
                                },
                            )
                        )
                )

                Spacer(Modifier.height(16.dp))

                if (advancedMode) {
                    Material3SettingsGroup(
                        items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.content_copy),
                                title = { Text(stringResource(R.string.diagnostic_copy_log)) },
                                onClick = {
                                    try {
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE)
                                                as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                "Iride log",
                                                DiagnosticLog.snapshot(),
                                            )
                                        )
                                        Toast.makeText(
                                                context,
                                                R.string.copied_to_clipboard,
                                                Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    } catch (_: Exception) {}
                                },
                            )
                        )
                )

                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom)))
            }
        }
    }
}
