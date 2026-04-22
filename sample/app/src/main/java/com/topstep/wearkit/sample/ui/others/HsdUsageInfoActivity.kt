package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdUsageInfoBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class HsdUsageInfoActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdUsageInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdUsageInfoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Usage Info"

        viewBind.btnRequestAppUsage.clickTrigger {
            requestAppUsageInfo()
        }
        viewBind.btnRequestGameUsage.clickTrigger {
            requestGameUsageInfo()
        }
        viewBind.btnResetUsage.clickTrigger {
            resetUsageInfo()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun requestAppUsageInfo() {
        wearKit.b2b.hsdAbility.requestAppUsageInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ usageByDay ->
                val sb = StringBuilder()
                sb.appendLine("App Usage Info")
                sb.appendLine("Days: ${usageByDay.size}")
                usageByDay.forEachIndexed { dayIndex, dayUsage ->
                    sb.appendLine("--- Day ${dayIndex + 1} ---")
                    dayUsage.forEach { info ->
                        sb.appendLine("  type=${info.type}(${getAppTypeName(info.type)}), count=${info.count}")
                    }
                }
                viewBind.tvResult.text = sb.toString()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun requestGameUsageInfo() {
        wearKit.b2b.hsdAbility.requestGameUsageInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ usageByDay ->
                val sb = StringBuilder()
                sb.appendLine("Game Usage Info")
                sb.appendLine("Days: ${usageByDay.size}")
                usageByDay.forEachIndexed { dayIndex, dayUsage ->
                    sb.appendLine("--- Day ${dayIndex + 1} ---")
                    dayUsage.forEach { info ->
                        sb.appendLine("  type=${info.type}(${getGameTypeName(info.type)}), count=${info.count}")
                    }
                }
                viewBind.tvResult.text = sb.toString()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult")
    private fun resetUsageInfo() {
        wearKit.b2b.hsdAbility.resetUsageInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun getAppTypeName(type: Int): String {
        return when (type) {
            0 -> "DayData"
            1 -> "HeartRate"
            2 -> "Alarm"
            3 -> "Game"
            4 -> "Sleep"
            5 -> "Weather"
            6 -> "Calculator"
            7 -> "Clock"
            8 -> "Music"
            9 -> "Settings"
            10 -> "WaterIntake"
            11 -> "SedentaryAlert"
            12 -> "HealthCheck"
            13 -> "QRCode"
            14 -> "Password"
            15 -> "Dialer"
            16 -> "Contacts"
            17 -> "CallHistory"
            18 -> "VoiceAssistant"
            19 -> "Sports"
            20 -> "Messages"
            21 -> "WomenHealth"
            22 -> "BreathingTraining"
            23 -> "Camera"
            24 -> "FindPhone"
            25 -> "BloodOxygen"
            26 -> "BloodPressure"
            27 -> "Stress"
            28 -> "Temperature"
            29 -> "Flashlight"
            30 -> "Timer"
            31 -> "Stopwatch"
            32 -> "Calendar"
            33 -> "WorldClock"
            34 -> "Agenda"
            35 -> "Pomodoro"
            36 -> "Menstrual"
            37 -> "Pill"
            38 -> "Habit"
            39 -> "Task"
            40 -> "Coins"
            41 -> "Pet"
            42 -> "AIChat"
            43 -> "PrayerTime"
            44 -> "Compass"
            45 -> "Altitude"
            46 -> "ECard"
            47 -> "PayCode"
            else -> "Unknown"
        }
    }

    private fun getGameTypeName(type: Int): String {
        return when (type) {
            0 -> "PetTraining"
            1 -> "2048"
            2 -> "CandyCrush"
            3 -> "Puzzle"
            4 -> "Airplane"
            5 -> "Racing"
            6 -> "Maze"
            7 -> "Basketball"
            8 -> "Math"
            9 -> "Tetris"
            10 -> "Sudoku"
            11 -> "QA"
            12 -> "Jump"
            13 -> "Stacking"
            14 -> "Connect"
            15 -> "Snake"
            16 -> "24Points"
            17 -> "CandyJump"
            18 -> "Breakout"
            19 -> "3PBasketball"
            20 -> "SoccerTarget"
            21 -> "CuteBlocks"
            22 -> "Gomoku"
            23 -> "BirdTap"
            24 -> "5ColorBricks"
            else -> "Unknown"
        }
    }

}
