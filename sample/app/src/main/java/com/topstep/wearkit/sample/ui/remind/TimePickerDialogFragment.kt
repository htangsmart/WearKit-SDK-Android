package com.topstep.wearkit.sample.ui.remind

import android.app.Dialog
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialogFragment
import com.github.kilnn.wheellayout.TwoWheelLayout
import com.github.kilnn.wheellayout.WheelIntAdapterKey
import com.github.kilnn.wheellayout.WheelIntConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.utils.AppUtils

const val PARCEL_ARGS = "parcelArgs"


class TimePickerDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args: Arguments = requireArguments().getParcelable(PARCEL_ARGS)!!

        val hourStart = if (args.start == null) {
            0
        } else {
            args.start / 60
        }
        val minuteStart = if (args.start == null) {
            0
        } else {
            args.start % 60
        }

        val hourEnd = if (args.end == null) {
            23
        } else {
            args.end / 60
        }
        val minuteEnd = if (args.end == null) {
            59
        } else {
            args.end % 60
        }

        val layout = TwoWheelLayout(requireContext())
        val linkages = SparseArray<WheelIntAdapterKey?>()
        if (hourStart == hourEnd) {
            linkages.put(hourStart, WheelIntAdapterKey(minuteStart, minuteEnd, false))
        } else {
            if (minuteStart > 0) {
                linkages.put(hourStart, WheelIntAdapterKey(minuteStart, 59, false))
            }
            if (minuteEnd < 59) {
                linkages.put(hourEnd, WheelIntAdapterKey(0, minuteEnd, false))
            }
        }

        val formatter = AppUtils.get02dWheelIntFormatter(MyApplication.instance)

        layout.setConfig(
            WheelIntConfig(hourStart, hourEnd, true, args.des1, formatter),
            WheelIntConfig(0, 59, true, args.des2, formatter),
            linkages
        )
        if (args.value != null) {
            layout.setValue(args.value / 60, args.value % 60)
        } else {
            layout.setValue(hourStart, minuteStart)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val values = layout.getValue()
                (activity as? Listener)?.onDialogTimePicker(tag, values[0] * 60 + values[1])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun onDialogTimePicker(tag: String?, selectValue: Int)
    }

    @Keep
    private class Arguments(
        val start: Int?,
        val end: Int?,
        val value: Int?,
        val title: String?,
        val des1: String?,
        val des2: String?,
    ) : Parcelable {

        constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeValue(start)
            parcel.writeValue(end)
            parcel.writeValue(value)
            parcel.writeString(title)
            parcel.writeString(des1)
            parcel.writeString(des2)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Arguments> {
            override fun createFromParcel(parcel: Parcel): Arguments {
                return Arguments(parcel)
            }

            override fun newArray(size: Int): Array<Arguments?> {
                return arrayOfNulls(size)
            }
        }

    }

    companion object {
        /**
         * @param value time minute
         */
        @Deprecated(message = "")
        fun newInstance(value: Int, title: String?): TimePickerDialogFragment {
            val fragment = TimePickerDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(
                    PARCEL_ARGS, Arguments(
                        start = null, end = null,
                        value = value, title = title,
                        des1 = null, des2 = null
                    )
                )
            }
            return fragment
        }

        fun newInstance(
            start: Int? = null,
            end: Int? = null,
            value: Int? = null,
            title: String? = null,
            des1: String? = null,
            des2: String? = null,
        ): TimePickerDialogFragment {
            val fragment = TimePickerDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(
                    PARCEL_ARGS, Arguments(
                        start = start, end = end,
                        value = value, title = title,
                        des1 = des1, des2 = des2
                    )
                )
            }
            return fragment
        }
    }

}