package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.theme.SatoshiFontFamily
import com.metrolist.music.ui.theme.textPrimary

@Composable
fun IridePageTitle(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
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
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = if (LocalFolioDisplayHeadings.current != null) title.homeSentenceCase() else title,
        modifier = modifier,
        style = iridePageTitleTextStyle(monospace),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
    )
}

@Composable
fun iridePageTitleTextStyle(monospace: Boolean = true): TextStyle =
    LocalFolioDisplayHeadings.current?.page
        ?: MaterialTheme.typography.headlineLarge
            .copy(
                fontSize = IrideFolio.TitleFontSize * IrideFolio.SatoshiTextHeightScale,
                lineHeight = IrideFolio.TitleLineHeight,
                fontWeight = FontWeight.Bold,
            )
            .copy(fontFamily = SatoshiFontFamily)
