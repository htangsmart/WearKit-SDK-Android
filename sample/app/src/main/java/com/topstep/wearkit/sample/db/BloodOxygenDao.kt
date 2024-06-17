package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.BloodOxygenEntity
import java.util.*

@Dao
interface BloodOxygenDao {

    @Query("SELECT * FROM BloodOxygenEntity")
    fun queryAll(): List<BloodOxygenEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<BloodOxygenEntity>)

    @Delete
    fun delete(bloodOxygenEntity: BloodOxygenEntity)


}