package com.topstep.wearkit.sample.ui.battery

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityBatteryBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class BatteryActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityBatteryBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityBatteryBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Battery"
        initView()
        initData()
        initEvent()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        wearKit.batteryAbility.observeBatteryChange()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvBattery.text = "电量改变为:${it.percentage}"
            }, {

            })
    }

    @SuppressLint("CheckResult")
    private fun initData() {

    }

    @SuppressLint("CheckResult")
    private fun initEvent() {
        viewBind.btnGetBattery.clickTrigger {
            wearKit.batteryAbility.requestBattery().observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewBind.tvBattery.text = "当前电量改变为:${it.percentage}"
                }, {

                })
        }

    }


    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}