package com.smartboiler.data.repository

import com.smartboiler.data.local.dao.BoilerDao
import com.smartboiler.data.local.entity.FeedbackEntity
import com.smartboiler.data.local.entity.ShowerScheduleEntity
import com.smartboiler.data.local.mapper.toDomain
import com.smartboiler.data.local.mapper.toEntity
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.model.RecurringScheduleConfig
import com.smartboiler.domain.repository.BoilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first implementation of [BoilerRepository] backed by Room.
 */
@Singleton
class LocalBoilerRepository @Inject constructor(
    private val boilerDao: BoilerDao,
) : BoilerRepository {

    override fun observeBoilerConfig(): Flow<BoilerConfig?> =
        boilerDao.observeConfig().map { it?.toDomain() }

    override suspend fun getBoilerConfig(): BoilerConfig? =
        boilerDao.getConfig()?.toDomain()

    override suspend fun saveBoilerConfig(config: BoilerConfig) {
        boilerDao.insertConfig(config.toEntity())
    }

    override suspend fun getBaselines(): List<HeatingBaseline> =
        boilerDao.getBaselines().map { it.toDomain() }

    override suspend fun saveBaselines(baselines: List<HeatingBaseline>) {
        boilerDao.clearBaselines()
        boilerDao.insertBaselines(baselines.map { it.toEntity() })
    }

    override suspend fun isOnboardingComplete(): Boolean =
        boilerDao.getConfig()?.onboardingComplete ?: false

    override suspend fun setOnboardingComplete(complete: Boolean) {
        val config = boilerDao.getConfig() ?: return
        boilerDao.setOnboardingComplete(config.id, complete)
    }

    override suspend fun saveSchedule(schedule: ShowerScheduleEntity) {
        boilerDao.insertSchedule(schedule)
    }

    override suspend fun getSchedulesForDate(date: String): List<ShowerScheduleEntity> =
        boilerDao.getSchedulesForDate(date)

    override suspend fun getScheduleById(id: Long): ShowerScheduleEntity? =
        boilerDao.getScheduleById(id)

    override suspend fun saveFeedback(feedback: FeedbackEntity) {
        boilerDao.insertFeedback(feedback)
    }

    override suspend fun getScheduleIdsNeedingFeedback(date: String, currentTime: String): List<Long> =
        boilerDao.getScheduleIdsNeedingFeedback(date, currentTime)

    override suspend fun getRecurringSchedules(): List<RecurringScheduleConfig> =
        boilerDao.getRecurringTemplates()
            .mapNotNull { template ->
                val groupId = template.recurrenceGroupId ?: return@mapNotNull null
                val days = template.recurrenceDaysCsv
                    ?.split(',')
                    ?.mapNotNull { raw ->
                        runCatching { DayOfWeek.valueOf(raw.trim()) }.getOrNull()
                    }
                    ?.toSet()
                    ?: emptySet()

                RecurringScheduleConfig(
                    recurrenceGroupId = groupId,
                    startDate = template.date,
                    scheduledTime = runCatching { LocalTime.parse(template.scheduledTime) }
                        .getOrDefault(LocalTime.of(18, 0)),
                    peopleCount = template.peopleCount,
                    daysOfWeek = days,
                    enabled = template.isRecurringEnabled,
                )
            }

    override suspend fun setRecurringScheduleEnabled(groupId: String, enabled: Boolean) {
        boilerDao.setRecurringGroupEnabled(groupId, enabled)
        if (!enabled) {
            boilerDao.deleteFutureOccurrencesByGroup(groupId, LocalDate.now().toString())
        }
    }

    override suspend fun deleteRecurringSchedule(groupId: String) {
        boilerDao.deleteRecurringGroup(groupId)
    }

    override suspend fun syncRecurringSchedules(fromDate: LocalDate, daysAhead: Int) {
        val templates = boilerDao.getRecurringTemplates()
            .filter { it.isRecurringEnabled }

        templates.forEach { template ->
            val groupId = template.recurrenceGroupId ?: return@forEach
            val days = template.recurrenceDaysCsv
                ?.split(',')
                ?.mapNotNull { raw ->
                    runCatching { DayOfWeek.valueOf(raw.trim()) }.getOrNull()
                }
                ?.toSet()
                .orEmpty()

            if (days.isEmpty()) return@forEach

            val endDate = fromDate.plusDays(daysAhead.toLong())
            var cursor = fromDate
            while (!cursor.isAfter(endDate)) {
                if (cursor.dayOfWeek in days) {
                    val dateStr = cursor.toString()
                    val exists = boilerDao.countGeneratedOccurrence(
                        date = dateStr,
                        time = template.scheduledTime,
                        groupId = groupId,
                    ) > 0

                    if (!exists) {
                        boilerDao.insertSchedule(
                            template.copy(
                                id = 0,
                                date = dateStr,
                                isRecurringTemplate = false,
                            )
                        )
                    }
                }
                cursor = cursor.plusDays(1)
            }
        }
    }
}
