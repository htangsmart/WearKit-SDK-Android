package com.topstep.wearkit.sample.ui.ai.ask

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class AskMessage(
    val id: Long,
    val isQuestion: Boolean,
    val text: String,
    val isComplete: Boolean,
)

/**
 * In-memory ask text produced by [AskHandler] for [AskActivity] to display.
 */
object AskTranscript {

    private val nextId = AtomicLong(0)
    private val _messages = MutableStateFlow<List<AskMessage>>(emptyList())
    val messages: StateFlow<List<AskMessage>> = _messages

    /** 正在采集（长按说话中）。 */
    private val _inUtterance = MutableStateFlow(false)
    val inUtterance: StateFlow<Boolean> = _inUtterance

    /** Session 从创建到完全结束（含回答 / TTS）。 */
    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive

    fun onSessionStarted() {
        _sessionActive.value = true
        _inUtterance.value = true
    }

    /** 采集结束：允许 UI 显示「回答中」，但不结束 session。 */
    fun onRecordingEnded() {
        _inUtterance.value = false
    }

    fun onSessionEnded() {
        _inUtterance.value = false
        _sessionActive.value = false
        val list = _messages.value
        val last = list.lastOrNull() ?: return
        if (!last.isComplete) {
            _messages.value = list.dropLast(1) + last.copy(isComplete = true)
        }
    }

    fun onText(isQuestion: Boolean, text: String, isComplete: Boolean) {
        val list = _messages.value
        val last = list.lastOrNull()
        _messages.value = if (last != null && last.isQuestion == isQuestion && !last.isComplete) {
            list.dropLast(1) + last.copy(text = text, isComplete = isComplete)
        } else {
            list + AskMessage(
                id = nextId.incrementAndGet(),
                isQuestion = isQuestion,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
