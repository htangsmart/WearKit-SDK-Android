package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SleepEntity(
    @PrimaryKey(autoGenerate = true)
    val timestamp: Long,
    /**
     * 睡眠总时长(秒) total sleep duration
     */
    val duration: Int = 0,

    /**
     * 深睡总时长(秒) Total duration of deep sleep
     */
    val deep: Int = 0,

    /**
     * 浅睡总时长(秒) Total duration of light sleep
     */
    val light: Int = 0,

    /**
     * 清醒总时长(秒) Total duration of wakefulness
     */
    val awake: Int = 0,

    /**
     * rem总时长(秒) Rem total duration
     */
    val rem: Int = 0,

    /**
     * 零星小睡 Sporadic naps
     */

    val nap: Int = 0,
)