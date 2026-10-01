package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageFrame
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import java.util.Locale
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
fun DesignTestScreen(navController: NavController) {
    if (!BuildConfig.DEBUG) return

    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("spray_design_test", Context.MODE_PRIVATE)
    }
    val initial = remember(preferences) {
        readSpraySettings(preferences.getString("settings", null))
    }
    var draft by remember { mutableStateOf(initial) }
    var applied by remember { mutableStateOf(draft) }
    var previewView by remember { mutableStateOf<SprayTextView?>(null) }
    var showGuides by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val applyChanges: () -> Unit = {
        applied = draft
        preferences.edit().putString("settings", draft.toJson().toString()).apply()
    }

    IridePageFrame(
        title = stringResource(R.string.design_test),
        scrollState = scrollState,
        navigationIcon = { IridePageBackButton(navController) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { context -> SprayTextView(context).also { previewView = it } },
            update = { view ->
                view.settings = applied
                view.showHaloGuides = showGuides
                view.onSettingsEdited = { edited ->
                    draft = edited
                    applied = edited
                    preferences.edit().putString("settings", edited.toJson().toString()).apply()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { previewView?.regenerateGrain() }) {
                Text(stringResource(R.string.design_test_new_grain))
            }
            OutlinedButton(onClick = {
                draft = SpraySettings()
                applied = draft
                preferences.edit().remove("settings").apply()
            }) {
                Text(stringResource(R.string.design_test_reset))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.design_test_show_circles))
            Switch(checked = showGuides, onCheckedChange = { showGuides = it })
        }
        Text(
            text = stringResource(R.string.design_test_drag_help),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.design_test_snap_letters))
            Switch(
                checked = draft.snapToLetters,
                onCheckedChange = {
                    draft = draft.copy(snapToLetters = it)
                    applyChanges()
                },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            SpraySection(stringResource(R.string.design_test_edge_section))
            SprayControl(stringResource(R.string.design_test_edge_blur), draft.edgeBlur, 0f..0.06f,
                format = ::percent, onChange = { draft = draft.copy(edgeBlur = it) }, onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_edge_passes), draft.edgePasses, 1f..6f,
                format = ::oneDecimal, onChange = { draft = draft.copy(edgePasses = it) },
                onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_edge_particles), draft.edgeParticles, 0f..3f,
                format = ::multiple, onChange = { draft = draft.copy(edgeParticles = it) }, onFinish = applyChanges)

            SpraySection(stringResource(R.string.design_test_mist_section))
            SprayControl(stringResource(R.string.design_test_mist_blur), draft.mistBlur, 0f..0.16f,
                format = ::percent, onChange = { draft = draft.copy(mistBlur = it) }, onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_mist_passes), draft.mistPasses, 1f..6f,
                format = ::oneDecimal, onChange = { draft = draft.copy(mistPasses = it) },
                onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_mist_strength), draft.mistStrength, 0f..1f,
                format = ::percent, onChange = { draft = draft.copy(mistStrength = it) }, onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_offset_x), draft.mistOffsetX, -0.35f..0.35f,
                format = ::percent, onChange = { draft = draft.copy(mistOffsetX = it) }, onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_offset_y), draft.mistOffsetY, -0.35f..0.35f,
                format = ::percent, onChange = { draft = draft.copy(mistOffsetY = it) }, onFinish = applyChanges)
            HaloControls(stringResource(R.string.design_test_left_halo), draft.leftHalo,
                onChange = { draft = draft.copy(leftHalo = it) }, onFinish = applyChanges)
            HaloControls(stringResource(R.string.design_test_right_halo), draft.rightHalo,
                onChange = { draft = draft.copy(rightHalo = it) }, onFinish = applyChanges)

            SpraySection(stringResource(R.string.design_test_particles_section))
            SprayControl(stringResource(R.string.design_test_particle_density), draft.particleDensity, 0f..10f,
                format = ::multiple, onChange = { draft = draft.copy(particleDensity = it) }, onFinish = applyChanges)
            SprayControl(stringResource(R.string.design_test_particle_size),
                (draft.particleSize + 1) / 2f, 1f..5f,
                format = { (it.roundToInt() * 2 - 1).toString() + " px" },
                onChange = { draft = draft.copy(particleSize = it.roundToInt() * 2 - 1) },
                onFinish = applyChanges)
            Spacer(Modifier.height(140.dp))
        }
        }
}
}

@Composable
private fun HaloControls(
    title: String,
    halo: SprayHalo,
    onChange: (SprayHalo) -> Unit,
    onFinish: () -> Unit,
) {
    SpraySection(title)
    SprayControl(stringResource(R.string.design_test_halo_x), halo.x, -0.5f..1.5f,
        format = ::percent, onChange = { onChange(halo.copy(x = it)) }, onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_y), halo.y, -0.5f..1.5f,
        format = ::percent, onChange = { onChange(halo.copy(y = it)) }, onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_radius_x), halo.radiusX, 0.1f..2.5f,
        format = ::percent, onChange = { onChange(halo.copy(radiusX = it)) }, onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_radius_y), halo.radiusY, 0.1f..2.5f,
        format = ::percent, onChange = { onChange(halo.copy(radiusY = it)) }, onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_angle), halo.angle, -180f..180f,
        format = { integer(it) + "°" }, onChange = { onChange(halo.copy(angle = it)) },
        onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_feather), halo.feather, 0f..1f,
        format = ::percent, onChange = { onChange(halo.copy(feather = it)) }, onFinish = onFinish)
    SprayControl(stringResource(R.string.design_test_halo_strength), halo.strength, 0f..1f,
        format = ::percent, onChange = { onChange(halo.copy(strength = it)) }, onFinish = onFinish)
}

@Composable
private fun SpraySection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SprayControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(format(value), style = MaterialTheme.typography.bodyMedium)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        onValueChangeFinished = onFinish,
    )
}

private fun percent(value: Float): String =
    String.format(Locale.getDefault(), "%.1f%%", value * 100f)
private fun multiple(value: Float): String = ((value * 10f).roundToInt() / 10f).toString() + "×"
private fun integer(value: Float): String = value.roundToInt().toString()
private fun oneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

private fun SpraySettings.toJson(): JSONObject = JSONObject().apply {
    put("edgeBlur", edgeBlur)
    put("edgePasses", edgePasses)
    put("edgeParticles", edgeParticles)
    put("mistBlur", mistBlur)
    put("mistPasses", mistPasses)
    put("mistStrength", mistStrength)
    put("mistOffsetX", mistOffsetX)
    put("mistOffsetY", mistOffsetY)
    put("particleDensity", particleDensity)
    put("particleSize", particleSize)
    put("snapToLetters", snapToLetters)
    put("leftHalo", leftHalo.toJson())
    put("rightHalo", rightHalo.toJson())
}

private fun SprayHalo.toJson(): JSONObject = JSONObject().apply {
    put("x", x)
    put("y", y)
    put("radiusX", radiusX)
    put("radiusY", radiusY)
    put("angle", angle)
    put("feather", feather)
    put("strength", strength)
}

private fun readSpraySettings(saved: String?): SpraySettings {
    val defaults = SpraySettings()
    val json = try {
        JSONObject(saved ?: return defaults)
    } catch (_: Exception) {
        return defaults
    }
    return defaults.copy(
        edgeBlur = json.float("edgeBlur", defaults.edgeBlur),
        edgePasses = json.float("edgePasses", defaults.edgePasses),
        edgeParticles = json.float("edgeParticles", defaults.edgeParticles),
        mistBlur = json.float("mistBlur", defaults.mistBlur),
        mistPasses = json.float("mistPasses", defaults.mistPasses),
        mistStrength = json.float("mistStrength", defaults.mistStrength),
        mistOffsetX = json.float("mistOffsetX", defaults.mistOffsetX),
        mistOffsetY = json.float("mistOffsetY", defaults.mistOffsetY),
        particleDensity = json.float("particleDensity", defaults.particleDensity),
        particleSize = json.optInt("particleSize", defaults.particleSize),
        snapToLetters = json.optBoolean("snapToLetters", defaults.snapToLetters),
        leftHalo = readHalo(json.optJSONObject("leftHalo"), defaults.leftHalo),
        rightHalo = readHalo(json.optJSONObject("rightHalo"), defaults.rightHalo),
    )
}

private fun readHalo(json: JSONObject?, defaults: SprayHalo): SprayHalo {
    if (json == null) return defaults
    return defaults.copy(
        x = json.float("x", defaults.x),
        y = json.float("y", defaults.y),
        radiusX = json.float("radiusX", json.float("z", defaults.radiusX)),
        radiusY = json.float("radiusY", json.float("z", defaults.radiusY)),
        angle = json.float("angle", defaults.angle),
        feather = json.float("feather", defaults.feather),
        strength = json.float("strength", defaults.strength),
    )
}

private fun JSONObject.float(key: String, default: Float): Float =
    optDouble(key, default.toDouble()).toFloat()
