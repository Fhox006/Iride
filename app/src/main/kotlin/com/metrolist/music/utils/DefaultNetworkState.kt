package com.metrolist.music.utils

internal class DefaultNetworkState<T>(
    private var network: T? = null,
    initiallyConnected: Boolean = false,
) {
    var connected: Boolean = initiallyConnected
        private set

    fun available(network: T): Boolean {
        if (this.network == network) return false
        this.network = network
        connected = false
        return true
    }

    fun capabilitiesChanged(network: T, hasInternet: Boolean, validated: Boolean): Boolean {
        if (this.network != network) return false
        val connected = hasInternet && validated
        if (this.connected == connected) return false
        this.connected = connected
        return true
    }

    fun lost(network: T): Boolean {
        if (this.network != network) return false
        this.network = null
        connected = false
        return true
    }
}
