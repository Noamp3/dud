package com.smartboiler.data.local.mapper

import com.smartboiler.data.local.entity.BoilerConfigEntity
import com.smartboiler.data.local.entity.HeatingBaselineEntity
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.HeatingBaseline

/** Map Room entity → domain model */
fun BoilerConfigEntity.toDomain(): BoilerConfig = BoilerConfig(
    id = id,
    capacityLiters = capacityLiters,
    heatingPowerKw = heatingPowerKw,
    desiredTempCelsius = desiredTempCelsius,
    latitude = latitude,
    longitude = longitude,
    cityName = cityName,
    avgShowerLiters = avgShowerLiters,
    avgShowerMinutes = avgShowerMinutes,
    defaultPeopleCount = defaultPeopleCount,
    onboardingComplete = onboardingComplete,
)

/** Map domain model → Room entity */
fun BoilerConfig.toEntity(): BoilerConfigEntity = BoilerConfigEntity(
    id = id,
    capacityLiters = capacityLiters,
    heatingPowerKw = heatingPowerKw,
    desiredTempCelsius = desiredTempCelsius,
    latitude = latitude,
    longitude = longitude,
    cityName = cityName,
    avgShowerLiters = avgShowerLiters,
    avgShowerMinutes = avgShowerMinutes,
    defaultPeopleCount = defaultPeopleCount,
    onboardingComplete = onboardingComplete,
)

fun HeatingBaselineEntity.toDomain(): HeatingBaseline = HeatingBaseline(
    id = id,
    dayType = DayType.valueOf(dayType),
    durationMinutes = durationMinutes,
)

fun HeatingBaseline.toEntity(): HeatingBaselineEntity = HeatingBaselineEntity(
    id = id,
    dayType = dayType.name,
    durationMinutes = durationMinutes,
)
