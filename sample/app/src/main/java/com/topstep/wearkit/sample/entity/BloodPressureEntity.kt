package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BloodPressureEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,
    val sbp: Int,
    val dbp: Int,
)