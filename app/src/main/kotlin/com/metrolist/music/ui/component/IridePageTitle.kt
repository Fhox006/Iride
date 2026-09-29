package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary

@Composable
fun IridePageTitle(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    maxLines: Int = 1,
) {
    IridePageTitle(
        title = stringResource(titleRes).lowercase().replaceFirstChar { it.titlecase() },
        modifier = modifier,
        monospace = monospace,
        maxLines = maxLines,
    )
}

@Composable
fun IridePageTitle(
    title: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = true,
    color: Color = MaterialTheme.colorScheme.textPrimary,
    maxLines: Int = 1,
) {
    Text(
        text = title,
        modifier = modifier,
        style = iridePageTitleTextStyle(monospace),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun iridePageTitleTextStyle(monospace: Boolean = true): TextStyle =
    MaterialTheme.typography.headlineLarge.copy(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
    ).let { if (monospace) it.copy(fontFamily = SpaceMonoFontFamily) else it }
