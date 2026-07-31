package com.topstep.wearkit.sample.ui.ai.handler

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiChatResult
import com.topstep.fitcloud.sdk.apis.ability.speech.FcSpeechAiAbility
import com.topstep.fitcloud.sdk.model.speech.FcSpeechSession
import timber.log.Timber

/**
 * [FcSpeechSession.Scene.CHAT]
 *
 * 与 Ask 不同：Chat 文本无需确认，ASR / LLM 结果可直接 [sendTextQuestion] / [sendTextAnswer]。
 * 发送前用 [FcSpeechAiAbility.Chat.isSupportText] 判断设备是否支持展示文本。
 */
class ChatHandler(
    context: Context,
    speechAi: FcSpeechAiAbility,
    aiKit: AiKit,
    session: FcSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = FcSpeechSession.Scene.CHAT
    override val tag = "ChatHandler"

    private var supportText = false

    override fun onStart() {
        supportText = speechAi.chat.isSupportText()
        Timber.tag(tag).i("supportText=%s", supportText)
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
            if (supportText) {
                disposables.add(
                    speechAi.chat
                        .sendTextQuestion(text, result.isComplete)
                        .onErrorComplete()
                        .subscribe()
                )
            }
            return
        }

        Timber.tag(tag).i("answer: %s complete=%s", text, result.isComplete)
        if (supportText) {
            disposables.add(
                speechAi.chat
                    .sendTextAnswer(text, result.isComplete)
                    .onErrorComplete()
                    .subscribe()
            )
        }
    }
}
