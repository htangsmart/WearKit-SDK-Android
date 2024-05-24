package com.topstep.wearkit.sample.ui.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.topstep.wearkit.sample.MyApplication
import timber.log.Timber

class MyNotificationListenerService : NotificationListenerService() {

    private val wearKit = MyApplication.wearKit

    @SuppressLint("CheckResult")
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName
        if (packageName.isNullOrEmpty()) return
        if (packageName == "com.android.incallui") return
        var tickerText = ""
        var notificationTitle = ""
        var notificationContent = ""
        if (notification.tickerText != null) {
            tickerText = notification.tickerText.toString()
        }
        if (notification.extras != null) {
            val bundle = notification.extras
             notificationTitle = bundle.getCharSequence(Notification.EXTRA_TITLE).toString()
            notificationContent = bundle.getCharSequence(Notification.EXTRA_TEXT).toString()
        }
        wearKit.notificationAbility.sendAppNotification(
            packageName = packageName,
            title = notificationTitle,
            content = notificationContent,
            tickerText = tickerText
        ).subscribe({

        }, {
            Timber.i(it)
        })
    }

}