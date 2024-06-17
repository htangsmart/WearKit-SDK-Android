package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.HeartRateEntity
import java.util.*

@Dao
interface HeartRateDao {

    @Query("SELECT * FROM HeartRateEntity")
    fun queryAll(): List<HeartRateEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<HeartRateEntity>)

    @Delete
    fun delete(heartRateEntity: HeartRateEntity)


}