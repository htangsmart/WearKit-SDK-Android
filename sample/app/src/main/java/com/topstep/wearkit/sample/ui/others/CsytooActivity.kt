package com.topstep.wearkit.sample.ui.others

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.aibuds.earphone.ABMateDeviceRepository
import com.topstep.aibuds.earphone.ABMateManager
import com.topstep.wearkit.abmate.apis.AbMateSDK
import com.topstep.wearkit.apis.model.WKBattery
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityCsytooBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

/**
 * 此页面仅为“Csytoo”客户演示功能
 */
class CsytooActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var abMateSDK: AbMateSDK
    private lateinit var manager: ABMateManager
    private lateinit var repository: ABMateDeviceRepository
    private lateinit var viewBind: ActivityCsytooBinding
    private var observeBatteryDisposable: Disposable? = null
    private var requestBatteryDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityCsytooBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Csytoo"
        val sdk = wearKit.getRawSDK() as? AbMateSDK
        if (sdk == null) {
            toast("当前不是 AbMate 设备")
            finish()
            return
        }
        abMateSDK = sdk
        manager = abMateSDK.abMateManager
        repository = manager.getRepository()
        //1.固件版本
        repository.deviceFirmwareVersion.observe(this) { version ->
            if (version == null) return@observe
            val summary = viewBind.itemFirmwareVersion.getSummaryView()
            summary.isVisible = true
            summary.text = formatFirmwareVersion(version)
        }

        //2.获取BLE Mac地址
        //不需要，就是当前设备地址

        //3.获取/监听电量信息
        observeBatteryDisposable = wearKit.batteryAbility.observeBatteryChange()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                showBattery(it)
            }, {
                Timber.w(it)
            })
        viewBind.itemBattery.clickTrigger {
            requestBatteryDisposable?.dispose()
            requestBatteryDisposable = wearKit.batteryAbility.requestBattery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showBattery(it)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        //4.获取当前场景档
        manager.requireDeviceAncMode()//刷新
        repository.deviceAncMode.observe(this) { value ->
            val summary = viewBind.itemSceneMode.getSummaryView()
            summary.isVisible = true
            summary.text = ancModeName(value)
        }

        //5.设置场景阈值
        viewBind.itemSceneThreshold.clickTrigger {
            showAncLevelDialog()
        }

        //6.眼镜佩戴信息
        repository.deviceInEarStatus.observe(this) {
            val summary = viewBind.itemWearState.getSummaryView()
            summary.isVisible = true
            summary.text = when (it) {
                true -> "佩戴中"
                false -> "摘下"
                else -> null
            }
        }

        //7.眼镜唯一标识
        repository.glassSn.observe(this) {
            val summary = viewBind.itemSerialNumber.getSummaryView()
            summary.isVisible = true
            summary.text = it.toString()
        }

        //8.录音事件
        repository.glassWorkState.observe(this) {
            val summary = viewBind.itemRecordState.getSummaryView()
            summary.isVisible = true
            summary.text = when (it) {
                12 -> "录音中"
                else -> "未开启"
            }
        }

        //9.电话状态上报
        manager.requireCallState()
        repository.callState.observe(this) {
            val summary = viewBind.itemCallState.getSummaryView()
            summary.isVisible = true
            summary.text = when (it) {
                0 -> "未在通话"
                1 -> "响铃中"
                2 -> "正在通话"
                3 -> "三方通话响铃中"
                4 -> "AI对话中"
                else -> ""
            }
        }

        //10.眼镜音量。设备范围 0-100，界面按 0-16 显示和选择。
        repository.deviceVolume.observe(this) { volume ->
            if (volume == null) return@observe
            val summary = viewBind.itemVolume.getSummaryView()
            summary.isVisible = true
            summary.text = deviceVolumeToLevel(volume).toString()
        }
        viewBind.itemVolume.clickTrigger {
            showVolumeDialog()
        }

        //11.自定义按键。两个条目分别设置固定操作，列表展示设备当前全部按键。
        repository.deviceKeySettings.observe(this) { settings ->
            viewBind.tvKeyList.text = formatKeySettings(settings)
        }
        viewBind.itemKeySet1.clickTrigger {
            manager.setEarOperation(0x10, 3) { success ->
                toast(if (success) "左触摸板短按已设为上一曲" else getString(R.string.tip_failed))
            }
        }
        viewBind.itemKeySet2.clickTrigger {
            manager.setEarOperation(0x11, 4) { success ->
                toast(if (success) "右触摸板短按已设为下一曲" else getString(R.string.tip_failed))
            }
        }

        //12.控制音乐。0播放 1停止 2上一首 3下一首
        viewBind.itemMusicPlay.clickTrigger {
            manager.controlMusic(0) { success ->
                toast(if (success) "已播放" else getString(R.string.tip_failed))
            }
        }
        viewBind.itemMusicPause.clickTrigger {
            manager.controlMusic(1) { success ->
                toast(if (success) "已暂停" else getString(R.string.tip_failed))
            }
        }
        viewBind.itemMusicPrevious.clickTrigger {
            manager.controlMusic(2) { success ->
                toast(if (success) "已切换到上一首" else getString(R.string.tip_failed))
            }
        }
        viewBind.itemMusicNext.clickTrigger {
            manager.controlMusic(3) { success ->
                toast(if (success) "已切换到下一首" else getString(R.string.tip_failed))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeBatteryDisposable?.dispose()
        requestBatteryDisposable?.dispose()
    }

    private fun showAncLevelDialog() {
        val ncInput = EditText(this).apply {
            hint = "降噪阈值"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val transparencyInput = EditText(this).apply {
            hint = "通透阈值"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
            addView(ncInput)
            addView(transparencyInput)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("设置场景阈值")
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val ncLevel = ncInput.text.toString().toIntOrNull()
                val transparencyLevel = transparencyInput.text.toString().toIntOrNull()
                if (ncLevel == null || transparencyLevel == null) {
                    toast(R.string.tip_failed)
                    return@setPositiveButton
                }
                manager.setAncLevel(ncLevel, transparencyLevel) { success ->
                    toast(if (success) "场景阈值已设置" else getString(R.string.tip_failed))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showVolumeDialog() {
        val current = repository.deviceVolume.value?.let { deviceVolumeToLevel(it) } ?: -1
        MaterialAlertDialogBuilder(this)
            .setTitle("眼镜音量")
            .setSingleChoiceItems(VOLUME_LEVELS, current) { dialog, which ->
                manager.setVolume(levelToDeviceVolume(which)) { success ->
                    toast(if (success) "音量已设置" else getString(R.string.tip_failed))
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun formatKeySettings(settings: Map<Int, Int>?): String {
        if (settings.isNullOrEmpty()) {
            return "暂无按键设置"
        }
        return settings.entries
            .sortedBy { it.key }
            .joinToString("\n") { (type, function) ->
                "${keyTypeName(type)}：${keyFunctionName(function)}"
            }
    }

    private fun showBattery(battery: WKBattery) {
        val summary = viewBind.itemBattery.getSummaryView()
        summary.isVisible = true
        summary.text = if (battery.isCharging) {
            "${battery.percentage}% 充电中"
        } else {
            "${battery.percentage}%"
        }
    }

    private companion object {
        const val VOLUME_LEVEL_MAX = 16
        val VOLUME_LEVELS: Array<String> = Array(VOLUME_LEVEL_MAX + 1) { it.toString() }

        fun deviceVolumeToLevel(volume: Byte): Int {
            val percent = (volume.toInt() and 0xFF).coerceIn(0, 100)
            return (percent * VOLUME_LEVEL_MAX + 50) / 100
        }

        fun levelToDeviceVolume(level: Int): Int {
            val safeLevel = level.coerceIn(0, VOLUME_LEVEL_MAX)
            return (safeLevel * 100 + VOLUME_LEVEL_MAX / 2) / VOLUME_LEVEL_MAX
        }

        /** 原始值为 v1.v2.v3.v4，展示时只去掉开头一次 "0."。0.1.2.6 → 1.2.6 */
        fun formatFirmwareVersion(version: Int): String {
            val text = listOf(24, 16, 8, 0).joinToString(".") { shift ->
                ((version ushr shift) and 0xFF).toString()
            }
            return text.removePrefix("0.")
        }

        val KEY_TYPE_NAMES = mapOf(
            0x10 to "左触摸板短按",
            0x11 to "右触摸板短按",
            0x12 to "左触摸板双击",
            0x13 to "右触摸板双击",
            0x14 to "左触摸板长按",
            0x15 to "右触摸板长按",
            0x16 to "左按键1短按",
            0x17 to "右按键1短按",
            0x18 to "左按键1双击",
            0x19 to "右按键1双击",
            0x1a to "左按键1长按",
            0x1b to "右按键1长按",
            0x1c to "左按键2短按",
            0x1d to "右按键2短按",
            0x1e to "左按键2双击",
            0x1f to "右按键2双击",
            0x20 to "左按键2长按",
            0x21 to "右按键2长按",
            0x22 to "触摸板从后向前滑动",
            0x23 to "触摸板从前向后滑动",
            0x24 to "触摸板(右)后向前滑动",
            0x25 to "触摸板(右)从前向后滑动",
        )

        val KEY_FUNCTION_NAMES = mapOf(
            0 to "无功能",
            1 to "回拨电话",
            2 to "语音助手",
            3 to "上一曲",
            4 to "下一曲",
            5 to "音量+",
            6 to "音量-",
            7 to "播放暂停",
            8 to "游戏模式",
            9 to "降噪设置",
            10 to "拍照",
            11 to "连续拍照",
            12 to "录像启动停止",
            13 to "录音启动停止",
            14 to "本地播放/蓝牙播放切换",
            15 to "进入/退出私语模式",
        )

        fun keyTypeName(type: Int): String {
            return KEY_TYPE_NAMES[type] ?: "按键 0x${type.toString(16)}"
        }

        fun keyFunctionName(function: Int): String {
            return KEY_FUNCTION_NAMES[function] ?: "功能 $function"
        }

        fun ancModeName(mode: Byte?): String {
            return when (mode?.toInt()) {
                0 -> "关闭"
                1 -> "降噪"
                2 -> "通透"
                else -> "未知($mode)"
            }
        }
    }

}