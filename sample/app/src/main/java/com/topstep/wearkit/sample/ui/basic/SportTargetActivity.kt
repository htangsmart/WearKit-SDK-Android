package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.config.WKActivityGoalConfig
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityTargetBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.remind.SelectIntDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult", "SetTextI18n")
class SportTargetActivity : BaseActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityTargetBinding

    private var consume = 0
    private var steps = 0
    private var activityTime = 0

    private val tagConsume = "tagConsume"
    private val tagSteps = "tagSteps"
    private val tagActivityTime = "tagActivityTime"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityTargetBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.activity_target)
        val config = wearKit.activityAbility.getGoalConfig()
        setActivityData(config.steps, config.calories.toInt(), config.sportDuration / 60)

        viewBind.targetConsume.clickTrigger {
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 100,
                multiples = 10,
                value = 1,
                title = null,
                des = getString(R.string.unit_k_calories)
            ).show(supportFragmentManager, tagConsume)
        }

        viewBind.targetStep.clickTrigger {
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 20,
                multiples = 1000,
                value = 1,
                title = null,
                des = getString(R.string.data_step)
            ).show(supportFragmentManager, tagSteps)
        }

        viewBind.targetActivityTime.clickTrigger {
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 6,
                multiples = 10,
                value = 1,
                title = null,
                des = getString(R.string.unit_minute)
            ).show(supportFragmentManager, tagActivityTime)
        }

        viewBind.targetSet.clickTrigger {
            wearKit.activityAbility.setGoalConfig(
                WKActivityGoalConfig(
                    timestampSeconds = 0,
                    steps = steps,
                    distance = 0.0,
                    calories = consume.toDouble(),
                    duration = 0,
                    number = 1,
                    sportDuration = activityTime * 60
                )
            ).observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.i(it)
                })
        }
    }

    private fun setActivityData(activitySteps: Int, calories: Int, activityDuration: Int) {
        consume = calories
        steps = activitySteps
        activityTime = activityDuration
        viewBind.targetConsume.getTextView().text = "$consume" + getString(R.string.unit_k_calories)
        viewBind.targetStep.getTextView().text = "$steps" + getString(R.string.unit_step)
        viewBind.targetActivityTime.getTextView().text = "$activityTime" + getString(R.string.unit_minute)
    }

    override fun dialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            tagConsume -> {
                consume = selectValue
                viewBind.targetConsume.getTextView().text = "$consume" + getString(R.string.unit_k_calories)
            }
            tagSteps -> {
                steps = selectValue
                viewBind.targetStep.getTextView().text = "$steps" + getString(R.string.unit_step)
            }
            tagActivityTime -> {
                activityTime = selectValue
                viewBind.targetActivityTime.getTextView().text = "$activityTime" + getString(R.string.unit_minute)
            }
        }
    }

}