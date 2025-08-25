package com.example.runningtrackerapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.CustomMarkerView
import com.example.runningtrackerapp.others.TrackingUtility
import com.example.runningtrackerapp.ui.viewModels.MainViewModel
import com.example.runningtrackerapp.ui.viewModels.StatisticsViewModel
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment: Fragment(R.layout.fragment_statistics) {
    private  val viewModel : StatisticsViewModel by viewModels()
    private lateinit var tvTotalTime: com.google.android.material.textview.MaterialTextView
    private lateinit var tvTotalDistance: com.google.android.material.textview.MaterialTextView
    private lateinit var tvAverageSpeed: com.google.android.material.textview.MaterialTextView
    private lateinit var tvTotalCalories: com.google.android.material.textview.MaterialTextView
    private lateinit var barChart : com.github.mikephil.charting.charts.BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance)
        tvAverageSpeed = view.findViewById(R.id.tvAverageSpeed)
        tvTotalCalories = view.findViewById(R.id.tvTotalCalories)
        barChart = view.findViewById(R.id.barChart)
        setupBarChart()
        subscribeToObservers()
    }
    private fun setupBarChart(){
        barChart.xAxis.apply{
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.axisLeft.apply{
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.axisRight.apply{
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.apply{
            description.text = "Avg Speed Over Time"
            legend.isEnabled = true
        }
    }

    private fun subscribeToObservers(){
        viewModel.totalTimeRun.observe(viewLifecycleOwner, Observer {
            it?.let{
                val totalTimeRun = TrackingUtility.getFormattedStopWatch(it)
                tvTotalTime.text = totalTimeRun
            }
        })

        viewModel.totalDistance.observe(viewLifecycleOwner, Observer{
            it?.let{
                val km = it
                val totalDistance = round((km / 1000f) * 10f) / 10f
                val totalDistanceString = "${totalDistance}km"
                tvTotalDistance.text = totalDistanceString

            }
        })

        viewModel.totalAvgSpeed.observe(viewLifecycleOwner, Observer{
            it?.let{
                val avgSpeed = round(it * 10f) / 10f
                val avgSpeedString = "${avgSpeed}km/h"
                tvAverageSpeed.text = avgSpeedString
            }
        })

        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner, Observer{
           it?.let {
              val totalCalories = "${it}kcal"
               tvTotalCalories.text = totalCalories

           }
        })
        viewModel.runsSortedByDate.observe(viewLifecycleOwner, Observer {
            it?.let{
                val allAvgSpeeds = it.indices.map{i -> BarEntry(i.toFloat(), it[i].avgSpeedInKMH)}
                val bardataSet = com.github.mikephil.charting.data.BarDataSet(allAvgSpeeds, "Avg Speed Over Time").apply{
                    valueTextColor = Color.WHITE
                    color = ContextCompat.getColor(requireContext(), R.color.white)
                }
                barChart.data = com.github.mikephil.charting.data.BarData(bardataSet)
                barChart.marker =
                    CustomMarkerView(it.reversed(), requireContext(), R.layout.marker_view)
                barChart.invalidate()
            }
        })
    }
}