package com.topstep.wearkit.sample.ui

import android.os.Bundle
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.PreferencesStorage
import com.topstep.wearkit.sample.databinding.ActivityMainBinding
import com.topstep.wearkit.sample.getConnectionMethod
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.setConnectionMethod
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
        viewBind.btnScanPrototb.setOnClickListener {
            DeviceScanActivity.start(this, WKDeviceType.PROTO_TB)
        }
        viewBind.btnQrcode.setOnClickListener {
            toast(R.string.main_not_implemented)
        }
        updateBtnConnectionMethodText()
        viewBind.btnConnectionMethod.setOnClickListener {
            val method = !getConnectionMethod()
            setConnectionMethod(method)
            if (method) {
                toast(getString(R.string.main_switch_done, "BLE"))
            } else {
                toast(getString(R.string.main_switch_done, "SPP"))
            }
            updateBtnConnectionMethodText()
        }
    }

    private fun updateBtnConnectionMethodText() {
        viewBind.btnConnectionMethod.text = getString(R.string.main_switch_method, if (getConnectionMethod()) "BLE" else "SPP")
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