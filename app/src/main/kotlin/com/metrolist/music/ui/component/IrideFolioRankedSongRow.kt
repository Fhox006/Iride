package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary

/** Folio's shared exception for chart ranks, including rows whose audio is still loading. */
@Composable
fun IrideFolioRankedSongRow(
    rank: Int,
    maxRank: Int = 100,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontSize = IrideFolio.ChartRankFontSize,
        lineHeight = IrideFolio.ChartRankLineHeight,
        textAlign = TextAlign.End,
    )
    val labelWidth = rememberTextMeasurer().measure(AnnotatedString(maxRank.toString()), style).size.width
    val gutter = maxOf(IrideFolio.ChartRankMinimumGutter, with(density) { labelWidth.toDp() } + IrideFolio.ChartRankGap)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            style = style,
            color = MaterialTheme.colorScheme.textSecondary,
            maxLines = 1,
            modifier = Modifier.width(gutter).padding(end = IrideFolio.ChartRankGap),
        )
        Box(modifier = Modifier.weight(1f).padding(end = IrideFolio.SongRowHorizontalPadding)) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false, content = content)
        }
    }
}
