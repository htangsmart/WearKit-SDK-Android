package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber

/**
 * 订阅设备 session，按场景**即时创建**对应 [SceneHandler]，由其自行处理消息与释放。
 *
 * 另支持 APP 主动 [startAppRecord]（[WKSpeechAiAbility.Session.createAppSession]）。
 */
class SpeechAiHandler(
    private val context: Context,
    private val speechAi: WKSpeechAiAbility,
    private val aiKit: AiKit,
    private val onRecordSaved: (() -> Unit)? = null,
    private val onAppRecordEnded: (() -> Unit)? = null,
    private val onAppRecordAudioStarted: (() -> Unit)? = null,
) {

    private var current: SceneHandler? = null
    private var appRecordActive = false
    private val disposables = CompositeDisposable()

    fun start() {
        disposables.clear()
        disposables.add(
            speechAi.session.observeDeviceSession().subscribe({ session ->
                if (current != null) {
                    Timber.w("old SceneHandler exit")
                }
                val wasAppRecord = appRecordActive
                appRecordActive = false
                current?.release()
                current = null
                if (wasAppRecord) {
                    onAppRecordEnded?.invoke()
                }

                val handler = createHandler(session)
                current = handler
                handler.start()
            }, {
                Timber.w(it, "observeDeviceSession error")
            })
        )
    }

    /**
     * APP 发起 [WKSpeechSession.Scene.RECORD]。
     * @return false：不支持 / 已有会话 / create 失败
     */
    fun startAppRecord(): Boolean {
        if (current != null || appRecordActive) {
            Timber.w("startAppRecord fail: busy")
            return false
        }
        if (!speechAi.session.isSupportAppScene(WKSpeechSession.Scene.RECORD)) {
            Timber.w("startAppRecord fail: unsupported")
            return false
        }
        val session = speechAi.session.createAppSession(WKSpeechSession.Scene.RECORD)
        if (session == null) {
            Timber.w("startAppRecord fail: createAppSession null")
            return false
        }
        appRecordActive = true
        val handler = createHandler(session)
        current = handler
        handler.start()
        return true
    }

    /** 结束 APP 发起的录音会话。 */
    fun stopAppRecord() {
        if (!appRecordActive) return
        current?.release()
    }

    fun isAppRecording(): Boolean = appRecordActive

    private fun createHandler(session: WKSpeechSession): SceneHandler {
        lateinit var handler: SceneHandler
        val clearIfMine: () -> Unit = {
            if (current === handler) {
                current = null
            }
            if (appRecordActive && session.origin == WKSpeechSession.Origin.APP) {
                appRecordActive = false
                onAppRecordEnded?.invoke()
            }
        }
        val isAppRecord = session.origin == WKSpeechSession.Origin.APP &&
                (session.scene == WKSpeechSession.Scene.RECORD ||
                        session.scene == WKSpeechSession.Scene.CALL_RECORD)
        handler = when (session.scene) {
            WKSpeechSession.Scene.CHAT ->
                ChatHandler(context, speechAi, aiKit, session, clearIfMine)

            WKSpeechSession.Scene.RECORD,
            WKSpeechSession.Scene.CALL_RECORD,
                -> RecordHandler(
                context, speechAi, aiKit, session, clearIfMine, onRecordSaved,
                onAudioStarted = if (isAppRecord) onAppRecordAudioStarted else null,
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
        val wasAppRecord = appRecordActive
        appRecordActive = false
        current?.release()
        current = null
        if (wasAppRecord) {
            onAppRecordEnded?.invoke()
        }
    }
}
