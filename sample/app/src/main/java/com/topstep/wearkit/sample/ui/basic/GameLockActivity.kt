package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputFilter
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.special.WKGameLock
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityGameLockBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Locale

class GameLockActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityGameLockBinding

    /** Start time as minute offset from midnight (e.g. 9:00 = 540). */
    private var startMinutes: Int = 9 * 60

    /** End time as minute offset from midnight (e.g. 18:00 = 1080). */
    private var endMinutes: Int = 18 * 60

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityGameLockBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.game_lock)

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

        setupPasswordInput()

        viewBind.btnSave.clickTrigger {
            saveGameLock()
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

    private fun setupPasswordInput() {
        val passwordLength = wearKit.lockAbility.compat.getPasswordLength()
        viewBind.editPassword.filters = arrayOf(InputFilter.LengthFilter(passwordLength))
        viewBind.editPassword.hint = getString(R.string.lock_password_hint, passwordLength)
    }

    private fun saveGameLock() {
        val isEnabled = viewBind.itemGameLockSwitch.getSwitchView().isChecked
        val pinStr = viewBind.editPassword.text?.toString().orEmpty()
        val passwordLength = wearKit.lockAbility.compat.getPasswordLength()

        if (pinStr.length != passwordLength) {
            toast(getString(R.string.lock_password_hint, passwordLength))
            return
        }
        if (!pinStr.all { it in '0'..'9' }) {
            toast(getString(R.string.lock_password_hint, passwordLength))
            return
        }

        val lock = WKGameLock(
            isEnabled = isEnabled,
            password = pinStr,
            start = startMinutes,
            end = endMinutes,
        )

        wearKit.lockAbility.setGameLock(lock)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    companion object {
        private const val DIALOG_START = "game_lock_start"
        private const val DIALOG_END = "game_lock_end"
    }
}
