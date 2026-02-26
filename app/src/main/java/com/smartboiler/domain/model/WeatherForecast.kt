package com.smartboiler.domain.model

/**
 * Processed weather forecast for the boiler algorithm.
 * Derived from raw API data.
 */
data class WeatherForecast(
    /** Current ambient temperature in °C */
    val currentTempCelsius: Double,

    /** Average cloud cover for today (0–100%) */
    val cloudCoverPercent: Int,

    /** Current direct solar radiation in W/m² */
    val currentSolarRadiation: Double,

    /** Total sunshine duration today in hours */
    val sunshineHoursToday: Double,

    /** Derived day type based on cloud cover */
    val dayType: DayType,

    /** Hourly temperatures for today (24 values) */
    val hourlyTemps: List<Double> = emptyList(),

    /** Hourly cloud cover for today (24 values) */
    val hourlyCloudCover: List<Double> = emptyList(),

    /** Hourly solar radiation for today (24 values) */
    val hourlySolarRadiation: List<Double> = emptyList(),
) {
    companion object {
        /** Derive day type from average cloud cover percentage. */
        fun deriveDayType(cloudCoverPercent: Int): DayType = when {
            cloudCoverPercent < 30 -> DayType.SUNNY
            cloudCoverPercent < 65 -> DayType.PARTLY_CLOUDY
            else -> DayType.CLOUDY
        }
    }
}
