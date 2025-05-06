package com.topstep.wearkit.sample.ui.dialog

import androidx.fragment.app.Fragment
import com.topstep.wearkit.sample.R

const val PARCEL_ARGS = "parcelArgs"

const val SBP_MIN = 50
const val SBP_DEFAULT = 125
const val SBP_MAX = 200
const val DBP_MIN = 20
const val DBP_DEFAULT = 80
const val DBP_MAX = 120
const val DIALOG_DBP = "dbp"
const val DIALOG_SBP = "sbp"
fun Fragment.showDbpDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = DBP_MIN,
        max = DBP_MAX,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_dbp),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_DBP)
}

fun Fragment.showSbpDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = SBP_MIN,
        max = SBP_MAX,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_sbp),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_SBP)
}

const val DIALOG_SBP_UPPER = "sbp_upper"
const val DIALOG_SBP_LOWER = "sbp_lower"
const val DIALOG_DBP_UPPER = "dbp_upper"
const val DIALOG_DBP_LOWER = "dbp_lower"
fun Fragment.showSbpUpperDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = 90,
        max = 180,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_blood_pressure_alarm_sbp_upper),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_SBP_UPPER)
}

fun Fragment.showSbpLowerDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = 60,
        max = 120,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_blood_pressure_alarm_sbp_lower),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_SBP_LOWER)
}

fun Fragment.showDbpUpperDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = 60,
        max = 120,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_blood_pressure_alarm_dbp_upper),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_DBP_UPPER)
}

fun Fragment.showDbpLowerDialog(value: Int) {
    SelectIntDialogFragment.newInstance(
        min = 40,
        max = 100,
        multiples = 1,
        value = value,
        title = getString(R.string.ds_blood_pressure_alarm_dbp_lower),
        des = getString(R.string.unit_mmhg)
    ).show(childFragmentManager, DIALOG_DBP_LOWER)
}