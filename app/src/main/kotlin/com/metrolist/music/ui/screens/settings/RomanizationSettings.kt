/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings
import com.metrolist.music.ui.component.IrideSwitch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.LyricsRomanizeAsMainKey
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.constants.LyricsRomanizeList
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.lyrics.JapaneseDictManager
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val defaultList = mutableListOf(
    "Japanese" to true,
    "Korean" to true,
    "Chinese" to true,
    "Hindi" to true,
    "Punjabi" to true,
    "Russian" to true,
    "Ukrainian" to true,
    "Serbian" to true,
    "Bulgarian" to true,
    "Belarusian" to true,
    "Kyrgyz" to true,
    "Macedonian" to true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationSettings(
    navController: NavController
) {
    val (pref, prefValue) = rememberPreference(LyricsRomanizeList, "")

    val initialList = remember(pref) {
        if (pref.isEmpty()) defaultList
        else {
            val savedMap = pref.split(",").associate { entry ->
                val (lang, checked) = entry.split(":")
                lang to checked.toBoolean()
            }

            defaultList.map { (lang, defaultChecked) ->
                Pair(lang, savedMap[lang] ?: defaultChecked)
            }
        }
    }

    val states = remember(initialList) { mutableStateListOf(*initialList.toTypedArray()) }

    val parentState = when {
        states.all { it.component2() } -> ToggleableState.On
        states.none { it.component2() } -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    val (lyricsRomanizeAsMain, onLyricsRomanizeAsMainChange) = rememberPreference(
        LyricsRomanizeAsMainKey,
        defaultValue = false
    )

    val (lyricsRomanizeCyrillicByLine, onLyricsRomanizeCyrillicByLineChange) = rememberPreference(
        LyricsRomanizeCyrillicByLineKey,
        defaultValue = false
    )

    val checkboxesList: MutableList<Material3SettingsItem> = mutableListOf()

    var showDictDialog by rememberSaveable { mutableStateOf(false) }
    var dictDownloading by rememberSaveable { mutableStateOf(false) }
    var dictDownloadProgress by remember { mutableIntStateOf(0) }
    var dictDownloadFailed by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
        Material3SettingsGroup(
            title = stringResource(R.string.options),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.lyrics_romanize_as_main)) },
                    trailingContent = {
                        IrideSwitch(
                            checked = lyricsRomanizeAsMain,
                            onCheckedChange = onLyricsRomanizeAsMainChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeAsMain) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        )
                    },
                    icon = painterResource(R.drawable.queue_music)
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.line_by_line_option_title)) },
                    trailingContent = {
                        IrideSwitch(
                            checked = lyricsRomanizeCyrillicByLine,
                            onCheckedChange = onLyricsRomanizeCyrillicByLineChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeCyrillicByLine) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        )
                    },
                    icon = painterResource(R.drawable.info)
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        checkboxesList += Material3SettingsItem(
            title = { Text("Play all") },
            trailingContent = {
                TriStateCheckbox(
                    state = parentState,
                    onClick = {
                        val newState = parentState != ToggleableState.On
                        var needsDictionary = false
                        states.forEachIndexed { index, (language, _) ->
                            val value =
                                if (language == "Japanese" && newState && !JapaneseDictManager.isDownloaded()) {
                                    needsDictionary = true
                                    false
                                } else {
                                    newState
                                }
                            states[index] = Pair(language, value)
                        }
                        prefValue(states.joinToString(",") { (lang, c) -> "$lang:$c" })
                        if (needsDictionary) showDictDialog = true
                    }
                )
            },
            icon = painterResource(R.drawable.info)
        )

        states.forEachIndexed { index, (language, checked) ->
            checkboxesList += Material3SettingsItem(
                title = { Text(language) },
                trailingContent = {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            if (language == "Japanese" && isChecked && !JapaneseDictManager.isDownloaded()) {
                                showDictDialog = true
                            } else {
                                states[index] = Pair(language, isChecked)
                                prefValue(states.joinToString(",") { (lang, c) -> "$lang:$c" })
                            }
                        }
                    )
                },
                icon = painterResource(R.drawable.language)
            )
        }

        Material3SettingsGroup(
            title = stringResource(R.string.content_language),
            items = checkboxesList
        )
    }
        }

        SettingsBackTopBar(
            title = stringResource(R.string.lyrics_romanize_title),
            navController = navController,
            backdrop = frostBackdrop,
            revealProgress = rememberDiscreteProgress(active = scrollState.value > 0),
        )
    }

    if (showDictDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!dictDownloading) {
                    showDictDialog = false
                    dictDownloadFailed = false
                }
            },
            title = { Text(stringResource(R.string.lyrics_romanize_japanese_dict_title)) },
            text = {
                Column {
                    when {
                        dictDownloadFailed ->
                            Text(stringResource(R.string.lyrics_romanize_japanese_dict_failed))
                        dictDownloading -> {
                            Text(
                                stringResource(
                                    R.string.lyrics_romanize_japanese_dict_downloading,
                                    dictDownloadProgress
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { dictDownloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else ->
                            Text(stringResource(R.string.lyrics_romanize_japanese_dict_message))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !dictDownloading,
                    onClick = {
                        dictDownloading = true
                        dictDownloadFailed = false
                        dictDownloadProgress = 0
                        scope.launch {
                            val result = JapaneseDictManager.download { progress ->
                                dictDownloadProgress = progress.coerceIn(0, 100)
                            }
                            dictDownloading = false
                            result
                                .onSuccess {
                                    showDictDialog = false
                                    val japaneseIndex =
                                        states.indexOfFirst { it.first == "Japanese" }
                                    if (japaneseIndex >= 0) {
                                        states[japaneseIndex] = Pair("Japanese", true)
                                        prefValue(states.joinToString(",") { (lang, c) -> "$lang:$c" })
                                    }
                                }
                                .onFailure {
                                    dictDownloadFailed = true
                                }
                        }
                    }
                ) {
                    Text(stringResource(R.string.lyrics_romanize_japanese_dict_download))
                }
            },
            dismissButton = {
                TextButton(enabled = !dictDownloading, onClick = {
                    showDictDialog = false
                    dictDownloadFailed = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}