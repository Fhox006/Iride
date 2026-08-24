package com.metrolist.music.eq

import com.metrolist.music.eq.data.EQProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-switches the active EQ profile when the audio output changes, for every kind of
 * output (Bluetooth, wired, speaker): a linked profile loads on connect; without a link
 * the currently active profile keeps applying everywhere, so the EQ is never gated by
 * the output type.
 */
@Singleton
class EqDeviceManager @Inject constructor(
    private val repository: EQProfileRepository,
    private val deviceMonitor: AudioDeviceMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var lastKey: String? = null

    init {
        scope.launch {
            deviceMonitor.current.collect { output ->
                handleOutput(output)
            }
        }
    }

    private suspend fun handleOutput(output: AudioOutput) {
        val key = output.deviceKey
        if (key == lastKey) return
        lastKey = key

        val bound = repository.profileForDevice(key)
        if (bound != null && bound.id != repository.activeProfile.value?.id) {
            repository.setActiveProfile(bound.id)
        }
    }
}
