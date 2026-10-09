package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.strokeCard

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
    modifier: Modifier = Modifier,
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
        cornerRadius = IrideFolio.CollectionSearchCornerRadius,
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
    IrideFolioPrimaryIconButton(
        onClick = onClick,
        size = IrideFolio.CollectionPlaySize,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(if (isPlaying) IrideFolio.CollectionPauseIcon else IrideFolio.CollectionPlayIcon),
            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
            modifier = Modifier
                .size(IrideFolio.CollectionPlayIconSize)
                .offset(x = if (isPlaying) 0.dp else IrideFolio.CollectionPlayIconOffset),
        )
    }
}
