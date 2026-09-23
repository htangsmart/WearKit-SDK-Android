package com.topstep.wearkit.sample.ui.ai

import android.annotation.SuppressLint
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.starburst.StarBurstKit
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.WKThirdPartyData
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.BuildConfig
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager._activeSession
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager.startAiKit
import com.topstep.wearkit.sample.ui.ai.ask.AskHandler
import com.topstep.wearkit.sample.ui.ai.chat.ChatHandler
import com.topstep.wearkit.sample.ui.ai.chattranslate.ChatTranslateHandler
import com.topstep.wearkit.sample.ui.ai.debug.DebugScoStorage
import com.topstep.wearkit.sample.ui.ai.handler.DialHandler
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import com.topstep.wearkit.sample.ui.ai.handler.TaxiHandler
import com.topstep.wearkit.sample.ui.ai.record.RecordHandler
import com.topstep.wearkit.sample.ui.ai.translate.TranslateHandler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
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
    private val wearKit: WKWearKit
        get() = MyApplication.wearKit
    private val speechAi: WKSpeechAiAbility
        get() = wearKit.speechAiAbility

    private val disposables = CompositeDisposable()
    private var current: SceneHandler? = null

    @Volatile
    private var initGeneration = 0
    private var sessionObserving = false
    private var connectionObserving = false
    private var thirdPartyDisposable: Disposable? = null

    @Volatile
    private var headsetProxy: BluetoothHeadset? = null
    private var headsetProxyBound = false

    fun requireAiKit(): AiKit? = aiKit.takeIf { _state.value == State.READY }

    /**
     * 初始化 [SpeechAiManager]：开始监听 device session 和设备连接状态。
     * 设备连接后再初始化 [AiKit]；断开后释放，下次连接再初始化。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        startObserveDeviceSession()
        startObserveConnection()
        bindHeadsetProxy()
    }

    /**
     * APP 发起 session，并立刻挂上对应 [SceneHandler]。
     * 如果当前有激活的[_activeSession],则不重复创建。
     *
     * @return null：已有活跃会话 / 场景不支持 / SDK 拒绝创建 / AiKit 未就绪
     */
    fun createAppSession(scene: WKSpeechSession.Scene, source: WKSpeechSession.Source? = null): WKSpeechSession? {
        val live = _activeSession.value
        if (live?.isActive() == true || speechAi.session.activeSession() != null) {
            Timber.tag(TAG).w("createAppSession fail: busy scene=%s", live?.scene)
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
        if (_activeSession.value === session) {
            _activeSession.value = null
        }
    }

    /**
     * 只结束采集，让 session 自行 release；Handler 继续活着。
     * 用于对话翻译 / 问答长按松手。幂等。
     */
    fun endActiveCapture() {
        Timber.tag(TAG).i("end capture scene=%s", _activeSession.value?.scene)
        current?.stopAudio()
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

    private fun startObserveConnection() {
        if (connectionObserving) return
        connectionObserving = true
        disposables.add(
            wearKit.connector.observeConnectorState()
                .startWithItem(wearKit.connector.getConnectorState())
                .map { it == WKConnectorState.CONNECTED }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ connected ->
                    if (connected) {
                        startAiKit()
                    } else {
                        releaseAiKit()
                    }
                }, {
                    Timber.tag(TAG).w(it, "observeConnectorState error")
                    connectionObserving = false
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
        val adapter = wearKit.bluetoothAdapter ?: return
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
        val kit: AiKit = StarBurstKit(appContext)
        aiKit = kit
        thirdPartyDisposable?.dispose()
        thirdPartyDisposable = wearKit.deviceAbility.observeThirdPartyData()
            .filter { it.type == WKThirdPartyData.Type.STAR_BURST }
            .subscribe({
                kit.sendInitData(it.data)
            }, {
                Timber.tag(TAG).w(it, "observeThirdPartyData")
            })
        kit.init(
            params = AiKit.InitParams(
                channel = BuildConfig.AIKIT_CHANNEL,
                macAddress = BuildConfig.AIKIT_MAC_ADDRESS,
                customPrompt = BuildConfig.AIKIT_CUSTOM_PROMPT,
            ),
            handler = object : AiKit.InitHandler {
                override fun onInitFail() {
                    Timber.tag(TAG).w("AiKit init fail")
                    AndroidSchedulers.mainThread().scheduleDirect {
                        if (generation != initGeneration) return@scheduleDirect
                        clearThirdPartyBridge()
                        aiKit = null
                        _state.value = State.FAILED
                        kit.release()
                    }
                }

                override fun onInitSuccess() {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        if (generation != initGeneration) {
                            kit.release()
                            return@scheduleDirect
                        }
                        Timber.tag(TAG).i("AiKit init success")
                        _state.value = State.READY
                    }
                }

                @SuppressLint("CheckResult")
                override fun receiveInitData(bytes: ByteArray) {
                    if (generation != initGeneration) return
                    wearKit.deviceAbility.sendThirdPartyData(
                        WKThirdPartyData(
                            type = WKThirdPartyData.Type.STAR_BURST,
                            data = bytes,
                        )
                    ).subscribe({}, {
                        Timber.tag(TAG).w(it, "sendThirdPartyData")
                    })
                }
            },
        )
    }

    /** 断开连接时释放当前 kit，下次连接由 [startAiKit] 重新握手。 */
    private fun releaseAiKit() {
        val kit = aiKit ?: return
        initGeneration++
        clearThirdPartyBridge()
        aiKit = null
        _state.value = State.IDLE
        kit.release()
    }

    private fun clearThirdPartyBridge() {
        thirdPartyDisposable?.dispose()
        thirdPartyDisposable = null
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
