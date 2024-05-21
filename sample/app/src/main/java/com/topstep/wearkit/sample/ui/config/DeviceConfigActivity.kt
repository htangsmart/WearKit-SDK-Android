package com.topstep.wearkit.sample.ui.config

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityConfigBinding
import com.topstep.wearkit.sample.ui.alarm.AlarmActivity
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.battery.BatteryActivity
import com.topstep.wearkit.sample.ui.contacts.ContactActivity
import com.topstep.wearkit.sample.ui.contacts.EmergencyContactActivity
import com.topstep.wearkit.sample.ui.find.FindWatchActivity
import com.topstep.wearkit.sample.ui.language.LanguageActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class DeviceConfigActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityConfigBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "config"

        initEvent()
    }

    @SuppressLint("CheckResult")
    private fun initEvent() {
        // 关机  Shutdown
        viewBind.btnShutdown.clickTrigger {
            wearKit.deviceAbility.shutdown()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(getString(R.string.tip_success))
                }, {
                    toast(getString(R.string.tip_failed))
                })
        }

        // 重启  Reboot
        viewBind.btnReboot.clickTrigger {
            wearKit.deviceAbility.reboot()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(getString(R.string.tip_success))
                }, {
                    toast(getString(R.string.tip_failed))
                })
        }

        // 恢复出厂  Reset
        viewBind.btnReset.clickTrigger {
            wearKit.deviceAbility.reset()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(getString(R.string.tip_success))
                }, {
                    toast(getString(R.string.tip_failed))
                })
        }

        // 时间 Time
        viewBind.btnSetTime.clickTrigger {
            wearKit.timeAbility.setTime(System.currentTimeMillis())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    toast(getString(R.string.tip_success))
                    toast(getString(R.string.tip_failed))
                })
        }
        // 语言 Language
        viewBind.btnSetLanguage.clickTrigger {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        // 电量 Battery
        viewBind.btnBattery.clickTrigger {
            startActivity(Intent(this, BatteryActivity::class.java))
        }

        // 常用联系人 Contacts
        viewBind.btnContacts.clickTrigger {
            startActivity(Intent(this, ContactActivity::class.java))
        }
        //紧急联系人 EmergencyContact
        viewBind.btnEmergencyContact.clickTrigger {
            startActivity(Intent(this, EmergencyContactActivity::class.java))
        }

        //闹钟  alArm
        viewBind.btnAlarm.clickTrigger {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        // 查找手表 手机 find watch phone
        viewBind.btnFind.clickTrigger {
            startActivity(Intent(this, FindWatchActivity::class.java))
        }

    }


    override fun onDestroy() {
        super.onDestroy()

    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}