package com.topstep.wearkit.sample.model

data class UserInfo(
    val id: Long,
    val name: String,//unique username
    val height: Int,//user height(cm)
    val weight: Int,//user weight(kg)
    val sex: Boolean,//True for male, false for female
    val age: Int,
)