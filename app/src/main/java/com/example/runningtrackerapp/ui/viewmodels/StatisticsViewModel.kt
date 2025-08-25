package com.example.runningtrackerapp.ui.viewModels

import androidx.lifecycle.ViewModel
import com.example.runningtrackerapp.repositories.MainRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepositories: MainRepositories
): ViewModel() {
    val totalTimeRun = mainRepositories.getTotalTimeInMillis()
    val totalDistance = mainRepositories.getTotalDistance()
    val totalCaloriesBurned = mainRepositories.getTotalCaloriesBurned()
    val totalAvgSpeed = mainRepositories.getTotalAvgSpeed()

    val runsSortedByDate = mainRepositories.getAllRunsSortedByDate()
}