package com.smartboiler.domain.usecase

import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.model.ShowerSchedule
import com.smartboiler.domain.model.WeatherForecast
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.min

/**
 * Calculates the optimal heating plan for a scheduled shower.
 *
 * Given the number of people, desired shower time, weather forecast, and boiler config,
 * determines whether electric heating is needed, how long, and when to start.
 */
class CalculateHeatingPlanUseCase @Inject constructor(
    private val estimateTemp: EstimateWaterTemperatureUseCase,
) {

    /**
     * Calculate the heating plan.
     *
     * @param peopleCount Number of people showering
     * @param scheduledTime Desired shower time
     * @param config Boiler configuration
     * @param baselines User's heating baselines
     * @param weather Current weather forecast
     * @return A [ShowerSchedule] with the complete heating plan
     */
    operator fun invoke(
        peopleCount: Int,
        scheduledDate: LocalDate,
        scheduledTime: LocalTime,
        config: BoilerConfig,
        baselines: List<HeatingBaseline>,
        weather: WeatherForecast,
        now: LocalDateTime = LocalDateTime.now(),
    ): ShowerSchedule {
        val scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime)
        if (!scheduledDateTime.isAfter(now)) {
            throw IllegalArgumentException("Scheduled shower must be in the future")
        }

        // Estimate temperature at shower time (without any electric heating today)
        val solarEstimate = estimateTemp(
            config = config,
            baselines = baselines,
            weather = weather,
            targetHour = scheduledTime.hour,
            electricHeatingMinutesToday = 0,
        )

        val solarTemp = solarEstimate.temperatureCelsius

        // Calculate water needed
        val waterNeeded = peopleCount * config.avgShowerLiters

        // In a stratified tank, showers draw from the top first.
        // We only need to heat the top volume that is expected to be used.
        val requiredHeatedVolumeLiters = min(waterNeeded, config.capacityLiters).toDouble()

        val effectiveSolarTemp = calculateEffectiveDeliveredTemp(
            hotTempCelsius = solarTemp,
            waterNeededLiters = waterNeeded,
            capacityLiters = config.capacityLiters,
            inletTempCelsius = solarEstimate.inletTempCelsius,
        )

        // Determine if heating is needed
        val tempDeficit = config.desiredTempCelsius - effectiveSolarTemp

        if (tempDeficit <= 0) {
            // Solar is enough! No electric heating needed
            return ShowerSchedule(
                date = scheduledDate,
                scheduledTime = scheduledTime,
                dayType = weather.dayType,
                cloudCoverPercent = weather.cloudCoverPercent,
                peopleCount = peopleCount,
                heatingRequired = false,
                heatingDurationMinutes = 0,
                heatingStartTime = null,
                estimatedSolarTempCelsius = solarTemp,
                estimatedFinalTempCelsius = effectiveSolarTemp,
                waterNeededLiters = waterNeeded,
            )
        }

        val heatingMinutes = ThermalMath.calculateHeatingDurationMinutes(
            tempDeficitCelsius = tempDeficit,
            heatedVolumeLiters = requiredHeatedVolumeLiters,
            powerKw = config.heatingPowerKw,
        )

        // Calculate start time
        val startDateTime = scheduledDateTime.minusMinutes(heatingMinutes.toLong())
        if (startDateTime.isBefore(now) || startDateTime.toLocalDate() != scheduledDate) {
            throw IllegalStateException(
                "Not enough time to heat water before this shower. Choose a later time or fewer people"
            )
        }
        val startTime = startDateTime.toLocalTime()

        // Final estimated delivered temp with heating, based on heated top layer volume
        val effectiveHeatingMinutes = heatingMinutes - ThermalMath.SAFETY_MARGIN_MINUTES
        val heatedLayerGain = calculateElectricGain(
            powerKw = config.heatingPowerKw,
            heatedVolumeLiters = requiredHeatedVolumeLiters,
            heatingMinutes = effectiveHeatingMinutes,
        )
        val heatedLayerTemp = solarTemp + heatedLayerGain
        val effectiveFinalTemp = calculateEffectiveDeliveredTemp(
            hotTempCelsius = heatedLayerTemp,
            waterNeededLiters = waterNeeded,
            capacityLiters = config.capacityLiters,
            inletTempCelsius = solarEstimate.inletTempCelsius,
        )

        return ShowerSchedule(
            date = scheduledDate,
            scheduledTime = scheduledTime,
            dayType = weather.dayType,
            cloudCoverPercent = weather.cloudCoverPercent,
            peopleCount = peopleCount,
            heatingRequired = true,
            heatingDurationMinutes = heatingMinutes,
            heatingStartTime = startTime,
            estimatedSolarTempCelsius = solarTemp,
            estimatedFinalTempCelsius = effectiveFinalTemp,
            waterNeededLiters = waterNeeded,
        )
    }

    private fun calculateEffectiveDeliveredTemp(
        hotTempCelsius: Double,
        waterNeededLiters: Int,
        capacityLiters: Int,
        inletTempCelsius: Double,
    ): Double = ThermalMath.calculateEffectiveDeliveredTempCelsius(
        hotTempCelsius = hotTempCelsius,
        waterNeededLiters = waterNeededLiters,
        capacityLiters = capacityLiters,
        inletTempCelsius = inletTempCelsius,
    )

    private fun calculateElectricGain(
        powerKw: Double,
        heatedVolumeLiters: Double,
        heatingMinutes: Int,
    ): Double = ThermalMath.calculateElectricGainCelsius(
        powerKw = powerKw,
        heatedVolumeLiters = heatedVolumeLiters,
        heatingMinutes = heatingMinutes,
    )
}
