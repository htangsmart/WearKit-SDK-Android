package com.topstep.wearkit.sample.utils

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.annotation.IntRange
import com.github.kilnn.wheellayout.WheelIntFormatter
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppUtils {

    /**
     * 中文12小时制
     */
    private val TIME_FORMAT_12_CN = "a hh:mm"

    /**
     * 英文12小时制
     */
    private val TIME_FORMAT_12_EN = "hh:mm a"

    /**
     * 24小时制
     */
    private val TIME_FORMAT_24 = "HH:mm"

    fun formatTimeBySystem(
        context: Context,
        @IntRange(from = 0, to = 23) hour: Int,
        @IntRange(from = 0, to = 59) min: Int
    ): String? {
        if (DateFormat.is24HourFormat(context)) {
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = min
            return formatDateFromTimeMillis(
                calendar.timeInMillis,
                TIME_FORMAT_24
            )
        }
        return format24to12(hour, min)
    }

    private fun formatDateFromTimeMillis(timeMillis: Long, format: String?): String? {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    private fun format24to12(
        @IntRange(from = 0, to = 23) hour: Int,
        @IntRange(from = 0, to = 59) min: Int
    ): String? {
        val instance = Calendar.getInstance()
        instance[Calendar.HOUR_OF_DAY] = hour
        instance[Calendar.MINUTE] = min
        return if (TextUtils.equals(Locale.getDefault().language, Locale.CHINESE.language)) {
            //中文
            formatDateFromTimeMillis(
                instance.timeInMillis,
                TIME_FORMAT_12_CN
            )
        } else formatDateFromTimeMillis(
            instance.timeInMillis,
            TIME_FORMAT_12_EN
        )
    }

    fun getWeek(context: Context,repeat: Int): String {
        val stringBuffer = StringBuffer()
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.MON)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_00_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.TUE)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_01_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.WED)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_02_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.THU)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_03_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.FRI)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_04_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SAT)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_05_simple))
        }
        if (WeekRepeatFlag.isRepeatEnabled(repeat, WeekRepeatFlag.SUN)) {
            stringBuffer.append(context.getString(R.string.ds_alarm_repeat_06_simple))
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

}