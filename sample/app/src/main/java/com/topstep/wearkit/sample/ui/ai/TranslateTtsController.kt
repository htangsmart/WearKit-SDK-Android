package com.topstep.wearkit.sample.ui.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.fitcloud.sdk.model.speech.FcTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.wav.WavFileWriter
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * 翻译 TTS：落盘为 wav，并按设备 [FcTranslatePlayerState] 控制播放。
 *
 * PCM：16k / mono / 16-bit（与 AiKit TranslateTts 一致）。
 */
class TranslateTtsController(private val context: Context) {

    private val writer = WavFileWriter(TAG)
    private val playerState = AtomicReference(FcTranslatePlayerState.START)
    private val streaming = AtomicBoolean(false)
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var playbackThread: Thread? = null
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var savedFile: File? = null
    private var writing = false

    /** 写入一段 TTS PCM；空数组忽略。 */
    fun writePcm(data: ByteArray) {
        if (data.isEmpty()) return
        ensureWriter()
        writer.write(data)
        if (isPlayEnabled()) {
            ensureStreaming()
            queue.offer(data.copyOf())
        }
    }

    /** TTS 流结束，完成 wav 落盘。 */
    fun complete(): File? {
        if (!writing) return savedFile
        writing = false
        savedFile = writer.finish()?.takeIf { it.exists() && it.length() > 0 }
        Timber.tag(TAG).i("tts saved: %s", savedFile?.absolutePath)
        if (streaming.get()) {
            queue.offer(END_MARKER)
        }
        return savedFile
    }

    fun applyPlayerState(state: FcTranslatePlayerState) {
        Timber.tag(TAG).i("playerState: %s -> %s", playerState.get(), state)
        playerState.set(state)
        when (state) {
            FcTranslatePlayerState.START -> {
                stopMediaPlayer()
                if (savedFile != null && !streaming.get()) {
                    playSavedFile()
                } else {
                    ensureStreaming()
                    audioTrack?.play()
                }
            }
            FcTranslatePlayerState.STOP -> {
                stopStreaming(clearQueue = true)
                stopMediaPlayer()
            }
            FcTranslatePlayerState.PAUSE -> {
                audioTrack?.pause()
                mediaPlayer?.takeIf { it.isPlaying }?.pause()
            }
            FcTranslatePlayerState.RESUME -> {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                } else {
                    ensureStreaming()
                    audioTrack?.play()
                }
            }
        }
    }

    fun release() {
        playerState.set(FcTranslatePlayerState.STOP)
        if (writing) {
            writer.finish()
            writing = false
        }
        stopStreaming(clearQueue = true)
        stopMediaPlayer()
        audioTrack?.release()
        audioTrack = null
    }

    private fun ensureWriter() {
        if (writing) return
        val file = createTtsFile(context) ?: return
        if (writer.start(file, AiAudioFormat.PCM)) {
            writing = true
            savedFile = null
        }
    }

    /** STOP 时丢弃；PAUSE 时仍入队，由 AudioTrack.pause 卡住写出。 */
    private fun isPlayEnabled(): Boolean {
        return playerState.get() != FcTranslatePlayerState.STOP
    }

    private fun ensureStreaming() {
        if (!streaming.compareAndSet(false, true)) return
        val track = audioTrack ?: createAudioTrack().also { audioTrack = it }
        track.play()
        playbackThread = thread(name = "translate-tts") {
            try {
                while (streaming.get()) {
                    val data = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    if (data === END_MARKER) break
                    if (data.isNotEmpty()) {
                        // PAUSE 时 AudioTrack.pause，write 会阻塞或写入缓冲
                        track.write(data, 0, data.size)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "stream play failed")
            } finally {
                streaming.set(false)
            }
        }
    }

    private fun stopStreaming(clearQueue: Boolean) {
        if (!streaming.getAndSet(false)) {
            if (clearQueue) queue.clear()
            audioTrack?.runCatching {
                pause()
                flush()
                stop()
            }
            return
        }
        playbackThread?.interrupt()
        playbackThread = null
        if (clearQueue) queue.clear()
        audioTrack?.runCatching {
            pause()
            flush()
            stop()
        }
    }

    private fun playSavedFile() {
        val file = savedFile ?: return
        stopMediaPlayer()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    Timber.tag(TAG).i("media play complete")
                }
                setOnErrorListener { _, what, extra ->
                    Timber.tag(TAG).w("media error what=%d extra=%d", what, extra)
                    true
                }
                prepare()
                start()
            }
        }.onFailure {
            Timber.tag(TAG).w(it, "play saved tts failed")
        }
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun createAudioTrack(): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE / 10)
        return AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
            AudioTrack.MODE_STREAM,
        )
    }

    companion object {
        private const val TAG = "TranslateTts"
        private const val DIR_NAME = "translate_tts"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val END_MARKER = ByteArray(0)

        private fun createTtsFile(context: Context): File? {
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            val dir = File(parent, DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) return null
            val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + "_tts.wav"
            return File(dir, name)
        }
    }
}
