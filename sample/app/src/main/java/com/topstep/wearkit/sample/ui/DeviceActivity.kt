package com.topstep.wearkit.sample.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.flywear.sdk.model.core.FwAuthMode
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.PreferencesStorage
import com.topstep.wearkit.sample.data.UserManager
import com.topstep.wearkit.sample.databinding.ActivityDeviceBinding
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.model.UserInfo
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.ota.LocalOtaActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow

class DeviceActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDeviceBinding
    private lateinit var device: DeviceInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        supportActionBar?.setTitle(R.string.device_module)

        device = intent.getParcelableExtra(EXTRA_DEVICE)!!
        PreferencesStorage.setLastDevice(device)

        viewBind.tvDeviceName.text = device.name
        viewBind.tvDeviceAddress.text = device.address

        lifecycle.launchRepeatOnStarted {
            launch {
                wearKit.connector.observeConnectorState().asFlow().collect {
                    viewBind.tvDeviceState.text = when (it) {
                        WKConnectorState.DISCONNECTED -> getString(R.string.device_state_disconnected)
                        WKConnectorState.PRE_CONNECTING,
                        WKConnectorState.CONNECTING,
                        WKConnectorState.PRE_CONNECTED,
                        -> getString(R.string.device_state_connecting)
                        WKConnectorState.CONNECTED -> getString(R.string.device_state_connected)
                    }
                }
            }
        }

        //Ensure has permission
        PermissionHelper.requestBle(this)

        //Connect device
        connect(UserManager.flowAuthedUser.value)

        viewBind.itemVersionInfo.clickTrigger {
            startActivity(Intent(this, LocalOtaActivity::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        //Close device
        if (isFinishing) {
            wearKit.connector.close()
        }
    }

    private fun connect(user: UserInfo?) {
        //zh:根据当前用户是否登录，调用close或connect
        if (user == null) {
            wearKit.connector.close()
        } else {
            //zh:用户信息改变时，直接调用connect即可
            wearKit.connector.connect(
                type = device.type,
                address = device.address,
                authMode = FwAuthMode.AUTO,
                authCode = null,
                userId = user.id.toString(),
                sex = user.sex,
                age = user.age,
                height = user.height.toFloat(),
                weight = user.weight.toFloat(),
            )
        }
    }

    companion object {
        private const val EXTRA_DEVICE = "device"

        fun start(context: Context, device: DeviceInfo) {
            context.startActivity(
                Intent(context, DeviceActivity::class.java).apply {
                    putExtra(EXTRA_DEVICE, device)
                }
            )
        }

    }
}