/**
 * Metrolist Project (C) 2026
 * Secure storage for sensitive tokens (cookies, API keys).
 * Uses EncryptedSharedPreferences with MasterKey AES256_GCM.
 * Migration from plaintext DataStore is done lazily on first read.
 */
package com.metrolist.music.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.metrolist.music.constants.InnerTubeCookieKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getSecureString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    fun putSecureString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun removeSecureString(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun containsSecure(key: String): Boolean = prefs.contains(key)
}
