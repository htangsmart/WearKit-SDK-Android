package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDeviceBasicBinding
import com.topstep.wearkit.sample.ui.alarm.AlarmActivity
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.contacts.ContactActivity
import com.topstep.wearkit.sample.ui.contacts.EmergencyContactActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class DeviceBasicActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDeviceBasicBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceBasicBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_device_basic)

        // Device shutdown
        viewBind.btnShutdown.clickTrigger {
            wearKit.deviceAbility.shutdown()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        // Device reboot
        viewBind.btnReboot.clickTrigger {
            wearKit.deviceAbility.reboot()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        // Device reset
        viewBind.btnReset.clickTrigger {
            wearKit.deviceAbility.reset()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        viewBind.btnSetTime.clickTrigger {
            wearKit.timeAbility.setTime(System.currentTimeMillis())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        viewBind.btnSetLanguage.clickTrigger {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        viewBind.btnBattery.clickTrigger {
            startActivity(Intent(this, BatteryActivity::class.java))
        }

        viewBind.btnContacts.clickTrigger {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        viewBind.btnEmergencyContact.clickTrigger {
            startActivity(Intent(this, EmergencyContactActivity::class.java))
        }

        viewBind.btnAlarm.clickTrigger {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        viewBind.btnFindWatch.clickTrigger {
            startActivity(Intent(this, FindWatchActivity::class.java))
        }

        viewBind.btnFindPhone.clickTrigger {
            startActivity(Intent(this, FindPhoneActivity::class.java))
        }
    }

}