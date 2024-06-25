package com.topstep.wearkit.sample.utils

import android.content.Context
import android.os.Build
import com.github.kilnn.wheellayout.WheelIntFormatter
import com.topstep.wearkit.apis.model.data.WKSportType
import com.topstep.wearkit.base.utils.WeekRepeatFlag
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import java.text.SimpleDateFormat
import java.util.Date
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

    fun intStr(value: Int): String {
        return String.format(getSystemLocale(MyApplication.instance), "%d", value)
    }

    fun secondsToHMS(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    fun convertTimestampToDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.format(Date(timestamp * 1000))
    }
    fun convertSportType(@WKSportType type: Int, context: Context): String {
        return when (type) {
            WKSportType.OUTDOOR_RIDE -> context.getString(R.string.sport_ui_type_000)
            WKSportType.OUTDOOR_RUN -> context.getString(R.string.sport_ui_type_001)
            WKSportType.INDOOR_RUN -> context.getString(R.string.sport_ui_type_002)
            WKSportType.OUTDOOR_WALK -> context.getString(R.string.sport_ui_type_003)
            WKSportType.MOUNTAINEERING -> context.getString(R.string.sport_ui_type_004)
            WKSportType.BASKETBALL -> context.getString(R.string.sport_ui_type_005)
            WKSportType.SWIM -> context.getString(R.string.sport_ui_type_006)
            WKSportType.BADMINTON -> context.getString(R.string.sport_ui_type_007)
            WKSportType.FOOTBALL -> context.getString(R.string.sport_ui_type_008)
            WKSportType.ELLIPTICAL_MACHINE -> context.getString(R.string.sport_ui_type_009)
            WKSportType.YOGA -> context.getString(R.string.sport_ui_type_010)
            WKSportType.TABLE_TENNIS -> context.getString(R.string.sport_ui_type_011)
            WKSportType.ROPE_SKIPPING -> context.getString(R.string.sport_ui_type_012)
            WKSportType.ROWING_MACHINE -> context.getString(R.string.sport_ui_type_013)
            WKSportType.LAZY_CAR -> context.getString(R.string.sport_ui_type_014)
            WKSportType.EXERCISE_BIKE -> context.getString(R.string.sport_ui_type_015)
            WKSportType.FREE_TRAINING -> context.getString(R.string.sport_ui_type_016)
            WKSportType.TENNIS -> context.getString(R.string.sport_ui_type_017)
            WKSportType.BASEBALL -> context.getString(R.string.sport_ui_type_018)
            WKSportType.RUGBY -> context.getString(R.string.sport_ui_type_019)
            WKSportType.CRICKET -> context.getString(R.string.sport_ui_type_020)
            WKSportType.FREE_SPORT -> context.getString(R.string.sport_ui_type_021)
            WKSportType.STRENGTH_TRAINING -> context.getString(R.string.sport_ui_type_022)
            WKSportType.INDOOR_WALK -> context.getString(R.string.sport_ui_type_023)
            WKSportType.INDOOR_RIDE -> context.getString(R.string.sport_ui_type_024)
            WKSportType.DUMBBELL -> context.getString(R.string.sport_ui_type_025)
            WKSportType.DANCE -> context.getString(R.string.sport_ui_type_026)
            WKSportType.HULA_HOOP -> context.getString(R.string.sport_ui_type_027)
            WKSportType.GOLF -> context.getString(R.string.sport_ui_type_028)
            WKSportType.LONG_JUMP -> context.getString(R.string.sport_ui_type_029)
            WKSportType.SIT_UPS -> context.getString(R.string.sport_ui_type_030)
            WKSportType.VOLLEYBALL -> context.getString(R.string.sport_ui_type_031)
            WKSportType.PARKOUR -> context.getString(R.string.sport_ui_type_032)
            WKSportType.HIKING -> context.getString(R.string.sport_ui_type_033)
            WKSportType.HOCKEY -> context.getString(R.string.sport_ui_type_034)
            WKSportType.BOATING -> context.getString(R.string.sport_ui_type_035)
            WKSportType.HIIT -> context.getString(R.string.sport_ui_type_036)
            WKSportType.SOFTBALL -> context.getString(R.string.sport_ui_type_037)
            WKSportType.CROSS_COUNTRY -> context.getString(R.string.sport_ui_type_038)
            WKSportType.SKIING -> context.getString(R.string.sport_ui_type_039)
            WKSportType.WALKING_MACHINE -> context.getString(R.string.sport_ui_type_040)
            WKSportType.RELAXATION_ACTIVITIES -> context.getString(R.string.sport_ui_type_041)
            WKSportType.CROSS_TRAINING -> context.getString(R.string.sport_ui_type_042)
            WKSportType.PILATES -> context.getString(R.string.sport_ui_type_043)
            WKSportType.CROSS_FIT -> context.getString(R.string.sport_ui_type_044)
            WKSportType.FUNCTIONAL_TRAINING -> context.getString(R.string.sport_ui_type_045)
            WKSportType.PHYSICAL_TRAINING -> context.getString(R.string.sport_ui_type_046)
            WKSportType.MIXED_AEROBIC -> context.getString(R.string.sport_ui_type_047)
            WKSportType.LATIN_DANCE -> context.getString(R.string.sport_ui_type_048)
            WKSportType.STREET_DANCE -> context.getString(R.string.sport_ui_type_049)
            WKSportType.FREE_FIGHTING -> context.getString(R.string.sport_ui_type_050)
            WKSportType.BALLET -> context.getString(R.string.sport_ui_type_051)
            WKSportType.AUSTRALIAN_FOOTBALL -> context.getString(R.string.sport_ui_type_052)
            WKSportType.BOWLING -> context.getString(R.string.sport_ui_type_053)
            WKSportType.SQUASH -> context.getString(R.string.sport_ui_type_054)
            WKSportType.CURLING -> context.getString(R.string.sport_ui_type_055)
            WKSportType.SNOWBOARDING -> context.getString(R.string.sport_ui_type_056)
            WKSportType.FISHING -> context.getString(R.string.sport_ui_type_057)
            WKSportType.FRISBEE -> context.getString(R.string.sport_ui_type_058)
            WKSportType.ALPINE_SKIING -> context.getString(R.string.sport_ui_type_059)
            WKSportType.CORE_TRAINING -> context.getString(R.string.sport_ui_type_060)
            WKSportType.OUTDOOR_SKATING -> context.getString(R.string.sport_ui_type_061)
            WKSportType.FITNESS_GAME -> context.getString(R.string.sport_ui_type_062)
            WKSportType.AEROBICS -> context.getString(R.string.sport_ui_type_063)
            WKSportType.GROUP_GYMNASTICS -> context.getString(R.string.sport_ui_type_064)
            WKSportType.BOXING_GYMNASTICS -> context.getString(R.string.sport_ui_type_065)
            WKSportType.FENCING -> context.getString(R.string.sport_ui_type_066)
            WKSportType.CLIMB_STAIRS -> context.getString(R.string.sport_ui_type_067)
            WKSportType.AMERICAN_FOOTBALL -> context.getString(R.string.sport_ui_type_068)
            WKSportType.FOAM_SHAFT -> context.getString(R.string.sport_ui_type_069)
            WKSportType.PICKLEBALL -> context.getString(R.string.sport_ui_type_070)
            WKSportType.BOXING -> context.getString(R.string.sport_ui_type_071)
            WKSportType.TAEKWONDO -> context.getString(R.string.sport_ui_type_072)
            WKSportType.KARATE -> context.getString(R.string.sport_ui_type_073)
            WKSportType.FLEXIBILITY_TRAINING -> context.getString(R.string.sport_ui_type_074)
            WKSportType.HANDBALL -> context.getString(R.string.sport_ui_type_075)
            WKSportType.HANDCAR -> context.getString(R.string.sport_ui_type_076)
            WKSportType.MEDITATION -> context.getString(R.string.sport_ui_type_077)
            WKSportType.WRESTLING -> context.getString(R.string.sport_ui_type_078)
            WKSportType.STEP_TRAINING -> context.getString(R.string.sport_ui_type_079)
            WKSportType.TAI_CHI -> context.getString(R.string.sport_ui_type_080)
            WKSportType.GYMNASTICS -> context.getString(R.string.sport_ui_type_081)
            WKSportType.TRACK_AND_FIELD -> context.getString(R.string.sport_ui_type_082)
            WKSportType.MARTIAL_ARTS -> context.getString(R.string.sport_ui_type_083)
            WKSportType.LEISURE_SPORTS -> context.getString(R.string.sport_ui_type_084)
            WKSportType.SNOW_SPORTS -> context.getString(R.string.sport_ui_type_085)
            WKSportType.LACROSSE -> context.getString(R.string.sport_ui_type_086)
            WKSportType.HORIZONTAL_BAR -> context.getString(R.string.sport_ui_type_087)
            WKSportType.PARALLEL_BARS -> context.getString(R.string.sport_ui_type_088)
            WKSportType.ROLLER_SKATING -> context.getString(R.string.sport_ui_type_089)
            WKSportType.DARTS -> context.getString(R.string.sport_ui_type_090)
            WKSportType.ARCHERY -> context.getString(R.string.sport_ui_type_091)
            WKSportType.EQUESTRIAN -> context.getString(R.string.sport_ui_type_092)
            WKSportType.SHUTTLECOCK -> context.getString(R.string.sport_ui_type_093)
            WKSportType.ICE_HOCKEY -> context.getString(R.string.sport_ui_type_094)
            WKSportType.WAIST_AND_ABDOMEN_TRAINING -> context.getString(R.string.sport_ui_type_095)
            WKSportType.VO2_MAX_TEST -> context.getString(R.string.sport_ui_type_096)
            WKSportType.JUDO -> context.getString(R.string.sport_ui_type_097)
            WKSportType.TRAMPOLINE -> context.getString(R.string.sport_ui_type_098)
            WKSportType.SKATEBOARDING -> context.getString(R.string.sport_ui_type_099)
            WKSportType.BALANCE_BIKE -> context.getString(R.string.sport_ui_type_100)
            WKSportType.BLADING -> context.getString(R.string.sport_ui_type_101)
            WKSportType.TREADMILL -> context.getString(R.string.sport_ui_type_102)
            WKSportType.DIVING -> context.getString(R.string.sport_ui_type_103)
            WKSportType.SURFING -> context.getString(R.string.sport_ui_type_104)
            WKSportType.SNORKELING -> context.getString(R.string.sport_ui_type_105)
            WKSportType.PULL_UPS -> context.getString(R.string.sport_ui_type_106)
            WKSportType.PUSH_UPS -> context.getString(R.string.sport_ui_type_107)
            WKSportType.PLANKING -> context.getString(R.string.sport_ui_type_108)
            WKSportType.ROCK_CLIMBING -> context.getString(R.string.sport_ui_type_109)
            WKSportType.HIGH_JUMP -> context.getString(R.string.sport_ui_type_110)
            WKSportType.BUNGEE_JUMPING -> context.getString(R.string.sport_ui_type_111)
            WKSportType.NATIONAL_DANCE -> context.getString(R.string.sport_ui_type_112)
            WKSportType.HUNTING -> context.getString(R.string.sport_ui_type_113)
            WKSportType.SHOOTING -> context.getString(R.string.sport_ui_type_114)
            WKSportType.MARATHON -> context.getString(R.string.sport_ui_type_115)
            WKSportType.SPINNING -> context.getString(R.string.sport_ui_type_116)
            WKSportType.BELLY_DANCE -> context.getString(R.string.sport_ui_type_117)
            WKSportType.SQUARE_DANCE -> context.getString(R.string.sport_ui_type_118)
            WKSportType.BALLROOM_DANCE -> context.getString(R.string.sport_ui_type_119)
            WKSportType.ZUMBA -> context.getString(R.string.sport_ui_type_120)
            WKSportType.JAZZ -> context.getString(R.string.sport_ui_type_121)
            WKSportType.STEPPER -> context.getString(R.string.sport_ui_type_122)
            WKSportType.CLIMBING_MACHINE -> context.getString(R.string.sport_ui_type_123)
            WKSportType.CROQUET -> context.getString(R.string.sport_ui_type_124)
            WKSportType.WATER_POLO -> context.getString(R.string.sport_ui_type_125)
            WKSportType.WALL_BALL -> context.getString(R.string.sport_ui_type_126)
            WKSportType.BILLIARDS -> context.getString(R.string.sport_ui_type_127)
            WKSportType.SEPAKTAKRAW -> context.getString(R.string.sport_ui_type_128)
            WKSportType.STRETCHING -> context.getString(R.string.sport_ui_type_129)
            WKSportType.FLOOR_EXERCISE -> context.getString(R.string.sport_ui_type_130)
            WKSportType.BARBELL -> context.getString(R.string.sport_ui_type_131)
            WKSportType.WEIGHTLIFTING -> context.getString(R.string.sport_ui_type_132)
            WKSportType.HARD_DRAWN -> context.getString(R.string.sport_ui_type_133)
            WKSportType.BOBBY_JUMP -> context.getString(R.string.sport_ui_type_134)
            WKSportType.JUMPING_JACK -> context.getString(R.string.sport_ui_type_135)
            WKSportType.UPPER_LIMB_TRAINING -> context.getString(R.string.sport_ui_type_136)
            WKSportType.LOWER_LIMB_TRAINING -> context.getString(R.string.sport_ui_type_137)
            WKSportType.BACK_TRAINING -> context.getString(R.string.sport_ui_type_138)
            WKSportType.ATV -> context.getString(R.string.sport_ui_type_139)
            WKSportType.PARAGLIDER -> context.getString(R.string.sport_ui_type_140)
            WKSportType.FLYING_KITES -> context.getString(R.string.sport_ui_type_141)
            WKSportType.TUG_OF_WAR -> context.getString(R.string.sport_ui_type_142)
            WKSportType.TRIATHLON -> context.getString(R.string.sport_ui_type_143)
            WKSportType.SNOWMOBILE -> context.getString(R.string.sport_ui_type_144)
            WKSportType.BOBSLEIGH -> context.getString(R.string.sport_ui_type_145)
            WKSportType.SLED -> context.getString(R.string.sport_ui_type_146)
            WKSportType.SKI_BOARD -> context.getString(R.string.sport_ui_type_147)
            WKSportType.CROSS_COUNTRY_SKIING -> context.getString(R.string.sport_ui_type_148)
            WKSportType.INDOOR_SKATING -> context.getString(R.string.sport_ui_type_149)
            WKSportType.KABADDI -> context.getString(R.string.sport_ui_type_150)
            WKSportType.MUAY_THAI -> context.getString(R.string.sport_ui_type_151)
            WKSportType.KICKBOXING -> context.getString(R.string.sport_ui_type_152)
            WKSportType.RACING_CAR -> context.getString(R.string.sport_ui_type_153)
            WKSportType.INDOOR_FITNESS -> context.getString(R.string.sport_ui_type_154)
            WKSportType.OUTDOOR_FOOTBALL -> context.getString(R.string.sport_ui_type_155)
            else -> context.getString(R.string.sport_ui_type_000)
        }
    }

}