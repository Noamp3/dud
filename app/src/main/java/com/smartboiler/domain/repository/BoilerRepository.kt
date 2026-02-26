package com.smartboiler.domain.repository

import com.smartboiler.data.local.entity.FeedbackEntity
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.model.RecurringScheduleConfig
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for boiler configuration and baselines.
 *
 * All data access goes through this interface. Currently backed by Room (local),
 * but designed so a remote implementation can replace or wrap it in the future.
 */
interface BoilerRepository {

    /** Observe the current boiler configuration. Emits null if onboarding not complete. */
    fun observeBoilerConfig(): Flow<BoilerConfig?>

    /** Get boiler config (one-shot). */
    suspend fun getBoilerConfig(): BoilerConfig?

    /** Save or update the boiler configuration. */
    suspend fun saveBoilerConfig(config: BoilerConfig)

    /** Get all heating baselines. */
    suspend fun getBaselines(): List<HeatingBaseline>

    /** Save heating baselines (replaces existing). */
    suspend fun saveBaselines(baselines: List<HeatingBaseline>)

    /** Check whether onboarding has been completed. */
    suspend fun isOnboardingComplete(): Boolean

    /** Mark onboarding as complete. */
    suspend fun setOnboardingComplete(complete: Boolean)

    /** Convenience alias used by ViewModels — same as getBoilerConfig(). */
    suspend fun getConfig(): BoilerConfig? = getBoilerConfig()

    /** Save a shower schedule. */
    suspend fun saveSchedule(schedule: ShowerScheduleEntity)

    /** Get all schedules for a given date. */
    suspend fun getSchedulesForDate(date: String): List<ShowerScheduleEntity>

    /** Get a specific schedule by ID. */
    suspend fun getScheduleById(id: Long): ShowerScheduleEntity?

    /** Save post-shower feedback. */
    suspend fun saveFeedback(feedback: FeedbackEntity)

    /** Get schedule IDs that need feedback (past showers with no feedback today). */
    suspend fun getScheduleIdsNeedingFeedback(date: String, currentTime: String): List<Long>

    /** List all recurring schedules (template definitions). */
    suspend fun getRecurringSchedules(): List<RecurringScheduleConfig>

    /** Enable or disable a recurring schedule group. */
    suspend fun setRecurringScheduleEnabled(groupId: String, enabled: Boolean)

    /** Delete a recurring schedule group (template + generated occurrences). */
    suspend fun deleteRecurringSchedule(groupId: String)

    /** Ensure recurring templates are materialized as dated schedules in a rolling window. */
    suspend fun syncRecurringSchedules(fromDate: LocalDate, daysAhead: Int = 30)
}
