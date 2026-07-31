package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber

/**
 * 订阅设备 session，按场景**即时创建**对应 [SceneHandler]，由其自行处理消息与释放。
 */
class SpeechAiHandler(
    private val context: Context,
    private val speechAi: WKSpeechAiAbility,
    private val aiKit: AiKit,
    private val onRecordSaved: (() -> Unit)? = null,
) {

    private var current: SceneHandler? = null
    private val disposables = CompositeDisposable()

    fun start() {
        disposables.clear()
        disposables.add(
            speechAi.session.observeDeviceSession().subscribe({ session ->
                if (current != null) {
                    Timber.w("old SceneHandler exit")
                }
                current?.release()
                current = null

                val handler = createHandler(session)
                current = handler
                handler.start()
            }, {
                Timber.w(it, "observeDeviceSession error")
            })
        )
    }

    private fun createHandler(session: WKSpeechSession): SceneHandler {
        lateinit var handler: SceneHandler
        val clearIfMine: () -> Unit = {
            if (current === handler) {
                current = null
            }
        }
        handler = when (session.scene) {
            WKSpeechSession.Scene.CHAT ->
                ChatHandler(context, speechAi, aiKit, session, clearIfMine)

            WKSpeechSession.Scene.RECORD,
            WKSpeechSession.Scene.CALL_RECORD,
                -> RecordHandler(
                context, speechAi, aiKit, session, clearIfMine, onRecordSaved
            )

            WKSpeechSession.Scene.TRANSLATE ->
                TranslateHandler(context, speechAi, aiKit, session, clearIfMine)

            WKSpeechSession.Scene.TAXI ->
                TaxiHandler(context, speechAi, aiKit, session, clearIfMine)

            WKSpeechSession.Scene.DIAL ->
                DialHandler(context, speechAi, aiKit, session, clearIfMine)

            WKSpeechSession.Scene.ASK ->
                AskHandler(context, speechAi, aiKit, session, clearIfMine)
        }
        return handler
    }

    fun release() {
        disposables.clear()
        current?.release()
        current = null
    }
}
