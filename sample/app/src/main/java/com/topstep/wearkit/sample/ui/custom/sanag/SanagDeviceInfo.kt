package com.topstep.wearkit.sample.ui.custom.sanag

import android.os.Parcel
import android.os.Parcelable

data class SanagDeviceInfo(
    val address: String,
    val name: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address)
        parcel.writeString(name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SanagDeviceInfo> {
        override fun createFromParcel(parcel: Parcel): SanagDeviceInfo {
            return SanagDeviceInfo(parcel)
        }

        override fun newArray(size: Int): Array<SanagDeviceInfo?> {
            return arrayOfNulls(size)
        }
    }
}
