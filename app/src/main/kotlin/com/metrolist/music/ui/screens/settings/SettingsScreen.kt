/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalTopNavBarController
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.AccountPhotoUrlKey
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0)
            true
        } catch (e: Exception) { false }
    }

    val (advancedMode, onAdvancedModeChange) = rememberPreference(AdvancedModeKey, false)
    var showAdvancedMenu by remember { mutableStateOf(false) }
    val topNavBarController = LocalTopNavBarController.current
    val advancedMenuButton: @Composable () -> Unit = {
        Box {
            IconButton(
                onClick = { showAdvancedMenu = true },
                onLongClick = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.advanced_mode)
                )
            }
            DropdownMenu(
                expanded = showAdvancedMenu,
                onDismissRequest = { showAdvancedMenu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                if (advancedMode) R.string.disable_advanced_settings
                                else R.string.enable_advanced_settings
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onSurface),
                    onClick = {
                        onAdvancedModeChange(!advancedMode)
                        showAdvancedMenu = false
                    }
                )
            }
        }
    }
    val backNavigationIcon: @Composable () -> Unit = {
        IconButton(
            onClick = navController::navigateUp,
            onLongClick = navController::backToMain,
        ) {
            Icon(
                painterResource(R.drawable.arrow_back),
                contentDescription = null,
            )
        }
    }

    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, _) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val (accountPhotoUrlPref, _) = rememberPreference(AccountPhotoUrlKey, "")

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountNameFlow by homeViewModel.accountName.collectAsState()
    val accountImageUrlFlow by homeViewModel.accountImageUrl.collectAsState()

    val accountName = if (accountNameFlow != "Guest") accountNameFlow else accountNamePref
    val accountImageUrl: String? = accountImageUrlFlow ?: accountPhotoUrlPref.takeIf { it.isNotEmpty() }

    val settingsScrollState = rememberScrollState()
    val frostBackdrop = rememberFrostBackdrop()
    val headerScrolled by remember {
        derivedStateOf { settingsScrollState.value > 8 }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerScrolled)

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopNavigationBar(
                navigationItems = topNavBarController?.navigationItems ?: emptyList(),
                currentRoute = topNavBarController?.currentRoute,
                onItemClick = topNavBarController?.onItemClick ?: { _, _ -> },
                modifier = Modifier.frostedTopBarBackground(
                    progress = topBarRevealProgress,
                    barColor = MaterialTheme.colorScheme.background,
                    strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backdrop = frostBackdrop,
                ),
                containerColor = Color.Transparent,
                compact = topNavBarController?.compact ?: false,
                accountImageUrl = topNavBarController?.accountImageUrl,
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
    Column(
        Modifier
            .fillMaxSize()
            .recordFrostBackdrop(frostBackdrop)
            .padding(paddingValues)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal
                )
            )
            .rubberBandOverscroll(Orientation.Vertical, settingsScrollState)
            .verticalScroll(settingsScrollState)
    ) {
        if (topNavBarController != null) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                advancedMenuButton()
            }
        }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val avatarSize = 84.dp
            val avatarBorder = Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
            val avatarFallbackBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            val primaryTextColor = MaterialTheme.colorScheme.onSurface
            val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            val fallbackIconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            val nameStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceMonoFontFamily, letterSpacing = (-0.2).sp)

            if (isLoggedIn) {
                if (accountImageUrl != null) {
                    AsyncImage(
                        model = accountImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .then(avatarBorder)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(avatarFallbackBg)
                            .then(avatarBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = fallbackIconTint
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = accountName.ifEmpty { stringResource(R.string.my_account) },
                    style = nameStyle,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                val handle = accountChannelHandle.takeIf { it.isNotEmpty() }
                    ?: accountEmail.takeIf { it.isNotEmpty() }
                if (handle != null) {
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(avatarFallbackBg)
                        .then(avatarBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = fallbackIconTint
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.login),
                    style = nameStyle,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = stringResource(R.string.sign_in_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { navController.navigate("login") },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(
                        text = stringResource(R.string.login),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (advancedMode) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.advanced_mode_banner),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.person),
                    title = { Text(stringResource(R.string.my_account)) },
                    description = { Text(stringResource(R.string.settings_account_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { navController.navigate("settings/my_account") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        val arrowIcon = painterResource(R.drawable.arrow_forward)

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.playback)) },
                    description = { Text(stringResource(R.string.settings_playback_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content)) },
                    description = { Text(stringResource(R.string.settings_content_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/content") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.interface_settings)) },
                    description = { Text(stringResource(R.string.settings_appearance_interface_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/interface") }
                ),
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.app_management_backup)) },
                    description = { Text(stringResource(R.string.settings_storage_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                ),
                if (hasAndroidAuto && advancedMode) Material3SettingsItem(
                    icon = painterResource(R.drawable.ic_android_auto),
                    title = { Text(stringResource(R.string.android_auto)) },
                    description = { Text(stringResource(R.string.android_auto_settings_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/android_auto") }
                ) else null,
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.about)) },
                    description = { Text(stringResource(R.string.settings_system_desc), style = MaterialTheme.typography.bodySmall) },
                    showBadge = BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME,
                    trailingContent = { Icon(painter = arrowIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { navController.navigate("settings/about") }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    leadingContent = { DonationIconBounce() },
                    title = { Text(stringResource(R.string.credits_donations)) },
                    description = { Text(stringResource(R.string.credits_donations_desc), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.open_in_new),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { uriHandler.openUri("https://ko-fi.com/fhox006") }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
            )
        )
    }
    }
    }
}

/** One-shot on mount: heart pops yellow with a small bounce, then settles back to white/still. */
@Composable
private fun DonationIconBounce() {
    val reducedMotion = rememberReducedMotion()
    val settledTint = MaterialTheme.colorScheme.onSurface
    var targetTint by remember { mutableStateOf(settledTint) }
    val tint by animateColorAsState(targetTint, tween(IrideMotion.Quick), label = "donationTint")
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reducedMotion) return@LaunchedEffect
        delay(400)
        targetTint = Color(0xFFFFD54F)
        scale.animateTo(1.22f, tween(120, easing = IrideMotion.EaseOutExpo))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        delay(250)
        targetTint = settledTint
    }
    Icon(
        painter = painterResource(R.drawable.favorite),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    )
}
