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
import com.metrolist.music.constants.TopNavigationBarKey
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
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (topNavigationBarEnabled) Color.Transparent else MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                .rubberBandOverscroll(Orientation.Vertical, scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = if (topNavigationBarEnabled) 20.dp else 16.dp),
        ) {
            Spacer(
                Modifier.windowInsetsPadding(
                    windowInsets.only(WindowInsetsSides.Top)
                )
            )

            // App header — flat in both modes; only colors and the badge treatment differ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = stringResource(R.string.app_name),
                    colorFilter = if (topNavigationBarEnabled) ColorFilter.tint(Color.White) else null,
                    modifier = Modifier.size(if (topNavigationBarEnabled) 64.dp else 72.dp)
                )

                Spacer(Modifier.width(20.dp))

                Column {
                    val appName = stringResource(R.string.metrolist)
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    Text(
                        text = appName,
                        style = if (topNavigationBarEnabled) {
                            MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceMonoFontFamily)
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(if (topNavigationBarEnabled) 6.dp else 8.dp))

                    if (topNavigationBarEnabled) {
                        Text(
                            text = "${stringResource(R.string.about_release_badge)} • ${BuildConfig.ARCHITECTURE.uppercase()}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = SpaceMonoFontFamily,
                                letterSpacing = 0.5.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                stringResource(R.string.about_release_badge),
                                BuildConfig.ARCHITECTURE.uppercase(),
                            ).forEach { badgeText ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (topNavigationBarEnabled) 32.dp else 24.dp))

            // Lead developer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ContributorAvatar(
                    avatarUrl = "",
                    sizeDp = 96,
                    irideMode = topNavigationBarEnabled,
                    contentDescription = leadDeveloper.name,
                    fallbackIconRes = R.drawable.fire
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = leadDeveloper.name,
                        style = if (topNavigationBarEnabled) {
                            MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceMonoFontFamily)
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = if (topNavigationBarEnabled) 34.sp else 38.sp,
                        color = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.about_creator_role),
                        style = if (topNavigationBarEnabled) {
                            MaterialTheme.typography.titleSmall.copy(fontFamily = SpaceMonoFontFamily)
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (topNavigationBarEnabled) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
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
                                irideMode = topNavigationBarEnabled,
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
