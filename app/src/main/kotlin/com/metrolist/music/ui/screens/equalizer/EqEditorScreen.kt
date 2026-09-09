package com.metrolist.music.ui.screens.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.eq.data.EQPreset
import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.IrideSlider
import com.metrolist.music.ui.component.IrideSwitch
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(22.dp)
private val PillShape = RoundedCornerShape(50)
private val SavedGreen = Color(0xFF2ECC71)
private val FlashRed = Color(0xFFE74C3C)

/** Cycle order of the tappable device-type icon: headphones → earbuds → speaker. */
private val IconCycle = listOf("headphones", "earbuds", "speaker")

private const val RED_FLASH_MS = 200L

@Composable
fun EqEditorScreen(
    navController: NavController,
    viewModel: EqEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showExactGainDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var exactGainDeviceKey by remember { mutableStateOf<String?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by remember(playerConnection) { playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null) }.collectAsStateWithLifecycle()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT)
    val scrollState = rememberScrollState()
    val frostBackdrop = rememberFrostBackdrop()
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    val currentKey = state.output?.deviceKey ?: "speaker"
    val globalOwnerKey = state.globalProfileId?.let { gid -> state.bindings.entries.firstOrNull { it.value == gid }?.key ?: currentKey }
    // Show the global card first when one is configured; otherwise show the current device.
    val orderedKeys = buildList {
        if (state.globalProfileId != null) {
            // Card ordering: "general" placeholder, then each bound device (current first),
            // then the global owner's own entry. Keeps visual order: general on top, then devices.
        }
        if (globalOwnerKey != null) add(globalOwnerKey)
        addAll(state.bindings.keys)
        if (!contains(currentKey)) add(currentKey)
    }.distinct()

    Box(modifier = Modifier.fillMaxSize().recordFrostBackdrop(frostBackdrop)) {
        if (mainTopGradient) {
            TopScreenGradientBackground(mediaMetadata = mediaMetadata, playerBackground = playerBackgroundStyle)
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        Column(
            Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .graphicsLayer { alpha = screenProgress }
        ) {
            Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
            Spacer(Modifier.height(4.dp))

            orderedKeys.forEachIndexed { index, deviceKey ->
                key(deviceKey) {
                    val isGeneral = deviceKey == globalOwnerKey && state.globalProfileId != null
                    val working = state.working[deviceKey] ?: return@forEachIndexed
                    DeviceEqCard(
                        deviceKey = deviceKey,
                        isGeneral = isGeneral,
                        isConnected = deviceKey == currentKey,
                        showGlobalRow = state.globalProfileId == null || isGeneral,
                        isGlobalOwner = isGeneral,
                        iconRes = iconResForKey(state, deviceKey),
                        working = working,
                        showGeneralHint = isGeneral,
                        entranceIndex = index,
                        onIconClick = {
                            val iconKey = state.deviceIcons[deviceKey] ?: defaultIconKey(deviceKey)
                            viewModel.setDeviceIcon(deviceKey, nextIconKey(iconKey))
                        },
                        onBassStepChange = { viewModel.setBassBoostStepped(deviceKey, it) },
                        onPresetSelect = { viewModel.applyPreset(deviceKey, it) },
                        onGlobalToggle = { viewModel.toggleGlobalForCurrent(deviceKey, it) },
                        onBandGain = { idx, v -> viewModel.setBandGain(deviceKey, idx, v) },
                        onBandFrequency = { idx, f -> viewModel.setBandFrequency(deviceKey, idx, f) },
                        onBandQ = { idx, q -> viewModel.setBandQ(deviceKey, idx, q) },
                        onSelectBand = { idx -> viewModel.selectBand(deviceKey, idx) },
                        onResetBand = { idx -> viewModel.resetBand(deviceKey, idx) },
                        onExactGain = { exactGainDeviceKey = deviceKey; showExactGainDialog = true },
                        onDeleteRequest = { showDeleteDialog = deviceKey },
                        viewModel = viewModel,
                        state = state,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExactGainDialog) {
        val key = exactGainDeviceKey
        val wp = if (key != null) state.working[key] else null
        if (wp != null) {
            ExactGainDialog(
                bandIndex = wp.selectedBand,
                gain = wp.bands[wp.selectedBand].gain,
                onGainChange = { v -> key?.let { viewModel.setBandGain(it, wp.selectedBand, v) } },
                onReset = { key?.let { viewModel.resetBand(it, wp.selectedBand) } },
                onDismiss = { showExactGainDialog = false }
            )
        } else showExactGainDialog = false
    }
    if (showDeleteDialog != null) {
        DeleteConfirmDialog(
            deviceName = displayNameForCard(state, showDeleteDialog!!),
            onConfirm = { viewModel.removeDeviceBinding(showDeleteDialog!!); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null }
        )
    }

    SettingsBackTopBar(title = stringResource(R.string.equalizer_header), navController = navController, backdrop = frostBackdrop, revealProgress = rememberDiscreteProgress(active = scrollState.value > 0))
}

private fun nextIconKey(current: String): String {
    val index = IconCycle.indexOf(current).coerceAtLeast(0)
    return IconCycle[(index + 1) % IconCycle.size]
}

private fun defaultIconKey(deviceKey: String): String = if (deviceKey == "speaker") "speaker" else "headphones"

private fun iconResForKey(state: EqEditorViewModel.UiState, key: String): Int = when (state.deviceIcons[key] ?: defaultIconKey(key)) {
    "earbuds" -> R.drawable.earbuds
    "speaker" -> R.drawable.speaker
    else -> R.drawable.headphones
}

private fun displayNameForCard(state: EqEditorViewModel.UiState, key: String): String {
    if (key == state.output?.deviceKey) {
        val o = state.output
        if (o != null) {
            if (o.isBluetooth && !o.productName.isNullOrBlank()) return o.productName!!.trim()
            if (o.type == com.metrolist.music.eq.AudioOutput.Type.WIRED) return "Wired"
        }
        return "Speaker"
    }
    state.deviceNames[key]?.takeIf { it.isNotBlank() }?.let { return it }
    return key.removePrefix("bt|").ifBlank { "Audio" }.replaceFirstChar { it.uppercase() }
}

private fun bassStepFromValue(v: Float): Int = when {
    v <= 0.05f -> 0
    v <= 0.35f -> 1
    v <= 0.65f -> 2
    else -> 3
}

/**
 * One device card. The title sits above the row split: left half is the identity (icon +
 * name, read-only), right half is the regulation rows (apply-to-all, bass boost fader, preset
 * dropdown). When the device is not the currently connected output, the regulation rows
 * collapse into a single "Non connesso" message. Long-press anywhere on the card removes
 * the profile.
 */
@Composable
private fun DeviceEqCard(
    deviceKey: String,
    isGeneral: Boolean,
    isConnected: Boolean,
    showGlobalRow: Boolean,
    isGlobalOwner: Boolean,
    iconRes: Int,
    working: EqEditorViewModel.WorkingProfile,
    showGeneralHint: Boolean,
    entranceIndex: Int,
    onIconClick: () -> Unit,
    onBassStepChange: (Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onGlobalToggle: (Boolean) -> Unit,
    onBandGain: (Int, Double) -> Unit,
    onBandFrequency: (Int, Double) -> Unit,
    onBandQ: (Int, Double) -> Unit,
    onSelectBand: (Int) -> Unit,
    onResetBand: (Int) -> Unit,
    onExactGain: () -> Unit,
    onDeleteRequest: () -> Unit,
    viewModel: EqEditorViewModel,
    state: EqEditorViewModel.UiState,
    modifier: Modifier = Modifier
) {
    val title = if (isGeneral) stringResource(R.string.eq_general_title) else stringResource(R.string.eq_specific_title, displayNameForCard(state, deviceKey))
    var presetMenuExpanded by remember { mutableStateOf(false) }
    val expanded = working.presetId == EQPreset.PRESET_CUSTOM && isConnected
    val entrance = rememberEnterProgress(
        play = true,
        delayMillis = entranceIndex * IrideMotion.StaggerStep,
        durationMillis = IrideMotion.Short,
        easing = IrideMotion.EaseOutQuart
    )
    val tickKey = remember(working.bands, working.bassBoost, working.presetId) { Any() }
    var flashing by remember { mutableStateOf(false) }
    var firstTick by remember(deviceKey) { mutableStateOf(true) }
    LaunchedEffect(tickKey) {
        if (firstTick) {
            firstTick = false
            return@LaunchedEffect
        }
        flashing = true
        delay(RED_FLASH_MS)
        flashing = false
    }
    val cardAlpha = if (!isConnected && !isGeneral) 0.55f else 1f

    Surface(
        shape = CardShape,
        color = if (isGeneral) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (isConnected) MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.strokeHairline
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isGeneral) 200.dp else 196.dp)
            .graphicsLayer { alpha = cardAlpha }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onDeleteRequest
            )
            .animateContentSize(tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart))
            .irideEnter(entrance, 10.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            CardHeader(title = title, isConnected = isConnected, isGeneral = isGeneral, flashing = flashing, state = state, working = working)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    DeviceIdentity(
                        iconRes = iconRes,
                        onIconClick = onIconClick,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isConnected && !isGeneral) {
                        DisconnectedState(
                            modifier = Modifier.weight(1.6f).padding(start = 18.dp)
                        )
                    } else {
                        DeviceControls(
                            showGlobalRow = showGlobalRow,
                            isGeneral = isGeneral,
                            isGlobalOwner = isGlobalOwner,
                            bassBoostStep = bassStepFromValue(working.bassBoost),
                            presetId = working.presetId,
                            presetMenuExpanded = presetMenuExpanded,
                            onPresetMenuToggle = { presetMenuExpanded = !presetMenuExpanded },
                            onPresetMenuDismiss = { presetMenuExpanded = false },
                            onBassStepChange = onBassStepChange,
                            onPresetSelect = onPresetSelect,
                            onGlobalToggle = onGlobalToggle,
                            modifier = Modifier.weight(1.6f).padding(start = 18.dp, end = 4.dp)
                        )
                    }
                }
                StatusDot(
                    isConnected = isConnected,
                    flashing = flashing,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp, top = 2.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(IrideMotion.Quick)) + fadeOut(tween(IrideMotion.Quick))
            ) {
                CustomPanel(
                    working = working,
                    onBandGain = onBandGain,
                    onBandFrequency = onBandFrequency,
                    onBandQ = onBandQ,
                    onSelectBand = onSelectBand,
                    onResetBand = onResetBand,
                    onResetToBase = { onPresetSelect(EQPreset.PRESET_STANDARD) },
                    onExactGain = onExactGain
                )
            }
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    isConnected: Boolean,
    isGeneral: Boolean,
    flashing: Boolean,
    state: EqEditorViewModel.UiState,
    working: EqEditorViewModel.WorkingProfile
) {
    val saved = !working.modified
    val statusText = when {
        isGeneral -> stringResource(R.string.eq_header_general)
        isConnected -> if (saved) stringResource(R.string.eq_header_saved) else stringResource(R.string.eq_header_modified)
        else -> stringResource(R.string.eq_header_offline)
    }
    val statusColor by animateColorAsState(
        targetValue = when {
            !isConnected && !isGeneral -> MaterialTheme.colorScheme.textSecondary.copy(alpha = 0.7f)
            working.modified -> MaterialTheme.colorScheme.textSecondary
            else -> MaterialTheme.colorScheme.textSecondary
        },
        animationSpec = tween(RED_FLASH_MS.toInt()),
        label = "cardHeaderStatus"
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = (-0.1).sp),
            color = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = statusText,
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.4.sp),
            color = statusColor
        )
    }
}

/** Left half: only the device-type icon. The name is the card title up top; no click here. */
@Composable
private fun DeviceIdentity(iconRes: Int, onIconClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIconClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Right half when the device is the currently connected output: the three regulation rows. */
@Composable
private fun DeviceControls(
    showGlobalRow: Boolean,
    isGeneral: Boolean,
    isGlobalOwner: Boolean,
    bassBoostStep: Int,
    presetId: String,
    presetMenuExpanded: Boolean,
    onPresetMenuToggle: () -> Unit,
    onPresetMenuDismiss: () -> Unit,
    onBassStepChange: (Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onGlobalToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = showGlobalRow,
            enter = expandVertically(tween(IrideMotion.Quick, easing = IrideMotion.EaseOutQuart)) + fadeIn(tween(IrideMotion.Quick)),
            exit = shrinkVertically(tween(IrideMotion.Quick)) + fadeOut(tween(IrideMotion.Quick))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Text(
                    text = stringResource(if (isGeneral) R.string.eq_use_for_all_devices else R.string.eq_use_for_all_devices),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                IrideSwitch(checked = isGlobalOwner, onCheckedChange = onGlobalToggle)
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.eq_bass_boost),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.textSecondary
            )
            Spacer(Modifier.height(6.dp))
            SteppedFader(
                step = bassBoostStep,
                onStepChange = onBassStepChange,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Text(text = stringResource(R.string.eq_bass_off), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 8.sp), color = MaterialTheme.colorScheme.textSecondary.copy(alpha = 0.65f))
                Text(text = stringResource(R.string.eq_bass_punch), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 8.sp), color = MaterialTheme.colorScheme.textSecondary.copy(alpha = 0.65f))
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(Modifier.fillMaxWidth()) {
            Surface(
                shape = PillShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline),
                onClick = onPresetMenuToggle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.eq_profile_label),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = presetDisplayName(presetId, false),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            DropdownMenu(expanded = presetMenuExpanded, onDismissRequest = onPresetMenuDismiss) {
                presetMenuItems().forEach { (id, labelRes) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(labelRes),
                                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                        },
                        onClick = { onPresetMenuDismiss(); onPresetSelect(id) }
                    )
                }
            }
        }
    }
}

/** Right half when the device is not connected: a single "Non connesso" line. */
@Composable
private fun DisconnectedState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.eq_not_connected),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = (-0.1).sp),
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SteppedFader(step: Int, onStepChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val accent = MaterialTheme.colorScheme.textPrimary
    val handleWidth = 16.dp
    val handleHeight = 4.dp
    fun nearestStep(x: Float, width: Float): Int {
        val pad = with(density) { (handleWidth / 2 + 2.dp).toPx() }
        val fraction = ((x - pad) / (width - 2 * pad)).coerceIn(0f, 1f)
        return (fraction * 3).roundToInt().coerceIn(0, 3)
    }
    Box(
        modifier = modifier
            .height(26.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var claimed = false
                    var lastX = down.position.x
                    var totalDx = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) { if (claimed) change.consume(); break }
                        if (!claimed && change.isConsumed) break
                        totalDx += change.position.x - lastX
                        lastX = change.position.x
                        if (!claimed && abs(totalDx) > viewConfiguration.touchSlop) claimed = true
                        if (claimed) { change.consume(); onStepChange(nearestStep(lastX, size.width.toFloat())) }
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onStepChange(nearestStep(it.x, size.width.toFloat())) }) }
    ) {
        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
            val y = size.height / 2f
            val pad = (handleWidth / 2 + 2.dp).toPx()
            val end = size.width - pad
            fun xFor(s: Int): Float = pad + (end - pad) * s / 3f
            drawLine(color = accent.copy(alpha = 0.22f), start = Offset(pad, y), end = Offset(end, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            if (step > 0) {
                drawLine(color = accent.copy(alpha = 0.5f), start = Offset(pad, y), end = Offset(xFor(step), y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            for (s in 0..3) {
                drawLine(color = accent.copy(alpha = if (s <= step) 0.55f else 0.28f), start = Offset(xFor(s), y - 5.dp.toPx()), end = Offset(xFor(s), y + 5.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }
            drawRoundRect(color = accent.copy(alpha = 0.85f), topLeft = Offset(xFor(step) - handleWidth.toPx() / 2f, y - handleHeight.toPx() / 2f), size = Size(handleWidth.toPx(), handleHeight.toPx()), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
        }
    }
}

@Composable
private fun StatusDot(isConnected: Boolean, flashing: Boolean, modifier: Modifier = Modifier) {
    val fill by animateColorAsState(
        targetValue = when {
            flashing -> FlashRed
            isConnected -> SavedGreen
            else -> Color.Transparent
        },
        animationSpec = tween(RED_FLASH_MS.toInt()),
        label = "eqDotFill"
    )
    val ring by animateColorAsState(
        targetValue = if (isConnected || flashing) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        animationSpec = tween(RED_FLASH_MS.toInt()),
        label = "eqDotRing"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, ring, CircleShape)
    )
}

@Composable
private fun CustomPanel(
    working: EqEditorViewModel.WorkingProfile,
    onBandGain: (Int, Double) -> Unit,
    onBandFrequency: (Int, Double) -> Unit,
    onBandQ: (Int, Double) -> Unit,
    onSelectBand: (Int) -> Unit,
    onResetBand: (Int) -> Unit,
    onResetToBase: () -> Unit,
    onExactGain: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        HairlineDivider(Modifier.padding(bottom = 14.dp))
        EqualizerResponseCurve(
            bands = working.bands,
            bassBoostDb = working.bassBoost.toDouble() * EQPreset.MAX_BASS_BOOST_DB,
            preampDb = 0.0,
            selectedIndex = working.selectedBand,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            working.bands.forEachIndexed { index, band ->
                VerticalGainSlider(
                    value = band.gain,
                    label = EqCurve.formatFrequency(band.frequency),
                    selected = working.selectedBand == index,
                    onSelect = { onSelectBand(index) },
                    onValueChange = { onBandGain(index, it) },
                    onValueClick = { onSelectBand(index); onExactGain() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        BandTrimControls(
            index = working.selectedBand,
            bands = working.bands,
            onFrequencyChange = onBandFrequency,
            onQChange = onBandQ,
            onReset = onResetBand,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = PillShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline),
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onResetToBase() }
        ) {
            Text(
                text = stringResource(R.string.eq_reset_to_base),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun presetDisplayName(presetId: String, isMatched: Boolean): String {
    val res = when (presetId) {
        EQPreset.PRESET_STANDARD -> if (isMatched) R.string.eq_matched_profile else R.string.eq_preset_standard
        EQPreset.PRESET_BALANCED -> R.string.eq_preset_balanced
        EQPreset.PRESET_MORE_BASS -> R.string.eq_preset_more_bass
        EQPreset.PRESET_MORE_TREBLE -> R.string.eq_preset_more_treble
        EQPreset.PRESET_VOICE -> R.string.eq_preset_voice
        else -> R.string.eq_preset_custom
    }
    return stringResource(res)
}

private fun presetMenuItems(): List<Pair<String, Int>> = listOf(
    EQPreset.PRESET_STANDARD to R.string.eq_preset_standard,
    EQPreset.PRESET_BALANCED to R.string.eq_preset_balanced,
    EQPreset.PRESET_MORE_BASS to R.string.eq_preset_more_bass,
    EQPreset.PRESET_MORE_TREBLE to R.string.eq_preset_more_treble,
    EQPreset.PRESET_VOICE to R.string.eq_preset_voice,
    EQPreset.PRESET_CUSTOM to R.string.eq_preset_custom
)

@Composable
private fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.strokeHairline))
}

@Composable
private fun BandTrimControls(index: Int, bands: List<ParametricEQBand>, onFrequencyChange: (Int, Double) -> Unit, onQChange: (Int, Double) -> Unit, onReset: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (index !in bands.indices) return
    val band = bands[index]
    val bounds = EQPreset.frequencyBounds(index)
    val freqFraction = (ln(band.frequency / bounds.start) / ln(bounds.endInclusive / bounds.start)).toFloat().coerceIn(0f, 1f)
    val qFraction = ((ln(band.q) - ln(EQPreset.MIN_Q)) / (ln(EQPreset.MAX_Q) - ln(EQPreset.MIN_Q))).toFloat().coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.eq_selected_band_label, index + 1, EqCurve.formatFrequency(band.frequency), "%.2f".format(band.q)), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.eq_reset_band).uppercase(), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = MaterialTheme.colorScheme.textSecondary, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onReset(index) }.padding(vertical = 6.dp, horizontal = 4.dp))
        }
        FlatSliderRow(label = stringResource(R.string.eq_frequency), valueText = EqCurve.formatFrequency(band.frequency), sliderValue = freqFraction, onSliderChange = { fraction -> val frequency = bounds.start * (bounds.endInclusive / bounds.start).pow(fraction.toDouble()); onFrequencyChange(index, frequency) })
        Spacer(Modifier.height(6.dp))
        FlatSliderRow(label = stringResource(R.string.eq_q_factor), valueText = "%.2f".format(band.q), sliderValue = qFraction, onSliderChange = { fraction -> val q = EQPreset.MIN_Q * (EQPreset.MAX_Q / EQPreset.MIN_Q).pow(fraction.toDouble()); onQChange(index, q) })
    }
}

@Composable
private fun FlatSliderRow(label: String, valueText: String, sliderValue: Float, onSliderChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = MaterialTheme.colorScheme.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(text = valueText, style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = MaterialTheme.colorScheme.textPrimary)
        }
        IrideSlider(value = sliderValue, onValueChange = onSliderChange, modifier = Modifier.padding(top = 2.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqGhostPill(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline), onClick = onClick, enabled = enabled) {
        Text(text = text, style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = MaterialTheme.colorScheme.textPrimary.copy(alpha = if (enabled) 0.85f else 0.35f), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun ExactGainDialog(bandIndex: Int, gain: Double, onGainChange: (Double) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.eq_exact_gain_title), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp), color = MaterialTheme.colorScheme.textPrimary)
                Text(text = EqCurve.formatGain(gain), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.textPrimary, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp))
                IrideSlider(value = gain.toFloat(), onValueChange = { onGainChange(it.toDouble()) }, valueRange = -12f..12f, steps = ((12f * 2) / EQPreset.GAIN_STEP.toFloat()).toInt() - 1)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
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
private fun DeleteConfirmDialog(deviceName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline)) {
            Column(Modifier.padding(22.dp)) {
                Text(text = stringResource(R.string.eq_delete_title), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp), color = MaterialTheme.colorScheme.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(R.string.eq_delete_message, deviceName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.textSecondary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    EqGhostPill(text = stringResource(android.R.string.cancel), onClick = onDismiss)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.error, onClick = onConfirm) {
                        Text(text = stringResource(R.string.eq_delete_confirm), style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}
