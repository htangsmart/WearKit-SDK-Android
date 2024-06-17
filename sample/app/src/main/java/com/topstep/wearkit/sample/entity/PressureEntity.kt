package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PressureEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,
    val pressure: Int,

)