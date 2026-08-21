package com.topstep.wearkit.sample.ui.ai

import android.annotation.SuppressLint
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.eyeear.EyeEarKit
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.BuildConfig
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.ui.ai.chat.ChatHandler
import com.topstep.wearkit.sample.ui.ai.chattranslate.ChatTranslateHandler
import com.topstep.wearkit.sample.ui.ai.debug.DebugScoStorage
import com.topstep.wearkit.sample.ui.ai.handler.*
import com.topstep.wearkit.sample.ui.ai.record.RecordHandler
import com.topstep.wearkit.sample.ui.ai.translate.TranslateHandler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * 进程级 SpeechAi 入口：在 [com.topstep.wearkit.sample.MyApplication] 初始化。
 *
 * 1. 管理 [AiKit] 初始化与状态
 * 2. 全程订阅 [WKSpeechAiAbility.Session.observeDeviceSession]
 * 3. 提供 APP 发起 session 的入口
 * 4. 持有当前活跃 session / [SceneHandler]
 *
 */
object SpeechAiManager {

    private const val TAG = "SpeechAiManager"

    enum class State {
        IDLE,
        INITIALIZING,
        READY,
        FAILED,
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _activeSession = MutableStateFlow<WKSpeechSession?>(null)
    val activeSession: StateFlow<WKSpeechSession?> = _activeSession

    @Volatile
    var aiKit: AiKit? = null
        private set

    private lateinit var appContext: Context
    private val speechAi: WKSpeechAiAbility
        get() = MyApplication.wearKit.speechAiAbility

    private val disposables = CompositeDisposable()
    private var current: SceneHandler? = null
    private var initGeneration = 0
    private var sessionObserving = false

    @Volatile
    private var headsetProxy: BluetoothHeadset? = null
    private var headsetProxyBound = false

    fun requireAiKit(): AiKit? = aiKit.takeIf { _state.value == State.READY }

    /**
     * 幂等。首次调用开始监听 device session 并初始化 AiKit；
     * AiKit 处于 [State.IDLE] / [State.FAILED] 时再次调用会重试初始化。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        startObserveDeviceSession()
        bindHeadsetProxy()
        startAiKit()
    }

    /**
     * APP 发起 session，并立刻挂上对应 [SceneHandler]。
     * @return null：已有活跃会话 / 场景不支持 / SDK 拒绝创建 / AiKit 未就绪
     */
    fun createAppSession(scene: WKSpeechSession.Scene, source: WKSpeechSession.Source? = null): WKSpeechSession? {
        if (current != null || _activeSession.value != null) {
            Timber.tag(TAG).w("createAppSession fail: busy scene=%s", _activeSession.value?.scene)
            return null
        }
        if (!speechAi.session.isSupportAppScene(scene)) {
            Timber.tag(TAG).w("createAppSession fail: unsupported scene=%s", scene)
            return null
        }
        val session = if (source == null) {
            speechAi.session.createAppSession(scene)
        } else {
            speechAi.session.createAppSession(scene, source)
        }
        if (session == null) {
            Timber.tag(TAG).w("createAppSession fail: sdk null scene=%s source=%s", scene, source)
            return null
        }
        attachSession(session)
        return if (current != null) session else null
    }

    /** 结束当前会话（Handler + [WKSpeechSession]）。幂等。 */
    fun stopActiveSession() {
        val handler = current
        val session = _activeSession.value
        if (handler == null && session == null) return
        Timber.tag(TAG).i("stop session scene=%s", session?.scene)
        handler?.release()
        session?.release()
    }

    private fun startObserveDeviceSession() {
        if (sessionObserving) return
        sessionObserving = true
        disposables.add(
            speechAi.session.observeDeviceSession().subscribe({ session ->
                Timber.tag(TAG).i("device session scene=%s source=%s", session.scene, session.source)
                attachSession(session)
            }, {
                Timber.tag(TAG).w(it, "observeDeviceSession error")
                sessionObserving = false
            })
        )
    }

    private fun attachSession(session: WKSpeechSession) {
        val previous = current
        if (previous != null) {
            Timber.tag(TAG).w("replace session handler scene=%s", _activeSession.value?.scene)
            previous.release()
        }
        val kit = requireAiKit()
        if (kit == null) {
            Timber.tag(TAG).w("drop session: AiKit not ready scene=%s", session.scene)
            session.release()
            return
        }
        lateinit var handler: SceneHandler
        handler = createHandler(session, kit) {
            if (current === handler) {
                current = null
                if (_activeSession.value === session) {
                    _activeSession.value = null
                }
            }
        }
        current = handler
        _activeSession.value = session
        handler.start()
    }

    private fun createHandler(
        session: WKSpeechSession,
        aiKit: AiKit,
        onReleased: () -> Unit,
    ): SceneHandler {
        return when (session.scene) {
            WKSpeechSession.Scene.CHAT ->
                ChatHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.RECORD,
            WKSpeechSession.Scene.CALL_RECORD,
                -> RecordHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.TRANSLATE ->
                TranslateHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.TAXI ->
                TaxiHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.DIAL ->
                DialHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.ASK ->
                AskHandler(appContext, speechAi, aiKit, session, onReleased)

            WKSpeechSession.Scene.CHAT_TRANSLATE_SELF,
            WKSpeechSession.Scene.CHAT_TRANSLATE_PEER,
                -> ChatTranslateHandler(appContext, speechAi, aiKit, session, onReleased)
        }
    }

    /**
     * [android.bluetooth.BluetoothManager] 只支持 GATT，HEADSET 必须走 [android.bluetooth.BluetoothAdapter.getProfileProxy]。
     */
    private fun bindHeadsetProxy() {
        if (headsetProxyBound) return
        val adapter = MyApplication.wearKit.bluetoothAdapter ?: return
        headsetProxyBound = adapter.getProfileProxy(
            appContext,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    if (profile == BluetoothProfile.HEADSET) {
                        headsetProxy = proxy as? BluetoothHeadset
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HEADSET) {
                        headsetProxy = null
                    }
                }
            },
            BluetoothProfile.HEADSET,
        )
        if (!headsetProxyBound) {
            Timber.tag(TAG).w("getProfileProxy HEADSET failed")
        }
    }

    @SuppressLint("MissingPermission")
    internal fun isHeadsetProfileConnected(device: android.bluetooth.BluetoothDevice): Boolean {
        return try {
            headsetProxy?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
        } catch (_: SecurityException) {
            false
        }
    }

    private fun startAiKit() {
        if (_state.value == State.INITIALIZING || _state.value == State.READY) return
        val generation = ++initGeneration
        _state.value = State.INITIALIZING
        val kit = EyeEarKit(appContext)
        kit.init(
            params = AiKit.InitParams(
                channel = BuildConfig.AIKIT_CHANNEL,
                macAddress = BuildConfig.AIKIT_MAC_ADDRESS,
                customPrompt = BuildConfig.AIKIT_CUSTOM_PROMPT,
            ),
            handler = object : AiKit.InitHandler {
                override fun onInitFail() {
                    Timber.tag(TAG).w("AiKit init fail")
                    kit.release()
                    if (generation != initGeneration) return
                    aiKit = null
                    _state.value = State.FAILED
                }

                override fun onInitSuccess() {
                    if (generation != initGeneration) {
                        kit.release()
                        return
                    }
                    Timber.tag(TAG).i("AiKit init success")
                    aiKit = kit
                    _state.value = State.READY
                }

                override fun receiveInitData(bytes: ByteArray) {
                    // do nothing
                }
            },
        )
    }

}

fun WKWearKit.isDeviceConnected(): Boolean {
    return connector.getConnectorState() == WKConnectorState.CONNECTED
}

fun WKWearKit.isScoConnected(): Boolean {
    val adapter = bluetoothAdapter ?: return false
    val debug = DebugScoStorage.get(MyApplication.instance)
    val device = if (debug != null) {
        runCatching { adapter.getRemoteDevice(debug.address) }.getOrNull()
    } else {
        connector.getDeviceCanBond()
    } ?: return false
    return SpeechAiManager.isHeadsetProfileConnected(device)
}
