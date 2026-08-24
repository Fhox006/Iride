package com.metrolist.music.eq.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved EQ Profile with metadata
 */
@Serializable
data class SavedEQProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val bands: List<ParametricEQBand>,
    val preamp: Double = 0.0,
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    /** 0..1 low-shelf boost intensity around 90 Hz */
    val bassBoostIntensity: Double = 0.0,
    /** 0..1 transient-shaping strength (advanced "duration" parameter) */
    val transientStrength: Double = 0.0
) {
    fun bassBoostDb(): Double = bassBoostIntensity * EQPreset.MAX_BASS_BOOST_DB
}

/**
 * Repository for managing EQ profiles
 * Handles saving, loading, and activating EQ profiles
 */
@Singleton
class EQProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nanosonic_eq_profiles",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _profiles = MutableStateFlow<List<SavedEQProfile>>(emptyList())
    val profiles: StateFlow<List<SavedEQProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<SavedEQProfile?>(null)
    val activeProfile: StateFlow<SavedEQProfile?> = _activeProfile.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Profile actually applied to playback: null when the EQ is disabled */
    private val _effectiveProfile = MutableStateFlow<SavedEQProfile?>(null)
    val effectiveProfile: StateFlow<SavedEQProfile?> = _effectiveProfile.asStateFlow()

    private val _deviceBindings = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceBindings: StateFlow<Map<String, String>> = _deviceBindings.asStateFlow()

    companion object {
        private const val KEY_PROFILES = "eq_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_DEVICE_BINDINGS = "eq_device_bindings"
    }

    init {
        loadProfiles()
        refreshEffective()
    }

    /**
     * Load all saved profiles from SharedPreferences
     */
    private fun loadProfiles() {
        try {
            val profilesJson = prefs.getString(KEY_PROFILES, null)
            if (profilesJson != null) {
                val loadedProfiles = json.decodeFromString<List<SavedEQProfile>>(profilesJson)
                _profiles.value = loadedProfiles

                val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
                _activeProfile.value = loadedProfiles.find { it.id == activeId }
            }
            _enabled.value = prefs.getBoolean(KEY_ENABLED, true)
            val bindingsJson = prefs.getString(KEY_DEVICE_BINDINGS, null)
            if (bindingsJson != null) {
                _deviceBindings.value = json.decodeFromString(bindingsJson)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading EQ profiles")
            _profiles.value = emptyList()
            _activeProfile.value = null
        }
    }

    private fun refreshEffective() {
        _effectiveProfile.value = if (_enabled.value) _activeProfile.value else null
    }

    /**
     * Save a new EQ profile
     */
    suspend fun saveProfile(profile: SavedEQProfile) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()

        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }

        if (existingIndex >= 0) {
            currentProfiles[existingIndex] = profile
        } else {
            currentProfiles.add(profile)
        }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        if (_activeProfile.value?.id == profile.id) {
            _activeProfile.value = profile
        }
        _profiles.value = currentProfiles
        refreshEffective()
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        currentProfiles.removeAll { it.id == profileId }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = null
            prefs.edit { remove(KEY_ACTIVE_PROFILE_ID) }
        }

        _profiles.value = currentProfiles
        refreshEffective()
    }

    /**
     * Set a profile as active (only one profile can be active at a time)
     * Pass null to deactivate all profiles
     */
    suspend fun setActiveProfile(profileId: String?) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value

        if (profileId == null) {
            _activeProfile.value = null
            prefs.edit { remove(KEY_ACTIVE_PROFILE_ID) }
        } else {
            val profile = currentProfiles.find { it.id == profileId }
            _activeProfile.value = profile
            prefs.edit { putString(KEY_ACTIVE_PROFILE_ID, profileId) }
        }
        refreshEffective()
    }

    /**
     * Enable or disable the equalizer globally.
     * The working profile is kept so re-enabling restores it.
     */
    suspend fun setEnabled(value: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        _enabled.value = value
        refreshEffective()
    }

    /**
     * Link an output device key to a profile: the device auto-switches to it on connect.
     */
    suspend fun bindDevice(deviceKey: String, profileId: String) = withContext(Dispatchers.IO) {
        val updated = _deviceBindings.value + (deviceKey to profileId)
        prefs.edit { putString(KEY_DEVICE_BINDINGS, json.encodeToString(updated)) }
        _deviceBindings.value = updated
    }

    /**
     * Remove a device link
     */
    suspend fun unbindDevice(deviceKey: String) = withContext(Dispatchers.IO) {
        val updated = _deviceBindings.value - deviceKey
        prefs.edit { putString(KEY_DEVICE_BINDINGS, json.encodeToString(updated)) }
        _deviceBindings.value = updated
    }

    /**
     * Profile bound to the given output device key, if any
     */
    fun profileForDevice(deviceKey: String): SavedEQProfile? {
        val id = _deviceBindings.value[deviceKey] ?: return null
        return _profiles.value.find { it.id == id }
    }

    /**
     * Get all saved profiles
     */
    fun getAllProfiles(): List<SavedEQProfile> {
        return _profiles.value
    }

    /**
     * Get active profile
     */
    fun getActiveProfile(): SavedEQProfile? {
        return _activeProfile.value
    }

    /**
     * Import a custom EQ profile from ParametricEQ data
     */
    suspend fun importCustomProfile(
        name: String,
        parametricEQ: ParametricEQ
    ) = withContext(Dispatchers.IO) {
        val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

        val customProfile = SavedEQProfile(
            id = id,
            name = name,
            deviceModel = name,
            bands = parametricEQ.bands,
            preamp = parametricEQ.preamp,
            isActive = false,
            isCustom = true
        )

        saveProfile(customProfile)
    }

    /**
     * Get profiles sorted by type: AutoEQ first, then custom profiles
     * Within each group, sort by timestamp (newest first)
     */
    fun getSortedProfiles(): List<SavedEQProfile> {
        return _profiles.value
            .filter { it.isCustom }
            .sortedByDescending { it.addedTimestamp }
    }
}
