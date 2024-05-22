package com.topstep.wearkit.sample.ui.alarm

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.github.kilnn.wheellayout.WheelIntConfig
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
    private val formatter = AppUtils.get02dWheelIntFormatter(this)
    private lateinit var adapter: AlarmAdapter

    private val alarmCount = ArrayList<WKAlarm>()

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
        adapter = AlarmAdapter()

        // Callback in device operation
        wearKit.alarmAbility.observeAlarmsChange()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                alarmCount.clear()
                alarmCount.addAll(it)
                adapter.sources = alarmCount
                adapter.notifyDataSetChanged()
            }, {

            })

        // request alarm
        wearKit.alarmAbility.requestAlarms()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isNotEmpty()) {
                    alarmCount.addAll(it)
                    adapter.sources = alarmCount
                    adapter.notifyDataSetChanged()
                }
            }, {

            })
    }

    private fun initData() {
        viewBind.alarmRy.layoutManager = LinearLayoutManager(this)
        viewBind.alarmRy.adapter = adapter
        viewBind.wheelHour.setConfig(WheelIntConfig(0, 23, true, getString(R.string.unit_hour), formatter))
        viewBind.wheelMinute.setConfig(WheelIntConfig(0, 59, true, getString(R.string.unit_minute), formatter))
    }


    @SuppressLint("CheckResult", "Range", "NotifyDataSetChanged")
    private fun initEvent() {
        // add alarm
        viewBind.alarmAdd.clickTrigger {
            if (alarmRepeat == 0) {
                toast(getString(R.string.alarm_week))
            } else {
                val wkAlarm = WKAlarm()
                wkAlarm.hour = viewBind.wheelHour.getValue()
                wkAlarm.minute = viewBind.wheelMinute.getValue()
                wkAlarm.isEnabled = true
                wkAlarm.repeat = alarmRepeat
                alarmCount.add(wkAlarm)
                adapter.sources = alarmCount
                setAlarm(alarmCount)
            }
        }

        //delete alarm
        viewBind.alarmDelete.clickTrigger {
            if (adapter.mDeleteIndex.size == 0) {
                toast(getString(R.string.ds_contacts_delete))
            } else {
                val sources = adapter.sources
                if (sources != null) {
                    for (i in sources.size - 1 downTo 0) {
                        if (adapter.mDeleteIndex.contains(i)) {
                            sources.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            adapter.notifyItemRangeChanged(0, sources.size)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    setAlarm(sources)
                }
            }
        }
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    fun setAlarm(alarm: ArrayList<WKAlarm>) {
        wearKit.alarmAbility.setAlarms(alarm)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.mDeleteIndex.clear()
                viewBind.alarmRy.scrollToPosition(adapter.itemCount - 1)
                adapter.notifyDataSetChanged()
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
            remindSunday.setBackgroundColor(getColor(R.color.colorPrimary))
        }
    }

}