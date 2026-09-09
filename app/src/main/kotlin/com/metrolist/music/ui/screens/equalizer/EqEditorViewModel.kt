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
 * Per-device EQ ViewModel. Each device key owns an independent working copy (bands,
 * bass boost, transient strength, selected band, preset id and modified flag), so
 * editing one card never bleeds into another. Auto-save is debounced per device.
 */
@HiltViewModel
class EqEditorViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService,
    private val audioDeviceMonitor: AudioDeviceMonitor,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    data class WorkingProfile(
        val id: String,
        val name: String,
        val bands: List<ParametricEQBand> = EQPreset.defaultBands(),
        val selectedBand: Int = 0,
        val bassBoost: Float = 0f,
        val transientStrength: Float = 0f,
        val presetId: String = EQPreset.PRESET_STANDARD,
        val modified: Boolean = false
    )

    data class UiState(
        val output: AudioOutput? = null,
        val bindings: Map<String, String> = emptyMap(),
        val globalProfileId: String? = null,
        val deviceIcons: Map<String, String> = emptyMap(),
        val deviceNames: Map<String, String> = emptyMap(),
        /** One WorkingProfile per device key. Includes "global" when a global exists. */
        val working: Map<String, WorkingProfile> = emptyMap(),
        /** Currently expanded device key (custom panel open). */
        val expandedDeviceKey: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val saveJobs = mutableMapOf<String, Job>()
    private val suppressAdoptUntil = 0L
    private val baseBandsByKey = mutableMapOf<String, List<ParametricEQBand>>()

    init {
        seedFromRepository()

        viewModelScope.launch {
            eqProfileRepository.deviceBindings.collect { bindings ->
                _state.update { old ->
                    val working = ensureWorkingForKnownDevices(old.working, bindings)
                    old.copy(bindings = bindings, working = working)
                }
                applyEffectiveToAudio()
            }
        }
        viewModelScope.launch {
            audioDeviceMonitor.current.collect { output ->
                _state.update { old -> old.copy(output = output) }
                applyEffectiveToAudio()
            }
        }
        viewModelScope.launch {
            eqProfileRepository.globalProfileId.collect { gid ->
                _state.update { old ->
                    val working = if (gid != null) {
                        val profile = eqProfileRepository.getAllProfiles().find { it.id == gid }
                        if (profile != null && profile.bands.size == EQPreset.DEFAULT_BAND_FREQUENCIES.size) {
                            old.working + ("global" to workingFromProfile(profile))
                        } else old.working
                    } else old.working - "global"
                    old.copy(globalProfileId = gid, working = working)
                }
                applyEffectiveToAudio()
            }
        }
        viewModelScope.launch {
            eqProfileRepository.deviceIcons.collect { icons ->
                _state.update { it.copy(deviceIcons = icons) }
            }
        }
        viewModelScope.launch {
            eqProfileRepository.deviceNames.collect { names ->
                _state.update { it.copy(deviceNames = names) }
            }
        }
        viewModelScope.launch {
            eqProfileRepository.profiles.collect { profiles ->
                _state.update { old ->
                    val working = profiles.associate { p ->
                        val key = old.bindings.entries.firstOrNull { it.value == p.id }?.key ?: p.id
                        key to workingFromProfile(p)
                    }
                    old.copy(working = mergeWorkingPreservingEdits(old.working, working))
                }
                applyEffectiveToAudio()
            }
        }
    }

    private fun seedFromRepository() {
        val bindings = eqProfileRepository.deviceBindings.value
        val gid = eqProfileRepository.globalProfileId.value
        val profiles = eqProfileRepository.getAllProfiles().associateBy { it.id }
        val working = mutableMapOf<String, WorkingProfile>()
        bindings.values.distinct().forEach { pid ->
            profiles[pid]?.let { p -> working[p.id] = workingFromProfile(p) }
        }
        if (gid != null) {
            profiles[gid]?.let { p -> working["global"] = workingFromProfile(p) }
        }
        _state.update { it.copy(working = working, bindings = bindings, globalProfileId = gid) }
    }

    private fun workingFromProfile(p: SavedEQProfile): WorkingProfile = WorkingProfile(
        id = p.id,
        name = p.name,
        bands = p.bands,
        bassBoost = p.bassBoostIntensity.toFloat(),
        transientStrength = p.transientStrength.toFloat(),
        presetId = EQPreset.matchPreset(p.bands) ?: EQPreset.PRESET_CUSTOM,
        modified = false
    )

    private fun ensureWorkingForKnownDevices(
        existing: Map<String, WorkingProfile>,
        bindings: Map<String, String>
    ): Map<String, WorkingProfile> {
        val profiles = eqProfileRepository.getAllProfiles().associateBy { it.id }
        val updated = existing.toMutableMap()
        bindings.forEach { (key, pid) ->
            if (updated[key] == null || updated[key]?.id != pid) {
                profiles[pid]?.let { p -> updated[key] = workingFromProfile(p) }
            }
        }
        val currentKey = _state.value.output?.deviceKey
        if (currentKey != null && updated[currentKey] == null) {
            val seedId = DEFAULT_EDITOR_PROFILE_ID + "_" + currentKey.hashCode()
            val seed = profiles[seedId] ?: profiles[DEFAULT_EDITOR_PROFILE_ID] ?: eqProfileRepository
                .getAllProfiles()
                .firstOrNull()
                ?.let { workingFromProfile(it) }
            if (seed != null) {
                updated[currentKey] = WorkingProfile(id = seedId, name = "Custom", bands = EQPreset.defaultBands())
            }
        }
        return updated
    }

    private fun mergeWorkingPreservingEdits(
        current: Map<String, WorkingProfile>,
        fresh: Map<String, WorkingProfile>
    ): Map<String, WorkingProfile> {
        if (current.isEmpty()) return fresh
        val out = current.toMutableMap()
        fresh.forEach { (k, v) ->
            val existing = current[k]
            if (existing == null || !existing.modified) {
                out[k] = v
            }
        }
        return out
    }

    private fun workingForKey(key: String): WorkingProfile? = _state.value.working[key]

    private fun updateWorking(key: String, transform: (WorkingProfile) -> WorkingProfile) {
        _state.update { old ->
            val current = old.working[key] ?: return@update old
            val next = transform(current)
            val newMap = old.working + (key to next)
            old.copy(working = newMap)
        }
        scheduleSave(key)
    }

    private fun baseBandsForKey(key: String): List<ParametricEQBand> {
        baseBandsByKey[key]?.let { return it }
        val profileId = _state.value.working[key]?.id
        if (profileId != null) {
            val p = eqProfileRepository.getAllProfiles().find { it.id == profileId }
            if (p != null) {
                baseBandsByKey[key] = p.bands
                return p.bands
            }
        }
        return EQPreset.defaultBands()
    }

    fun selectBand(key: String, index: Int) {
        updateWorking(key) { w -> w.copy(selectedBand = index.coerceIn(0, w.bands.lastIndex)) }
    }

    fun setBandGain(key: String, index: Int, gainDb: Double) {
        updateWorking(key) { w ->
            val newBands = w.bands.toMutableList().also {
                it[index] = it[index].copy(
                    gain = gainDb.snapTo(EQPreset.GAIN_STEP).coerceIn(-EQPreset.MAX_GAIN_DB, EQPreset.MAX_GAIN_DB)
                )
            }
            w.copy(bands = newBands, modified = !sameAs(newBands, baseBandsForKey(key)))
        }
    }

    fun setBandFrequency(key: String, index: Int, frequency: Double) {
        val bounds = EQPreset.frequencyBounds(index)
        updateWorking(key) { w ->
            val newBands = w.bands.toMutableList().also {
                it[index] = it[index].copy(frequency = frequency.coerceIn(bounds.start, bounds.endInclusive))
            }
            w.copy(bands = newBands, modified = !sameAs(newBands, baseBandsForKey(key)))
        }
    }

    fun setBandQ(key: String, index: Int, q: Double) {
        updateWorking(key) { w ->
            val newBands = w.bands.toMutableList().also {
                it[index] = it[index].copy(q = q.coerceIn(EQPreset.MIN_Q, EQPreset.MAX_Q))
            }
            w.copy(bands = newBands, modified = !sameAs(newBands, baseBandsForKey(key)))
        }
    }

    fun resetBand(key: String, index: Int) {
        updateWorking(key) { w ->
            val newBands = w.bands.toMutableList().also {
                it[index] = it[index].copy(
                    gain = 0.0,
                    frequency = EQPreset.DEFAULT_BAND_FREQUENCIES[index],
                    q = EQPreset.DEFAULT_Q
                )
            }
            w.copy(bands = newBands, modified = !sameAs(newBands, baseBandsForKey(key)))
        }
    }

    fun applyPreset(key: String, presetId: String) {
        if (presetId == EQPreset.PRESET_CUSTOM) {
            updateWorking(key) { w -> w.copy(presetId = EQPreset.PRESET_CUSTOM, modified = true) }
            baseBandsByKey[key] = workingForKey(key)?.bands ?: EQPreset.defaultBands()
            scheduleSave(key)
            return
        }
        val bands = EQPreset.bandsForPreset(presetId) ?: return
        val flat = presetId == EQPreset.PRESET_STANDARD
        baseBandsByKey[key] = bands
        updateWorking(key) { w ->
            w.copy(
                bands = bands,
                presetId = presetId,
                modified = false,
                bassBoost = if (flat) 0f else w.bassBoost,
                transientStrength = if (flat) 0f else w.transientStrength
            )
        }
        scheduleSave(key)
    }

    fun setBassBoost(key: String, value: Float) {
        updateWorking(key) { w -> w.copy(bassBoost = value.coerceIn(0f, 1f), modified = true) }
    }

    fun setTransientStrength(key: String, value: Float) {
        updateWorking(key) { w -> w.copy(transientStrength = value.coerceIn(0f, 1f), modified = true) }
    }

    fun setBassBoostStepped(key: String, step: Int) {
        val value = when (step) {
            0 -> 0f
            1 -> 0.25f
            2 -> 0.5f
            else -> 0.85f
        }
        setBassBoost(key, value)
    }

    fun setExpandedDevice(key: String?) {
        _state.update { it.copy(expandedDeviceKey = key) }
    }

    fun toggleGlobalForCurrent(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val wp = _state.value.working[key] ?: return@launch
            if (enabled) {
                flushSaveNow(key)
                val profile = buildProfileFromWorking(key, wp)
                eqProfileRepository.saveProfile(profile)
                eqProfileRepository.setGlobalProfileId(profile.id)
                eqProfileRepository.bindDevice(key, profile.id)
            } else {
                eqProfileRepository.setGlobalProfileId(null)
            }
        }
    }

    fun setDeviceIcon(deviceKey: String, iconKey: String) {
        viewModelScope.launch { eqProfileRepository.setDeviceIcon(deviceKey, iconKey) }
    }

    fun setDeviceName(deviceKey: String, name: String?) {
        viewModelScope.launch { eqProfileRepository.setDeviceName(deviceKey, name) }
    }

    fun removeDeviceBinding(deviceKey: String) {
        viewModelScope.launch {
            eqProfileRepository.unbindDevice(deviceKey)
            val gid = _state.value.globalProfileId
            if (gid != null) {
                val boundProfile = eqProfileRepository.profileForDevice(deviceKey)
                if (boundProfile?.id == gid) eqProfileRepository.setGlobalProfileId(null)
            }
        }
    }

    fun saveForCurrent(key: String) {
        viewModelScope.launch { flushSaveNow(key) }
    }

    private fun scheduleSave(key: String) {
        saveJobs[key]?.cancel()
        saveJobs[key] = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            flushSaveNow(key)
        }
    }

    private suspend fun flushSaveNow(key: String) {
        val wp = _state.value.working[key] ?: return
        val profile = buildProfileFromWorking(key, wp)
        eqProfileRepository.saveProfile(profile)
        _state.update { it.copy(working = it.working + (key to wp.copy(modified = false))) }
        applyEffectiveToAudio()
    }

    private fun buildProfileFromWorking(key: String, w: WorkingProfile): SavedEQProfile {
        val bassDb = w.bassBoost.toDouble() * EQPreset.MAX_BASS_BOOST_DB
        val name = displayNameForKey(key, w)
        val id = w.id
        return SavedEQProfile(
            id = id,
            name = name,
            deviceModel = name,
            bands = w.bands,
            preamp = EQPreset.autoPreampDb(w.bands, bassDb),
            isCustom = true,
            bassBoostIntensity = w.bassBoost.toDouble(),
            transientStrength = w.transientStrength.toDouble()
        )
    }

    private fun displayNameForKey(key: String, w: WorkingProfile): String {
        if (key == "global") return w.name.ifBlank { DEFAULT_PROFILE_NAME }
        return w.name.ifBlank {
            val custom = _state.value.deviceNames[key]?.takeIf { it.isNotBlank() }
            if (custom != null) return custom
            val out = _state.value.output
            if (key == out?.deviceKey) {
                if (out.isBluetooth && !out.productName.isNullOrBlank()) return out.productName!!.trim()
                return DEFAULT_PROFILE_NAME
            }
            key.removePrefix("bt|").ifBlank { DEFAULT_PROFILE_NAME }
        }
    }

    private fun applyEffectiveToAudio() {
        val currentKey = _state.value.output?.deviceKey ?: return
        val gid = _state.value.globalProfileId
        val wp = if (gid != null && _state.value.working["global"]?.id == gid) {
            _state.value.working[currentKey] ?: _state.value.working["global"]
        } else {
            _state.value.working[currentKey]
        }
        if (wp == null) return
        val profile = buildProfileFromWorking(currentKey, wp)
        viewModelScope.launch { equalizerService.applyProfile(profile) }
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

    private fun Double.snapTo(step: Double): Double = kotlin.math.round(this / step) * step

    override fun onCleared() {
        saveJobs.values.forEach { it.cancel() }
        _state.value.working.keys.forEach { key ->
            val wp = _state.value.working[key] ?: return@forEach
            appScope.launch { runCatching { eqProfileRepository.saveProfile(buildProfileFromWorking(key, wp)) } }
        }
        super.onCleared()
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 250L
        private const val DEFAULT_PROFILE_NAME = "Custom"
        private const val DEFAULT_EDITOR_PROFILE_ID = "editor_default"
    }
}
