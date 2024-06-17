package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,

    /**
     * 步数 step
     */
    val steps: Int,

    /**
     * 距离，单位米  distance
     */
    val distance: Float,

    /**
     * 活动热量，单位千卡  calories
     */
    val calories: Float,

    /**
     * 活动次数 activity number
     */
    val number: Int,

    /**
     * 活动时长，单位分钟 activity duration
     */
    val duration: Int,

    /**
     * 锻炼时长，单位分钟 sport Duration
     */
    val sportDuration: Int,
)