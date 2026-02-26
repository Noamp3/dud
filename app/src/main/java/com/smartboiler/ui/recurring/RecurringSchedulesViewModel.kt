package com.smartboiler.ui.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.domain.model.RecurringScheduleConfig
import com.smartboiler.domain.repository.BoilerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RecurringScheduleItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean,
)

data class RecurringSchedulesUiState(
    val items: List<RecurringScheduleItemUi> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class RecurringSchedulesViewModel @Inject constructor(
    private val repository: BoilerRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RecurringSchedulesUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val items = repository.getRecurringSchedules()
                .map { config -> config.toRecurringItemUi() }
            uiState = RecurringSchedulesUiState(items = items, isLoading = false)
        }
    }

    fun setEnabled(groupId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRecurringScheduleEnabled(groupId, enabled)
            if (enabled) {
                repository.syncRecurringSchedules(fromDate = java.time.LocalDate.now(), daysAhead = 30)
            }
            refresh()
        }
    }

    fun delete(groupId: String) {
        viewModelScope.launch {
            repository.deleteRecurringSchedule(groupId)
            refresh()
        }
    }

    private fun RecurringScheduleConfig.toRecurringItemUi(): RecurringScheduleItemUi {
        val orderedDays = daysOfWeek
            .sortedBy { it.value }
            .joinToString(" ") { it.name.take(3) }

        return RecurringScheduleItemUi(
            id = recurrenceGroupId,
            title = "${scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm"))} · ${peopleCount} people",
            subtitle = orderedDays,
            enabled = enabled,
        )
    }
}
