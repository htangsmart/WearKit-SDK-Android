package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import com.topstep.wearkit.sample.ui.ai.wav.SpeechRecordSaver
import timber.log.Timber

/**
 * [FcSpeechSession.Scene.RECORD] / [FcSpeechSession.Scene.CALL_RECORD]：接收音频并保存为 wav。
 */
class RecordHandler(
    context: Context,
    speechAi: FcSpeechAiAbility,
    aiKit: AiKit,
    session: FcSpeechSession,
    onReleased: () -> Unit,
    private val onRecordSaved: (() -> Unit)? = null,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = session.scene
    override val tag = "RecordHandler"

    private var saver: SpeechRecordSaver? = null
    private var finished = false

    override fun onStart() {
        Timber.tag(tag).i("start scene=%s", scene)
        disposables.add(
            session.audio().subscribe({ data ->
                ensureSaverStarted()
                saver?.write(data)
            }, {
                Timber.tag(tag).w(it, "audio error")
                finishSaver()
                release()
            }, {
                Timber.tag(tag).i("audio complete")
                finishSaver()
                release()
            })
        )
    }

    private fun ensureSaverStarted() {
        if (saver != null) return
        val format = session.format?.toAiAudioFormat() ?: return
        val next = SpeechRecordSaver(context, scene)
        if (!next.start(format)) {
            session.release(FcSpeechSession.Reason.ERROR_STORAGE)
            release()
            return
        }
        saver = next
    }

    private fun finishSaver() {
        if (finished) return
        finished = true
        val file = saver?.finish()
        saver = null
        if (file != null) {
            onRecordSaved?.invoke()
        }
    }

    override fun onRelease() {
        finishSaver()
    }

    private fun FcSpeechSession.Format.toAiAudioFormat(): AiAudioFormat {
        return when (this) {
            FcSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is FcSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(frameSize)
        }
    }
}
