package com.smartboiler.ui.schedule

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.domain.model.ShowerSchedule
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.repository.WeatherRepository
import com.smartboiler.domain.usecase.CalculateHeatingPlanUseCase
import com.smartboiler.workers.BoilerScheduleWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class ScheduleUiState(
    val peopleCount: Int = 2,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedHour: Int = 18,
    val selectedMinute: Int = 0,
    val isRecurring: Boolean = false,
    val recurringDays: Set<DayOfWeek> = setOf(LocalDate.now().dayOfWeek),
    val heatingPlan: ShowerSchedule? = null,
    val isCalculating: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val warning: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val application: Application,
    private val boilerRepository: BoilerRepository,
    private val weatherRepository: WeatherRepository,
    private val calculateHeatingPlan: CalculateHeatingPlanUseCase,
) : ViewModel() {

    companion object {
        private const val MAX_ESTIMATION_DAYS_AHEAD = 10L
    }

    var uiState by mutableStateOf(ScheduleUiState())
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

        // Load default people count from config
        viewModelScope.launch {
            boilerRepository.getConfig()?.let { config ->
                uiState = uiState.copy(peopleCount = config.defaultPeopleCount)
            }
        }
        // Calculate initial plan
        recalculate()
    }

    fun updatePeopleCount(count: Int) {
        uiState = uiState.copy(peopleCount = count.coerceIn(1, 8))
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

    fun applyPrefill(
        peopleCount: Int,
        date: LocalDate,
        hour: Int,
        minute: Int,
    ) {
        uiState = uiState.copy(
            peopleCount = peopleCount.coerceIn(1, 8),
            selectedDate = date,
            selectedHour = hour.coerceIn(0, 23),
            selectedMinute = minute.coerceIn(0, 59),
        )
        recalculate()
    }

    fun updateRecurring(enabled: Boolean) {
        uiState = uiState.copy(isRecurring = enabled)
    }

    fun updateRecurringDay(day: DayOfWeek, enabled: Boolean) {
        val updated = uiState.recurringDays.toMutableSet().apply {
            if (enabled) add(day) else remove(day)
        }
        uiState = uiState.copy(recurringDays = updated)
    }

    fun confirmSchedule() {
        val plan = uiState.heatingPlan ?: return
        if (uiState.warning != null) return
        uiState = uiState.copy(isSaving = true)

        viewModelScope.launch {
            try {
                if (uiState.isRecurring && uiState.recurringDays.isEmpty()) {
                    throw IllegalArgumentException("Select at least one weekday for recurring schedule")
                }

                if (uiState.isRecurring) {
                    val recurrenceId = UUID.randomUUID().toString()
                    boilerRepository.saveSchedule(
                        ShowerScheduleEntity(
                            date = plan.date.toString(),
                            scheduledTime = plan.scheduledTime.toString(),
                            isRecurringDaily = true,
                            recurrenceGroupId = recurrenceId,
                            recurrenceDaysCsv = uiState.recurringDays
                                .sortedBy(DayOfWeek::getValue)
                                .joinToString(",") { it.name },
                            isRecurringTemplate = true,
                            isRecurringEnabled = true,
                            dayType = plan.dayType.name,
                            cloudCoverPercent = plan.cloudCoverPercent,
                            peopleCount = plan.peopleCount,
                            heatingRequired = plan.heatingRequired,
                            heatingDurationMinutes = plan.heatingDurationMinutes,
                            heatingStartTime = plan.heatingStartTime?.toString(),
                            estimatedSolarTempCelsius = plan.estimatedSolarTempCelsius,
                            estimatedFinalTempCelsius = plan.estimatedFinalTempCelsius,
                            waterNeededLiters = plan.waterNeededLiters,
                        )
                    )
                    boilerRepository.syncRecurringSchedules(fromDate = LocalDate.now(), daysAhead = 30)
                } else {
                    boilerRepository.saveSchedule(
                        ShowerScheduleEntity(
                            date = plan.date.toString(),
                            scheduledTime = plan.scheduledTime.toString(),
                            isRecurringDaily = false,
                            recurrenceGroupId = null,
                            recurrenceDaysCsv = null,
                            isRecurringTemplate = false,
                            isRecurringEnabled = true,
                            dayType = plan.dayType.name,
                            cloudCoverPercent = plan.cloudCoverPercent,
                            peopleCount = plan.peopleCount,
                            heatingRequired = plan.heatingRequired,
                            heatingDurationMinutes = plan.heatingDurationMinutes,
                            heatingStartTime = plan.heatingStartTime?.toString(),
                            estimatedSolarTempCelsius = plan.estimatedSolarTempCelsius,
                            estimatedFinalTempCelsius = plan.estimatedFinalTempCelsius,
                            waterNeededLiters = plan.waterNeededLiters,
                        )
                    )
                }

                if (plan.heatingRequired && plan.heatingStartTime != null) {
                    val now = LocalDateTime.now()
                    val startDateTime = LocalDateTime.of(plan.date, plan.heatingStartTime)
                    val delayMinutes = Duration.between(now, startDateTime).toMinutes()
                        .coerceAtLeast(0)

                    BoilerScheduleWorker.schedule(
                        context = application,
                        delayMinutes = delayMinutes,
                        durationMinutes = plan.heatingDurationMinutes,
                        estimatedTemp = plan.estimatedFinalTempCelsius.toInt(),
                        peopleCount = plan.peopleCount,
                    )
                }

                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                uiState = uiState.copy(isSaving = false, error = e.message)
            }
        }
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
                        warning = "Estimation is available up to 10 days ahead",
                        error = null,
                    )
                    return@launch
                }

                val config = boilerRepository.getConfig()
                    ?: throw IllegalStateException("No boiler config found")
                val baselines = boilerRepository.getBaselines()
                val weatherResult = weatherRepository.getWeatherForecast(
                    config.latitude,
                    config.longitude,
                    uiState.selectedDate,
                )
                val weather = weatherResult.getOrThrow()

                val plan = calculateHeatingPlan(
                    peopleCount = uiState.peopleCount,
                    scheduledDate = uiState.selectedDate,
                    scheduledTime = LocalTime.of(uiState.selectedHour, uiState.selectedMinute),
                    config = config,
                    baselines = baselines,
                    weather = weather,
                )

                uiState = uiState.copy(heatingPlan = plan, isCalculating = false, warning = null)
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
                        warning = null,
                    )
                }
            } catch (e: IllegalArgumentException) {
                uiState = uiState.copy(
                    heatingPlan = null,
                    isCalculating = false,
                    error = e.message,
                    warning = null,
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    heatingPlan = null,
                    isCalculating = false,
                    error = e.message,
                    warning = null,
                )
            }
        }
    }
}
