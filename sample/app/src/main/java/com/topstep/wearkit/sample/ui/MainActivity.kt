package com.topstep.wearkit.sample.ui

import android.os.Bundle
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.data.PreferencesStorage
import com.topstep.wearkit.sample.databinding.ActivityMainBinding
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.discovery.DeviceScanActivity

class MainActivity : BaseActivity() {

    private lateinit var viewBind: ActivityMainBinding

    private var lastDevice: DeviceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        viewBind.btnConnectLast.setOnClickListener {
            lastDevice?.let { device ->
                DeviceActivity.start(this, device)
            }
        }

        viewBind.btnScanFitcloud.setOnClickListener {
            DeviceScanActivity.start(this, WKDeviceType.FIT_CLOUD)
        }
        viewBind.btnScanFlywear.setOnClickListener {
            DeviceScanActivity.start(this, WKDeviceType.FLY_WEAR)
        }
        viewBind.btnScanShenju.setOnClickListener {
            DeviceScanActivity.start(this, WKDeviceType.SHEN_JU)
        }
    }

    override fun onResume() {
        super.onResume()
        val lastDevice = PreferencesStorage.getLastDevice().also {
            this.lastDevice = it
        }
        if (lastDevice != null) {
            viewBind.tvLastDevice.text = "${lastDevice.address}    ${lastDevice.name}"
        } else {
            viewBind.tvLastDevice.text = null

        }
    }

}