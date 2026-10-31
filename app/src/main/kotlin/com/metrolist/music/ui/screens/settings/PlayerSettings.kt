/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.AudioOffload
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.VideoQuality
import com.metrolist.music.constants.VideoQualityKey
import com.metrolist.music.constants.AutoDownloadOnLikeKey
import com.metrolist.music.constants.AutoLoadMoreKey
import com.metrolist.music.constants.AutoSkipNextOnErrorKey
import com.metrolist.music.constants.CrossfadeDurationKey
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.DisableLoadMoreWhenRepeatAllKey
import com.metrolist.music.constants.HistoryDuration
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PauseOnMute
import com.metrolist.music.constants.PersistentShuffleAcrossQueuesKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PreventDuplicateTracksInQueueKey
import com.metrolist.music.constants.RememberShuffleAndRepeatKey
import com.metrolist.music.constants.ResumeOnBluetoothConnectKey
import com.metrolist.music.constants.ScratchBufferKey
import com.metrolist.music.constants.ScratchBufferSize
import com.metrolist.music.constants.SeekExtraSeconds
import com.metrolist.music.constants.ShufflePlaylistFirstKey
import com.metrolist.music.constants.SkipFadeDurationKey
import com.metrolist.music.constants.SkipFadeKey
import com.metrolist.music.constants.SleepTimerCustomDaysKey
import com.metrolist.music.constants.SleepTimerDayTimesKey
import com.metrolist.music.constants.SleepTimerEnabledKey
import com.metrolist.music.constants.SleepTimerEndTimeKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerRepeatKey
import com.metrolist.music.constants.SleepTimerStartTimeKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.VarispeedKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.ui.component.IrideSlider
import com.metrolist.music.ui.component.IrideSwitch
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SleepTimerDialog
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.decodeDayTimes
import com.metrolist.music.ui.component.encodeDayTimes
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(navController: NavController) {
    val (advancedMode, _) = rememberPreference(AdvancedModeKey, defaultValue = false)

    val (audioQuality, onAudioQualityChange) =
        rememberEnumPreference(
            AudioQualityKey,
            defaultValue = AudioQuality.AUTO,
        )
    val (videoQuality, onVideoQualityChange) =
        rememberEnumPreference(VideoQualityKey, defaultValue = VideoQuality.P720)
    val (crossfadeEnabled, onCrossfadeEnabledChange) =
        rememberPreference(
            CrossfadeEnabledKey,
            defaultValue = false,
        )
    val (crossfadeDuration, onCrossfadeDurationChange) =
        rememberPreference(
            CrossfadeDurationKey,
            defaultValue = 5f,
        )
    val (skipFade, onSkipFadeChange) =
        rememberPreference(
            SkipFadeKey,
            defaultValue = true,
        )
    val (skipFadeDuration, onSkipFadeDurationChange) =
        rememberPreference(
            SkipFadeDurationKey,
            defaultValue = 3.5f,
        )
    val (audioNormalization, onAudioNormalizationChange) =
        rememberPreference(
            AudioNormalizationKey,
            defaultValue = true,
        )
    val (audioOffload, onAudioOffloadChange) =
        rememberPreference(
            key = AudioOffload,
            defaultValue = false,
        )
    val (varispeed, onVarispeedChange) =
        rememberPreference(
            key = VarispeedKey,
            defaultValue = false,
        )
    val (seekExtraSeconds, onSeekExtraSeconds) =
        rememberPreference(
            SeekExtraSeconds,
            defaultValue = false,
        )
    val (autoLoadMore, onAutoLoadMoreChange) =
        rememberPreference(
            AutoLoadMoreKey,
            defaultValue = true,
        )
    val (disableLoadMoreWhenRepeatAll, onDisableLoadMoreWhenRepeatAllChange) =
        rememberPreference(
            DisableLoadMoreWhenRepeatAllKey,
            defaultValue = false,
        )
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) =
        rememberPreference(
            AutoDownloadOnLikeKey,
            defaultValue = false,
        )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) =
        rememberPreference(
            AutoSkipNextOnErrorKey,
            defaultValue = false,
        )
    val (persistentShuffleAcrossQueues, onPersistentShuffleAcrossQueuesChange) =
        rememberPreference(
            PersistentShuffleAcrossQueuesKey,
            defaultValue = false,
        )
    val (rememberShuffleAndRepeat, onRememberShuffleAndRepeatChange) =
        rememberPreference(
            RememberShuffleAndRepeatKey,
            defaultValue = true,
        )
    val (shufflePlaylistFirst, onShufflePlaylistFirstChange) =
        rememberPreference(
            ShufflePlaylistFirstKey,
            defaultValue = false,
        )
    val (preventDuplicateTracksInQueue, onPreventDuplicateTracksInQueueChange) =
        rememberPreference(
            PreventDuplicateTracksInQueueKey,
            defaultValue = true,
        )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) =
        rememberPreference(
            StopMusicOnTaskClearKey,
            defaultValue = false,
        )
    val (pauseOnMute, onPauseOnMuteChange) =
        rememberPreference(
            PauseOnMute,
            defaultValue = false,
        )
    val (resumeOnBluetoothConnect, onResumeOnBluetoothConnectChange) =
        rememberPreference(
            ResumeOnBluetoothConnectKey,
            defaultValue = false,
        )
    val (keepScreenOn, onKeepScreenOnChange) =
        rememberPreference(
            KeepScreenOn,
            defaultValue = false,
        )
    val (historyDuration, onHistoryDurationChange) =
        rememberPreference(
            HistoryDuration,
            defaultValue = 30f,
        )

    val (scratchBuffer, onScratchBufferChange) =
        rememberEnumPreference(
            ScratchBufferKey,
            defaultValue = ScratchBufferSize.MEDIUM,
        )

    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showScratchBufferDialog by remember { mutableStateOf(false) }

    if (showScratchBufferDialog) {
        EnumDialog(
            onDismiss = { showScratchBufferDialog = false },
            onSelect = {
                onScratchBufferChange(it)
                showScratchBufferDialog = false
            },
            title = stringResource(R.string.scratch_buffer),
            current = scratchBuffer,
            values = ScratchBufferSize.values().toList(),
            valueText = { scratchBufferLabel(it) },
            valueDescription = { scratchBufferDescription(it) },
        )
    }

    if (showAudioQualityDialog) {
        EnumDialog(
            onDismiss = { showAudioQualityDialog = false },
            onSelect = {
                onAudioQualityChange(it)
                showAudioQualityDialog = false
            },
            title = stringResource(R.string.audio_quality),
            current = audioQuality,
            values = AudioQuality.values().toList(),
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_highest)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            },
            valueDescription = {
                if (it == AudioQuality.HIGHEST) stringResource(R.string.audio_quality_highest_description) else ""
            },
        )
    }

    if (showVideoQualityDialog) {
        EnumDialog(
            onDismiss = { showVideoQualityDialog = false },
            onSelect = {
                onVideoQualityChange(it)
                showVideoQualityDialog = false
            },
            title = stringResource(R.string.video_quality),
            current = videoQuality,
            values = VideoQuality.entries.toList(),
            valueText = { "${it.maxHeight}p" + if (it == VideoQuality.P2160) " · 4K" else "" },
        )
    }

    val playerScrollState = rememberScrollState()
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
        title = stringResource(R.string.settings_section_player),
        scrollState = playerScrollState,
        navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
        modifier = Modifier.fillMaxSize(),
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
                Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 16.dp)
            ) {
                var showSleepTimerDialog by remember { mutableStateOf(false) }

                val (sleepTimerEnabled, onSleepTimerEnabledChange) =
                    rememberPreference(
                        SleepTimerEnabledKey,
                        defaultValue = false,
                    )
                val (sleepTimerRepeat, onSleepTimerRepeatChange) =
                    rememberPreference(
                        SleepTimerRepeatKey,
                        defaultValue = "daily",
                    )
                val (sleepTimerStartTime, onSleepTimerStartTimeChange) =
                    rememberPreference(
                        SleepTimerStartTimeKey,
                        defaultValue = "22:00",
                    )
                val (sleepTimerEndTime, onSleepTimerEndTimeChange) =
                    rememberPreference(
                        SleepTimerEndTimeKey,
                        defaultValue = "06:00",
                    )
                val (sleepTimerCustomDays, onSleepTimerCustomDaysChange) =
                    rememberPreference(
                        SleepTimerCustomDaysKey,
                        defaultValue = "0,1,2,3,4",
                    )
                val (sleepTimerDayTimes, onSleepTimerDayTimesChange) =
                    rememberPreference(
                        SleepTimerDayTimesKey,
                        defaultValue = "",
                    )
                val (sleepTimerStopAfterCurrentSong, onSleepTimerStopAfterCurrentSongChange) =
                    rememberPreference(
                        SleepTimerStopAfterCurrentSongKey,
                        defaultValue = false,
                    )
                val (sleepTimerFadeOut, onSleepTimerFadeOutChange) =
                    rememberPreference(
                        SleepTimerFadeOutKey,
                        defaultValue = true,
                    )

                if (showSleepTimerDialog) {
                    val customDays = sleepTimerCustomDays.split(",").mapNotNull { it.toIntOrNull() }
                    val dayTimesMap = decodeDayTimes(sleepTimerDayTimes)

                    SleepTimerDialog(
                        isVisible = true,
                        onDismiss = { showSleepTimerDialog = false },
                        onConfirm = { repeat, startTime, endTime, days, dayTimes ->
                            onSleepTimerRepeatChange(repeat)
                            onSleepTimerStartTimeChange(startTime)
                            onSleepTimerEndTimeChange(endTime)
                            onSleepTimerCustomDaysChange(days?.joinToString(",") ?: "0,1,2,3,4")
                            onSleepTimerDayTimesChange(encodeDayTimes(dayTimes))
                            showSleepTimerDialog = false
                        },
                        initialRepeat = sleepTimerRepeat,
                        initialStartTime = sleepTimerStartTime,
                        initialEndTime = sleepTimerEndTime,
                        initialCustomDays = customDays,
                        initialDayTimes = dayTimesMap,
                    )
                }

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_audio),
                    items =
                        buildList {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.graphic_eq),
                                    title = { Text(stringResource(R.string.audio_quality)) },
                                    description = {
                                        Text(
                                            when (audioQuality) {
                                                AudioQuality.AUTO ->
                                                    stringResource(R.string.audio_quality_auto)
                                                AudioQuality.HIGH ->
                                                    stringResource(R.string.audio_quality_high)
                                                AudioQuality.HIGHEST ->
                                                    stringResource(R.string.audio_quality_highest)
                                                AudioQuality.LOW ->
                                                    stringResource(R.string.audio_quality_low)
                                            }
                                        )
                                    },
                                    onClick = { showAudioQualityDialog = true },
                                )
                            )

                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.slow_motion_video),
                                    title = { Text(stringResource(R.string.video_quality)) },
                                    description = {
                                        Text(stringResource(R.string.video_quality_description, videoQuality.maxHeight))
                                    },
                                    onClick = { showVideoQualityDialog = true },
                                )
                            )

                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.equalizer),
                                    title = { Text(stringResource(R.string.equalizer_header)) },
                                    description = { Text(stringResource(R.string.equalizer_desc)) },
                                    onClick = { navController.navigate("settings/equalizer") },
                                )
                            )

                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.album),
                                        title = { Text(stringResource(R.string.scratch_buffer)) },
                                        description = { Text(scratchBufferLabel(scratchBuffer)) },
                                        onClick = { showScratchBufferDialog = true },
                                    )
                                )

                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.linear_scale),
                                    title = { Text(stringResource(R.string.crossfade)) },
                                    description = {
                                        Column {
                                            Text(stringResource(R.string.crossfade_desc))
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = crossfadeEnabled || skipFade,
                                                enter =
                                                    androidx.compose.animation.expandVertically() +
                                                        androidx.compose.animation.fadeIn(),
                                                exit =
                                                    androidx.compose.animation.shrinkVertically() +
                                                        androidx.compose.animation.fadeOut(),
                                            ) {
                                                Column {
                                                    Text(stringResource(R.string.crossfade_duration))
                                                    Text(
                                                        pluralStringResource(
                                                            R.plurals.seconds,
                                                            crossfadeDuration.toInt(),
                                                            crossfadeDuration.toInt(),
                                                        )
                                                    )
                                                    IrideSlider(
                                                        value = crossfadeDuration,
                                                        onValueChange = onCrossfadeDurationChange,
                                                        valueRange = 1f..15f,
                                                        steps = 14,
                                                    )
                                                    Text(stringResource(R.string.skip_fade_duration))
                                                    Text(
                                                        pluralStringResource(
                                                            R.plurals.seconds,
                                                            skipFadeDuration.toInt(),
                                                            skipFadeDuration.toInt(),
                                                        )
                                                    )
                                                    IrideSlider(
                                                        value = skipFadeDuration,
                                                        onValueChange = onSkipFadeDurationChange,
                                                        valueRange = 1f..8f,
                                                        steps = 7,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IrideSwitch(
                                            checked = crossfadeEnabled || skipFade,
                                            onCheckedChange = { enabled ->
                                                onCrossfadeEnabledChange(enabled)
                                                onSkipFadeChange(enabled)
                                            },
                                            thumbContent = {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            if (crossfadeEnabled || skipFade)
                                                                R.drawable.check
                                                            else R.drawable.close
                                                        ),
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            },
                                        )
                                    },
                                    onClick = {
                                        val enabled = !(crossfadeEnabled || skipFade)
                                        onCrossfadeEnabledChange(enabled)
                                        onSkipFadeChange(enabled)
                                    },
                                )
                            )

                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.history),
                                        title = { Text(stringResource(R.string.history_duration)) },
                                        description = {
                                            Column {
                                                Text(historyDuration.roundToInt().toString())
                                                IrideSlider(
                                                    value = historyDuration,
                                                    onValueChange = onHistoryDurationChange,
                                                    valueRange = 1f..100f,
                                                )
                                            }
                                        },
                                    )
                                )
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.volume_up),
                                    title = { Text(stringResource(R.string.audio_normalization)) },
                                    trailingContent = {
                                        IrideSwitch(
                                            checked = audioNormalization,
                                            onCheckedChange = onAudioNormalizationChange,
                                            thumbContent = {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            if (audioNormalization) R.drawable.check
                                                            else R.drawable.close
                                                        ),
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            },
                                        )
                                    },
                                    onClick = { onAudioNormalizationChange(!audioNormalization) },
                                )
                            )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.equalizer),
                                        title = { Text(stringResource(R.string.audio_offload)) },
                                        description = {
                                            Text(
                                                if (crossfadeEnabled)
                                                    stringResource(
                                                        R.string.audio_offload_disabled_by_crossfade
                                                    )
                                                else
                                                    stringResource(
                                                        R.string.audio_offload_description
                                                    )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked =
                                                    if (crossfadeEnabled) false else audioOffload,
                                                onCheckedChange = onAudioOffloadChange,
                                                enabled = !crossfadeEnabled,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (
                                                                    !crossfadeEnabled &&
                                                                        audioOffload
                                                                )
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            if (!crossfadeEnabled)
                                                onAudioOffloadChange(!audioOffload)
                                        },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.tune),
                                        title = { Text(stringResource(R.string.varispeed)) },
                                        description = {
                                            Text(stringResource(R.string.varispeed_description))
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = varispeed,
                                                onCheckedChange = onVarispeedChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (varispeed) R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { onVarispeedChange(!varispeed) },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.navigate_next),
                                        title = {
                                            Text(stringResource(R.string.seek_seconds_addup))
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.seek_seconds_addup_description
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = seekExtraSeconds,
                                                onCheckedChange = onSeekExtraSeconds,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (seekExtraSeconds)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { onSeekExtraSeconds(!seekExtraSeconds) },
                                    )
                                )
                        },
                )

                Spacer(modifier = Modifier.height(27.dp))

                if (advancedMode) {
                    Material3SettingsGroup(
                        title = stringResource(R.string.sleep_timer),
                        items =
                            buildList {
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.time_auto),
                                        title = {
                                            Text(
                                                stringResource(R.string.enable_automatic_sleeptimer)
                                            )
                                        },
                                        description = {
                                            Text(stringResource(R.string.sleeptimer_description))
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = sleepTimerEnabled,
                                                onCheckedChange = onSleepTimerEnabledChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (sleepTimerEnabled)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { onSleepTimerEnabledChange(!sleepTimerEnabled) },
                                    )
                                )
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.baseline_event_repeat_24),
                                        title = {
                                            Text(stringResource(R.string.sleep_timer_repeat))
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.sleep_timer_repeat_description
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = sleepTimerEnabled,
                                                onCheckedChange = { showSleepTimerDialog = true },
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (sleepTimerEnabled)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { showSleepTimerDialog = true },
                                    )
                                )
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.more_time),
                                        title = {
                                            Text(
                                                stringResource(
                                                    R.string
                                                        .sleep_timer_stop_after_current_song_title
                                                )
                                            )
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string
                                                        .sleep_timer_stop_after_current_song_description
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = sleepTimerStopAfterCurrentSong,
                                                onCheckedChange =
                                                    onSleepTimerStopAfterCurrentSongChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (sleepTimerStopAfterCurrentSong)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onSleepTimerStopAfterCurrentSongChange(
                                                !sleepTimerStopAfterCurrentSong
                                            )
                                        },
                                    )
                                )
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.timer_arrow_down),
                                        title = {
                                            Text(
                                                stringResource(R.string.sleep_timer_fade_out_title)
                                            )
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.sleep_timer_fade_out_description
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = sleepTimerFadeOut,
                                                onCheckedChange = onSleepTimerFadeOutChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (sleepTimerFadeOut)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { onSleepTimerFadeOutChange(!sleepTimerFadeOut) },
                                    )
                                )
                            },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AlarmSettingsSection()

                    Spacer(modifier = Modifier.height(27.dp))
                }

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_behavior),
                    items =
                        buildList {
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.playlist_add),
                                        title = { Text(stringResource(R.string.auto_load_more)) },
                                        description = {
                                            Text(stringResource(R.string.auto_load_more_desc))
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = autoLoadMore,
                                                onCheckedChange = onAutoLoadMoreChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (autoLoadMore) R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = { onAutoLoadMoreChange(!autoLoadMore) },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.repeat),
                                        title = {
                                            Text(
                                                stringResource(
                                                    R.string.disable_load_more_when_repeat_all
                                                )
                                            )
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.disable_load_more_when_repeat_all_desc
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = disableLoadMoreWhenRepeatAll,
                                                onCheckedChange =
                                                    onDisableLoadMoreWhenRepeatAllChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (disableLoadMoreWhenRepeatAll)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onDisableLoadMoreWhenRepeatAllChange(
                                                !disableLoadMoreWhenRepeatAll
                                            )
                                        },
                                    )
                                )
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.download),
                                    title = {
                                        Text(stringResource(R.string.auto_download_on_like))
                                    },
                                    description = {
                                        Text(stringResource(R.string.auto_download_on_like_desc))
                                    },
                                    trailingContent = {
                                        IrideSwitch(
                                            checked = autoDownloadOnLike,
                                            onCheckedChange = onAutoDownloadOnLikeChange,
                                            thumbContent = {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            if (autoDownloadOnLike) R.drawable.check
                                                            else R.drawable.close
                                                        ),
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            },
                                        )
                                    },
                                    onClick = { onAutoDownloadOnLikeChange(!autoDownloadOnLike) },
                                )
                            )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.shuffle),
                                        title = {
                                            Text(stringResource(R.string.persistent_shuffle_title))
                                        },
                                        description = {
                                            Text(stringResource(R.string.persistent_shuffle_desc))
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = persistentShuffleAcrossQueues,
                                                onCheckedChange =
                                                    onPersistentShuffleAcrossQueuesChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (persistentShuffleAcrossQueues)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onPersistentShuffleAcrossQueuesChange(
                                                !persistentShuffleAcrossQueues
                                            )
                                        },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.repeat),
                                        title = {
                                            Text(
                                                stringResource(R.string.remember_shuffle_and_repeat)
                                            )
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.remember_shuffle_and_repeat_desc
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = rememberShuffleAndRepeat,
                                                onCheckedChange = onRememberShuffleAndRepeatChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (rememberShuffleAndRepeat)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onRememberShuffleAndRepeatChange(
                                                !rememberShuffleAndRepeat
                                            )
                                        },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.shuffle_on),
                                        title = {
                                            Text(stringResource(R.string.shuffle_playlist_first))
                                        },
                                        description = {
                                            Text(
                                                stringResource(R.string.shuffle_playlist_first_desc)
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = shufflePlaylistFirst,
                                                onCheckedChange = onShufflePlaylistFirstChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (shufflePlaylistFirst)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onShufflePlaylistFirstChange(!shufflePlaylistFirst)
                                        },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.queue_music),
                                        title = {
                                            Text(
                                                stringResource(
                                                    R.string.prevent_duplicate_tracks_in_queue
                                                )
                                            )
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.prevent_duplicate_tracks_in_queue_desc
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = preventDuplicateTracksInQueue,
                                                onCheckedChange =
                                                    onPreventDuplicateTracksInQueueChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (preventDuplicateTracksInQueue)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onPreventDuplicateTracksInQueueChange(
                                                !preventDuplicateTracksInQueue
                                            )
                                        },
                                    )
                                )
                            if (advancedMode)
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.skip_next),
                                        title = {
                                            Text(stringResource(R.string.auto_skip_next_on_error))
                                        },
                                        description = {
                                            Text(
                                                stringResource(
                                                    R.string.auto_skip_next_on_error_desc
                                                )
                                            )
                                        },
                                        trailingContent = {
                                            IrideSwitch(
                                                checked = autoSkipNextOnError,
                                                onCheckedChange = onAutoSkipNextOnErrorChange,
                                                thumbContent = {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (autoSkipNextOnError)
                                                                    R.drawable.check
                                                                else R.drawable.close
                                                            ),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                },
                                            )
                                        },
                                        onClick = {
                                            onAutoSkipNextOnErrorChange(!autoSkipNextOnError)
                                        },
                                    )
                                )
                        },
                )

                Spacer(modifier = Modifier.height(27.dp))

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_system_short),
                    items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.clear_all),
                                title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                                trailingContent = {
                                    IrideSwitch(
                                        checked = stopMusicOnTaskClear,
                                        onCheckedChange = onStopMusicOnTaskClearChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        if (stopMusicOnTaskClear) R.drawable.check
                                                        else R.drawable.close
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) },
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.volume_off_pause),
                                title = {
                                    Text(stringResource(R.string.pause_music_when_media_is_muted))
                                },
                                trailingContent = {
                                    IrideSwitch(
                                        checked = pauseOnMute,
                                        onCheckedChange = onPauseOnMuteChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        if (pauseOnMute) R.drawable.check
                                                        else R.drawable.close
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onPauseOnMuteChange(!pauseOnMute) },
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.bluetooth),
                                title = {
                                    Text(stringResource(R.string.resume_on_bluetooth_connect))
                                },
                                trailingContent = {
                                    IrideSwitch(
                                        checked = resumeOnBluetoothConnect,
                                        onCheckedChange = onResumeOnBluetoothConnectChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        if (resumeOnBluetoothConnect)
                                                            R.drawable.check
                                                        else R.drawable.close
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = {
                                    onResumeOnBluetoothConnectChange(!resumeOnBluetoothConnect)
                                },
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.screenshot),
                                title = {
                                    Text(
                                        stringResource(
                                            R.string.keep_screen_on_when_player_is_expanded
                                        )
                                    )
                                },
                                trailingContent = {
                                    IrideSwitch(
                                        checked = keepScreenOn,
                                        onCheckedChange = onKeepScreenOnChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        if (keepScreenOn) R.drawable.check
                                                        else R.drawable.close
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onKeepScreenOnChange(!keepScreenOn) },
                            ),
                        ),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun scratchBufferLabel(size: ScratchBufferSize): String =
    when (size) {
        ScratchBufferSize.SHORT -> stringResource(R.string.scratch_buffer_short)
        ScratchBufferSize.MEDIUM -> stringResource(R.string.scratch_buffer_medium)
        ScratchBufferSize.LONG -> stringResource(R.string.scratch_buffer_long)
        ScratchBufferSize.FULL -> stringResource(R.string.scratch_buffer_full)
    }

@Composable
private fun scratchBufferDescription(size: ScratchBufferSize): String =
    when (size) {
        ScratchBufferSize.SHORT -> stringResource(R.string.scratch_buffer_short_desc)
        ScratchBufferSize.MEDIUM -> stringResource(R.string.scratch_buffer_medium_desc)
        ScratchBufferSize.LONG -> stringResource(R.string.scratch_buffer_long_desc)
        ScratchBufferSize.FULL -> stringResource(R.string.scratch_buffer_full_desc)
    }
