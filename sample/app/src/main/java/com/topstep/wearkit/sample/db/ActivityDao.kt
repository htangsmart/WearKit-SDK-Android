package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.ActivityEntity
import java.util.*

@Dao
interface ActivityDao {

    @Query("SELECT * FROM ActivityEntity")
    fun queryAll(): List<ActivityEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<ActivityEntity>)

    @Query("DELETE FROM ActivityEntity")
    fun delete()
}