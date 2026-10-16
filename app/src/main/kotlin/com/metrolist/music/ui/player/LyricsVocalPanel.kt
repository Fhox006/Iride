package com.metrolist.music.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.playback.vocals.VocalSeparationState
import com.metrolist.music.playback.vocals.VocalPreparation
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioControlPanel
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.roundToInt

@Composable
internal fun LyricsVocalPanel(
    expanded: Boolean,
    state: VocalSeparationState,
    onDismiss: () -> Unit,
    onLevelChange: (Float) -> Unit,
    onPrepare: () -> Unit,
) {
    IrideFolioControlPanel(expanded, onDismiss) {
        val label = stringResource(R.string.vocal_voice_level)
        val preparing = state.phase != VocalPreparation.Error && !state.audioReady
        val reducedMotion = rememberReducedMotion()
        val opacity = if (preparing && !reducedMotion) {
            val pulse by rememberInfiniteTransition(label = "karaokePreparation").animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "karaokeBarOpacity",
            )
            pulse
        } else if (preparing) 0.65f else 1f
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontFamily = SpaceMonoFontFamily, fontSize = 13.sp)
            Text("${(state.voiceLevel * 100).roundToInt()}%", color = Color.White, fontFamily = SpaceMonoFontFamily, fontSize = 13.sp)
        }
        Slider(
            value = state.voiceLevel,
            onValueChange = onLevelChange,
            enabled = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            colors = SliderDefaults.colors(
                thumbColor = Color.White.copy(alpha = opacity), activeTrackColor = Color.White.copy(alpha = opacity),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.vocal_instrumental), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontFamily = SpaceMonoFontFamily)
            Text(stringResource(R.string.vocal_original), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontFamily = SpaceMonoFontFamily)
        }
        if (state.phase == VocalPreparation.Error) {
            Spacer(Modifier.height(IrideFolio.DialogSectionSpacing))
            IrideFolioSecondaryButton(onClick = onPrepare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vocal_retry))
            }
        }
        state.error?.let { error ->
            Spacer(Modifier.height(IrideFolio.MenuVerticalPadding))
            Text(error, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = SpaceMonoFontFamily)
        }
    }
}
