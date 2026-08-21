package com.topstep.wearkit.sample.ui.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import androidx.core.content.ContextCompat
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.wav.WavFileWriter
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * 翻译 TTS 控制器：落盘 + 设备播放状态；PCM 出声交给 [MyAudioPlayer]（默认手机扬声器）。
 *
 * PCM：16k / mono / 16-bit（与 AiKit TranslateTts 一致）。
 */
class TranslateTtsController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val writer = WavFileWriter(TAG)
    private val playerState = AtomicReference(WKTranslatePlayerState.START)
    private var mediaPlayer: MediaPlayer? = null
    private var savedFile: File? = null
    private var writing = false
    private var playerOwned = false

    /** 写入一段 TTS PCM；空数组忽略。 */
    fun writePcm(data: ByteArray) {
        if (data.isEmpty()) return
        ensureWriter()
        writer.write(data)
        if (isPlayEnabled()) {
            ensurePlayerStarted()
            MyAudioPlayer.sendData(data.copyOf())
        }
    }

    /** TTS 流结束，完成 wav 落盘。 */
    fun complete(): File? {
        if (!writing) return savedFile
        writing = false
        savedFile = writer.finish()?.takeIf { it.exists() && it.length() > 0 }
        Timber.tag(TAG).i("tts saved: %s", savedFile?.absolutePath)
        if (MyAudioPlayer.isStarted()) {
            MyAudioPlayer.sendFinish()
        }
        return savedFile
    }

    fun applyPlayerState(state: WKTranslatePlayerState) {
        Timber.tag(TAG).i("playerState: %s -> %s", playerState.get(), state)
        playerState.set(state)
        when (state) {
            WKTranslatePlayerState.START -> {
                stopMediaPlayer()
                if (savedFile != null && !MyAudioPlayer.isStarted()) {
                    playSavedFile()
                } else if (MyAudioPlayer.isStarted()) {
                    MyAudioPlayer.resume()
                } else if (writing) {
                    ensurePlayerStarted()
                }
            }
            WKTranslatePlayerState.STOP -> {
                if (MyAudioPlayer.isStarted()) {
                    MyAudioPlayer.stop()
                }
                stopMediaPlayer()
            }
            WKTranslatePlayerState.PAUSE -> {
                MyAudioPlayer.pause()
                mediaPlayer?.takeIf { it.isPlaying }?.pause()
            }
            WKTranslatePlayerState.RESUME -> {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                } else {
                    MyAudioPlayer.resume()
                }
            }
        }
    }

    fun release() {
        playerState.set(WKTranslatePlayerState.STOP)
        if (writing) {
            writer.finish()
            writing = false
        }
        stopMediaPlayer()
        if (playerOwned) {
            MyAudioPlayer.deactivate()
            playerOwned = false
        }
    }

    private fun ensureWriter() {
        if (writing) return
        val file = createTtsFile(context) ?: return
        if (writer.start(file, AiAudioFormat.PCM)) {
            writing = true
            savedFile = null
        }
    }

    private fun isPlayEnabled(): Boolean {
        return playerState.get() != WKTranslatePlayerState.STOP
    }

    private fun ensurePlayerStarted() {
        if (!playerOwned) {
            MyAudioPlayer.activate(WKSpeechSession.Source.PHONE_MIC)
            playerOwned = true
        }
        if (!MyAudioPlayer.isStarted()) {
            MyAudioPlayer.start()
        }
    }

    private fun playSavedFile() {
        val file = savedFile ?: return
        stopMediaPlayer()
        // 复用 MyAudioPlayer 的手机外放路由习惯：先 activate 再播文件
        if (!playerOwned) {
            MyAudioPlayer.activate(WKSpeechSession.Source.PHONE_MIC)
            playerOwned = true
        }
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    Timber.tag(TAG).i("media play complete")
                }
                setOnErrorListener { _, what, extra ->
                    Timber.tag(TAG).w("media error what=%d extra=%d", what, extra)
                    true
                }
                prepare()
                preferBuiltinSpeaker(this)
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

    private fun preferBuiltinSpeaker(player: MediaPlayer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val speaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        if (speaker == null) {
            Timber.tag(TAG).w("builtin speaker not found")
            return
        }
        if (!player.setPreferredDevice(speaker)) {
            Timber.tag(TAG).w("MediaPlayer setPreferredDevice speaker failed")
        }
    }

    companion object {
        private const val TAG = "TranslateTts"
        private const val DIR_NAME = "translate_tts"

        private fun createTtsFile(context: Context): File? {
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            val dir = File(parent, DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) return null
            val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + "_tts.wav"
            return File(dir, name)
        }
    }
}
