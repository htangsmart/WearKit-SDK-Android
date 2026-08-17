package com.topstep.wearkit.sample.ui.custom.sanag

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.handler.ChatHandler
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber

/**
 * Only handles device-initiated [WKSpeechSession.Scene.CHAT].
 */
internal class SanagSpeechAiHandler(
    private val context: Context,
    private val speechAi: WKSpeechAiAbility,
    private val aiKit: AiKit,
    private val onChatChanged: (Boolean) -> Unit,
) {

    private var current: SceneHandler? = null
    private val disposables = CompositeDisposable()

    fun start() {
        disposables.clear()
        disposables.add(
            speechAi.session.observeDeviceSession().subscribe({ session ->
                current?.release()
                current = null
                if (session.scene != WKSpeechSession.Scene.CHAT) {
                    Timber.w("ignore scene: %s", session.scene)
                    return@subscribe
                }
                lateinit var handler: SceneHandler
                handler = ChatHandler(
                    context = context,
                    speechAi = speechAi,
                    aiKit = aiKit,
                    session = session,
                    onReleased = {
                        if (current === handler) {
                            current = null
                            onChatChanged(false)
                        }
                    },
                )
                current = handler
                handler.start()
                onChatChanged(true)
            }, {
                Timber.w(it, "observeDeviceSession error")
            })
        )
    }

    fun release() {
        disposables.clear()
        current?.release()
        current = null
    }
}
