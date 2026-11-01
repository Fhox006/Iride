package com.metrolist.music.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.Android14AcknowledgedBuildKey
import com.metrolist.music.constants.GestureSetupRequiredKey
import com.metrolist.music.constants.GestureTutorialCompletedKey
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.*
import kotlinx.coroutines.launch

internal val LocalSystemNavigationMode = staticCompositionLocalOf { SystemNavigationMode.Unknown }

/** A modal surface isolates the gate from deep links, player controls and underlying dialogs. */
@Composable
internal fun DeviceExperienceGate(mode: SystemNavigationMode) {
    val context = LocalContext.current
    val onboardingCompleted by rememberPreference(OnboardingCompletedKey, false)
    val tutorialCompleted by rememberPreference(GestureTutorialCompletedKey, false)
    val acceptedBuild by rememberPreference(Android14AcknowledgedBuildKey, "")
    val scope = rememberCoroutineScope()
    val buildIdentity = remember(context) {
        // lastUpdateTime also catches an installed update that reuses the same version code.
        val installedAt = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        "${BuildConfig.VERSION_CODE}:$installedAt"
    }
    var accepting by remember { mutableStateOf(false) }
    var acceptanceFailed by remember { mutableStateOf(false) }
    val panel = deviceExperiencePanel(
        Build.VERSION.SDK_INT, buildIdentity, acceptedBuild, onboardingCompleted, tutorialCompleted, mode,
    )
    LaunchedEffect(mode) {
        if (needsNavigationSetup(mode)) {
            context.dataStore.edit {
                it[GestureTutorialCompletedKey] = false
                it[GestureSetupRequiredKey] = true
            }
        }
    }
    if (panel != DeviceExperiencePanel.None) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            if (panel == DeviceExperiencePanel.Android14) {
                Android14Warning(
                    accepting = accepting,
                    saveFailed = acceptanceFailed,
                    onExit = { context.experienceActivity()?.finishAndRemoveTask() },
                    onAccept = {
                        if (!accepting) {
                            accepting = true
                            scope.launch {
                                try {
                                    context.dataStore.edit { it[Android14AcknowledgedBuildKey] = buildIdentity }
                                } catch (_: java.io.IOException) {
                                    acceptanceFailed = true
                                } finally {
                                    accepting = false
                                }
                            }
                        }
                    },
                )
            } else {
                GestureNavigationFlow(mode = mode, onComplete = {})
            }
        }
    }
}

private tailrec fun Context.experienceActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.experienceActivity()
    else -> null
}

@Composable
private fun Android14Warning(accepting: Boolean, saveFailed: Boolean, onExit: () -> Unit, onAccept: () -> Unit) {
    ExperiencePage(
        title = stringResource(R.string.experience_android14_title),
        description = stringResource(R.string.experience_android14_body),
        illustration = { NavigationMotionIllustration(NavigationDemo.Compatibility) },
        actions = {
            ExperienceButton(stringResource(R.string.experience_exit), onExit)
            ExperienceButton(stringResource(R.string.experience_android14_continue), onAccept, secondary = true, enabled = !accepting)
            if (saveFailed) ExperienceBody(stringResource(R.string.experience_save_failed))
        },
    )
}

@Composable
internal fun GestureNavigationFlow(
    mode: SystemNavigationMode = LocalSystemNavigationMode.current,
    applyInsets: Boolean = true,
    onComplete: suspend () -> Unit,
) {
    val context = LocalContext.current
    val activatedHere by rememberPreference(GestureSetupRequiredKey, false)
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    BackHandler(enabled = LocalExperienceVisible.current) { context.experienceActivity()?.finish() }
    AnimatedContent(
        targetState = mode == SystemNavigationMode.Gestures,
        transitionSpec = {
            (fadeIn(tween(if (reducedMotion) 0 else 320)) +
                scaleIn(tween(if (reducedMotion) 0 else 420), initialScale = 0.96f)) togetherWith
                (fadeOut(tween(if (reducedMotion) 0 else 180)) +
                    scaleOut(tween(if (reducedMotion) 0 else 240), targetScale = 0.96f))
        },
        label = "gesture setup to tutorial",
    ) { gesturesEnabled ->
        if (!gesturesEnabled) {
            GestureSetupPage(applyInsets = applyInsets)
        } else {
            GestureTutorialPage(
                applyInsets = applyInsets,
                activatedHere = activatedHere,
                saving = saving,
                saveFailed = saveFailed,
                onComplete = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            try {
                                context.dataStore.edit {
                                    it[GestureTutorialCompletedKey] = true
                                    it[GestureSetupRequiredKey] = false
                                }
                                onComplete()
                            } catch (_: java.io.IOException) {
                                saveFailed = true
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
internal fun GestureSetupPage(applyInsets: Boolean = true) {
    val context = LocalContext.current
    val guide = remember { navigationSettingsGuide(Build.MANUFACTURER, Build.BRAND, Build.VERSION.SDK_INT) }
    var destination by rememberSaveable { mutableStateOf<NavigationSettingsDestination?>(null) }
    val directSettings = remember(context) { canOpenNavigationSettings(context) }
    val showPath = !directSettings || (destination != null && destination != NavigationSettingsDestination.Navigation)
    val path = if (guide.verifiedFamily) {
        (guide.steps + guide.selection).map { stringResource(it.resource) }.joinToString("  /  ")
    } else stringResource(R.string.experience_search_settings)
    ExperiencePage(
        applyInsets = applyInsets,
        title = stringResource(R.string.experience_setup_title),
        description = stringResource(R.string.experience_setup_body),
        illustration = { NavigationMotionIllustration(NavigationDemo.Settings) },
        details = {

            if (destination == NavigationSettingsDestination.Unavailable) {
                ExperienceBody(stringResource(R.string.experience_settings_unavailable))
            }
        },
        actions = {
            if (showPath) ExperienceBody(path)
            ExperienceButton(
                stringResource(R.string.experience_open_settings),
                onClick = { destination = openNavigationSettings(context) },
            )
        },
    )
}

@Composable
private fun GestureTutorialPage(
    applyInsets: Boolean,
    activatedHere: Boolean,
    saving: Boolean,
    saveFailed: Boolean,
    onComplete: () -> Unit,
) {
    GestureWelcomePeek {
        ExperiencePage(
            applyInsets = applyInsets,
            title = stringResource(if (activatedHere) R.string.experience_welcome_gestures else R.string.experience_already_gestures),
            description = stringResource(R.string.experience_back_body),
            illustration = { NavigationMotionIllustration(NavigationDemo.Back) },
            details = {
                if (saveFailed) ExperienceBody(stringResource(R.string.experience_save_failed))
            },
            actions = {
                ExperienceButton(stringResource(R.string.experience_enter), onComplete, enabled = !saving)
            },
        )
    }
}

@Composable
internal fun ExperiencePage(
    title: String,
    description: String? = null,
    illustration: @Composable () -> Unit,
    applyInsets: Boolean = true,
    illustrationMaxHeight: Dp? = null,
    details: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .then(if (applyInsets) Modifier.safeDrawingPadding() else Modifier),
    ) {
        val compact = maxHeight < 640.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        val illustrationHeight = (maxHeight * 0.42f).coerceIn(270.dp, 350.dp)
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxSize().align(Alignment.TopCenter)
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Text(
                title,
                style = iridePageTitleTextStyle().copy(fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = (-1.4).sp),
                color = MaterialTheme.colorScheme.textPrimary,
                modifier = Modifier.padding(top = 12.dp).semantics { heading() },
            )
            Box(
                Modifier.fillMaxWidth().then(
                    if (illustrationMaxHeight != null) Modifier.height(illustrationMaxHeight).padding(vertical = 16.dp)
                    else if (compact) Modifier.height(illustrationHeight).padding(vertical = 24.dp)
                    else Modifier.weight(1f).padding(vertical = 28.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxWidth().heightIn(max = 360.dp).fillMaxHeight()) { illustration() }
            }
            if (description != null) {
                ExperienceBody(description)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                details()
            }
            if (illustrationMaxHeight != null && !compact) Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                actions()
            }
        }
    }
}

@Composable
private fun ExperienceBody(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, letterSpacing = 0.sp),
        color = MaterialTheme.colorScheme.textSecondary,
    )
}

@Composable
internal fun ExperienceButton(text: String, onClick: () -> Unit, secondary: Boolean = false, enabled: Boolean = true) {
    if (secondary) {
        IrideFolioSecondaryButton(onClick, Modifier.fillMaxWidth(), enabled) { Text(text, textAlign = TextAlign.Center) }
    } else {
        IrideFolioPrimaryButton(onClick, Modifier.fillMaxWidth(), enabled) { Text(text, textAlign = TextAlign.Center) }
    }
}

private val NavigationSettingsLabel.resource: Int
    @StringRes get() = when (this) {
        NavigationSettingsLabel.Settings -> R.string.experience_label_settings
        NavigationSettingsLabel.System -> R.string.experience_label_system
        NavigationSettingsLabel.Gestures -> R.string.experience_label_gestures
        NavigationSettingsLabel.NavigationMode -> R.string.experience_label_navigation_mode
        NavigationSettingsLabel.GestureNavigation -> R.string.experience_label_gesture_navigation
        NavigationSettingsLabel.Display -> R.string.experience_label_display
        NavigationSettingsLabel.NavigationBar -> R.string.experience_label_navigation_bar
        NavigationSettingsLabel.SwipeGestures -> R.string.experience_label_swipe_gestures
        NavigationSettingsLabel.HomeScreen -> R.string.experience_label_home_screen
        NavigationSettingsLabel.SystemNavigation -> R.string.experience_label_system_navigation
        NavigationSettingsLabel.SystemUpdates -> R.string.experience_label_system_updates
        NavigationSettingsLabel.AdditionalSettings -> R.string.experience_label_additional_settings
    }
