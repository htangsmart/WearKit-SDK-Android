package com.topstep.wearkit.sample.ui.ai.ask

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class AskMessage(
    val id: Long,
    /** 本次会话内的轮次。下次开启会重新计数，具体从几开始不影响归并。 */
    val dialogId: Int,
    val isQuestion: Boolean,
    val text: String,
    val isComplete: Boolean,
)

/**
 * In-memory ask text produced by [AskHandler] for [AskActivity] to display.
 */
object AskTranscript {

    private val nextId = AtomicLong(0)

    /** 本轮会话开始前已分配的最大 id。dialogId 跨会话会重复，只在此之后的消息里按它归并。 */
    private var sessionFloorId = 0L

    private val _messages = MutableStateFlow<List<AskMessage>>(emptyList())
    val messages: StateFlow<List<AskMessage>> = _messages

    /** 正在采集（长按说话中）。 */
    private val _inUtterance = MutableStateFlow(false)
    val inUtterance: StateFlow<Boolean> = _inUtterance

    /** Session 从创建到完全结束（含回答 / TTS）。 */
    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive

    fun onSessionStarted() {
        sessionFloorId = nextId.get()
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
            list + AskMessage(
                id = nextId.incrementAndGet(),
                dialogId = dialogId,
                isQuestion = isQuestion,
                text = text,
                isComplete = isComplete,
            )
        }
    }
}
