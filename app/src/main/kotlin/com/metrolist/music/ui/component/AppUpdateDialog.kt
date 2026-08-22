/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.R
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.UpdateDownloadState
import com.metrolist.music.utils.UpdateDownloader
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import sv.lib.squircleshape.SquircleShape

private fun ReleaseInfo.isPreviewBuild(): Boolean {
    if (preRelease) return true
    val combined = (tagName + versionName).lowercase()
    return listOf("alpha", "beta", "rc", "pre").any { combined.contains(it) }
}

@Composable
fun AppUpdateDialog(
    releaseInfo: ReleaseInfo,
    downloadUrl: String?,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
) {
    val context = LocalContext.current
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var canInstallPackages by remember { mutableStateOf(UpdateDownloader.canInstallPackages(context)) }
    LaunchedEffect(downloadUrl) {
        while (isActive) {
            downloadState = UpdateDownloader.queryDownloadState(context)
            canInstallPackages = UpdateDownloader.canInstallPackages(context)
            delay(1_000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = if (topNavigationBarEnabled) RoundedCornerShape(16.dp) else SquircleShape(radius = 28.dp, cornerSmoothing = 0.5f),
            color = if (topNavigationBarEnabled) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = if (topNavigationBarEnabled) 0.dp else AlertDialogDefaults.TonalElevation,
            border = if (topNavigationBarEnabled) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
        ) {
            val titleColor = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.onSurface
            val bodyColor = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            val mutedColor = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
            val titleStyle = if (topNavigationBarEnabled) {
                MaterialTheme.typography.headlineSmall.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.headlineSmall
            }
            val bodyStyle = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            }
            val smallStyle = if (topNavigationBarEnabled) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.bodySmall
            }

            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_title_new_version),
                    style = titleStyle,
                    color = titleColor,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.update_dialog_version_available, releaseInfo.versionName),
                    style = bodyStyle,
                    color = bodyColor,
                )
                if (releaseInfo.isPreviewBuild()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_experimental_build),
                        style = smallStyle,
                        color = bodyColor,
                    )
                }
                when (val state = downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.update_state_downloading_percent, state.progressPercent),
                            style = smallStyle,
                            color = bodyColor,
                        )
                    }
                    is UpdateDownloadState.Failed -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.reason ?: stringResource(R.string.update_download_failed),
                            style = smallStyle,
                            color = bodyColor,
                        )
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.update_state_ready_to_install),
                            style = smallStyle,
                            color = bodyColor,
                        )
                        if (!canInstallPackages) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.update_permission_explanation),
                                style = smallStyle,
                                color = mutedColor,
                            )
                        }
                    }
                    is UpdateDownloadState.Idle -> Unit
                }
                if (downloadUrl == null && downloadState != UpdateDownloadState.ReadyToInstall) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_no_compatible_package),
                        style = smallStyle,
                        color = mutedColor,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = if (topNavigationBarEnabled) {
                            ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                        } else {
                            ButtonDefaults.textButtonColors()
                        },
                    ) {
                        Text(
                            stringResource(R.string.update_dialog_back),
                            style = if (topNavigationBarEnabled) MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily) else MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val actionEnabled = when (downloadState) {
                        is UpdateDownloadState.ReadyToInstall -> true
                        is UpdateDownloadState.Downloading -> false
                        else -> downloadUrl != null
                    }
                    Button(
                        onClick = {
                            when {
                                downloadState is UpdateDownloadState.ReadyToInstall && canInstallPackages ->
                                    UpdateDownloader.promptInstall(context)
                                downloadState is UpdateDownloadState.ReadyToInstall ->
                                    UpdateDownloader.openInstallPermissionSettings(context)
                                downloadState is UpdateDownloadState.Downloading -> Unit
                                else -> onStartDownload()
                            }
                        },
                        enabled = actionEnabled,
                        shape = if (topNavigationBarEnabled) RoundedCornerShape(50) else ButtonDefaults.shape,
                        colors = if (topNavigationBarEnabled) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.2f),
                                disabledContentColor = Color.Black.copy(alpha = 0.4f),
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                    ) {
                        Text(
                            text = when (val state = downloadState) {
                                is UpdateDownloadState.ReadyToInstall ->
                                    if (canInstallPackages) {
                                        stringResource(R.string.update_action_install)
                                    } else {
                                        stringResource(R.string.update_action_allow)
                                    }
                                is UpdateDownloadState.Downloading ->
                                    stringResource(R.string.update_state_downloading_percent, state.progressPercent)
                                is UpdateDownloadState.Failed -> stringResource(R.string.update_action_retry)
                                is UpdateDownloadState.Idle -> stringResource(R.string.update_action_download)
                            },
                            style = if (topNavigationBarEnabled) MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
