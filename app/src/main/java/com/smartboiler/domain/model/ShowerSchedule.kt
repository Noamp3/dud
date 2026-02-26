package com.smartboiler.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A scheduled shower event with calculated heating plan.
 */
data class ShowerSchedule(
    val id: Long = 0,
    val date: LocalDate,
    val scheduledTime: LocalTime,
    /** Weather day type used when this schedule was calculated */
    val dayType: DayType,
    /** Average cloud cover (%) used when this schedule was calculated */
    val cloudCoverPercent: Int,
    val peopleCount: Int,
    /** Whether electric heating is needed */
    val heatingRequired: Boolean,
    /** Duration of electric heating in minutes (0 if not needed) */
    val heatingDurationMinutes: Int,
    /** Time to start electric heating */
    val heatingStartTime: LocalTime?,
    /** Estimated water temperature at shower time without electric heating */
    val estimatedSolarTempCelsius: Double,
    /** Estimated water temperature at shower time with heating plan */
    val estimatedFinalTempCelsius: Double,
    /** Water needed in liters (people × avg per shower) */
    val waterNeededLiters: Int,
)
