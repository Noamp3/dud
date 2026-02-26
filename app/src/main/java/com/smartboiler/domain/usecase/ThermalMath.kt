package com.smartboiler.domain.usecase

import kotlin.math.ceil

object ThermalMath {
    const val SPECIFIC_HEAT_KJ_PER_KG_C = 4.186
    const val SAFETY_MARGIN_MINUTES = 10

    fun calculateElectricGainCelsius(
        powerKw: Double,
        heatedVolumeLiters: Double,
        heatingMinutes: Int,
    ): Double {
        if (powerKw <= 0.0 || heatingMinutes <= 0 || heatedVolumeLiters <= 0.0) return 0.0
        val energyKj = powerKw * heatingMinutes * 60.0
        return energyKj / (heatedVolumeLiters * SPECIFIC_HEAT_KJ_PER_KG_C)
    }

    fun calculateHeatingDurationMinutes(
        tempDeficitCelsius: Double,
        heatedVolumeLiters: Double,
        powerKw: Double,
        safetyMarginMinutes: Int = SAFETY_MARGIN_MINUTES,
    ): Int {
        if (tempDeficitCelsius <= 0.0 || heatedVolumeLiters <= 0.0 || powerKw <= 0.0) return 0
        val heatingSeconds =
            (tempDeficitCelsius * heatedVolumeLiters * SPECIFIC_HEAT_KJ_PER_KG_C) / powerKw
        return ceil(heatingSeconds / 60.0).toInt() + safetyMarginMinutes.coerceAtLeast(0)
    }

    fun calculateEffectiveDeliveredTempCelsius(
        hotTempCelsius: Double,
        waterNeededLiters: Int,
        capacityLiters: Int,
        inletTempCelsius: Double,
    ): Double {
        if (waterNeededLiters <= 0 || capacityLiters <= 0) return inletTempCelsius
        val mixFactor = if (waterNeededLiters <= capacityLiters) 1.0
        else capacityLiters.toDouble() / waterNeededLiters

        return hotTempCelsius * mixFactor + inletTempCelsius * (1.0 - mixFactor)
    }
}