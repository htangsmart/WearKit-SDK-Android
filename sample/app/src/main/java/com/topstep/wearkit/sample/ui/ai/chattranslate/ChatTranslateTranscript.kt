package com.topstep.wearkit.sample.ui.ai.chattranslate

import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class ChatTranslateMessage(
    val id: Long,
    val index: Int,
    val isSelf: Boolean,
    val isSource: Boolean,
    val text: String,
    val isComplete: Boolean,
)

data class ChatTranslateInfo(
    val isSelf: Boolean,
    val origin: WKSpeechSession.Origin,
    val source: WKSpeechSession.Source,
    val originalLocale: String,
    val translateLocale: String,
)

/**
 * Chat-translate UI state. Separate from TranslateTranscript.
 */
object ChatTranslateTranscript {

    @Volatile
    var activeMode: WKChatTranslateMode? = null

    @Volatile
    var selfLocale: String = "zh-CN"

    @Volatile
    var peerLocale: String = "en-US"

    private val nextId = AtomicLong(0)

    private val _inUtterance = MutableStateFlow(false)
    val inUtterance: StateFlow<Boolean> = _inUtterance

    private val _info = MutableStateFlow<ChatTranslateInfo?>(null)
    val info: StateFlow<ChatTranslateInfo?> = _info

    private val _messages = MutableStateFlow<List<ChatTranslateMessage>>(emptyList())
    val messages: StateFlow<List<ChatTranslateMessage>> = _messages

    fun swapLocales() {
        val from = selfLocale
        selfLocale = peerLocale
        peerLocale = from
    }

    fun onSessionStarted(
        session: WKSpeechSession,
        isSelf: Boolean,
        originalLocale: String,
        translateLocale: String,
    ) {
        _info.value = ChatTranslateInfo(
            isSelf = isSelf,
            origin = session.origin,
            source = session.source,
            originalLocale = originalLocale,
            translateLocale = translateLocale,
        )
        _inUtterance.value = true
        // 收尾上一轮可能残留的未完成消息（异常中断时 onSessionEnded 只补最后一条），
        // 避免下一轮同 index 的流式消息命中历史未完成条目导致"新消息不显示"。
        val list = _messages.value
        if (list.any { !it.isComplete }) {
            _messages.value = list.map {
                if (it.isComplete) it else it.copy(isComplete = true)
            }
        }
    }

    fun onSourceText(isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(isSelf = isSelf, isSource = true, text = text, isComplete = isComplete, index = index)
    }

    fun onTargetText(isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(isSelf = isSelf, isSource = false, text = text, isComplete = isComplete, index = index)
    }

    fun onSessionEnded() {
        _inUtterance.value = false
        _info.value = null
        val list = _messages.value
        val last = list.lastOrNull() ?: return
        if (!last.isComplete) {
            _messages.value = list.dropLast(1) + last.copy(isComplete = true)
        }
    }

    fun clearMode() {
        activeMode = null
    }

    private fun onText(
        isSelf: Boolean,
        isSource: Boolean,
        text: String,
        isComplete: Boolean,
        index: Int,
    ) {
        val list = _messages.value
        val existingIdx = list.indexOfLast {
            !it.isComplete && it.isSelf == isSelf && it.isSource == isSource && it.index == index
        }
        _messages.value = if (existingIdx >= 0) {
            list.toMutableList().also { mutable ->
                mutable[existingIdx] = list[existingIdx].copy(text = text, isComplete = isComplete)
            }
        } else {
            list + ChatTranslateMessage(
                id = nextId.incrementAndGet(),
                index = index,
                isSelf = isSelf,
                isSource = isSource,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
