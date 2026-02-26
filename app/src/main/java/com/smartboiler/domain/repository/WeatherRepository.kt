package com.smartboiler.domain.repository

import com.smartboiler.domain.model.WeatherForecast
import java.time.LocalDate

/**
 * Repository interface for weather data.
 * Currently backed by Open-Meteo API, but can be swapped for
 * a cached/offline implementation or a different provider.
 */
interface WeatherRepository {

    /** Fetch current weather forecast for the given coordinates. */
    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate = LocalDate.now(),
    ): Result<WeatherForecast>
}
