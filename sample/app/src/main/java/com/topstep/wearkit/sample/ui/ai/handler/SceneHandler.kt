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
 * - 收到匹配本场景的 [WKSpeechAiMessage.Type.SCENE_EXIT]（基类统一处理；APP 会话见 [releaseOnAppSceneExit]）
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

    /**
     * APP 发起的会话收到本场景 SCENE_EXIT 时是否 [release]。
     *
     * SCENE_EXIT 是全局消息，不带会话轮次：手表上一轮迟到的退出会误杀 APP 新开的一轮。
     * SDK 收到 EXIT 时已结束当前会话的音频流，采集收尾会走 onAudioStop。
     * 自带放弃兜底的场景（如 ASK）可覆写为 false，保住已在生成中的回答。
     */
    protected open val releaseOnAppSceneExit: Boolean = true

    private val released = AtomicBoolean(false)
    protected val disposables = CompositeDisposable()
    private var audioSource: SessionAudioSource? = null

    fun start() {
        disposables.add(
            speechAi.observeMessage().subscribe({ msg ->
                if (msg.type == WKSpeechAiMessage.Type.SCENE_EXIT) {
                    if (msg.data == scene) {
                        if (session.origin == WKSpeechSession.Origin.APP && !releaseOnAppSceneExit) {
                            Timber.tag(tag).i("SCENE_EXIT ignored: APP session")
                        } else {
                            Timber.tag(tag).i("SCENE_EXIT")
                            release()
                        }
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
     * @param onAudioStart 收到首帧音频时回调一次。
     * @param onAudioStop 音频流结束回调；返回 true 则 [release]，false 则仅处理业务不 release。
     */
    protected fun bindAudioSource(
        onAudioStart: (() -> Unit)? = null,
        onAudioStop: ((Throwable?) -> Boolean)? = null,
    ): SessionAudioSource {
        audioSource?.requestStop()
        return SessionAudioSource(
            context = context,
            session = session,
            onAudioStart = onAudioStart,
            onAudioStop = onAudioStop?.let { callback ->
                { err ->
                    if (callback(err)) {
                        if (err != null) {
                            Timber.tag(tag).w(err, "audio stop → release")
                        } else {
                            Timber.tag(tag).i("audio complete → release")
                        }
                        release()
                    }
                }
            },
        ).also { audioSource = it }
    }

    /**
     * 结束采集。`audio()` dispose 会使 session 自行 [WKSpeechSession.release]；
     * 不释放 Handler。可重复调用。早于 AiKit 启动音频源调用也有效（见 [SessionAudioSource.requestStop]）。
     */
    fun stopAudio() {
        audioSource?.requestStop()
    }

    /**
     * 销毁本场景资源。可重复调用，仅首次生效。
     */
    fun release() {
        if (!released.compareAndSet(false, true)) return
        runCatching {
            audioSource?.requestStop()
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
