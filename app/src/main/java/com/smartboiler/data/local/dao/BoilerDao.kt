package com.smartboiler.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartboiler.data.local.entity.BoilerConfigEntity
import com.smartboiler.data.local.entity.FeedbackEntity
import com.smartboiler.data.local.entity.HeatingBaselineEntity
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoilerDao {

    // --- Boiler Config ---

    @Query("SELECT * FROM boiler_config LIMIT 1")
    fun observeConfig(): Flow<BoilerConfigEntity?>

    @Query("SELECT * FROM boiler_config LIMIT 1")
    suspend fun getConfig(): BoilerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: BoilerConfigEntity): Long

    @Query("UPDATE boiler_config SET onboardingComplete = :complete WHERE id = :id")
    suspend fun setOnboardingComplete(id: Long, complete: Boolean)

    // --- Heating Baselines ---

    @Query("SELECT * FROM heating_baseline")
    suspend fun getBaselines(): List<HeatingBaselineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaselines(baselines: List<HeatingBaselineEntity>)

    @Query("DELETE FROM heating_baseline")
    suspend fun clearBaselines()

    // --- Shower Schedules ---

    @Query("SELECT * FROM shower_schedule WHERE id = :id")
    suspend fun getScheduleById(id: Long): ShowerScheduleEntity?

    @Query("SELECT * FROM shower_schedule WHERE date = :date AND isRecurringTemplate = 0 ORDER BY scheduledTime ASC")
    suspend fun getSchedulesForDate(date: String): List<ShowerScheduleEntity>

    @Query("SELECT * FROM shower_schedule WHERE date = :date AND isRecurringTemplate = 0 ORDER BY scheduledTime ASC")
    fun observeSchedulesForDate(date: String): Flow<List<ShowerScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ShowerScheduleEntity): Long

    @Query("DELETE FROM shower_schedule WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("""
        SELECT * FROM shower_schedule
        WHERE isRecurringTemplate = 1
        ORDER BY scheduledTime ASC
    """)
    suspend fun getRecurringTemplates(): List<ShowerScheduleEntity>

    @Query("""
        SELECT COUNT(*) FROM shower_schedule
        WHERE date = :date
          AND scheduledTime = :time
          AND recurrenceGroupId = :groupId
          AND isRecurringTemplate = 0
    """)
    suspend fun countGeneratedOccurrence(
        date: String,
        time: String,
        groupId: String,
    ): Int

    @Query("""
        UPDATE shower_schedule
        SET isRecurringEnabled = :enabled
        WHERE recurrenceGroupId = :groupId
    """)
    suspend fun setRecurringGroupEnabled(groupId: String, enabled: Boolean)

    @Query("""
        DELETE FROM shower_schedule
        WHERE recurrenceGroupId = :groupId
          AND isRecurringTemplate = 0
          AND date >= :fromDate
    """)
    suspend fun deleteFutureOccurrencesByGroup(groupId: String, fromDate: String)

    @Query("""
        DELETE FROM shower_schedule
        WHERE recurrenceGroupId = :groupId
    """)
    suspend fun deleteRecurringGroup(groupId: String)

    // --- Feedback ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM feedback WHERE scheduleId = :scheduleId LIMIT 1")
    suspend fun getFeedbackForSchedule(scheduleId: Long): FeedbackEntity?

    @Query("SELECT * FROM feedback ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFeedback(limit: Int = 20): List<FeedbackEntity>

    /** Get schedule IDs from today that don't have feedback yet. */
    @Query("""
        SELECT s.id FROM shower_schedule s 
        LEFT JOIN feedback f ON s.id = f.scheduleId
        WHERE s.date = :date AND s.isRecurringTemplate = 0 AND f.id IS NULL AND s.scheduledTime < :currentTime
        ORDER BY s.scheduledTime ASC
    """)
    suspend fun getScheduleIdsNeedingFeedback(date: String, currentTime: String): List<Long>
}
