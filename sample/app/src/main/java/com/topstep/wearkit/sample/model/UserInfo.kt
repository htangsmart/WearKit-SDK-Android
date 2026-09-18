package com.topstep.wearkit.sample.model

import java.util.Date

data class UserInfo(
    val id: Long,
    val name: String,//unique username
    val height: Int,//user height(cm)
    val weight: Int,//user weight(kg)
    val sex: Boolean,//True for male, false for female
    val age: Int,
    /**
     * Birthday. Used by [com.topstep.wearkit.apis.model.WKUserInfo.birthday] when syncing.
     * Null if unknown / device ignores birthday.
     */
    val birthday: Date? = null,
)
