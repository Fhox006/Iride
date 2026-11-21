package com.metrolist.music.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.metrolist.music.R
import com.metrolist.music.constants.ButtonNavigationAcceptedKey
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioDialog
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun ButtonNavigationConfirmation(onDismiss: () -> Unit, onContinue: suspend () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) reveal.snapTo(1f)
        else reveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    IrideFolioDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        modifier = Modifier.graphicsLayer {
            alpha = reveal.value
            scaleX = 0.96f + reveal.value * 0.04f
            scaleY = scaleX
        },
    ) {
        Column(Modifier.padding(IrideFolio.DialogContentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.navigation_choice_title),
                    style = iridePageTitleTextStyle().copy(fontSize = 30.sp, lineHeight = 34.sp),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IrideOutlineIconButton(
                    onClick = onDismiss,
                    icon = R.drawable.close,
                    contentDescription = stringResource(R.string.navigation_choice_close),
                    enabled = !saving,
                )
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.navigation_choice_heart), color = MaterialTheme.colorScheme.textPrimary)
                Text(stringResource(R.string.navigation_choice_motion), color = MaterialTheme.colorScheme.textSecondary)
                Text(stringResource(R.string.navigation_choice_support), color = MaterialTheme.colorScheme.textSecondary)
                Text(
                    stringResource(R.string.navigation_choice_alternatives),
                    color = MaterialTheme.colorScheme.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                AlternativeMusicApps()
                if (saveFailed) {
                    Text(stringResource(R.string.navigation_choice_save_failed), color = MaterialTheme.colorScheme.textSecondary)
                }
            }
            Spacer(Modifier.height(IrideFolio.DialogActionsTopSpacing))
            ExperienceButton(
                stringResource(R.string.navigation_choice_confirm),
                enabled = !saving,
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            try {
                                context.dataStore.edit { it[ButtonNavigationAcceptedKey] = true }
                                onContinue()
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

private data class AlternativeApp(val name: String, val icon: Int, val repository: String)

private val alternativeApps = listOf(
    AlternativeApp("Archive Tune", R.drawable.ic_alternative_archive, "https://github.com/rukamori/ArchiveTune"),
    AlternativeApp("SimpMusic", R.drawable.ic_alternative_simpmusic, "https://github.com/maxrave-dev/SimpMusic"),
    AlternativeApp("Metrolist", R.drawable.ic_alternative_metrolist, "https://github.com/MetrolistGroup/Metrolist"),
)

/** Same slow floating cadence as the YouTube account illustration, with staggered phases. */
@Composable
private fun AlternativeMusicApps() {
    val phase = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uriHandler = LocalUriHandler.current
    val ink = MaterialTheme.colorScheme.textPrimary
    val paper = MaterialTheme.colorScheme.surfaceContainerHigh
    var linkFailed by remember { mutableStateOf(false) }
    LaunchedEffect(reducedMotion, lifecycle) {
        if (reducedMotion) phase.snapTo(0f)
        else lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                phase.snapTo(0f)
                phase.animateTo(1f, tween(5200, easing = LinearEasing))
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        alternativeApps.forEachIndexed { index, app ->
            Column(
                Modifier.weight(1f).clip(FolioShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.navigation_choice_open_repository, app.name)) {
                        try {
                            uriHandler.openUri(app.repository)
                        } catch (_: IllegalArgumentException) {
                            linkFailed = true
                        }
                    }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.widthIn(max = 72.dp).fillMaxWidth().aspectRatio(1f).graphicsLayer {
                        val wave = if (reducedMotion) 0f else sin(phase.value * 2f * PI.toFloat() + index * 2f)
                        translationY = wave * 3.dp.toPx()
                        rotationZ = (index - 1) * 4f + wave * 1.5f
                    }.clip(FolioShape(20.dp)).background(paper)
                        .background(ink.copy(alpha = 0.045f))
                        .border(1.dp, ink.copy(alpha = 0.25f), FolioShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(app.icon), null, Modifier.fillMaxSize().padding(5.dp), tint = ink)
                }
                Text(app.name, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
            }
        }
    }
    if (linkFailed) Text(stringResource(R.string.navigation_choice_browser_failed), color = MaterialTheme.colorScheme.textSecondary)
}
