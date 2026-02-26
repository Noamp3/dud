package com.smartboiler.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.BoilerState
import com.smartboiler.domain.model.BoilerStatus
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.repository.WeatherRepository
import com.smartboiler.domain.usecase.EstimateWaterTemperatureUseCase
import com.smartboiler.domain.usecase.ThermalMath
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.workers.BoilerScheduleWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.min

data class HomeUiState(
    val boilerConfig: BoilerConfig? = null,
    val boilerState: BoilerState = BoilerState(),
    val weatherSummary: String = "Fetching weather…",
    val weatherEmoji: String = "🌤️",
    val isLoading: Boolean = true,
    val asapOneShowerMinutes: Int = 0,
    val isShowerNowLoading: Boolean = false,
    val showerNowMessage: String? = null,
    /** If non-null, a past shower needs feedback. */
    val feedbackScheduleId: Long? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val repository: BoilerRepository,
    private val weatherRepository: WeatherRepository,
    private val estimateTemp: EstimateWaterTemperatureUseCase,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    fun startOnePersonShowerNow() {
        if (uiState.isShowerNowLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isShowerNowLoading = true, showerNowMessage = null)
            try {
                val config = repository.getConfig()
                    ?: throw IllegalStateException("No boiler config found")

                val weather = weatherRepository.getWeatherForecast(
                    config.latitude,
                    config.longitude,
                ).getOrThrow()

                val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                val estimate = estimateTemp(
                    config = config,
                    baselines = repository.getBaselines(),
                    weather = weather,
                    targetHour = now.hour,
                )

                val waterNeeded = config.avgShowerLiters
                val heatedVolume = min(waterNeeded, config.capacityLiters).toDouble()
                val mixFactor = heatedVolume / waterNeeded.toDouble()
                val effectiveCurrentTemp = estimate.temperatureCelsius * mixFactor +
                    estimate.inletTempCelsius * (1.0 - mixFactor)

                val deficit = config.desiredTempCelsius - effectiveCurrentTemp
                val needsHeating = deficit > 0

                val heatingMinutes = if (needsHeating) {
                    ThermalMath.calculateHeatingDurationMinutes(
                        tempDeficitCelsius = deficit,
                        heatedVolumeLiters = heatedVolume,
                        powerKw = config.heatingPowerKw,
                    )
                } else {
                    0
                }

                val readyAt = now.plusMinutes(heatingMinutes.toLong())
                val finalTemp = if (needsHeating) {
                    config.desiredTempCelsius.toDouble()
                } else {
                    effectiveCurrentTemp
                }

                repository.saveSchedule(
                    ShowerScheduleEntity(
                        date = readyAt.toLocalDate().toString(),
                        scheduledTime = readyAt.toLocalTime().toString(),
                        isRecurringDaily = false,
                        recurrenceGroupId = null,
                        dayType = weather.dayType.name,
                        cloudCoverPercent = weather.cloudCoverPercent,
                        peopleCount = 1,
                        heatingRequired = needsHeating,
                        heatingDurationMinutes = heatingMinutes,
                        heatingStartTime = if (needsHeating) now.toLocalTime().toString() else null,
                        estimatedSolarTempCelsius = estimate.temperatureCelsius,
                        estimatedFinalTempCelsius = finalTemp,
                        waterNeededLiters = waterNeeded,
                    )
                )

                if (needsHeating) {
                    BoilerScheduleWorker.schedule(
                        context = application,
                        delayMinutes = 0,
                        durationMinutes = heatingMinutes,
                        estimatedTemp = finalTemp.toInt(),
                        peopleCount = 1,
                    )
                }

                uiState = uiState.copy(
                    isShowerNowLoading = false,
                    asapOneShowerMinutes = heatingMinutes,
                    showerNowMessage = if (needsHeating) {
                        "Heating started. Ready around ${readyAt.toLocalTime()}"
                    } else {
                        "Water is already warm enough for one shower"
                    },
                    boilerState = uiState.boilerState.copy(
                        nextEventDescription = if (needsHeating) {
                            "Shower at ${readyAt.toLocalTime()} (1 person)"
                        } else {
                            "Shower now (1 person)"
                        },
                    ),
                    feedbackScheduleId = null,
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isShowerNowLoading = false,
                    showerNowMessage = e.message ?: "Could not start shower now",
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.observeBoilerConfig().collectLatest { config ->
                uiState = uiState.copy(boilerConfig = config, isLoading = config == null)
                config?.let { fetchWeatherAndEstimate(it) }
            }
        }

        // Load today's scheduled showers
        viewModelScope.launch {
            try {
                repository.syncRecurringSchedules(fromDate = java.time.LocalDate.now(), daysAhead = 30)

                val today = java.time.LocalDate.now().toString()
                val now = java.time.LocalTime.now().toString()
                val schedules = repository.getSchedulesForDate(today)
                val upcoming = schedules
                    .filter { !it.isRecurringTemplate && it.scheduledTime > now }
                    .firstOrNull()
                upcoming?.let {
                    uiState = uiState.copy(
                        boilerState = uiState.boilerState.copy(
                            nextEventDescription = "Shower at ${it.scheduledTime} (${it.peopleCount} people)",
                        )
                    )
                }

                // Check for past showers needing feedback
                val needFeedback = repository.getScheduleIdsNeedingFeedback(today, now)
                if (needFeedback.isNotEmpty()) {
                    uiState = uiState.copy(feedbackScheduleId = needFeedback.first())
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private suspend fun fetchWeatherAndEstimate(config: BoilerConfig) {
        val weatherResult = weatherRepository.getWeatherForecast(
            config.latitude, config.longitude,
        )

        weatherResult.onSuccess { weather ->
            val currentHour = LocalDateTime.now().hour
            val estimate = estimateTemp(
                config = config,
                baselines = repository.getBaselines(),
                weather = weather,
                targetHour = currentHour,
            )

            // Derive emoji and summary
            val emoji = when (weather.dayType) {
                DayType.SUNNY -> "☀️"
                DayType.PARTLY_CLOUDY -> "⛅"
                DayType.CLOUDY -> "☁️"
            }
            val weatherLabel = when (weather.dayType) {
                DayType.SUNNY -> "Sunny"
                DayType.PARTLY_CLOUDY -> "Partly Cloudy"
                DayType.CLOUDY -> "Cloudy"
            }
            val summary = "$weatherLabel, %.0f°C".format(weather.currentTempCelsius)

            // Dashboard shows delivered temperature for one shower (not full-tank average).
            val oneShowerWaterNeeded = config.avgShowerLiters
            val oneShowerMixFactor = min(oneShowerWaterNeeded, config.capacityLiters)
                .toDouble() / oneShowerWaterNeeded.toDouble()
            val oneShowerTemp = estimate.temperatureCelsius * oneShowerMixFactor +
                estimate.inletTempCelsius * (1.0 - oneShowerMixFactor)

            val oneShowerDeficit = config.desiredTempCelsius - oneShowerTemp
            val oneShowerHeatingMinutes = if (oneShowerDeficit > 0) {
                ThermalMath.calculateHeatingDurationMinutes(
                    tempDeficitCelsius = oneShowerDeficit,
                    heatedVolumeLiters = min(config.avgShowerLiters, config.capacityLiters).toDouble(),
                    powerKw = config.heatingPowerKw,
                )
            } else {
                0
            }

            // Estimate solar contribution as a percentage of current one-shower delivered temperature rise.
            val deliveredRise = oneShowerTemp - estimate.inletTempCelsius
            val solarPercent = if (deliveredRise > 0.0) {
                ((estimate.solarGainCelsius / deliveredRise) * 100).toInt().coerceIn(0, 100)
            } else 0

            uiState = uiState.copy(
                isLoading = false,
                asapOneShowerMinutes = oneShowerHeatingMinutes,
                weatherSummary = summary,
                weatherEmoji = emoji,
                boilerState = BoilerState(
                    status = BoilerStatus.OFF,
                    estimatedTempCelsius = oneShowerTemp,
                    solarContributionPercent = solarPercent,
                ),
            )
        }.onFailure { error ->
            uiState = uiState.copy(
                isLoading = false,
                weatherSummary = "Weather unavailable",
                weatherEmoji = "❓",
            )
        }
    }
}
