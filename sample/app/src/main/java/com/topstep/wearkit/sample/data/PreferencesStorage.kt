package com.topstep.wearkit.sample.data

import android.content.Context
import android.content.SharedPreferences
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.model.DeviceInfo

object PreferencesStorage {

    private lateinit var sharedPreferences: SharedPreferences

    private const val KEY_DEVICE_TYPE = "type"
    private const val KEY_DEVICE_ADDRESS = "address"
    private const val KEY_DEVICE_NAME = "name"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("sample_sp", Context.MODE_PRIVATE)
    }

    fun setLastDevice(info: DeviceInfo) {
        sharedPreferences.edit()
            .putString(KEY_DEVICE_TYPE, info.type.name)
            .putString(KEY_DEVICE_ADDRESS, info.address)
            .putString(KEY_DEVICE_NAME, info.name)
            .apply()
    }

    fun getLastDevice(): DeviceInfo? {
        val type = sharedPreferences.getString(KEY_DEVICE_TYPE, null) ?: return null
        val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null) ?: return null
        val name = sharedPreferences.getString(KEY_DEVICE_NAME, null) ?: return null
        return DeviceInfo(WKDeviceType.valueOf(type), address, name)
    }

}