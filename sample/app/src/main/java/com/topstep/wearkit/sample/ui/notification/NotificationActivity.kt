package com.topstep.wearkit.sample.ui.notification

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import com.topstep.wearkit.apis.ability.base.WKNotificationAbility
import com.topstep.wearkit.base.utils.telephony.TelephonySmsUtil
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityNotificationBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import timber.log.Timber

@Suppress("DEPRECATION")
class NotificationActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityNotificationBinding

    private var mPhoneStateListener: MyPhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    @SuppressLint("ObsoleteSdkInt", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "notification"
        initEvent()
        wearKit.notificationAbility.observeTelephonyHangup().subscribe({
            var endCallResult = 0
            if (it.endCall) {
                endCallResult = TelephonySmsUtil.endCall(this)
            }
            var sendSmsResult = 0
            if (!TextUtils.isEmpty(it.number) && !TextUtils.isEmpty(it.sendSms)) {
                sendSmsResult = TelephonySmsUtil.sendSms(this, null, it.number!!, it.sendSms!!)
            }
            wearKit.notificationAbility.replayTelephonyHangup(getResult(endCallResult), getResult(sendSmsResult))
                .onErrorComplete().subscribe()
        }, {
            Timber.i(it)
        })
    }

    private fun initEvent() {
        viewBind.switchNotification.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
                    PermissionHelper.requestTelephony(this) {
                        startService(Intent(this, MyNotificationListenerService::class.java))
                        mPhoneStateListener = MyPhoneStateListener()
                        telephonyManager = getSystemService(Service.TELEPHONY_SERVICE) as TelephonyManager
                        telephonyManager!!.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
                    }
                } else {
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                            startActivity(Intent("android.settings.NOTIFICATION_LISTENER_SETTINGS"))
                        } else {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        viewBind.switchNotification.isChecked = false
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            }
        }
    }

    private fun getResult(result: Int): WKNotificationAbility.ReplayResult {
        when (result) {
            0 -> return WKNotificationAbility.ReplayResult.SUCCESS
            1 -> return WKNotificationAbility.ReplayResult.FAIL_NO_PERMISSION
            255 -> return WKNotificationAbility.ReplayResult.FAIL_UNKNOWN
        }
        return WKNotificationAbility.ReplayResult.SUCCESS
    }

    override fun onDestroy() {
        super.onDestroy()
        if (telephonyManager != null) {
            telephonyManager!!.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
        stopService(Intent(this, MyNotificationListenerService::class.java))
    }
}