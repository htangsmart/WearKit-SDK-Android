package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechAiMessage
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import com.topstep.fitcloud.sdk.model.speech.FcTranslateLang
import com.topstep.fitcloud.sdk.model.speech.FcTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.TranslateTtsController
import timber.log.Timber

/**
 * [FcSpeechSession.Scene.TRANSLATE]
 *
 * - 调 AiKit ASR/翻译，原文/译文回传设备
 * - 保存并播放 TranslateTts PCM
 * - 启动时用 [FcSpeechAiAbility.Translate.getLang] 取语言（仅中/英）
 * - 处理 [TRANSLATE_PLAYER_STATE]
 */
class TranslateHandler(
    context: Context,
    speechAi: FcSpeechAiAbility,
    aiKit: AiKit,
    session: FcSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = FcSpeechSession.Scene.TRANSLATE
    override val tag = "TranslateHandler"

    private val ttsController = TranslateTtsController(context)

    private val originalLocale: String
    private val translateLocale: String

    init {
        val lang = speechAi.translate.getLang() ?: FcTranslateLang.defaultFromSystemLocale()
        val source = deviceLangToLocale(lang.source)
        val target = deviceLangToLocale(lang.target)
        if (source != null && target != null) {
            originalLocale = source
            translateLocale = target
        } else {
            Timber.tag(tag).w(
                "unsupported lang pair source=0x%02X target=0x%02X, fallback zh-CN -> en-US",
                lang.source, lang.target
            )
            originalLocale = "zh-CN"
            translateLocale = "en-US"
        }
    }

    override fun onStart() {
        Timber.tag(tag).i("start %s -> %s", originalLocale, translateLocale)
        startAsr()
    }

    private fun startAsr() {
        val source = bindAudioSource()
        disposables.add(
            aiKit.audio.asr(
                source, AiAsrParams(
                    originalLocale = originalLocale,
                    translateLocale = translateLocale,
                    originalFileRequired = false,
                    translateTtsRequired = true,
                    autoStop = true,
                )
            ).subscribe({ result ->
                when (result) {
                    is AiAsrResult.OriginalText -> {
                        Timber.tag(tag).i(
                            "source[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete
                        )
                        speechAi.translate
                            .sendTextSource(result.text, result.isComplete)
                            .onErrorComplete().subscribe()
                    }
                    is AiAsrResult.TranslateText -> {
                        Timber.tag(tag).i(
                            "target[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete
                        )
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
                // 音频/识别出错：收尾 TTS，等 SCENE_EXIT 再整体释放也可；这里直接 release
                release()
            })
        )
    }

    override fun onMessage(msg: FcSpeechAiMessage) {
        when (msg.type) {
            FcSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE -> {
                val state = msg.data as? FcTranslatePlayerState
                if (state == null) {
                    Timber.tag(tag).w("TRANSLATE_PLAYER_STATE invalid data: %s", msg.data)
                    return
                }
                ttsController.applyPlayerState(state)
            }
        }
    }

    override fun onRelease() {
        ttsController.release()
    }

    companion object {
        /** 0x01/0x02 → 中文，0x03 → 英文；其它不支持。 */
        fun deviceLangToLocale(code: Byte): String? {
            return when (code.toInt() and 0xFF) {
                FcTranslateLang.LANG_ZH.toInt() and 0xFF, 0x02 -> "zh-CN"
                FcTranslateLang.LANG_EN.toInt() and 0xFF -> "en-US"
                else -> null
            }
        }
    }
}
