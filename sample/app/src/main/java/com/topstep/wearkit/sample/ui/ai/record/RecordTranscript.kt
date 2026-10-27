package com.topstep.wearkit.sample.ui.ai.record

import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.SpeechTextKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RecordText(
    val kind: SpeechTextKind,
    val text: String,
)

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

    /** 当前要展示的最新一条。下次开始录音时清掉。 */
    private val _latest = MutableStateFlow<RecordText?>(null)
    val latest: StateFlow<RecordText?> = _latest

    fun onSessionStarted(
        session: WKSpeechSession,
        localeLabel: String,
    ) {
        _latest.value = null
        _info.value = RecordInfo(
            origin = session.origin,
            source = session.source,
            scene = session.scene,
            localeLabel = localeLabel,
        )
        _recording.value = true
    }

    fun onText(kind: SpeechTextKind, text: String, isComplete: Boolean) {
        val shown = if (kind == SpeechTextKind.SOURCE && !isComplete) "$text…" else text
        _latest.value = RecordText(kind, shown)
    }

    fun onWarning(text: String) {
        onText(SpeechTextKind.WARNING, text, true)
    }

    fun onError(text: String) {
        onText(SpeechTextKind.ERROR, text, true)
    }

    fun onSessionEnded() {
        _recording.value = false
        _info.value = null
    }
}
