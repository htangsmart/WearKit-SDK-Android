package com.topstep.wearkit.sample.ui.others

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.b2b.UGreenConfig
import com.topstep.wearkit.apis.model.b2b.UGreenDanMu
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityUgreenBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UGreenActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityUgreenBinding
    private var disposable: Disposable? = null
    private var playDisposable: Disposable? = null
    private var lastConfig: UGreenConfig? = null
    private val playStopRequested = AtomicBoolean(false)

    /** Bumped on each start/stop so a late finally{} cannot stop a newer play session. */
    private val playSession = AtomicInteger(0)

    private val selectAudioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK) {
            if (uri == null) {
                toast("Select file error!")
            } else {
                startPlayOpus(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityUgreenBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "UGreen"

        viewBind.itemUgreenAddMine.clickTrigger {
            addUGreenDanMu(UGreenDanMu.Type.MINE)
        }
        viewBind.itemUgreenAddFriend.clickTrigger {
            addUGreenDanMu(UGreenDanMu.Type.FRIEND)
        }
        viewBind.itemUgreenClearAll.clickTrigger {
            executeDanMu("Clear all danmu") {
                wearKit.b2b.ugreenAbility.clearDanMu(UGreenDanMu.Clear.ALL)
            }
        }

        viewBind.itemRequestConfig.clickTrigger {
            requestConfig()
        }
        viewBind.itemDeskMode.clickTrigger {
            withCurrentConfig { config ->
                val items = arrayOf("Off", "On")
                val checked = if (config.deskModeEnabled) 1 else 0
                MaterialAlertDialogBuilder(this)
                    .setTitle("Desk Mode")
                    .setSingleChoiceItems(items, checked) { dialog, which ->
                        dialog.dismiss()
                        updateConfig(config.copy(deskModeEnabled = which == 1), "Desk mode")
                    }
                    .show()
            }
        }
        viewBind.itemKeyFunction.clickTrigger {
            withCurrentConfig { config ->
                val items = arrayOf("Live Record", "Voice Assistant")
                val checked = if (config.keyFunction == UGreenConfig.KeyFunction.VOICE_ASSISTANT) 1 else 0
                MaterialAlertDialogBuilder(this)
                    .setTitle("Key Function")
                    .setSingleChoiceItems(items, checked) { dialog, which ->
                        dialog.dismiss()
                        val value = if (which == 1) {
                            UGreenConfig.KeyFunction.VOICE_ASSISTANT
                        } else {
                            UGreenConfig.KeyFunction.RECORD
                        }
                        updateConfig(config.copy(keyFunction = value), "Key function")
                    }
                    .show()
            }
        }
        viewBind.itemKeepScreenOn.clickTrigger {
            withCurrentConfig { config ->
                val items = arrayOf("Music", "Status")
                val checked = booleanArrayOf(
                    config.keepScreenOnPages and UGreenConfig.KeepScreenOnPage.MUSIC != 0,
                    config.keepScreenOnPages and UGreenConfig.KeepScreenOnPage.STATUS != 0,
                )
                MaterialAlertDialogBuilder(this)
                    .setTitle("Keep Screen On")
                    .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        var pages = 0
                        if (checked[0]) pages = pages or UGreenConfig.KeepScreenOnPage.MUSIC
                        if (checked[1]) pages = pages or UGreenConfig.KeepScreenOnPage.STATUS
                        updateConfig(config.copy(keepScreenOnPages = pages), "Keep screen on")
                    }
                    .show()
            }
        }
        viewBind.itemLyricTheme.clickTrigger {
            withCurrentConfig { config ->
                val items = arrayOf("Custom (0)", "1", "2", "3", "4")
                val checked = config.lyricTheme.coerceIn(0, 4)
                MaterialAlertDialogBuilder(this)
                    .setTitle("Lyric Theme")
                    .setSingleChoiceItems(items, checked) { dialog, which ->
                        dialog.dismiss()
                        updateConfig(config.copy(lyricTheme = which), "Lyric theme")
                    }
                    .show()
            }
        }
        viewBind.itemLyricColor.clickTrigger {
            withCurrentConfig { config ->
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
                val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
                MaterialAlertDialogBuilder(this)
                    .setTitle("Lyric Color")
                    .setView(view)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        updateConfig(config.copy(lyricColor = colorPickerView.selectedColor), "Lyric color")
                    }
                    .show()
                Unit
            }
        }
        viewBind.itemPlayOpus.clickTrigger {
            if (playDisposable?.isDisposed == false) {
                stopPlayOpus()
            } else {
                openAudioPicker()
            }
        }
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "*/*"))
        }
        try {
            selectAudioLauncher.launch(intent)
        } catch (e: Exception) {
            Timber.w(e)
            toast("Select file error!")
        }
    }

    private fun startPlayOpus(uri: Uri) {
        val player = wearKit.speechAiAbility.player
        if (!player.isSupport(WKSpeechSession.Scene.CHAT)) {
            toast(R.string.tip_un_support)
            return
        }
        stopPlayOpus()
        val session = playSession.incrementAndGet()
        playStopRequested.set(false)
        setPlayingUi(true)
        toast("Play Opus…")
        playDisposable = Completable.fromAction {
            // Decode once, then loop the PCM buffer. Streaming MediaCodec rewind after AAC EOS
            // often stalls on short MP4 clips.
            val pcmAll = Pcm16kMonoDecoder.decodeAll(this, uri)
            if (pcmAll.isEmpty()) error("Decoded PCM is empty")
            if (playStopRequested.get() || playSession.get() != session) return@fromAction
            Timber.i(
                "Play Opus pcmBytes=%d (~%dms @16k mono)",
                pcmAll.size,
                pcmAll.size * 1000L / (Pcm16kMonoDecoder.TARGET_SAMPLE_RATE * 2),
            )
            player.start(
                Pcm16kMonoDecoder.TARGET_SAMPLE_RATE,
                Pcm16kMonoDecoder.TARGET_CHANNELS,
            )
            try {
                var offset = 0
                while (!playStopRequested.get() && playSession.get() == session) {
                    val remain = pcmAll.size - offset
                    val n = minOf(PCM_WRITE_CHUNK_BYTES, remain) and 0x7FFFFFFE
                    if (n <= 0) {
                        offset = 0
                        continue
                    }
                    val pcm = pcmAll.copyOfRange(offset, offset + n)
                    try {
                        player.write(pcm, isFinal = false)
                    } catch (e: Throwable) {
                        // stop()/dispose() interrupts blocking write — treat as normal stop.
                        if (playStopRequested.get() || playSession.get() != session || isWriteInterrupted(e)) {
                            Thread.interrupted()
                            break
                        }
                        throw e
                    }
                    offset += n
                    if (offset >= pcmAll.size) {
                        offset = 0
                    }
                }
            } finally {
                if (playSession.get() == session) {
                    runCatching { player.stop() }
                        .onFailure { Timber.w(it, "player stop") }
                }
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (playSession.get() == session) {
                    setPlayingUi(false)
                    toast("Play Opus stopped")
                }
            }, {
                if (playSession.get() != session || playStopRequested.get() || isWriteInterrupted(it)) {
                    if (playSession.get() == session) {
                        setPlayingUi(false)
                        toast("Play Opus stopped")
                    }
                } else {
                    setPlayingUi(false)
                    Timber.w(it)
                    toast(it.message ?: it.stackTraceToString())
                }
            })
    }

    private fun stopPlayOpus() {
        playStopRequested.set(true)
        playSession.incrementAndGet()
        runCatching { wearKit.speechAiAbility.player.stop() }
        // Dispose may interrupt blocking write; fromAction catches that as a normal stop.
        playDisposable?.dispose()
        playDisposable = null
        setPlayingUi(false)
    }

    private fun isWriteInterrupted(error: Throwable): Boolean {
        var cur: Throwable? = error
        while (cur != null) {
            if (cur is InterruptedException) return true
            if (cur is IllegalStateException &&
                cur.message?.contains("interrupted", ignoreCase = true) == true
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    private fun setPlayingUi(playing: Boolean) {
        if (playing) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewBind.itemPlayOpus.getTitleView().text = "Stop Opus"
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewBind.itemPlayOpus.getTitleView().text = "Play Opus"
        }
    }

    private fun addUGreenDanMu(@UGreenDanMu.Type type: Int) {
        val isMine = type == UGreenDanMu.Type.MINE
        val item = UGreenDanMu(
            type = type,
            text = if (isMine) "我的弹幕" else "好友弹幕",
            color = if (isMine) Color.CYAN else Color.MAGENTA,
            fontSizePx = 32,
            animation = if (isMine) UGreenDanMu.Animation.HEART else UGreenDanMu.Animation.NONE,
            walkSpeedPxPerSec = 60,
        )
        val successMsg = if (isMine) "Add mine danmu" else "Add friend danmu"
        executeDanMu(successMsg) {
            wearKit.b2b.ugreenAbility.addDanMu(listOf(item))
        }
    }

    private fun executeDanMu(successMsg: String, action: () -> Completable) {
        if (!wearKit.b2b.ugreenAbility.compat.isSupportDanMu()) {
            toast(R.string.tip_un_support)
            return
        }
        disposable?.dispose()
        disposable = action()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(successMsg)
            }, {
                Timber.w(it)
                toast(it.stackTraceToString())
            })
    }

    private fun withCurrentConfig(block: (UGreenConfig) -> Unit) {
        val cached = lastConfig
        if (cached != null) {
            block(cached)
            return
        }
        disposable?.dispose()
        disposable = wearKit.b2b.ugreenAbility.requestConfig()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                lastConfig = it
                showConfig(it)
                block(it)
            }, {
                Timber.w(it)
                toast(it.stackTraceToString())
            })
    }

    private fun requestConfig() {
        disposable?.dispose()
        disposable = wearKit.b2b.ugreenAbility.requestConfig()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                lastConfig = it
                showConfig(it)
                toast("Request config")
            }, {
                Timber.w(it)
                toast(it.stackTraceToString())
            })
    }

    private fun updateConfig(next: UGreenConfig, successMsg: String) {
        disposable?.dispose()
        disposable = wearKit.b2b.ugreenAbility.setConfig(next)
            .andThen(wearKit.b2b.ugreenAbility.requestConfig())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                lastConfig = it
                showConfig(it)
                toast(successMsg)
            }, {
                Timber.w(it)
                toast(it.stackTraceToString())
            })
    }

    private fun showConfig(config: UGreenConfig) {
        val pages = buildList {
            if (config.keepScreenOnPages and UGreenConfig.KeepScreenOnPage.MUSIC != 0) add("MUSIC")
            if (config.keepScreenOnPages and UGreenConfig.KeepScreenOnPage.STATUS != 0) add("STATUS")
        }.ifEmpty { listOf("NONE") }.joinToString()
        val key = if (config.keyFunction == UGreenConfig.KeyFunction.VOICE_ASSISTANT) {
            "VOICE_ASSISTANT"
        } else {
            "RECORD"
        }
        viewBind.tvConfig.text = buildString {
            append("deskMode=").append(config.deskModeEnabled).append('\n')
            append("keyFunction=").append(key).append('\n')
            append("keepScreenOn=").append(pages).append('\n')
            append("lyricTheme=").append(config.lyricTheme).append('\n')
            append("lyricColor=#").append("%08X".format(config.lyricColor))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (playDisposable?.isDisposed == false) {
            toast("Playing… tap Stop Opus first")
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onDestroy() {
        stopPlayOpus()
        disposable?.dispose()
        super.onDestroy()
    }

    companion object {
        /** ~20 ms @ 16 kHz mono s16le */
        private const val PCM_WRITE_CHUNK_BYTES = 640
    }
}
