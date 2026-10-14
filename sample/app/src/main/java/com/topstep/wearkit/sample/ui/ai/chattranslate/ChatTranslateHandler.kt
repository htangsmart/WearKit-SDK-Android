package com.topstep.wearkit.sample.ui.ai.chattranslate

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.CHAT_TRANSLATE_SELF] / [WKSpeechSession.Scene.CHAT_TRANSLATE_PEER]
 *
 * 独立于普通 Translate：按 [WKChatTranslateMode] + 声源角色决定回传文本与 TTS 路由。
 * 不调用 [WKSpeechAiAbility.Translate.sendTtsReady]，也不处理
 * [com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE]。
 *
 * 先录完再播：录音期交给 [ChatTranslateReplay] 缓存；ASR Observable complete 后再更新页面、下发并播放
 *（[autoStop] 为 false，complete 发生在停采之后）。
 * Session 自行 release 后 Handler 仍可交付；新 session attach 会 [release] 本 Handler。
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

    /** 本轮是否识别出非空文本；用于"采集过短"提示的判定。 */
    @Volatile
    private var gotText = false

    /** 首帧音频到达时刻（[SystemClock.elapsedRealtime]）；0 = 本轮尚未开始采集。 */
    @Volatile
    private var firstAudioAtMs = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val isSelf = session.scene == WKSpeechSession.Scene.CHAT_TRANSLATE_SELF
    private val policy: Policy
    private val originalLocale: String
    private val translateLocale: String
    private val replay: ChatTranslateReplay

    init {
        val mode = ChatTranslateTranscript.activeMode ?: WKChatTranslateMode.FACE_TO_FACE
        policy = Policy.resolve(mode, isSelf)
        replay = ChatTranslateReplay(
            speechAi = speechAi,
            ttsRoute = policy.ttsRoute,
            isSelf = isSelf,
            sendSource = policy.sendSource,
            sendTarget = policy.sendTarget,
        )
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
        gotText = false
        firstAudioAtMs = 0L
        startAsr()
    }

    private fun startAsr() {
        val source = bindAudioSource(
            onAudioStart = {
                // 首帧到达才说明麦克风真的在采（SCO / 设备通道要先开会话 + 建链）
                firstAudioAtMs = SystemClock.elapsedRealtime()
                ChatTranslateTranscript.onRecordingStarted()
            },
            onAudioStop = {
                Timber.tag(tag).i("audio stop → UI follow session")
                ChatTranslateTranscript.onRecordingEnded()
                hintIfCaptureTooShort()
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
                        if (result.text.isNotEmpty()) gotText = true
                        replay.cacheSource(result.text, result.isComplete, result.index)
                    }
                    is AiAsrResult.TranslateText -> {
                        Timber.tag(tag).i(
                            "target[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete,
                        )
                        if (result.text.isNotEmpty()) gotText = true
                        replay.cacheTarget(result.text, result.isComplete, result.index)
                    }
                    is AiAsrResult.TranslateTts -> {
                        if (!result.isComplete) {
                            replay.cacheTts(result.bytes)
                        }
                    }
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it, "asr error")
                release()
            }, {
                Timber.tag(tag).i("asr/tts complete → deliver")
                disposables.add(
                    replay.deliver().subscribe({
                        release()
                    }, {
                        Timber.tag(tag).w(it, "deliver error")
                        release()
                    })
                )
            })
        )
    }

    /**
     * 采集过短提示：SCO / 设备通道按下后要 1~2 秒才真正开始采（会话握手 + 建链），
     * 这段时间松手就会"看起来收不到音"。本轮没识别出文本且采集窗口过短时提示一次。
     */
    private fun hintIfCaptureTooShort() {
        if (gotText) return
        val capturedMs = if (firstAudioAtMs == 0L) {
            // 首帧从未到达：整轮都停在握手/建链阶段
            0L
        } else {
            SystemClock.elapsedRealtime() - firstAudioAtMs
        }
        if (capturedMs >= MIN_CAPTURE_MS) return
        Timber.tag(tag).i("capture too short: %dms, hint hold-to-talk", capturedMs)
        mainHandler.post {
            Toast.makeText(context, R.string.ds_speech_ct_tip_short_capture, Toast.LENGTH_LONG).show()
        }
    }

    override fun onRelease() {
        replay.abort()
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

    private companion object {
        /** 采集窗口短于该值且没有识别出文本时，提示用户等链路就绪再开口。 */
        const val MIN_CAPTURE_MS = 1_000L
    }
}
