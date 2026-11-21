package com.metrolist.music.ui.component

import android.app.ActivityManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.ReducedFolioModeKey
import com.metrolist.music.utils.rememberPreference

internal val FolioLowPerformanceSurfaceColor = Color(0xFF101012)

internal data class FolioDeviceProfile(val lowRam: Boolean, val sdk: Int) {
    val defaultLightweightMode: Boolean get() = false
    val supportsFullMotion: Boolean get() = sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    val recommendedLowPowerMode: Boolean get() = lowRam || !supportsFullMotion
    val needsIntroduction: Boolean get() = recommendedLowPowerMode
    fun lightweight(preference: Boolean): Boolean = preference
}

@Composable
internal fun rememberFolioDeviceProfile(includePowerSaveMode: Boolean = BuildConfig.DEBUG): FolioDeviceProfile {
    val context = LocalContext.current
    return remember(context, includePowerSaveMode) {
        val simulateLowRam = includePowerSaveMode &&
            context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        FolioDeviceProfile(
            lowRam = simulateLowRam ||
                context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true,
            sdk = Build.VERSION.SDK_INT,
        )
    }
}

/** Recommend lighter effects where needed, while honoring the user's saved choice. */
@Composable
internal fun rememberLightweightFolioMode(): State<Boolean> {
    val device = rememberFolioDeviceProfile()
    val preference = rememberPreference(ReducedFolioModeKey, device.defaultLightweightMode)
    return remember(device, preference) { derivedStateOf { device.lightweight(preference.value) } }
}
