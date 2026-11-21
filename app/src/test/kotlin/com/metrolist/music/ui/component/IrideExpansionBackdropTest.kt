package com.metrolist.music.ui.component

import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrideExpansionBackdropTest {
    private val graphics = object : GraphicsContext {
        override fun createGraphicsLayer(): GraphicsLayer = error("Capture scheduling must not allocate GPU layers")
        override fun releaseGraphicsLayer(layer: GraphicsLayer) = Unit
    }

    @Test
    fun thumbnailRemainsVisibleUntilTheFlightStartsAndAtItsReturnEndpoint() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val backdrop = IrideExpansionBackdrop(graphics, scope, Density(1f), LayoutDirection.Ltr)
        val progress = mutableStateOf(0f)
        try {
            backdrop.inFlight = true
            assertFalse(backdrop.artworkInFlight)
            backdrop.progress = progress
            assertFalse(backdrop.artworkInFlight)
            progress.value = 0.01f
            assertTrue(backdrop.artworkInFlight)
            progress.value = 0f
            assertFalse(backdrop.artworkInFlight)
        } finally {
            backdrop.dispose()
            scope.cancel()
        }
    }

    @Test
    fun captureCannotFreezeTheSourceBeforeItsThumbnailIsHidden() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val backdrop = IrideExpansionBackdrop(graphics, scope, Density(1f), LayoutDirection.Ltr)
        val progress = mutableStateOf(0f)
        try {
            backdrop.inFlight = true
            backdrop.progress = progress
            backdrop.capture("source")
            backdrop.recorded("source")
            assertTrue(backdrop.capturePending("source"))
            progress.value = 0.01f
            backdrop.recorded("source")
            assertFalse(backdrop.capturePending("source"))
        } finally {
            backdrop.dispose()
            scope.cancel()
        }
    }

    @Test
    fun sourceCaptureWaitsForItsOwnDrawBeforeReadingBack() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val backdrop = IrideExpansionBackdrop(graphics, scope, Density(1f), LayoutDirection.Ltr)
        try {
            backdrop.capture("source")
            assertTrue(backdrop.capturePending("source"))
            backdrop.recorded("destination")
            assertTrue(backdrop.capturePending("source"))
            backdrop.recorded("source")
            assertFalse(backdrop.capturePending("source"))
        } finally {
            backdrop.dispose()
            scope.cancel()
        }
    }

    @Test
    fun aNewOpeningRequestsAFreshCaptureAndDisposalClearsIt() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val backdrop = IrideExpansionBackdrop(graphics, scope, Density(1f), LayoutDirection.Ltr)
        try {
            backdrop.capture("source")
            backdrop.recorded("source")
            backdrop.capture("source")
            assertTrue(backdrop.capturePending("source"))
            backdrop.dispose()
            assertFalse(backdrop.capturePending("source"))
        } finally {
            scope.cancel()
        }
    }
}
