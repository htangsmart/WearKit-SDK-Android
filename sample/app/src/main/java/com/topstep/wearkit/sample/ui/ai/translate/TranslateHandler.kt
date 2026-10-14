package com.topstep.wearkit.sample.ui.ai.translate

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslateLang
import com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.TRANSLATE]
 *
 * - 调 AiKit ASR/翻译，原文/译文回传设备并写入 [TranslateTranscript]
 * - TTS 回放路由跟随录音来源（从哪里收就从哪里播）：手机麦克风→手机外放 / SCO→耳机 /
 *   设备指令通道→设备；设备不支持下发 PCM 时退回手机外放。
 *   响应设备 [WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE]
 * - APP 语言来自 [TranslateTranscript]；设备语言来自 [WKSpeechAiAbility.Translate.getLang]
 *
 * 音频结束只复位 UI；ASR Observable complete 再 [release]，正常收尾不强制停 TTS。
 */
class TranslateHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = WKSpeechSession.Scene.TRANSLATE
    override val tag = "TranslateHandler"

    /**
     * TTS 回放路由 = 本次录音来源（从哪里收就从哪里播）；
     * DEVICE_CMD 但设备不支持下发 PCM 时退回手机外放（与 Chat / Ask 保持一致）。
     */
    private val ttsRoute: WKSpeechSession.Source = resolveTtsRoute()

    private val ttsController = TranslateTtsController(context, ttsRoute)

    private val originalLocale: String
    private val translateLocale: String

    /** true：正常收尾，release 时不掐断 TTS。 */
    @Volatile
    private var allowTtsDrain = false

    init {
        val pair = resolveLocales()
        originalLocale = pair.first
        translateLocale = pair.second
    }

    override fun onStart() {
        Timber.tag(tag).i(
            "start origin=%s source=%s ttsRoute=%s %s -> %s",
            session.origin, session.source, ttsRoute, originalLocale, translateLocale,
        )
        TranslateTranscript.onSessionStarted(session, originalLocale, translateLocale)
        // 提前绑定回放路由：PHONE_MIC 先切通话路由给 AEC；DEVICE_SCO 挂到耳机；DEVICE_CMD 走设备 Opus
        MyAudioPlayer.activate(ttsRoute)
        startAsr()
    }

    /** DEVICE_CMD 但设备不支持下发 PCM 时退回手机外放。 */
    private fun resolveTtsRoute(): WKSpeechSession.Source {
        if (session.source != WKSpeechSession.Source.DEVICE_CMD) {
            return session.source
        }
        return if (speechAi.player.isSupport(session.scene)) {
            WKSpeechSession.Source.DEVICE_CMD
        } else {
            WKSpeechSession.Source.PHONE_MIC
        }
    }

    private fun resolveLocales(): Pair<String, String> {
        if (session.origin == WKSpeechSession.Origin.APP) {
            return TranslateTranscript.appOriginalLocale to TranslateTranscript.appTranslateLocale
        }
        val lang = speechAi.translate.getLang() ?: WKTranslateLang.defaultFromSystemLocale()
        val source = deviceLangToLocale(lang.source)
        val target = deviceLangToLocale(lang.target)
        if (source != null && target != null) {
            return source to target
        }
        Timber.tag(tag).w(
            "unsupported lang pair source=0x%02X target=0x%02X, fallback zh-CN -> en-US",
            lang.source, lang.target,
        )
        return "zh-CN" to "en-US"
    }

    private fun startAsr() {
        val source = bindAudioSource(
            onAudioStop = {
                Timber.tag(tag).i("audio stop → UI follow session")
                TranslateTranscript.onSessionEnded()
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
                        TranslateTranscript.onSourceText(result.text, result.isComplete, result.index)
                        speechAi.translate
                            .sendTextSource(result.text, result.isComplete)
                            .onErrorComplete().subscribe()
                    }
                    is AiAsrResult.TranslateText -> {
                        Timber.tag(tag).i(
                            "target[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete,
                        )
                        TranslateTranscript.onTargetText(result.text, result.isComplete, result.index)
                        speechAi.translate
                            .sendTextTarget(result.text, result.isComplete)
                            .onErrorComplete().subscribe()
                    }
                    is AiAsrResult.TranslateTts -> {
                        if (!result.isComplete) {
                            ttsController.writePcm(result.bytes)
                        } else {
                            val file = ttsController.complete()
                            Timber.tag(tag).i("tts complete file=%s", file?.absolutePath)
                            speechAi.translate
                                .sendTtsReady()
                                .onErrorComplete().subscribe()
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

    override fun onMessage(msg: WKSpeechAiMessage) {
        when (msg.type) {
            WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE -> {
                val state = msg.data as? WKTranslatePlayerState
                if (state == null) {
                    Timber.tag(tag).w("TRANSLATE_PLAYER_STATE invalid data: %s", msg.data)
                    return
                }
                ttsController.applyPlayerState(state)
                TranslateTranscript.onPlayerState(state)
            }
        }
    }

    override fun onRelease() {
        ttsController.release(stopPlayer = !allowTtsDrain)
        TranslateTranscript.onSessionEnded()
    }

    companion object {
        /** 0x01/0x02 → 中文，0x03 → 英文；其它不支持。 */
        fun deviceLangToLocale(code: Byte): String? {
            return when (code.toInt() and 0xFF) {
                WKTranslateLang.LANG_ZH.toInt() and 0xFF, 0x02 -> "zh-CN"
                WKTranslateLang.LANG_EN.toInt() and 0xFF -> "en-US"
                else -> null
            }
        }
    }
}
