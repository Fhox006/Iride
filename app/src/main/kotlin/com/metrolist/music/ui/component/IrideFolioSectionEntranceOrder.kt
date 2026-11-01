package com.metrolist.music.ui.component

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

internal val LocalFolioSectionEntranceOrder =
    staticCompositionLocalOf<IrideFolioSectionEntranceOrder?> { null }

/** Order only visible blocks: a fast fling never waits for sections already above the viewport. */
internal class IrideFolioSectionEntranceOrder {
    private data class Slot(val top: Float, val visible: Boolean, val ready: Boolean)

    private val slots = mutableStateMapOf<String, Slot>()
    private val started = mutableStateMapOf<String, Boolean>()

    fun position(key: String, top: Float, visible: Boolean, ready: Boolean) {
        slots[key] = Slot(top, visible, ready)
    }

    fun readiness(key: String, ready: Boolean) {
        slots[key]?.let { slots[key] = it.copy(ready = ready) }
    }

    fun presented(key: String) {
        started[key] = true
    }

    fun remove(key: String) {
        slots.remove(key)
        started.remove(key)
    }

    private fun canStart(key: String): Boolean {
        if (started[key] == true) return true
        val next =
            slots.entries
                .filter { it.value.visible && started[it.key] != true }
                .minWithOrNull(compareBy({ it.value.top }, { it.key }))
        return next?.key == key && next.value.ready
    }

    suspend fun awaitTurn(key: String) {
        // Collect all layout positions before choosing the first block, not callback arrival order.
        withFrameNanos {}
        while (true) {
            snapshotFlow { canStart(key) }.first { it }
            if (started[key] == true) return
            delay(32)
            // Scroll or new content may have changed the visible order during this short stagger.
            if (canStart(key)) {
                started[key] = true
                return
            }
        }
    }
}
