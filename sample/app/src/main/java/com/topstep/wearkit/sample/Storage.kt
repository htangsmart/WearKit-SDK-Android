package com.topstep.wearkit.sample

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


fun Context.getAppStorage(): SharedPreferences {
    return getSharedPreferences("wearkit_sample", Context.MODE_PRIVATE)
}

/**
 * Get connection method.
 * true for BLE
 * false for SPP
 */
fun Context.getConnectionMethod(): Boolean {
    return getAppStorage().getBoolean("connection_method", true)
}

/**
 * Set connection method.
 * true for BLE
 * false for SPP
 */
fun Context.setConnectionMethod(method: Boolean) {
    return getAppStorage().edit {
        putBoolean("connection_method", method)
    }
}
