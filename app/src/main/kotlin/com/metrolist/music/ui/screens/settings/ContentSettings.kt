/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings
import com.metrolist.music.ui.component.IrideSlider
import com.metrolist.music.ui.component.IrideSwitch

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.ContentCountryKey
import com.metrolist.music.constants.ContentLanguageKey
import com.metrolist.music.constants.CountryCodeToName
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.constants.EnableBetterLyricsUnisonKey
import com.metrolist.music.constants.EnableBetterLyricsSillabaKey
import com.metrolist.music.constants.EnableKugouKey
import com.metrolist.music.constants.EnableLrcLibKey
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.constants.EnableLyricsPlus
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoOnlyResultsKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideVideosInLibraryKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.ResolveVideoSongsKey
import com.metrolist.music.constants.LanguageCodeToName
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.constants.ProxyEnabledKey
import com.metrolist.music.constants.ProxyPasswordKey
import com.metrolist.music.constants.ProxyTypeKey
import com.metrolist.music.constants.ProxyUrlKey
import com.metrolist.music.constants.ProxyUsernameKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.RespectAgentPositioningKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.constants.TopSize
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.DraggableLyricsProviderItem
import com.metrolist.music.ui.component.DraggableLyricsProviderList
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import java.net.Proxy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController
) {
    val context = LocalContext.current
    val (appLanguage, onAppLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (hideVideoSongs, onHideVideoSongsChange) = rememberPreference(key = HideVideoSongsKey, defaultValue = false)
    val (resolveVideoSongs, onResolveVideoSongsChange) = rememberPreference(key = ResolveVideoSongsKey, defaultValue = true)
    val (hideVideoOnlyResults, onHideVideoOnlyResultsChange) = rememberPreference(key = HideVideoOnlyResultsKey, defaultValue = false)
    val (hideVideosInLibrary, onHideVideosInLibraryChange) = rememberPreference(key = HideVideosInLibraryKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (proxyUsername, onProxyUsernameChange) = rememberPreference(key = ProxyUsernameKey, defaultValue = "username")
    val (proxyPassword, onProxyPasswordChange) = rememberPreference(key = ProxyPasswordKey, defaultValue = "password")
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enableBetterLyricsUnison, onEnableBetterLyricsUnisonChange) = rememberPreference(key = EnableBetterLyricsUnisonKey, defaultValue = true)
    val (enableBetterLyricsSillaba, onEnableBetterLyricsSillabaChange) = rememberPreference(key = EnableBetterLyricsSillabaKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(key = EnableLyricsPlus, defaultValue = false)
    val (respectAgentPositioning, onRespectAgentPositioningChange) =
        rememberPreference(key = RespectAgentPositioningKey, defaultValue = true)
    val (lyricsClick, onLyricsClickChange) =
        rememberPreference(key = LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(key = LyricsScrollKey, defaultValue = true)
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(
        key = LyricsProviderOrderKey,
        defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder())
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, false)
    val providerDisplayNames =
        mapOf(
            "BetterLyricsUnison" to "Better Lyrics Unison",
            "BetterLyricsSillaba" to "Better Lyrics Sillaba",
            "BetterLyrics" to "Better Lyrics",
            "Paxsenix" to "Paxsenix",
            "LrcLib" to "LrcLib",
            "KuGou" to "KuGou",
            "LyricsPlus" to "LyricsPlus",
            "YouTubeSubtitle" to "YouTube Subtitles",
            "YouTube" to "YouTube",
        )

    var showProxyConfigurationDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showProxyConfigurationDialog) {
        var expandedDropdown by remember { mutableStateOf(false) }

        var tempProxyUrl by rememberSaveable { mutableStateOf(proxyUrl) }
        var tempProxyUsername by rememberSaveable { mutableStateOf(proxyUsername) }
        var tempProxyPassword by rememberSaveable { mutableStateOf(proxyPassword) }
        var authEnabled by rememberSaveable { mutableStateOf(proxyUsername.isNotBlank() || proxyPassword.isNotBlank()) }

        AlertDialog(
            onDismissRequest = { showProxyConfigurationDialog = false },
            title = {
                Text(stringResource(R.string.config_proxy))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = proxyType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.proxy_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        onProxyTypeChange(type)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempProxyUrl,
                        onValueChange = { tempProxyUrl = it },
                        label = { Text(stringResource(R.string.proxy_url)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.enable_authentication))
                        IrideSwitch(
                            checked = authEnabled,
                            onCheckedChange = {
                                authEnabled = it
                                if (!it) {
                                    tempProxyUsername = ""
                                    tempProxyPassword = ""
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = authEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempProxyUsername,
                                onValueChange = { tempProxyUsername = it },
                                label = { Text(stringResource(R.string.proxy_username)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = tempProxyPassword,
                                onValueChange = { tempProxyPassword = it },
                                label = { Text(stringResource(R.string.proxy_password)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onProxyUrlChange(tempProxyUrl)
                        onProxyUsernameChange(if (authEnabled) tempProxyUsername else "")
                        onProxyPasswordChange(if (authEnabled) tempProxyPassword else "")
                        showProxyConfigurationDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProxyConfigurationDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var showContentLanguageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showContentLanguageDialog) {
        EnumDialog(
            onDismiss = { showContentLanguageDialog = false },
            onSelect = {
                onContentLanguageChange(it)
                showContentLanguageDialog = false
            },
            title = stringResource(R.string.content_language),
            current = contentLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }

    var showContentCountryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showContentCountryDialog) {
        EnumDialog(
            onDismiss = { showContentCountryDialog = false },
            onSelect = {
                onContentCountryChange(it)
                showContentCountryDialog = false
            },
            title = stringResource(R.string.content_country),
            current = contentCountry,
            values = (listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList()),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }

    var showAppLanguageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showAppLanguageDialog) {
        EnumDialog(
            onDismiss = { showAppLanguageDialog = false },
            onSelect = {
                onAppLanguageChange(it)
                showAppLanguageDialog = false
            },
            title = stringResource(R.string.app_language),
            current = appLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }

    var showProviderSelectionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showProviderSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showProviderSelectionDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_selection)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_lrclib))
                            Text(
                                text = stringResource(R.string.enable_lrclib_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableLrclib,
                            onCheckedChange = onEnableLrclibChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableLrclib) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_kugou))
                            Text(
                                text = stringResource(R.string.enable_kugou_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableKugou,
                            onCheckedChange = onEnableKugouChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableKugou) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_better_lyrics))
                            Text(
                                text = stringResource(R.string.enable_better_lyrics_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableBetterLyrics,
                            onCheckedChange = onEnableBetterLyricsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableBetterLyrics) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_better_lyrics_unison))
                            Text(
                                text = stringResource(R.string.enable_better_lyrics_unison_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableBetterLyricsUnison,
                            onCheckedChange = onEnableBetterLyricsUnisonChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableBetterLyricsUnison) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_better_lyrics_sillaba))
                            Text(
                                text = stringResource(R.string.enable_better_lyrics_sillaba_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableBetterLyricsSillaba,
                            onCheckedChange = onEnableBetterLyricsSillabaChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableBetterLyricsSillaba) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_paxsenix))
                            Text(
                                text = stringResource(R.string.enable_paxsenix_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enablePaxsenix,
                            onCheckedChange = onEnablePaxsenixChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enablePaxsenix) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.enable_lyricsplus))
                            Text(
                                text = stringResource(R.string.enable_lyricsplus_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IrideSwitch(
                            checked = enableLyricsPlus,
                            onCheckedChange = onEnableLyricsPlusChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableLyricsPlus) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    Column(modifier = Modifier.padding(2.dp)) {
                        Text(
                            text = stringResource(R.string.youtube_music_lyrics_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProviderSelectionDialog = false }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    var showQuickPicksDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showQuickPicksDialog) {
        EnumDialog(
            onDismiss = { showQuickPicksDialog = false },
            onSelect = {
                onQuickPicksChange(it)
                showQuickPicksDialog = false
            },
            title = stringResource(R.string.set_quick_picks),
            current = quickPicks,
            values = QuickPicks.values().toList(),
            valueText = {
                when (it) {
                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                }
            }
        )
    }

    var showTopLengthDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showTopLengthDialog) {
        var tempLength by rememberSaveable { mutableFloatStateOf(lengthTop.toFloat()) }

        AlertDialog(
            onDismissRequest = { showTopLengthDialog = false },
            title = { Text(stringResource(R.string.top_length)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tempLength.toInt().toString())
                    IrideSlider(
                        value = tempLength,
                        onValueChange = { tempLength = it },
                        valueRange = 1f..100f,
                        steps = 98
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLengthTopChange(tempLength.toInt().toString())
                        showTopLengthDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        )
    }

    var showProviderPriorityDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showProviderPriorityDialog) {
        val currentOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val normalizedOrder = currentOrder.filter { it in defaultOrder } +
                defaultOrder.filter { it !in currentOrder }

        val enabledProviders = setOf(
            "LrcLib".takeIf { enableLrclib },
            "KuGou".takeIf { enableKugou },
            "BetterLyrics".takeIf { enableBetterLyrics },
            "BetterLyricsUnison".takeIf { enableBetterLyricsUnison },
            "BetterLyricsSillaba".takeIf { enableBetterLyricsSillaba },
            "Paxsenix".takeIf { enablePaxsenix },
            "LyricsPlus".takeIf { enableLyricsPlus },
        ).filterNotNull().toSet()
        val lyricsIcon = painterResource(R.drawable.lyrics)
        val draggableItems = remember { mutableStateListOf<DraggableLyricsProviderItem>() }

        LaunchedEffect(
            normalizedOrder, enableLrclib, enableKugou, enableBetterLyrics,
            enableBetterLyricsUnison, enableBetterLyricsSillaba, enablePaxsenix, enableLyricsPlus
        ) {
            val orderedEnabledProviders = normalizedOrder.filter { it in enabledProviders }
            draggableItems.clear()
            draggableItems.addAll(
                orderedEnabledProviders.mapNotNull { providerName ->
                    LyricsProviderRegistry.getProviderByName(providerName) ?: return@mapNotNull null
                    DraggableLyricsProviderItem(
                        id = providerName,
                        name = providerDisplayNames[providerName] ?: providerName,
                        icon = lyricsIcon,
                    )
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showProviderPriorityDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_priority)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text(
                        stringResource(R.string.lyrics_provider_priority_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DraggableLyricsProviderList(
                        items = draggableItems,
                        onItemsReordered = { reorderedItems ->
                            val enabledOrder = reorderedItems.map { it.id }
                            val disabledOrder = normalizedOrder.filter { it !in enabledProviders }
                            onLyricsProviderOrderChange(
                                LyricsProviderRegistry.serializeProviderOrder(enabledOrder + disabledOrder)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProviderPriorityDialog = false }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    val contentScrollState = rememberScrollState()
    val frostBackdrop = rememberFrostBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordFrostBackdrop(frostBackdrop)
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp),
            ) {
                Material3SettingsGroup(
                    title = stringResource(R.string.general),
                    items = listOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.language),
                                title = { Text(stringResource(R.string.app_language)) },
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APP_LOCALE_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        )
                                    )
                                }
                            )
                        } else {
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.language),
                                title = { Text(stringResource(R.string.app_language)) },
                                description = {
                                    Text(
                                        LanguageCodeToName.getOrElse(appLanguage) { stringResource(R.string.system_default) }
                                    )
                                },
                                onClick = { showAppLanguageDialog = true }
                            )
                        },
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.content_language)) },
                            description = {
                                Text(
                                    LanguageCodeToName.getOrElse(contentLanguage) { stringResource(R.string.system_default) }
                                )
                            },
                            onClick = { showContentLanguageDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.location_on),
                            title = { Text(stringResource(R.string.content_country)) },
                            description = {
                                Text(
                                    CountryCodeToName.getOrElse(contentCountry) { stringResource(R.string.system_default) }
                                )
                            },
                            onClick = { showContentCountryDialog = true }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.explicit),
                            title = { Text(stringResource(R.string.hide_explicit)) },
                            description = { Text(stringResource(R.string.hide_explicit_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = hideExplicit,
                                    onCheckedChange = onHideExplicitChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (hideExplicit) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onHideExplicitChange(!hideExplicit) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.slow_motion_video),
                            title = { Text(stringResource(R.string.hide_video_songs)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = hideVideoSongs,
                                    onCheckedChange = onHideVideoSongsChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (hideVideoSongs) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onHideVideoSongsChange(!hideVideoSongs) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.cached),
                            title = { Text(stringResource(R.string.resolve_video_songs)) },
                            description = { Text(stringResource(R.string.resolve_video_songs_desc)) },
                            trailingContent = {
                                IrideSwitch(
                                    checked = resolveVideoSongs,
                                    onCheckedChange = onResolveVideoSongsChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (resolveVideoSongs) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { onResolveVideoSongsChange(!resolveVideoSongs) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.group_outlined),
                            title = { Text(stringResource(R.string.together)) },
                            description = { Text(stringResource(R.string.listen_together_description), style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { navController.navigate("listen_together") }
                        )
                    )
                )

                AnimatedVisibility(visible = advancedMode) {
                    AnimatedVisibility(visible = resolveVideoSongs) {
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.close),
                                    title = { Text(stringResource(R.string.hide_video_only_results)) },
                                    description = { Text(stringResource(R.string.hide_video_only_results_desc)) },
                                    trailingContent = {
                                        IrideSwitch(
                                            checked = hideVideoOnlyResults,
                                            onCheckedChange = onHideVideoOnlyResultsChange,
                                            thumbContent = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (hideVideoOnlyResults) R.drawable.check else R.drawable.close
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    },
                                    onClick = { onHideVideoOnlyResultsChange(!hideVideoOnlyResults) }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.library_music),
                                    title = { Text(stringResource(R.string.hide_videos_in_library)) },
                                    description = { Text(stringResource(R.string.hide_videos_in_library_desc)) },
                                    trailingContent = {
                                        IrideSwitch(
                                            checked = hideVideosInLibrary,
                                            onCheckedChange = onHideVideosInLibraryChange,
                                            thumbContent = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (hideVideosInLibrary) R.drawable.check else R.drawable.close
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    },
                                    onClick = { onHideVideosInLibraryChange(!hideVideosInLibrary) }
                                )
                            )
                        )
                    }
                }


                AnimatedVisibility(visible = advancedMode) {
                    Column {
                        Spacer(modifier = Modifier.height(27.dp))

                        Material3SettingsGroup(
                            title = stringResource(R.string.proxy),
                            items = buildList {
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.wifi_proxy),
                                        title = { Text(stringResource(R.string.enable_proxy)) },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = proxyEnabled,
                                                onCheckedChange = onProxyEnabledChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (proxyEnabled) R.drawable.check else R.drawable.close
                                                        ),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        onClick = { onProxyEnabledChange(!proxyEnabled) }
                                    )
                                )
                                if (proxyEnabled) {
                                    add(
                                        Material3SettingsItem(
                                            icon = painterResource(R.drawable.settings),
                                            title = { Text(stringResource(R.string.config_proxy)) },
                                            onClick = { showProxyConfigurationDialog = true }
                                        )
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(27.dp))

                        Material3SettingsGroup(
                            title = stringResource(R.string.lyrics),
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.translate),
                                    title = { Text(stringResource(R.string.lyrics_translation)) },
                                    description = { Text(stringResource(R.string.settings_ai_desc), style = MaterialTheme.typography.bodySmall) },
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = { navController.navigate("settings/ai") }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.lyrics),
                                    title = { Text(stringResource(R.string.lyrics_provider_selection)) },
                                    description = { Text(stringResource(R.string.lyrics_provider_selection_desc)) },
                                    onClick = { showProviderSelectionDialog = true }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.lyrics),
                                    title = { Text(stringResource(R.string.lyrics_provider_priority)) },
                                    description = { Text(stringResource(R.string.lyrics_provider_priority_desc)) },
                                    onClick = { showProviderPriorityDialog = true }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.group_outlined),
                                    title = { Text(stringResource(R.string.respect_agent_positioning)) },
                                    description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                                    trailingContent = {
                                        IrideSwitch(
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
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.skip_next),
                                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                                    trailingContent = {
                                        IrideSwitch(
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
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.arrow_downward),
                                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                                    trailingContent = {
                                        IrideSwitch(
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
                                ),
                            )
                        )

                        Spacer(modifier = Modifier.height(27.dp))

                        Material3SettingsGroup(
                            title = stringResource(R.string.home),
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.trending_up),
                                    title = { Text(stringResource(R.string.top_length)) },
                                    description = { Text(lengthTop) },
                                    onClick = { showTopLengthDialog = true }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.home_outlined),
                                    title = { Text(stringResource(R.string.set_quick_picks)) },
                                    description = {
                                        Text(
                                            when (quickPicks) {
                                                QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                                QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                                            }
                                        )
                                    },
                                    onClick = { showQuickPicksDialog = true }
                                )
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SettingsBackTopBar(
            title = stringResource(R.string.content),
            navController = navController,
            backdrop = frostBackdrop,
            revealProgress = rememberDiscreteProgress(active = contentScrollState.value > 0),
        )
    }
}
