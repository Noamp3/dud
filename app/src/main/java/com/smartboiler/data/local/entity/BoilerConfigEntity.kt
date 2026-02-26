package com.smartboiler.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boiler_config")
data class BoilerConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capacityLiters: Int,
    val heatingPowerKw: Double,
    val desiredTempCelsius: Int,
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val avgShowerLiters: Int,
    val avgShowerMinutes: Int,
    val defaultPeopleCount: Int,
    val onboardingComplete: Boolean,
)
