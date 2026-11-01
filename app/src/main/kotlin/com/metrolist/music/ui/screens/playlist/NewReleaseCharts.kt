package com.metrolist.music.ui.screens.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ContentCountryKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.data.remote.DailyChart
import com.metrolist.music.data.remote.isCurrentFor
import com.metrolist.music.ui.component.IrideChartPoster
import com.metrolist.music.ui.component.CollectionExpansionKind
import com.metrolist.music.ui.component.rememberCollectionExpansionTile
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioCollectionSearchBox
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.SharedHomeSectionTitle
import com.metrolist.music.ui.component.collectDeferredHomeState
import com.metrolist.music.ui.component.SharedYouTubePlaylistRow
import com.metrolist.music.ui.component.chartCountryName
import com.metrolist.music.ui.component.hasChartArtwork
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ChartDirectoryViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import sv.lib.squircleshape.SquircleShape

internal fun isCurrentChart(chart: DailyChart): Boolean = runCatching {
    chart.isCurrentFor(ChartMarket(chart.marketCode))
}.getOrDefault(false)

@Composable
private fun homeChartMarkets(viewModel: ChartDirectoryViewModel): List<ChartMarket> {
    val catalog by viewModel.catalog.collectDeferredHomeState("chart_catalog") { it.markets.isNotEmpty() }
    val region = rememberChartRegion()
    return remember(catalog.markets, region) {
        listOf(region, "global", "us", "br", "gb", "de", "mx", "fr", "in", "jp")
            .distinct().mapNotNull { code -> catalog.markets.find { it.code == code && hasChartArtwork(code) } }
    }
}

@Composable
fun PreloadHomeCharts(viewModel: ChartDirectoryViewModel = hiltViewModel()) {
    val candidates = homeChartMarkets(viewModel)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel, candidates) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh()
                candidates.forEach { viewModel.checkMarket(it) }
                delay(60_000)
            }
        }
    }
}

@Composable
fun NewReleaseChartsShelf(
    navController: NavController,
    viewModel: ChartDirectoryViewModel = hiltViewModel(),
    preload: Boolean = true,
) {
    if (preload) PreloadHomeCharts(viewModel)
    val charts by viewModel.charts.collectDeferredHomeState("charts") { entries ->
        entries.values.any { !it.sourceMissing && it.chart?.let(::isCurrentChart) == true }
    }
    val candidates = homeChartMarkets(viewModel)
    val available = remember(candidates, charts) {
        candidates.filter { market ->
            charts[market.code]?.let { !it.sourceMissing && it.chart?.let(::isCurrentChart) == true } == true
        }
    }
    if (available.isNotEmpty()) {
        com.metrolist.music.ui.component.IrideFolioSection(sectionKey = "home_charts") {
            SharedHomeSectionTitle(stringResource(R.string.new_charts_title), showNavigationArrow = true,
                onClick = { navController.navigate("all_charts") })
            SharedYouTubePlaylistRow(available, key = { it.code }, contentPadding = IrideFolio.HorizontalPadding) { market ->
                ChartCard(market, charts.getValue(market.code).chart!!.tracks.size,
                    onClick = { navController.navigate("new_releases_chart/${market.code}") }, modifier = Modifier.width(172.dp))
            }
        }
    }
}

@Composable
fun AllChartsScreen(navController: NavController, viewModel: ChartDirectoryViewModel = hiltViewModel()) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val charts by viewModel.charts.collectAsStateWithLifecycle()
    val discovered by viewModel.discovered.collectAsStateWithLifecycle()
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    val region = rememberChartRegion()
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val candidates = remember(catalog.markets, region, query.text) {
        catalog.markets.filter { hasChartArtwork(it.code) }.sortedWith(compareBy<ChartMarket> {
            when (it.code) { region -> 0; "global" -> 1; else -> 2 }
        }.thenBy { chartCountryName(it.code) }).filter {
            chartCountryName(it.code).contains(query.text.trim(), ignoreCase = true) ||
                it.code.equals(query.text.trim(), ignoreCase = true)
        }
    }
    val available = candidates.filter { market ->
        charts[market.code]?.let { !it.sourceMissing && it.chart?.let(::isCurrentChart) == true } == true
    }
    val rows = available.chunked(2)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel, candidates, discovered) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh()
                candidates.filter { it.code in discovered }.filter { market ->
                    charts[market.code]?.let { !it.sourceMissing && it.chart?.let(::isCurrentChart) != true } == true
                }.forEach { viewModel.checkMarket(it) }
                delay(60_000)
            }
        }
    }
    LaunchedEffect(lifecycle, viewModel, rows) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val visible = listState.layoutInfo.visibleItemsInfo.map { it.key }.toSet()
                rows.filter { "charts_${it.first().code}" in visible }.flatten().forEach { viewModel.checkMarket(it) }
                delay(60_000)
            }
        }
    }
    IridePageLayout(
        title = stringResource(R.string.new_charts_all), state = listState, showTopBar = true,
        navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
    ) {
        item(key = "chart_search") {
            IrideFolioCollectionSearchBox(query, onQueryChange = { query = it },
                placeholderText = stringResource(R.string.new_charts_search), focusRequester = focusRequester,
                onFocusChanged = {}, onSearch = { focusManager.clearFocus() }, onClear = { query = TextFieldValue() },
                modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding, vertical = 8.dp))
        }
        items(rows, key = { "charts_${it.first().code}" }) { pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { market ->
                    ChartCard(market, charts.getValue(market.code).chart!!.tracks.size,
                        onClick = { navController.navigate("new_releases_chart/${market.code}") }, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (candidates.any { it.code !in discovered }) {
            item(key = "chart_discovery") {
                LaunchedEffect(candidates, discovered, checking) {
                    if (checking.isEmpty()) viewModel.discoverNext(candidates)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (available.isEmpty() && candidates.all { it.code in discovered } && checking.isEmpty() && query.text.isNotBlank()) {
            item(key = "chart_search_empty") {
                Text(stringResource(R.string.new_charts_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(IrideFolio.HorizontalPadding))
            }
        }
    }
}

@Composable
private fun rememberChartRegion(): String {
    val country by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)
    return remember(country, Locale.getDefault()) {
        val preferred = country.takeUnless { it.equals("system", true) || it == SYSTEM_DEFAULT } ?: Locale.getDefault().country
        preferred.lowercase(Locale.ROOT).takeIf { it in Locale.getISOCountries().map { code -> code.lowercase(Locale.ROOT) } } ?: "us"
    }
}

@Composable
private fun ChartCard(market: ChartMarket, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (tap, cover) = rememberCollectionExpansionTile(
        CollectionExpansionKind.Playlist, market.code,
    )
    val name = chartCountryName(market.code)
    val description = stringResource(R.string.new_charts_playlist_title, name)
    Column(tap.then(modifier)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).then(cover).clip(SquircleShape(7.dp))
            .semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick)) {
            IrideChartPoster(market.code, name, stringResource(R.string.new_charts_top_count, count), Modifier.fillMaxSize())
        }
    }
}
