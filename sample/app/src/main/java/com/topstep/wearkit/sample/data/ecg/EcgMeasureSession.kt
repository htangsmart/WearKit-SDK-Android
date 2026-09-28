package com.topstep.wearkit.sample.data.ecg

import com.topstep.fitcloud.sdk.v2.model.data.FcEcgMeasureEvent
import com.topstep.fitcloud.sdk.v2.model.data.FcEcgMeasureSummary
import com.topstep.wearkit.sample.entity.EcgReportEntity
import java.util.UUID

/**
 * 心电测量会话内存累加器：收集波形 / 导联 / 指标时间线，完成后生成 [EcgReportEntity]。
 */
class EcgMeasureSession(
    val reportId: String = UUID.randomUUID().toString(),
    val startTimeMs: Long = System.currentTimeMillis(),
) {
    var samplingRate: Int = 0
        private set
    var expectedDurationSec: Int = 0
        private set

    private val samples = ArrayList<Int>(8192)
    private val leadTimeline = ArrayList<LeadPoint>()
    private val metricsTimeline = ArrayList<MetricsPoint>()
    private val progressTimeline = ArrayList<Int>()

    var summary: FcEcgMeasureSummary? = null
        private set
    var failedReasonCode: Int? = null
        private set

    fun onEvent(event: FcEcgMeasureEvent) {
        when (event) {
            is FcEcgMeasureEvent.Started -> {
                samplingRate = event.samplingRate
                expectedDurationSec = event.expectedDurationSec
            }
            is FcEcgMeasureEvent.Waveform -> {
                val incoming = event.samples
                samples.ensureCapacity(samples.size + incoming.size)
                for (i in incoming.indices) {
                    samples.add(incoming[i])
                }
            }
            is FcEcgMeasureEvent.LeadContact -> {
                leadTimeline.add(LeadPoint(event.sampleIndex, event.contacted))
            }
            is FcEcgMeasureEvent.Metrics -> {
                metricsTimeline.add(
                    MetricsPoint(
                        sampleIndex = event.sampleIndex,
                        heartRate = event.heartRate,
                        qtc = event.qtc,
                        rmssd = event.rmssd,
                    )
                )
            }
            is FcEcgMeasureEvent.Progress -> {
                progressTimeline.add(event.percent)
            }
            is FcEcgMeasureEvent.Completed -> {
                summary = event.summary
            }
            is FcEcgMeasureEvent.Failed -> {
                failedReasonCode = event.reasonCode
            }
        }
    }

    fun getSamples(): List<Int> = samples.toList()

    fun getLeadTimeline(): List<LeadPoint> = leadTimeline.toList()

    fun getMetricsTimeline(): List<MetricsPoint> = metricsTimeline.toList()

    /** 仅在 Completed 后可生成报告实体。 */
    fun toReportEntityOrNull(): EcgReportEntity? {
        val s = summary ?: return null
        return EcgReportEntity(
            reportId = reportId,
            timeMs = startTimeMs,
            samplingRate = samplingRate,
            expectedDurationSec = expectedDurationSec,
            avgHr = s.avgHr,
            maxHr = s.maxHr,
            minHr = s.minHr,
            actualDurationSec = s.actualDurationSec,
            qtc = s.qtc,
            rmssd = s.rmssd,
            qrsAmplitudeMv = s.qrsAmplitudeMv,
            qrsDurationMs = s.qrsDurationMs,
            hrZoneNormalPct = s.hrZoneNormalPct,
            hrZoneFastPct = s.hrZoneFastPct,
            hrZoneSlowPct = s.hrZoneSlowPct,
            samples = samples.toList(),
        )
    }

    data class LeadPoint(val sampleIndex: Int, val contacted: Boolean)

    data class MetricsPoint(
        val sampleIndex: Int,
        val heartRate: Int?,
        val qtc: Int?,
        val rmssd: Int?,
    )
}
