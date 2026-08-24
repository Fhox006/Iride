package com.metrolist.music.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.eq.AudioDeviceMonitor
import com.metrolist.music.eq.AudioOutput
import com.metrolist.music.eq.EqualizerService
import com.metrolist.music.di.ApplicationScope
import com.metrolist.music.eq.data.EQPreset
import com.metrolist.music.eq.data.EQProfileRepository
import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * ViewModel of the parametric EQ editor: keeps the working profile in sync with the
 * repository (debounced auto-save) and reacts to output device changes.
 */
@HiltViewModel
class EqEditorViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService,
    private val audioDeviceMonitor: AudioDeviceMonitor,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    data class UiState(
        val enabled: Boolean = true,
        val bands: List<ParametricEQBand> = EQPreset.defaultBands(),
        val selectedBand: Int = 0,
        val bassBoost: Float = 0f,
        val transientStrength: Float = 0f,
        val output: AudioOutput? = null,
        val bindings: Map<String, String> = emptyMap(),
        val workingProfileId: String? = null,
        /** Selected style tab; stays sticky while the user tweaks, [modified] marks divergence */
        val presetId: String = EQPreset.PRESET_STANDARD,
        val modified: Boolean = false,
        val savedForCurrentDevice: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var saveJob: Job? = null

    /** Ignore repository echoes right after our own save, so they don't overwrite UI edits */
    private var suppressAdoptUntil = 0L

    /** Curve the selected style resolves to; manual edits are diffed against it */
    private var baseBands: List<ParametricEQBand> = EQPreset.defaultBands()

    init {
        seedFromRepository()

        viewModelScope.launch {
            eqProfileRepository.enabled.collect { enabled ->
                _state.update { it.copy(enabled = enabled) }
            }
        }
        viewModelScope.launch {
            eqProfileRepository.deviceBindings.collect { bindings ->
                _state.update { old ->
                    old.copy(
                        bindings = bindings,
                        savedForCurrentDevice =
                        old.output?.let { bindings.containsKey(it.deviceKey) } == true
                    )
                }
            }
        }
        viewModelScope.launch {
            eqProfileRepository.activeProfile.collect { profile ->
                adoptExternalProfile(profile)
            }
        }
        viewModelScope.launch {
            audioDeviceMonitor.current.collect { output ->
                _state.update { old ->
                    old.copy(
                        output = output,
                        savedForCurrentDevice = old.bindings.containsKey(output.deviceKey)
                    )
                }
            }
        }
    }

    private fun seedFromRepository() {
        val active = eqProfileRepository.activeProfile.value
        if (active != null && active.isCustom && active.bands.size == EQPreset.DEFAULT_BAND_FREQUENCIES.size) {
            val matched = EQPreset.matchPreset(active.bands)
            baseBands = active.bands
            _state.value = UiState(
                enabled = eqProfileRepository.enabled.value,
                bands = active.bands,
                bassBoost = active.bassBoostIntensity.toFloat(),
                transientStrength = active.transientStrength.toFloat(),
                output = audioDeviceMonitor.current.value,
                bindings = eqProfileRepository.deviceBindings.value,
                workingProfileId = active.id,
                presetId = matched ?: EQPreset.PRESET_CUSTOM,
                savedForCurrentDevice = eqProfileRepository.profileForDevice(
                    audioDeviceMonitor.current.value.deviceKey
                )?.id == active.id
            )
            // Re-apply on startup so a persisted profile reaches the processor even
            // when MusicService collected it before the processors were attached.
            viewModelScope.launch {
                if (eqProfileRepository.enabled.value) {
                    equalizerService.applyProfile(active)
                }
            }
        } else {
            // No usable profile yet: materialize one immediately so every action that
            // needs a profile (device binding, persistence) is available from the start.
            val id = DEFAULT_EDITOR_PROFILE_ID
            val seeded = eqProfileRepository.getAllProfiles().find { it.id == id }
                ?: buildProfile(id, UiState(output = audioDeviceMonitor.current.value))
            baseBands = seeded.bands
            _state.value = UiState(
                enabled = eqProfileRepository.enabled.value,
                bands = seeded.bands,
                bassBoost = seeded.bassBoostIntensity.toFloat(),
                transientStrength = seeded.transientStrength.toFloat(),
                output = audioDeviceMonitor.current.value,
                bindings = eqProfileRepository.deviceBindings.value,
                workingProfileId = id,
                presetId = EQPreset.PRESET_STANDARD,
                savedForCurrentDevice =
                    eqProfileRepository.profileForDevice(audioDeviceMonitor.current.value.deviceKey)?.id == id
            )
            viewModelScope.launch {
                eqProfileRepository.saveProfile(seeded)
                eqProfileRepository.setActiveProfile(seeded.id)
                if (eqProfileRepository.enabled.value) {
                    equalizerService.applyProfile(seeded)
                }
            }
        }
    }

    private fun adoptExternalProfile(profile: SavedEQProfile?) {
        if (profile == null || System.currentTimeMillis() < suppressAdoptUntil) return
        val current = _state.value
        if (profile.id == current.workingProfileId &&
            contentEquals(current, profile)
        ) {
            return
        }
        if (profile.bands.size != EQPreset.DEFAULT_BAND_FREQUENCIES.size) return

        baseBands = profile.bands
        _state.update {
            it.copy(
                bands = profile.bands,
                bassBoost = profile.bassBoostIntensity.toFloat(),
                transientStrength = profile.transientStrength.toFloat(),
                workingProfileId = profile.id,
                presetId = EQPreset.matchPreset(profile.bands) ?: EQPreset.PRESET_CUSTOM,
                modified = false
            )
        }
    }

    private fun contentEquals(state: UiState, profile: SavedEQProfile): Boolean {
        if (!sameAs(state.bands, profile.bands)) return false
        return abs(state.bassBoost - profile.bassBoostIntensity) < 0.01 &&
                abs(state.transientStrength - profile.transientStrength) < 0.01
    }

    private fun sameAs(a: List<ParametricEQBand>, b: List<ParametricEQBand>): Boolean {
        if (a.size != b.size) return false
        a.forEachIndexed { i, band ->
            val other = b[i]
            if (abs(band.gain - other.gain) > 0.01 ||
                abs(band.frequency - other.frequency) > 0.01 ||
                abs(band.q - other.q) > 0.01
            ) return false
        }
        return true
    }

    fun setEnabled(value: Boolean) {
        _state.update { it.copy(enabled = value) }
        viewModelScope.launch {
            eqProfileRepository.setEnabled(value)
            if (value) {
                val id = _state.value.workingProfileId
                    ?: run {
                        flushSaveNow()
                        _state.value.workingProfileId
                    }
                id?.let { eqProfileRepository.setActiveProfile(it) }
            }
        }
    }

    fun selectBand(index: Int) {
        _state.update { it.copy(selectedBand = index.coerceIn(0, it.bands.lastIndex)) }
    }

    fun setBandGain(index: Int, gainDb: Double) {
        updateBand(index) { band ->
            band.copy(gain = gainDb.snapTo(EQPreset.GAIN_STEP).coerceIn(-EQPreset.MAX_GAIN_DB, EQPreset.MAX_GAIN_DB))
        }
    }

    fun setBandFrequency(index: Int, frequency: Double) {
        val bounds = EQPreset.frequencyBounds(index)
        updateBand(index) { band ->
            band.copy(frequency = frequency.coerceIn(bounds.start, bounds.endInclusive))
        }
    }

    fun setBandQ(index: Int, q: Double) {
        updateBand(index) { band -> band.copy(q = q.coerceIn(EQPreset.MIN_Q, EQPreset.MAX_Q)) }
    }

    fun resetBand(index: Int) {
        updateBand(index) { band ->
            band.copy(gain = 0.0, frequency = EQPreset.DEFAULT_BAND_FREQUENCIES[index], q = EQPreset.DEFAULT_Q)
        }
    }

    /**
     * Select a style tab. Tapping the already-selected style rebuilds its base curve,
     * which is the documented way to reset gains, frequencies and Q to defaults.
     */
    fun applyPreset(presetId: String) {
        if (presetId == EQPreset.PRESET_CUSTOM) {
            baseBands = _state.value.bands
            _state.update { it.copy(presetId = EQPreset.PRESET_CUSTOM, modified = false) }
            scheduleSave()
            return
        }
        val bands = EQPreset.bandsForPreset(presetId) ?: return
        baseBands = bands
        // Flat must be truly flat: also clear bass enhance and duration shaping
        val flat = presetId == EQPreset.PRESET_STANDARD
        _state.update {
            it.copy(
                bands = bands,
                presetId = presetId,
                modified = false,
                bassBoost = if (flat) 0f else it.bassBoost,
                transientStrength = if (flat) 0f else it.transientStrength
            )
        }
        scheduleSave()
    }

    fun setBassBoost(value: Float) {
        _state.update { it.copy(bassBoost = value.coerceIn(0f, 1f)) }
        scheduleSave()
    }

    fun setTransientStrength(value: Float) {
        _state.update { it.copy(transientStrength = value.coerceIn(0f, 1f)) }
        scheduleSave()
    }

    fun saveForDevice() {
        val s = _state.value
        val output = s.output ?: return
        val id = s.workingProfileId ?: return
        viewModelScope.launch {
            flushSaveNow()
            eqProfileRepository.bindDevice(output.deviceKey, id)
        }
    }

    fun removeDeviceBinding(deviceKey: String) {
        viewModelScope.launch {
            eqProfileRepository.unbindDevice(deviceKey)
        }
    }

    private fun updateBand(index: Int, transform: (ParametricEQBand) -> ParametricEQBand) {
        _state.update { state ->
            val newBands = state.bands.toMutableList().also {
                it[index] = transform(it[index])
            }
            state.copy(
                bands = newBands,
                modified = !sameAs(newBands, baseBands)
            )
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            flushSaveNow()
        }
    }

    override fun onCleared() {
        // The debounced job dies with viewModelScope; carry a pending save over to an
        // application scope so quick exits never lose the last edit.
        if (saveJob?.isActive == true) {
            val pending = _state.value
            appScope.launch { flushSave(pending) }
        }
        super.onCleared()
    }

    private suspend fun flushSaveNow() {
        flushSave(_state.value)
    }

    private suspend fun flushSave(s: UiState) {
        val bassDb = s.bassBoost.toDouble() * EQPreset.MAX_BASS_BOOST_DB
        val name = displayName(s)

        val id = s.workingProfileId ?: "editor_${System.currentTimeMillis()}"
        val profile = buildProfile(id, s, name, bassDb)

        suppressAdoptUntil = System.currentTimeMillis() + ADOPT_SUPPRESS_MS
        eqProfileRepository.saveProfile(profile)
        _state.update { it.copy(workingProfileId = id) }
        eqProfileRepository.setActiveProfile(id)
    }

    private fun buildProfile(
        id: String,
        s: UiState,
        name: String = displayName(s),
        bassDb: Double = s.bassBoost.toDouble() * EQPreset.MAX_BASS_BOOST_DB
    ): SavedEQProfile = SavedEQProfile(
        id = id,
        name = name,
        deviceModel = name,
        bands = s.bands,
        preamp = EQPreset.autoPreampDb(s.bands, bassDb),
        isCustom = true,
        bassBoostIntensity = s.bassBoost.toDouble(),
        transientStrength = s.transientStrength.toDouble()
    )

    private fun displayName(state: UiState): String {
        val output = state.output
        return when {
            output != null && output.isBluetooth && !output.productName.isNullOrBlank() ->
                output.productName!!.trim()
            else -> DEFAULT_PROFILE_NAME
        }
    }

    private fun Double.snapTo(step: Double): Double = kotlin.math.round(this / step) * step

    companion object {
        private const val SAVE_DEBOUNCE_MS = 250L
        private const val ADOPT_SUPPRESS_MS = 800L
        private const val DEFAULT_PROFILE_NAME = "Custom"
        private const val DEFAULT_EDITOR_PROFILE_ID = "editor_default"
    }
}
