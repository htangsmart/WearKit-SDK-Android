package com.topstep.wearkit.sample.ui.ai.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class ChatMessage(
    val id: Long,
    /** 本次会话内的轮次。下次开启会重新计数，具体从几开始不影响归并。 */
    val dialogId: Int,
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

    /** 本轮会话开始前已分配的最大 id。dialogId 跨会话会重复，只在此之后的消息里按它归并。 */
    private var sessionFloorId = 0L

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatting = MutableStateFlow(false)
    val chatting: StateFlow<Boolean> = _chatting

    fun onSessionStarted() {
        sessionFloorId = nextId.get()
        _chatting.value = true
    }

    fun onSessionEnded() {
        _chatting.value = false
        val floor = sessionFloorId
        val list = _messages.value
        if (list.none { it.id > floor && !it.isComplete }) return
        _messages.value = list.map { msg ->
            if (msg.id > floor && !msg.isComplete) msg.copy(isComplete = true) else msg
        }
    }

    fun onText(dialogId: Int, isQuestion: Boolean, text: String, isComplete: Boolean) {
        val floor = sessionFloorId
        val list = _messages.value
        val existingIdx = list.indexOfLast {
            it.id > floor && it.dialogId == dialogId && it.isQuestion == isQuestion
        }
        _messages.value = if (existingIdx >= 0) {
            list.toMutableList().also { mutable ->
                mutable[existingIdx] = list[existingIdx].copy(text = text, isComplete = isComplete)
            }
        } else {
            list + ChatMessage(
                id = nextId.incrementAndGet(),
                dialogId = dialogId,
                isQuestion = isQuestion,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
