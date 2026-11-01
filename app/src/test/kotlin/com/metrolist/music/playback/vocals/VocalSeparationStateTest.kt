package com.metrolist.music.playback.vocals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocalSeparationStateTest {
    @Test fun `download at zero progress has visible activity before the first network bytes`() {
        val state = VocalSeparationState(phase = VocalPreparation.DownloadingRuntime, downloadRequired = true)
        assertTrue(state.downloading)
        assertFalse(state.active)
        assertFalse(state.starting)
    }

    @Test fun `completed inference still shows waiting until audio covers the current position`() {
        val state = VocalSeparationState(phase = VocalPreparation.Ready, voiceLevel = 0f)
        assertTrue(state.starting)
        assertFalse(state.active)
        val audible = state.copy(audioReady = true)
        assertFalse(audible.starting)
        assertTrue(audible.active)
        val afterSeek = audible.copy(audioReady = false)
        assertTrue(afterSeek.starting)
        assertFalse(afterSeek.active)
    }

    @Test fun `original audio is inactive even with separated windows in memory`() {
        val state = VocalSeparationState(phase = VocalPreparation.Ready, voiceLevel = 1f, audioReady = true)
        assertFalse(state.starting)
        assertFalse(state.active)
        assertFalse(state.downloading)
        assertFalse(state.copy(phase = VocalPreparation.Separating, audioReady = false).starting)
    }

    @Test fun `failed download keeps installation action available without a loading indicator`() {
        val state = VocalSeparationState(phase = VocalPreparation.Error, downloadRequired = true)
        assertTrue(state.downloadRequired)
        assertFalse(state.downloading)
        assertFalse(state.starting)
        assertFalse(state.active)
    }
}
