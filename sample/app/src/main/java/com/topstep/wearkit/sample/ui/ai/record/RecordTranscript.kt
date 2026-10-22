package com.topstep.wearkit.sample.ui.ai.record

import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RecordInfo(
    val origin: WKSpeechSession.Origin,
    val source: WKSpeechSession.Source,
    val scene: WKSpeechSession.Scene,
    val localeLabel: String,
)

/**
 * In-memory record session state / ASR text for [RecordActivity].
 */
object RecordTranscript {

    @Volatile
    var appLocale: String = "zh-CN"

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _info = MutableStateFlow<RecordInfo?>(null)
    val info: StateFlow<RecordInfo?> = _info

    private val _asrText = MutableStateFlow("")
    val asrText: StateFlow<String> = _asrText

    /**
     * 按句保存：ASR 的 index 是句序号，同一 index 的结果是该句的最新快照。
     * 只存最新一句会让下一句的中间结果覆盖前面已定稿的句子。
     */
    private val segments = sortedMapOf<Int, Segment>()
    private var separator = ""

    // ASR 回调不在主线程，与主线程的 start/end 读改写须串行
    @Synchronized
    fun onSessionStarted(
        session: WKSpeechSession,
        localeLabel: String,
    ) {
        segments.clear()
        separator = if (localeLabel == "en") " " else ""
        _asrText.value = ""
        _info.value = RecordInfo(
            origin = session.origin,
            source = session.source,
            scene = session.scene,
            localeLabel = localeLabel,
        )
        _recording.value = true
    }

    @Synchronized
    fun onAsrText(text: String, isComplete: Boolean, index: Int) {
        segments[index] = Segment(text, isComplete)
        _asrText.value = segments.values
            .filter { it.text.isNotEmpty() }
            .joinToString(separator) { if (it.isComplete) it.text else "${it.text}…" }
    }

    @Synchronized
    fun onSessionEnded() {
        _recording.value = false
        _info.value = null
    }

    private data class Segment(val text: String, val isComplete: Boolean)
}
