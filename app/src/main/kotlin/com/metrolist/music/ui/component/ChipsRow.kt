/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.height
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.screens.OptionStats
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    onSelectedContainerColor: Color = MaterialTheme.colorScheme.onPrimary,
    chipHeight: androidx.compose.ui.unit.Dp = 32.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
    useIrideStyle: Boolean = false,
) {
    if (useIrideStyle) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(horizontalPadding))

            val density = LocalDensity.current

            chips.forEach { (value, label) ->
                val isSelected = currentValue == value
                var textWidthPx by remember(value) { mutableStateOf(0) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onValueUpdate(value) }
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.onSizeChanged { textWidthPx = it.width },
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(with(density) { textWidthPx.toDp() })
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            ),
                    )
                }
                Spacer(Modifier.width(20.dp))
            }
        }
        return
    }

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Spacer(Modifier.width(horizontalPadding))

        chips.forEach { (value, label) ->
            FilterChip(
                modifier = Modifier.height(chipHeight),
                label = { 
                    Text(
                        text = label,
                        style = labelStyle
                    ) 
                },
                selected = currentValue == value,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = selectedContainerColor,
                    selectedLabelColor = onSelectedContainerColor,
                    selectedLeadingIconColor = onSelectedContainerColor,
                    selectedTrailingIconColor = onSelectedContainerColor,
                ),
                onClick = { onValueUpdate(value) },
                shape = CircleShape,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentValue == value,
                ),
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * Compact two-or-more-option pill switcher in the New Iride UI style: small underlined
 * text labels side by side, no background capsule so options never visually overlap.
 * Meant for header trailing slots (library saved/downloaded, search online/library, ...).
 *
 * The underline is a single shared indicator that glides smoothly from one label to the other on
 * selection change (position + width both spring-animated) instead of each label independently
 * popping its own static underline on/off, which used to read as an abrupt, un-animated flicker.
 */
@Composable
fun <E> IrideSegmentedToggle(
    options: List<Pair<E, String>>,
    selected: E,
    onSelect: (E) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    spacing: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val density = LocalDensity.current
    val labelBoundsPx = remember { androidx.compose.runtime.mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val targetBounds = labelBoundsPx[selectedIndex]
    val indicatorAnimSpec = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )
    val indicatorX by animateFloatAsState(targetBounds?.first ?: 0f, indicatorAnimSpec, label = "irideSegmentedIndicatorX")
    val indicatorWidth by animateFloatAsState(targetBounds?.second ?: 0f, indicatorAnimSpec, label = "irideSegmentedIndicatorWidth")

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { index, (value, label) ->
                val isSelected = selected == value
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(value) }
                        .onGloballyPositioned { coords ->
                            labelBoundsPx[index] = coords.positionInParent().x to coords.size.width.toFloat()
                        },
                )
                if (index != options.lastIndex) Spacer(Modifier.width(spacing))
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (targetBounds != null) {
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(indicatorX.roundToInt(), 0) }
                        .width(with(density) { indicatorWidth.toDp() })
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    useIrideStyle: Boolean = false,
) {
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    if (useIrideStyle) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var expanded by remember { mutableStateOf(false) }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            expanded = !expanded
                            expandIconDegree -= 180
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                ) {
                    Text(
                        text = when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                        style = TextStyle(
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = (-0.1).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(rotationZ = rotationAnimation),
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandIn() + fadeIn(),
                    exit = shrinkOut() + fadeOut(),
                ) {
                    DropdownMenu(
                        modifier = Modifier.padding(start = 12.dp),
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                            expandIconDegree -= 180
                        },
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.second,
                                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                    )
                                },
                                onClick = {
                                    onSelectionChange(option.first)
                                    expandIconDegree -= 180
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            AnimatedContent(
                targetState = selectedOption,
                transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
                label = "",
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    chips.forEach { (value, label) ->
                        val isSelected = currentValue == value
                        var textWidthPx by remember(value) { mutableStateOf(0) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onValueUpdate(value) }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.onSizeChanged { textWidthPx = it.width },
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(with(LocalDensity.current) { textWidthPx.toDp() })
                                    .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            ),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
        }
        return
    }

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            AssistChip(
                onClick = {
                    expanded = !expanded
                    expandIconDegree -= 180
                },
                label = {
                    Text(
                        text =
                        when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                shape = CircleShape,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = containerColor,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            ) {
                chips.forEach { (value, label) ->
                    Spacer(Modifier.width(8.dp))

                    FilterChip(
                        label = { Text(label) },
                        selected = currentValue == value,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = containerColor,
                        ),
                        onClick = { onValueUpdate(value) },
                        shape = CircleShape,
                        border = null
                    )
                }
            }
        }
    }
}
