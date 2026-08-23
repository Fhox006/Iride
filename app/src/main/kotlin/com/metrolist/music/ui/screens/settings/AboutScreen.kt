/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.rememberPreference
import java.util.Locale

private data class Contributor(
    val name: String,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
)

private val leadDeveloper = Contributor(
    name = "Fhox",
    githubHandle = "Fhox006",
    avatarUrl = "",
)

private val specialThanks = Contributor(
    name = "Mo Agramy",
    githubHandle = "mostafaalagamy",
)

@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    irideMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    fallbackIconRes: Int = R.drawable.small_icon,
) {
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = shape,
        color = if (irideMode) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (irideMode) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isBlank()) {
                Icon(
                    painter = painterResource(fallbackIconRes),
                    contentDescription = contentDescription,
                    tint = if (irideMode) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size((sizeDp * 0.42f).dp)
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(fallbackIconRes),
                    fallback = painterResource(fallbackIconRes),
                    error = painterResource(fallbackIconRes),
                )
            }
        }
    }
}

@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                .rubberBandOverscroll(Orientation.Vertical, scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(
                Modifier.windowInsetsPadding(
                    windowInsets.only(WindowInsetsSides.Top)
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = stringResource(R.string.app_name),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.width(20.dp))

                Column {
                    val appName = stringResource(R.string.metrolist)
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "${stringResource(R.string.about_release_badge)} • ${BuildConfig.ARCHITECTURE.uppercase()}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = SpaceMonoFontFamily,
                            letterSpacing = 0.5.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ContributorAvatar(
                    avatarUrl = "",
                    sizeDp = 96,
                    irideMode = true,
                    contentDescription = leadDeveloper.name,
                    fallbackIconRes = R.drawable.fire
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = leadDeveloper.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.about_creator_role),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.credits_lead_developer),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.github),
                        title = { Text(stringResource(R.string.credits_github)) },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { uriHandler.openUri(leadDeveloper.githubUrl) }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.credits_special_thanks),
                items = listOf(
                    Material3SettingsItem(
                        leadingContent = {
                            ContributorAvatar(
                                avatarUrl = specialThanks.avatarUrl,
                                sizeDp = 48,
                                irideMode = true,
                                contentDescription = specialThanks.name
                            )
                        },
                        title = { Text(text = specialThanks.name) },
                        description = { Text(stringResource(R.string.credits_collaborator)) },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.github),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { uriHandler.openUri(specialThanks.githubUrl) }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            Material3SettingsGroup(
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text(stringResource(R.string.credits_license_name)) },
                        description = { Text(stringResource(R.string.credits_license_desc)) },
                        onClick = { uriHandler.openUri("https://github.com/Fhox006/Iride/blob/main/LICENSE") }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            Spacer(
                Modifier.windowInsetsPadding(
                    windowInsets.only(WindowInsetsSides.Bottom)
                )
            )
        }

        SettingsBackTopBar(
            title = stringResource(R.string.about),
            navController = navController,
        )
    }
}
