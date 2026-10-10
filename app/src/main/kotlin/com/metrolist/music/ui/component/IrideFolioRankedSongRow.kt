package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.metrolist.music.R
import com.metrolist.music.data.remote.ChartRankMovement
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary

/** Folio's shared exception for chart ranks, including rows whose audio is still loading. */
@Composable
fun IrideFolioRankedSongRow(
    rank: Int,
    maxRank: Int = 100,
    modifier: Modifier = Modifier,
    movement: ChartRankMovement? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontSize = IrideFolio.ChartRankFontSize,
        lineHeight = IrideFolio.ChartRankLineHeight,
        textAlign = TextAlign.End,
    )
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = textMeasurer.measure(AnnotatedString(maxRank.toString()), style).size.width
    val rankWidth = textMeasurer.measure(AnnotatedString(rank.toString()), style).size.width
    val labelWidthDp = with(density) { labelWidth.toDp() }
    val rankWidthDp = with(density) { rankWidth.toDp() }
    val gutter = maxOf(IrideFolio.ChartRankMinimumGutter, labelWidthDp + IrideFolio.ChartRankGap)
    val movementDescription = when (movement) {
        ChartRankMovement.UP -> stringResource(R.string.new_charts_rank_up)
        ChartRankMovement.DOWN -> stringResource(R.string.new_charts_rank_down)
        ChartRankMovement.NEW -> stringResource(R.string.new_charts_rank_new)
        null -> null
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(gutter).padding(end = IrideFolio.ChartRankGap)) {
            Box(
                Modifier.align(Alignment.CenterEnd).width(rankWidthDp)
                    .height(with(density) { IrideFolio.ChartRankLineHeight.toDp() } + IrideFolio.ChartRankIndicatorSize * 2)
                    .semantics(mergeDescendants = true) {
                        if (movementDescription != null) stateDescription = movementDescription
                    },
            ) {
                Text(
                    text = rank.toString(),
                    style = style,
                    color = MaterialTheme.colorScheme.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                )
                when (movement) {
                    ChartRankMovement.UP, ChartRankMovement.DOWN -> Icon(
                        painter = painterResource(if (movement == ChartRankMovement.UP) R.drawable.expand_less else R.drawable.expand_more),
                        contentDescription = null,
                        tint = if (movement == ChartRankMovement.UP && rank <= IrideFolio.ChartRankHighlightedCount) {
                            IrideFolio.ChartRankUpColor
                        } else IrideFolio.ChartRankMutedColor,
                        modifier = Modifier.align(if (movement == ChartRankMovement.UP) Alignment.TopCenter else Alignment.BottomCenter)
                            .requiredSize(IrideFolio.ChartRankIndicatorSize),
                    )
                    ChartRankMovement.NEW -> Box(
                        Modifier.align(Alignment.BottomCenter).width(rankWidthDp).height(IrideFolio.ChartRankIndicatorSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(IrideFolio.ChartRankNewEntryDotSize).background(Color.White, CircleShape))
                    }
                    null -> Unit
                }
            }
        }
        Box(modifier = Modifier.weight(1f).padding(end = IrideFolio.SongRowHorizontalPadding)) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false, content = content)
        }
    }
}
