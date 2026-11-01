package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.viewmodels.HomeContentSnapshot
import kotlinx.coroutines.flow.StateFlow

val LocalHomeContentSnapshot = staticCompositionLocalOf<HomeContentSnapshot?> { null }

@Composable
fun <T> StateFlow<T>.collectDeferredHomeState(
    key: String,
    identity: (T) -> Any? = { Unit },
    mergeWhileVisible: (T, T) -> T = { current, _ -> current },
    hasContent: (T) -> Boolean = { value ->
        when (value) {
            null -> false
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    },
): State<T> {
    val snapshot = LocalHomeContentSnapshot.current
    val displayed = remember(snapshot, key, this) {
        snapshot?.state(key, this, hasContent, identity, mergeWhileVisible) ?: this
    }
    return displayed.collectAsStateWithLifecycle()
}
