package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

internal val LocalFolioButtonBackdrop = staticCompositionLocalOf<FrostBackdrop?> { null }

/** The same glass surface, press feedback and disabled state for text and icon actions. */
@Composable
internal fun IrideFolioFrostedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    grouped: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = IrideFolio.ButtonHorizontalPadding,
        vertical = IrideFolio.ButtonVerticalPadding,
    ),
    content: @Composable RowScope.() -> Unit,
) = IrideFolioButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    secondary = false,
    frosted = true,
    grouped = grouped,
    contentPadding = contentPadding,
    content = content,
)

@Composable
internal fun Modifier.irideFolioGlassSurface(
    backdrop: FrostBackdrop?,
    shape: Shape = IrideFolio.ControlsShape,
    bordered: Boolean = true,
): Modifier =
    frostedNavigationSurface(
        backdrop ?: LocalFolioButtonBackdrop.current ?: LocalScreenFrostBackground.current, Color.White, shape,
        surfaceAlpha = IrideFolio.FrostedLightTintOpacity,
        observeBackdrop = true,
        opaqueBackground = false,
    ).then(
        if (bordered) Modifier.border(
            IrideFolio.ButtonBorderWidth,
            Color.White.copy(alpha = IrideFolio.ButtonBorderOpacity),
            shape,
        ) else Modifier,
    )

/** Reversible native reveal; newly added actions use the same timing and reduced-motion rules. */
@Composable
internal fun IrideFolioActionReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val duration = if (rememberReducedMotion()) 0 else IrideMotion.Long
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandHorizontally(tween(duration, easing = FastOutSlowInEasing), Alignment.End) +
            fadeIn(tween(duration)) + scaleIn(tween(duration), initialScale = 0.92f),
        exit = shrinkHorizontally(tween(duration, easing = FastOutSlowInEasing), Alignment.End) +
            fadeOut(tween(duration)) + scaleOut(tween(duration), targetScale = 0.92f),
    ) { content() }
}

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
    compact: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (compact) {
        val ink = LocalArtworkContentColor.current ?: MaterialTheme.colorScheme.textPrimary
        val interactions = remember { MutableInteractionSource() }
        val opacity = if (enabled) 1f else IrideFolio.ButtonDisabledOpacity
        Row(
            modifier
                .heightIn(min = IrideFolio.CompactButtonMinHeight)
                .pressScale(interactions, pressedScale = IrideFolio.ButtonPressedScale)
                .clip(IrideFolio.ButtonShape)
                .background(
                    ink.copy(
                        alpha =
                            if (selected) IrideFolio.CompactChoiceSelectedOpacity * opacity else 0f
                    )
                )
                .border(
                    IrideFolio.ButtonBorderWidth,
                    ink.copy(
                        alpha =
                            (if (selected) IrideFolio.CompactChoiceSelectedBorderOpacity
                            else IrideFolio.ButtonBorderOpacity) * opacity
                    ),
                    IrideFolio.ButtonShape,
                )
                .selectable(
                    selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    interactionSource = interactions,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = IrideFolio.CompactButtonHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    ink.copy(alpha = .8f * opacity)
            ) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    content()
                }
            }
        }
    } else
        IrideFolioButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            secondary = !selected,
            content = content,
        )
}

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
    minHeight: Dp = IrideFolio.ButtonMinHeight,
    frosted: Boolean = false,
    grouped: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = IrideFolio.ButtonHorizontalPadding,
        vertical = IrideFolio.ButtonVerticalPadding,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val ink = if (frosted) Color.White else if (secondary) colors.textPrimary else colors.background
    val shape = if (frosted) IrideFolio.ControlsShape else IrideFolio.ButtonShape
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .then(
                if (frosted) Modifier
                    .pressScale(interactions, pressedScale = IrideFolio.ButtonPressedScale)
                    .clip(shape)
                    .then(
                        if (grouped) Modifier
                        else Modifier.irideFolioGlassSurface(LocalFolioButtonBackdrop.current),
                    )
                else Modifier.folioButtonSurface(secondary, enabled, interactions),
            ),
        enabled = enabled,
        shape = shape,
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
