package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.pressScale

/** All filled actions, including collection play controls, use this protocol. */
@Composable
fun IrideFolioPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = IrideFolioButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    secondary = false,
    interactionSource = interactionSource,
    content = content,
)

/** All outlined actions have transparent fill and the same border, corners and typography. */
@Composable
fun IrideFolioSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = IrideFolioButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    secondary = true,
    interactionSource = interactionSource,
    content = content,
)

/** A selected choice uses the primary protocol; other choices use the outlined protocol. */
@Composable
fun IrideFolioChoiceButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = IrideFolioButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    secondary = !selected,
    content = content,
)

@Composable
internal fun IrideFolioPrimaryIconButton(
    onClick: () -> Unit,
    size: Dp,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit,
) = IrideFolioButton(
    onClick = onClick,
    modifier = Modifier.size(size),
    enabled = enabled,
    secondary = false,
    contentPadding = PaddingValues(0.dp),
    content = content,
)

@Composable
internal fun IrideFolioSecondaryIconButton(action: IrideFolioCollectionAction) {
    val colors = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    IrideOutlineIconButton(
        onClick = action.onClick,
        icon = action.icon,
        contentDescription = action.contentDescription,
        enabled = action.enabled,
        loading = action.loading,
        pressEffect = action.pressEffect,
        tint = colors.textPrimary.copy(alpha = if (action.enabled) 1f else IrideFolio.ButtonDisabledOpacity),
        size = IrideFolio.CollectionActionSize,
        iconSize = IrideFolio.CollectionActionIconSize,
        interactionSource = interactions,
        modifier = Modifier.folioButtonSurface(secondary = true, enabled = action.enabled, interactions = interactions),
    )
}

@Composable
private fun IrideFolioButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    secondary: Boolean,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = IrideFolio.ButtonHorizontalPadding,
        vertical = IrideFolio.ButtonVerticalPadding,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val ink = if (secondary) colors.textPrimary else colors.background
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = IrideFolio.ButtonMinHeight)
            .folioButtonSurface(secondary, enabled, interactions),
        enabled = enabled,
        shape = IrideFolio.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = ink.copy(alpha = IrideFolio.ButtonDisabledOpacity),
        ),
        contentPadding = contentPadding,
        interactionSource = interactions,
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            content()
        }
    }
}

/** Text and icon buttons share this single surface and press response. */
@Composable
private fun Modifier.folioButtonSurface(
    secondary: Boolean,
    enabled: Boolean,
    interactions: MutableInteractionSource,
): Modifier {
    val ink = MaterialTheme.colorScheme.textPrimary
    val opacity = if (enabled) 1f else IrideFolio.ButtonDisabledOpacity
    return pressScale(interactions, pressedScale = IrideFolio.ButtonPressedScale)
        .clip(IrideFolio.ButtonShape)
        .background(if (secondary) Color.Transparent else ink.copy(alpha = opacity))
        // Outlined controls inherit the parent's grain; filled controls replace it within the clip.
        .then(
            if (secondary) Modifier else Modifier.irideSurfaceGrain(
                IrideFolio.ButtonGrainOpacity * opacity,
                soft = false,
            ),
        )
        .then(
            if (secondary) Modifier.border(
                IrideFolio.ButtonBorderWidth,
                ink.copy(alpha = IrideFolio.ButtonBorderOpacity * opacity),
                IrideFolio.ButtonShape,
            ) else Modifier,
        )
}
