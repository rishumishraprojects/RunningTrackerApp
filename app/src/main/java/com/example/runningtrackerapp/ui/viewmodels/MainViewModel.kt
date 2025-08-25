package com.example.runningtrackerapp.ui.viewModels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runningtrackerapp.db.Run
import com.example.runningtrackerapp.others.SortTypes
import com.example.runningtrackerapp.repositories.MainRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepositories: MainRepositories
): ViewModel() {

    val runsSortedByDate = mainRepositories.getAllRunsSortedByDate()
    val runsSortedByDistance = mainRepositories.getAllRunsSortedByDistance()
    val runsSortedByTimeInMillis = mainRepositories.getAllRunsSortedByTimeInMillis()
    val runsSortedByAvgSpeed = mainRepositories.getAllRunsSortedByAvgSpeed()
    val runsSortedByCaloriesBurned = mainRepositories.getAllRunsSortedByCaloriesBurned()

    val runs = MediatorLiveData<List<Run>>()
    var sortType = SortTypes.DATE
    init {
        runs.addSource(runsSortedByDate) { result ->
            if (sortType == SortTypes.DATE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByAvgSpeed) { result ->
            if (sortType == SortTypes.AVG_SPEED) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByCaloriesBurned) { result ->
            if (sortType == SortTypes.CALORIES_BURNED) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByDistance) { result ->
            if (sortType == SortTypes.DISTANCE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByTimeInMillis) { result ->
            if (sortType == SortTypes.RUNNING_TIME) {
                result?.let { runs.value = it }
            }
        }
    }

    fun sortRuns(sortType: SortTypes) = when(sortType) {
        SortTypes.DATE -> runsSortedByDate.value?.let { runs.value = it }
        SortTypes.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortTypes.AVG_SPEED -> runsSortedByAvgSpeed.value?.let { runs.value = it }
        SortTypes.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
        SortTypes.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
    }.also {
        this.sortType = sortType
    }


    fun insertRun(run : Run) = viewModelScope.launch {
        mainRepositories.insertRun(run)
    }
}