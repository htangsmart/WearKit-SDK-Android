package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.wav.SpeechRecordSaver
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.RECORD] / [WKSpeechSession.Scene.CALL_RECORD]：接收音频并保存为 wav。
 */
class RecordHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
    private val onRecordSaved: (() -> Unit)? = null,
    private val onAudioStarted: (() -> Unit)? = null,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = session.scene
    override val tag = "RecordHandler"

    private var saver: SpeechRecordSaver? = null
    private var finished = false
    private var audioStartedNotified = false

    override fun onStart() {
        Timber.tag(tag).i("start scene=%s", scene)
        disposables.add(
            session.audio().subscribe({ data ->
                notifyAudioStartedOnce()
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

    private fun notifyAudioStartedOnce() {
        if (audioStartedNotified) return
        audioStartedNotified = true
        onAudioStarted?.invoke()
    }

    private fun ensureSaverStarted() {
        if (saver != null) return
        val format = session.format?.toAiAudioFormat() ?: return
        val next = SpeechRecordSaver(context, scene)
        if (!next.start(format)) {
            session.release(WKSpeechSession.Reason.ERROR_STORAGE)
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

    private fun WKSpeechSession.Format.toAiAudioFormat(): AiAudioFormat {
        return when (this) {
            WKSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is WKSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(frameSize)
        }
    }
}
