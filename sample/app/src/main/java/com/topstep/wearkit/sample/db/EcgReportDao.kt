package com.topstep.wearkit.sample.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.topstep.wearkit.sample.entity.EcgReportEntity

@Dao
interface EcgReportDao {

    @Query("SELECT * FROM EcgReportEntity ORDER BY timeMs DESC")
    fun queryAll(): List<EcgReportEntity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: EcgReportEntity)

    @Query("DELETE FROM EcgReportEntity WHERE reportId=:reportId")
    fun delete(reportId: String)
}
