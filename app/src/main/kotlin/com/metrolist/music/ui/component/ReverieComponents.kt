package com.metrolist.music.ui.component

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import com.metrolist.music.models.reverieEdition
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReverieInk = Color(0xFF24242A)
private val ReverieTitleFont = FontFamily(Font(R.font.anton))

@Composable
fun ReverieHomeCard(month: YearMonth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val edition = remember(month) { reverieEdition(month) }
    val palette = remember(month) { com.metrolist.music.models.reverieMonthPalette(month) }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = FolioShape(6.dp),
        color = Color(palette.background),
        contentColor = ReverieInk,
    ) {
        Column(Modifier.irideSurfaceGrain(.18f)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text(stringResource(R.string.reverie_edition, month.format(DateTimeFormatter.ofPattern("MM.yyyy"))),
                    fontFamily = IBMPlexMonoFontFamily, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(stringResource(R.string.reverie_name).uppercase(Locale.ROOT),
                    fontFamily = ReverieTitleFont, fontSize = 44.sp, lineHeight = 52.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            Canvas(Modifier.fillMaxWidth().height(152.dp).clipToBounds()) {
                drawReveriePrintFigure(edition.primary, edition.figureVariant)
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    fontFamily = IBMPlexMonoFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.reverie_home_action), fontFamily = IBMPlexMonoFontFamily, fontSize = 11.sp)
                    Box(Modifier.size(36.dp).background(Color(palette.accent), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.play), null, tint = Color(0xFFF2F0EB), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReverieMonthSelector(
    months: List<YearMonth>,
    selectedMonth: YearMonth,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val years = months.map { it.year }.distinct().sortedDescending()
    var showYears by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Row(
                modifier = Modifier
                    .clickable(enabled = years.size > 1) { showYears = true }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedMonth.year.toString(),
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
                if (years.size > 1) {
                    Icon(painterResource(R.drawable.expand_more), null, modifier = Modifier.size(18.dp))
                }
            }
            IrideFolioChoiceMenu(
                expanded = showYears && years.size > 1,
                onDismissRequest = { showYears = false },
                actions = years.map { year ->
                    IrideFolioMenuAction(
                        label = year.toString(),
                        selected = year == selectedMonth.year,
                        onClick = {
                            showYears = false
                            months.firstOrNull { it.year == year }?.let(onSelect)
                        },
                    )
                },
            )
        }
        Spacer(Modifier.width(16.dp))
        // Start each year's strip at its most recent available month.
        key(selectedMonth.year) {
            ChipsRow(
                chips = months.filter { it.year == selectedMonth.year }.map { month ->
                    month to month.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                },
                currentValue = selectedMonth,
                onValueUpdate = onSelect,
                labelStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = IBMPlexMonoFontFamily, fontSize = 13.sp),
                horizontalPadding = 8.dp,
                useIrideStyle = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ReverieArchiveDialog(
    months: List<YearMonth>,
    preferredMonth: YearMonth,
    onDismiss: () -> Unit,
    onPlay: (YearMonth) -> Unit,
) {
    var selectedYear by remember(months, preferredMonth) {
        mutableStateOf((preferredMonth.takeIf { it in months } ?: months.firstOrNull())?.year)
    }
    IrideFolioDialog(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reverie_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.close), stringResource(R.string.close)) }
            }
            Text(
                stringResource(if (months.isEmpty()) R.string.reverie_no_months else R.string.reverie_choose_month),
                color = MaterialTheme.colorScheme.textSecondary,
            )
            if (months.isNotEmpty()) {
                val years = months.map { it.year }.distinct().sortedDescending()
                if (years.size > 1) {
                    IrideSegmentedToggle(
                        options = years.map { it to it.toString() },
                        selected = selectedYear,
                        onSelect = { selectedYear = it },
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                } else {
                    Text(years.first().toString(), style = MaterialTheme.typography.labelLarge)
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(months.filter { it.year == selectedYear }, key = { it.toString() }) { month ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onPlay(month) }.padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())),
                                modifier = Modifier.weight(1f),
                                fontFamily = IBMPlexMonoFontFamily,
                            )
                            Icon(painterResource(R.drawable.play), stringResource(R.string.reverie_play_month), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReverieAvailabilityCard(month: YearMonth, firstFullMonth: YearMonth?, modifier: Modifier = Modifier) {
    val current = YearMonth.now()
    val partial = firstFullMonth != null && month < firstFullMonth
    val upcoming = month >= current
    val first = if (partial) requireNotNull(firstFullMonth) else firstFullMonth?.coerceAtLeast(current) ?: current
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    Surface(modifier.fillMaxWidth(), shape = FolioShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.irideSurfaceGrain(.12f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.reverie_name), fontFamily = ReverieTitleFont, fontSize = 26.sp)
            Text(stringResource(when {
                partial -> R.string.reverie_partial_month
                upcoming -> R.string.reverie_coming_month
                else -> R.string.reverie_insufficient_month
            }, first.format(formatter), first.plusMonths(1).atDay(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))),
                fontFamily = IBMPlexMonoFontFamily, fontSize = 12.sp, lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.textSecondary)
        }
    }
}
