/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle

object LibraryPageDefaults {
    val SidePadding = IrideFolio.HorizontalPadding
    val HeroToControlsGap = IridePageLayoutDefaults.TitleBottomSpacing
    val GridSpacing = 12.dp
}

@Composable
fun LibrarySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 12.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding),
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp,
                ),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun LibraryPlaybackActions(
    onShuffle: () -> Unit,
    onRotation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        LibraryPlaybackAction(
            label = stringResource(R.string.shuffle),
            icon = R.drawable.shuffle,
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        )
        LibraryPlaybackAction(
            label = stringResource(R.string.in_rotation),
            icon = R.drawable.graphic_eq,
            onClick = onRotation,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryPlaybackAction(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.fillSubtle,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun LibraryCollapsibleSectionLabel(
    text: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by
        animateFloatAsState(
            targetValue = if (collapsed) 180f else 0f,
            label = "librarySectionCollapse",
        )
    LibrarySectionLabel(
        text = text,
        modifier = modifier,
        trailing = {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = Color.White,
                modifier =
                    Modifier.rotate(rotation)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggle,
                        ),
            )
        },
    )
}

@Composable
fun LibraryFooterCount(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
