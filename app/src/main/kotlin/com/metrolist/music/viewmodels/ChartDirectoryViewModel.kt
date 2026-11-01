package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.repository.ChartDirectoryRepository
import com.metrolist.music.repository.DailyChartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChartDirectoryViewModel @Inject constructor(
    private val directory: ChartDirectoryRepository,
    private val dailyCharts: DailyChartRepository,
) : ViewModel() {
    val catalog = directory.state
    val charts = dailyCharts.charts
    private val mutableChecking = MutableStateFlow<Set<String>>(emptySet())
    val checking = mutableChecking.asStateFlow()
    private val mutableDiscovered = MutableStateFlow<Set<String>>(emptySet())
    val discovered = mutableDiscovered.asStateFlow()

    init {
        viewModelScope.launch { dailyCharts.loadCachedCharts() }
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch { directory.refresh(force) }
    }

    fun checkMarket(market: ChartMarket, force: Boolean = false) {
        if (market.code in mutableChecking.value) return
        mutableChecking.update { it + market.code }
        viewModelScope.launch {
            try {
                directory.checkMarket(market, force)
            } finally {
                mutableChecking.update { it - market.code }
            }
        }
    }

    fun discoverNext(markets: List<ChartMarket>) {
        markets.filter { it.code !in mutableDiscovered.value }.take(8).forEach { market ->
            mutableDiscovered.update { it + market.code }
            checkMarket(market)
        }
    }
}
