package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityBatteryBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class BatteryActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityBatteryBinding
    private var observeBatteryDisposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityBatteryBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_battery)

        //Observe battery change
        observeBatteryDisposable = wearKit.batteryAbility.observeBatteryChange()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvBattery.text = "Battery level:${it.percentage}"
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })

        //Request battery once
        viewBind.btnGetBattery.clickTrigger {
            wearKit.batteryAbility.requestBattery().observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewBind.tvBattery.text = "Battery level:${it.percentage}"
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeBatteryDisposable?.dispose()
    }

}