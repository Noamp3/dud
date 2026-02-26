package com.smartboiler.domain.usecase

import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.model.ShowerRating
import com.smartboiler.domain.repository.BoilerRepository
import javax.inject.Inject

/**
 * Adjusts the user's baseline heating durations based on post-shower feedback.
 *
 * When feedback is tagged with a [DayType], the corresponding baseline is
 * increased or decreased so future predictions are more accurate.
 */
class AdjustBaselinesUseCase @Inject constructor(
    private val repository: BoilerRepository,
) {
    companion object {
        /** Minutes to adjust per feedback event */
        private const val ADJUSTMENT_STEP = 10
    }

    suspend operator fun invoke(rating: ShowerRating, dayType: DayType) {
        val baselines = repository.getBaselines().toMutableList()

        val index = baselines.indexOfFirst { it.dayType == dayType }
        if (index < 0) return // no baseline for this day type

        val current = baselines[index]
        val newDuration = when (rating) {
            ShowerRating.TOO_COLD -> current.durationMinutes + ADJUSTMENT_STEP
            ShowerRating.TOO_HOT -> (current.durationMinutes - ADJUSTMENT_STEP).coerceAtLeast(0)
            ShowerRating.JUST_RIGHT -> return // no change needed
        }

        baselines[index] = current.copy(durationMinutes = newDuration)
        repository.saveBaselines(baselines)
    }
}
