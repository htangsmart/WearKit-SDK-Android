package com.topstep.wearkit.sample.ui.ai.chattranslate

import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.SpeechTextKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class ChatTranslateMessage(
    val id: Long,
    val index: Int,
    val isSelf: Boolean,
    val kind: SpeechTextKind,
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
    }

    fun onSourceText(isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(isSelf, SpeechTextKind.SOURCE, text, isComplete, index)
    }

    fun onTargetText(isSelf: Boolean, text: String, isComplete: Boolean, index: Int) {
        onText(isSelf, SpeechTextKind.TARGET, text, isComplete, index)
    }

    fun onWarning(text: String) {
        append(SpeechTextKind.WARNING, text)
    }

    fun onError(text: String) {
        append(SpeechTextKind.ERROR, text)
    }

    private fun append(kind: SpeechTextKind, text: String) {
        _messages.value += ChatTranslateMessage(
            id = nextId.incrementAndGet(),
            index = -1,
            isSelf = false,
            kind = kind,
            text = text,
            isComplete = true,
        )
    }

    /** 采集结束：允许再开新一轮，不把进行中的文本标成 complete。 */
    fun onRecordingEnded() {
        _inUtterance.value = false
        _info.value = null
    }

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

    private fun onText(
        isSelf: Boolean,
        kind: SpeechTextKind,
        text: String,
        isComplete: Boolean,
        index: Int,
    ) {
        val list = _messages.value
        val existingIdx = list.indexOfLast {
            it.isSelf == isSelf && it.kind == kind && !it.isComplete
        }
        _messages.value = if (existingIdx >= 0) {
            list.toMutableList().also { mutable ->
                mutable[existingIdx] = list[existingIdx].copy(
                    index = index,
                    text = text,
                    isComplete = isComplete,
                )
            }
        } else {
            list + ChatTranslateMessage(
                id = nextId.incrementAndGet(),
                index = index,
                isSelf = isSelf,
                kind = kind,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
