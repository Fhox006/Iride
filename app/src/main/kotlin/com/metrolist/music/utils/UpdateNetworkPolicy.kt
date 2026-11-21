package com.metrolist.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.UpdateHomeCountryIsoKey
import java.util.Locale

enum class UpdateNetworkStatus {
    NO_CONNECTION,
    AUTOMATIC,
    ROAMING_CONFIRMATION_REQUIRED,
}

object UpdateNetworkPolicy {
    suspend fun currentStatus(context: Context): UpdateNetworkStatus {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return UpdateNetworkStatus.NO_CONNECTION
        val capabilities = manager.getNetworkCapabilities(network) ?: return UpdateNetworkStatus.NO_CONNECTION
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            return UpdateNetworkStatus.NO_CONNECTION
        }

        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val simCountry = telephony?.let { runCatching { normalizeCountry(it.simCountryIso) }.getOrNull() }
        val networkCountry = telephony?.let { runCatching { normalizeCountry(it.networkCountryIso) }.getOrNull() }
        val savedHomeCountry = context.dataStore.get(UpdateHomeCountryIsoKey, "")
            .takeIf(String::isNotBlank)
            ?.let(::normalizeCountry)
        val localeCountry = normalizeCountry(Locale.getDefault().country)
        val homeCountry = savedHomeCountry ?: simCountry ?: localeCountry

        if (savedHomeCountry == null && homeCountry != null) {
            context.dataStore.edit { preferences ->
                if (preferences[UpdateHomeCountryIsoKey].isNullOrBlank()) {
                    preferences[UpdateHomeCountryIsoKey] = homeCountry
                }
            }
        }

        val networkIsCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        if (!isMetered) return UpdateNetworkStatus.AUTOMATIC
        val carrierReportsRoaming =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && networkIsCellular) {
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            } else if (networkIsCellular) {
                telephony?.let { runCatching { it.isNetworkRoaming }.getOrDefault(false) } ?: false
            } else {
                false
            }
        val countryChanged =
            networkIsCellular && homeCountry != null && networkCountry != null && homeCountry != networkCountry

        return if (carrierReportsRoaming || countryChanged) {
            UpdateNetworkStatus.ROAMING_CONFIRMATION_REQUIRED
        } else {
            UpdateNetworkStatus.AUTOMATIC
        }
    }

    private fun normalizeCountry(value: String?): String? =
        value
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
}
