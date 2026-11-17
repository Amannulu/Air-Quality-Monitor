package com.example.airquality.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.api.RetrofitClient
import com.example.airquality.data.db.AppDatabase
import com.example.airquality.data.model.AirQualityReading
import com.example.airquality.data.model.AirQualityResponse
import com.example.airquality.utils.AqiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val readingDao = db.readingDao()

    val savedReadings: LiveData<List<AirQualityReading>> = readingDao.getAllReadings()

    private val _airQualityResponse = MutableLiveData<AirQualityResponse?>()
    val airQualityResponse: LiveData<AirQualityResponse?> = _airQualityResponse

    private val _hourlyList = MutableLiveData<List<HourlyAqiItem>>()
    val hourlyList: LiveData<List<HourlyAqiItem>> = _hourlyList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _currentLocationText = MutableLiveData<String>("Default (San Francisco)")
    val currentLocationText: LiveData<String> = _currentLocationText

    var currentLat: Double = 37.7749
    var currentLon: Double = -122.4194

    fun fetchAirQualityForLocation(lat: Double, lon: Double, locationName: String? = null) {
        currentLat = lat
        currentLon = lon
        val locStr = locationName ?: String.format(Locale.US, "Lat: %.2f, Lon: %.2f", lat, lon)
        _currentLocationText.value = locStr
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAirQuality(
                    latitude = lat,
                    longitude = lon,
                    current = "us_aqi,pm2_5,pm10,carbon_monoxide",
                    hourly = "us_aqi",
                    timezone = "auto"
                )
                _airQualityResponse.value = response
                processHourlyForecast(response)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch air quality: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun processHourlyForecast(response: AirQualityResponse) {
        val hourly = response.hourly ?: return
        val times = hourly.time ?: return
        val aqis = hourly.usAqi ?: return

        val items = mutableListOf<HourlyAqiItem>()

        val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault())
        val currentTimeIso = sdfIso.format(Date())

        var startIndex = 0
        for (i in times.indices) {
            if (times[i] >= currentTimeIso) {
                startIndex = i
                break
            }
        }

        val endIndex = minOf(startIndex + 24, times.size)
        for (i in startIndex until endIndex) {
            val rawTime = times[i]
            val aqiVal = aqis.getOrNull(i) ?: continue

            val displayTime = try {
                val parts = rawTime.split("T")
                if (parts.size > 1) parts[1] else rawTime
            } catch (e: Exception) {
                rawTime
            }

            items.add(HourlyAqiItem(timeFormatted = displayTime, aqi = aqiVal))
        }

        _hourlyList.value = items
    }

    fun saveCurrentReading() {
        val response = _airQualityResponse.value ?: return
        val current = response.current ?: return
        val aqi = current.usAqi ?: return

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val category = AqiUtils.getCategoryName(aqi)

        val reading = AirQualityReading(
            0,
            dateStr,
            aqi,
            category,
            _currentLocationText.value ?: String.format(Locale.US, "Lat: %.2f, Lon: %.2f", currentLat, currentLon),
            current.pm25 ?: 0.0,
            current.pm10 ?: 0.0,
            current.carbonMonoxide ?: 0.0,
            System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                readingDao.insertReading(reading)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Reading saved successfully!"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to save reading: ${e.localizedMessage}"
                }
            }
        }
    }

    fun deleteReading(reading: AirQualityReading) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                readingDao.deleteReading(reading)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to delete reading: ${e.localizedMessage}"
                }
            }
        }
    }
}
