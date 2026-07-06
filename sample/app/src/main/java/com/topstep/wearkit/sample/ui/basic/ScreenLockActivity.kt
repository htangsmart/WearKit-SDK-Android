package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputFilter
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.special.WKScreenLock
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityScreenLockBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class ScreenLockActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityScreenLockBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityScreenLockBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.screen_lock)

        setupPasswordInput()

        viewBind.btnSave.clickTrigger {
            saveScreenLock()
        }
    }

    private fun setupPasswordInput() {
        val passwordLength = wearKit.lockAbility.compat.getPasswordLength()
        viewBind.editPassword.filters = arrayOf(InputFilter.LengthFilter(passwordLength))
        viewBind.editPassword.hint = getString(R.string.lock_password_hint, passwordLength)
    }

    private fun saveScreenLock() {
        val isEnabled = viewBind.switchScreenLock.isChecked
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

        val lock = WKScreenLock(isEnabled = isEnabled, password = pinStr)

        wearKit.lockAbility.setScreenLock(lock)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
