package com.topstep.wearkit.sample.ui.others

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.b2b.UGreenConfig
import com.topstep.wearkit.apis.model.b2b.UGreenDanMu
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityUgreenBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class UGreenActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityUgreenBinding
    private var disposable: Disposable? = null
    private var lastConfig: UGreenConfig? = null

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

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

}
