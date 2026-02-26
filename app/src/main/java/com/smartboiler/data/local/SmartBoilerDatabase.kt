package com.smartboiler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartboiler.data.local.dao.BoilerDao
import com.smartboiler.data.local.entity.BoilerConfigEntity
import com.smartboiler.data.local.entity.FeedbackEntity
import com.smartboiler.data.local.entity.HeatingBaselineEntity
import com.smartboiler.data.local.entity.ShowerScheduleEntity

@Database(
    entities = [
        BoilerConfigEntity::class,
        HeatingBaselineEntity::class,
        ShowerScheduleEntity::class,
        FeedbackEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class SmartBoilerDatabase : RoomDatabase() {
    abstract fun boilerDao(): BoilerDao
}
