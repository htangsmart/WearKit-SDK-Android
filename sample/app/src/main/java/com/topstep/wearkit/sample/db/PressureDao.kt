package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.PressureEntity
import java.util.*

@Dao
interface PressureDao {

    @Query("SELECT * FROM PressureEntity")
    fun queryAll(): List<PressureEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<PressureEntity>)
}