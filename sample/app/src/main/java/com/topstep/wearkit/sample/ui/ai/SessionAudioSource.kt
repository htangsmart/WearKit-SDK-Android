package com.topstep.wearkit.sample.ui.ai

import android.content.Context
import com.topstep.aikit.audio.AiAudioSource
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import io.reactivex.rxjava3.disposables.Disposable

/**
 * 将手表会话音频转发给 AiKit。
 *
 * 注意：[FcSpeechSession.format] 仅在订阅 [FcSpeechSession.audio] 后才有值，
 * [getFormat] 会在首帧非空数据时由 [AiAudioSource] 调用，勿在订阅前访问。
 */
class SessionAudioSource(context: Context, val session: FcSpeechSession) : AiAudioSource(context) {

    private var audioDisposable: Disposable? = null
    private val saveWavForDebug = SaveWavForDebug(context)
    private var debugStarted = false

    override fun getFormat(): AiAudioFormat {
        val f = session.format
            ?: error("session.format is null; audio() must be subscribed first")
        return when (f) {
            FcSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is FcSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(f.frameSize)
        }
    }

    override fun onStart() {
        super.onStart()
        // 尽快订阅 audio()，否则设备会话会因超时自动 release
        audioDisposable = session.audio().subscribe({ data ->
            if (!debugStarted) {
                debugStarted = true
                saveWavForDebug.start(getFormat())
            }
            saveWavForDebug.write(data)
            sendData(data)
        }, {
            stop(it)
        }, {
            stop()
        })
    }

    override fun onStop(throwable: Throwable?) {
        super.onStop(throwable)
        audioDisposable?.dispose()
        audioDisposable = null
        if (debugStarted) {
            saveWavForDebug.finish()
            debugStarted = false
        }
    }
}
