package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkStateTest {
    @Test
    fun captivePortalStaysOfflineUntilValidated() {
        val state = DefaultNetworkState<String>()
        assertTrue(state.available("wifi"))
        assertFalse(state.connected)
        assertFalse(state.capabilitiesChanged("wifi", hasInternet = true, validated = false))
        assertTrue(state.capabilitiesChanged("wifi", hasInternet = true, validated = true))
        assertTrue(state.connected)
    }

    @Test
    fun wifiToCellularHandoverEmitsChangeEvenWhenBothHaveInternet() {
        val state = DefaultNetworkState("wifi", initiallyConnected = true)
        assertTrue(state.available("cellular"))
        assertTrue(state.capabilitiesChanged("cellular", hasInternet = true, validated = true))
        assertTrue(state.connected)
    }

    @Test
    fun oldNetworkLossCannotInvalidateNewDefaultNetwork() {
        val state = DefaultNetworkState("wifi", initiallyConnected = true)
        state.available("cellular")
        state.capabilitiesChanged("cellular", hasInternet = true, validated = true)
        assertFalse(state.lost("wifi"))
        assertFalse(state.capabilitiesChanged("wifi", hasInternet = false, validated = false))
        assertTrue(state.connected)
    }

    @Test
    fun validationLossAndRecoveryOnSameNetworkBothEmitChange() {
        val state = DefaultNetworkState("wifi", initiallyConnected = true)
        assertTrue(state.capabilitiesChanged("wifi", hasInternet = true, validated = false))
        assertFalse(state.connected)
        assertTrue(state.capabilitiesChanged("wifi", hasInternet = true, validated = true))
        assertTrue(state.connected)
    }

    @Test
    fun repeatedCapabilitiesDoNotTriggerAccountReloads() {
        val state = DefaultNetworkState("wifi", initiallyConnected = true)
        assertFalse(state.available("wifi"))
        assertFalse(state.capabilitiesChanged("wifi", hasInternet = true, validated = true))
        assertTrue(state.connected)
    }

    @Test
    fun lossAndReconnectOfSameNetworkRecoverConnectivity() {
        val state = DefaultNetworkState("wifi", initiallyConnected = true)
        assertTrue(state.lost("wifi"))
        assertFalse(state.connected)
        assertFalse(state.capabilitiesChanged("wifi", hasInternet = true, validated = true))
        assertTrue(state.available("wifi"))
        assertTrue(state.capabilitiesChanged("wifi", hasInternet = true, validated = true))
        assertTrue(state.connected)
    }
}
