package com.smartboiler.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.repository.WeatherRepository
import com.smartboiler.domain.device.SmartSwitchController
import com.smartboiler.domain.usecase.EstimateWaterTemperatureUseCase
import com.smartboiler.domain.usecase.ThermalMath
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * WorkManager worker to turn the boiler on or off at scheduled times.
 *
 * Enqueued when the user confirms a shower schedule that requires electric heating.
 */
@HiltWorker
class BoilerScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val controller: SmartSwitchController,
    private val boilerRepository: BoilerRepository,
    private val weatherRepository: WeatherRepository,
    private val estimateTemp: EstimateWaterTemperatureUseCase,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BoilerWorker"
        const val KEY_ACTION = "action"
        const val ACTION_TURN_ON = "turn_on"
        const val ACTION_TURN_OFF = "turn_off"
        const val KEY_DURATION_MINUTES = "duration_minutes"
        const val KEY_ESTIMATED_TEMP = "estimated_temp"
        const val KEY_PEOPLE_COUNT = "people_count"

        /**
         * Schedule the boiler to turn on after [delayMinutes] and off after
         * [delayMinutes] + [durationMinutes].
         */
        fun schedule(
            context: Context,
            delayMinutes: Long,
            durationMinutes: Int,
            estimatedTemp: Int,
            peopleCount: Int,
        ) {
            val workManager = WorkManager.getInstance(context)

            // Schedule turn ON
            val turnOnRequest = OneTimeWorkRequestBuilder<BoilerScheduleWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_TURN_ON)
                        .putInt(KEY_DURATION_MINUTES, durationMinutes)
                        .putInt(KEY_ESTIMATED_TEMP, estimatedTemp)
                        .putInt(KEY_PEOPLE_COUNT, peopleCount)
                        .build()
                )
                .addTag("boiler_on")
                .build()

            workManager.enqueue(turnOnRequest)
            Log.i(TAG, "Scheduled: ON in ${delayMinutes}min (duration will be recalculated at start)")
        }

        private fun scheduleTurnOff(
            context: Context,
            delayMinutes: Int,
            estimatedTemp: Int,
        ) {
            val turnOffRequest = OneTimeWorkRequestBuilder<BoilerScheduleWorker>()
                .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_TURN_OFF)
                        .putInt(KEY_ESTIMATED_TEMP, estimatedTemp)
                        .build()
                )
                .addTag("boiler_off")
                .build()
            WorkManager.getInstance(context).enqueue(turnOffRequest)
        }

        /** Cancel all pending boiler work. */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("boiler_on")
            WorkManager.getInstance(context).cancelAllWorkByTag("boiler_off")
        }
    }

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val temp = inputData.getInt(KEY_ESTIMATED_TEMP, 40)

        NotificationHelper.createChannel(applicationContext)

        return when (action) {
            ACTION_TURN_ON -> {
                val plannedDuration = inputData.getInt(KEY_DURATION_MINUTES, 0)
                val peopleCount = inputData.getInt(KEY_PEOPLE_COUNT, 1).coerceAtLeast(1)
                val actualDuration = calculateUpdatedDuration(
                    plannedDuration = plannedDuration,
                    peopleCount = peopleCount,
                )

                if (actualDuration <= 0) {
                    Log.i(TAG, "Executing: TURN ON skipped (updated duration is 0min)")
                    NotificationHelper.notifyNoHeatingNeeded(applicationContext)
                    return Result.success()
                }

                Log.i(TAG, "Executing: TURN ON (updated duration: ${actualDuration}min)")
                val result = controller.turnOn()
                if (result.isSuccess) {
                    scheduleTurnOff(
                        context = applicationContext,
                        delayMinutes = actualDuration,
                        estimatedTemp = temp,
                    )
                    NotificationHelper.notifyBoilerStarted(applicationContext, actualDuration)
                    Result.success()
                } else {
                    Log.e(TAG, "Failed to turn on: ${result.exceptionOrNull()}")
                    Result.retry()
                }
            }
            ACTION_TURN_OFF -> {
                Log.i(TAG, "Executing: TURN OFF")
                val result = controller.turnOff()
                if (result.isSuccess) {
                    NotificationHelper.notifyBoilerReady(applicationContext, temp)
                    Result.success()
                } else {
                    Log.e(TAG, "Failed to turn off: ${result.exceptionOrNull()}")
                    Result.retry()
                }
            }
            else -> Result.failure()
        }
    }

    private suspend fun calculateUpdatedDuration(
        plannedDuration: Int,
        peopleCount: Int,
    ): Int {
        return runCatching {
            val config = boilerRepository.getConfig() ?: return plannedDuration
            val weather = weatherRepository
                .getWeatherForecast(config.latitude, config.longitude)
                .getOrElse { return plannedDuration }

            val estimate = estimateTemp(
                config = config,
                baselines = boilerRepository.getBaselines(),
                weather = weather,
                targetHour = LocalDateTime.now().hour,
            )

            val waterNeeded = peopleCount * config.avgShowerLiters
            val requiredHeatedVolume = min(waterNeeded, config.capacityLiters).toDouble()
            val mixFactor = requiredHeatedVolume / waterNeeded.toDouble()
            val effectiveCurrentTemp = estimate.temperatureCelsius * mixFactor +
                estimate.inletTempCelsius * (1.0 - mixFactor)

            val deficit = config.desiredTempCelsius - effectiveCurrentTemp
            if (deficit <= 0.0) return 0
            ThermalMath.calculateHeatingDurationMinutes(
                tempDeficitCelsius = deficit,
                heatedVolumeLiters = requiredHeatedVolume,
                powerKw = config.heatingPowerKw,
            )
        }.getOrDefault(plannedDuration)
    }
}
