/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.ContentCountryKey
import com.metrolist.music.constants.ContentLanguageKey
import com.metrolist.music.constants.CountryCodeToName
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DensityScale
import com.metrolist.music.constants.DensityScaleKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.constants.EnableDynamicIconKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.EnableKugouKey
import com.metrolist.music.constants.EnableLrcLibKey
import com.metrolist.music.constants.EnableLyricsPlus
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoOnlyResultsKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideVideosInLibraryKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LanguageCodeToName
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.constants.ProxyEnabledKey
import com.metrolist.music.constants.ProxyPasswordKey
import com.metrolist.music.constants.ProxyTypeKey
import com.metrolist.music.constants.ProxyUrlKey
import com.metrolist.music.constants.ProxyUsernameKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.constants.ResolveVideoSongsKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.ShowArtistDescriptionKey
import com.metrolist.music.constants.ShowArtistSubscriberCountKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowExplicitBadgeKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowMonthlyListenersKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.constants.TopSize
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.DraggableLyricsProviderItem
import com.metrolist.music.ui.component.DraggableLyricsProviderList
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.ExpandableSettingsSection
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.IconUtils
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy

// ── Internal composables ───────────────────────────────────────────────────────

@Composable
private fun SettingsNavCard(
    icon: Painter,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CardSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun CardSubLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun InlineSettingRow(
    icon: Painter? = null,
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            title()
            description?.invoke()
        }
        trailing?.invoke()
    }
}

@Composable
private fun InlineSwitchRow(
    icon: Painter? = null,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    InlineSettingRow(
        icon = icon,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        description = description?.let { desc ->
            {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        },
        onClick = { if (enabled) onCheckedChange(!checked) }
    )
}

@Composable
private fun CardNavRow(
    icon: Painter,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

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
    val scope = rememberCoroutineScope()
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0)
            true
        } catch (e: Exception) { false }
    }

    // ── Advanced mode ──────────────────────────────────────────────────────
    val (advancedMode, onAdvancedModeChange) = rememberPreference(AdvancedModeKey, false)

    // ── Account prefs ──────────────────────────────────────────────────────
    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    // ── Theme prefs ────────────────────────────────────────────────────────
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.ON)
    val (pureBlack, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = true)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(PureBlackMiniPlayerKey, defaultValue = true)
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = false)
    val (enableDynamicIcon, onEnableDynamicIconChangeRaw) = rememberPreference(EnableDynamicIconKey, defaultValue = true)
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(EnableHighRefreshRateKey, defaultValue = true)
    val (selectedThemeColorInt, onSelectedThemeColorChange) = rememberPreference(SelectedThemeColorKey, DefaultThemeColor.toArgb())
    val selectedThemeColor = Color(selectedThemeColorInt)
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    val onPureBlackChange: (Boolean) -> Unit = { enabled ->
        onPureBlackChangeRaw(enabled)
        onPureBlackMiniPlayerChange(enabled)
    }
    val handleColorSelection: (Color) -> Unit = { color ->
        onSelectedThemeColorChange(color.toArgb())
        onDynamicThemeChange(color == DefaultThemeColor)
    }
    val handleIconChange: (Boolean) -> Unit = { enabled ->
        onEnableDynamicIconChangeRaw(enabled)
        IconUtils.setIcon(activity, enabled)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Icon updated, restart to apply",
                actionLabel = "Restart"
            )
            if (result == SnackbarResult.ActionPerformed) {
                val packageManager = activity.packageManager
                val intent = packageManager.getLaunchIntentForPackage(activity.packageName)
                val componentName = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                activity.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    // ── Interface prefs ────────────────────────────────────────────────────
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (showWrappedCard, onShowWrappedCardChange) = rememberPreference(ShowWrappedCardKey, defaultValue = false)
    val (randomizeHomeOrder, onRandomizeHomeOrderChange) = rememberPreference(RandomizeHomeOrderKey, defaultValue = true)
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(ShowDownloadedPlaylistKey, defaultValue = true)
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(ShowTopPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(ShowCachedPlaylistKey, defaultValue = true)
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(ShowUploadedPlaylistKey, defaultValue = true)
    val sharedPreferences = remember { context.getSharedPreferences("metrolist_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) { sharedPreferences.getFloat("density_scale_factor", 1.0f) }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit { putFloat("density_scale_factor", newScale) }
        showRestartDialog = true
    }

    // ── Content prefs ──────────────────────────────────────────────────────
    val (appLanguage, onAppLanguageChange) = rememberPreference(AppLanguageKey, SYSTEM_DEFAULT)
    val (contentLanguage, onContentLanguageChange) = rememberPreference(ContentLanguageKey, "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(ContentCountryKey, "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(HideExplicitKey, false)
    val (showExplicitBadge, onShowExplicitBadgeChange) = rememberPreference(ShowExplicitBadgeKey, false)
    val (hideVideoSongs, onHideVideoSongsChange) = rememberPreference(HideVideoSongsKey, false)
    val (resolveVideoSongs, onResolveVideoSongsChange) = rememberPreference(ResolveVideoSongsKey, true)
    val (hideVideoOnlyResults, onHideVideoOnlyResultsChange) = rememberPreference(HideVideoOnlyResultsKey, false)
    val (hideVideosInLibrary, onHideVideosInLibraryChange) = rememberPreference(HideVideosInLibraryKey, false)
    val (hideYoutubeShorts, onHideYoutubeShortsChange) = rememberPreference(HideYoutubeShortsKey, false)
    val (showArtistDescription, onShowArtistDescriptionChange) = rememberPreference(ShowArtistDescriptionKey, true)
    val (showArtistSubscriberCount, onShowArtistSubscriberCountChange) = rememberPreference(ShowArtistSubscriberCountKey, true)
    val (showMonthlyListeners, onShowMonthlyListenersChange) = rememberPreference(ShowMonthlyListenersKey, true)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(ProxyEnabledKey, false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(ProxyTypeKey, Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(ProxyUrlKey, "host:port")
    val (proxyUsername, onProxyUsernameChange) = rememberPreference(ProxyUsernameKey, "username")
    val (proxyPassword, onProxyPasswordChange) = rememberPreference(ProxyPasswordKey, "password")
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(
        key = LyricsProviderOrderKey,
        defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder())
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(EnableKugouKey, true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(EnableLrcLibKey, true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(EnableBetterLyricsKey, true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(EnablePaxsenixKey, true)
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(EnableLyricsPlus, false)
    val (lengthTop, onLengthTopChange) = rememberPreference(TopSize, "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(QuickPicksKey, QuickPicks.QUICK_PICKS)

    val providerDisplayNames = mapOf(
        "BetterLyrics" to "Better Lyrics",
        "Paxsenix" to "Paxsenix",
        "LrcLib" to "LrcLib",
        "KuGou" to "KuGou",
        "LyricsPlus" to "LyricsPlus",
        "YouTubeSubtitle" to "YouTube Subtitles",
        "YouTube" to "YouTube",
    )

    // ── UI state ───────────────────────────────────────────────────────────
    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var accountExpanded by rememberSaveable { mutableStateOf(true) }
    var showProxyConfigurationDialog by rememberSaveable { mutableStateOf(false) }
    var showContentLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showContentCountryDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showProviderSelectionDialog by rememberSaveable { mutableStateOf(false) }
    var showProviderPriorityDialog by rememberSaveable { mutableStateOf(false) }
    var showTopLengthDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickPicksDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showTokenEditor) {
        val text = """
            ***INNERTUBE COOKIE*** =$innerTubeCookie
            ***VISITOR DATA*** =$visitorData
            ***DATASYNC ID*** =$dataSyncId
            ***ACCOUNT NAME*** =$accountNamePref
            ***ACCOUNT EMAIL*** =$accountEmail
            ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
        """.trimIndent()

        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(text),
            onDone = { data ->
                var cookie = ""; var visitorDataValue = ""; var dataSyncIdValue = ""
                var accountNameValue = ""; var accountEmailValue = ""; var accountChannelHandleValue = ""
                data.split("\n").forEach {
                    when {
                        it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                        it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                        it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                    }
                }
                accountSettingsViewModel.saveTokenAndRestart(
                    context = context, cookie = cookie, visitorData = visitorDataValue,
                    dataSyncId = dataSyncIdValue, accountName = accountNameValue,
                    accountEmail = accountEmailValue, accountChannelHandle = accountChannelHandleValue,
                )
            },
            onDismiss = { showTokenEditor = false },
            singleLine = false, maxLines = 20,
            isInputValid = { fullText ->
                val cookieLine = fullText.lines().find { it.startsWith("***INNERTUBE COOKIE*** =") }
                val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
            },
            extraContent = {
                Spacer(Modifier.height(8.dp))
                InfoLabel(text = stringResource(R.string.token_adv_login_description))
            }
        )
    }

    if (showProxyConfigurationDialog) {
        var expandedDropdown by remember { mutableStateOf(false) }
        var tempProxyUrl by rememberSaveable { mutableStateOf(proxyUrl) }
        var tempProxyUsername by rememberSaveable { mutableStateOf(proxyUsername) }
        var tempProxyPassword by rememberSaveable { mutableStateOf(proxyPassword) }
        var authEnabled by rememberSaveable { mutableStateOf(proxyUsername.isNotBlank() || proxyPassword.isNotBlank()) }

        AlertDialog(
            onDismissRequest = { showProxyConfigurationDialog = false },
            title = { Text(stringResource(R.string.config_proxy)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = proxyType.name, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.proxy_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                            listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS).forEach { type ->
                                DropdownMenuItem(text = { Text(type.name) }, onClick = { onProxyTypeChange(type); expandedDropdown = false })
                            }
                        }
                    }
                    OutlinedTextField(value = tempProxyUrl, onValueChange = { tempProxyUrl = it }, label = { Text(stringResource(R.string.proxy_url)) }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.enable_authentication))
                        Switch(checked = authEnabled, onCheckedChange = { authEnabled = it; if (!it) { tempProxyUsername = ""; tempProxyPassword = "" } })
                    }
                    AnimatedVisibility(visible = authEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = tempProxyUsername, onValueChange = { tempProxyUsername = it }, label = { Text(stringResource(R.string.proxy_username)) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = tempProxyPassword, onValueChange = { tempProxyPassword = it }, label = { Text(stringResource(R.string.proxy_password)) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onProxyUrlChange(tempProxyUrl)
                    onProxyUsernameChange(if (authEnabled) tempProxyUsername else "")
                    onProxyPasswordChange(if (authEnabled) tempProxyPassword else "")
                    showProxyConfigurationDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showProxyConfigurationDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showContentLanguageDialog) {
        EnumDialog(
            onDismiss = { showContentLanguageDialog = false },
            onSelect = { onContentLanguageChange(it); showContentLanguageDialog = false },
            title = stringResource(R.string.content_language),
            current = contentLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = { LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) } }
        )
    }

    if (showContentCountryDialog) {
        EnumDialog(
            onDismiss = { showContentCountryDialog = false },
            onSelect = { onContentCountryChange(it); showContentCountryDialog = false },
            title = stringResource(R.string.content_country),
            current = contentCountry,
            values = (listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList()),
            valueText = { CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) } }
        )
    }

    if (showAppLanguageDialog) {
        EnumDialog(
            onDismiss = { showAppLanguageDialog = false },
            onSelect = { onAppLanguageChange(it); showAppLanguageDialog = false },
            title = stringResource(R.string.app_language),
            current = appLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = { LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) } }
        )
    }

    if (showProviderSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showProviderSelectionDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_selection)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple(enableLrclib, onEnableLrclibChange, Pair(R.string.enable_lrclib, R.string.enable_lrclib_desc)),
                        Triple(enableKugou, onEnableKugouChange, Pair(R.string.enable_kugou, R.string.enable_kugou_desc)),
                        Triple(enableBetterLyrics, onEnableBetterLyricsChange, Pair(R.string.enable_better_lyrics, R.string.enable_better_lyrics_desc)),
                        Triple(enablePaxsenix, onEnablePaxsenixChange, Pair(R.string.enable_paxsenix, R.string.enable_paxsenix_desc)),
                        Triple(enableLyricsPlus, onEnableLyricsPlusChange, Pair(R.string.enable_lyricsplus, R.string.enable_lyricsplus_desc)),
                    ).forEach { (checked, onChange, labels) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(labels.first))
                                Text(stringResource(labels.second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = checked, onCheckedChange = onChange, thumbContent = {
                                Icon(painter = painterResource(if (checked) R.drawable.check else R.drawable.close), contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                            })
                        }
                    }
                    Column(modifier = Modifier.padding(2.dp)) {
                        Text(stringResource(R.string.youtube_music_lyrics_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProviderSelectionDialog = false }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (showProviderPriorityDialog) {
        val currentOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val normalizedOrder = currentOrder.filter { it in defaultOrder } + defaultOrder.filter { it !in currentOrder }
        val enabledProviders = setOf(
            "LrcLib".takeIf { enableLrclib }, "KuGou".takeIf { enableKugou },
            "BetterLyrics".takeIf { enableBetterLyrics }, "Paxsenix".takeIf { enablePaxsenix },
            "LyricsPlus".takeIf { enableLyricsPlus },
        ).filterNotNull().toSet()
        val lyricsIcon = painterResource(R.drawable.lyrics)
        val draggableItems = remember { mutableStateListOf<DraggableLyricsProviderItem>() }

        LaunchedEffect(normalizedOrder, enableLrclib, enableKugou, enableBetterLyrics, enablePaxsenix, enableLyricsPlus) {
            val orderedEnabledProviders = normalizedOrder.filter { it in enabledProviders }
            draggableItems.clear()
            draggableItems.addAll(orderedEnabledProviders.mapNotNull { providerName ->
                LyricsProviderRegistry.getProviderByName(providerName) ?: return@mapNotNull null
                DraggableLyricsProviderItem(id = providerName, name = providerDisplayNames[providerName] ?: providerName, icon = lyricsIcon)
            })
        }

        AlertDialog(
            onDismissRequest = { showProviderPriorityDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_priority)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    Text(stringResource(R.string.lyrics_provider_priority_desc), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DraggableLyricsProviderList(
                        items = draggableItems,
                        onItemsReordered = { reorderedItems ->
                            val enabledOrder = reorderedItems.map { it.id }
                            val disabledOrder = normalizedOrder.filter { it !in enabledProviders }
                            onLyricsProviderOrderChange(LyricsProviderRegistry.serializeProviderOrder(enabledOrder + disabledOrder))
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showProviderPriorityDialog = false }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (showTopLengthDialog) {
        var tempLength by rememberSaveable { mutableFloatStateOf(lengthTop.toFloat()) }
        AlertDialog(
            onDismissRequest = { showTopLengthDialog = false },
            title = { Text(stringResource(R.string.top_length)) },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tempLength.toInt().toString())
                    Slider(value = tempLength, onValueChange = { tempLength = it }, valueRange = 1f..100f, steps = 98)
                }
            },
            confirmButton = { TextButton(onClick = { onLengthTopChange(tempLength.toInt().toString()); showTopLengthDialog = false }) { Text(stringResource(R.string.save)) } }
        )
    }

    if (showQuickPicksDialog) {
        EnumDialog(
            onDismiss = { showQuickPicksDialog = false },
            onSelect = { onQuickPicksChange(it); showQuickPicksDialog = false },
            title = stringResource(R.string.set_quick_picks),
            current = quickPicks,
            values = QuickPicks.values().toList(),
            valueText = { when (it) { QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks); QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened) } }
        )
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = { onDefaultOpenTabChange(it); showDefaultOpenTabDialog = false },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = { when (it) { NavigationTab.HOME -> stringResource(R.string.home); NavigationTab.SEARCH -> stringResource(R.string.search); NavigationTab.LIBRARY -> stringResource(R.string.filter_library) } }
        )
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = { onDefaultChipChange(it); showDefaultChipDialog = false },
            title = stringResource(R.string.default_category),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = { onGridItemSizeChange(it); showGridSizeDialog = false },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = { when (it) { GridItemSize.BIG -> stringResource(R.string.big); GridItemSize.SMALL -> stringResource(R.string.small) } }
        )
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = { TextButton(onClick = { showDensityScaleDialog = false }) { Text(stringResource(android.R.string.cancel)) } }
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDensityScaleChange(scale.value); showDensityScaleDialog = false }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = scale.label, style = MaterialTheme.typography.bodyLarge, color = if (densityScale == scale.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        DefaultDialog(
            onDismiss = { showRestartDialog = false },
            buttons = {
                TextButton(onClick = { showRestartDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRestartDialog = false
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }) { Text(stringResource(R.string.restart)) }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.restart_required), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.density_restart_message), style = MaterialTheme.typography.bodyMedium)
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

        // ── Account section ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn && accountImageUrl != null) {
                AsyncImage(
                    model = accountImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val handle = accountChannelHandle.takeIf { it.isNotEmpty() }
                    ?: accountEmail.takeIf { it.isNotEmpty() }
                if (handle != null) {
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.sign_in_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("login") },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.login))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── My Account ────────────────────────────────────────────────────
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.person),
                    title = { Text(stringResource(R.string.my_account)) },
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

        // ── Aspetto ────────────────────────────────────────────────────────
        ExpandableSettingsSection(
            title = stringResource(R.string.appearance),
            icon = painterResource(R.drawable.palette),
            description = stringResource(R.string.settings_appearance_desc)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.theme_mode),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModeCircle(darkMode = darkMode, pureBlack = pureBlack, targetMode = DarkMode.AUTO, targetPureBlack = pureBlack, onClick = { onDarkModeChange(DarkMode.AUTO) }, showIcon = true)
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                ModeCircle(darkMode = darkMode, pureBlack = pureBlack, targetMode = DarkMode.OFF, targetPureBlack = false, onClick = { onDarkModeChange(DarkMode.OFF); onPureBlackChange(false) }, showIcon = false)
                                ModeCircle(darkMode = darkMode, pureBlack = pureBlack, targetMode = DarkMode.ON, targetPureBlack = false, onClick = { onDarkModeChange(DarkMode.ON); onPureBlackChange(false) }, showIcon = false)
                                ModeCircle(darkMode = darkMode, pureBlack = pureBlack, targetMode = DarkMode.ON, targetPureBlack = true, onClick = { onDarkModeChange(DarkMode.ON); onPureBlackChange(true) }, showIcon = false)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.color_palette),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(PaletteColors) { palette ->
                                    val isDynamicPalette = palette.seedColor == Color.Transparent
                                    val isSelected = if (isDynamicPalette) selectedThemeColor == DefaultThemeColor
                                                     else selectedThemeColor == palette.seedColor
                                    PaletteItem(
                                        palette = palette,
                                        isSelected = isSelected,
                                        onClick = {
                                            val colorToSave = if (isDynamicPalette) DefaultThemeColor else palette.seedColor
                                            handleColorSelection(colorToSave)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (advancedMode) {
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.palette),
                            title = stringResource(R.string.enable_dynamic_icon),
                            checked = enableDynamicIcon,
                            onCheckedChange = { handleIconChange(it) }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    CardSubLabel(text = stringResource(R.string.interface_section))
                    InlineSettingRow(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.default_open_tab), style = MaterialTheme.typography.bodyLarge) },
                        description = {
                            Text(
                                text = when (defaultOpenTab) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.SEARCH -> stringResource(R.string.search)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showDefaultOpenTabDialog = true }
                    )
                    InlineSettingRow(
                        icon = painterResource(R.drawable.tab),
                        title = { Text(stringResource(R.string.default_category), style = MaterialTheme.typography.bodyLarge) },
                        description = {
                            Text(
                                text = when (defaultChip) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showDefaultChipDialog = true }
                    )
                    if (advancedMode) {
                        InlineSettingRow(
                            icon = painterResource(R.drawable.grid_view),
                            title = { Text(stringResource(R.string.grid_cell_size), style = MaterialTheme.typography.bodyLarge) },
                            description = {
                                Text(
                                    text = when (gridItemSize) { GridItemSize.BIG -> stringResource(R.string.big); GridItemSize.SMALL -> stringResource(R.string.small) },
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { showGridSizeDialog = true }
                        )
                        CardSubLabel(text = stringResource(R.string.home))
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.star),
                            title = stringResource(R.string.show_wrapped_card),
                            checked = showWrappedCard,
                            onCheckedChange = onShowWrappedCardChange
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.shuffle),
                            title = stringResource(R.string.randomize_home_order),
                            description = stringResource(R.string.randomize_home_order_desc),
                            checked = randomizeHomeOrder,
                            onCheckedChange = onRandomizeHomeOrderChange
                        )
                        CardSubLabel(text = stringResource(R.string.auto_playlists))
                        CardNavRow(
                            icon = painterResource(R.drawable.favorite),
                            title = stringResource(R.string.behavior),
                            onClick = { navController.navigate("settings/appearance/interface") }
                        )
                        InlineSettingRow(
                            icon = painterResource(R.drawable.grid_view),
                            title = { Text(stringResource(R.string.display_density), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(DensityScale.fromValue(densityScale).label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showDensityScaleDialog = true }
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.settings),
                            title = stringResource(R.string.enable_high_refresh_rate),
                            checked = enableHighRefreshRate,
                            onCheckedChange = onEnableHighRefreshRateChange
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Riproduzione ───────────────────────────────────────────────────
        ExpandableSettingsSection(
            title = stringResource(R.string.playback),
            icon = painterResource(R.drawable.play),
            description = stringResource(R.string.settings_playback_desc)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                CardNavRow(
                    icon = painterResource(R.drawable.play),
                    title = stringResource(R.string.playback),
                    onClick = { navController.navigate("settings/player") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Contenuti ──────────────────────────────────────────────────────
        ExpandableSettingsSection(
            title = stringResource(R.string.content),
            icon = painterResource(R.drawable.language),
            description = stringResource(R.string.settings_content_desc)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    CardSubLabel(text = stringResource(R.string.general))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        InlineSettingRow(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.app_language), style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri()))
                            }
                        )
                    } else {
                        InlineSettingRow(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.app_language), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(LanguageCodeToName.getOrElse(appLanguage) { stringResource(R.string.system_default) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showAppLanguageDialog = true }
                        )
                    }
                    if (advancedMode) {
                        InlineSettingRow(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.content_language), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(LanguageCodeToName.getOrElse(contentLanguage) { stringResource(R.string.system_default) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showContentLanguageDialog = true }
                        )
                        InlineSettingRow(
                            icon = painterResource(R.drawable.location_on),
                            title = { Text(stringResource(R.string.content_country), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(CountryCodeToName.getOrElse(contentCountry) { stringResource(R.string.system_default) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showContentCountryDialog = true }
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.wifi_proxy),
                            title = stringResource(R.string.enable_proxy),
                            checked = proxyEnabled,
                            onCheckedChange = onProxyEnabledChange
                        )
                        if (proxyEnabled) {
                            InlineSettingRow(
                                icon = painterResource(R.drawable.settings),
                                title = { Text(stringResource(R.string.config_proxy), style = MaterialTheme.typography.bodyLarge) },
                                onClick = { showProxyConfigurationDialog = true }
                            )
                        }
                        InlineSettingRow(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_provider_selection), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(stringResource(R.string.lyrics_provider_selection_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showProviderSelectionDialog = true }
                        )
                        InlineSettingRow(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_provider_priority), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(stringResource(R.string.lyrics_provider_priority_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showProviderPriorityDialog = true }
                        )
                        InlineSettingRow(
                            icon = painterResource(R.drawable.language_korean_latin),
                            title = { Text(stringResource(R.string.lyrics_romanization), style = MaterialTheme.typography.bodyLarge) },
                            onClick = { navController.navigate("settings/content/romanization") }
                        )
                    }
                    CardSubLabel(text = stringResource(R.string.music_section))
                    InlineSwitchRow(
                        icon = painterResource(R.drawable.explicit),
                        title = stringResource(R.string.hide_explicit),
                        checked = hideExplicit,
                        onCheckedChange = onHideExplicitChange
                    )
                    if (advancedMode) {
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.explicit),
                            title = stringResource(R.string.show_explicit_badge),
                            description = stringResource(R.string.show_explicit_badge_desc),
                            checked = showExplicitBadge,
                            onCheckedChange = onShowExplicitBadgeChange
                        )
                    }
                    InlineSwitchRow(
                        icon = painterResource(R.drawable.slow_motion_video),
                        title = stringResource(R.string.hide_video_songs),
                        checked = hideVideoSongs,
                        onCheckedChange = onHideVideoSongsChange
                    )
                    if (advancedMode) {
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.cached),
                            title = stringResource(R.string.resolve_video_songs),
                            description = stringResource(R.string.resolve_video_songs_desc),
                            checked = resolveVideoSongs,
                            onCheckedChange = onResolveVideoSongsChange
                        )
                    }
                    InlineSwitchRow(
                        icon = painterResource(R.drawable.close),
                        title = stringResource(R.string.hide_video_only_results),
                        description = stringResource(R.string.hide_video_only_results_desc),
                        checked = hideVideoOnlyResults,
                        onCheckedChange = onHideVideoOnlyResultsChange
                    )
                    if (advancedMode) {
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.library_music),
                            title = stringResource(R.string.hide_videos_in_library),
                            description = stringResource(R.string.hide_videos_in_library_desc),
                            checked = hideVideosInLibrary,
                            onCheckedChange = onHideVideosInLibraryChange
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.hide_image),
                            title = stringResource(R.string.hide_youtube_shorts),
                            checked = hideYoutubeShorts,
                            onCheckedChange = onHideYoutubeShortsChange
                        )
                        CardSubLabel(text = stringResource(R.string.artist_page_settings))
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.info),
                            title = stringResource(R.string.show_artist_description),
                            checked = showArtistDescription,
                            onCheckedChange = onShowArtistDescriptionChange
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.person),
                            title = stringResource(R.string.show_artist_subscriber_count),
                            checked = showArtistSubscriberCount,
                            onCheckedChange = onShowArtistSubscriberCountChange
                        )
                        InlineSwitchRow(
                            icon = painterResource(R.drawable.person),
                            title = stringResource(R.string.show_artist_monthly_listeners),
                            checked = showMonthlyListeners,
                            onCheckedChange = onShowMonthlyListenersChange
                        )
                        CardSubLabel(text = stringResource(R.string.home))
                        InlineSettingRow(
                            icon = painterResource(R.drawable.trending_up),
                            title = { Text(stringResource(R.string.top_length), style = MaterialTheme.typography.bodyLarge) },
                            description = { Text(lengthTop, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showTopLengthDialog = true }
                        )
                        InlineSettingRow(
                            icon = painterResource(R.drawable.home_outlined),
                            title = { Text(stringResource(R.string.set_quick_picks), style = MaterialTheme.typography.bodyLarge) },
                            description = {
                                Text(
                                    text = when (quickPicks) {
                                        QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                        QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                                    },
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { showQuickPicksDialog = true }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Traduzione AI ──────────────────────────────────────────────────
        ExpandableSettingsSection(
            title = stringResource(R.string.lyrics_translation),
            icon = painterResource(R.drawable.translate),
            description = stringResource(R.string.settings_ai_desc)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                CardNavRow(
                    icon = painterResource(R.drawable.translate),
                    title = stringResource(R.string.lyrics_translation),
                    onClick = { navController.navigate("settings/ai") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Integrations ───────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.settings_section_integrations),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsNavCard(
                icon = painterResource(R.drawable.integration),
                title = stringResource(R.string.integrations),
                description = stringResource(R.string.settings_integrations_desc),
                onClick = { navController.navigate("settings/integrations") }
            )
            if (hasAndroidAuto) {
                SettingsNavCard(
                    icon = painterResource(R.drawable.ic_android_auto),
                    title = stringResource(R.string.android_auto),
                    description = "",
                    onClick = { navController.navigate("settings/android_auto") }
                )
            }
            SettingsNavCard(
                icon = painterResource(R.drawable.group_outlined),
                title = stringResource(R.string.together),
                description = "",
                onClick = { navController.navigate("listen_together") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.settings_section_privacy),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsNavCard(
            icon = painterResource(R.drawable.security),
            title = stringResource(R.string.privacy),
            description = stringResource(R.string.settings_privacy_desc),
            onClick = { navController.navigate("settings/privacy") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.settings_section_storage),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsNavCard(
                icon = painterResource(R.drawable.storage),
                title = stringResource(R.string.storage),
                description = stringResource(R.string.settings_storage_desc),
                onClick = { navController.navigate("settings/storage") }
            )
            SettingsNavCard(
                icon = painterResource(R.drawable.restore),
                title = stringResource(R.string.backup_restore),
                description = "",
                onClick = { navController.navigate("settings/backup_restore") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.settings_section_system),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isAndroid12OrLater) {
                SettingsNavCard(
                    icon = painterResource(R.drawable.link),
                    title = stringResource(R.string.default_links),
                    description = "",
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                "package:${context.packageName}".toUri()
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, R.string.open_app_settings_error, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            if (BuildConfig.UPDATER_AVAILABLE) {
                SettingsNavCard(
                    icon = painterResource(R.drawable.update),
                    title = stringResource(R.string.updater),
                    description = "",
                    onClick = { navController.navigate("settings/updater") }
                )
            }
            SettingsNavCard(
                icon = painterResource(R.drawable.info),
                title = stringResource(R.string.about),
                description = stringResource(R.string.settings_system_desc),
                badge = BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME,
                onClick = { navController.navigate("settings/about") }
            )
            if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME) {
                val releaseInfo = Updater.getCachedLatestRelease()
                val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                if (downloadUrl != null) {
                    SettingsNavCard(
                        icon = painterResource(R.drawable.update),
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.new_version_available),
                        description = latestVersionName,
                        onClick = { uriHandler.openUri(downloadUrl) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        actions = {
            IconButton(
                onClick = { onAdvancedModeChange(!advancedMode) },
                onLongClick = {}
            ) {
                Icon(
                    painter = painterResource(if (advancedMode) R.drawable.lock_open else R.drawable.lock),
                    contentDescription = stringResource(R.string.advanced_mode),
                    tint = if (advancedMode) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
