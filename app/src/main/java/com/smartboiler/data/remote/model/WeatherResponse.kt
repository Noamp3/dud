package com.smartboiler.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo Forecast API response.
 *
 * Example request:
 * https://api.open-meteo.com/v1/forecast
 *   ?latitude=32.08&longitude=34.78
 *   &hourly=temperature_2m,cloud_cover,direct_radiation
 *   &daily=sunshine_duration
 *   &forecast_days=2
 *   &timezone=auto
 */
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val timezone: String?,
    val hourly: HourlyData?,
    val daily: DailyData?,
)

data class HourlyData(
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>,
    @SerializedName("cloud_cover")
    val cloudCover: List<Double>,
    @SerializedName("direct_radiation")
    val directRadiation: List<Double>,
    @SerializedName("shortwave_radiation")
    val shortwaveRadiation: List<Double>,
)

data class DailyData(
    val time: List<String>,
    @SerializedName("sunshine_duration")
    val sunshineDuration: List<Double>, // seconds
)
