package com.smartboiler.data.remote

import com.smartboiler.data.remote.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for the Open-Meteo Forecast API.
 * Free tier, no API key required.
 * Docs: https://open-meteo.com/en/docs
 */
interface WeatherApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,cloud_cover,direct_radiation,shortwave_radiation",
        @Query("daily") daily: String = "sunshine_duration",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timezone") timezone: String = "auto",
    ): WeatherResponse
}
