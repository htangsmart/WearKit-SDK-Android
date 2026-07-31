package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber

/**
 * 订阅设备 session，按场景**即时创建**对应 [SceneHandler]，由其自行处理消息与释放。
 */
class SpeechAiHandler(
    private val context: Context,
    private val speechAi: FcSpeechAiAbility,
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

    private fun createHandler(session: FcSpeechSession): SceneHandler {
        lateinit var handler: SceneHandler
        val clearIfMine: () -> Unit = {
            if (current === handler) {
                current = null
            }
        }
        handler = when (session.scene) {
            FcSpeechSession.Scene.CHAT ->
                ChatHandler(context, speechAi, aiKit, session, clearIfMine)

            FcSpeechSession.Scene.RECORD,
            FcSpeechSession.Scene.CALL_RECORD,
                -> RecordHandler(
                context, speechAi, aiKit, session, clearIfMine, onRecordSaved
            )

            FcSpeechSession.Scene.TRANSLATE ->
                TranslateHandler(context, speechAi, aiKit, session, clearIfMine)

            FcSpeechSession.Scene.TAXI ->
                TaxiHandler(context, speechAi, aiKit, session, clearIfMine)

            FcSpeechSession.Scene.DIAL ->
                DialHandler(context, speechAi, aiKit, session, clearIfMine)

            FcSpeechSession.Scene.ASK ->
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
