package com.topstep.wearkit.sample.ui.ai.translate

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.content.ContextCompat
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.apis.model.speech.WKTranslatePlayerState
import com.topstep.wearkit.sample.ui.ai.MyAudioPlayer
import com.topstep.wearkit.sample.ui.ai.wav.WavFileWriter
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * 翻译 TTS 控制器：落盘 + 设备播放状态 + PCM 出声。
 *
 * **出声路由与本次录音来源同路**（从哪里收就从哪里播），由 [TranslateHandler] 解析后传入 [route]：
 * - [WKSpeechSession.Source.PHONE_MIC]：手机外放（[MyAudioPlayer] 同时切通话路由，配合系统 AEC）
 * - [WKSpeechSession.Source.DEVICE_SCO]：SCO 耳机（不再占手机外放，避免与 SCO 采集冲突）
 * - [WKSpeechSession.Source.DEVICE_CMD]：设备（BLE Opus 通道）
 *
 * PCM 出声统一交给 [MyAudioPlayer]，本类只负责路由选择与落盘；PCM 为 16k / mono / 16-bit
 */
class TranslateTtsController(
    private val context: Context,
    /** TTS 回放路由，与 [TranslateHandler] 的录音来源一致。 */
    private val route: WKSpeechSession.Source,
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val writer = WavFileWriter(TAG)
    private val playerState = AtomicReference(WKTranslatePlayerState.START)
    private var mediaPlayer: MediaPlayer? = null
    private var savedFile: File? = null
    private var writing = false

    /** 落盘文件回放：喂 PCM 不能阻塞设备消息线程，放独立线程。 */
    private val replayExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "TranslateTtsReplay")
    }

    /** 回放代际：新的播放/停止请求让上一轮喂包循环提前退出。 */
    @Volatile
    private var replayGeneration = 0

    @Volatile
    private var released = false

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
        Timber.tag(TAG).i("playerState: %s -> %s route=%s", playerState.get(), state, route)
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
                cancelReplay()
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

    /**
     * @param stopPlayer true：强制停播（中断/退页）；false：仅交还所有权，让 [com.topstep.aikit.player.AiChatTtsPlayer.sendFinish] 自然播完。
     */
    fun release(stopPlayer: Boolean = true) {
        released = true
        replayExecutor.shutdown()
        if (writing) {
            writer.finish()
            writing = false
        }
        if (stopPlayer) {
            cancelReplay()
            playerState.set(WKTranslatePlayerState.STOP)
            stopMediaPlayer()
            // 回放路由由本会话持有（Handler 在 onStart 按同一路由 activate），收尾时一并释放
            MyAudioPlayer.deactivate()
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
        if (released) return
        // 已按同一路由激活就不再 activate（activate 会先 deactivate，掐断正在 drain 的队列）
        if (MyAudioPlayer.currentRoute != route) {
            MyAudioPlayer.activate(route)
        }
        if (!MyAudioPlayer.isStarted()) {
            MyAudioPlayer.start()
        }
    }

    private fun playSavedFile() {
        val file = savedFile ?: return
        stopMediaPlayer()
        cancelReplay()
        if (route == WKSpeechSession.Source.PHONE_MIC) {
            playSavedFileOnPhone(file)
        } else {
            playSavedFileOnRoute(file)
        }
    }

    /**
     * 手机外放：MediaPlayer + 钉死自带扬声器（保持既有行为）。
     */
    private fun playSavedFileOnPhone(file: File) {
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

    /**
     * SCO / 设备路由：回放落盘的 PCM，走 [MyAudioPlayer] 原路播回
     */
    private fun playSavedFileOnRoute(file: File) {
        if (released) return
        val generation = ++replayGeneration
        runCatching {
            replayExecutor.execute {
                val pcm = readPcmFromWav(file) ?: return@execute
                if (generation != replayGeneration) return@execute
                ensurePlayerStarted()
                var offset = 0
                while (offset < pcm.size) {
                    if (generation != replayGeneration) {
                        Timber.tag(TAG).i("replay cancelled at %d/%d", offset, pcm.size)
                        return@execute
                    }
                    val size = minOf(REPLAY_CHUNK_BYTES, pcm.size - offset)
                    MyAudioPlayer.sendData(pcm.copyOfRange(offset, offset + size))
                    offset += size
                }
                if (MyAudioPlayer.isStarted()) {
                    MyAudioPlayer.sendFinish()
                }
                Timber.tag(TAG).i("replay %s bytes=%d route=%s", file.name, pcm.size, route)
            }
        }.onFailure {
            Timber.tag(TAG).w(it, "replay saved tts failed")
        }
    }

    private fun cancelReplay() {
        replayGeneration++
    }

    /** 读回 wav 里的 PCM（跳过 44 字节文件头；非 wav 时原样返回）。 */
    private fun readPcmFromWav(file: File): ByteArray? {
        return runCatching {
            val bytes = file.readBytes()
            val isWav = bytes.size > WAV_HEADER_SIZE &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte()
            val header = if (isWav) WAV_HEADER_SIZE else 0
            bytes.copyOfRange(header, bytes.size)
        }.getOrElse {
            Timber.tag(TAG).w(it, "read saved tts failed")
            null
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
        private const val WAV_HEADER_SIZE = 44
        private const val REPLAY_CHUNK_BYTES = 4096

        private fun createTtsFile(context: Context): File? {
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            val dir = File(parent, DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) return null
            val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + "_tts.wav"
            return File(dir, name)
        }
    }
}
