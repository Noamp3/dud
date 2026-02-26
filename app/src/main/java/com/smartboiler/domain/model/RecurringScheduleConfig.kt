package com.smartboiler.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class RecurringScheduleConfig(
    val recurrenceGroupId: String,
    val startDate: String,
    val scheduledTime: LocalTime,
    val peopleCount: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val enabled: Boolean,
)
