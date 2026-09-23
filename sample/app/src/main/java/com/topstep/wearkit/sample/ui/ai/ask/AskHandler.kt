package com.topstep.wearkit.sample.ui.ai.ask

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiChatResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechAiMessage
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.ASK]
 *
 * - ASR 问题文本：流式 [sendTextQuestion] 给设备
 * - LLM 回答：等 [ASK_GENERATE_ANSWER] 后再 [sendTextAnswer]
 *   （部分设备需用户确认问题；无确认需求时 SDK 会在问题发完后自动发出该消息）
 * - TTS：与录音 source 对齐（PHONE_MIC → 手机扬声器；DEVICE_SCO → SCO；DEVICE_CMD 优先设备播放）
 *
 * 音频结束只复位「采集中」UI。识别失败（如 6400）不会结束 chat Observable，
 * 采集结束后若迟迟没有问题/回答，自行 [release]，避免页面停在「回答中」。
 * 正常收尾不 [MyAudioPlayer.deactivate]，让回答 TTS 播完。
 */
class AskHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = WKSpeechSession.Scene.ASK
    override val tag = "AskHandler"

    /** 是否已允许向设备发送回答 */
    @Volatile
    private var canSendAnswer = false

    /**
     * 在允许发送前缓存的最新回答快照（累计全文）。
     * Pair.first = text，Pair.second = isComplete
     */
    @Volatile
    private var pendingAnswer: Pair<String, Boolean>? = null

    /** true：正常收尾，release 时不掐断 TTS。 */
    @Volatile
    private var allowTtsDrain = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureEnded = false
    private var questionReady = false
    private var sawAnswer = false
    private val giveUp = Runnable {
        Timber.tag(tag).i("no chat result after capture end → release")
        release()
    }

    override fun onStart() {
        canSendAnswer = false
        pendingAnswer = null
        allowTtsDrain = false
        AskTranscript.onSessionStarted()

        val mode = if (session.source == WKSpeechSession.Source.DEVICE_CMD) {
            if (speechAi.player.isSupport(session.scene)) {
                WKSpeechSession.Source.DEVICE_CMD
            } else {
                WKSpeechSession.Source.PHONE_MIC
            }
        } else {
            session.source
        }
        MyAudioPlayer.activate(mode)

        val source = bindAudioSource(
            onAudioStop = {
                Timber.tag(tag).i("audio stop → UI follow session")
                AskTranscript.onRecordingEnded()
                captureEnded = true
                armGiveUp()
                false
            },
        )
        disposables.add(
            aiKit.chat.chat(
                audioSource = source,
                photoSource = null,
                locale = "zh",
                vadEnabled = false,
                multiModeEnabled = false,
                isSupportEcho = true,
                ttsPlayer = MyAudioPlayer,
            ).subscribe({
                when (it) {
                    is AiChatResult.OnText -> handleChatText(it)
                    is AiChatResult.OnTtsPlayComplete -> onTtsPlayComplete()
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it)
                release()
            }, {
                Timber.tag(tag).i("ask complete → release (drain tts)")
                allowTtsDrain = true
                release()
            })
        )
    }

    private fun handleChatText(result: AiChatResult.OnText) {
        val text = result.text.orEmpty()
        if (result.isAsr) {
            if (result.isComplete) questionReady = true
        } else if (text.isNotEmpty()) {
            sawAnswer = true
        }
        if (captureEnded) armGiveUp()
        AskTranscript.onText(dialogId = result.dialogId, isQuestion = result.isAsr, text = text, isComplete = result.isComplete)
        if (result.isAsr) {
            Timber.tag(tag).i("question: %s complete=%s", text, result.isComplete)
            disposables.add(
                speechAi.ask
                    .sendTextQuestion(text, result.isComplete)
                    .onErrorComplete()
                    .subscribe()
            )
            return
        }

        Timber.tag(tag).i(
            "answer: %s complete=%s canSend=%s",
            text, result.isComplete, canSendAnswer
        )
        if (canSendAnswer) {
            sendAnswer(text, result.isComplete)
        } else {
            // 累计快照，只保留最新一份
            pendingAnswer = text to result.isComplete
        }
    }

    override fun onMessage(msg: WKSpeechAiMessage) {
        when (msg.type) {
            WKSpeechAiMessage.Type.ASK_GENERATE_ANSWER -> {
                Timber.tag(tag).i("ASK_GENERATE_ANSWER, flush pending=%s", pendingAnswer != null)
                canSendAnswer = true
                pendingAnswer?.let { (text, isComplete) ->
                    pendingAnswer = null
                    sendAnswer(text, isComplete)
                }
            }
            WKSpeechAiMessage.Type.ASK_SWITCH_MODEL -> {
                Timber.tag(tag).i("ASK_SWITCH_MODEL: %s", msg.data)
            }
            else -> {}
        }
    }

    private fun sendAnswer(text: String, isComplete: Boolean) {
        disposables.add(
            speechAi.ask
                .sendTextAnswer(text, isComplete)
                .onErrorComplete()
                .subscribe()
        )
    }

    private fun onTtsPlayComplete() {
        if (!captureEnded || !sawAnswer) return
        Timber.tag(tag).i("tts complete after capture end → release")
        allowTtsDrain = true
        release()
    }

    /** 识别失败不会结束 chat。没识别出问题就短等，已有问题或回答则留给生成和播报。 */
    private fun armGiveUp() {
        val delay = if (questionReady || sawAnswer) ANSWER_WAIT_MS else RECOGNIZE_WAIT_MS
        mainHandler.removeCallbacks(giveUp)
        mainHandler.postDelayed(giveUp, delay)
    }

    override fun onRelease() {
        mainHandler.removeCallbacks(giveUp)
        canSendAnswer = false
        pendingAnswer = null
        if (!allowTtsDrain) {
            MyAudioPlayer.deactivate()
        }
        AskTranscript.onSessionEnded()
    }

    private companion object {
        const val RECOGNIZE_WAIT_MS = 5_000L
        const val ANSWER_WAIT_MS = 25_000L
    }
}
