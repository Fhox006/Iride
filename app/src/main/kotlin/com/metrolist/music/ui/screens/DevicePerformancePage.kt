package com.metrolist.music.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.FolioDeviceProfile
import com.metrolist.music.ui.component.IrideSwitch
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

@Composable
internal fun DevicePerformancePage(
    device: FolioDeviceProfile,
    fullMotion: Boolean,
    onFullMotionChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    applyInsets: Boolean = true,
    continueEnabled: Boolean = true,
    saveFailed: Boolean = false,
) {
    val visible = LocalExperienceVisible.current
    val reducedMotion = rememberReducedMotion()
    val target = when {
        !fullMotion -> 0.28f
        device.lowRam -> 0.94f
        else -> 0.66f
    }
    val load = remember { Animatable(0f) }
    LaunchedEffect(visible, target, reducedMotion) {
        if (!visible) return@LaunchedEffect
        if (reducedMotion) load.snapTo(target)
        else load.animateTo(target, tween(650, easing = IrideMotion.EaseOutQuart))
    }

    ExperiencePage(
        title = stringResource(R.string.onboarding_performance_title),
        description = stringResource(
            if (device.lowRam) R.string.onboarding_performance_memory_body
            else R.string.onboarding_performance_system_body,
        ),
        illustration = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MemoryLoadCard(load.value, fullMotion, onFullMotionChange)
            }
        },
        applyInsets = applyInsets,
        details = {
            if (device.lowRam && !device.supportsFullMotion) {
                Text(
                    stringResource(R.string.onboarding_performance_system_hint),
                    color = MaterialTheme.colorScheme.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        actions = {
            ExperienceButton(stringResource(R.string.onboarding_continue), onNext, enabled = continueEnabled)
            if (saveFailed) {
                Text(stringResource(R.string.experience_save_failed), color = MaterialTheme.colorScheme.textSecondary)
            }
        },
    )
}

@Composable
private fun MemoryLoadCard(load: Float, fullMotion: Boolean, onFullMotionChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = FolioShape(22.dp)
    val trackShape = FolioShape(5.dp)
    MaterialTheme(
        colorScheme = colors.copy(
            background = Color.Black,
            surface = Color.Black,
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.72f),
        ),
    ) {
        Column(
            Modifier.fillMaxWidth(0.96f).graphicsLayer { rotationZ = -2.2f }
                .clip(shape).background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.strokeHairline, shape)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_performance_load),
                color = MaterialTheme.colorScheme.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            Canvas(
                Modifier.fillMaxWidth().height(10.dp).clip(trackShape)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(load, 0f..1f) },
            ) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(Color.White, cornerRadius = radius)
                val fillWidth = size.width * load.coerceIn(0f, 1f)
                val greenEnd = size.width * 0.42f
                if (fillWidth > 0f) {
                    val greenWidth = minOf(fillWidth, greenEnd)
                    drawRect(Color(0xFF55E878), size = Size(greenWidth, size.height))
                    if (fillWidth > greenEnd) {
                        drawRect(
                            color = Color(0xFFFFD34E),
                            topLeft = Offset(greenEnd, 0f),
                            size = Size(fillWidth - greenEnd, size.height),
                        )
                    }
                    drawCircle(
                        color = if (fillWidth > greenEnd) Color(0xFFFFD34E) else Color(0xFF55E878),
                        radius = radius.x,
                        center = Offset(fillWidth - radius.x, radius.y),
                    )
                }
            }
            Row(
                Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 52.dp)
                    .toggleable(fullMotion, role = Role.Switch, onValueChange = onFullMotionChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.onboarding_performance_full), Modifier.weight(1f), color = MaterialTheme.colorScheme.textPrimary)
                IrideSwitch(checked = fullMotion, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
            }
        }
    }
}
