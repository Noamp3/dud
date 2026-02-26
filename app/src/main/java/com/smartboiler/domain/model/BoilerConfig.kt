package com.smartboiler.domain.model

/**
 * Core domain model representing the user's boiler configuration.
 * This is the single source of truth used by all use cases.
 */
data class BoilerConfig(
    val id: Long = 0,
    val capacityLiters: Int,
    val heatingPowerKw: Double,
    val desiredTempCelsius: Int = 40,
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val avgShowerLiters: Int = 50,
    val avgShowerMinutes: Int = 8,
    val defaultPeopleCount: Int = 2,
    val onboardingComplete: Boolean = false,
)
