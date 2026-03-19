package com.topstep.wearkit.sample.ui

import android.bluetooth.BluetoothAdapter
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.github.kilnn.tool.ui.DisplayUtil
import com.huawei.hms.hmsscankit.RemoteView
import com.huawei.hms.ml.scan.HmsScan
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDeviceQrCodeBinding
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.discovery.DeviceScanActivity
import com.topstep.wearkit.sample.ui.discovery.DeviceScanActivity.Companion.EXTRA_TYPE
import timber.log.Timber
import java.net.URLDecoder
import java.util.regex.Pattern

class DeviceQrCodeActivity : BaseActivity() {

    private lateinit var viewBind: ActivityDeviceQrCodeBinding

    private lateinit var remoteView: RemoteView

    private val wearKit = MyApplication.wearKit
    private lateinit var deviceType: WKDeviceType
    private val addressPattern = Pattern.compile("([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceQrCodeBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        deviceType = WKDeviceType.valueOf(intent.getStringExtra(EXTRA_TYPE)!!)

        val screen = DisplayUtil.getScreenSize(this)
        val scanRectSize = DisplayUtil.dip2px(this, 240f)
        val rect = Rect()
        rect.left = screen.x / 2 - scanRectSize / 2
        rect.right = screen.x / 2 + scanRectSize / 2
        rect.top = screen.y / 2 - scanRectSize / 2
        rect.bottom = screen.y / 2 + scanRectSize / 2
        remoteView = RemoteView.Builder().setContext(this).setBoundingBox(rect).setFormat(HmsScan.ALL_SCAN_TYPE).build()

        remoteView.setOnLightVisibleCallback { visible ->
            if (visible) {
                viewBind.btnFlush.visibility = View.VISIBLE
            }
        }
        remoteView.setOnResultCallback { result ->
            if (!result.isNullOrEmpty() && result[0] != null) {
                var str = result[0].getOriginalValue()
                if (!str.isNullOrEmpty()) {
                    str = URLDecoder.decode(str.replace("+", "%2B"), "UTF-8")
                    Timber.v("str:%s", str)
                    val result = wearKit.scanner.qrcode(str)
                    if (result != null) {
                        DeviceActivity.start(this, DeviceInfo(deviceType, result.address, result.name))
                        finish()
                    } else {
                        val address = findAddress(str)
                        if (BluetoothAdapter.checkBluetoothAddress(address)) {
                            DeviceActivity.start(this, DeviceInfo(deviceType, address!!, DeviceScanActivity.UNKNOWN_DEVICE_NAME))
                        }
                    }
                }
            }
        }

        remoteView.onCreate(savedInstanceState)
        val params = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        viewBind.rim.addView(remoteView, params)

        viewBind.btnFlush.setOnClickListener {
            remoteView.switchLight()
            if (remoteView.lightStatus) {
                viewBind.btnFlush.setImageResource(R.drawable.flashlight_off)
            } else {
                viewBind.btnFlush.setImageResource(R.drawable.flashlight_on)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        remoteView.onStart()
    }

    override fun onResume() {
        super.onResume()
        remoteView.onResume()
        viewBind.scanCodeLayout.start()
    }

    override fun onPause() {
        super.onPause()
        remoteView.onPause()
        viewBind.scanCodeLayout.stop()
    }

    override fun onStop() {
        super.onStop()
        remoteView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteView.onDestroy()
    }

    private fun findAddress(str: String): String? {
        val matcher = addressPattern.matcher(str)
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

}