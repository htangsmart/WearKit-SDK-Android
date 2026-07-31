package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.special.taxi.FcTaxiInfo
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import io.reactivex.rxjava3.core.Completable
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * [WKSpeechSession.Scene.TAXI]：接收语音后模拟 AI 识别，再 [setTaxiInfo] 下发假数据。
 */
class TaxiHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = WKSpeechSession.Scene.TAXI
    override val tag = "TaxiHandler"

    private val saveWavForDebug = SaveWavForDebug(context)
    private var debugStarted = false

    override fun onStart() {
        Timber.tag(tag).i("start, wait audio then fake AI")
        // 尽快订阅 audio()，避免会话超时；本场景用假 AI，不调 AiKit
        disposables.add(
            session.audio().subscribe({ data ->
                if (!debugStarted) {
                    debugStarted = true
                    saveWavForDebug.start(sessionFormat())
                }
                saveWavForDebug.write(data)
            }, {
                Timber.tag(tag).w(it, "audio error")
                finishDebugSave()
                release()
            }, {
                Timber.tag(tag).i("audio complete, simulate AI")
                finishDebugSave()
                simulateAiAndSetTaxiInfo()
            })
        )
    }

    private fun sessionFormat(): AiAudioFormat {
        return when (val f = session.format!!) {
            WKSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is WKSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(f.frameSize)
        }
    }

    private fun finishDebugSave() {
        if (!debugStarted) return
        debugStarted = false
        saveWavForDebug.finish()
    }

    /**
     * 模拟一次 AI 打车意图识别，然后把订单确认页推到设备。
     */
    private fun simulateAiAndSetTaxiInfo() {
        val fcSDK = MyApplication.wearKit.getRawSDK() as? FcSDK
        if (fcSDK == null) {
            Timber.tag(tag).e("raw SDK is not FcSDK, cannot setTaxiInfo")
            return
        }

        // 假识别结果
        val fakeAsr = "帮我打车去深圳前海壹方城"
        Timber.tag(tag).i("fake ASR: %s", fakeAsr)

        val fakeInfo = FcTaxiInfo.OrderConfirm(
            origin = "半里花汇",
            destination = "深圳前海壹方城",
            carType = "快车",
            estimatedPrice = "22元",
            estimatedPickupMinutes = "4分钟",
        )
        Timber.tag(tag).i(
            "fake taxi info: %s -> %s, %s, %s, %s",
            fakeInfo.origin,
            fakeInfo.destination,
            fakeInfo.carType,
            fakeInfo.estimatedPrice,
            fakeInfo.estimatedPickupMinutes,
        )

        // 模拟 AI 耗时，再下发
        disposables.add(
            Completable.timer(800, TimeUnit.MILLISECONDS)
                .andThen(fcSDK.connector.specialFeature().setTaxiInfo(fakeInfo))
                .subscribe({
                    Timber.tag(tag).i("setTaxiInfo success")
                }, {
                    Timber.tag(tag).w(it, "setTaxiInfo failed")
                })
        )
    }

    override fun onRelease() {
        finishDebugSave()
    }
}
