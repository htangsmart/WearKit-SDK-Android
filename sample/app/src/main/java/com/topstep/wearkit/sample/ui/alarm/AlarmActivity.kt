package com.topstep.wearkit.sample.ui.alarm

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKAlarm
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityAlarmBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.AppUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class AlarmActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityAlarmBinding

    private var alarmRepeat = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Alarm"
        initView()
        initData()
        initEvent()
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged", "SetTextI18n")
    private fun initView() {
        viewBind.alarmSunday.clickTrigger(block = blockClick)
        viewBind.alarmMonday.clickTrigger(block = blockClick)
        viewBind.alarmTuesday.clickTrigger(block = blockClick)
        viewBind.alarmWednesday.clickTrigger(block = blockClick)
        viewBind.alarmThursday.clickTrigger(block = blockClick)
        viewBind.alarmFriday.clickTrigger(block = blockClick)
        viewBind.alarmSaturday.clickTrigger(block = blockClick)

        // Callback in device operation
        /*wearKit.alarmAbility.observeAlarmsChange().observeOn(AndroidSchedulers.mainThread())
            .subscribe({

            }, {

            })*/


        // request
        wearKit.alarmAbility.requestAlarms()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (!it.isNullOrEmpty()) {
                    val alarm = it[0]
                    viewBind.alarmTime.text = "${alarm.hour} : ${alarm.minute}"
                     viewBind.alarmWeek.text = AppUtils.getWeek(this, alarm.repeat)
                }
            }, {

            })


    }

    private fun initData() {


    }


    @SuppressLint("CheckResult", "Range")
    private fun initEvent() {
        // add
        viewBind.alarmAdd.clickTrigger {
            if (alarmRepeat == 0) {
                toast(getString(R.string.alarm_week))
            } else {
                val alarm = ArrayList<WKAlarm>()
                val wkAlarm = WKAlarm()
                wkAlarm.hour = 11
                wkAlarm.minute = 30
                wkAlarm.isEnabled = true
                wkAlarm.repeat = alarmRepeat
                alarm.add(wkAlarm)
                setAlarm(alarm)
            }
        }

        //delete
        viewBind.alarmDelete.clickTrigger {
            setAlarm(ArrayList())
        }

    }

    @SuppressLint("CheckResult")
    fun setAlarm(alarm: ArrayList<WKAlarm>) {
        wearKit.alarmAbility.setAlarms(alarm)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(getString(R.string.tip_success))
            }, {
                toast(getString(R.string.tip_failed))
            })
    }

    private val blockClick: (View) -> Unit = blockClick@{ view ->
        when (view) {
            viewBind.alarmMonday -> {
                setRemindWeek(WeekRepeatFlag.MON, viewBind.alarmMonday)
            }

            viewBind.alarmTuesday -> {
                setRemindWeek(WeekRepeatFlag.TUE, viewBind.alarmTuesday)
            }

            viewBind.alarmWednesday -> {
                setRemindWeek(WeekRepeatFlag.WED, viewBind.alarmWednesday)
            }

            viewBind.alarmThursday -> {
                setRemindWeek(WeekRepeatFlag.THU, viewBind.alarmThursday)
            }

            viewBind.alarmFriday -> {
                setRemindWeek(WeekRepeatFlag.FRI, viewBind.alarmFriday)
            }

            viewBind.alarmSaturday -> {
                setRemindWeek(WeekRepeatFlag.SAT, viewBind.alarmSaturday)
            }

            viewBind.alarmSunday -> {
                setRemindWeek(WeekRepeatFlag.SUN, viewBind.alarmSunday)
            }
        }

    }

    private fun setRemindWeek(flag: Int, remindSunday: TextView) {
        if (WeekRepeatFlag.isRepeatEnabled(alarmRepeat, flag)) {
            alarmRepeat = WeekRepeatFlag.setRepeatEnabled(alarmRepeat, flag, false)
            remindSunday.background = null
        } else {
            alarmRepeat = WeekRepeatFlag.setRepeatEnabled(alarmRepeat, flag, true)
            remindSunday.setBackgroundColor(Color.parseColor("#3fcce2"))
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}