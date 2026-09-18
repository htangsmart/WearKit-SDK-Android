package com.topstep.wearkit.sample.data

import com.topstep.wearkit.sample.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 管理当前登录的用户。个人信息持久化在 [PreferencesStorage]。
 */
object UserManager {

    private val _flowAuthedUser: MutableStateFlow<UserInfo?> =
        MutableStateFlow(PreferencesStorage.getUserInfo() ?: defaultUser(1).also {
            PreferencesStorage.setUserInfo(it)
        })

    val flowAuthedUser: StateFlow<UserInfo?> = _flowAuthedUser

    /**
     * 更新并缓存个人信息（供连接参数 / syncUserInfo 使用）。
     */
    fun updateUser(user: UserInfo) {
        PreferencesStorage.setUserInfo(user)
        _flowAuthedUser.value = user
    }

    /**
     * 退出账号
     */
    fun mockClearUser() {
        PreferencesStorage.clearUserInfo()
        _flowAuthedUser.value = null
    }

    /**
     * 切换用户
     */
    fun mockSwitchUser() {
        val previousUser = _flowAuthedUser.value
        val next = if (previousUser == null) {
            defaultUser(1)
        } else {
            defaultUser(previousUser.id + 1)
        }
        updateUser(next)
    }

    private fun defaultUser(id: Long): UserInfo {
        // 默认生日：按年龄 22 岁粗算到当年 1 月 1 日
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.YEAR, -22)
            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return UserInfo(
            id = id,
            name = "test$id",
            height = 180,
            weight = 70,
            sex = true,
            age = 22,
            birthday = calendar.time,
        )
    }
}
