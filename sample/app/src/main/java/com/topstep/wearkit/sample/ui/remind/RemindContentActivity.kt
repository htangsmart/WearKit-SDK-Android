package com.topstep.wearkit.sample.ui.remind

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKRemind
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityRemindContentBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.AppUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class RemindContentActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityRemindContentBinding
    private lateinit var adapter: RemindTimeAdapter
    private var remindId: Int? = null
    private var remindName: String? = null
    private var isAdd: Boolean = false
    private var mWKRemind: WKRemind? = null
    private var remindRepeat = 0
    private val tagAddTime = "addTime"
    private val tagStartTime = "startTime"
    private val tagEndTime = "endTime"
    private val tagInterval = "tagInterval"
    private val remindFixedTimeList = ArrayList<RemindTime>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRemindContentBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        adapter = RemindTimeAdapter()
        supportActionBar?.title = "remindContent"
        remindId = intent.getIntExtra(REMIND_ID, -1)
        remindName = intent.getStringExtra(REMIND_NAME)
        isAdd = intent.getBooleanExtra(REMIND_IS_ADD, false)
        initView()
        initData()
        initEvent()
    }

    private fun initView() {
        viewBind.remindMonday.clickTrigger(block = blockClick)
        viewBind.remindTuesday.clickTrigger(block = blockClick)
        viewBind.remindWednesday.clickTrigger(block = blockClick)
        viewBind.remindThursday.clickTrigger(block = blockClick)
        viewBind.remindFriday.clickTrigger(block = blockClick)
        viewBind.remindSaturday.clickTrigger(block = blockClick)
        viewBind.remindSunday.clickTrigger(block = blockClick)
        viewBind.remindFixedTime.clickTrigger(block = blockClick)
        viewBind.remindFixedInterval.clickTrigger(block = blockClick)
        viewBind.remindAdd.clickTrigger(block = blockClick)
        viewBind.remindLayout.clickTrigger(block = blockClick)
        viewBind.itemStart.clickTrigger(block = blockClick)
        viewBind.itemEnd.clickTrigger(block = blockClick)
        viewBind.itemNumber.clickTrigger(block = blockClick)
        if (!wearKit.remindAbility.compat.isSupportField(transId2Type(remindId!!).javaClass, WKRemind.Field.DND)) {
            viewBind.remindNotDisturb.visibility = View.GONE
        }
    }

    private fun initData() {
        if (isAdd) {
            viewBind.remindConCustomLl.visibility = View.VISIBLE
            if (remindId != null) {
                mWKRemind = WKRemind(transId2Type(remindId!!))
                bindData(mWKRemind)
            }
        } else {
            viewBind.remindConCustomLl.visibility = View.GONE
            if (transId2Type(remindId!!) != WKRemind.Type.Sedentary
                && transId2Type(remindId!!) != WKRemind.Type.DrinkWater
                && transId2Type(remindId!!) != WKRemind.Type.TakeMedicine
            ) {
                viewBind.btnCommit.text = getText(R.string.action_delete)
            }

            wearKit.remindAbility.requestRemind(transId2Type(remindId!!))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.value != null) {
                        mWKRemind = it.value
                        bindData(mWKRemind)
                    }
                }, {
                    Timber.i(it)
                })
        }

        viewBind.editContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewBind.tvContentLimitTips.text = "${viewBind.editContent.length()} / 50"
            }
        })

        adapter.listener = object : RemindTimeAdapter.Listener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemDelete(position: Int) {
                val sources = adapter.sources
                if (sources != null) {
                    for (i in sources.size - 1 downTo 0) {
                        if (position == i) {
                            sources.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            adapter.notifyItemRangeChanged(0, sources.size)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    val newsSources = adapter.sources
                    if (newsSources.isNullOrEmpty()) {
                        viewBind.remindAdd.visibility = View.VISIBLE
                        viewBind.remindLayout.visibility = View.GONE
                    } else {
                        viewBind.remindLayout.visibility = View.VISIBLE
                        viewBind.remindAdd.visibility = View.GONE
                    }
                    val list = ArrayList<Int>()
                    for (time in newsSources!!) {
                        list.add(time.time)
                    }
                    if (mWKRemind != null) {
                        mWKRemind!!.times = list
                        setCommitState()
                    }
                }
            }
        }
    }

    private fun initEvent() {
        viewBind.btnCommit.clickTrigger {
            if (mWKRemind != null) {
                if (viewBind.btnCommit.text.trim() == getString(R.string.action_delete)) {
                    //delete
                    wearKit.remindAbility.deleteRemind(WKRemind.Type.Custom(type2TransId(mWKRemind!!.type)))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            toast(getString(R.string.tip_success))
                            finish()
                        }, {
                            toast(getString(R.string.tip_failed))
                        })

                } else {
                    //add or update
                    if (isAdd) {
                        if (TextUtils.isEmpty(viewBind.edit.text.toString().trim())) {
                            toast(getString(R.string.remind_custom_name))
                            return@clickTrigger
                        }
                        mWKRemind!!.name = viewBind.edit.text.toString().trim()
                    }
                    mWKRemind!!.note = viewBind.editContent.text.toString().trim()
                    mWKRemind!!.isEnabled = true
                    wearKit.remindAbility.addOrUpdateRemind(mWKRemind!!)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            toast(getString(R.string.tip_success))
                            finish()
                        }, {
                            toast(getString(R.string.tip_failed))
                        })
                }

            }
        }
    }


    private fun bindData(mWKRemind: WKRemind?) {
        if (mWKRemind != null) {
            remindRepeat = mWKRemind.repeat
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.MON)) {
                viewBind.remindMonday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.TUE)) {
                viewBind.remindTuesday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.WED)) {
                viewBind.remindWednesday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.THU)) {
                viewBind.remindThursday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.FRI)) {
                viewBind.remindFriday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.SAT)) {
                viewBind.remindSaturday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (WeekRepeatFlag.isRepeatEnabled(mWKRemind.repeat, WeekRepeatFlag.SUN)) {
                viewBind.remindSunday.setBackgroundColor(getColor(R.color.colorPrimary))
            }
            if (transId2Type(remindId!!) == WKRemind.Type.Sedentary) {
                viewBind.remindType.text = getString(R.string.remind_monitor)
                viewBind.itemNumber.getTextView().text = "${mWKRemind.interval}"
                //  Sedentary reminder
                viewBind.remindNotDisturb.visibility = View.VISIBLE
                viewBind.remindIntervalLl.visibility = View.VISIBLE
                viewBind.remindType.visibility = View.GONE
                viewBind.itemNumber.getTitleView().text = getString(R.string.remind_Sedentary_time)
                viewBind.remindType3.text = getString(R.string.sedentary_setting)
                viewBind.remindTimesLl.visibility = View.GONE
                viewBind.remindFrequencyLl.visibility = View.GONE
                viewBind.remindType2.visibility = View.VISIBLE
            } else {
                viewBind.remindType.text = getString(R.string.remind_way)
                viewBind.remindFrequencyLl.visibility = View.VISIBLE
                viewBind.itemStart.getTitleView().text = getString(R.string.remind_start)
                viewBind.itemEnd.getTitleView().text = getString(R.string.remind_end)
                if (mWKRemind.mode == WKRemind.Mode.TIMES) {
                    // Fixed time
                    viewBind.remindFixedTime.setTextColor(getColor(R.color.colorPrimary))
                    viewBind.remindFixedInterval.setTextColor(getColor(R.color.black))
                    viewBind.remindTimesLl.visibility = View.VISIBLE
                    viewBind.remindIntervalLl.visibility = View.GONE
                    viewBind.remindNotDisturb.visibility = View.GONE
                    viewBind.remindType2.visibility = View.GONE
                    viewBind.remindType3.visibility = View.GONE
                    viewBind.remindNumber.visibility = View.GONE
                } else if (mWKRemind.mode == WKRemind.Mode.PERIOD) {
                    // Fixed interval
                    viewBind.remindFixedInterval.setTextColor(getColor(R.color.colorPrimary))
                    viewBind.remindFixedTime.setTextColor(getColor(R.color.black))
                    viewBind.remindIntervalLl.visibility = View.VISIBLE
                    viewBind.remindType2.visibility = View.VISIBLE
                    viewBind.remindType3.visibility = View.VISIBLE
                    viewBind.remindNotDisturb.visibility = View.VISIBLE
                    viewBind.remindType.visibility = View.VISIBLE
                    viewBind.remindTimesLl.visibility = View.GONE
                    viewBind.remindNumber.visibility = View.VISIBLE
                }
            }
            viewBind.itemNumber.getTextView().text = AppUtils.minute2Duration(mWKRemind.interval)
            //times
            val times = mWKRemind.times
            if (times.isEmpty()) {
                viewBind.remindAdd.visibility = View.VISIBLE
                viewBind.remindLayout.visibility = View.GONE
            } else {
                viewBind.remindLayout.visibility = View.VISIBLE
                viewBind.remindAdd.visibility = View.GONE
                remindFixedTimeList.clear()
                for (time in times) {
                    remindFixedTimeList.add(RemindTime(time))
                }
            }
            viewBind.remindRe.layoutManager = LinearLayoutManager(this)
            adapter.sources = remindFixedTimeList
            viewBind.remindRe.adapter = adapter
            adapter.notifyDataSetChanged()
            viewBind.itemStart.getTextView().text = AppUtils.minute2Duration(mWKRemind.start)
            viewBind.itemEnd.getTextView().text = AppUtils.minute2Duration(mWKRemind.end)
            viewBind.remindSwitch.isChecked = mWKRemind.dnd.isEnabled
            viewBind.editContent.setText(mWKRemind.note)
            viewBind.remindSwitch.setOnCheckedChangeListener { compoundButton, checked ->
                if (compoundButton.isPressed) {
                    if (mWKRemind != null) {
                        mWKRemind.dnd = mWKRemind.dnd.copy(isEnabled = checked)
                        setCommitState()
                    }
                }
            }
        }
    }


    private val blockClick: (View) -> Unit = blockClick@{ view ->
        when (view) {
            viewBind.remindSunday -> {
                setRemindWeek(WeekRepeatFlag.SUN, viewBind.remindSunday)
            }
            viewBind.remindMonday -> {
                setRemindWeek(WeekRepeatFlag.MON, viewBind.remindMonday)
            }
            viewBind.remindTuesday -> {
                setRemindWeek(WeekRepeatFlag.TUE, viewBind.remindTuesday)
            }
            viewBind.remindWednesday -> {
                setRemindWeek(WeekRepeatFlag.WED, viewBind.remindWednesday)
            }
            viewBind.remindThursday -> {
                setRemindWeek(WeekRepeatFlag.THU, viewBind.remindThursday)
            }
            viewBind.remindFriday -> {
                setRemindWeek(WeekRepeatFlag.FRI, viewBind.remindFriday)
            }
            viewBind.remindSaturday -> {
                setRemindWeek(WeekRepeatFlag.SAT, viewBind.remindSaturday)
            }
            viewBind.remindFixedTime -> {
                viewBind.remindFixedTime.setTextColor(getColor(R.color.colorPrimary))
                viewBind.remindFixedInterval.setTextColor(getColor(R.color.black))
                viewBind.remindTimesLl.visibility = View.VISIBLE
                viewBind.remindIntervalLl.visibility = View.GONE
                viewBind.remindNotDisturb.visibility = View.GONE
                viewBind.remindType2.visibility = View.GONE
                viewBind.remindType3.visibility = View.GONE
                viewBind.remindNumber.visibility = View.GONE
                setCommitState()
                if (mWKRemind != null) {
                    mWKRemind!!.mode = WKRemind.Mode.TIMES
                }
            }
            viewBind.remindFixedInterval -> {
                viewBind.remindFixedInterval.setTextColor(getColor(R.color.colorPrimary))
                viewBind.remindFixedTime.setTextColor(getColor(R.color.black))
                viewBind.remindIntervalLl.visibility = View.VISIBLE
                viewBind.remindNotDisturb.visibility = View.VISIBLE
                viewBind.remindType2.visibility = View.VISIBLE
                viewBind.remindType3.visibility = View.VISIBLE
                viewBind.remindNumber.visibility = View.VISIBLE
                viewBind.remindTimesLl.visibility = View.GONE
                setCommitState()
                if (mWKRemind != null) {
                    mWKRemind!!.mode = WKRemind.Mode.PERIOD
                }
            }
            viewBind.remindLayout,
            viewBind.remindAdd,
            -> {
                TimePickerDialogFragment.newInstance(
                    des1 = getString(R.string.unit_hour),
                    des2 = getString(R.string.unit_minute)
                ).show(supportFragmentManager, tagAddTime)
            }
            viewBind.itemStart -> {
                TimePickerDialogFragment.newInstance(
                    des1 = getString(R.string.unit_hour),
                    des2 = getString(R.string.unit_minute)
                ).show(supportFragmentManager, tagStartTime)
            }
            viewBind.itemEnd -> {
                TimePickerDialogFragment.newInstance(
                    des1 = getString(R.string.unit_hour),
                    des2 = getString(R.string.unit_minute)
                ).show(supportFragmentManager, tagEndTime)
            }
            viewBind.itemNumber -> {
                if (transId2Type(remindId!!) != WKRemind.Type.Sedentary) {
                    TimePickerDialogFragment.newInstance(
                        start = 10,
                        end = 10 * 60 + 50,
                        des1 = getString(R.string.unit_hour),
                        des2 = getString(R.string.unit_minute)
                    ).show(supportFragmentManager, tagInterval)
                } else {
                    TimePickerDialogFragment.newInstance(
                        start = 60,
                        end = 6 * 60,
                        des1 = getString(R.string.unit_hour),
                        des2 = getString(R.string.unit_minute)
                    ).show(supportFragmentManager, tagInterval)
                }
            }
        }
    }

    private fun setRemindWeek(flag: Int, remindSunday: TextView) {
        if (WeekRepeatFlag.isRepeatEnabled(remindRepeat, flag)) {
            remindRepeat = WeekRepeatFlag.setRepeatEnabled(remindRepeat, flag, false)
            remindSunday.background = null
        } else {
            remindRepeat = WeekRepeatFlag.setRepeatEnabled(remindRepeat, flag, true)
            remindSunday.setBackgroundColor(getColor(R.color.colorPrimary))
        }
        setCommitState()
        if (mWKRemind != null) {
            mWKRemind!!.repeat = remindRepeat
        }
    }

    private fun setCommitState() {
        if (!isAdd) {
            viewBind.btnCommit.text = getString(R.string.action_save)
        }
    }

    override fun onDialogTimePicker(tag: String?, selectValue: Int) {
        if (tag == tagAddTime) {
            adapter.sources?.add(RemindTime(selectValue))
            adapter.notifyDataSetChanged()
            setCommitState()
            val sources = adapter.sources
            if (sources.isNullOrEmpty()) {
                viewBind.remindAdd.visibility = View.VISIBLE
                viewBind.remindLayout.visibility = View.GONE
            } else {
                viewBind.remindLayout.visibility = View.VISIBLE
                viewBind.remindAdd.visibility = View.GONE
                val list = ArrayList<Int>()
                for (time in sources) {
                    list.add(time.time)
                }
                if (mWKRemind != null) {
                    mWKRemind!!.times = list
                }
            }
        } else if (tag == tagStartTime) {
            // start time
            viewBind.itemStart.getTextView().text = AppUtils.minute2Duration(selectValue)
            setCommitState()
            if (mWKRemind != null) {
                mWKRemind!!.start = selectValue
            }
        } else if (tag == tagEndTime) {
            // end time
            viewBind.itemEnd.getTextView().text = AppUtils.minute2Duration(selectValue)
            setCommitState()
            if (mWKRemind != null) {
                mWKRemind!!.end = selectValue
            }
        } else if (tag == tagInterval) {
            viewBind.itemNumber.getTextView().text = AppUtils.minute2Duration(selectValue)
            setCommitState()
            if (mWKRemind != null) {
                mWKRemind!!.interval = selectValue
            }
        }
    }


    companion object {
        private const val REMIND_ID = "remind_id"
        private const val REMIND_NAME = "remind_name"
        private const val REMIND_IS_ADD = "remind_isAdd"

        @JvmStatic
        fun startActivity(context: Context?, id: Int?, name: String?, isAdd: Boolean) {
            if (context == null) return
            val intent = Intent(context, RemindContentActivity::class.java)
            intent.putExtra(REMIND_ID, id)
            intent.putExtra(REMIND_NAME, name)
            intent.putExtra(REMIND_IS_ADD, isAdd)
            context.startActivity(intent)
        }

        fun type2TransId(type: WKRemind.Type): Int {
            return when (type) {
                WKRemind.Type.Sedentary -> -1
                WKRemind.Type.DrinkWater -> -2
                WKRemind.Type.TakeMedicine -> -3
                is WKRemind.Type.Custom -> type.id
            }
        }

        fun transId2Type(id: Int): WKRemind.Type {
            return when (id) {
                -1 -> WKRemind.Type.Sedentary
                -2 -> WKRemind.Type.DrinkWater
                -3 -> WKRemind.Type.TakeMedicine
                else -> WKRemind.Type.Custom(id)
            }
        }
    }
}