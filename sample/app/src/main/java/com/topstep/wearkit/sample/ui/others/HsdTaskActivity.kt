package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdTask
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdTaskBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class HsdTaskActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdTaskBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdTaskBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Task & Reward"

        viewBind.btnSetTasks.clickTrigger {
            setTasks()
        }
        viewBind.btnRequestTasks.clickTrigger {
            requestTasks()
        }
        viewBind.btnExchangeReward.clickTrigger {
            exchangeReward()
        }
    }

    @SuppressLint("CheckResult")
    private fun setTasks() {
        val coins = viewBind.editCoins.text?.toString()?.toIntOrNull() ?: 0
        val task = HsdTask().apply {
            label = viewBind.editTaskLabel.text?.toString().orEmpty()
            description = viewBind.editTaskDescription.text?.toString().orEmpty()
            time = parseTime(viewBind.editTaskTime.text?.toString().orEmpty())
            this.coins = viewBind.editTaskCoins.text?.toString()?.toIntOrNull() ?: 0
            isEnabled = viewBind.cbTaskEnabled.isChecked
            isTimeEnabled = viewBind.cbTaskTimeEnabled.isChecked
        }

        wearKit.b2b.hsdAbility.setTasks(coins, listOf(task))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun requestTasks() {
        wearKit.b2b.hsdAbility.requestTasks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (coins, tasks) ->
                viewBind.editCoins.setText(coins.toString())
                val sb = StringBuilder()
                sb.appendLine("Total Coins: $coins")
                sb.appendLine("Tasks count: ${tasks.size}")
                tasks.forEachIndexed { index, task ->
                    sb.appendLine("--- Task ${index + 1} ---")
                    sb.appendLine("  id=${task.id}, label=${task.label}")
                    sb.appendLine("  time=${task.time / 60}:${String.format("%02d", task.time % 60)}")
                    sb.appendLine("  enabled=${task.isEnabled}, timeEnabled=${task.isTimeEnabled}")
                    sb.appendLine("  type=${task.type}, state=${task.state}, coins=${task.coins}")
                    sb.appendLine("  description=${task.description}")
                }
                viewBind.tvResult.text = sb.toString()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult")
    private fun exchangeReward() {
        wearKit.b2b.hsdAbility.exchangeTaskReward()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ needRefresh ->
                if (needRefresh) {
                    viewBind.tvResult.text = "Exchange success, refreshing tasks..."
                    requestTasks()
                } else {
                    viewBind.tvResult.text = "Exchange result: no change"
                }
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

}
