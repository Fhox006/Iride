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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.rememberPreference
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
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)

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
                    text = "New version available",
                    style = titleStyle,
                    color = titleColor,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Iride ${releaseInfo.versionName} is available.",
                    style = bodyStyle,
                    color = bodyColor,
                )
                if (releaseInfo.isPreviewBuild()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This build may be experimental.",
                        style = smallStyle,
                        color = bodyColor,
                    )
                }
                if (downloadUrl == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "No compatible package found for this build.",
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
                        Text("Back", style = if (topNavigationBarEnabled) MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily) else MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onInstall,
                        enabled = downloadUrl != null,
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
                        Text("Install", style = if (topNavigationBarEnabled) MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
