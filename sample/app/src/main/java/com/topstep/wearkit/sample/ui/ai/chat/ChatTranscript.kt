package com.topstep.wearkit.sample.ui.ai.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class ChatMessage(
    val id: Long,
    val isQuestion: Boolean,
    val text: String,
    val isComplete: Boolean,
)

/**
 * In-memory chat text produced by [ChatHandler]
 * (running in [com.topstep.wearkit.sample.ui.ai.SpeechAiActivity]) for [ChatActivity] to display.
 */
object ChatTranscript {

    private val nextId = AtomicLong(0)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatting = MutableStateFlow(false)
    val chatting: StateFlow<Boolean> = _chatting

    fun onSessionStarted() {
        _chatting.value = true
    }

    fun onSessionEnded() {
        _chatting.value = false
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
            list + ChatMessage(
                id = nextId.incrementAndGet(),
                isQuestion = isQuestion,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
