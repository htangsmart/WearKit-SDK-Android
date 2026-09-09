package com.topstep.wearkit.sample.ui.ai.ask

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityAskBinding
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager
import com.topstep.wearkit.sample.ui.ai.isScoConnected
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Ask page: show Q/A text; hold-to-talk on phone mic or SCO (release ends capture).
 * TTS follows the same source: phone → speaker, SCO → headset.
 */
class AskActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityAskBinding
    private var heldButton: View? = null

    @Volatile
    private var holdDown = false
    private var pressStarted = false
    private var pendingStart: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityAskBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_ask)

        PermissionHelper.requestRecordAudio(this) { granted ->
            if (!granted) toast(R.string.ds_speech_need_record)
        }
        bindHoldToTalk(viewBind.btnAskPhone, WKSpeechSession.Source.PHONE_MIC)
        bindHoldToTalk(viewBind.btnAskSco, WKSpeechSession.Source.DEVICE_SCO, requireSco = true)

        lifecycle.launchRepeatOnStarted {
            launch {
                combine(AskTranscript.inUtterance, AskTranscript.sessionActive) { recording, active ->
                    recording to active
                }.collect { (recording, active) ->
                    viewBind.tvAskState.setText(
                        when {
                            recording -> R.string.ds_speech_asking
                            active -> R.string.ds_speech_answering
                            else -> R.string.ds_speech_ready
                        }
                    )
                    // 与对话翻译一致：松手结束采集后立刻恢复按钮，不等整轮回答结束
                    setStartButtonsEnabled(!recording)
                }
            }
            launch {
                AskTranscript.messages.collect { messages ->
                    renderMessages(messages)
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopAskSession()
        }
        super.onDestroy()
    }

    private fun bindHoldToTalk(
        button: Button,
        source: WKSpeechSession.Source,
        requireSco: Boolean = false,
    ) {
        button.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (AskTranscript.inUtterance.value) return@setOnTouchListener true
                    if (requireSco && !wearKit.isScoConnected()) {
                        toast(R.string.device_state_disconnected)
                        return@setOnTouchListener true
                    }
                    heldButton = v
                    holdDown = true
                    v.isPressed = true
                    val start = Runnable {
                        if (!holdDown) return@Runnable
                        val ok = startAppAsk(source)
                        if (!ok) {
                            v.isPressed = false
                            heldButton = null
                            return@Runnable
                        }
                        if (!holdDown) {
                            SpeechAiManager.endActiveCapture()
                            AskTranscript.onRecordingEnded()
                        } else {
                            pressStarted = true
                        }
                    }
                    pendingStart = start
                    v.postDelayed(start, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdDown = false
                    pendingStart?.let { v.removeCallbacks(it) }
                    pendingStart = null
                    v.isPressed = false
                    heldButton = null
                    if (pressStarted) {
                        pressStarted = false
                        SpeechAiManager.endActiveCapture()
                        // 先解锁 UI；Handler 侧 onAudioStop 再调一次也无妨
                        AskTranscript.onRecordingEnded()
                    }
                }
            }
            true
        }
    }

    private fun stopAskSession() {
        if (SpeechAiManager.activeSession.value?.scene == WKSpeechSession.Scene.ASK) {
            SpeechAiManager.stopActiveSession()
        }
        MyAudioPlayer.deactivate()
    }

    /** @return true 已开到 session。 */
    private fun startAppAsk(source: WKSpeechSession.Source): Boolean {
        if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
            toast(R.string.ds_speech_init)
            return false
        }
        if (!wearKit.speechAiAbility.session.isSupportAppScene(WKSpeechSession.Scene.ASK)) {
            toast(R.string.tip_un_support)
            return false
        }
        // 上一轮回答/TTS 未结束时，先结束再开新一轮，避免按钮看似可点却创建失败
        if (SpeechAiManager.activeSession.value?.scene == WKSpeechSession.Scene.ASK) {
            SpeechAiManager.stopActiveSession()
            MyAudioPlayer.deactivate()
        }
        val session = SpeechAiManager.createAppSession(WKSpeechSession.Scene.ASK, source)
        if (session == null) {
            toast(R.string.tip_failed)
            return false
        }
        return true
    }

    private fun setStartButtonsEnabled(enabled: Boolean) {
        listOf(viewBind.btnAskPhone, viewBind.btnAskSco).forEach { btn ->
            if (!enabled && btn === heldButton) return@forEach
            btn.isEnabled = enabled
        }
    }

    private fun renderMessages(messages: List<AskMessage>) {
        viewBind.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        viewBind.llMessages.removeAllViews()
        messages.forEach { msg ->
            val label = getString(if (msg.isQuestion) R.string.ds_speech_question else R.string.ds_speech_answer)
            val suffix = if (msg.isComplete) "" else "…"
            viewBind.llMessages.addView(TextView(this).apply {
                text = "$label: ${msg.text}$suffix"
                textSize = 15f
                setPadding(0, 8, 0, 8)
                setTextColor(if (msg.isQuestion) 0xFF333333.toInt() else 0xFF1565C0.toInt())
            })
        }
        viewBind.scrollAsk.post {
            viewBind.scrollAsk.fullScroll(View.FOCUS_DOWN)
        }
    }
}
