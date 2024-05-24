package com.topstep.wearkit.sample.ui.notification

import android.annotation.SuppressLint
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.topstep.wearkit.apis.model.message.WKTelephonyType
import com.topstep.wearkit.sample.MyApplication.Companion.wearKit
import timber.log.Timber


class MyPhoneStateListener : PhoneStateListener() {
    @SuppressLint("CheckResult")
    override fun onCallStateChanged(state: Int, phoneNumber: String) {
        super.onCallStateChanged(state, phoneNumber)
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // 来电 Incoming call
                wearKit.notificationAbility.sendTelephonyNotification(
                    type = WKTelephonyType.INCOMING,
                    phoneNumber = phoneNumber,
                    name = ""
                ).subscribe({

                }, {
                    Timber.i(it)
                })
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // 挂断 Hang up
                sendTelephonyNotification(1)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                //接听 Answer
                sendTelephonyNotification(0)
            }
        }
    }

    @SuppressLint("CheckResult")
    fun sendTelephonyNotification(status: Int) {
        wearKit.notificationAbility.sendTelephonyNotification(
            if (status == 0) WKTelephonyType.ANSWERED else WKTelephonyType.REJECTED,
            "",
            ""
        ).subscribe({

        }, {
            Timber.i(it)
        })
    }
}
