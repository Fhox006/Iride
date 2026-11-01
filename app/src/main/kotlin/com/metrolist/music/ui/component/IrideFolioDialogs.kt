package com.metrolist.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.LocalPlayerAwareWindowInsets

/** Immersive editors own their viewport and keyboard insets, independently of the mini player. */
@Composable
fun IrideFolioEditorDialog(
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = dismissEnabled,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.textPrimary,
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .irideSurfaceGrain(IrideFolio.BackgroundGrainOpacity)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .union(WindowInsets.ime),
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
                    LocalScreenFrostBackground provides null,
                    LocalScreenFrostBackdrop provides null,
                    LocalFolioButtonBackdrop provides null,
                    LocalArtworkContentColor provides MaterialTheme.colorScheme.textPrimary,
                    LocalIrideFolioScrimColor provides MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.widthIn(max = IrideFolio.EditorMaxWidth).fillMaxSize()) { content() }
                }
            }
        }
    }
}

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
                headlineSmall = typography.headlineSmall.copy(fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold),
                titleLarge = typography.titleLarge.copy(fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold),
                titleMedium = typography.titleMedium.copy(fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold),
                bodyLarge = typography.bodyLarge.copy(fontFamily = IBMPlexMonoFontFamily),
                bodyMedium = typography.bodyMedium.copy(fontFamily = IBMPlexMonoFontFamily),
                bodySmall = typography.bodySmall.copy(fontFamily = IBMPlexMonoFontFamily),
                labelLarge = typography.labelLarge.copy(fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold),
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
