package com.topstep.wearkit.sample.ui.ai.chattranslate

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityChatTranslateBinding
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager
import com.topstep.wearkit.sample.ui.ai.isScoConnected
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch

/**
 * Chat-translate page: dual-side source/target text, mode-specific start buttons.
 * Device [WKSpeechAiMessage.Type.SCENE_EXIT] for SELF/PEER finishes this page.
 */
class ChatTranslateActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityChatTranslateBinding
    private val disposables = CompositeDisposable()
    private var leaving = false

    private val mode: WKChatTranslateMode by lazy {
        val name = intent.getStringExtra(EXTRA_MODE)
        runCatching { WKChatTranslateMode.valueOf(name.orEmpty()) }
            .getOrElse { WKChatTranslateMode.FACE_TO_FACE }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityChatTranslateBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_chat_translate)

        ChatTranslateTranscript.activeMode = mode
        viewBind.tvMode.text = modeLabel(mode)
        syncLangUi()
        applyModeButtons()

        viewBind.btnSwapLang.setOnClickListener {
            applyUiLocales()
            ChatTranslateTranscript.swapLocales()
            syncLangUi()
        }
        viewBind.btnSelfPhone.setOnClickListener {
            startAppUtterance(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF, WKSpeechSession.Source.PHONE_MIC)
        }
        viewBind.btnSelfSco.setOnClickListener {
            if (!wearKit.isScoConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppUtterance(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF, WKSpeechSession.Source.DEVICE_SCO)
        }
        viewBind.btnPeerSco.setOnClickListener {
            if (!wearKit.isScoConnected()) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            startAppUtterance(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER, WKSpeechSession.Source.DEVICE_SCO)
        }
        viewBind.btnPeerPhone.setOnClickListener {
            startAppUtterance(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER, WKSpeechSession.Source.PHONE_MIC)
        }
        viewBind.btnStopSession.setOnClickListener {
            stopCurrentSession()
        }
        viewBind.btnExit.setOnClickListener {
            leaveChatTranslate(finishPage = true)
        }

        disposables.add(
            wearKit.speechAiAbility.observeMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ msg ->
                    if (msg.type != WKSpeechAiMessage.Type.SCENE_EXIT) return@subscribe
                    val scene = msg.data as? WKSpeechSession.Scene ?: return@subscribe
                    if (scene == WKSpeechSession.Scene.CHAT_TRANSLATE_SELF
                        || scene == WKSpeechSession.Scene.CHAT_TRANSLATE_PEER
                    ) {
                        leaveChatTranslate(finishPage = true)
                    }
                }, {
                    // ignore
                })
        )

        lifecycle.launchRepeatOnStarted {
            launch {
                ChatTranslateTranscript.inUtterance.collect { active ->
                    setStartButtonsEnabled(!active)
                    viewBind.btnStopSession.isEnabled = active
                    viewBind.btnSwapLang.isEnabled = !active
                    setLangGroupsEnabled(!active)
                    if (!active) {
                        viewBind.tvState.setText(R.string.ds_speech_ready)
                    }
                }
            }
            launch {
                ChatTranslateTranscript.info.collect { info ->
                    if (info == null) return@collect
                    val role = getString(
                        if (info.isSelf) R.string.ds_speech_ct_role_self else R.string.ds_speech_ct_role_peer,
                    )
                    viewBind.tvState.text = getString(
                        R.string.ds_speech_ct_utterance_detail,
                        role,
                        info.origin.name,
                        info.source.name,
                        shortLocale(info.originalLocale),
                        shortLocale(info.translateLocale),
                    )
                }
            }
            launch {
                ChatTranslateTranscript.messages.collect { messages ->
                    renderMessages(messages)
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            leaveChatTranslate(finishPage = false)
        }
        disposables.clear()
        super.onDestroy()
    }

    private fun stopCurrentSession() {
        val scene = SpeechAiManager.activeSession.value?.scene ?: return
        if (scene != WKSpeechSession.Scene.CHAT_TRANSLATE_SELF
            && scene != WKSpeechSession.Scene.CHAT_TRANSLATE_PEER
        ) {
            return
        }
        SpeechAiManager.stopActiveSession()
    }

    private fun leaveChatTranslate(finishPage: Boolean) {
        if (leaving) return
        leaving = true
        val scene = SpeechAiManager.activeSession.value?.scene
        if (scene == WKSpeechSession.Scene.CHAT_TRANSLATE_SELF
            || scene == WKSpeechSession.Scene.CHAT_TRANSLATE_PEER
        ) {
            SpeechAiManager.stopActiveSession()
        }
        // 页面退出时彻底停止 TTS（onRelease 已不负责停，避免残留播放）
        MyAudioPlayer.stop()
        MyAudioPlayer.deactivate()
        wearKit.speechAiAbility.translate.stopChatTranslate()
            .onErrorComplete()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                ChatTranslateTranscript.clearMode()
                if (finishPage && !isFinishing) {
                    finish()
                }
            }.also { disposable ->
                // 页面仍在时纳入生命周期；onDestroy 已 isFinishing 时不 cancel，让 0xB3 退出包发完
                if (!isFinishing) {
                    disposables.add(disposable)
                }
            }
    }

    private fun startAppUtterance(scene: WKSpeechSession.Scene, source: WKSpeechSession.Source) {
        if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
            toast(R.string.ds_speech_init)
            return
        }
        if (!wearKit.speechAiAbility.session.isSupportAppScene(scene)) {
            toast(R.string.tip_un_support)
            return
        }
        applyUiLocales()
        if (ChatTranslateTranscript.selfLocale == ChatTranslateTranscript.peerLocale) {
            toast(R.string.ds_speech_lang_same)
            return
        }
        val session = SpeechAiManager.createAppSession(scene, source)
        if (session == null) {
            toast(R.string.tip_failed)
        }
    }

    private fun applyModeButtons() {
        viewBind.btnSelfPhone.visibility = View.GONE
        viewBind.btnSelfSco.visibility = View.GONE
        viewBind.btnPeerSco.visibility = View.GONE
        viewBind.btnPeerPhone.visibility = View.GONE
        when (mode) {
            WKChatTranslateMode.FACE_TO_FACE -> {
                viewBind.btnSelfPhone.visibility = View.VISIBLE
            }
            WKChatTranslateMode.PRIVATE -> {
                viewBind.btnSelfSco.visibility = View.VISIBLE
            }
            WKChatTranslateMode.PORTABLE -> {
                viewBind.btnSelfPhone.visibility = View.VISIBLE
                viewBind.btnPeerSco.visibility = View.VISIBLE
                viewBind.btnPeerPhone.visibility = View.VISIBLE
            }
        }
    }

    private fun setStartButtonsEnabled(enabled: Boolean) {
        viewBind.btnSelfPhone.isEnabled = enabled
        viewBind.btnSelfSco.isEnabled = enabled
        viewBind.btnPeerSco.isEnabled = enabled
        viewBind.btnPeerPhone.isEnabled = enabled
    }

    private fun applyUiLocales() {
        ChatTranslateTranscript.selfLocale =
            if (viewBind.rgSelfLang.checkedRadioButtonId == R.id.rb_self_en) "en-US" else "zh-CN"
        ChatTranslateTranscript.peerLocale =
            if (viewBind.rgPeerLang.checkedRadioButtonId == R.id.rb_peer_en) "en-US" else "zh-CN"
    }

    private fun syncLangUi() {
        viewBind.rgSelfLang.check(
            if (ChatTranslateTranscript.selfLocale == "en-US") R.id.rb_self_en else R.id.rb_self_zh,
        )
        viewBind.rgPeerLang.check(
            if (ChatTranslateTranscript.peerLocale == "en-US") R.id.rb_peer_en else R.id.rb_peer_zh,
        )
    }

    private fun setLangGroupsEnabled(enabled: Boolean) {
        viewBind.rgSelfLang.isEnabled = enabled
        viewBind.rgPeerLang.isEnabled = enabled
        for (i in 0 until viewBind.rgSelfLang.childCount) {
            viewBind.rgSelfLang.getChildAt(i).isEnabled = enabled
        }
        for (i in 0 until viewBind.rgPeerLang.childCount) {
            viewBind.rgPeerLang.getChildAt(i).isEnabled = enabled
        }
    }

    private fun renderMessages(messages: List<ChatTranslateMessage>) {
        viewBind.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        viewBind.llMessages.removeAllViews()
        messages.forEach { msg ->
            val role = getString(if (msg.isSelf) R.string.ds_speech_ct_role_self else R.string.ds_speech_ct_role_peer)
            val kind = getString(if (msg.isSource) R.string.ds_speech_source else R.string.ds_speech_target)
            val suffix = if (msg.isComplete) "" else "…"
            viewBind.llMessages.addView(TextView(this).apply {
                text = "$role · $kind: ${msg.text}$suffix"
                textSize = 15f
                setPadding(0, 8, 0, 8)
                setTextColor(
                    when {
                        msg.isSelf && msg.isSource -> 0xFF333333.toInt()
                        msg.isSelf -> 0xFF1565C0.toInt()
                        msg.isSource -> 0xFF5D4037.toInt()
                        else -> 0xFF2E7D32.toInt()
                    },
                )
            })
        }
        viewBind.scrollMessages.post {
            viewBind.scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun modeLabel(mode: WKChatTranslateMode): String {
        return when (mode) {
            WKChatTranslateMode.FACE_TO_FACE -> getString(R.string.ds_speech_ct_mode_face)
            WKChatTranslateMode.PRIVATE -> getString(R.string.ds_speech_ct_mode_private)
            WKChatTranslateMode.PORTABLE -> getString(R.string.ds_speech_ct_mode_portable)
        }
    }

    private fun shortLocale(locale: String): String {
        return if (locale.startsWith("en")) "en" else "zh"
    }

    companion object {
        const val EXTRA_MODE = "chat_translate_mode"
    }
}
