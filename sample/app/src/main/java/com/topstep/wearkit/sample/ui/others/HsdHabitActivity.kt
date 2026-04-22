package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdHabit
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdHabitBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class HsdHabitActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdHabitBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdHabitBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Habit"

        viewBind.btnSetHabits.clickTrigger {
            setHabits()
        }
        viewBind.btnRequestHabits.clickTrigger {
            requestHabits()
        }
    }

    @SuppressLint("CheckResult")
    private fun setHabits() {
        val habit = HsdHabit().apply {
            type = viewBind.spinnerType.selectedItemPosition
            label = viewBind.editLabel.text?.toString().orEmpty()
            time = parseTime(viewBind.editTime.text?.toString().orEmpty())
            duration = viewBind.editDuration.text?.toString()?.toIntOrNull() ?: 0
            taskDays = viewBind.editTaskDays.text?.toString()?.toIntOrNull() ?: 0
            remindAdvance = viewBind.editRemindAdvance.text?.toString()?.toIntOrNull() ?: 0
            remindDuration = viewBind.editRemindDuration.text?.toString()?.toIntOrNull() ?: 0
        }

        wearKit.b2b.hsdAbility.setHabits(listOf(habit))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun requestHabits() {
        wearKit.b2b.hsdAbility.requestHabits()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ habits ->
                val sb = StringBuilder()
                sb.appendLine("Habits count: ${habits.size}")
                habits.forEachIndexed { index, habit ->
                    sb.appendLine("--- Habit ${index + 1} ---")
                    sb.appendLine("  id=${habit.id}, type=${typeToString(habit.type)}, label=${habit.label}")
                    sb.appendLine("  time=${habit.time / 60}:${String.format("%02d", habit.time % 60)}, duration=${habit.duration}min")
                    sb.appendLine("  repeat=${habit.repeat}, state=${stateToString(habit.state)}")
                    sb.appendLine("  reachGoalDays=${habit.reachGoalDays}, maxReachGoalDays=${habit.maxReachGoalDays}")
                    sb.appendLine("  taskDays=${habit.taskDays}, associatedFunction=${habit.associatedFunction}")
                    sb.appendLine("  remindDuration=${habit.remindDuration}s, remindAdvance=${habit.remindAdvance}min")
                    sb.appendLine("  latestAchieveGoal=${habit.latestAchieveGoalMonth}/${habit.latestAchieveGoalDay}")
                    sb.appendLine("  achieveGoalRepeat=${habit.achieveGoalRepeat}")
                }
                viewBind.tvResult.text = sb.toString()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun parseTime(text: String): Int {
        val parts = text.split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            return h * 60 + m
        }
        return 0
    }

    private fun typeToString(type: Int): String {
        return when (type) {
            HsdHabit.Type.CUSTOM -> "Custom"
            HsdHabit.Type.SPORT -> "Sport"
            HsdHabit.Type.STUDY -> "Study"
            HsdHabit.Type.SLEEP -> "Sleep"
            else -> "Unknown($type)"
        }
    }

    private fun stateToString(state: Int): String {
        return when (state) {
            HsdHabit.State.INIT -> "Init"
            HsdHabit.State.ONGOING -> "Ongoing"
            HsdHabit.State.COMPLETED -> "Completed"
            HsdHabit.State.OVERDUE -> "Overdue"
            HsdHabit.State.CLOSED -> "Closed"
            HsdHabit.State.DELETED -> "Deleted"
            else -> "Unknown($state)"
        }
    }

}
