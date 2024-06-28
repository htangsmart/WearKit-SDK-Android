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
import com.topstep.wearkit.sample.ui.basic.DeviceBasicActivity
import com.topstep.wearkit.sample.ui.dial.style.DialStyleCustomActivity
import com.topstep.wearkit.sample.ui.measure.MeasureActivity
import com.topstep.wearkit.sample.ui.music.MediaControlActivity
import com.topstep.wearkit.sample.ui.music.MusicActivity
import com.topstep.wearkit.sample.ui.ota.LocalOtaActivity
import com.topstep.wearkit.sample.ui.sync.SyncDataActivity
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
                wearKit.connector.observeConnectorState().startWithItem(wearKit.connector.getConnectorState()).asFlow().collect {
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

        viewBind.itemBasic.clickTrigger {
            startActivity(Intent(this, DeviceBasicActivity::class.java))
        }

        viewBind.itemVersionInfo.clickTrigger {
            startActivity(Intent(this, LocalOtaActivity::class.java))
        }

        viewBind.itemMusicPush.clickTrigger {
            PermissionHelper.requestReadAudio(this) { granted ->
                if (granted) {
                    startActivity(Intent(this, MusicActivity::class.java))
                }
            }

        }

        viewBind.itemHealthMeasurement.clickTrigger {
            startActivity(Intent(this, MeasureActivity::class.java))
        }

        viewBind.itemMedia.clickTrigger {
            startActivity(Intent(this, MediaControlActivity::class.java))
        }

        viewBind.itemSyncData.clickTrigger {
            startActivity(Intent(this, SyncDataActivity::class.java))
        }

        viewBind.itemDialCustomStyle.clickTrigger {
            if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
                toast("Device not connected!")
            } else {
                startActivity(Intent(this, DialStyleCustomActivity::class.java))
            }
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
        //Call close or connect based on whether the current user is logged in or not
        if (user == null) {
            wearKit.connector.close()
        } else {
            //When user information changes, call connect again
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