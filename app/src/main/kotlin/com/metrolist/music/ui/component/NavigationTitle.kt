/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary

@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    useIrideStyle: Boolean = false,
    collapsed: Boolean = false,
    onCollapseToggle: (() -> Unit)? = null,
    topPadding: Dp? = null,
    bottomPadding: Dp? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(
                start = if (useIrideStyle) 20.dp else 12.dp,
                end = if (useIrideStyle) 20.dp else 12.dp,
                top = topPadding ?: if (useIrideStyle) 26.dp else 12.dp,
                bottom = bottomPadding ?: if (useIrideStyle) 2.dp else 12.dp,
            )
    ) {
        thumbnail?.invoke()

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = title,
                style = if (useIrideStyle) {
                    MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = (-0.1).sp,
                    )
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = if (useIrideStyle) MaterialTheme.colorScheme.textSecondary else MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        onPlayAllClick?.let { playAllClick ->
            if (useIrideStyle) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = stringResource(R.string.play_all),
                    tint = MaterialTheme.colorScheme.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = playAllClick,
                        ),
                )
            } else {
                OutlinedButton(
                    onClick = playAllClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.play_all),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        onRefreshClick?.let { refreshClick ->
            val rotation = if (isRefreshing) {
                val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
                    label = "refreshRotationAngle",
                )
                angle
            } else {
                0f
            }
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = stringResource(R.string.refresh),
                tint = if (useIrideStyle) MaterialTheme.colorScheme.textSecondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isRefreshing,
                        onClick = refreshClick,
                    ),
            )
        }

        if (useIrideStyle && onCollapseToggle != null) {
            val rotation by animateFloatAsState(
                targetValue = if (collapsed) 180f else 0f,
                label = "collapseArrowRotation",
            )
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapseToggle,
                    ),
            )
        } else if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = if (useIrideStyle) MaterialTheme.colorScheme.textSecondary else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun IrideCollapsibleSection(
    collapsed: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = !collapsed,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(tween(140)),
    ) {
        content()
    }
}
