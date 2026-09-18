package com.topstep.wearkit.sample.data

import android.content.Context
import android.content.SharedPreferences
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.model.UserInfo
import java.util.Date

object PreferencesStorage {

    private lateinit var sharedPreferences: SharedPreferences

    private const val KEY_DEVICE_TYPE = "type"
    private const val KEY_DEVICE_ADDRESS = "address"
    private const val KEY_DEVICE_NAME = "name"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_HEIGHT = "user_height"
    private const val KEY_USER_WEIGHT = "user_weight"
    private const val KEY_USER_SEX = "user_sex"
    private const val KEY_USER_AGE = "user_age"
    private const val KEY_USER_BIRTHDAY = "user_birthday"

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

    fun setUserInfo(info: UserInfo) {
        val editor = sharedPreferences.edit()
            .putLong(KEY_USER_ID, info.id)
            .putString(KEY_USER_NAME, info.name)
            .putInt(KEY_USER_HEIGHT, info.height)
            .putInt(KEY_USER_WEIGHT, info.weight)
            .putBoolean(KEY_USER_SEX, info.sex)
            .putInt(KEY_USER_AGE, info.age)
        val birthday = info.birthday
        if (birthday == null) {
            editor.remove(KEY_USER_BIRTHDAY)
        } else {
            editor.putLong(KEY_USER_BIRTHDAY, birthday.time)
        }
        editor.apply()
    }

    fun getUserInfo(): UserInfo? {
        if (!sharedPreferences.contains(KEY_USER_ID)) return null
        val birthdayMillis = sharedPreferences.getLong(KEY_USER_BIRTHDAY, -1L)
        return UserInfo(
            id = sharedPreferences.getLong(KEY_USER_ID, 1L),
            name = sharedPreferences.getString(KEY_USER_NAME, "test") ?: "test",
            height = sharedPreferences.getInt(KEY_USER_HEIGHT, 180),
            weight = sharedPreferences.getInt(KEY_USER_WEIGHT, 70),
            sex = sharedPreferences.getBoolean(KEY_USER_SEX, true),
            age = sharedPreferences.getInt(KEY_USER_AGE, 22),
            birthday = if (birthdayMillis > 0) Date(birthdayMillis) else null,
        )
    }

    fun clearUserInfo() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_HEIGHT)
            .remove(KEY_USER_WEIGHT)
            .remove(KEY_USER_SEX)
            .remove(KEY_USER_AGE)
            .remove(KEY_USER_BIRTHDAY)
            .apply()
    }

}
