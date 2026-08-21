package com.topstep.wearkit.sample.ui.ai.chat

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityChatBinding
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager
import com.topstep.wearkit.sample.ui.ai.isScoConnected
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.launch

/**
 * Displays chat text. ASR / LLM run in [SpeechAiManager] via device session handlers.
 */
class ChatActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityChatBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_chat)

        viewBind.btnChatPhone.setOnClickListener {
            startAppChat(WKSpeechSession.Source.PHONE_MIC)
        }
        viewBind.btnChatSco.setOnClickListener {
            if (!wearKit.isScoConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppChat(WKSpeechSession.Source.DEVICE_SCO)
        }
        viewBind.btnChatAuto.setOnClickListener {
            val source = if (wearKit.isScoConnected()) {
                WKSpeechSession.Source.DEVICE_SCO
            } else {
                WKSpeechSession.Source.PHONE_MIC
            }
            startAppChat(source)
        }
        viewBind.btnChatExit.setOnClickListener {
            stopChatSession()
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                ChatTranscript.chatting.collect { chatting ->
                    viewBind.tvChatState.setText(
                        if (chatting) R.string.ds_speech_chatting else R.string.ds_speech_ready
                    )
                    viewBind.btnChatPhone.isEnabled = !chatting
                    viewBind.btnChatSco.isEnabled = !chatting
                    viewBind.btnChatAuto.isEnabled = !chatting
                    viewBind.btnChatExit.isEnabled = chatting
                }
            }
            launch {
                ChatTranscript.messages.collect { messages ->
                    renderMessages(messages)
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopChatSession()
        }
        super.onDestroy()
    }

    private fun stopChatSession() {
        if (SpeechAiManager.activeSession.value?.scene != WKSpeechSession.Scene.CHAT) return
        SpeechAiManager.stopActiveSession()
    }

    private fun startAppChat(source: WKSpeechSession.Source) {
        if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
            toast(R.string.ds_speech_init)
            return
        }
        if (!wearKit.speechAiAbility.session.isSupportAppScene(WKSpeechSession.Scene.CHAT)) {
            toast(R.string.tip_un_support)
            return
        }
        PermissionHelper.requestRecordAudio(this) { granted ->
            if (!granted) {
                toast(R.string.ds_speech_need_record)
                return@requestRecordAudio
            }
            val session = SpeechAiManager.createAppSession(WKSpeechSession.Scene.CHAT, source)
            if (session == null) {
                toast(R.string.tip_failed)
            }
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
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
        viewBind.scrollChat.post {
            viewBind.scrollChat.fullScroll(View.FOCUS_DOWN)
        }
    }
}
