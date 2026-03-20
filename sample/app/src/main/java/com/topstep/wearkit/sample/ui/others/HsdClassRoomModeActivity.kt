package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdClassRoomMode
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdClassRoomModeBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Locale

class HsdClassRoomModeActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdClassRoomModeBinding

    private var startMinutes: Int = 8 * 60
    private var endMinutes: Int = 17 * 60

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdClassRoomModeBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "ClassRoom Mode"

        viewBind.itemStartTime.getTextView().text = formatMinutes(startMinutes)
        viewBind.itemEndTime.getTextView().text = formatMinutes(endMinutes)

        viewBind.itemStartTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = startMinutes,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEndTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = endMinutes,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }

        viewBind.btnRequest.clickTrigger {
            requestClassRoomMode()
        }
        viewBind.btnSave.clickTrigger {
            saveClassRoomMode()
        }
    }

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_START -> {
                startMinutes = timeMinute
                viewBind.itemStartTime.getTextView().text = formatMinutes(startMinutes)
            }
            DIALOG_END -> {
                endMinutes = timeMinute
                viewBind.itemEndTime.getTextView().text = formatMinutes(endMinutes)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun requestClassRoomMode() {
        wearKit.b2b.hsdAbility.requestClassRoomMode()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mode ->
                viewBind.itemEnabled.getSwitchView().isChecked = mode.isEnabled
                startMinutes = mode.start
                endMinutes = mode.end
                viewBind.itemStartTime.getTextView().text = formatMinutes(startMinutes)
                viewBind.itemEndTime.getTextView().text = formatMinutes(endMinutes)
                applyRepeatToUI(mode.repeat)
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun saveClassRoomMode() {
        val mode = HsdClassRoomMode(
            isEnabled = viewBind.itemEnabled.getSwitchView().isChecked,
            start = startMinutes,
            end = endMinutes,
            repeat = getRepeatFromUI(),
        )

        wearKit.b2b.hsdAbility.setClassRoomMode(mode)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun getRepeatFromUI(): Int {
        var repeat = 0
        if (viewBind.cbMon.isChecked) repeat = repeat or WeekRepeatFlag.MON
        if (viewBind.cbTue.isChecked) repeat = repeat or WeekRepeatFlag.TUE
        if (viewBind.cbWed.isChecked) repeat = repeat or WeekRepeatFlag.WED
        if (viewBind.cbThu.isChecked) repeat = repeat or WeekRepeatFlag.THU
        if (viewBind.cbFri.isChecked) repeat = repeat or WeekRepeatFlag.FRI
        if (viewBind.cbSat.isChecked) repeat = repeat or WeekRepeatFlag.SAT
        if (viewBind.cbSun.isChecked) repeat = repeat or WeekRepeatFlag.SUN
        return repeat
    }

    private fun applyRepeatToUI(repeat: Int) {
        viewBind.cbMon.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.MON)
        viewBind.cbTue.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.TUE)
        viewBind.cbWed.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.WED)
        viewBind.cbThu.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.THU)
        viewBind.cbFri.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.FRI)
        viewBind.cbSat.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SAT)
        viewBind.cbSun.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SUN)
    }

    companion object {
        private const val DIALOG_START = "classroom_start"
        private const val DIALOG_END = "classroom_end"
    }
}
