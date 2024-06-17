package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.SleepEntity
import java.util.*

@Dao
interface SleepDao {

    @Query("SELECT * FROM SleepEntity")
    fun queryAll(): List<SleepEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<SleepEntity>)
}