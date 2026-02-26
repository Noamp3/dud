package com.smartboiler.data.repository

import com.smartboiler.data.remote.WeatherApiService
import com.smartboiler.data.remote.model.DailyData
import com.smartboiler.data.remote.model.HourlyData
import com.smartboiler.domain.model.WeatherForecast
import com.smartboiler.domain.repository.WeatherRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote weather repository backed by Open-Meteo API.
 * Parses the hourly/daily data and produces a domain [WeatherForecast].
 */
@Singleton
class RemoteWeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService,
) : WeatherRepository {

    companion object {
        private const val FORECAST_DAYS_TO_FETCH = 10
        private const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
    }

    private data class CacheKey(
        val latitudeRounded: String,
        val longitudeRounded: String,
        val targetDate: LocalDate,
    )

    private data class CacheEntry(
        val forecast: WeatherForecast,
        val savedAtMillis: Long,
    )

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    private fun cacheKey(latitude: Double, longitude: Double, targetDate: LocalDate): CacheKey {
        // Round coords to avoid tiny floating point differences creating cache misses.
        val latRounded = "%.4f".format(java.util.Locale.US, latitude)
        val lonRounded = "%.4f".format(java.util.Locale.US, longitude)
        return CacheKey(latRounded, lonRounded, targetDate)
    }

    private fun CacheEntry.isFresh(nowMillis: Long): Boolean =
        nowMillis - savedAtMillis <= CACHE_TTL_MILLIS

    override suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate,
    ): Result<WeatherForecast> {
        val nowMillis = System.currentTimeMillis()
        val key = cacheKey(latitude, longitude, targetDate)
        val cached = cache[key]

        if (cached != null && cached.isFresh(nowMillis)) {
            return Result.success(cached.forecast)
        }

        val networkResult = runCatching {
            val today = LocalDate.now()
            val response = weatherApi.getForecast(
                latitude = latitude,
                longitude = longitude,
                forecastDays = FORECAST_DAYS_TO_FETCH,
            )

            val hourly = response.hourly
                ?: throw IllegalStateException("No hourly data in weather response")
            val daily = response.daily

            val nowHour = LocalDateTime.now().hour
            val availableDates = hourly.time
                .mapNotNull { timestamp ->
                    timestamp.substringBefore("T").let { dateText ->
                        runCatching { LocalDate.parse(dateText) }.getOrNull()
                    }
                }
                .distinct()

            availableDates.forEach { date ->
                val forecast = buildForecastForDate(
                    hourly = hourly,
                    daily = daily,
                    date = date,
                    today = today,
                    nowHour = nowHour,
                ) ?: return@forEach

                cache[cacheKey(latitude, longitude, date)] =
                    CacheEntry(forecast = forecast, savedAtMillis = nowMillis)
            }

            cache[key]?.forecast
                ?: throw IllegalStateException("No hourly weather data for selected date")
        }

        return networkResult
            .onSuccess { forecast ->
                cache[key] = CacheEntry(forecast = forecast, savedAtMillis = nowMillis)
            }
            .recoverCatching {
                // If network fails but we have previous cached data (even stale), use it.
                cached?.forecast ?: throw it
            }
    }

    private fun buildForecastForDate(
        hourly: HourlyData,
        daily: DailyData?,
        date: LocalDate,
        today: LocalDate,
        nowHour: Int,
    ): WeatherForecast? {
        val dateStr = date.toString()
        val targetHour = if (date == today) nowHour else 12

        val targetIndices = hourly.time.indices.filter { i ->
            hourly.time[i].startsWith(dateStr)
        }
        if (targetIndices.isEmpty()) return null

        val targetTemps = targetIndices.map { hourly.temperature2m[it] }
        val targetCloudCover = targetIndices.map { hourly.cloudCover[it] }
        val targetSolarRadiation = targetIndices.map { hourly.shortwaveRadiation[it] }

        val currentIndex = targetIndices.indexOfFirst { i ->
            val hour = hourly.time[i].substringAfterLast("T").substringBefore(":").toIntOrNull()
            hour != null && hour >= targetHour
        }.takeIf { it >= 0 } ?: (targetIndices.size - 1).coerceAtLeast(0)

        val currentTemp = targetTemps.getOrElse(currentIndex) { 20.0 }
        val currentRadiation = targetSolarRadiation.getOrElse(currentIndex) { 0.0 }

        val avgCloudCover = if (targetCloudCover.isNotEmpty()) {
            targetCloudCover.average().toInt()
        } else {
            50
        }

        val sunshineHours = daily?.let {
            val dayIndex = it.time.indexOf(dateStr)
            if (dayIndex >= 0) it.sunshineDuration.getOrNull(dayIndex)?.div(3600.0) else null
        } ?: 0.0

        return WeatherForecast(
            currentTempCelsius = currentTemp,
            cloudCoverPercent = avgCloudCover,
            currentSolarRadiation = currentRadiation,
            sunshineHoursToday = sunshineHours,
            dayType = WeatherForecast.deriveDayType(avgCloudCover),
            hourlyTemps = targetTemps,
            hourlyCloudCover = targetCloudCover,
            hourlySolarRadiation = targetSolarRadiation,
        )
    }
}
