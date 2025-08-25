package com.example.runningtrackerapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.db.Run
import com.example.runningtrackerapp.others.TrackingUtility
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    private var onItemClickListener: ((Run) -> Unit)? = null

    fun setOnItemClickListener(listener: (Run) -> Unit) {
        onItemClickListener = listener
    }

    inner class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivRunImage: ImageView = itemView.findViewById(R.id.ivRunImage)
        var tvAvgSpeed: MaterialTextView = itemView.findViewById(R.id.tvAvgSpeed)
        var tvDistance: MaterialTextView = itemView.findViewById(R.id.tvDistance)
        var tvDate: MaterialTextView = itemView.findViewById(R.id.tvDate)
        var tvTime: MaterialTextView = itemView.findViewById(R.id.tvTime)
        var tvCalories: MaterialTextView = itemView.findViewById(R.id.tvCalories)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        return RunViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_run,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]

        Glide.with(holder.itemView).load(run.img).into(holder.ivRunImage)

        val calendar = Calendar.getInstance().apply { timeInMillis = run.timestamp }
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(calendar.time)

        holder.tvAvgSpeed.text = "${run.avgSpeedInKMH} km/h"
        holder.tvDistance.text = "${run.distanceInMeters / 1000f} km"
        holder.tvTime.text = TrackingUtility.getFormattedStopWatch(run.timeInMillis)
        holder.tvCalories.text = "${run.caloriesBurned} kcal"

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(run)
        }
    }

    override fun getItemCount() = differ.currentList.size
}
