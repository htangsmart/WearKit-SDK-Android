package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKActivityGoalConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityGoalConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class GoalConfigActivity : BaseActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityGoalConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityGoalConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.activity_goal)
        observeDispose = wearKit.activityAbility.observeGoalConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemStep.getTextView().text = it.steps.toString() + " " + getString(R.string.unit_step)
                viewBind.itemDistance.getTextView().text = (it.distance / 1000).toInt().toString() + " " + getString(R.string.unit_km)
                viewBind.itemCalories.getTextView().text = it.calories.toInt().toString() + " " + getString(R.string.unit_k_calories)
                viewBind.itemDuration.getTextView().text = (it.duration / 60).toString() + " " + getString(R.string.unit_minute)
                viewBind.itemNumber.getTextView().text = it.number.toString()
                viewBind.itemSportDuration.getTextView().text = (it.sportDuration / 60).toString() + " " + getString(R.string.unit_minute)
            }, {
                Timber.w(it)
            })

        viewBind.itemStep.setOnClickListener {
            val value = wearKit.activityAbility.getGoalConfig().steps
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 50,
                multiples = 1000,
                value = value,
                title = getString(R.string.activity_goal_step),
                des = getString(R.string.unit_step)
            ).show(supportFragmentManager, DIALOG_STEP)
        }
        viewBind.itemDistance.setOnClickListener {
            val value = (wearKit.activityAbility.getGoalConfig().distance / 1000).toInt()//meters to km
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 50,
                value = value,
                title = getString(R.string.activity_goal_distance),
                des = getString(R.string.unit_km)
            ).show(supportFragmentManager, DIALOG_DISTANCE)
        }
        viewBind.itemCalories.setOnClickListener {
            val value = wearKit.activityAbility.getGoalConfig().calories.toInt()
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 50,
                multiples = 30,
                value = value,
                title = getString(R.string.activity_goal_calories),
                des = getString(R.string.unit_k_calories)
            ).show(supportFragmentManager, DIALOG_CALORIES)
        }
        viewBind.itemDuration.setOnClickListener {
            val value = wearKit.activityAbility.getGoalConfig().duration / 60//seconds to minutes
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 60,
                multiples = 10,
                value = value,
                title = getString(R.string.activity_goal_duration),
                des = getString(R.string.unit_minute)
            ).show(supportFragmentManager, DIALOG_DURATION)
        }
        viewBind.itemNumber.setOnClickListener {
            val value = wearKit.activityAbility.getGoalConfig().number
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 30,
                value = value,
                title = getString(R.string.activity_goal_number),
            ).show(supportFragmentManager, DIALOG_NUMBER)
        }
        viewBind.itemSportDuration.setOnClickListener {
            val value = wearKit.activityAbility.getGoalConfig().sportDuration / 60//seconds to minutes
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 60,
                multiples = 10,
                value = value,
                title = getString(R.string.activity_goal_sport_duration),
                des = getString(R.string.unit_minute)
            ).show(supportFragmentManager, DIALOG_SPORT_DURATION)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getGoalConfig()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        viewBind.tvTips.text = it.toString()
                    }, {
                        viewBind.tvTips.text = it.toString()
                    })
            }
        } else {
            viewBind.itemPbTestGetConfig.isVisible = false
        }
    }

    private fun setGoal(config: WKActivityGoalConfig) {
        setDispose?.dispose()
        setDispose = wearKit.activityAbility.setGoalConfig(config)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("Set Success")
            }, { throwable ->
                viewBind.tvTips.text = throwable.toString()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        observeDispose?.dispose()
        getDispose?.dispose()
        setDispose?.dispose()
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            DIALOG_STEP -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        steps = selectValue
                    )
                )
            }
            DIALOG_DISTANCE -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        distance = selectValue * 1000.0
                    )
                )
            }
            DIALOG_CALORIES -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        calories = selectValue.toDouble()
                    )
                )
            }
            DIALOG_DURATION -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        duration = selectValue * 60
                    )
                )
            }
            DIALOG_NUMBER -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        number = selectValue
                    )
                )
            }
            DIALOG_SPORT_DURATION -> {
                setGoal(
                    wearKit.activityAbility.getGoalConfig().copy(
                        sportDuration = selectValue * 60
                    )
                )
            }
        }
    }

    companion object {
        private const val DIALOG_STEP = "step"
        private const val DIALOG_DISTANCE = "distance"
        private const val DIALOG_CALORIES = "calories"
        private const val DIALOG_DURATION = "duration"
        private const val DIALOG_NUMBER = "number"
        private const val DIALOG_SPORT_DURATION = "sport_duration"
    }
}