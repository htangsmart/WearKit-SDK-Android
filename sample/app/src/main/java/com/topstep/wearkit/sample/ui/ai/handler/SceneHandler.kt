package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechAiMessage
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import com.topstep.wearkit.sample.ui.ai.SessionAudioSource
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 单个场景的一次性 Handler：随 session 创建，在 [SCENE_EXIT] / 出错 / 结束时 [release]。
 */
abstract class SceneHandler(
    protected val context: Context,
    protected val speechAi: FcSpeechAiAbility,
    protected val aiKit: AiKit,
    protected val session: FcSpeechSession,
    private val onReleased: () -> Unit,
) {

    protected abstract val scene: FcSpeechSession.Scene
    protected abstract val tag: String

    private val released = AtomicBoolean(false)
    protected val disposables = CompositeDisposable()
    private var audioSource: SessionAudioSource? = null

    fun start() {
        disposables.add(
            speechAi.observeMessage().subscribe({ msg ->
                if (msg.type == FcSpeechAiMessage.Type.SCENE_EXIT) {
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

    /** 本场景专属设备消息（不含 [FcSpeechAiMessage.Type.SCENE_EXIT]）。 */
    protected open fun onMessage(msg: FcSpeechAiMessage) {}

    protected fun bindAudioSource(): SessionAudioSource {
        audioSource?.stop()
        return SessionAudioSource(context, session).also { audioSource = it }
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
        }.onFailure {
            Timber.tag(tag).w(it, "release failed")
        }
        onReleased()
    }

    protected open fun onRelease() {}
}
