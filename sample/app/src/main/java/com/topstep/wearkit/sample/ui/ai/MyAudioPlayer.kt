package com.topstep.wearkit.sample.ui.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.topstep.aikit.player.AiChatTtsPlayer
import com.topstep.aikit.player.TtsAudioPlayer
import com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import timber.log.Timber

/**
 * 单例 TTS 播放器：Chat / Translate 共用。
 *
 * 播放前 [activate] 设置 mode，与录音通路对齐：
 * - [WKSpeechSession.Source.PHONE_MIC]：强制 [AudioDeviceInfo.TYPE_BUILTIN_SPEAKER]，
 *   并走通话路由（[AudioManager.MODE_IN_COMMUNICATION] + [AudioAttributes.USAGE_VOICE_COMMUNICATION]）
 *   以便系统 AEC 消除扬声器回声
 * - [WKSpeechSession.Source.DEVICE_SCO]：强制 [AudioDeviceInfo.TYPE_BLUETOOTH_SCO]（耳机）
 * - [WKSpeechSession.Source.DEVICE_CMD]：PCM → SDK 编码后经指令通道下发设备
 *
 * Chat：注入 [com.topstep.aikit.AiChatAbility.chat] 的 `ttsPlayer`。
 * Translate：由 [TranslateTtsController] 调用 [start]/[sendData]/[sendFinish]/[pause]/[resume]/[stop]。
 * [onWrite] 阻塞到该包发送/播放完成。
 */
object MyAudioPlayer : AiChatTtsPlayer() {

    private val speechAi: WKSpeechAiAbility = MyApplication.wearKit.speechAiAbility
    private val audioManager =
        MyApplication.instance.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var mode: WKSpeechSession.Source = WKSpeechSession.Source.PHONE_MIC

    @Volatile
    private var sessionActive = false

    private var audioTrack: AudioTrack? = null
    private var preferredCommunicationDevice: AudioDeviceInfo? = null

    override fun tag(): String = TAG

    fun activate(mode: WKSpeechSession.Source) {
        this.mode = mode
        sessionActive = true
        // 录音开始前就切到通话路由，AEC 才能在首包 TTS 前完成通路建立
        if (mode == WKSpeechSession.Source.PHONE_MIC || mode == WKSpeechSession.Source.DEVICE_SCO) {
            applyLocalRouting(mode)
        }
    }

    fun deactivate() {
        sessionActive = false
        // Stop AiChatTtsPlayer workers first; late callbacks will be ignored by the active guard.
        stop()
        if (mode == WKSpeechSession.Source.DEVICE_CMD) {
            runCatching { speechAi.player.stop() }
                .onFailure { Timber.tag(TAG).w(it, "force stop player") }
        }
        releaseTrack()
        clearLocalRouting()
    }

    override fun onStart() {
        if (!sessionActive) return
        Timber.tag(TAG).i("start mode=%s", mode)
        when (mode) {
            WKSpeechSession.Source.DEVICE_CMD -> {
                releaseTrack()
                speechAi.player.start(SAMPLE_RATE, CHANNELS)
            }
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> startLocalTrack()
        }
    }

    override fun onWrite(data: ByteArray) {
        if (!sessionActive) return
        if (data.isEmpty()) return
        when (mode) {
            WKSpeechSession.Source.DEVICE_CMD -> {
                runCatching { speechAi.player.write(data, isFinal = false) }
                    .onFailure { Timber.tag(TAG).w(it, "player write") }
            }
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> {
                val track = audioTrack ?: return
                val amplified = TtsAudioPlayer.applyVolumeGain(data, 2f)
                track.write(amplified, 0, amplified.size)
            }
        }
    }

    override fun onStop() {
        if (!sessionActive) return
        Timber.tag(TAG).i("stop mode=%s", mode)
        when (mode) {
            WKSpeechSession.Source.DEVICE_CMD -> {
                runCatching {
                    try {
                        speechAi.player.write(byteArrayOf(), isFinal = true)
                    } finally {
                        speechAi.player.stop()
                    }
                }
            }
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> {
                audioTrack?.let { track ->
                    runCatching { track.pause() }
                    runCatching { track.flush() }
                    runCatching { track.stop() }
                }
                releaseTrack()
            }
        }
    }

    /** 暂停本地 AudioTrack 输出（Translate 设备 PAUSE）；不结束播放队列。 */
    fun pause() {
        if (!sessionActive) return
        when (mode) {
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> audioTrack?.runCatching { pause() }
            WKSpeechSession.Source.DEVICE_CMD -> Unit
        }
    }

    /** 恢复本地 AudioTrack 输出（Translate 设备 RESUME/START）。 */
    fun resume() {
        if (!sessionActive) return
        when (mode) {
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> {
                val track = audioTrack
                if (track == null) {
                    if (isStarted()) startLocalTrack()
                } else {
                    track.play()
                }
            }
            WKSpeechSession.Source.DEVICE_CMD -> Unit
        }
    }

    private fun startLocalTrack() {
        releaseTrack()
        applyLocalRouting(mode)
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2 * 2 / 10)
        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(audioAttributesFor(mode))
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                streamTypeFor(mode),
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize,
                AudioTrack.MODE_STREAM,
            )
        }
        preferOutputDevice(track, mode)
        track.play()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            track.setVolume(1.0f)
        } else {
            @Suppress("DEPRECATION")
            track.setStereoVolume(1.0f, 1.0f)
        }
        audioTrack = track
        Timber.tag(TAG).i(
            "local track mode=%s audioMode=%d speaker=%s preferred=%s",
            mode,
            audioManager.mode,
            audioManager.isSpeakerphoneOn,
            track.preferredDevice?.type,
        )
    }

    /**
     * 蓝牙已连接时，仅设 streamType / speakerphone 不够：媒体流仍会走 A2DP。
     * 与 PhoneMicReader 一样，用 [AudioTrack.setPreferredDevice] 钉死输出设备。
     */
    private fun preferOutputDevice(track: AudioTrack, mode: WKSpeechSession.Source) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val targetType = when (mode) {
            WKSpeechSession.Source.PHONE_MIC -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            WKSpeechSession.Source.DEVICE_SCO -> AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            WKSpeechSession.Source.DEVICE_CMD -> return
        }
        val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == targetType }
        if (device == null) {
            Timber.tag(TAG).w("output device type=%d not found", targetType)
            return
        }
        if (!track.setPreferredDevice(device)) {
            Timber.tag(TAG).w("setPreferredDevice failed type=%d", targetType)
        }
    }

    private fun applyLocalRouting(mode: WKSpeechSession.Source) {
        when (mode) {
            WKSpeechSession.Source.PHONE_MIC -> {
                // 与 PhoneMicReader 同一条通话通路，才能让系统 AEC 把扬声器回声从麦里消掉。
                // 仍要关掉残留 SCO，并用 preferred / communication device 钉死外放。
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speaker != null && audioManager.setCommunicationDevice(speaker)) {
                        preferredCommunicationDevice = speaker
                    }
                }
            }
            WKSpeechSession.Source.DEVICE_SCO -> {
                // ScoReader 已 startBluetoothSco；保持通话路由到耳机，并关掉扬声器
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val sco = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                    if (sco != null && audioManager.setCommunicationDevice(sco)) {
                        preferredCommunicationDevice = sco
                    }
                }
            }
            WKSpeechSession.Source.DEVICE_CMD -> Unit
        }
    }

    private fun clearLocalRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && preferredCommunicationDevice != null) {
            runCatching { audioManager.clearCommunicationDevice() }
            preferredCommunicationDevice = null
        }
        when (mode) {
            WKSpeechSession.Source.PHONE_MIC -> {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            WKSpeechSession.Source.DEVICE_SCO -> {
                // SCO 链路由 ScoReader 在会话结束时关闭；通话模式仍需在此恢复
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            WKSpeechSession.Source.DEVICE_CMD -> Unit
        }
    }

    private fun streamTypeFor(mode: WKSpeechSession.Source): Int {
        return when (mode) {
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> AudioManager.STREAM_VOICE_CALL
            WKSpeechSession.Source.DEVICE_CMD -> AudioManager.STREAM_MUSIC
        }
    }

    private fun audioAttributesFor(mode: WKSpeechSession.Source): AudioAttributes {
        return when (mode) {
            WKSpeechSession.Source.PHONE_MIC,
            WKSpeechSession.Source.DEVICE_SCO,
                -> AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            WKSpeechSession.Source.DEVICE_CMD -> AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        }
    }

    private fun releaseTrack() {
        audioTrack?.let { track ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching { track.setPreferredDevice(null) }
            }
            runCatching { track.release() }
        }
        audioTrack = null
    }

    private const val TAG = "MyAudioPlayer"
    private const val SAMPLE_RATE = 16000
    private const val CHANNELS = 1
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
}
