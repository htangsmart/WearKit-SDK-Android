package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 表盘数据 dial
 *
 */
@Entity
data class SuperDialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val device: String,

    var width: Int,//宽
    val height: Int,//高

    val time: String,//匹配图片，和zip
    val picPath: String,
    val zipPath: String,
)