package com.topstep.wearkit.sample.db

import androidx.room.*
import com.topstep.wearkit.sample.entity.SuperDialEntity
import java.util.*

@Dao
interface SuperDialDao {

    @Query("SELECT * FROM SuperDialEntity WHERE device=:device")
     fun queryAllUsers(device: String): List<SuperDialEntity>?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(vararg superDialEntity: SuperDialEntity)


    @Delete
     fun delete(superDialEntity: SuperDialEntity)


}