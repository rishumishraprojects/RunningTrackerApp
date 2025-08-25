package com.example.runningtrackerapp.others

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.db.Run
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    private val runs: List<Run>,
    c: Context,
    layoutId: Int
) : MarkerView(c, layoutId) {


    override fun getOffset(): com.github.mikephil.charting.utils.MPPointF {
        return com.github.mikephil.charting.utils.MPPointF(-width / 2f, -height.toFloat())
    }

    private val tvDate = findViewById<TextView>(R.id.tvDate)
    private val tvDuration = findViewById<TextView>(R.id.tvDuration)
    private val tvAvgSpeed = findViewById<TextView>(R.id.tvAvgSpeed)
    private val tvDistance = findViewById<TextView>(R.id.tvDistance)
    private val tvCaloriesBurned = findViewById<TextView>(R.id.tvCaloriesBurned)


    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if (e == null) return

        val curRunId = e.x.toInt()
        val run = runs[curRunId]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = run.timestamp
        }
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        tvDate.text = dateFormat.format(calendar.time)

        tvAvgSpeed.text = "${run.avgSpeedInKMH} km/h"
        tvDistance.text = "${run.distanceInMeters / 1000f} km"
        tvDuration.text = TrackingUtility.getFormattedStopWatch(run.timeInMillis)
        tvCaloriesBurned.text = "${run.caloriesBurned} kcal"
    }
}
