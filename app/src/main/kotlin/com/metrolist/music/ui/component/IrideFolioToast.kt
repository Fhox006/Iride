package com.metrolist.music.ui.component

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Drop-in Toast facade; in foreground, messages use Folio's in-app surface. */
class IrideFolioToast private constructor(
    private val context: Context,
    private val message: CharSequence,
    private val duration: Int,
) {
    fun show() {
        IrideFolioToastCenter.show(context, message, duration)
    }

    companion object {
        const val LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT
        const val LENGTH_LONG = android.widget.Toast.LENGTH_LONG

        fun makeText(context: Context, message: CharSequence, duration: Int) =
            IrideFolioToast(context, message, duration)

        fun makeText(context: Context, @StringRes messageRes: Int, duration: Int) =
            IrideFolioToast(context, context.getText(messageRes), duration)
    }
}

/** Keeps Toast call sites working outside MainActivity, where no Compose host is present. */
typealias Toast = IrideFolioToast

private data class IrideFolioToastMessage(
    val id: Long,
    val text: String,
    val durationMillis: Long,
)

private object IrideFolioToastCenter {
    private val nextId = AtomicLong()
    private val hostStarted = AtomicBoolean(false)
    private val messages = Channel<IrideFolioToastMessage>(Channel.UNLIMITED)

    val events = messages.receiveAsFlow()

    fun setHostStarted(started: Boolean) {
        hostStarted.set(started)
        if (!started) {
            while (messages.tryReceive().isSuccess) Unit
        }
    }

    fun show(context: Context, message: CharSequence, duration: Int) {
        val durationMillis = if (duration == IrideFolioToast.LENGTH_LONG) 3_500L else 2_000L
        val event = IrideFolioToastMessage(nextId.incrementAndGet(), message.toString(), durationMillis)
        if (hostStarted.get()) {
            messages.trySend(event)
        } else {
            android.widget.Toast.makeText(context, message, duration).show()
        }
    }
}

@Composable
fun IrideFolioToastHost(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var hostStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var currentMessage by remember { mutableStateOf<IrideFolioToastMessage?>(null) }

    DisposableEffect(lifecycleOwner) {
        var attached = false
        fun updateStarted(started: Boolean) {
            hostStarted = started
            if (attached != started) {
                attached = started
                IrideFolioToastCenter.setHostStarted(started)
            }
            if (!started) currentMessage = null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> updateStarted(true)
                Lifecycle.Event.ON_STOP -> updateStarted(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            updateStarted(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            updateStarted(false)
        }
    }

    LaunchedEffect(hostStarted) {
        if (!hostStarted) {
            currentMessage = null
            return@LaunchedEffect
        }
        IrideFolioToastCenter.events.collect { message ->
            currentMessage = message
            delay(message.durationMillis)
            if (currentMessage?.id == message.id) currentMessage = null
        }
    }

    AnimatedVisibility(
        visible = currentMessage != null,
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp).widthIn(max = 520.dp),
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
    ) {
        currentMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = IrideFolio.ControlsShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).clip(CircleShape),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
