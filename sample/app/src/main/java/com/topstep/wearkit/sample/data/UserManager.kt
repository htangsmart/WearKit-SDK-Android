package com.topstep.wearkit.sample.data

import com.topstep.wearkit.sample.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 管理当前登录的用户
 */
object UserManager {

    private val _flowAuthedUser: MutableStateFlow<UserInfo?> = MutableStateFlow(mockAuthedUser(1))

    val flowAuthedUser: StateFlow<UserInfo?> = _flowAuthedUser

    /**
     * 退出账号
     */
    fun mockClearUser() {
        _flowAuthedUser.value = null
    }

    /**
     * 切换用户
     */
    fun mockSwitchUser() {
        val previousUser = _flowAuthedUser.value
        if (previousUser == null) {
            _flowAuthedUser.value = mockAuthedUser(1)
        } else {
            _flowAuthedUser.value = mockAuthedUser(previousUser.id + 1)
        }
    }

    private fun mockAuthedUser(id: Long): UserInfo {
        return UserInfo(
            id = id,
            name = "test$id",
            height = 180,
            weight = 70,
            sex = true,
            age = 22
        )
    }
}