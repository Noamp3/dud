package com.smartboiler.domain.model

/**
 * Represents the user's baseline heating duration for a given weather condition.
 * These baselines seed the algorithm before feedback data is available.
 */
data class HeatingBaseline(
    val id: Long = 0,
    val dayType: DayType,
    val durationMinutes: Int,
)

/**
 * Weather condition categories used for baseline heating durations.
 */
enum class DayType(val label: String, val emoji: String) {
    SUNNY("Sunny", "☀️"),
    PARTLY_CLOUDY("Partly Cloudy", "⛅"),
    CLOUDY("Cloudy / Rainy", "☁️"),
}
