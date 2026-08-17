package com.topstep.wearkit.sample.ui.custom.sanag

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.topstep.wearkit.apis.model.core.WKAuthMode
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.UserManager
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow

/**
 * Scan / connect / disconnect and connection state.
 */
internal class SanagDeviceFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var lastDevice: SanagDeviceInfo? = null

    private val scanLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        refreshDeviceInfo()
        val device = lastDevice ?: return@registerForActivityResult
        connectDevice(device)
    }

    override fun onCreate() {
        viewBind.btnScan.setOnClickListener {
            wearKit.connector.close()
            scanLauncher.launch(Intent(activity, SanagScanActivity::class.java))
        }
        viewBind.btnConnect.setOnClickListener {
            val device = lastDevice
            if (device == null) {
                activity.toast(R.string.device_state_no_device)
                return@setOnClickListener
            }
            connectDevice(device)
        }
        viewBind.btnDisconnect.setOnClickListener {
            wearKit.connector.close()
        }
        refreshDeviceInfo()
        PermissionHelper.requestBle(activity)
    }

    override fun observe(scope: CoroutineScope) {
        scope.launch {
            wearKit.connector.observeConnectorState()
                .startWithItem(wearKit.connector.getConnectorState())
                .asFlow()
                .collect { state ->
                    viewBind.tvDeviceState.text = when (state) {
                        WKConnectorState.DISCONNECTED -> activity.getString(R.string.device_state_disconnected)
                        WKConnectorState.PRE_CONNECTING,
                        WKConnectorState.CONNECTING,
                        WKConnectorState.PRE_CONNECTED,
                            -> activity.getString(R.string.device_state_connecting)
                        WKConnectorState.CONNECTED -> activity.getString(R.string.device_state_connected)
                    }
                }
        }
    }

    override fun onResume() {
        refreshDeviceInfo()
    }

    override fun onDestroy() {
        wearKit.connector.close()
    }

    private fun refreshDeviceInfo() {
        val device = SanagPreferencesStorage.getLastDevice().also {
            lastDevice = it
        }
        if (device != null) {
            viewBind.tvDeviceInfo.text = "${device.address}    ${device.name}"
        } else {
            viewBind.tvDeviceInfo.text = activity.getString(R.string.device_state_no_device)
        }
    }

    private fun connectDevice(device: SanagDeviceInfo) {
        PermissionHelper.requestBle(activity) { granted ->
            if (!granted) return@requestBle
            val user = UserManager.flowAuthedUser.value
            if (user == null) {
                wearKit.connector.close()
                activity.toast(R.string.tip_failed)
                return@requestBle
            }
            SanagPreferencesStorage.setLastDevice(device)
            wearKit.connector.connect(
                type = WKDeviceType.FIT_CLOUD,
                address = device.address,
                authMode = WKAuthMode.AUTO,
                authCode = null,
                userId = user.id.toString(),
            )
        }
    }
}
