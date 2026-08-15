package com.topstep.wearkit.sample.ui.custom.sanag

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.topstep.wearkit.apis.model.WKBattery
import com.topstep.wearkit.apis.model.core.WKAuthMode
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.UserManager
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import timber.log.Timber

/**
 * "塞那"定制功能
 */
class SanagDemoActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySanagDemoBinding

    private var lastDevice: SanagDeviceInfo? = null
    private var batteryDisposable: Disposable? = null

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        refreshDeviceInfo()
        val device = lastDevice ?: return@registerForActivityResult
        connectDevice(device)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SanagPreferencesStorage.init(this)
        // Avoid competing with DeviceActivity / other modules on the shared connector
        wearKit.connector.close()
        viewBind = ActivitySanagDemoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.main_sanag_demo)

        lifecycle.launchRepeatOnStarted {
            launch {
                wearKit.connector.observeConnectorState()
                    .startWithItem(wearKit.connector.getConnectorState())
                    .asFlow()
                    .collect { state ->
                        viewBind.tvDeviceState.text = when (state) {
                            WKConnectorState.DISCONNECTED -> getString(R.string.device_state_disconnected)
                            WKConnectorState.PRE_CONNECTING,
                            WKConnectorState.CONNECTING,
                            WKConnectorState.PRE_CONNECTED,
                                -> getString(R.string.device_state_connecting)
                            WKConnectorState.CONNECTED -> getString(R.string.device_state_connected)
                        }
                    }
            }
            launch {
                wearKit.batteryAbility.observeBatteryChange()
                    .asFlow()
                    .catch { Timber.w(it) }
                    .collect { updateBatteryText(it) }
            }
        }

        viewBind.btnScan.setOnClickListener {
            wearKit.connector.close()//close before scan
            scanLauncher.launch(Intent(this, SanagScanActivity::class.java))
        }

        viewBind.btnConnect.setOnClickListener {
            val device = lastDevice
            if (device == null) {
                toast(R.string.device_state_no_device)
                return@setOnClickListener
            }
            connectDevice(device)
        }

        viewBind.btnDisconnect.setOnClickListener {
            wearKit.connector.close()
        }

        viewBind.btnBattery.setOnClickListener {
            if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
                toast(R.string.device_state_disconnected)
                return@setOnClickListener
            }
            batteryDisposable?.dispose()
            batteryDisposable = wearKit.batteryAbility.requestBattery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateBatteryText(it)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        refreshDeviceInfo()
        PermissionHelper.requestBle(this)
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceInfo()
    }

    override fun onDestroy() {
        batteryDisposable?.dispose()
        batteryDisposable = null
        wearKit.connector.close()
        super.onDestroy()
    }

    private fun refreshDeviceInfo() {
        val device = SanagPreferencesStorage.getLastDevice().also {
            lastDevice = it
        }
        if (device != null) {
            viewBind.tvDeviceInfo.text = "${device.address}    ${device.name}"
        } else {
            viewBind.tvDeviceInfo.text = getString(R.string.device_state_no_device)
        }
    }

    private fun updateBatteryText(battery: WKBattery) {
        viewBind.tvBattery.text = "Battery level:${battery.percentage} isCharging:${battery.isCharging}"
    }

    private fun connectDevice(device: SanagDeviceInfo) {
        PermissionHelper.requestBle(this) { granted ->
            if (!granted) return@requestBle
            val user = UserManager.flowAuthedUser.value
            if (user == null) {
                wearKit.connector.close()
                toast(R.string.tip_failed)
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
