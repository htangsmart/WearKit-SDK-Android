package com.topstep.wearkit.sample.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.topstep.wearkit.sample.entity.*

@Database(
    version = 7,
    entities = [
        SuperDialEntity::class,
        HeartRateEntity::class,
        ActivityEntity::class,
        BloodOxygenEntity::class,
        PressureEntity::class,
        SportEntity::class,
        SleepEntity::class,
        BloodPressureEntity::class,
        EcgReportEntity::class,
    ],
)
@TypeConverters(ListIntConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun superDialDao(): SuperDialDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun activityDao(): ActivityDao
    abstract fun bloodOxygenDao(): BloodOxygenDao
    abstract fun pressureDao(): PressureDao
    abstract fun sportDao(): SportDao
    abstract fun sleepDao(): SleepDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun ecgReportDao(): EcgReportDao

    companion object {
        private const val DB_NAME = "db_sample"

        fun getInstance(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
