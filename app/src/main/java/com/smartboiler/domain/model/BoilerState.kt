package com.smartboiler.domain.model

/**
 * Represents the current status of the boiler system.
 */
enum class BoilerStatus(val label: String) {
    OFF("Off"),
    SCHEDULED("Scheduled"),
    HEATING("Heating"),
    READY("Ready"),
}

/**
 * Snapshot of the current boiler state shown on the dashboard.
 */
data class BoilerState(
    val status: BoilerStatus = BoilerStatus.OFF,
    val estimatedTempCelsius: Double = 0.0,
    val solarContributionPercent: Int = 0,
    val nextEventDescription: String? = null,
)
