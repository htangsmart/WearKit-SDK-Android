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

    fun onSessionStarted(
        session: WKSpeechSession,
        localeLabel: String,
    ) {
        _asrText.value = ""
        _info.value = RecordInfo(
            origin = session.origin,
            source = session.source,
            scene = session.scene,
            localeLabel = localeLabel,
        )
        _recording.value = true
    }

    fun onAsrText(text: String, isComplete: Boolean) {
        _asrText.value = if (isComplete) text else "$text…"
    }

    fun onSessionEnded() {
        _recording.value = false
        _info.value = null
    }
}
