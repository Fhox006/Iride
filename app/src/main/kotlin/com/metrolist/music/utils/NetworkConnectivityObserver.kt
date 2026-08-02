/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple NetworkConnectivityObserver based on OuterTune's implementation
 * Provides network connectivity monitoring for auto-play functionality
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // StateFlow, not Channel.receiveAsFlow(): a channel-backed flow delivers each element to
    // exactly one collector, so with several collectors (MusicService, LyricsMenuViewModel,
    // ListenTogetherClient, App) every status change reached only one of them at random and the
    // others stayed stuck on their last seen value.
    private val _networkStatus = MutableStateFlow(true)
    val networkStatus: StateFlow<Boolean> = _networkStatus.asStateFlow()

    // Fires on every raw onAvailable/onLost/onCapabilitiesChanged, unconditionally — unlike
    // networkStatus above (a distinct-until-changed boolean) this does NOT collapse a Wi-Fi→mobile
    // handover to a no-op. Android hands the new network off before tearing the old one down, so
    // isCurrentlyConnected() reads true on both the onLost(wifi) and onAvailable(mobile) callbacks
    // and the boolean never flips — meaning the OkHttp pool never got evicted and every request
    // kept reusing sockets bound to the network that just disappeared.
    private val _networkChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val networkChanged: SharedFlow<Unit> = _networkChanged.asSharedFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkStatus.value = isCurrentlyConnected()
            _networkChanged.tryEmit(Unit)
        }

        override fun onLost(network: Network) {
            _networkStatus.value = isCurrentlyConnected()
            _networkChanged.tryEmit(Unit)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _networkStatus.value = isCurrentlyConnected()
            _networkChanged.tryEmit(Unit)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            _networkStatus.value = isCurrentlyConnected()
        } catch (e: Exception) {
            _networkStatus.value = true
        }
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    /**
     * Check current connectivity state synchronously
     */
    fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            // Check if we have internet capability. NET_CAPABILITY_VALIDATED is intentionally
            // not required here: Android can drop it transiently during signal degradation
            // (walking, cellular handover, in-call) while the connection is still usable, and
            // requiring it made lyrics/library/album fetches bail out immediately instead of
            // attempting the request.
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }
}
