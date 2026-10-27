package com.topstep.wearkit.sample.ui.ai.translate

import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.SpeechTextKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class TranslateMessage(
    val id: Long,
    val index: Int,
    val kind: SpeechTextKind,
    val text: String,
    val isComplete: Boolean,
)

data class TranslateInfo(
    val origin: WKSpeechSession.Origin,
    val source: WKSpeechSession.Source,
    val originalLocale: String,
    val translateLocale: String,
)

/**
 * In-memory translate state for [TranslateActivity], written by [TranslateHandler].
 */
object TranslateTranscript {

    @Volatile
    var appOriginalLocale: String = "zh-CN"

    @Volatile
    var appTranslateLocale: String = "en-US"

    private val nextId = AtomicLong(0)

    private val _translating = MutableStateFlow(false)
    val translating: StateFlow<Boolean> = _translating

    private val _info = MutableStateFlow<TranslateInfo?>(null)
    val info: StateFlow<TranslateInfo?> = _info

    private val _messages = MutableStateFlow<List<TranslateMessage>>(emptyList())
    val messages: StateFlow<List<TranslateMessage>> = _messages

    private val _playerState = MutableStateFlow<WKTranslatePlayerState?>(null)
    val playerState: StateFlow<WKTranslatePlayerState?> = _playerState

    fun swapAppLocales() {
        val from = appOriginalLocale
        appOriginalLocale = appTranslateLocale
        appTranslateLocale = from
    }

    fun onSessionStarted(
        session: WKSpeechSession,
        originalLocale: String,
        translateLocale: String,
    ) {
        _messages.value = emptyList()
        _playerState.value = WKTranslatePlayerState.START
        _info.value = TranslateInfo(
            origin = session.origin,
            source = session.source,
            originalLocale = originalLocale,
            translateLocale = translateLocale,
        )
        _translating.value = true
    }

    fun onSourceText(text: String, isComplete: Boolean, index: Int) {
        onText(SpeechTextKind.SOURCE, text, isComplete, index)
    }

    fun onTargetText(text: String, isComplete: Boolean, index: Int) {
        onText(SpeechTextKind.TARGET, text, isComplete, index)
    }

    fun onPlayerState(state: WKTranslatePlayerState) {
        _playerState.value = state
    }

    fun onWarning(text: String) {
        append(SpeechTextKind.WARNING, text)
    }

    fun onError(text: String) {
        append(SpeechTextKind.ERROR, text)
    }

    private fun append(kind: SpeechTextKind, text: String) {
        _messages.value += TranslateMessage(
            id = nextId.incrementAndGet(),
            index = -1,
            kind = kind,
            text = text,
            isComplete = true,
        )
    }

    fun onSessionEnded() {
        _translating.value = false
        _info.value = null
        _playerState.value = null
        val list = _messages.value
        val last = list.lastOrNull() ?: return
        if (!last.isComplete) {
            _messages.value = list.dropLast(1) + last.copy(isComplete = true)
        }
    }

    private fun onText(kind: SpeechTextKind, text: String, isComplete: Boolean, index: Int) {
        val list = _messages.value
        val existingIdx = list.indexOfLast {
            it.kind == kind && it.index == index
        }
        _messages.value = if (existingIdx >= 0) {
            list.toMutableList().also { mutable ->
                mutable[existingIdx] = list[existingIdx].copy(text = text, isComplete = isComplete)
            }
        } else {
            list + TranslateMessage(
                id = nextId.incrementAndGet(),
                index = index,
                kind = kind,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
