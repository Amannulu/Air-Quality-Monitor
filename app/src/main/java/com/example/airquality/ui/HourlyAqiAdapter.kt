package com.example.airquality.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.airquality.databinding.ItemHourlyAqiBinding
import com.example.airquality.utils.AqiUtils

data class HourlyAqiItem(
    val timeFormatted: String,
    val aqi: Int
)

class HourlyAqiAdapter(
    private var items: List<HourlyAqiItem> = emptyList()
) : RecyclerView.Adapter<HourlyAqiAdapter.HourlyViewHolder>() {

    fun submitList(newItems: List<HourlyAqiItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = ItemHourlyAqiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HourlyViewHolder(private val binding: ItemHourlyAqiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyAqiItem) {
            binding.tvHourlyTime.text = item.timeFormatted
            binding.tvHourlyAqi.text = item.aqi.toString()

            val category = AqiUtils.getCategoryName(item.aqi)
            binding.tvHourlyCategory.text = category

            val colorRes = AqiUtils.getColorResId(item.aqi)
            val colorInt = ContextCompat.getColor(itemView.context, colorRes)
            binding.tvHourlyAqi.backgroundTintList = ColorStateList.valueOf(colorInt)
        }
    }
}
