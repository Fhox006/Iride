/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.R
import com.metrolist.music.ui.theme.fillSelected
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
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

    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var canInstallPackages by remember { mutableStateOf(UpdateDownloader.canInstallPackages(context)) }
    LaunchedEffect(downloadUrl) {
        while (isActive) {
            downloadState = UpdateDownloader.queryDownloadState(context)
            canInstallPackages = UpdateDownloader.canInstallPackages(context)
            delay(1_000)
        }
    }

    IrideFolioDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val titleColor = MaterialTheme.colorScheme.textPrimary
        val bodyColor = MaterialTheme.colorScheme.textPrimary
        val mutedColor = MaterialTheme.colorScheme.textSecondary
        val titleStyle =
            MaterialTheme.typography.headlineSmall
        val bodyStyle =
            MaterialTheme.typography.bodyMedium
        val smallStyle =
            MaterialTheme.typography.bodySmall

        Column(
            modifier = Modifier.padding(IrideFolio.DialogContentPadding),
        ) {
            Text(
                text = stringResource(R.string.update_dialog_title_new_version),
                style = MaterialTheme.typography.headlineSmall,
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
            Spacer(Modifier.height(IrideFolio.DialogActionsTopSpacing))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
            ) {
                IrideFolioSecondaryButton(
                    onClick = onDismiss,
                ) {
                    Text(
                        stringResource(R.string.update_dialog_back),
                    )
                }
                val actionEnabled = when (downloadState) {
                    is UpdateDownloadState.ReadyToInstall -> true
                    is UpdateDownloadState.Downloading -> false
                    else -> downloadUrl != null
                }
                IrideFolioPrimaryButton(
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
                    )
                }
            }
        }
    }
}
