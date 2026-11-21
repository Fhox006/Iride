package com.metrolist.music.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize

@Stable
class IrideBridgeState {
    var miniPlayerAlpha by mutableStateOf(1f)
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)

    // Live playback fraction, reported every frame by the mini pill's own ring (PillPlayButton) —
    // lets the bridge overlay's ring (drawn on the moving cover, see IrideMiniPlayerBridgeOverlay)
    // start already in sync instead of snapping from 0 the instant the expand begins.
    var progress by mutableStateOf(0f)

    var panelActive by mutableStateOf(false)
    // True while the lyrics FullScreenLyricsDialog (a separate Android Window drawn on top of
    // everything) is showing. MainActivity's app-peek-height "tap to collapse" catcher sits in
    // the same Activity window underneath that dialog and has no other way to know a fullscreen
    // overlay is up — without this flag a tap landing in that strip during the dialog's
    // open/close transition collapses the whole player and drops the lyrics with it.
    var lyricsFullScreenActive by mutableStateOf(false)
}

internal fun Modifier.irideReportRect(target: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { target(Rect(it.positionInWindow(), it.size.toSize())) }
