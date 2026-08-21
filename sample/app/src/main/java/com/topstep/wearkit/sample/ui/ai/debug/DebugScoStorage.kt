package com.topstep.wearkit.sample.ui.ai.debug

import android.content.Context

/**
 * 调试用 SCO 耳机：设备 [com.topstep.wearkit.apis.WKConnector.getDeviceCanBond] 拿不到时，
 * 手动指定已配对设备的 MAC，供 [com.topstep.wearkit.sample.ui.ai.isScoConnected] 使用。
 */
object DebugScoStorage {

    private const val PREF = "sample_debug_sco"
    private const val KEY_ADDRESS = "address"
    private const val KEY_NAME = "name"

    data class Device(
        val address: String,
        val name: String,
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun get(context: Context): Device? {
        val address = prefs(context).getString(KEY_ADDRESS, null) ?: return null
        val name = prefs(context).getString(KEY_NAME, null).orEmpty()
        return Device(address, name)
    }

    fun set(context: Context, address: String, name: String) {
        prefs(context).edit()
            .putString(KEY_ADDRESS, address)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
