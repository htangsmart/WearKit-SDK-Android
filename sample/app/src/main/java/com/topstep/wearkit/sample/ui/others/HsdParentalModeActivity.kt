package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdParentalMode
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdParentalModeBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Locale

class HsdParentalModeActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdParentalModeBinding

    private var gameStartMinutes: Int = 9 * 60
    private var gameEndMinutes: Int = 18 * 60

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdParentalModeBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Parental Mode"

        viewBind.itemGameStartTime.getTextView().text = formatMinutes(gameStartMinutes)
        viewBind.itemGameEndTime.getTextView().text = formatMinutes(gameEndMinutes)

        viewBind.itemGameStartTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = gameStartMinutes,
                title = "Game Start Time"
            ).show(supportFragmentManager, DIALOG_GAME_START)
        }
        viewBind.itemGameEndTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = gameEndMinutes,
                title = "Game End Time"
            ).show(supportFragmentManager, DIALOG_GAME_END)
        }

        viewBind.btnRequest.clickTrigger {
            requestParentalMode()
        }
        viewBind.btnSave.clickTrigger {
            saveParentalMode()
        }
    }

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_GAME_START -> {
                gameStartMinutes = timeMinute
                viewBind.itemGameStartTime.getTextView().text = formatMinutes(gameStartMinutes)
            }
            DIALOG_GAME_END -> {
                gameEndMinutes = timeMinute
                viewBind.itemGameEndTime.getTextView().text = formatMinutes(gameEndMinutes)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun requestParentalMode() {
        wearKit.b2b.hsdAbility.requestParentalMode()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mode ->
                viewBind.itemEnabled.getSwitchView().isChecked = mode.isEnabled
                viewBind.itemTimeSetting.getSwitchView().isChecked = mode.timeSettingEnabled
                viewBind.itemEnterSetting.getSwitchView().isChecked = mode.enterSettingEnabled
                viewBind.itemAlarmSetting.getSwitchView().isChecked = mode.alarmSettingEnabled
                viewBind.itemGameDuration.getSwitchView().isChecked = mode.gameDurationEnabled
                gameStartMinutes = mode.gameStartMinuteOffset
                gameEndMinutes = mode.gameEndMinuteOffset
                viewBind.itemGameStartTime.getTextView().text = formatMinutes(gameStartMinutes)
                viewBind.itemGameEndTime.getTextView().text = formatMinutes(gameEndMinutes)
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun saveParentalMode() {
        val mode = HsdParentalMode(
            isEnabled = viewBind.itemEnabled.getSwitchView().isChecked,
            timeSettingEnabled = viewBind.itemTimeSetting.getSwitchView().isChecked,
            enterSettingEnabled = viewBind.itemEnterSetting.getSwitchView().isChecked,
            alarmSettingEnabled = viewBind.itemAlarmSetting.getSwitchView().isChecked,
            gameDurationEnabled = viewBind.itemGameDuration.getSwitchView().isChecked,
            gameStartMinuteOffset = gameStartMinutes,
            gameEndMinuteOffset = gameEndMinutes,
        )

        wearKit.b2b.hsdAbility.setParentalMode(mode)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    companion object {
        private const val DIALOG_GAME_START = "parental_game_start"
        private const val DIALOG_GAME_END = "parental_game_end"
    }
}
