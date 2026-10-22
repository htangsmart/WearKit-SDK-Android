package com.topstep.wearkit.sample.ui.ai.chattranslate

import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class ChatTranslateMessage(
    val id: Long,
    /** 所属轮次（一次按住说话），见 [ChatTranslateTranscript.newUtterance]。 */
    val utterance: Long,
    /** 轮次内的句序号（ASR index），每轮从头计数。 */
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
    private val utteranceSeq = AtomicLong(0)

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

    /** 分配一轮（一次按住说话）的标识，用于文本归并。 */
    fun newUtterance(): Long = utteranceSeq.incrementAndGet()

    @Synchronized
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
    }

    fun onSourceText(utterance: Long, isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(utterance, isSelf = isSelf, isSource = true, text = text, isComplete = isComplete, index = index)
    }

    fun onTargetText(utterance: Long, isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(utterance, isSelf = isSelf, isSource = false, text = text, isComplete = isComplete, index = index)
    }

    /** 采集结束：允许再开新一轮，不把进行中的文本标成 complete。 */
    @Synchronized
    fun onRecordingEnded() {
        _inUtterance.value = false
        _info.value = null
    }

    @Synchronized
    fun onSessionEnded() {
        onRecordingEnded()
        val list = _messages.value
        if (list.any { !it.isComplete }) {
            _messages.value = list.map {
                if (it.isComplete) it else it.copy(isComplete = true)
            }
        }
    }

    fun clearMode() {
        activeMode = null
    }

    /**
     * 按「轮次 + 角色 + 原文/译文 + 句序号」归并：同一句的新快照覆盖旧快照，不同句各占一条。
     * 只按「最后一条未完成」归并时，第 N+1 句的中间结果会顶掉第 N 句尚未定稿的文本。
     */
    @Synchronized
    private fun onText(
        utterance: Long,
        isSelf: Boolean,
        isSource: Boolean,
        text: String,
        isComplete: Boolean,
        index: Int,
    ) {
        val list = _messages.value
        val existingIdx = list.indexOfLast {
            it.utterance == utterance && it.isSelf == isSelf && it.isSource == isSource && it.index == index
        }
        _messages.value = if (existingIdx >= 0) {
            list.toMutableList().also { mutable ->
                mutable[existingIdx] = list[existingIdx].copy(
                    text = text,
                    isComplete = isComplete,
                )
            }
        } else {
            list + ChatTranslateMessage(
                id = nextId.incrementAndGet(),
                utterance = utterance,
                index = index,
                isSelf = isSelf,
                isSource = isSource,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
