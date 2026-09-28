package com.topstep.wearkit.sample.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.topstep.wearkit.sample.db.ListIntConverter

/**
 * Flywear 心电测量报告（FcDataFeature.openEcgMeasure 完成后落库）。
 */
@Entity
@TypeConverters(ListIntConverter::class)
data class EcgReportEntity(
    @PrimaryKey
    val reportId: String,

    /** 测量开始时间戳（毫秒） */
    val timeMs: Long,

    val samplingRate: Int,
    val expectedDurationSec: Int,

    val avgHr: Int,
    val maxHr: Int,
    val minHr: Int,
    val actualDurationSec: Int,
    val qtc: Int?,
    val rmssd: Int?,
    val qrsAmplitudeMv: Float?,
    val qrsDurationMs: Int?,
    val hrZoneNormalPct: Int,
    val hrZoneFastPct: Int,
    val hrZoneSlowPct: Int,

    val samples: List<Int>?,
)
