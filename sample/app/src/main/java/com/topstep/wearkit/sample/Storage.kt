package com.topstep.wearkit.sample

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


fun Context.getAppStorage(): SharedPreferences {
    return getSharedPreferences("app_sample", Context.MODE_PRIVATE)
}

/**
 * 获取连接方式
 * true代表ble
 * false代表spp
 */
fun Context.getConnectionMethod(): Boolean {
    return getAppStorage().getBoolean("connection_method", true)
}

/**
 * 设置连接方式
 * true代表ble
 * false代表spp
 */
fun Context.setConnectionMethod(method: Boolean) {
    return getAppStorage().edit {
        putBoolean("connection_method", method)
    }
}
