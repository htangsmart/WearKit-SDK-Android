package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.BloodPressureEntity
import java.util.*

@Dao
interface BloodPressureDao {

    @Query("SELECT * FROM BloodPressureEntity")
    fun queryAll(): List<BloodPressureEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<BloodPressureEntity>)

    @Delete
    fun delete(bloodPressureEntity: BloodPressureEntity)


}