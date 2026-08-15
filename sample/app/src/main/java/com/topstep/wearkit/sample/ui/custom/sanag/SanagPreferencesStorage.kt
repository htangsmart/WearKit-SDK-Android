package com.topstep.wearkit.sample.ui.custom.sanag

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SanagPreferencesStorage {

    private lateinit var sharedPreferences: SharedPreferences

    private const val KEY_DEVICE_ADDRESS = "sanag_address"
    private const val KEY_DEVICE_NAME = "sanag_name"

    fun init(context: Context) {
        if (::sharedPreferences.isInitialized) return
        sharedPreferences = context.applicationContext.getSharedPreferences("sanag_sample_sp", Context.MODE_PRIVATE)
    }

    fun setLastDevice(info: SanagDeviceInfo) {
        sharedPreferences.edit {
            putString(KEY_DEVICE_ADDRESS, info.address)
                .putString(KEY_DEVICE_NAME, info.name)
        }
    }

    fun getLastDevice(): SanagDeviceInfo? {
        val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null) ?: return null
        val name = sharedPreferences.getString(KEY_DEVICE_NAME, null) ?: return null
        return SanagDeviceInfo(address, name)
    }

}
