package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import android.graphics.Point
import android.net.Uri
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAsrParams
import com.topstep.aikit.model.AiAsrResult
import com.topstep.wearkit.apis.ability.dial.WKDialStyleAbility
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechAiError
import com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.MyDialStyleProvider
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * [WKSpeechSession.Scene.DIAL]
 *
 * 1. 语音 → AiKit ASR → [sendText]
 * 2. [DIAL_GENERATE_IMAGE]：确认用文字生图 → [prepareImage] → [sendImage] 推**图片**预览
 * 3. [DIAL_GENERATE_DIAL]：确认用图片生成表盘 → [prepareDial] → [install] 推**表盘**；
 *    仅生成表盘失败时 [sendError]（安装失败不上报）
 */
class DialHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = WKSpeechSession.Scene.DIAL
    override val tag = "DialHandler"

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    /** 预览阶段已准备好的图片（供后续组盘使用） */
    private var preparedImageFile: File? = null

    override fun onStart() {
        Timber.tag(tag).i("start ASR")
        val source = bindAudioSource()
        disposables.add(
            aiKit.audio.asr(
                source, AiAsrParams(
                    originalLocale = "zh-CN",
                    translateLocale = null,
                    originalFileRequired = false,
                    translateTtsRequired = false,
                    autoStop = true,
                )
            ).subscribe({ result ->
                when (result) {
                    is AiAsrResult.OriginalText -> {
                        Timber.tag(tag).i(
                            "asr[%d]: %s complete=%s",
                            result.index, result.text, result.isComplete
                        )
                        if (result.isComplete) {
                            speechAi.dial
                                .sendText(result.text, true)
                                .onErrorComplete()
                                .subscribe()
                        }
                    }
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it, "asr error")
                speechAi.dial
                    .sendError(WKSpeechAiError.UNRECOGNIZED, it.message.orEmpty())
                    .onErrorComplete()
                    .subscribe()
                release()
            })
        )
    }

    override fun onMessage(msg: WKSpeechAiMessage) {
        when (msg.type) {
            WKSpeechAiMessage.Type.DIAL_GENERATE_IMAGE -> {
                val size = msg.data as? Point
                if (size == null || size.x <= 0 || size.y <= 0) {
                    Timber.tag(tag).w("DIAL_GENERATE_IMAGE invalid size: %s", msg.data)
                    speechAi.dial
                        .sendError(WKSpeechAiError.OTHERS, "invalid image size")
                        .onErrorComplete()
                        .subscribe()
                    return
                }
                imageWidth = size.x
                imageHeight = size.y
                Timber.tag(tag).i(
                    "DIAL_GENERATE_IMAGE %dx%d → push preview image",
                    imageWidth, imageHeight
                )
                pushPreviewImage()
            }
            WKSpeechAiMessage.Type.DIAL_GENERATE_DIAL -> {
                Timber.tag(tag).i("DIAL_GENERATE_DIAL → push dial")
                pushDial()
            }
        }
    }

    /** 将准备好的图片推到设备做预览（非表盘安装）。 */
    private fun pushPreviewImage() {
        val file = prepareImage() ?: return
        Timber.tag(tag).i("sendImage %s %dx%d", file.absolutePath, imageWidth, imageHeight)
        disposables.add(
            speechAi.dial.sendImage(file, imageWidth, imageHeight).subscribe({ progress ->
                Timber.tag(tag).i("sendImage progress=%d", progress)
            }, {
                // 发送失败不走 sendError；sendError 仅用于准备图片失败
                Timber.tag(tag).w(it, "sendImage failed")
            }, {
                Timber.tag(tag).i("sendImage complete")
            })
        )
    }

    /**
     * 将准备好的表盘文件安装到设备。
     * 仅 [prepareDial]（生成表盘）失败时 [WKSpeechAiAbility.Dial.sendError]；安装失败只打日志。
     */
    private fun pushDial() {
        disposables.add(
            prepareDial()
                .doOnError {
                    Timber.tag(tag).w(it, "prepareDial failed")
                    speechAi.dial
                        .sendError(WKSpeechAiError.OTHERS, it.message.orEmpty())
                        .onErrorComplete()
                        .subscribe()
                }
                .flatMapObservable { output ->
                    Timber.tag(tag).i(
                        "install dialId=%s file=%s",
                        output.dialId, output.dialFile.absolutePath
                    )
                    MyApplication.wearKit.dialAbility.install(output.dialId, output.dialFile)
                }
                .subscribe({ progress ->
                    Timber.tag(tag).i("install dial progress=%d", progress)
                }, {
                    // prepareDial 错误已在 doOnError 上报；此处多为 install 失败，不再 sendError
                    Timber.tag(tag).w(it, "push dial failed")
                }, {
                    Timber.tag(tag).i("push dial complete")
                })
        )
    }

    /**
     * 准备预览图片。
     *
     * 模拟「根据 ASR 文本请求生图」：当前 aikit 无生图能力，直接使用 assets/dial_test.jpg。
     * 准备失败时才 [WKSpeechAiAbility.Dial.sendError]。
     */
    private fun prepareImage(): File? {
        return runCatching {
            val out = File(context.cacheDir, "dial_test.jpg")
            if (!out.exists() || out.length() == 0L) {
                context.assets.open(ASSET_DIAL_TEST_IMAGE).use { input ->
                    FileOutputStream(out).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            out.also { preparedImageFile = it }
        }.getOrElse {
            Timber.tag(tag).w(it, "prepareImage failed")
            speechAi.dial
                .sendError(WKSpeechAiError.OTHERS, "prepare image failed")
                .onErrorComplete()
                .subscribe()
            null
        }
    }

    /**
     * 准备表盘文件（图片 → 表盘 bin）。
     */
    private fun prepareDial(): Single<WKDialStyleAbility.CreateOutput> {
        val imageFile = preparedImageFile ?: prepareImage()
        if (imageFile == null) {
            return Single.error(IllegalStateException("no prepared image for dial"))
        }
        val wearKit = MyApplication.wearKit
        val backgroundUri = Uri.fromFile(imageFile)
        return MyDialStyleProvider.getResources(wearKit.deviceAbility.getDeviceInfo())
            .flatMap { optional ->
                optional.value?.let { Single.just(it) }
                    ?: wearKit.dialStyleAbility.requestCloudDialStyleResources()
            }
            .flatMap { resources ->
                wearKit.dialStyleAbility.requestConstraint(resources)
            }
            .flatMap { constraint ->
                wearKit.dialStyleAbility.createCustom(
                    constraint = constraint,
                    input = WKDialStyleAbility.CreateInput.base(
                        backgroundUri = backgroundUri,
                        style = WKDialStyleAbility.StyleConfig(
                            styleIndex = 0,
                            positionIndex = 0,
                        ),
                    ),
                )
            }
    }

    override fun onRelease() {
        imageWidth = 0
        imageHeight = 0
        preparedImageFile = null
    }

    companion object {
        /** 模拟生图用的本地图片资源（不是表盘文件） */
        private const val ASSET_DIAL_TEST_IMAGE = "dial_test.jpg"
    }
}
