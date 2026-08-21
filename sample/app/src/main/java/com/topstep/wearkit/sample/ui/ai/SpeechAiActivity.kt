package com.topstep.wearkit.sample.ui.ai

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.topstep.wearkit.apis.model.speech.WKChatTranslateMode
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySpeechAiBinding
import com.topstep.wearkit.sample.ui.ai.chat.ChatActivity
import com.topstep.wearkit.sample.ui.ai.chattranslate.ChatTranslateActivity
import com.topstep.wearkit.sample.ui.ai.chattranslate.ChatTranslateTranscript
import com.topstep.wearkit.sample.ui.ai.debug.SettingDebugScoActivity
import com.topstep.wearkit.sample.ui.ai.record.RecordActivity
import com.topstep.wearkit.sample.ui.ai.translate.TranslateActivity
import com.topstep.wearkit.sample.ui.ai.wav.SaveWavForDebug
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import timber.log.Timber

class SpeechAiActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySpeechAiBinding
    private var mediaPlayer: MediaPlayer? = null
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySpeechAiBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Speech+Ai"

        val speechAi = wearKit.speechAiAbility
        if (!speechAi.isSupport()) {
            toast("设备 SpeechAi 不支持")
            viewBind.tvAikitState.setText(R.string.tip_un_support)
            return
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                SpeechAiManager.state.collect {
                    bindAiKit(it)
                }
            }
        }

        viewBind.btnChat.setOnClickListener {
            if (!speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT)
                && !speechAi.session.isSupportAppScene(WKSpeechSession.Scene.CHAT)
            ) {
                toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            if (SpeechAiManager.state.value == SpeechAiManager.State.READY) {
                startActivity(Intent(this, ChatActivity::class.java))
            }
        }

        viewBind.btnRecord.setOnClickListener {
            if (!speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.RECORD)
                && !speechAi.session.isSupportAppScene(WKSpeechSession.Scene.RECORD)
            ) {
                toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            if (SpeechAiManager.state.value == SpeechAiManager.State.READY) {
                startActivity(Intent(this, RecordActivity::class.java))
            }
        }

        viewBind.btnTranslate.setOnClickListener {
            if (!speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.TRANSLATE)
                && !speechAi.session.isSupportAppScene(WKSpeechSession.Scene.TRANSLATE)
            ) {
                toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            if (SpeechAiManager.state.value == SpeechAiManager.State.READY) {
                startActivity(Intent(this, TranslateActivity::class.java))
            }
        }

        viewBind.btnChatTranslate.setOnClickListener {
            if (!isChatTranslateSupported(speechAi.session)) {
                toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            if (SpeechAiManager.state.value != SpeechAiManager.State.READY) {
                toast(R.string.ds_speech_init)
                return@setOnClickListener
            }
            showChatTranslateModeDialog()
        }

        viewBind.btnPlayLastAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                stopPlayback()
                toast("已停止")
            } else {
                playDebugAudio()
            }
        }

        refreshDebugAudioHint()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_speech_ai, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_debug_sco -> {
                startActivity(Intent(this, SettingDebugScoActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isChatTranslateSupported(session: com.topstep.wearkit.apis.ability.speech.WKSpeechAiAbility.Session): Boolean {
        return session.isSupportAppScene(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF)
            || session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF)
            || session.isSupportAppScene(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER)
            || session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER)
    }

    private fun showChatTranslateModeDialog() {
        val modes = arrayOf(
            getString(R.string.ds_speech_ct_mode_face),
            getString(R.string.ds_speech_ct_mode_private),
            getString(R.string.ds_speech_ct_mode_portable),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.ds_speech_chat_translate)
            .setItems(modes) { _, which ->
                val mode = when (which) {
                    0 -> WKChatTranslateMode.FACE_TO_FACE
                    1 -> WKChatTranslateMode.PRIVATE
                    else -> WKChatTranslateMode.PORTABLE
                }
                startChatTranslate(mode)
            }
            .show()
    }

    private fun startChatTranslate(mode: WKChatTranslateMode) {
        disposables.add(
            wearKit.speechAiAbility.translate.startChatTranslate(mode)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (isDestroyed || isFinishing) return@subscribe
                    ChatTranslateTranscript.activeMode = mode
                    startActivity(
                        Intent(this, ChatTranslateActivity::class.java)
                            .putExtra(ChatTranslateActivity.EXTRA_MODE, mode.name),
                    )
                }, {
                    Timber.w(it, "startChatTranslate failed")
                    if (!isDestroyed) {
                        toast(R.string.tip_failed)
                    }
                })
        )
    }

    private fun bindAiKit(state: SpeechAiManager.State) {
        when (state) {
            SpeechAiManager.State.IDLE -> Unit
            SpeechAiManager.State.INITIALIZING -> {
                viewBind.tvAikitState.setText(R.string.ds_speech_init)
            }
            SpeechAiManager.State.READY -> {
                viewBind.tvAikitState.setText(R.string.ds_speech_aikit_ready)
            }
            SpeechAiManager.State.FAILED -> {
                viewBind.tvAikitState.setText(R.string.ds_speech_init_fail)
            }
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
        viewBind.btnPlayLastAudio.text = "播放最近 Debug 录音"
        refreshDebugAudioHint()
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
        disposables.clear()
        stopPlayback()
        super.onDestroy()
    }
}
