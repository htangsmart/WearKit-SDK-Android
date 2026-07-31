package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiChatResult
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechAiMessage
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import timber.log.Timber

/**
 * [FcSpeechSession.Scene.ASK]
 *
 * - ASR 问题文本：流式 [sendTextQuestion] 给设备
 * - LLM 回答：等 [ASK_GENERATE_ANSWER] 后再 [sendTextAnswer]
 *   （部分设备需用户确认问题；无确认需求时 SDK 会在问题发完后自动发出该消息）
 */
class AskHandler(
    context: Context,
    speechAi: FcSpeechAiAbility,
    aiKit: AiKit,
    session: FcSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = FcSpeechSession.Scene.ASK
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

    override fun onStart() {
        canSendAnswer = false
        pendingAnswer = null
        val source = bindAudioSource()
        disposables.add(
            aiKit.chat.chat(
                audioSource = source,
                photoSource = null,
                locale = "zh",
                vadEnabled = false,
                multiModeEnabled = false,
                isSupportEcho = true,
            ).subscribe({
                when (it) {
                    is AiChatResult.OnText -> handleChatText(it)
                    else -> {}
                }
            }, {
                Timber.tag(tag).w(it)
                release()
            })
        )
    }

    private fun handleChatText(result: AiChatResult.OnText) {
        val text = result.text.orEmpty()
        if (result.isAsr) {
            Timber.tag(tag).i("question: %s complete=%s", text, result.isComplete)
            speechAi.ask
                .sendTextQuestion(text, result.isComplete)
                .onErrorComplete()
                .subscribe()
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

    override fun onMessage(msg: FcSpeechAiMessage) {
        when (msg.type) {
            FcSpeechAiMessage.Type.ASK_GENERATE_ANSWER -> {
                Timber.tag(tag).i("ASK_GENERATE_ANSWER, flush pending=%s", pendingAnswer != null)
                canSendAnswer = true
                pendingAnswer?.let { (text, isComplete) ->
                    pendingAnswer = null
                    sendAnswer(text, isComplete)
                }
            }
            FcSpeechAiMessage.Type.ASK_SWITCH_MODEL -> {
                Timber.tag(tag).i("ASK_SWITCH_MODEL: %s", msg.data)
            }
        }
    }

    private fun sendAnswer(text: String, isComplete: Boolean) {
        speechAi.ask
            .sendTextAnswer(text, isComplete)
            .onErrorComplete()
            .subscribe()
    }

    override fun onRelease() {
        canSendAnswer = false
        pendingAnswer = null
    }
}
