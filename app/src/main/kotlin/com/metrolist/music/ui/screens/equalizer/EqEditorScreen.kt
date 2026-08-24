package com.metrolist.music.ui.screens.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.eq.data.EQPreset
import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.ui.component.IrideSlider
import com.metrolist.music.ui.component.IrideSwitch
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import kotlin.math.ln
import kotlin.math.pow

private val PillShape = RoundedCornerShape(50)

private val MonoBoldSmall = TextStyle(
    fontFamily = SpaceMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = (-0.1).sp
)

/**
 * Parametric equalizer editor, New Iride UI style: a single flat column, no boxed console.
 * Style labels drive the presets and tapping them again restores their base curve.
 */
@Composable
fun EqEditorScreen(
    navController: NavController,
    viewModel: EqEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showExactGainDialog by remember { mutableStateOf(false) }
    var showSavedDevicesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val bassBoostDb = state.bassBoost.toDouble() * EQPreset.MAX_BASS_BOOST_DB
    // Preamp is an anti-clipping measure applied in audio only; the graph shows the pure EQ shape
    val displayPreampDb = 0.0

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.eq_editor_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = stringResource(R.string.eq_help_title),
                    tint = MaterialTheme.colorScheme.textSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        FlatFeatureRow(
            iconRes = R.drawable.graphic_eq,
            title = stringResource(R.string.eq_master_enable),
            checked = state.enabled,
            onCheckedChange = viewModel::setEnabled
        )

        HairlineDivider()

        OutputDeviceRow(
            outputName = outputDisplayName(state),
            isBluetooth = state.output?.isBluetooth == true,
            savedForDevice = state.savedForCurrentDevice,
            savedForName = state.output?.productName ?: "",
            enabled = state.workingProfileId != null,
            onSaveForDevice = viewModel::saveForDevice,
            onManageDevices = { showSavedDevicesDialog = true }
        )

        HairlineDivider()

        Spacer(Modifier.height(16.dp))
        PresetTabs(
            selectedPreset = state.presetId,
            onSelectPreset = viewModel::applyPreset
        )
        Text(
            text = presetDescriptionText(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )

        EqualizerResponseCurve(
            bands = state.bands,
            bassBoostDb = bassBoostDb,
            preampDb = displayPreampDb,
            selectedIndex = state.selectedBand,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            state.bands.forEachIndexed { index, band ->
                VerticalGainSlider(
                    value = band.gain,
                    label = EqCurve.formatFrequency(band.frequency),
                    selected = state.selectedBand == index,
                    onSelect = { viewModel.selectBand(index) },
                    onValueChange = { viewModel.setBandGain(index, it) },
                    onValueClick = {
                        viewModel.selectBand(index)
                        showExactGainDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HairlineDivider()

        BandTrimControls(
            index = state.selectedBand,
            bands = state.bands,
            onFrequencyChange = viewModel::setBandFrequency,
            onQChange = viewModel::setBandQ,
            onReset = viewModel::resetBand,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(Modifier.height(20.dp))

        FlatFeatureRow(
            iconRes = R.drawable.volume_up,
            title = stringResource(R.string.eq_bass_enhance),
            description = stringResource(R.string.eq_bass_enhance_desc),
            valueText = "${(state.bassBoost * 100).toInt()}%",
            checked = state.bassBoost > 0f,
            onCheckedChange = { on ->
                viewModel.setBassBoost(if (on) 0.35f else 0f)
            },
            expandedContent = {
                IrideSlider(
                    value = state.bassBoost,
                    onValueChange = viewModel::setBassBoost,
                    valueRange = 0f..1f
                )
            }
        )

        FlatFeatureRow(
            iconRes = R.drawable.tune,
            title = stringResource(R.string.eq_transient_title),
            description = stringResource(R.string.eq_transient_desc),
            valueText = "${(state.transientStrength * 100).toInt()}%",
            checked = state.transientStrength > 0f,
            onCheckedChange = { on ->
                viewModel.setTransientStrength(if (on) 0.5f else 0f)
            },
            expandedContent = {
                IrideSlider(
                    value = state.transientStrength,
                    onValueChange = viewModel::setTransientStrength,
                    valueRange = 0f..1f
                )
            }
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showExactGainDialog && state.selectedBand in state.bands.indices) {
        ExactGainDialog(
            bandIndex = state.selectedBand,
            gain = state.bands[state.selectedBand].gain,
            onGainChange = { viewModel.setBandGain(state.selectedBand, it) },
            onReset = { viewModel.resetBand(state.selectedBand) },
            onDismiss = { showExactGainDialog = false }
        )
    }

    if (showSavedDevicesDialog) {
        SavedDevicesDialog(
            bindings = state.bindings,
            onRemove = viewModel::removeDeviceBinding,
            onDismiss = { showSavedDevicesDialog = false }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    SettingsBackTopBar(
        title = stringResource(R.string.equalizer_header),
        navController = navController
    )
}

@Composable
private fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.strokeHairline)
    )
}

@Composable
private fun FlatFeatureRow(
    iconRes: Int,
    title: String,
    description: String? = null,
    valueText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCheckedChange(!checked) }
                .padding(vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    if (valueText != null && checked) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = valueText,
                            style = TextStyle(
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    }
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                }
            }
            IrideSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(
                            if (checked) R.drawable.check else R.drawable.close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
        AnimatedVisibility(
            visible = checked && expandedContent != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 38.dp, end = 4.dp, bottom = 12.dp)) {
                expandedContent?.invoke()
            }
        }
    }
}

@Composable
private fun outputDisplayName(state: EqEditorViewModel.UiState): String {
    val output = state.output ?: return stringResource(R.string.eq_device_unknown)
    return when {
        output.isBluetooth -> output.productName ?: stringResource(R.string.eq_device_unknown)
        output.type == com.metrolist.music.eq.AudioOutput.Type.WIRED ->
            stringResource(R.string.eq_device_wired)
        else -> stringResource(R.string.eq_device_speaker)
    }
}

@Composable
private fun OutputDeviceRow(
    outputName: String,
    isBluetooth: Boolean,
    savedForDevice: Boolean,
    savedForName: String,
    enabled: Boolean,
    onSaveForDevice: () -> Unit,
    onManageDevices: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(if (isBluetooth) R.drawable.bluetooth else R.drawable.volume_up),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.eq_output_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary
            )
            Text(
                text = outputName.ifBlank { stringResource(R.string.eq_device_unknown) },
                style = MonoBoldSmall,
                color = MaterialTheme.colorScheme.textPrimary
            )
            if (savedForDevice) {
                Text(
                    text = stringResource(R.string.eq_saved_for_device, savedForName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textSecondary
                )
            }
        }
        if (!savedForDevice) {
            EqGhostPill(
                text = stringResource(R.string.eq_save_for_device),
                onClick = onSaveForDevice,
                enabled = enabled
            )
        } else {
            IconButton(onClick = onManageDevices) {
                Icon(
                    painter = painterResource(R.drawable.link),
                    contentDescription = stringResource(R.string.eq_manage_devices),
                    tint = MaterialTheme.colorScheme.textPrimary
                )
            }
        }
    }
}

/**
 * Preset selector in the "Your Mood" style: single row of close underlined labels,
 * no boxes. Selected label is full opacity with a solid underline; the rest are dimmed.
 */
@Composable
private fun PresetTabs(
    selectedPreset: String,
    onSelectPreset: (String) -> Unit
) {
    val density = LocalDensity.current
    val presets = listOf(
        EQPreset.PRESET_STANDARD to R.string.eq_preset_standard,
        EQPreset.PRESET_BALANCED to R.string.eq_preset_balanced,
        EQPreset.PRESET_MORE_BASS to R.string.eq_preset_more_bass,
        EQPreset.PRESET_MORE_TREBLE to R.string.eq_preset_more_treble,
        EQPreset.PRESET_VOICE to R.string.eq_preset_voice,
        EQPreset.PRESET_CUSTOM to R.string.eq_preset_custom
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        presets.forEachIndexed { index, (presetId, labelRes) ->
            val selected = selectedPreset == presetId
            var textWidthPx by remember(presetId) { mutableStateOf(0) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectPreset(presetId) }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.4f),
                    modifier = Modifier.onSizeChanged { textWidthPx = it.width }
                )
                Spacer(Modifier.height(3.dp))
                // Underline exactly as wide as the label, like ChipsRow's iride style
                Box(
                    modifier = Modifier
                        .width(with(density) { textWidthPx.toDp() })
                        .height(2.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
            if (index != presets.lastIndex) Spacer(Modifier.width(14.dp))
        }
    }
}

@Composable
private fun presetDescriptionText(state: EqEditorViewModel.UiState): String {
    val base = stringResource(
        when (state.presetId) {
            EQPreset.PRESET_BALANCED -> R.string.eq_desc_balanced
            EQPreset.PRESET_MORE_BASS -> R.string.eq_desc_more_bass
            EQPreset.PRESET_MORE_TREBLE -> R.string.eq_desc_more_treble
            EQPreset.PRESET_VOICE -> R.string.eq_desc_voice
            EQPreset.PRESET_CUSTOM -> R.string.eq_desc_custom
            else -> R.string.eq_desc_standard
        }
    )
    return if (state.modified) "$base · ${stringResource(R.string.eq_preset_modified)}" else base
}

@Composable
private fun BandTrimControls(
    index: Int,
    bands: List<ParametricEQBand>,
    onFrequencyChange: (Int, Double) -> Unit,
    onQChange: (Int, Double) -> Unit,
    onReset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (index !in bands.indices) return
    val band = bands[index]
    val bounds = EQPreset.frequencyBounds(index)

    val freqFraction =
        (ln(band.frequency / bounds.start) / ln(bounds.endInclusive / bounds.start))
            .toFloat().coerceIn(0f, 1f)

    val qFraction = ((ln(band.q) - ln(EQPreset.MIN_Q)) /
            (ln(EQPreset.MAX_Q) - ln(EQPreset.MIN_Q))).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.eq_selected_band_label,
                    index + 1,
                    EqCurve.formatFrequency(band.frequency),
                    "%.2f".format(band.q)
                ),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.eq_reset_band).uppercase(),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onReset(index) }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
        }

        FlatSliderRow(
            label = stringResource(R.string.eq_frequency),
            valueText = EqCurve.formatFrequency(band.frequency),
            sliderValue = freqFraction,
            onSliderChange = { fraction ->
                val frequency = bounds.start *
                        (bounds.endInclusive / bounds.start).pow(fraction.toDouble())
                onFrequencyChange(index, frequency)
            }
        )
        Spacer(Modifier.height(6.dp))

        FlatSliderRow(
            label = stringResource(R.string.eq_q_factor),
            valueText = "%.2f".format(band.q),
            sliderValue = qFraction,
            onSliderChange = { fraction ->
                val q = EQPreset.MIN_Q *
                        (EQPreset.MAX_Q / EQPreset.MIN_Q).pow(fraction.toDouble())
                onQChange(index, q)
            }
        )
    }
}

@Composable
private fun FlatSliderRow(
    label: String,
    valueText: String,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.textSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = valueText,
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.textPrimary
            )
        }
        IrideSlider(
            value = sliderValue,
            onValueChange = onSliderChange,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqGhostPill(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        shape = PillShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.textPrimary.copy(alpha = if (enabled) 0.85f else 0.35f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Exact-value editor styled like the rest of the New Iride UI: flat panel,
 * hairline border, monospace bold title, IrideSlider.
 */
@Composable
private fun ExactGainDialog(
    bandIndex: Int,
    gain: Double,
    onGainChange: (Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.eq_exact_gain_title),
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Text(
                    text = EqCurve.formatGain(gain),
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 6.dp)
                )
                IrideSlider(
                    value = gain.toFloat(),
                    onValueChange = { onGainChange(it.toDouble()) },
                    valueRange = (-EQPreset.MAX_GAIN_DB).toFloat()..EQPreset.MAX_GAIN_DB.toFloat(),
                    steps = ((EQPreset.MAX_GAIN_DB * 2) / EQPreset.GAIN_STEP).toInt() - 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    EqGhostPill(text = "-", onClick = { onGainChange(gain - EQPreset.GAIN_STEP) })
                    Spacer(Modifier.weight(1f))
                    EqGhostPill(text = stringResource(R.string.eq_reset_band), onClick = onReset)
                    Spacer(Modifier.weight(1f))
                    EqGhostPill(text = "+", onClick = { onGainChange(gain + EQPreset.GAIN_STEP) })
                }
            }
        }
    }
}

@Composable
private fun SavedDevicesDialog(
    bindings: Map<String, String>,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = stringResource(R.string.eq_manage_devices),
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Spacer(Modifier.height(14.dp))
                if (bindings.isEmpty()) {
                    Text(
                        text = stringResource(R.string.eq_no_saved_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                } else {
                    bindings.forEach { (deviceKey, _) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.bluetooth),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = deviceKey.removePrefix("bt|").ifBlank {
                                    stringResource(R.string.eq_device_unknown)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemove(deviceKey) }) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.eq_unbind_device),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EqGhostPill(
                        text = stringResource(android.R.string.ok),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = stringResource(R.string.eq_help_title),
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Spacer(Modifier.height(14.dp))
                HelpBullet(stringResource(R.string.eq_help_drag))
                HelpBullet(stringResource(R.string.eq_help_exact))
                HelpBullet(stringResource(R.string.eq_help_q))
                HelpBullet(stringResource(R.string.eq_help_reset_style))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EqGhostPill(
                        text = stringResource(android.R.string.ok),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.9f)
        )
    }
}
