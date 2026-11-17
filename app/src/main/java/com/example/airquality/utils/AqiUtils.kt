package com.example.airquality.utils

import com.example.airquality.R

object AqiUtils {

    fun getCategoryName(aqi: Int): String {
        return when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }

    fun getColorResId(aqi: Int): Int {
        return when {
            aqi <= 50 -> R.color.aqi_good
            aqi <= 100 -> R.color.aqi_moderate
            aqi <= 150 -> R.color.aqi_unhealthy_sensitive
            aqi <= 200 -> R.color.aqi_unhealthy
            aqi <= 300 -> R.color.aqi_very_unhealthy
            else -> R.color.aqi_hazardous
        }
    }

    fun getHealthAdvice(aqi: Int): String {
        return when {
            aqi <= 50 -> "Air quality is satisfactory, and air pollution poses little or no risk."
            aqi <= 100 -> "Air quality is acceptable. However, sensitive individuals may experience minor irritation."
            aqi <= 150 -> "Members of sensitive groups may experience health effects. The general public is less likely to be affected."
            aqi <= 200 -> "Everyone may begin to experience health effects; members of sensitive groups may experience more serious health effects."
            aqi <= 300 -> "Health alert: The risk of health effects is increased for everyone."
            else -> "Health warning of emergency conditions: everyone is more likely to be affected."
        }
    }
}
