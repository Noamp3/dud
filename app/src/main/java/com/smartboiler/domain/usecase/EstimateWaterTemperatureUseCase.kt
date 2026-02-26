package com.smartboiler.domain.usecase

import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.model.WeatherForecast
import javax.inject.Inject
import kotlin.math.max

/**
 * Estimates the water temperature in the boiler tank at a given hour.
 *
 * Model:
 * 1. Solar gain — uses **actual hourly solar radiation data** from the weather API
 *    to estimate how much the sun has heated the water up to the requested hour.
 *    This correctly handles:
 *    - Morning (partial solar gain)
 *    - Evening (full day's solar gain minus standby losses since sunset)
 *    - Night (all solar gain from earlier, with accumulated loss)
 *
 * 2. Standby heat loss — heat lost after last significant solar radiation hour.
 *
 * 3. Electric heating gain — standard thermodynamics formula.
 */
data class TemperatureEstimate(
    /** Estimated water temperature in °C */
    val temperatureCelsius: Double,
    /** Solar contribution to the temperature in °C */
    val solarGainCelsius: Double,
    /** How much electric heating has contributed (if any) */
    val electricGainCelsius: Double,
    /** Estimated heat lost through standby */
    val standbyLossCelsius: Double,
    /** Estimated cold-water inlet temperature */
    val inletTempCelsius: Double,
)

class EstimateWaterTemperatureUseCase @Inject constructor() {

    companion object {
        /** Specific heat of water in kJ/(kg·°C) */
        private const val SPECIFIC_HEAT = 4.186

        /** Standby loss rate in °C per hour (well-insulated tank) */
        private const val STANDBY_LOSS_RATE = 0.5

        /** Typical cold water inlet temperature in °C */
        private const val COLD_WATER_INLET_TEMP = 15.0

        /** Effective collector area in m² for solar contribution estimation. */
        private const val EFFECTIVE_COLLECTOR_AREA_M2 = 2.0

        /** Effective thermal conversion efficiency of collector + piping + tank transfer. */
        private const val SOLAR_THERMAL_EFFICIENCY = 0.45

        /** Limit baseline correction effect to avoid overfitting extreme user feedback. */
        private const val MAX_BASELINE_CORRECTION_C = 8.0

        /** Maximum temperature the boiler can reach from solar alone */
        private const val MAX_SOLAR_TEMP = 70.0

        /** Minimum solar radiation (W/m²) to consider as "significant" */
        private const val SOLAR_THRESHOLD = 50.0
    }

    /**
     * Estimate the water temperature at [targetHour].
     *
     * @param config Boiler configuration (capacity, power, desired temp)
     * @param baselines User baseline heating durations used for day-type calibration
     * @param weather Current weather forecast (includes hourly solar radiation data)
     * @param targetHour Hour of day to estimate for (0–23)
     * @param electricHeatingMinutesToday Minutes of electric heating applied today
     */
    operator fun invoke(
        config: BoilerConfig,
        baselines: List<HeatingBaseline>,
        weather: WeatherForecast,
        targetHour: Int,
        electricHeatingMinutesToday: Int = 0,
    ): TemperatureEstimate {
        // 1. Calculate solar gain using actual hourly radiation data
        val solarGain = calculateSolarGain(config, weather, targetHour)

        // 1.1 Calibrate estimate by day-type baselines (feedback-driven, independent of target temp)
        val baselineCalibration = calculateBaselineCalibration(config, baselines, weather.dayType)

        // 2. Calculate electric heating gain
        val electricGain = calculateElectricGain(
            config.heatingPowerKw,
            config.capacityLiters,
            electricHeatingMinutesToday,
        )

        // 3. Calculate standby loss since the last hour with significant solar radiation
        val lastSolarHour = findLastSolarHour(weather, targetHour)
        val hoursSinceSolar = if (lastSolarHour >= 0) max(0, targetHour - lastSolarHour) else 0
        val standbyLoss = hoursSinceSolar * STANDBY_LOSS_RATE

        // 4. Combine: start from inlet temp, add gains, subtract loss
        val estimatedTemp = (COLD_WATER_INLET_TEMP + solarGain + baselineCalibration + electricGain - standbyLoss)
            .coerceIn(COLD_WATER_INLET_TEMP, MAX_SOLAR_TEMP)

        return TemperatureEstimate(
            temperatureCelsius = estimatedTemp,
            solarGainCelsius = (solarGain + baselineCalibration).coerceAtLeast(0.0),
            electricGainCelsius = electricGain,
            standbyLossCelsius = standbyLoss,
            inletTempCelsius = COLD_WATER_INLET_TEMP,
        )
    }

    /**
     * Calculate solar gain using actual hourly radiation data.
     *
     * Uses the real hourly solar radiation from the weather API to determine
     * how much of the full-day solar potential has been captured up to [targetHour].
     * This correctly handles evening showers where all solar radiation has already
     * been absorbed during the day.
     */
    private fun calculateSolarGain(
        config: BoilerConfig,
        weather: WeatherForecast,
        targetHour: Int,
    ): Double {
        val hourlyRadiation = weather.hourlySolarRadiation
        if (hourlyRadiation.isNotEmpty()) {
            val hoursToSum = (targetHour + 1).coerceAtMost(hourlyRadiation.size)
            val accumulatedRadiationWm2 = hourlyRadiation.take(hoursToSum).sum()

            // W/m² integrated over one-hour samples:
            // energy (kJ) = Σ[W/m²] × area × seconds / 1000
            val capturedSolarEnergyKj =
                accumulatedRadiationWm2 * EFFECTIVE_COLLECTOR_AREA_M2 * 3.6 * SOLAR_THERMAL_EFFICIENCY

            return capturedSolarEnergyKj / (config.capacityLiters * SPECIFIC_HEAT)
        }

        // Weather-based fallback when hourly radiation is unavailable.
        // Uses daily sunshine hours and current radiation as a conservative estimate.
        val fallbackEffectiveSunHours = weather.sunshineHoursToday.coerceIn(0.0, 12.0)
        val fallbackRadiationWm2 = weather.currentSolarRadiation.coerceAtLeast(0.0)
        val fallbackEnergyKj =
            fallbackRadiationWm2 * EFFECTIVE_COLLECTOR_AREA_M2 * fallbackEffectiveSunHours * 3.6 * SOLAR_THERMAL_EFFICIENCY

        return fallbackEnergyKj / (config.capacityLiters * SPECIFIC_HEAT)
    }

    private fun calculateBaselineCalibration(
        config: BoilerConfig,
        baselines: List<HeatingBaseline>,
        dayType: DayType,
    ): Double {
        if (baselines.isEmpty()) return 0.0

        val baselineForDayType = baselines.find { it.dayType == dayType }
            ?: baselines.find { it.dayType == DayType.PARTLY_CLOUDY }
            ?: return 0.0

        val averageBaselineMinutes = baselines.map { it.durationMinutes }.average()
        val minuteDelta = averageBaselineMinutes - baselineForDayType.durationMinutes

        val gainPerMinute = ThermalMath.calculateElectricGainCelsius(
            powerKw = config.heatingPowerKw,
            heatedVolumeLiters = config.capacityLiters.toDouble(),
            heatingMinutes = 1,
        )

        return (minuteDelta * gainPerMinute)
            .coerceIn(-MAX_BASELINE_CORRECTION_C, MAX_BASELINE_CORRECTION_C)
    }

    /**
     * Find the last hour (up to [targetHour]) that had significant solar radiation.
     * Used to calculate standby losses after solar heating stops.
     */
    private fun findLastSolarHour(weather: WeatherForecast, targetHour: Int): Int {
        val hourlyRadiation = weather.hourlySolarRadiation
        if (hourlyRadiation.isEmpty()) {
            val estimatedSunEndHour = (6.0 + weather.sunshineHoursToday)
                .toInt()
                .coerceIn(0, targetHour)
            return if (weather.sunshineHoursToday > 0.0) estimatedSunEndHour else -1
        }

        val searchRange = (targetHour).coerceAtMost(hourlyRadiation.size - 1)
        for (hour in searchRange downTo 0) {
            if (hourlyRadiation[hour] > SOLAR_THRESHOLD) return hour
        }
        return -1 // no solar radiation found
    }

    /**
     * Standard thermodynamics: ΔT = (P × t) / (m × c)
     * P = power in kW, t = time in seconds, m = mass in kg, c = specific heat
     */
    private fun calculateElectricGain(
        powerKw: Double,
        capacityLiters: Int,
        heatingMinutes: Int,
    ): Double = ThermalMath.calculateElectricGainCelsius(
        powerKw = powerKw,
        heatedVolumeLiters = capacityLiters.toDouble(),
        heatingMinutes = heatingMinutes,
    )
}
