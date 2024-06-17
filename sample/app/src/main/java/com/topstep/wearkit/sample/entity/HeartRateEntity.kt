package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,
    val value: Int,
)