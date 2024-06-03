package com.topstep.wearkit.sample.utils

import android.content.Context
import android.os.Build
import com.github.kilnn.wheellayout.WheelIntFormatter
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import java.util.Locale

object AppUtils {

    fun getWeek(context: Context, repeat: Int): String {
        val stringBuffer = StringBuffer()
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.MON)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_00))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.TUE)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_01))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.WED)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_02))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.THU)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_03))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.FRI)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_04))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SAT)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_05))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SUN)) {
            stringBuffer.append(context.getString(R.string.remind_repeat_06))
        }
        return stringBuffer.toString()
    }

    fun get02dWheelIntFormatter(context: Context): WheelIntFormatter {
        return object : WheelIntFormatter {
            override fun format(index: Int, value: Int): String {
                return String.format(getSystemLocale(context), "%02d", value)
            }
        }
    }

    fun getSystemLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    fun minute2Duration(minute: Int): String {
        return String.format(getSystemLocale(MyApplication.instance), "%02d:%02d", minute / 60, minute % 60)
    }

}