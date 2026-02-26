package com.smartboiler.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shower_schedule")
data class ShowerScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO date "2026-02-23"
    val scheduledTime: String, // "18:00"
    val isRecurringDaily: Boolean = false,
    val recurrenceGroupId: String? = null,
    val recurrenceDaysCsv: String? = null,
    val isRecurringTemplate: Boolean = false,
    val isRecurringEnabled: Boolean = true,
    val dayType: String,
    val cloudCoverPercent: Int,
    val peopleCount: Int,
    val heatingRequired: Boolean,
    val heatingDurationMinutes: Int,
    val heatingStartTime: String?, // "17:15"
    val estimatedSolarTempCelsius: Double,
    val estimatedFinalTempCelsius: Double,
    val waterNeededLiters: Int,
)
