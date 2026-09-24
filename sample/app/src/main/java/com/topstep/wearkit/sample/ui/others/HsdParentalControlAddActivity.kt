package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdParentalControl
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdParentalControlAddBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import com.topstep.wearkit.sample.utils.AppUtils

class HsdParentalControlAddActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdParentalControlAddBinding

    private var startMinutes: Int = 8 * 60
    private var endMinutes: Int = 12 * 60
    private val periods = ArrayList<HsdParentalControl.Period>()
    private var isEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdParentalControlAddBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        isEdit = intent.getBooleanExtra(EXTRA_EDIT, false)
        supportActionBar?.setTitle(
            if (isEdit) R.string.hsd_parental_control_edit else R.string.hsd_parental_control_add
        )

        viewBind.itemItemEnabled.getSwitchView().isChecked = true
        applyRepeatToUI(WEEKDAYS)
        restoreItem(intent)

        viewBind.spinnerId.isEnabled = !isEdit
        viewBind.itemStartTime.getTextView().text = AppUtils.minute2Duration(startMinutes)
        viewBind.itemEndTime.getTextView().text = AppUtils.minute2Duration(endMinutes)
        refreshPeriods()

        viewBind.itemStartTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = startMinutes,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEndTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = endMinutes,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }
        viewBind.btnAddPeriod.clickTrigger {
            addPeriod()
        }
        viewBind.btnSave.clickTrigger {
            saveItem()
        }
    }

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_START -> {
                startMinutes = timeMinute
                viewBind.itemStartTime.getTextView().text = AppUtils.minute2Duration(startMinutes)
            }
            DIALOG_END -> {
                endMinutes = timeMinute
                viewBind.itemEndTime.getTextView().text = AppUtils.minute2Duration(endMinutes)
            }
        }
    }

    private fun restoreItem(intent: Intent) {
        if (!intent.hasExtra(EXTRA_ID)) {
            return
        }
        val item = parseResult(intent) ?: return
        viewBind.spinnerId.setSelection((item.id - 1).coerceIn(0, viewBind.spinnerId.count - 1))
        viewBind.itemItemEnabled.getSwitchView().isChecked = item.isEnabled
        viewBind.spinnerMode.setSelection(
            if (item.mode == HsdParentalControl.Mode.ALLOW) 1 else 0
        )
        periods.clear()
        periods.addAll(item.periods)
        val last = periods.lastOrNull() ?: return
        startMinutes = last.start
        endMinutes = last.end
        applyRepeatToUI(last.repeat)
    }

    private fun addPeriod() {
        val maxPeriod = wearKit.b2b.hsdAbility.compat.getParentalControlMaxPeriodNumber()
        if (periods.size >= maxPeriod) {
            toast(R.string.tip_failed)
            return
        }
        periods.add(currentPeriod())
        refreshPeriods()
        toast(R.string.tip_success)
    }

    private fun saveItem() {
        if (periods.isEmpty()) {
            periods.add(currentPeriod())
        } else {
            periods[periods.lastIndex] = currentPeriod()
        }
        val item = HsdParentalControl.Item(
            id = viewBind.spinnerId.selectedItemPosition + 1,
            isEnabled = viewBind.itemItemEnabled.getSwitchView().isChecked,
            mode = if (viewBind.spinnerMode.selectedItemPosition == 0) {
                HsdParentalControl.Mode.BLOCK
            } else {
                HsdParentalControl.Mode.ALLOW
            },
            periods = periods.toList(),
        )
        setResult(RESULT_OK, createResultIntent(item))
        finish()
    }

    private fun currentPeriod(): HsdParentalControl.Period {
        return HsdParentalControl.Period(
            start = startMinutes,
            end = endMinutes,
            repeat = getRepeatFromUI(),
        )
    }

    @SuppressLint("SetTextI18n")
    private fun refreshPeriods() {
        if (periods.isEmpty()) {
            viewBind.tvPeriods.text = ""
            return
        }
        viewBind.tvPeriods.text = periods.mapIndexed { index, period ->
            "[$index] ${hsdParentalControlPeriodText(this, period)}"
        }.joinToString("\n")
    }

    private fun getRepeatFromUI(): Int {
        var repeat = 0
        if (viewBind.cbMon.isChecked) repeat = repeat or WeekRepeatFlag.MON
        if (viewBind.cbTue.isChecked) repeat = repeat or WeekRepeatFlag.TUE
        if (viewBind.cbWed.isChecked) repeat = repeat or WeekRepeatFlag.WED
        if (viewBind.cbThu.isChecked) repeat = repeat or WeekRepeatFlag.THU
        if (viewBind.cbFri.isChecked) repeat = repeat or WeekRepeatFlag.FRI
        if (viewBind.cbSat.isChecked) repeat = repeat or WeekRepeatFlag.SAT
        if (viewBind.cbSun.isChecked) repeat = repeat or WeekRepeatFlag.SUN
        return repeat
    }

    private fun applyRepeatToUI(repeat: Int) {
        viewBind.cbMon.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.MON)
        viewBind.cbTue.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.TUE)
        viewBind.cbWed.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.WED)
        viewBind.cbThu.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.THU)
        viewBind.cbFri.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.FRI)
        viewBind.cbSat.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SAT)
        viewBind.cbSun.isChecked = WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SUN)
    }

    companion object {
        private const val DIALOG_START = "parental_control_start"
        private const val DIALOG_END = "parental_control_end"
        private const val EXTRA_EDIT = "edit"
        private const val EXTRA_ID = "id"
        private const val EXTRA_ITEM_ENABLED = "item_enabled"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_STARTS = "starts"
        private const val EXTRA_ENDS = "ends"
        private const val EXTRA_REPEATS = "repeats"

        private val WEEKDAYS =
            WeekRepeatFlag.MON or WeekRepeatFlag.TUE or WeekRepeatFlag.WED or
                WeekRepeatFlag.THU or WeekRepeatFlag.FRI

        fun createIntent(context: Context, item: HsdParentalControl.Item? = null): Intent {
            return Intent(context, HsdParentalControlAddActivity::class.java).apply {
                if (item != null) {
                    putExtra(EXTRA_EDIT, true)
                    putExtras(createResultIntent(item))
                }
            }
        }

        fun parseResult(intent: Intent?): HsdParentalControl.Item? {
            intent ?: return null
            if (!intent.hasExtra(EXTRA_ID)) {
                return null
            }
            val starts = intent.getIntArrayExtra(EXTRA_STARTS) ?: intArrayOf()
            val ends = intent.getIntArrayExtra(EXTRA_ENDS) ?: intArrayOf()
            val repeats = intent.getIntArrayExtra(EXTRA_REPEATS) ?: intArrayOf()
            if (starts.size != ends.size || starts.size != repeats.size) {
                return null
            }
            return HsdParentalControl.Item(
                id = intent.getIntExtra(EXTRA_ID, 0),
                isEnabled = intent.getBooleanExtra(EXTRA_ITEM_ENABLED, true),
                mode = intent.getIntExtra(EXTRA_MODE, HsdParentalControl.Mode.BLOCK),
                periods = starts.indices.map { index ->
                    HsdParentalControl.Period(
                        start = starts[index],
                        end = ends[index],
                        repeat = repeats[index],
                    )
                },
            )
        }

        private fun createResultIntent(item: HsdParentalControl.Item): Intent {
            return Intent().apply {
                putExtra(EXTRA_ID, item.id)
                putExtra(EXTRA_ITEM_ENABLED, item.isEnabled)
                putExtra(EXTRA_MODE, item.mode)
                putExtra(EXTRA_STARTS, item.periods.map { it.start }.toIntArray())
                putExtra(EXTRA_ENDS, item.periods.map { it.end }.toIntArray())
                putExtra(EXTRA_REPEATS, item.periods.map { it.repeat }.toIntArray())
            }
        }
    }
}
