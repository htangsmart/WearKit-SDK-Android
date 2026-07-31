package com.topstep.wearkit.sample.ui.ai

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.topstep.aikit.AiKit
import com.topstep.aikit.eyeear.EyeEarKit
import com.topstep.wearkit.sample.BuildConfig
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivitySpeechAiBinding
import com.topstep.wearkit.sample.ui.ai.handler.SpeechAiHandler
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import com.topstep.wearkit.sample.ui.ai.wav.SpeechRecordSaver
import com.topstep.wearkit.sample.ui.base.BaseActivity
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeechAiActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySpeechAiBinding
    private var aikit: AiKit? = null
    private var mediaPlayer: MediaPlayer? = null
    private var handler: SpeechAiHandler? = null
    private var playingRecordFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySpeechAiBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Speech+Ai"

        viewBind.btnPlayLastAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true && playingRecordFile == null) {
                stopPlayback()
                toast("已停止")
            } else {
                playDebugAudio()
            }
        }

        refreshRecordList()

        val speechAi = wearKit.speechAiAbility
        if (!speechAi.isSupport()) {
            toast("UnSupport!")
            return
        }

        val aikit = EyeEarKit(this)
        aikit.init(
            // 参数来自主仓库 secrets/aikit.local.properties，经 BuildConfig 注入
            params = AiKit.InitParams(
                channel = BuildConfig.AIKIT_CHANNEL,
                macAddress = BuildConfig.AIKIT_MAC_ADDRESS,
                customPrompt = BuildConfig.AIKIT_CUSTOM_PROMPT
            ),
            handler = object : AiKit.InitHandler {
                override fun onInitFail() {
                    this@SpeechAiActivity.aikit = null
                    handler?.release()
                    handler = null
                }

                override fun onInitSuccess() {
                    this@SpeechAiActivity.aikit = aikit
                    handler?.release()
                    handler = SpeechAiHandler(
                        context = this@SpeechAiActivity,
                        speechAi = speechAi,
                        aiKit = aikit,
                        onRecordSaved = {
                            runOnUiThread { refreshRecordList() }
                        },
                    ).also { it.start() }
                }

                override fun receiveInitData(bytes: ByteArray) {
                    //do nothing
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        refreshRecordList()
    }

    private fun refreshRecordList() {
        val files = SpeechRecordSaver.latestRecords(this, 3)
        viewBind.llRecords.removeAllViews()
        viewBind.tvRecordsEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE

        val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        files.forEachIndexed { index, file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, if (index == 0) 0 else 12, 0, 0)
            }
            val info = TextView(this).apply {
                text = "${index + 1}. ${file.name}\n" +
                        "${timeFormat.format(Date(file.lastModified()))}  ${file.length()} bytes\n" +
                        file.absolutePath
                textSize = 12f
                setTextColor(0xFF666666.toInt())
            }
            val playBtn = Button(this).apply {
                text = if (mediaPlayer?.isPlaying == true && playingRecordFile == file) {
                    "停止播放"
                } else {
                    "播放"
                }
                setOnClickListener {
                    if (mediaPlayer?.isPlaying == true && playingRecordFile == file) {
                        stopPlayback()
                        toast("已停止")
                    } else {
                        playRecordFile(file)
                    }
                }
            }
            row.addView(info)
            row.addView(playBtn)
            viewBind.llRecords.addView(row)
        }
    }

    private fun playRecordFile(file: File) {
        stopPlayback()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    toast("播放完成")
                }
                setOnErrorListener { _, what, extra ->
                    Timber.w("MediaPlayer error what=%d extra=%d", what, extra)
                    toast("播放失败")
                    stopPlayback()
                    true
                }
                prepare()
                start()
            }
            playingRecordFile = file
            refreshRecordList()
            toast("开始播放")
        }.onFailure {
            Timber.w(it, "play failed: %s", file.absolutePath)
            toast("播放失败: ${it.message}")
            stopPlayback()
        }
    }

    private fun playDebugAudio() {
        refreshDebugAudioHint()
        val file = SaveWavForDebug.latestRecordFile(this)
        if (file == null) {
            toast("暂无 Debug 录音文件")
            return
        }
        if (file.name.endsWith(".pcm", true)) {
            toast("当前为 pcm，请改为 wav 后再播放")
            return
        }
        stopPlayback()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    toast("播放完成")
                }
                setOnErrorListener { _, what, extra ->
                    Timber.w("MediaPlayer error what=%d extra=%d", what, extra)
                    toast("播放失败")
                    stopPlayback()
                    true
                }
                prepare()
                start()
            }
            playingRecordFile = null
            viewBind.btnPlayLastAudio.text = "停止播放"
            viewBind.tvAudioFile.text = "正在播放: ${file.name}\n${file.absolutePath}"
            toast("开始播放")
        }.onFailure {
            Timber.w(it, "play failed: %s", file.absolutePath)
            toast("播放失败: ${it.message}")
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playingRecordFile = null
        viewBind.btnPlayLastAudio.text = "播放最近 Debug 录音"
        refreshDebugAudioHint()
        refreshRecordList()
    }

    private fun refreshDebugAudioHint() {
        val file = SaveWavForDebug.latestRecordFile(this)
        viewBind.tvAudioFile.text = if (file == null) {
            "最近 Debug 录音: 无"
        } else {
            "最近 Debug 录音: ${file.name} (${file.length()} bytes)\n${file.absolutePath}"
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
