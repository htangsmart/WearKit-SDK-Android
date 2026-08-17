package com.topstep.wearkit.sample.ui.custom.sanag

import android.media.MediaPlayer
import android.os.Bundle
import com.topstep.aikit.AiKit
import com.topstep.aikit.eyeear.EyeEarKit
import com.topstep.wearkit.sample.BuildConfig
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagSpeechAiBinding
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import timber.log.Timber

/**
 * Sanag speech: device chat + last debug audio playback.
 */
class SanagSpeechAiActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySanagSpeechAiBinding
    private var aikit: AiKit? = null
    private var handler: SanagSpeechAiHandler? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySanagSpeechAiBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech)

        viewBind.btnPlayLastAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                stopPlayback()
                toast(R.string.ds_speech_stopped)
            } else {
                playDebugAudio()
            }
        }
        refreshDebugAudioHint()

        val speechAi = wearKit.speechAiAbility
        if (!speechAi.isSupport()) {
            viewBind.tvSpeechState.setText(R.string.tip_un_support)
            toast(R.string.tip_un_support)
            return
        }

        PermissionHelper.requestRecordAudio(this) { granted ->
            if (!granted) {
                viewBind.tvSpeechState.setText(R.string.ds_speech_need_record)
                toast(R.string.ds_speech_need_record)
                return@requestRecordAudio
            }
            initAiKit()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDebugAudioHint()
    }

    private fun initAiKit() {
        val speechAi = wearKit.speechAiAbility
        val aikit = EyeEarKit(this)
        aikit.init(
            params = AiKit.InitParams(
                channel = BuildConfig.AIKIT_CHANNEL,
                macAddress = BuildConfig.AIKIT_MAC_ADDRESS,
                customPrompt = BuildConfig.AIKIT_CUSTOM_PROMPT,
            ),
            handler = object : AiKit.InitHandler {
                override fun onInitFail() {
                    this@SanagSpeechAiActivity.aikit = null
                    handler?.release()
                    handler = null
                    runOnUiThread {
                        viewBind.tvSpeechState.setText(R.string.ds_speech_init_fail)
                    }
                }

                override fun onInitSuccess() {
                    this@SanagSpeechAiActivity.aikit = aikit
                    handler?.release()
                    handler = SanagSpeechAiHandler(
                        context = this@SanagSpeechAiActivity,
                        speechAi = speechAi,
                        aiKit = aikit,
                        onChatChanged = { chatting ->
                            runOnUiThread {
                                viewBind.tvSpeechState.setText(
                                    if (chatting) R.string.ds_speech_chatting else R.string.ds_speech_ready
                                )
                                refreshDebugAudioHint()
                            }
                        },
                    ).also { it.start() }
                    runOnUiThread {
                        viewBind.tvSpeechState.setText(R.string.ds_speech_ready)
                    }
                }

                override fun receiveInitData(bytes: ByteArray) {
                    // do nothing
                }
            },
        )
    }

    private fun playDebugAudio() {
        refreshDebugAudioHint()
        val file = SaveWavForDebug.latestRecordFile(this)
        if (file == null) {
            toast(R.string.ds_speech_no_audio)
            return
        }
        if (file.name.endsWith(".pcm", true)) {
            toast(R.string.ds_speech_pcm_unsupported)
            return
        }
        stopPlayback()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    toast(R.string.ds_speech_play_done)
                }
                setOnErrorListener { _, what, extra ->
                    Timber.w("MediaPlayer error what=%d extra=%d", what, extra)
                    toast(R.string.tip_failed)
                    stopPlayback()
                    true
                }
                prepare()
                start()
            }
            viewBind.btnPlayLastAudio.setText(R.string.ds_speech_stop)
            viewBind.tvAudioFile.text = getString(
                R.string.ds_speech_playing,
                file.name,
                file.absolutePath,
            )
        }.onFailure {
            Timber.w(it, "play failed: %s", file.absolutePath)
            toast(it.message ?: getString(R.string.tip_failed))
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        viewBind.btnPlayLastAudio.setText(R.string.ds_speech_play_last)
        refreshDebugAudioHint()
    }

    private fun refreshDebugAudioHint() {
        val file = SaveWavForDebug.latestRecordFile(this)
        viewBind.tvAudioFile.text = if (file == null) {
            getString(R.string.ds_speech_no_audio)
        } else {
            getString(R.string.ds_speech_audio_hint, file.name, file.length(), file.absolutePath)
        }
    }

    override fun onDestroy() {
        stopPlayback()
        handler?.release()
        handler = null
        super.onDestroy()
        aikit?.release()
        aikit = null
    }
}
