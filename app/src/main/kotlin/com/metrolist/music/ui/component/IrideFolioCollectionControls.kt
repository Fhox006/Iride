package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val CollectionPlayPunchDurationMillis = 420
private const val CollectionPlayPunchScale = 0.28f

/** Collection searches use Folio's fixed corners and borderless surface. */
@Composable
fun IrideFolioCollectionSearchBox(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    placeholderText: String,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = IrideFolio.TrackArtworkInset, vertical = 8.dp),
) {
    IrideSearchBox(
        query = query,
        onQueryChange = onQueryChange,
        placeholderText = placeholderText,
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged,
        onSearch = onSearch,
        onClear = onClear,
        modifier = modifier,
        showBorder = false,
        animateQueryCorners = true,
        cornerRadius = 18.dp,
    )
}

/** Screens supply behavior and icons; Folio owns all collection control styling. */
data class IrideFolioCollectionAction(
    @DrawableRes val icon: Int,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val pressEffect: IridePressEffect = IridePressEffect.None,
)

@Composable
fun IrideFolioCollectionControlPanel(
    leadingAction: IrideFolioCollectionAction,
    onPlayClick: () -> Unit,
    isPlaying: Boolean,
    trailingAction: IrideFolioCollectionAction? = null,
    modifier: Modifier = Modifier,
    playEnabled: Boolean = true,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(IrideFolio.CollectionPanelShape)
                .border(
                    IrideFolio.CollectionPanelBorderWidth,
                    MaterialTheme.colorScheme.strokeCard.copy(alpha = IrideFolio.CollectionPanelBorderOpacity),
                    IrideFolio.CollectionPanelShape,
                )
                .padding(
                    horizontal = IrideFolio.CollectionPanelHorizontalPadding,
                    vertical = IrideFolio.CollectionPanelVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IrideFolio.CollectionPanelButtonSpacing),
        ) {
            IrideFolioCollectionActionButton(leadingAction)
            IrideFolioCollectionPlayButton(onPlayClick, isPlaying, playEnabled)
            trailingAction?.let { IrideFolioCollectionActionButton(it) }
        }
    }
}

@Composable
private fun IrideFolioCollectionActionButton(action: IrideFolioCollectionAction) {
    Box(
        modifier = Modifier.size(IrideFolio.CollectionActionTouchTargetSize),
        contentAlignment = Alignment.Center,
    ) {
        IrideFolioSecondaryIconButton(action)
    }
}

@Composable
private fun IrideFolioCollectionPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    enabled: Boolean,
) {
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val iconPulse = remember { Animatable(0f) }
    IrideFolioPrimaryIconButton(
        onClick = {
            if (!reducedMotion) {
                scope.launch {
                    iconPulse.snapTo(0f)
                    iconPulse.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = CollectionPlayPunchDurationMillis,
                            easing = IrideMotion.EaseOutExpo,
                        ),
                    )
                }
            }
            onClick()
        },
        size = IrideFolio.CollectionPlaySize,
        enabled = enabled,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (
                    scaleIn(
                        initialScale = 0.72f,
                        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short),
                    ) + fadeIn(animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short))
                ) togetherWith (
                    scaleOut(
                        targetScale = 0.72f,
                        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short),
                    ) + fadeOut(animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short))
                )
            },
            contentAlignment = Alignment.Center,
            label = "collectionPlayPause",
            modifier = Modifier
                .size(IrideFolio.CollectionPlayIconSize)
                .offset(x = if (isPlaying) 0.dp else IrideFolio.CollectionPlayIconOffset),
        ) { playing ->
            Icon(
                painter = painterResource(
                    if (playing) IrideFolio.CollectionPauseIcon else IrideFolio.CollectionPlayIcon,
                ),
                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                modifier = Modifier
                    .size(IrideFolio.CollectionPlayIconSize)
                    .graphicsLayer {
                        val scale = 1f + sin(PI * iconPulse.value).toFloat() * CollectionPlayPunchScale
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}
