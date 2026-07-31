package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.ability.base.WKCameraAbility
import com.topstep.wearkit.apis.model.message.WKCameraMessage
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDeviceBasicBinding
import com.topstep.wearkit.sample.ui.alarm.AlarmActivity
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.camera.CameraActivity
import com.topstep.wearkit.sample.ui.contacts.ContactsHomeActivity
import com.topstep.wearkit.sample.ui.notification.NotificationActivity
import com.topstep.wearkit.sample.ui.remind.RemindActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class DeviceBasicActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDeviceBasicBinding

    private var observeCameraDisposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceBasicBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_device_basic)

        observeCameraDisposable = wearKit.cameraAbility.observeCameraMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it) {
                    WKCameraMessage.OPEN -> {
                        startCamera()
                    }
                }
            }, {
                Timber.w(it)
            })

        // Device shutdown
        viewBind.btnShutdown.clickTrigger {
            wearKit.deviceAbility.shutdown()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        // Device reboot
        viewBind.btnReboot.clickTrigger {
            wearKit.deviceAbility.reboot()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        // Device reset
        viewBind.btnReset.clickTrigger {
            wearKit.deviceAbility.reset()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        viewBind.btnSetTime.clickTrigger {
            wearKit.timeAbility.setTime(System.currentTimeMillis())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        viewBind.btnSetLanguage.clickTrigger {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        viewBind.btnBattery.clickTrigger {
            startActivity(Intent(this, BatteryActivity::class.java))
        }

        viewBind.btnContacts.clickTrigger {
            startActivity(Intent(this, ContactsHomeActivity::class.java))
        }

        viewBind.btnAlarm.clickTrigger {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        viewBind.btnFindWatch.clickTrigger {
            startActivity(Intent(this, FindWatchActivity::class.java))
        }

        viewBind.btnFindPhone.clickTrigger {
            startActivity(Intent(this, FindPhoneActivity::class.java))
        }

        val supportPreview = wearKit.cameraAbility.compat.isSupportPreview()
        viewBind.tvPreviewSupport.text = if (supportPreview) "支持" else "不支持"
        viewBind.tvPreviewSupport.setTextColor(
            if (supportPreview) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
        )

        viewBind.etTestFps.setText(WKCameraAbility.TEST_FPS.toString())
        viewBind.etTestBitrate.setText(WKCameraAbility.TEST_H264_BITRATE.toString())
        viewBind.etTestQuality.setText(WKCameraAbility.TEST_JPEG_QUALITY.toString())

        viewBind.btnCamera.clickTrigger {
            startCamera()
        }

        viewBind.btnWeather.clickTrigger {
            startActivity(Intent(this, WeatherActivity::class.java))
        }

        viewBind.btnNotification.clickTrigger {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        viewBind.btnRemind.clickTrigger {
            startActivity(Intent(this, RemindActivity::class.java))
        }

        viewBind.btnTimeFormat.clickTrigger {
            startActivity(Intent(this, TimeFormatActivity::class.java))
        }

        viewBind.btnActivityTarget.clickTrigger {
            startActivity(Intent(this, SportTargetActivity::class.java))
        }

        viewBind.btnScreenLock.clickTrigger {
            if (wearKit.lockAbility.compat.isSupportScreenLock()) {
                startActivity(Intent(this, ScreenLockActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.btnGameLock.clickTrigger {
            if (wearKit.lockAbility.compat.isSupportGameLock()) {
                startActivity(Intent(this, GameLockActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeCameraDisposable?.dispose()
    }

    private fun startCamera() {
        applyCameraTestSettings()
        PermissionHelper.requestAppCamera(this) { granted ->
            if (granted) {
                startActivity(Intent(this, CameraActivity::class.java))
            }
        }
    }

    private fun applyCameraTestSettings() {
        val fps = viewBind.etTestFps.text?.toString()?.toIntOrNull() ?: 0
        val bitrate = viewBind.etTestBitrate.text?.toString()?.toIntOrNull() ?: 0
        val quality = viewBind.etTestQuality.text?.toString()?.toIntOrNull() ?: 0
        WKCameraAbility.TEST_FPS = fps.coerceIn(0, 120)
        WKCameraAbility.TEST_H264_BITRATE = if (bitrate <= 0) 0 else bitrate.coerceIn(100, 1000)
        WKCameraAbility.TEST_JPEG_QUALITY = quality.coerceIn(0, 100)
    }

}