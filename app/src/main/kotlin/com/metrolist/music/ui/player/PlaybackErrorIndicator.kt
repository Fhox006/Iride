package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.ui.theme.textSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PLAYBACK_ERROR_STARTUP_GRACE_MS = 3_000L

@Composable
internal fun PlaybackErrorIndicator(
    hasPlaybackError: Boolean,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val canShow by produceState(initialValue = false, lifecycle) {
        var revealJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    value = false
                    revealJob?.cancel()
                    revealJob = launch {
                        delay(PLAYBACK_ERROR_STARTUP_GRACE_MS)
                        value = true
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    revealJob?.cancel()
                    value = false
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        awaitDispose {
            lifecycle.removeObserver(observer)
            revealJob?.cancel()
        }
    }

    AnimatedVisibility(
        visible = hasPlaybackError && canShow,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = MaterialTheme.colorScheme.textSecondary,
            strokeWidth = 1.5.dp,
        )
    }
}
