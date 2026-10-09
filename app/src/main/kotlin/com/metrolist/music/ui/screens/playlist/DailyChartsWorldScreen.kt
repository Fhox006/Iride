package com.metrolist.music.ui.screens.playlist

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.viewmodels.DailyChartsHomeViewModel

@Composable
fun DailyChartsWorldScreen(navController: NavController, viewModel: DailyChartsHomeViewModel = hiltViewModel()) {
    AllChartsScreen(navController)
}