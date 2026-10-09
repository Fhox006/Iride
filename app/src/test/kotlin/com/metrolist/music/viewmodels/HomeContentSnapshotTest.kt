package com.metrolist.music.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeContentSnapshotTest {
    @Test fun queuesMultipleCategoriesAndKeepsCollectingWhenHomeIsNotComposed() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val snapshot = HomeContentSnapshot(scope)
            val songs = MutableStateFlow(listOf("old song"))
            val charts = MutableStateFlow(listOf("old chart"))
            val shownSongs = snapshot.state("songs", songs, List<String>::isNotEmpty)
            val shownCharts = snapshot.state("charts", charts, List<String>::isNotEmpty)
            songs.value = listOf("new song")
            charts.value = listOf("new chart")
            songs.value = listOf("latest song")
            assertEquals(listOf("old song"), shownSongs.value)
            assertEquals(listOf("old chart"), shownCharts.value)
            snapshot.setVisible(false)
            assertEquals(listOf("latest song"), shownSongs.value)
            assertEquals(listOf("new chart"), shownCharts.value)
            charts.value = listOf("background chart")
            snapshot.setVisible(true)
            assertEquals(listOf("background chart"), snapshot.state("charts", charts, List<String>::isNotEmpty).value)
            charts.value = emptyList()
            assertEquals(listOf("background chart"), shownCharts.value)
        } finally {
            scope.cancel()
        }
    }
}
