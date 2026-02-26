package com.smartboiler.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.domain.repository.BoilerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryDay(
    val date: String,
    val schedules: List<ShowerScheduleEntity>,
)

data class HistoryUiState(
    val days: List<HistoryDay> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BoilerRepository,
) : ViewModel() {

    var uiState by mutableStateOf(HistoryUiState())
        private set

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val days = mutableListOf<HistoryDay>()

            // Load past 7 days
            for (i in 0..6) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.toString()
                val schedules = repository.getSchedulesForDate(dateStr)
                if (schedules.isNotEmpty()) {
                    days.add(HistoryDay(date = dateStr, schedules = schedules))
                }
            }

            uiState = HistoryUiState(days = days, isLoading = false)
        }
    }
}
