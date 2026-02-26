package com.smartboiler.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heating_baseline")
data class HeatingBaselineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayType: String, // SUNNY, PARTLY_CLOUDY, CLOUDY
    val durationMinutes: Int,
)
