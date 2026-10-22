package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.audio.AiAudioSource
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 将手表会话音频转发给 AiKit。
 *
 * 创建即订阅 [WKSpeechSession.audio]：SDK 的 APP 会话 3s、设备会话 5s 内未订阅会自动 release，
 * 不能等 AiKit 握手完成、调用 [start] 后再订阅。AiKit 启动前的音频先缓冲，[onStart] 时一并送出。
 * 订阅后 [WKSpeechSession.format] 即有值，[getFormat] 在 AiKit 任意时机调用都安全。
 *
 * 业务侧停止采集用 [requestStop]，不要直接调 [stop]（[stop] 在 AiKit 启动前调用会落空）。
 *
 * @param onAudioStart 收到首帧音频时回调一次（用于 UI 开始计时等）。
 * @param onAudioStop 采集结束（complete / error / [requestStop] / AiKit [stop]）时回调一次。
 */
class SessionAudioSource(
    context: Context,
    val session: WKSpeechSession,
    private val onAudioStart: (() -> Unit)? = null,
    private val onAudioStop: ((Throwable?) -> Unit)? = null,
) : AiAudioSource(context) {

    private val saveWavForDebug = SaveWavForDebug(context)
    private val stopNotified = AtomicBoolean(false)

    @Volatile
    private var startNotified = false

    // 以下状态由 lock 保护：音频在 SDK 读线程到达，onStart / onStop 在主线程，requestStop 可来自任意线程
    private val lock = Any()
    private val pending = ArrayDeque<ByteArray>()
    private var pendingBytes = 0
    private var droppedBytes = 0L
    private var debugStarted = false

    /** AiKit 已启动并完成缓冲冲刷，新数据直接 [sendData]。 */
    private var forwarding = false

    /** 采集已结束（会话结束 / 业务要求停止 / AiKit 停止），之后的数据丢弃。 */
    private var captureEnded = false
    private var endError: Throwable? = null

    private val audioDisposable: Disposable = session.audio().subscribe(
        { data -> onAudio(data) },
        { error -> onCaptureEnd(error) },
        { onCaptureEnd(null) },
    )

    /**
     * 业务侧停止采集，可重复调用，任意时机有效。
     *
     * 已转发给 AiKit：[stop]，AiKit 处理完已送出的数据后结束。
     * AiKit 尚未启动：只结束采集并通知业务，缓冲的音频在 [onStart] 时仍会送出，
     * 避免「按得短 / 网络慢」时整段话丢失。
     */
    fun requestStop() {
        // dispose 会让 SDK 会话自行 release，放掉唯一会话名额
        audioDisposable.dispose()
        onCaptureEnd(null)
    }

    override fun getFormat(): AiAudioFormat {
        val f = session.format
            ?: error("session.format is null; audio() must be subscribed first")
        return when (f) {
            WKSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is WKSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(f.frameSize)
        }
    }

    private fun onAudio(data: ByteArray) {
        if (!startNotified) {
            startNotified = true
            onAudioStart?.invoke()
        }
        synchronized(lock) {
            if (captureEnded) return
            if (!debugStarted) {
                debugStarted = true
                saveWavForDebug.start(getFormat())
            }
            saveWavForDebug.write(data)
            if (forwarding) {
                sendData(data)
            } else {
                buffer(data)
            }
        }
    }

    private fun buffer(data: ByteArray) {
        pending.addLast(data)
        pendingBytes += data.size
        while (pendingBytes > MAX_PENDING_BYTES && pending.isNotEmpty()) {
            val dropped = pending.removeFirst()
            pendingBytes -= dropped.size
            droppedBytes += dropped.size
        }
    }

    private fun onCaptureEnd(error: Throwable?) {
        val forwardingNow = synchronized(lock) {
            if (captureEnded) return
            captureEnded = true
            endError = error
            forwarding
        }
        if (forwardingNow) {
            // onStop 里通知业务
            stop(error)
        } else {
            // AiKit 未启动：先通知业务（UI 解锁 / 计时），缓冲留给 onStart
            notifyAudioStop(error)
        }
    }

    override fun onStart() {
        super.onStart()
        val endedError: Throwable?
        synchronized(lock) {
            if (!isStarted()) {
                // onStart 执行前 AiKit 已 stop，onStop 已处理
                return
            }
            if (droppedBytes > 0) {
                Timber.tag(TAG).w("buffer overflow, dropped %d bytes before AiKit start", droppedBytes)
            }
            Timber.tag(TAG).i("AiKit start, flush %d bytes, captureEnded=%s", pendingBytes, captureEnded)
            while (pending.isNotEmpty()) {
                sendData(pending.removeFirst())
            }
            pendingBytes = 0
            if (!captureEnded) {
                forwarding = true
                return
            }
            endedError = endError
        }
        // 采集在 AiKit 启动前已结束：缓冲已送出，随即结束
        stop(endedError)
    }

    private fun notifyAudioStop(error: Throwable?) {
        if (!stopNotified.compareAndSet(false, true)) return
        onAudioStop?.invoke(error)
    }

    override fun onStop(throwable: Throwable?) {
        super.onStop(throwable)
        audioDisposable.dispose()
        synchronized(lock) {
            captureEnded = true
            forwarding = false
            pending.clear()
            pendingBytes = 0
            if (debugStarted) {
                saveWavForDebug.finish()
                debugStarted = false
            }
        }
        // dispose 之后 session 已自行 release，再通知业务
        notifyAudioStop(throwable)
    }

    private companion object {
        const val TAG = "SessionAudioSource"

        /** AiKit 启动前最多缓冲约 32s 的 16k/16bit PCM，超出丢最旧的。 */
        const val MAX_PENDING_BYTES = 1024 * 1024
    }
}
