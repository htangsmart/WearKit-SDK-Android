package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.audio.AiAudioSource
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import io.reactivex.rxjava3.disposables.Disposable

/**
 * 将手表会话音频转发给 AiKit。
 *
 * 注意：[WKSpeechSession.format] 仅在订阅 [WKSpeechSession.audio] 后才有值，
 * [getFormat] 会在首帧非空数据时由 [AiAudioSource] 调用，勿在订阅前访问。
 *
 * @param onAudioStart 收到首帧音频时回调一次（用于 UI 开始计时等）。
 * @param onAudioStop 音频流结束（complete / error / 主动 [stop]）时回调一次。
 */
class SessionAudioSource(
    context: Context,
    val session: WKSpeechSession,
    private val onAudioStart: (() -> Unit)? = null,
    private val onAudioStop: ((Throwable?) -> Unit)? = null,
) : AiAudioSource(context) {

    private var audioDisposable: Disposable? = null
    private val saveWavForDebug = SaveWavForDebug(context)
    private var debugStarted = false
    private var startNotified = false
    private var stopNotified = false

    override fun getFormat(): AiAudioFormat {
        val f = session.format
            ?: error("session.format is null; audio() must be subscribed first")
        return when (f) {
            WKSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is WKSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(f.frameSize)
        }
    }

    override fun onStart() {
        super.onStart()
        // 尽快订阅 audio()，否则设备会话会因超时自动 release
        audioDisposable = session.audio().subscribe({ data ->
            if (!startNotified) {
                startNotified = true
                onAudioStart?.invoke()
            }
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

    private fun notifyAudioStop(error: Throwable?) {
        if (stopNotified) return
        stopNotified = true
        onAudioStop?.invoke(error)
    }

    override fun onStop(throwable: Throwable?) {
        super.onStop(throwable)
        audioDisposable?.dispose()
        audioDisposable = null
        if (debugStarted) {
            saveWavForDebug.finish()
            debugStarted = false
        }
        // dispose 之后 session 已自行 release，再通知业务
        notifyAudioStop(throwable)
    }
}
