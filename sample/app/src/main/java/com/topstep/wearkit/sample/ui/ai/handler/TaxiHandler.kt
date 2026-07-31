package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.special.taxi.FcTaxiInfo
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import io.reactivex.rxjava3.core.Completable
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * [FcSpeechSession.Scene.TAXI]：接收语音后模拟 AI 识别，再 [setTaxiInfo] 下发假数据。
 */
class TaxiHandler(
    context: Context,
    speechAi: FcSpeechAiAbility,
    aiKit: AiKit,
    session: FcSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = FcSpeechSession.Scene.TAXI
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
            FcSpeechSession.Format.PCM -> AiAudioFormat.PCM
            is FcSpeechSession.Format.OPUS -> AiAudioFormat.OPUS(f.frameSize)
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
