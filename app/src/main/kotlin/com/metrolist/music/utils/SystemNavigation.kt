package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Suppress("DiscouragedApi")
internal fun readSystemNavigationMode(context: Context, view: View): SystemNavigationMode {
    val edgeInsets = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemGestures())
    val platformMode = runCatching {
        Settings.Secure.getInt(context.contentResolver, "navigation_mode")
    }.getOrNull() ?: runCatching {
        val id = context.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        if (id != 0) context.resources.getInteger(id) else null
    }.getOrNull()
    val xiaomiGestures = if (setOf(Build.MANUFACTURER.lowercase(), Build.BRAND.lowercase())
            .any { it in setOf("xiaomi", "redmi", "poco") }
    ) runCatching { Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar") }.getOrNull() else null
    return navigationModeFromSignals(
        edgeGestures = edgeInsets != null && (edgeInsets.left > 0 || edgeInsets.right > 0),
        platformMode = platformMode,
        xiaomiGestures = xiaomiGestures,
    )
}

/** Observe settings while foregrounded, including changes made in split-screen Settings. */
@Composable
internal fun rememberSystemNavigationMode(): State<SystemNavigationMode> {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val gestureInsets = WindowInsets.systemGestures
    val density = LocalDensity.current
    val mode = remember(context, view) { mutableStateOf(readSystemNavigationMode(context, view)) }
    LaunchedEffect(view, gestureInsets, density) {
        snapshotFlow {
            gestureInsets.getLeft(density, LayoutDirection.Ltr) to gestureInsets.getRight(density, LayoutDirection.Ltr)
        }.collect { mode.value = readSystemNavigationMode(context, view) }
    }
    DisposableEffect(context, view, owner, configuration) {
        fun refresh() { mode.value = readSystemNavigationMode(context, view) }
        var registered = false
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        fun register() {
            if (registered) return
            registered = true
            listOf(
                Settings.Secure.getUriFor("navigation_mode"),
                Settings.Global.getUriFor("force_fsg_nav_bar"),
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            ).forEach { uri ->
                runCatching { context.contentResolver.registerContentObserver(uri, false, observer) }
            }
        }
        fun unregister() {
            if (registered) context.contentResolver.unregisterContentObserver(observer)
            registered = false
        }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refresh() }
        view.addOnLayoutChangeListener(layoutListener)
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { register(); refresh() }
                Lifecycle.Event.ON_RESUME -> { refresh(); view.post { refresh() } }
                Lifecycle.Event.ON_STOP -> unregister()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) register()
        refresh()
        onDispose {
            unregister()
            view.removeOnLayoutChangeListener(layoutListener)
            owner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return mode
}

internal enum class NavigationSettingsDestination { Navigation, Display, Settings, Unavailable }

private fun navigationSettingsIntents(): List<Pair<Intent, NavigationSettingsDestination>> = buildList {
    add(Intent("com.android.settings.NAVIGATION_MODE_SETTINGS").setPackage("com.android.settings") to NavigationSettingsDestination.Navigation)
    add(Intent("android.settings.NAVIGATION_MODE_SETTINGS").setPackage("com.android.settings") to NavigationSettingsDestination.Navigation)
    if (Build.MANUFACTURER.equals("samsung", true)) {
        add(Intent().setClassName("com.android.settings", "com.android.settings.Settings\$NavigationBarSettingsActivity") to NavigationSettingsDestination.Navigation)
    }
    if (Build.MANUFACTURER.equals("samsung", true)) {
        add(Intent(Settings.ACTION_DISPLAY_SETTINGS) to NavigationSettingsDestination.Display)
    }
    add(Intent(Settings.ACTION_SETTINGS) to NavigationSettingsDestination.Settings)
}

internal fun canOpenNavigationSettings(context: Context): Boolean = navigationSettingsIntents()
    .filter { it.second == NavigationSettingsDestination.Navigation }
    .any { (intent, _) ->
        context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.let { it.exported && it.enabled } == true
    }

internal fun openNavigationSettings(context: Context): NavigationSettingsDestination {
    for ((intent, destination) in navigationSettingsIntents()) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return destination
        } catch (_: android.content.ActivityNotFoundException) {
            // OEMs do not all export a navigation settings entry point.
        } catch (_: SecurityException) {
            // Fall back when the matching OEM activity is private.
        }
    }
    return NavigationSettingsDestination.Unavailable
}
