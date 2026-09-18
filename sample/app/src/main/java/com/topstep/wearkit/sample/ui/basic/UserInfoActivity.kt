package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKUserInfo
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.data.UserManager
import com.topstep.wearkit.sample.databinding.ActivityUserInfoBinding
import com.topstep.wearkit.sample.model.UserInfo
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.DatePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Demo：编辑个人信息，写入 SharedPreferences，并在已连接时 [syncUserInfo] 到设备。
 * 连接设备时使用的用户参数见 [com.topstep.wearkit.sample.ui.DeviceActivity]。
 */
@SuppressLint("CheckResult")
class UserInfoActivity : BaseActivity(), DatePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityUserInfoBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var birthday: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.user_info)

        bindUser(UserManager.flowAuthedUser.value)

        viewBind.tvBirthday.clickTrigger {
            DatePickerDialogFragment.newInstance(
                start = null,
                end = Date(),
                value = birthday ?: Date(),
                title = getString(R.string.user_info_birthday),
            ).show(supportFragmentManager, TAG_BIRTHDAY)
        }

        viewBind.btnSave.clickTrigger {
            val user = readInputOrNull() ?: return@clickTrigger
            UserManager.updateUser(user)
            toast(R.string.tip_success)
        }

        viewBind.btnSync.clickTrigger {
            val user = readInputOrNull() ?: return@clickTrigger
            UserManager.updateUser(user)
            syncToDevice(user)
        }
    }

    private fun bindUser(user: UserInfo?) {
        val current = user ?: return
        viewBind.etUserId.setText(current.id.toString())
        viewBind.etUserName.setText(current.name)
        viewBind.etAge.setText(current.age.toString())
        viewBind.etHeight.setText(current.height.toString())
        viewBind.etWeight.setText(current.weight.toString())
        if (current.sex) {
            viewBind.rbMale.isChecked = true
        } else {
            viewBind.rbFemale.isChecked = true
        }
        birthday = current.birthday
        viewBind.tvBirthday.text = birthday?.let { dateFormat.format(it) }.orEmpty()
    }

    private fun readInputOrNull(): UserInfo? {
        val id = viewBind.etUserId.text?.toString()?.trim()?.toLongOrNull()
        val name = viewBind.etUserName.text?.toString()?.trim().orEmpty()
        val age = viewBind.etAge.text?.toString()?.trim()?.toIntOrNull()
        val height = viewBind.etHeight.text?.toString()?.trim()?.toIntOrNull()
        val weight = viewBind.etWeight.text?.toString()?.trim()?.toIntOrNull()
        if (id == null || id <= 0 || name.isEmpty() || age == null || height == null || weight == null) {
            toast(R.string.user_info_invalid)
            return null
        }
        if (age !in 1..120 || height !in 50..250 || weight !in 20..300) {
            toast(R.string.user_info_invalid)
            return null
        }
        return UserInfo(
            id = id,
            name = name,
            height = height,
            weight = weight,
            sex = viewBind.rbMale.isChecked,
            age = age,
            birthday = birthday,
        )
    }

    private fun syncToDevice(user: UserInfo) {
        if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
            toast(R.string.user_info_not_connected)
            return
        }
        val wkInfo = WKUserInfo(
            sex = if (user.sex) WKUserInfo.Sex.MALE else WKUserInfo.Sex.FEMALE,
            age = user.age,
            height = user.height.toFloat(),
            weight = user.weight.toFloat(),
            birthday = user.birthday,
            timestampSeconds = System.currentTimeMillis() / 1000,
        )
        wearKit.connector.syncUserInfo(wkInfo)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ deviceInfo ->
                // 设备侧更新时间更新，回写本地缓存（保留 id/name）
                val merged = user.copy(
                    sex = deviceInfo.sex == WKUserInfo.Sex.MALE,
                    age = deviceInfo.age,
                    height = deviceInfo.height.toInt(),
                    weight = deviceInfo.weight.toInt(),
                    birthday = deviceInfo.birthday ?: user.birthday,
                )
                UserManager.updateUser(merged)
                bindUser(merged)
                toast(R.string.user_info_sync_from_device)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            }, {
                toast(R.string.tip_success)
            })
    }

    override fun onDialogDatePicker(tag: String?, date: Date) {
        if (tag != TAG_BIRTHDAY) return
        birthday = date
        viewBind.tvBirthday.text = dateFormat.format(date)
        // 按生日粗算年龄，方便和连接参数一致
        viewBind.etAge.setText(ageFromBirthday(date).toString())
    }

    private fun ageFromBirthday(birthday: Date): Int {
        val birth = Calendar.getInstance().apply { time = birthday }
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age -= 1
        }
        return age.coerceIn(1, 120)
    }

    companion object {
        private const val TAG_BIRTHDAY = "birthday"
    }
}
