package com.topstep.wearkit.sample.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.*
import com.topstep.wearkit.sample.data.PreferencesStorage
import com.topstep.wearkit.sample.databinding.ActivityMainBinding
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.LogShareDialogFragment
import com.topstep.wearkit.sample.ui.discovery.DeviceScanActivity
import com.topstep.wearkit.sample.utils.log.AppLogger
import kotlinx.coroutines.launch

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

        viewBind.btnLog.setOnClickListener {
            lifecycleScope.launch {
                val files = AppLogger.getLogFiles(this@MainActivity)
                if (files.isNullOrEmpty()) {
                    toast(R.string.tip_current_no_data)
                    return@launch
                }
                LogShareDialogFragment.newInstance(files).show(supportFragmentManager, null)
            }
        }

        getSkipAuth().let {
            viewBind.cbSkipAuth.isChecked = it
            updateSkipAuth(it)
        }

        viewBind.cbSkipAuth.setOnCheckedChangeListener { _, isChecked ->
            setSkipAuth(isChecked)
            updateSkipAuth(isChecked)
        }

    }

    private fun updateSkipAuth(skip: Boolean) {
        //Only for test. Developer should not use this(Only part watches support it)
        FcSDK.CONNECT_SKIP_AUTH = skip
        PbSDK.CONNECT_SKIP_AUTH = skip
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