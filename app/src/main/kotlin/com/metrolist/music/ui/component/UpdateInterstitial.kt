/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metrolist.music.R
import com.metrolist.music.constants.PendingUpdateNotesKey
import com.metrolist.music.constants.PendingUpdateVersionNameKey
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.UpdateDownloadState
import com.metrolist.music.utils.UpdateDownloader
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Fixed New-Iride-UI palette: the announcement replaces the whole app, so it must not inherit
// the dynamic (artwork-tinted) scheme — pure black surface, white ink, white pill CTA.
private val InterstitialBlack = Color.Black
private val InterstitialInk = Color.White

/** How many changelog lines are shown at most — the list stays scannable, never a wall of text. */
private const val MAX_WHATS_NEW_LINES = 8

/**
 * Full-screen announcement shown on the launch AFTER an update was detected and silently
 * downloaded. Purely local: reads the persisted announcement metadata and polls the local
 * DownloadManager state, so it renders instantly and works offline.
 *
 * Two CTA shapes:
 *  - install permission already granted: one big pill button handing the finished APK to the
 *    system installer dialog;
 *  - first time ever: a single "Allow installation" button opening the system page; on return,
 *    the installer fires automatically so the user never taps twice.
 *
 * "Not now" dismisses for this release tag; the badge in Settings keeps informing afterwards.
 */
@Composable
fun UpdateInterstitialScreen(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val versionName = remember { context.dataStore[PendingUpdateVersionNameKey].orEmpty() }
    val notes = remember { context.dataStore[PendingUpdateNotesKey] }
    val whatsNewLines = remember(notes) { parseWhatsNewLines(notes.orEmpty()).take(MAX_WHATS_NEW_LINES) }

    var canInstall by remember { mutableStateOf(UpdateDownloader.canInstallPackages(context)) }
    var headingToPermissionSettings by remember { mutableStateOf(false) }

    val downloadState by produceState<UpdateDownloadState>(UpdateDownloadState.Idle) {
        while (isActive) {
            value = UpdateDownloader.queryDownloadState(context)
            delay(1_000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && headingToPermissionSettings) {
                headingToPermissionSettings = false
                if (UpdateDownloader.canInstallPackages(context)) {
                    canInstall = true
                    UpdateDownloader.promptInstall(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InterstitialBlack),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
        ) {
            Spacer(Modifier.weight(0.7f))

            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = InterstitialInk,
                modifier = Modifier.size(72.dp),
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.update_interstitial_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SpaceMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = InterstitialInk,
                textAlign = TextAlign.Center,
            )

            if (versionName.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.update_interstitial_version, versionName),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                    color = InterstitialInk.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }

            if (whatsNewLines.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.update_interstitial_whats_new),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = InterstitialInk.copy(alpha = 0.45f),
                    )
                    Spacer(Modifier.height(10.dp))
                    whatsNewLines.forEach { line ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                                color = InterstitialInk.copy(alpha = 0.45f),
                                modifier = Modifier.padding(end = 10.dp),
                            )
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                                color = InterstitialInk.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (canInstall) {
                    val ready = downloadState is UpdateDownloadState.ReadyToInstall
                    Button(
                        onClick = { UpdateDownloader.promptInstall(context) },
                        enabled = ready,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InterstitialInk,
                            contentColor = InterstitialBlack,
                            disabledContainerColor = InterstitialInk.copy(alpha = 0.25f),
                            disabledContentColor = InterstitialBlack.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.update_interstitial_update_now),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    when (downloadState) {
                        is UpdateDownloadState.Downloading ->
                            InterstitialCaption(stringResource(R.string.update_interstitial_preparing))
                        is UpdateDownloadState.Failed ->
                            InterstitialCaption(stringResource(R.string.update_download_failed))
                        else -> Unit
                    }
                } else {
                    Button(
                        onClick = {
                            headingToPermissionSettings = true
                            UpdateDownloader.openInstallPermissionSettings(context)
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InterstitialInk,
                            contentColor = InterstitialBlack,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.update_interstitial_enable_install),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = InterstitialInk.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = stringResource(R.string.update_interstitial_not_now),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InterstitialCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily),
        color = InterstitialInk.copy(alpha = 0.45f),
        textAlign = TextAlign.Center,
    )
}

/** Extracts plain changelog lines from GitHub markdown, dropping headings entirely. */
private fun parseWhatsNewLines(body: String): List<String> =
    body
        .replace("\r", "")
        .lines()
        .mapNotNull { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> null
                line.startsWith("#") -> null
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ->
                    stripInlineMarkdown(line.substring(2).trim())
                else -> null // prose paragraphs are skipped: bullets only
            }
        }

private fun stripInlineMarkdown(text: String): String =
    text
        .replace(Regex("\\*\\*(.+?)\\*\\*")) { it.groupValues[1] }
        .replace(Regex("\\*(.+?)\\*")) { it.groupValues[1] }
        .replace(Regex("`(.+?)`")) { it.groupValues[1] }
        .replace(Regex("\\[(.+?)]\\((.+?)\\)")) { it.groupValues[1] }
        .trim()
