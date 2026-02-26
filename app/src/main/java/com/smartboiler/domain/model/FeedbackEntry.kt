package com.smartboiler.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Rating for post-shower feedback.
 */
enum class ShowerRating(val emoji: String, val label: String) {
    TOO_COLD("🥶", "Not enough hot water"),
    JUST_RIGHT("👍", "Just right"),
    TOO_HOT("🔥", "Too hot — could have heated less"),
}

/**
 * A feedback entry after a scheduled shower.
 * Tagged with weather conditions so corrections apply to similar future days.
 */
data class FeedbackEntry(
    val id: Long = 0,
    val scheduleId: Long,
    val date: LocalDate,
    val dayType: DayType,
    val rating: ShowerRating,
    val heatingMinutesUsed: Int,
    val cloudCoverPercent: Int,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
