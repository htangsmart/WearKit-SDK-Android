package com.topstep.wearkit.sample.ui.ai.translate

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslateLang
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.TRANSLATE]
 *
 * - 调 AiKit ASR/翻译，原文/译文回传设备并写入 [TranslateTranscript]
 * - TTS 默认手机外放，PCM 经 [MyAudioPlayer] 直接播放（不做落盘/回放）
 * - APP 语言来自 [TranslateTranscript]；设备语言来自 [WKSpeechAiAbility.Translate.getLang]
 *
 * 本 Sample 不响应 [com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE]。
 * 若要对齐设备播控，可在 [onMessage] 里按 [com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState] 驱动
 * [MyAudioPlayer]，并视需要 [TranslateTranscript.onPlayerState] 刷新 UI：
 * - START：未在播则 [MyAudioPlayer.start]（若有落盘/缓存可再喂入）；已在播则 [MyAudioPlayer.resume]
 * - STOP：[MyAudioPlayer.stop]
 * - PAUSE：[MyAudioPlayer.pause]
 * - RESUME：[MyAudioPlayer.resume]
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
            "start origin=%s source=%s %s -> %s",
            session.origin, session.source, originalLocale, translateLocale,
        )
        TranslateTranscript.onSessionStarted(session, originalLocale, translateLocale)
        if (session.source == WKSpeechSession.Source.PHONE_MIC) {
            // 录音前切通话路由，便于系统 AEC；TTS 仍默认手机外放
            MyAudioPlayer.activate(WKSpeechSession.Source.PHONE_MIC)
        }
        startAsr()
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
                            if (result.bytes.isNotEmpty()) {
                                ensureTtsStarted()
                                MyAudioPlayer.sendData(result.bytes.copyOf())
                            }
                        } else {
                            if (MyAudioPlayer.isStarted()) {
                                MyAudioPlayer.sendFinish()
                            }
                            Timber.tag(tag).i("tts complete")
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

    /** 默认手机外放；非 PHONE_MIC 会话若尚未 activate，这里补上。 */
    private fun ensureTtsStarted() {
        if (!MyAudioPlayer.isStarted()) {
            MyAudioPlayer.activate(WKSpeechSession.Source.PHONE_MIC)
            MyAudioPlayer.start()
        }
    }

    override fun onRelease() {
        if (!allowTtsDrain) {
            MyAudioPlayer.deactivate()
        }
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
