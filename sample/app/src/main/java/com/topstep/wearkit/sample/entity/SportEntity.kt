package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SportEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,

    val sportType: Int,

    val sportName: String,

    /**
     * 结束时间戳，秒。 End timestamp
     */
    val endTimestampSeconds: Long,

    /**
     * 持续时间，单位秒 Duration in seconds
     */
    val duration: Int,

    /**
     * 距离，单位米 Distance in meters
     */
    val distance: Double,

    /**
     * 卡路里，单位千卡 Calories, in kilocalories
     */
    val calories: Double,

    /**
     * 步数 Steps
     */
    val steps: Int,

    /**
     * 热身时间，单位秒
     */
    val warmUpDuration: Int,

    /**
     * 燃脂时间，单位秒 Warming up time in seconds
     */
    val fatBurningDuration: Int,

    /**
     * 有氧耐力时间，单位秒 Aerobic endurance time, in seconds
     */
    val aerobicDuration: Int,

    /**
     * 无氧耐力时间，单位秒 Anaerobic endurance time, in seconds
     */
    val anaerobicDuration: Int,

    /**
     * 极限运动时间，单位秒 Extreme exercise time in seconds
     */
    val heartLimitDuration: Int,

    /**
     * 平均心率 Average heart rate
     */
    val avgHeartRate: Int,

    /**
     * 最大心率 Maximum heart rate
     */
    val maxHeartRate: Int,

    /**
     * 最小心率 Minimum heart rate
     */
    val minHeartRate: Int,

    )