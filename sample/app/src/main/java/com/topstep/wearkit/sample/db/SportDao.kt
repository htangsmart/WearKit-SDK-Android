package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.SportEntity
import java.util.*

@Dao
interface SportDao {

    @Query("SELECT * FROM SportEntity")
    fun queryAll(): List<SportEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<SportEntity>)
}