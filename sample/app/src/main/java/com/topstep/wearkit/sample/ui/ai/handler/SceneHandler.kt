package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 单个场景的一次性 Handler：随 session 创建。
 *
 * 何时 [release] 由各场景自行决定：
 * - 收到匹配本场景的 [WKSpeechAiMessage.Type.SCENE_EXIT]（基类统一处理）
 * - 业务出错
 * - 部分场景（如 CHAT）在音频流结束时主动 [release]（见 [bindAudioSource]）
 *
 */
abstract class SceneHandler(
    protected val context: Context,
    protected val speechAi: WKSpeechAiAbility,
    protected val aiKit: AiKit,
    protected val session: WKSpeechSession,
    private val onReleased: () -> Unit,
) {

    protected abstract val scene: WKSpeechSession.Scene
    protected abstract val tag: String

    private val released = AtomicBoolean(false)
    protected val disposables = CompositeDisposable()
    private var audioSource: SessionAudioSource? = null

    fun start() {
        disposables.add(
            speechAi.observeMessage().subscribe({ msg ->
                if (msg.type == WKSpeechAiMessage.Type.SCENE_EXIT) {
                    if (msg.data == scene) {
                        Timber.tag(tag).i("SCENE_EXIT")
                        release()
                    }
                    return@subscribe
                }
                onMessage(msg)
            }, {
                Timber.tag(tag).w(it, "observeMessage error")
                release()
            })
        )
        onStart()
    }

    /** 本场景业务启动（订阅音频、调 AiKit 等）。 */
    protected abstract fun onStart()

    /** 本场景专属设备消息（不含 [WKSpeechAiMessage.Type.SCENE_EXIT]）。 */
    protected open fun onMessage(msg: WKSpeechAiMessage) {}

    /**
     * @param releaseOnAudioEnd true：音频流 complete/error 时 [release]；
     * @param onFirstAudio 收到首帧音频时回调一次。
     */
    protected fun bindAudioSource(
        releaseOnAudioEnd: Boolean = false,
        onFirstAudio: (() -> Unit)? = null,
    ): SessionAudioSource {
        audioSource?.stop()
        return SessionAudioSource(
            context = context,
            session = session,
            onAudioEnded = if (releaseOnAudioEnd) {
                { err ->
                    if (err != null) {
                        Timber.tag(tag).w(err, "audio ended → release")
                    } else {
                        Timber.tag(tag).i("audio complete → release")
                    }
                    release()
                }
            } else {
                null
            },
            onFirstAudio = onFirstAudio,
        ).also { audioSource = it }
    }

    /**
     * 销毁本场景资源。可重复调用，仅首次生效。
     */
    fun release() {
        if (!released.compareAndSet(false, true)) return
        runCatching {
            audioSource?.stop()
            audioSource = null
            disposables.clear()
            onRelease()
            session.release()
        }.onFailure {
            Timber.tag(tag).w(it, "release failed")
        }
        onReleased()
    }

    protected open fun onRelease() {}
}
