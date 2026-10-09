package com.metrolist.music.utils

/** Keep the latest refresh pending until the user leaves Home. Empty shelves may load once. */
internal class DeferredHomeContent<T>(
    initial: T,
    private val hasContent: (T) -> Boolean,
    private val identity: (T) -> Any? = { Unit },
) {
    var displayed: T = initial
        private set
    private var pending: T = initial
    private var visible = true

    fun update(value: T): T {
        pending = value
        if (!visible || !hasContent(displayed) || identity(displayed) != identity(value)) displayed = value
        return displayed
    }

    fun setVisible(value: Boolean): T {
        visible = value
        if (!visible) displayed = pending
        return displayed
    }
}
