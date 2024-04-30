package com.topstep.wearkit.sample.model

import android.os.Parcel
import android.os.Parcelable
import com.topstep.wearkit.apis.model.core.WKDeviceType

data class DeviceInfo(
    val type: WKDeviceType,
    val address: String,
    val name: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        WKDeviceType.valueOf(parcel.readString()!!),
        parcel.readString()!!,
        parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
        parcel.writeString(address)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeviceInfo> {
        override fun createFromParcel(parcel: Parcel): DeviceInfo {
            return DeviceInfo(parcel)
        }

        override fun newArray(size: Int): Array<DeviceInfo?> {
            return arrayOfNulls(size)
        }
    }

}