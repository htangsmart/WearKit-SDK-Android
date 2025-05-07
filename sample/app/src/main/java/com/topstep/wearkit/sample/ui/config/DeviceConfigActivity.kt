package com.topstep.wearkit.sample.ui.config

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDeviceConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class DeviceConfigActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDeviceConfigBinding
    private var getDispose: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_device_config)

        viewBind.itemFunctionConfig.setOnClickListener {
            startActivity(Intent(this, FunctionConfigActivity::class.java))
        }

        viewBind.itemUnitConfig.setOnClickListener {
            startActivity(Intent(this, UnitConfigActivity::class.java))
        }

        viewBind.itemGoalConfig.setOnClickListener {
            startActivity(Intent(this, GoalConfigActivity::class.java))
        }

        viewBind.itemDndConfig.setOnClickListener {
            startActivity(Intent(this, DndConfigActivity::class.java))
        }

        viewBind.itemRaisewakeupConfig.setOnClickListener {
            startActivity(Intent(this, RaiseWakeupConfigActivity::class.java))
        }

        viewBind.itemHeartRateConfig.setOnClickListener {
            startActivity(Intent(this, HeartRateConfigActivity::class.java))
        }

        viewBind.itemPressureConfig.setOnClickListener {
            startActivity(Intent(this, PressureConfigActivity::class.java))
        }

        viewBind.itemBloodOxygenConfig.setOnClickListener {
            startActivity(Intent(this, BloodOxygenConfigActivity::class.java))
        }

        viewBind.itemBloodPressureConfig.setOnClickListener {
            startActivity(Intent(this, BloodPressureConfigActivity::class.java))
        }

        viewBind.itemNotificationConfig.setOnClickListener {
            startActivity(Intent(this, NotificationConfigActivity::class.java))
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetAll.isVisible = true
            viewBind.itemPbTestGetDeviceInfo.isVisible = true
            viewBind.itemPbTestGetAll.setOnClickListener {
                getDispose?.dispose()
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getAllConfigs()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        viewBind.tvTips.text = it.toString()
                    }, {
                        viewBind.tvTips.text = it.toString()
                    })
            }
            viewBind.itemPbTestGetDeviceInfo.setOnClickListener {
                getDispose?.dispose()
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getDeviceInfo()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        viewBind.tvTips.text = it.toString()
                    }, {
                        viewBind.tvTips.text = it.toString()
                    })
            }
        } else {
            viewBind.itemPbTestGetAll.isVisible = true
            viewBind.itemPbTestGetDeviceInfo.isVisible = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getDispose?.dispose()
    }

}