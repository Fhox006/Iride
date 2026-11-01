/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.rememberReducedMotion

val LocalMenuState = compositionLocalOf { MenuState() }
internal val LocalSongMenuPopupState = compositionLocalOf<MenuState?> { null }
internal val LocalMenuActionScope = compositionLocalOf<kotlinx.coroutines.CoroutineScope?> { null }

@Composable
internal fun rememberMenuActionScope(): kotlinx.coroutines.CoroutineScope {
    val fallback = androidx.compose.runtime.rememberCoroutineScope()
    return LocalMenuActionScope.current ?: fallback
}

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    internal var sourceTypography: androidx.compose.material3.Typography? = null
    internal var presentationTypography by
        mutableStateOf<androidx.compose.material3.Typography?>(null)
        private set

    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)
    var anchored by mutableStateOf(false)
        private set

    internal var pointerAnchor: androidx.compose.ui.geometry.Rect? = null
    internal var popupAnchor by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
        private set

    internal var pendingPressMotion: IrideFolioContextMotion? = null
    internal var popupMotion by mutableStateOf<IrideFolioContextMotion?>(null)
        private set

    fun show(content: @Composable ColumnScope.() -> Unit) =
        show(anchored = false, content = content)

    fun show(anchored: Boolean, content: @Composable ColumnScope.() -> Unit) {
        show(anchored, pendingPressMotion, content)
    }

    internal fun capturePressMotion(): IrideFolioContextMotion? = pendingPressMotion?.also {
        it.reserveForMenu()
    }

    internal fun showFromPress(
        motion: IrideFolioContextMotion?,
        content: @Composable ColumnScope.() -> Unit,
    ) = show(anchored = true, motion = motion, content = content)

    private fun show(
        anchored: Boolean,
        motion: IrideFolioContextMotion?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        presentationTypography = sourceTypography
        this.anchored = anchored
        val presentation = if (anchored) motion ?: IrideFolioContextMotion() else null
        if (popupMotion !== presentation) popupMotion?.abandon()
        popupMotion = presentation?.also { it.attachMenu() }
        popupAnchor = motion?.sourceBounds ?: pointerAnchor
        pointerAnchor = null
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) {
    var lastContent by remember { mutableStateOf(content) }

    LaunchedEffect(content) {
        if (isVisible) {
            lastContent = content
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (!sheetState.isVisible && !isVisible) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
        content = lastContent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    val focusManager = LocalFocusManager.current
    val actionScope = androidx.compose.runtime.rememberCoroutineScope()
    if (state.anchored) {
        val motion = state.popupMotion
        val reducedMotion = rememberReducedMotion()
        LaunchedEffect(motion, state.isVisible, motion?.panelReady, reducedMotion) {
            // Begin the shared pop only once the panel has real geometry to draw from.
            if (!state.isVisible || motion?.panelReady == true) {
                motion?.present(state.isVisible, reducedMotion)
            }
        }
        if (state.isVisible || motion?.retainMenu == true) {
            CompositionLocalProvider(
                LocalSongMenuPopupState provides state,
                LocalMenuActionScope provides actionScope,
            ) {
                Column { MenuPresentationContent(state) }
            }
        }
        return
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    AnimatedBottomSheet(
        isVisible = state.isVisible,
        onDismissRequest = {
            focusManager.clearFocus()
            state.isVisible = false
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.textPrimary,
        shape = FolioShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier =
                    Modifier.padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(FolioShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        },
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MenuPresentationContent(state)
        }
    }
}

/** Preserve the source type while the global host retains its captured, grainy backdrop. */
@Composable
private fun ColumnScope.MenuPresentationContent(state: MenuState) {
    val columnScope = this
    val typography = state.presentationTypography
    if (typography == null) state.content(columnScope)
    else
        MaterialTheme(typography = typography) {
            CompositionLocalProvider(
                LocalFolioTypewriterFont provides typography.bodyLarge.fontFamily,
                androidx.compose.material3.LocalTextStyle provides typography.bodyLarge,
            ) {
                state.content(columnScope)
            }
        }
}
