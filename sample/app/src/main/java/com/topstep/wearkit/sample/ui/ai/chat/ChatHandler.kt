package com.topstep.wearkit.sample.ui.ai.chat

import android.content.Context
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiChatResult
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.handler.SceneHandler
import timber.log.Timber

/**
 * [WKSpeechSession.Scene.CHAT]
 *
 * 与 Ask 不同：Chat 文本无需确认，ASR / LLM 结果可直接 [WKSpeechAiAbility.Chat.sendTextQuestion] / [WKSpeechAiAbility.Chat.sendTextAnswer]。
 * 发送前用 [WKSpeechAiAbility.Chat.isSupportText] 判断设备是否支持展示文本。
 *
 * 离场：CHAT 为持续音频流。因此在音频流结束时即 [release]；若随后仍收到 EXIT，[release] 幂等。
 */
class ChatHandler(
    context: Context,
    speechAi: WKSpeechAiAbility,
    aiKit: AiKit,
    session: WKSpeechSession,
    onReleased: () -> Unit,
) : SceneHandler(context, speechAi, aiKit, session, onReleased) {

    override val scene = WKSpeechSession.Scene.CHAT
    override val tag = "ChatHandler"

    private var supportText = false

    override fun onStart() {
        supportText = speechAi.chat.isSupportText()
        Timber.tag(tag).i("supportText=%s", supportText)
        ChatTranscript.onSessionStarted()

        val mode = if (session.source == WKSpeechSession.Source.DEVICE_CMD) {
            if (speechAi.player.isSupport(session.scene)) {
                WKSpeechSession.Source.DEVICE_CMD
            } else {
                //如果不支持播放到设备，默认就用手机麦克风播放
                WKSpeechSession.Source.PHONE_MIC
            }
        } else {
            session.source
        }
        MyAudioPlayer.activate(mode)
        val source = bindAudioSource(releaseOnAudioEnd = true)
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
        ChatTranscript.onText(isQuestion = result.isAsr, text = text, isComplete = result.isComplete)
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

    override fun onRelease() {
        MyAudioPlayer.deactivate()
        ChatTranscript.onSessionEnded()
    }
}