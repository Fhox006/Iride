package com.metrolist.music.eq

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of the current preferred audio output.
 * [key] uniquely identifies a physical device (Bluetooth products are distinguished by name),
 * so the equalizer can bind profiles to specific headphones.
 */
data class AudioOutput(
    val type: Type,
    val productName: String?
) {
    enum class Type { BLUETOOTH, WIRED, SPEAKER, OTHER }

    val deviceKey: String = when (type) {
        Type.BLUETOOTH -> "bt|${productName?.lowercase() ?: ""}"
        Type.WIRED -> "wired"
        Type.SPEAKER -> "speaker"
        Type.OTHER -> "other"
    }

    val isBluetooth: Boolean get() = type == Type.BLUETOOTH
}

/**
 * Tracks the active audio output device via AudioManager.
 * Reading AudioDeviceInfo.productName requires no runtime permission.
 */
@Singleton
class AudioDeviceMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _current = MutableStateFlow(readCurrentOutput())
    val current: StateFlow<AudioOutput> = _current.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    private fun refresh() {
        val next = readCurrentOutput()
        if (next != _current.value) {
            _current.value = next
        }
    }

    private fun readCurrentOutput(): AudioOutput {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }?.let {
            return AudioOutput(
                AudioOutput.Type.BLUETOOTH,
                it.productName?.toString()?.takeIf { n -> n.isNotBlank() }
            )
        }

        devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }?.let {
            return AudioOutput(
                AudioOutput.Type.WIRED,
                it.productName?.toString()?.takeIf { n -> n.isNotBlank() }
            )
        }

        devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE
        }?.let {
            return AudioOutput(AudioOutput.Type.SPEAKER, null)
        }

        return AudioOutput(AudioOutput.Type.OTHER, null)
    }
}
