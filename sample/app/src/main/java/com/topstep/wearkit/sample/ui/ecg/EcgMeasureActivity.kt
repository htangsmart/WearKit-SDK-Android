package com.topstep.wearkit.sample.ui.ecg

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.fitcloud.sdk.exception.FcEcgStartFailedException
import com.topstep.fitcloud.sdk.exception.FcSyncBusyException
import com.topstep.fitcloud.sdk.exception.FcUnSupportFeatureException
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.config.FcDeviceInfo
import com.topstep.fitcloud.sdk.v2.model.data.FcEcgMeasureEvent
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.ecg.EcgMeasureSession
import com.topstep.wearkit.sample.databinding.ActivityEcgMeasureBinding
import com.topstep.wearkit.sample.db.AppDatabase
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeoutException

/**
 * 心电测量页（对齐 Flywear 交互原型：指标 / 波形 / 导联提示 / 进度 / 完成与失败）。
 */
@SuppressLint("CheckResult")
class EcgMeasureActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityEcgMeasureBinding
    private lateinit var appDatabase: AppDatabase

    private var measureDisposable: Disposable? = null
    private var session: EcgMeasureSession? = null
    private var lastReportId: String? = null

    private var displayHr: Int? = null
    private var displayQtc: Int? = null
    private var displayRmssd: Int? = null
    private var leadContacted = false
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityEcgMeasureBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.hide()
        appDatabase = AppDatabase.getInstance(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewBind.btnClose.setOnClickListener { askExit() }
        viewBind.btnRetry.setOnClickListener { startMeasure() }
        viewBind.btnViewReport.setOnClickListener {
            toast(getString(R.string.ecg_measure_report_saved, lastReportId.orEmpty()))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                askExit()
            }
        })

        startMeasure()
    }

    private fun startMeasure() {
        val fc = (wearKit.getRawSDK() as? FcSDK)
        if (fc == null) {
            toast(R.string.ecg_measure_need_fc)
            finish()
            return
        }
        if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
            enterStartFailed(getString(R.string.ecg_measure_not_connected))
            return
        }
        if (!fc.connector.configFeature().getDeviceInfo().isSupportFeature(FcDeviceInfo.Feature.ECG_MEASURE)) {
            enterStartFailed(getString(R.string.ecg_measure_unsupported))
            return
        }

        measureDisposable?.dispose()
        finished = false
        leadContacted = false
        displayHr = null
        displayQtc = null
        displayRmssd = null
        session = EcgMeasureSession()
        lastReportId = null

        resetUiForMeasuring()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        measureDisposable = fc.connector.dataFeature().openEcgMeasure()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event: FcEcgMeasureEvent ->
                handleEvent(event)
            }, { error: Throwable ->
                Timber.w(error)
                val message = when (error) {
                    is FcEcgStartFailedException -> getString(R.string.ecg_measure_start_failed)
                    is FcSyncBusyException -> getString(R.string.ecg_measure_busy)
                    is FcUnSupportFeatureException -> getString(R.string.ecg_measure_unsupported)
                    is TimeoutException -> getString(R.string.ecg_measure_timeout)
                    else -> error.message ?: getString(R.string.ecg_measure_start_failed)
                }
                enterStartFailed(message)
            }, {
                // onComplete after Completed/Failed
            })
    }

    private fun resetUiForMeasuring() {
        viewBind.ecgWaveform.resetLive()
        viewBind.ecgWaveform.setFlatLine(false)
        viewBind.progressMeasure.visibility = View.VISIBLE
        viewBind.progressMeasure.progress = 0
        viewBind.btnRetry.visibility = View.GONE
        viewBind.btnViewReport.visibility = View.INVISIBLE
        viewBind.tvDuration.visibility = View.VISIBLE
        viewBind.tvLeadHint.visibility = View.GONE
        viewBind.layoutKpi.alpha = 1f
        bindMetric(viewBind.tvHr, null)
        bindMetric(viewBind.tvQtc, null)
        bindMetric(viewBind.tvRmssd, null)
        viewBind.tvDuration.text = getString(R.string.ecg_measure_duration, 0)
        viewBind.tvSpec.text = getString(R.string.ecg_measure_spec, 250)
    }

    private fun handleEvent(event: FcEcgMeasureEvent) {
        session?.onEvent(event)
        when (event) {
            is FcEcgMeasureEvent.Started -> {
                viewBind.ecgWaveform.setSamplingRate(event.samplingRate)
                viewBind.tvDuration.text = getString(R.string.ecg_measure_duration, event.expectedDurationSec)
                viewBind.tvSpec.text = getString(R.string.ecg_measure_spec, event.samplingRate)
            }
            is FcEcgMeasureEvent.Waveform -> {
                if (!finished) {
                    viewBind.ecgWaveform.setFlatLine(!leadContacted)
                    viewBind.ecgWaveform.appendSamples(event.samples)
                }
            }
            is FcEcgMeasureEvent.LeadContact -> {
                leadContacted = event.contacted
                viewBind.ecgWaveform.setFlatLine(!event.contacted)
                viewBind.tvLeadHint.visibility = if (event.contacted) View.GONE else View.VISIBLE
                viewBind.layoutKpi.alpha = if (event.contacted) 1f else 0.34f
            }
            is FcEcgMeasureEvent.Metrics -> {
                event.heartRate?.let { hr ->
                    displayHr = hr.takeIf { it > 0 }
                    bindMetric(viewBind.tvHr, displayHr)
                }
                event.qtc?.let { qtc ->
                    displayQtc = qtc.takeIf { it > 0 }
                    bindMetric(viewBind.tvQtc, displayQtc)
                }
                event.rmssd?.let { rmssd ->
                    displayRmssd = rmssd.takeIf { it > 0 }
                    bindMetric(viewBind.tvRmssd, displayRmssd)
                }
            }
            is FcEcgMeasureEvent.Progress -> {
                viewBind.progressMeasure.progress = event.percent.coerceIn(0, 100)
            }
            is FcEcgMeasureEvent.Completed -> {
                finished = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                viewBind.progressMeasure.progress = 100
                enterDoneState()
                saveReport()
                showCompletedDialog()
            }
            is FcEcgMeasureEvent.Failed -> {
                finished = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                showFailedDialog()
            }
        }
    }

    private fun bindMetric(tv: android.widget.TextView, value: Int?) {
        tv.text = if (value == null || value <= 0) {
            getString(R.string.ecg_placeholder)
        } else {
            value.toString()
        }
    }

    private fun enterStartFailed(message: String) {
        finished = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewBind.progressMeasure.visibility = View.GONE
        viewBind.btnRetry.visibility = View.VISIBLE
        toast(message)
    }

    private fun enterDoneState() {
        viewBind.progressMeasure.visibility = View.GONE
        viewBind.btnRetry.visibility = View.VISIBLE
        viewBind.btnViewReport.visibility = View.VISIBLE
        viewBind.tvDuration.visibility = View.GONE
        viewBind.tvLeadHint.visibility = View.GONE
        viewBind.layoutKpi.alpha = 1f
        val samples = session?.getSamples().orEmpty()
        viewBind.ecgWaveform.freeze(samples)
    }

    private fun saveReport() {
        val entity = session?.toReportEntityOrNull() ?: return
        try {
            appDatabase.ecgReportDao().insert(entity)
            lastReportId = entity.reportId
        } catch (e: Exception) {
            Timber.w(e)
            toast(R.string.ecg_measure_save_failed)
        }
    }

    private fun showCompletedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ecg_measure_done_title)
            .setMessage(R.string.ecg_measure_done_msg)
            .setPositiveButton(R.string.ecg_measure_view_report) { _, _ ->
                toast(getString(R.string.ecg_measure_report_saved, lastReportId.orEmpty()))
            }
            .setNegativeButton(R.string.ecg_measure_cancel, null)
            .show()
    }

    private fun showFailedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ecg_measure_fail_title)
            .setMessage(R.string.ecg_measure_fail_msg)
            .setPositiveButton(R.string.ecg_measure_retry) { _, _ -> startMeasure() }
            .setNegativeButton(R.string.ecg_measure_cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun askExit() {
        if (finished) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ecg_measure_exit_title)
            .setMessage(R.string.ecg_measure_exit_msg)
            .setPositiveButton(R.string.ecg_measure_exit) { _, _ ->
                measureDisposable?.dispose()
                measureDisposable = null
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                finish()
            }
            .setNegativeButton(R.string.ecg_measure_continue, null)
            .show()
    }

    override fun onDestroy() {
        measureDisposable?.dispose()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EcgMeasureActivity::class.java))
        }
    }
}
