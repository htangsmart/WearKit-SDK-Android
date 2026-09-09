package com.topstep.wearkit.sample.ui.ai.record

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityRecordBinding
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager
import com.topstep.wearkit.sample.ui.ai.isDeviceConnected
import com.topstep.wearkit.sample.ui.ai.isScoConnected
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * APP / 设备录音页：状态、时长、ASR 文本；设备会话由 [SpeechAiManager] 驱动。
 */
class RecordActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private val record = wearKit.speechAiAbility.record
    private lateinit var viewBind: ActivityRecordBinding
    private val uiHandler = Handler(Looper.getMainLooper())

    private val durationTicker = object : Runnable {
        override fun run() {
            if (!RecordTranscript.recording.value) return
            refreshProgress()
            uiHandler.postDelayed(this, 200L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_record)

        viewBind.btnRecordPhone.setOnClickListener {
            startAppRecord(WKSpeechSession.Source.PHONE_MIC)
        }
        viewBind.btnRecordSco.setOnClickListener {
            if (!wearKit.isScoConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppRecord(WKSpeechSession.Source.DEVICE_SCO)
        }
        viewBind.btnRecordDevice.setOnClickListener {
            if (!wearKit.isDeviceConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppRecord(WKSpeechSession.Source.DEVICE_CMD)
        }
        viewBind.btnRecordPause.setOnClickListener {
            record.pause().onErrorComplete().subscribe()
        }
        viewBind.btnRecordResume.setOnClickListener {
            record.resume().onErrorComplete().subscribe()
        }
        viewBind.btnRecordExit.setOnClickListener {
            stopRecordSession()
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                RecordTranscript.recording.collect { recording ->
                    viewBind.btnRecordPhone.isEnabled = !recording
                    viewBind.btnRecordSco.isEnabled = !recording
                    viewBind.btnRecordDevice.isEnabled = !recording
                    viewBind.btnRecordExit.isEnabled = recording
                    viewBind.rgRecordLang.isEnabled = !recording
                    for (i in 0 until viewBind.rgRecordLang.childCount) {
                        viewBind.rgRecordLang.getChildAt(i).isEnabled = !recording
                    }
                    if (recording) {
                        uiHandler.removeCallbacks(durationTicker)
                        uiHandler.post(durationTicker)
                    } else {
                        uiHandler.removeCallbacks(durationTicker)
                        viewBind.tvRecordDuration.setText(R.string.ds_speech_duration_idle)
                        viewBind.tvRecordState.setText(R.string.ds_speech_ready)
                        refreshPauseButtons(recording = false, paused = false)
                    }
                }
            }
            launch {
                RecordTranscript.info.collect { info ->
                    if (info == null) return@collect
                    refreshProgress()
                }
            }
            launch {
                RecordTranscript.asrText.collect { text ->
                    viewBind.tvAsrText.text = text
                }
            }
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(durationTicker)
        if (isFinishing) {
            stopRecordSession()
        }
        super.onDestroy()
    }

    private fun stopRecordSession() {
        val scene = SpeechAiManager.activeSession.value?.scene ?: return
        if (scene != WKSpeechSession.Scene.RECORD && scene != WKSpeechSession.Scene.CALL_RECORD) {
            return
        }
        SpeechAiManager.stopActiveSession()
    }

    private fun startAppRecord(source: WKSpeechSession.Source) {
        if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
            toast(R.string.ds_speech_init)
            return
        }
        if (!wearKit.speechAiAbility.session.isSupportAppScene(WKSpeechSession.Scene.RECORD)) {
            toast(R.string.tip_un_support)
            return
        }
        RecordTranscript.appLocale = selectedAppLocale()
        val session = SpeechAiManager.createAppSession(WKSpeechSession.Scene.RECORD, source)
        if (session == null) {
            toast(R.string.tip_failed)
        }
    }

    private fun selectedAppLocale(): String {
        return if (viewBind.rgRecordLang.checkedRadioButtonId == R.id.rb_lang_en) {
            "en-US"
        } else {
            "zh-CN"
        }
    }

    private fun refreshProgress() {
        val info = RecordTranscript.info.value ?: return
        val paused = record.isPaused()
        val durationMs = record.getDurationMs()
        val stateRes = if (paused) {
            R.string.ds_speech_paused_detail
        } else {
            R.string.ds_speech_recording_detail
        }
        viewBind.tvRecordState.text = getString(
            stateRes,
            info.origin.name,
            info.source.name,
            info.localeLabel,
        )
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        viewBind.tvRecordDuration.text = getString(R.string.ds_speech_duration, minutes, seconds)
        refreshPauseButtons(recording = true, paused = paused)
    }

    private fun refreshPauseButtons(recording: Boolean, paused: Boolean) {
        val support = recording && record.isSupportPause()
        viewBind.btnRecordPause.isEnabled = support && !paused
        viewBind.btnRecordResume.isEnabled = support && paused
    }
}
