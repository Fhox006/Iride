/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.utils.ReleaseInfo
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
    onInstall: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = SquircleShape(radius = 28.dp, cornerSmoothing = 0.5f),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "New version available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Iride ${releaseInfo.versionName} is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (releaseInfo.isPreviewBuild()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This build may be experimental.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (downloadUrl == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "No compatible package found for this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onInstall,
                        enabled = downloadUrl != null,
                    ) {
                        Text("Install")
                    }
                }
            }
        }
    }
}
