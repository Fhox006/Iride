package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo

private object FrostBackdropCaptureKey

internal fun Modifier.frostBackdropCapture(backdrop: FrostBackdrop): Modifier =
    then(FrostBackdropCaptureElement(backdrop))

private data class FrostBackdropCaptureElement(val backdrop: FrostBackdrop) :
    ModifierNodeElement<FrostBackdropCaptureNode>() {
    override fun create() = FrostBackdropCaptureNode(backdrop)

    override fun update(node: FrostBackdropCaptureNode) = node.updateBackdrop(backdrop)

    override fun InspectorInfo.inspectableProperties() {
        name = "frostBackdropCapture"
    }
}

private class FrostBackdropCaptureNode(private var backdrop: FrostBackdrop) :
    Modifier.Node(), DrawModifierNode, TraversableNode {
    override val traverseKey: Any = FrostBackdropCaptureKey
    private val onInvalidated: () -> Unit = { invalidateDraw() }
    private val onBackgroundInvalidated: () -> Unit = { invalidateCapture() }

    override fun onAttach() = subscribe()

    override fun onDetach() = unsubscribe()

    private fun subscribe() {
        backdrop.addInvalidationListener(onInvalidated)
        backdrop.background?.addInvalidationListener(onBackgroundInvalidated)
    }

    private fun unsubscribe() {
        backdrop.removeInvalidationListener(onInvalidated)
        backdrop.background?.removeInvalidationListener(onBackgroundInvalidated)
    }

    fun updateBackdrop(newBackdrop: FrostBackdrop) {
        if (backdrop === newBackdrop) return
        if (isAttached) unsubscribe()
        backdrop = newBackdrop
        if (isAttached) {
            subscribe()
            invalidateCapture()
        }
    }

    fun invalidateCapture() = backdrop.invalidateRecording()

    override fun ContentDrawScope.draw() = drawContent()
}

/** Observe animations across retained graphics layers without scheduling an extra frame clock. */
internal fun Modifier.invalidateFrostOnChanges(readChanges: () -> Unit): Modifier =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) this
    else then(FrostBackdropChangesElement(readChanges))

private data class FrostBackdropChangesElement(val readChanges: () -> Unit) :
    ModifierNodeElement<FrostBackdropChangesNode>() {
    override fun create() = FrostBackdropChangesNode(readChanges)

    override fun update(node: FrostBackdropChangesNode) = node.updateReads(readChanges)

    override fun InspectorInfo.inspectableProperties() {
        name = "invalidateFrostOnChanges"
    }
}

private class FrostBackdropChangesNode(private var readChanges: () -> Unit) :
    Modifier.Node(), ObserverModifierNode {
    override fun onAttach() = onObservedReadsChanged()

    fun updateReads(newReadChanges: () -> Unit) {
        readChanges = newReadChanges
        if (isAttached) onObservedReadsChanged()
    }

    override fun onObservedReadsChanged() {
        observeReads(readChanges)
        // A child can animate inside its own layer without redrawing any capture above it.
        traverseAncestors(FrostBackdropCaptureKey) {
            (it as FrostBackdropCaptureNode).invalidateCapture()
            true
        }
    }
}
