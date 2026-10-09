package com.metrolist.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary

/** Shared window and surface for every IRIDE popup, including updates and input/list dialogs. */
@Composable
fun IrideFolioDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val typography = MaterialTheme.typography
        MaterialTheme(
            typography = typography.copy(
                headlineSmall = typography.headlineSmall.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                titleLarge = typography.titleLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                titleMedium = typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                bodyLarge = typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily),
                bodyMedium = typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                bodySmall = typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily),
                labelLarge = typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
            ),
        ) {
            Surface(
                modifier = Modifier.padding(IrideFolio.DialogOuterPadding).fillMaxWidth().then(modifier),
                shape = IrideFolio.DialogShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.textPrimary,
                border = BorderStroke(IrideFolio.DialogBorderWidth, MaterialTheme.colorScheme.strokeHairline),
            ) {
                Box(
                    modifier = Modifier.irideSurfaceGrain(IrideFolio.SurfaceGrainOpacity),
                    propagateMinConstraints = true,
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
                }
            }
        }
    }
}

/** Material-style slots are adapted to the same Folio popup as [DefaultDialog]. */
@Composable
fun IrideFolioAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
) {
    DefaultDialog(
        onDismiss = onDismissRequest,
        modifier = modifier,
        icon = icon,
        title = title,
        properties = properties,
        horizontalAlignment = androidx.compose.ui.Alignment.Start,
        buttons = {
            dismissButton?.invoke()
            confirmButton()
        },
    ) { text?.invoke() }
}
