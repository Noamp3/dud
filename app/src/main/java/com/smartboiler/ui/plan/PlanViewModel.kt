package com.smartboiler.ui.plan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.domain.model.ShowerSchedule
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.repository.WeatherRepository
import com.smartboiler.domain.usecase.CalculateHeatingPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class PlanUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedHour: Int = 18,
    val selectedMinute: Int = 0,
    val desiredTempCelsius: Int = 40,
    val showersCount: Int = 1,
    val weatherSummary: String = "",
    val heatingPlan: ShowerSchedule? = null,
    val isCalculating: Boolean = false,
    val warning: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val boilerRepository: BoilerRepository,
    private val weatherRepository: WeatherRepository,
    private val calculateHeatingPlan: CalculateHeatingPlanUseCase,
) : ViewModel() {

    companion object {
        private const val MAX_ESTIMATION_DAYS_AHEAD = 10L
    }

    var uiState by mutableStateOf(PlanUiState())
        private set

    init {
        val defaultDateTime = LocalDateTime.now()
            .plusHours(1)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        uiState = uiState.copy(
            selectedDate = defaultDateTime.toLocalDate(),
            selectedHour = defaultDateTime.hour,
            selectedMinute = defaultDateTime.minute,
        )

        viewModelScope.launch {
            boilerRepository.getConfig()?.let { config ->
                uiState = uiState.copy(
                    desiredTempCelsius = config.desiredTempCelsius,
                )
            }
            recalculate()
        }
    }

    fun updateDesiredTemp(temp: Int) {
        uiState = uiState.copy(desiredTempCelsius = temp.coerceIn(35, 60))
        recalculate()
    }

    fun updateShowersCount(count: Int) {
        uiState = uiState.copy(showersCount = count.coerceIn(1, 6))
        recalculate()
    }

    fun updateDate(date: LocalDate) {
        uiState = uiState.copy(selectedDate = date)
        recalculate()
    }

    fun updateTime(hour: Int, minute: Int) {
        uiState = uiState.copy(selectedHour = hour, selectedMinute = minute)
        recalculate()
    }

    private fun recalculate() {
        uiState = uiState.copy(isCalculating = true, error = null, warning = null)
        viewModelScope.launch {
            try {
                val daysAhead = Duration.between(
                    LocalDate.now().atStartOfDay(),
                    uiState.selectedDate.atStartOfDay(),
                ).toDays()
                if (daysAhead > MAX_ESTIMATION_DAYS_AHEAD) {
                    uiState = uiState.copy(
                        heatingPlan = null,
                        isCalculating = false,
                        warning = "Planning is available up to 10 days ahead",
                    )
                    return@launch
                }

                val config = boilerRepository.getConfig()
                    ?: throw IllegalStateException("No boiler config found")
                val baselines = boilerRepository.getBaselines()
                val weather = weatherRepository.getWeatherForecast(
                    config.latitude,
                    config.longitude,
                    uiState.selectedDate,
                ).getOrThrow()

                val plan = calculateHeatingPlan(
                    peopleCount = uiState.showersCount,
                    scheduledDate = uiState.selectedDate,
                    scheduledTime = LocalTime.of(uiState.selectedHour, uiState.selectedMinute),
                    config = config.copy(desiredTempCelsius = uiState.desiredTempCelsius),
                    baselines = baselines,
                    weather = weather,
                )

                uiState = uiState.copy(
                    heatingPlan = plan,
                    weatherSummary = "${weather.dayType.name.replace('_', ' ')} • ${weather.cloudCoverPercent}% clouds",
                    isCalculating = false,
                )
            } catch (e: IllegalStateException) {
                val message = e.message
                if (message?.startsWith("Not enough time") == true) {
                    uiState = uiState.copy(
                        heatingPlan = null,
                        isCalculating = false,
                        warning = message,
                        error = null,
                    )
                } else {
                    uiState = uiState.copy(
                        heatingPlan = null,
                        isCalculating = false,
                        error = message,
                    )
                }
            } catch (e: IllegalArgumentException) {
                uiState = uiState.copy(
                    heatingPlan = null,
                    isCalculating = false,
                    error = e.message,
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    heatingPlan = null,
                    isCalculating = false,
                    error = e.message,
                )
            }
        }
    }
}
