package com.topstep.wearkit.sample.ui.ai.translate

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityTranslateBinding
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager
import com.topstep.wearkit.sample.ui.ai.isDeviceConnected
import com.topstep.wearkit.sample.ui.ai.isScoConnected
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import kotlinx.coroutines.launch

/**
 * APP / 设备翻译页：状态、原文/译文、TTS 状态；设备会话由 [SpeechAiManager] 驱动。
 */
class TranslateActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityTranslateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityTranslateBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_translate)

        syncLangUiFromTranscript()

        viewBind.btnSwapLang.setOnClickListener {
            applyUiLocalesToTranscript()
            TranslateTranscript.swapAppLocales()
            syncLangUiFromTranscript()
        }
        viewBind.btnTranslatePhone.setOnClickListener {
            startAppTranslate(WKSpeechSession.Source.PHONE_MIC)
        }
        viewBind.btnTranslateSco.setOnClickListener {
            if (!wearKit.isScoConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppTranslate(WKSpeechSession.Source.DEVICE_SCO)
        }
        viewBind.btnTranslateDevice.setOnClickListener {
            if (!wearKit.isDeviceConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppTranslate(WKSpeechSession.Source.DEVICE_CMD)
        }
        viewBind.btnTranslateExit.setOnClickListener {
            stopTranslateSession()
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                TranslateTranscript.translating.collect { translating ->
                    viewBind.btnTranslatePhone.isEnabled = !translating
                    viewBind.btnTranslateSco.isEnabled = !translating
                    viewBind.btnTranslateDevice.isEnabled = !translating
                    viewBind.btnTranslateExit.isEnabled = translating
                    viewBind.btnSwapLang.isEnabled = !translating
                    setLangGroupsEnabled(!translating)
                    if (!translating) {
                        viewBind.tvTranslateState.setText(R.string.ds_speech_ready)
                        viewBind.tvTtsState.setText(R.string.ds_speech_tts_idle)
                    }
                }
            }
            launch {
                TranslateTranscript.info.collect { info ->
                    if (info == null) return@collect
                    viewBind.tvTranslateState.text = getString(
                        R.string.ds_speech_translating_detail,
                        info.origin.name,
                        info.source.name,
                        shortLocale(info.originalLocale),
                        shortLocale(info.translateLocale),
                    )
                }
            }
            launch {
                TranslateTranscript.messages.collect { messages ->
                    renderMessages(messages)
                }
            }
            launch {
                TranslateTranscript.playerState.collect { state ->
                    viewBind.tvTtsState.text = when (state) {
                        null -> getString(R.string.ds_speech_tts_idle)
                        WKTranslatePlayerState.START -> getString(R.string.ds_speech_tts_start)
                        WKTranslatePlayerState.STOP -> getString(R.string.ds_speech_tts_stop)
                        WKTranslatePlayerState.PAUSE -> getString(R.string.ds_speech_tts_pause)
                        WKTranslatePlayerState.RESUME -> getString(R.string.ds_speech_tts_resume)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopTranslateSession()
        }
        super.onDestroy()
    }

    private fun stopTranslateSession() {
        if (SpeechAiManager.activeSession.value?.scene != WKSpeechSession.Scene.TRANSLATE) return
        SpeechAiManager.stopActiveSession()
    }

    private fun startAppTranslate(source: WKSpeechSession.Source) {
        if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
            toast(R.string.ds_speech_init)
            return
        }
        if (!wearKit.speechAiAbility.session.isSupportAppScene(WKSpeechSession.Scene.TRANSLATE)) {
            toast(R.string.tip_un_support)
            return
        }
        applyUiLocalesToTranscript()
        if (TranslateTranscript.appOriginalLocale == TranslateTranscript.appTranslateLocale) {
            toast(R.string.ds_speech_lang_same)
            return
        }
        val session = SpeechAiManager.createAppSession(WKSpeechSession.Scene.TRANSLATE, source)
        if (session == null) {
            toast(R.string.tip_failed)
        }
    }

    private fun applyUiLocalesToTranscript() {
        TranslateTranscript.appOriginalLocale =
            if (viewBind.rgSourceLang.checkedRadioButtonId == R.id.rb_source_en) "en-US" else "zh-CN"
        TranslateTranscript.appTranslateLocale =
            if (viewBind.rgTargetLang.checkedRadioButtonId == R.id.rb_target_en) "en-US" else "zh-CN"
    }

    private fun syncLangUiFromTranscript() {
        viewBind.rgSourceLang.check(
            if (TranslateTranscript.appOriginalLocale == "en-US") R.id.rb_source_en else R.id.rb_source_zh,
        )
        viewBind.rgTargetLang.check(
            if (TranslateTranscript.appTranslateLocale == "en-US") R.id.rb_target_en else R.id.rb_target_zh,
        )
    }

    private fun setLangGroupsEnabled(enabled: Boolean) {
        viewBind.rgSourceLang.isEnabled = enabled
        viewBind.rgTargetLang.isEnabled = enabled
        for (i in 0 until viewBind.rgSourceLang.childCount) {
            viewBind.rgSourceLang.getChildAt(i).isEnabled = enabled
        }
        for (i in 0 until viewBind.rgTargetLang.childCount) {
            viewBind.rgTargetLang.getChildAt(i).isEnabled = enabled
        }
    }

    private fun renderMessages(messages: List<TranslateMessage>) {
        viewBind.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        viewBind.llMessages.removeAllViews()
        messages.forEach { msg ->
            val label = getString(
                if (msg.isSource) R.string.ds_speech_source else R.string.ds_speech_target,
            )
            val suffix = if (msg.isComplete) "" else "…"
            viewBind.llMessages.addView(TextView(this).apply {
                text = "$label: ${msg.text}$suffix"
                textSize = 15f
                setPadding(0, 8, 0, 8)
                setTextColor(if (msg.isSource) 0xFF333333.toInt() else 0xFF1565C0.toInt())
            })
        }
        viewBind.scrollTranslate.post {
            viewBind.scrollTranslate.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun shortLocale(locale: String): String {
        return if (locale.startsWith("en")) "en" else "zh"
    }
}
