package com.smartboiler.ui.feedback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.data.local.entity.FeedbackEntity
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.ShowerRating
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.usecase.AdjustBaselinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class FeedbackUiState(
    val schedule: ShowerScheduleEntity? = null,
    val submitted: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BoilerRepository,
    private val adjustBaselines: AdjustBaselinesUseCase,
) : ViewModel() {

    var uiState by mutableStateOf(FeedbackUiState())
        private set

    private val scheduleId: Long = savedStateHandle.get<Long>("scheduleId") ?: -1L

    init {
        viewModelScope.launch {
            val schedule = repository.getScheduleById(scheduleId)
            uiState = uiState.copy(schedule = schedule, isLoading = false)
        }
    }

    fun submitFeedback(rating: ShowerRating) {
        val schedule = uiState.schedule ?: return

        viewModelScope.launch {
            val dayType = runCatching { DayType.valueOf(schedule.dayType) }
                .getOrDefault(DayType.PARTLY_CLOUDY)

            // Save feedback
            repository.saveFeedback(
                FeedbackEntity(
                    scheduleId = schedule.id,
                    date = schedule.date,
                    dayType = dayType.name,
                    rating = rating.name,
                    heatingMinutesUsed = schedule.heatingDurationMinutes,
                    cloudCoverPercent = schedule.cloudCoverPercent,
                    timestamp = LocalDateTime.now().toString(),
                )
            )

            // Adjust baselines based on feedback
            adjustBaselines(rating, dayType)

            uiState = uiState.copy(submitted = true)
        }
    }
}
