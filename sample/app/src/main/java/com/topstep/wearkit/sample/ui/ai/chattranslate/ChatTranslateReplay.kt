package com.topstep.wearkit.sample.ui.ai.chattranslate

import android.os.SystemClock
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 对话翻译「先录完再播」：录音期缓存原文/译文快照与 TTS PCM，
 * [deliver] 时按原间隔更新页面、下发设备并播放。
 */
class ChatTranslateReplay(
    private val speechAi: WKSpeechAiAbility,
    private val ttsRoute: WKSpeechSession.Source,
    private val isSelf: Boolean,
    private val sendSource: Boolean,
    private val sendTarget: Boolean,
) {

    private val startedAt = SystemClock.elapsedRealtime()
    private val sourceSnaps = mutableListOf<TextSnap>()
    private val targetSnaps = mutableListOf<TextSnap>()
    private val ttsChunks = mutableListOf<ByteArray>()
    private val started = AtomicBoolean(false)

    @Volatile
    var drained = false
        private set

    fun cacheSource(text: String, isComplete: Boolean, index: Int) {
        sourceSnaps += TextSnap(elapsedMs(), text, isComplete, index)
    }

    fun cacheTarget(text: String, isComplete: Boolean, index: Int) {
        targetSnaps += TextSnap(elapsedMs(), text, isComplete, index)
    }

    fun cacheTts(pcm: ByteArray) {
        if (pcm.isEmpty()) return
        ttsChunks += pcm.copyOf()
    }

    fun deliver(): Completable {
        if (!started.compareAndSet(false, true)) {
            return Completable.complete()
        }
        Timber.tag(TAG).i(
            "deliver source=%d target=%d tts=%d",
            sourceSnaps.size, targetSnaps.size, ttsChunks.size,
        )
        return Completable.mergeArray(
            replaySnaps(sourceSnaps) { snap ->
                ChatTranslateTranscript.onSourceText(isSelf, snap.text, snap.isComplete, snap.index)
                if (sendSource) {
                    speechAi.translate.sendTextSource(snap.text, snap.isComplete).onErrorComplete()
                } else {
                    Completable.complete()
                }
            },
            replaySnaps(targetSnaps) { snap ->
                ChatTranslateTranscript.onTargetText(isSelf, snap.text, snap.isComplete, snap.index)
                if (sendTarget) {
                    speechAi.translate.sendTextTarget(snap.text, snap.isComplete).onErrorComplete()
                } else {
                    Completable.complete()
                }
            },
            Completable.fromAction { playCachedTts() }.subscribeOn(Schedulers.io()),
        ).doOnComplete { drained = true }
    }

    fun abort() {
        if (!drained) {
            MyAudioPlayer.deactivate()
        }
    }

    private fun replaySnaps(
        snaps: List<TextSnap>,
        send: (TextSnap) -> Completable,
    ): Completable {
        if (snaps.isEmpty()) return Completable.complete()
        return Observable.range(0, snaps.size)
            .concatMapCompletable { i ->
                val snap = snaps[i]
                val delayMs = if (i == 0) {
                    0L
                } else {
                    (snap.atElapsedMs - snaps[i - 1].atElapsedMs).coerceAtLeast(0L)
                }
                Completable.timer(delayMs, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .andThen(send(snap))
            }
    }

    private fun playCachedTts() {
        if (ttsChunks.isEmpty()) return
        MyAudioPlayer.activate(ttsRoute)
        MyAudioPlayer.start()
        ttsChunks.forEach { chunk ->
            if (chunk.isNotEmpty()) {
                MyAudioPlayer.sendData(chunk)
            }
        }
        if (MyAudioPlayer.isStarted()) {
            MyAudioPlayer.sendFinish()
        }
    }

    private fun elapsedMs(): Long = SystemClock.elapsedRealtime() - startedAt

    private data class TextSnap(
        val atElapsedMs: Long,
        val text: String,
        val isComplete: Boolean,
        val index: Int,
    )

    private companion object {
        const val TAG = "ChatTranslateReplay"
    }
}
