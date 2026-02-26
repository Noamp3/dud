package com.smartboiler.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val date: String,
    val dayType: String,
    val rating: String,
    val heatingMinutesUsed: Int,
    val cloudCoverPercent: Int,
    val timestamp: String,
)
