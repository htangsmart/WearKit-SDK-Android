package com.topstep.wearkit.sample.ui.ai.chattranslate

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.CHAT_TRANSLATE_SELF] / [WKSpeechSession.Scene.CHAT_TRANSLATE_PEER]
 *
 * 独立于普通 Translate：按 [WKChatTranslateMode] + 声源角色决定回传文本与 TTS 路由。
 * 不调用 [WKSpeechAiAbility.Translate.sendTtsReady]，也不处理
 * [com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE]。
 *
 * Session 音频结束时只复位页面（[ChatTranslateTranscript.onSessionEnded]），
 * ASR/TTS 流继续；Observable complete 再 [release]，且不 [MyAudioPlayer.deactivate]，
 * 让 [MyAudioPlayer.sendFinish] 自然播完。用户停止 / SCENE_EXIT / 出错时才强制停 TTS。
 */
class ChatTranslateHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = session.scene
    override val tag = "ChatTranslateHandler"

    private val isSelf = session.scene == WKSpeechSession.Scene.CHAT_TRANSLATE_SELF
    private val policy: Policy
    private val originalLocale: String
    private val translateLocale: String

    /** true：正常收尾，release 时不掐断 TTS。 */
    @Volatile
    private var allowTtsDrain = false

    init {
        val mode = ChatTranslateTranscript.activeMode ?: WKChatTranslateMode.FACE_TO_FACE
        policy = Policy.resolve(mode, isSelf)
        if (isSelf) {
            originalLocale = ChatTranslateTranscript.selfLocale
            translateLocale = ChatTranslateTranscript.peerLocale
        } else {
            originalLocale = ChatTranslateTranscript.peerLocale
            translateLocale = ChatTranslateTranscript.selfLocale
        }
    }

    override fun onStart() {
        Timber.tag(tag).i(
            "start mode=%s self=%s origin=%s source=%s %s -> %s policy=%s",
            ChatTranslateTranscript.activeMode, isSelf,
            session.origin, session.source, originalLocale, translateLocale, policy,
        )
        ChatTranslateTranscript.onSessionStarted(session, isSelf, originalLocale, translateLocale)
        MyAudioPlayer.activate(policy.ttsRoute)
        startAsr()
    }

    private fun startAsr() {
        // Session 音频结束 → 仅复位页面（false 不 release）；ASR/TTS 数据流结束后再 release
        val source = bindAudioSource(
            onAudioStop = {
                Timber.tag(tag).i("audio stop → UI follow session")
                ChatTranslateTranscript.onSessionEnded()
                false
            },
        )
        disposables.add(
            aiKit.audio.asr(
                source,
                AiAsrParams(
                    originalLocale = originalLocale,
                    translateLocale = translateLocale,
                    originalFileRequired = false,
                    translateTtsRequired = true,
                    autoStop = true,
                ),
            ).subscribe({ result ->
                when (result) {
                    is AiAsrResult.OriginalText -> {
                        Timber.tag(tag).i(
                            "source[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete,
                        )
                        ChatTranslateTranscript.onSourceText(isSelf, result.text, result.isComplete, result.index)
                        if (policy.sendSource) {
                            speechAi.translate
                                .sendTextSource(result.text, result.isComplete)
                                .onErrorComplete().subscribe()
                        }
                    }
                    is AiAsrResult.TranslateText -> {
                        Timber.tag(tag).i(
                            "target[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete,
                        )
                        ChatTranslateTranscript.onTargetText(isSelf, result.text, result.isComplete, result.index)
                        if (policy.sendTarget) {
                            speechAi.translate
                                .sendTextTarget(result.text, result.isComplete)
                                .onErrorComplete().subscribe()
                        }
                    }
                    is AiAsrResult.TranslateTts -> {
                        if (!result.isComplete) {
                            ensureTtsStarted()
                            if (result.bytes.isNotEmpty()) {
                                MyAudioPlayer.sendData(result.bytes.copyOf())
                            }
                        } else if (MyAudioPlayer.isStarted()) {
                            MyAudioPlayer.sendFinish()
                        }
                    }
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it, "asr error")
                release()
            }, {
                Timber.tag(tag).i("asr/tts complete → release (drain tts)")
                allowTtsDrain = true
                release()
            })
        )
    }

    private fun ensureTtsStarted() {
        if (!MyAudioPlayer.isStarted()) {
            MyAudioPlayer.activate(policy.ttsRoute)
            MyAudioPlayer.start()
        }
    }

    override fun onRelease() {
        if (!allowTtsDrain) {
            MyAudioPlayer.deactivate()
        }
        ChatTranslateTranscript.onSessionEnded()
    }

    /**
     * Per-utterance routing: what to send to device and where TTS plays.
     */
    private data class Policy(
        val sendSource: Boolean,
        val sendTarget: Boolean,
        val ttsRoute: WKSpeechSession.Source,
    ) {
        companion object {
            fun resolve(mode: WKChatTranslateMode, isSelf: Boolean): Policy {
                return when (mode) {
                    WKChatTranslateMode.FACE_TO_FACE -> if (isSelf) {
                        Policy(
                            sendSource = false,
                            sendTarget = true,
                            ttsRoute = WKSpeechSession.Source.DEVICE_CMD,
                        )
                    } else {
                        Policy(
                            sendSource = true,
                            sendTarget = false,
                            ttsRoute = WKSpeechSession.Source.PHONE_MIC,
                        )
                    }
                    WKChatTranslateMode.PRIVATE -> if (isSelf) {
                        Policy(
                            sendSource = false,
                            sendTarget = true,
                            ttsRoute = WKSpeechSession.Source.DEVICE_CMD,
                        )
                    } else {
                        Policy(
                            sendSource = true,
                            sendTarget = false,
                            ttsRoute = WKSpeechSession.Source.DEVICE_SCO,
                        )
                    }
                    WKChatTranslateMode.PORTABLE -> if (isSelf) {
                        Policy(
                            sendSource = false,
                            sendTarget = false,
                            ttsRoute = WKSpeechSession.Source.DEVICE_SCO,
                        )
                    } else {
                        Policy(
                            sendSource = false,
                            sendTarget = false,
                            ttsRoute = WKSpeechSession.Source.PHONE_MIC,
                        )
                    }
                }
            }
        }
    }
}
