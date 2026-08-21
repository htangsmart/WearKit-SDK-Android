package com.topstep.wearkit.sample.ui.ai.record

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslateLang
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.RECORD] / [WKSpeechSession.Scene.CALL_RECORD]：ASR 并写入 [RecordTranscript]。
 */
class RecordHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = session.scene
    override val tag = "RecordHandler"

    override fun onStart() {
        val locale = resolveLocale()
        val localeLabel = if (locale == "en-US") "en" else "zh"
        Timber.tag(tag).i("start scene=%s origin=%s source=%s locale=%s", scene, session.origin, session.source, locale)
        RecordTranscript.onSessionStarted(session, localeLabel)

        val source = bindAudioSource(
            releaseOnAudioEnd = true,
            onFirstAudio = { RecordTranscript.onAudioStarted() },
        )
        disposables.add(
            aiKit.audio.asr(
                source,
                AiAsrParams(
                    originalLocale = locale,
                    translateLocale = null,
                    originalFileRequired = false,
                    translateTtsRequired = false,
                    autoStop = true,
                ),
            ).subscribe({ result ->
                when (result) {
                    is AiAsrResult.OriginalText -> {
                        Timber.tag(tag).i(
                            "asr[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete,
                        )
                        RecordTranscript.onAsrText(result.text, result.isComplete)
                    }
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it, "asr error")
                release()
            })
        )
    }

    private fun resolveLocale(): String {
        return when (session.origin) {
            WKSpeechSession.Origin.APP -> RecordTranscript.appLocale
            WKSpeechSession.Origin.DEVICE -> {
                when (speechAi.record.getLang()) {
                    null -> RecordTranscript.appLocale
                    WKTranslateLang.LANG_EN -> "en-US"
                    else -> "zh-CN"
                }
            }
        }
    }

    override fun onRelease() {
        RecordTranscript.onSessionEnded()
    }
}