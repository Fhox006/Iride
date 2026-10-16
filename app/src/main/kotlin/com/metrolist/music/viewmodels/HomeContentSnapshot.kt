package com.metrolist.music.viewmodels

import com.metrolist.music.utils.DeferredHomeContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Refresh jobs keep running; only publication to Home is deferred. Access on the main thread. */
class HomeContentSnapshot(private val scope: CoroutineScope) {
    private class Binding<T>(
        initial: T,
        hasContent: (T) -> Boolean,
        identity: (T) -> Any?,
        mergeWhileVisible: (T, T) -> T,
    ) {
        val content = DeferredHomeContent(initial, hasContent, identity, mergeWhileVisible)
        val displayed = MutableStateFlow(initial)

        fun setVisible(visible: Boolean) {
            displayed.value = content.setVisible(visible)
        }
    }

    private val bindings = mutableMapOf<String, Binding<*>>()
    private var visible = true

    fun setVisible(value: Boolean) {
        visible = value
        bindings.values.forEach { it.setVisible(value) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> state(
        key: String,
        source: StateFlow<T>,
        hasContent: (T) -> Boolean,
        identity: (T) -> Any? = { Unit },
        mergeWhileVisible: (T, T) -> T = { current, _ -> current },
    ): StateFlow<T> {
        bindings[key]?.let { return (it as Binding<T>).displayed.asStateFlow() }
        val binding = Binding(source.value, hasContent, identity, mergeWhileVisible)
        binding.setVisible(visible)
        bindings[key] = binding
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            source.collect { binding.displayed.value = binding.content.update(it) }
        }
        return binding.displayed.asStateFlow()
    }
}
