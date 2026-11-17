package com.example.airquality.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.airquality.data.model.AirQualityReading
import com.example.airquality.databinding.ItemHistoryBinding
import com.example.airquality.utils.AqiUtils

class HistoryAdapter(
    private val onDeleteClick: (AirQualityReading) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var readings: List<AirQualityReading> = emptyList()

    fun submitList(newReadings: List<AirQualityReading>) {
        readings = newReadings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(readings[position])
    }

    override fun getItemCount(): Int = readings.size

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reading: AirQualityReading) {
            binding.tvHistoryDate.text = reading.dateFormatted
            binding.tvHistoryLocation.text = reading.location
            binding.tvHistoryDetails.text =
                "AQI: ${reading.aqi} (${reading.category}) | PM2.5: ${reading.pm25} | PM10: ${reading.pm10} | CO: ${reading.co}"

            val colorRes = AqiUtils.getColorResId(reading.aqi)
            val colorInt = ContextCompat.getColor(itemView.context, colorRes)
            binding.vCategoryColor.setBackgroundColor(colorInt)

            binding.btnDelete.setOnClickListener {
                onDeleteClick(reading)
            }
        }
    }
}
