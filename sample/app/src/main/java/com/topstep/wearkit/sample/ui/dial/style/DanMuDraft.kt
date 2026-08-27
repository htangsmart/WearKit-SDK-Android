package com.topstep.wearkit.sample.ui.dial.style

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.topstep.wearkit.apis.ability.dial.DanMuCoord
import com.topstep.wearkit.sample.R

data class DanMuDraft(
    val text: String,
    val textColor: Int,
    val fontStyleIndex: Int = 0,
    val fontSizePx: Int = DanMuTextRenderer.DEFAULT_FONT_SIZE_PX,
    val imageX: Int,
    val imageY: Int,
    val walkSpeed: Int,
    val ltr: Boolean = false,
    val animationUri: String? = null,
    val animX: Int = 0,
    val animY: Int = 0,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        text = parcel.readString().orEmpty(),
        textColor = parcel.readInt(),
        fontStyleIndex = parcel.readInt(),
        fontSizePx = parcel.readInt(),
        imageX = parcel.readInt(),
        imageY = parcel.readInt(),
        walkSpeed = parcel.readInt(),
        ltr = parcel.readByte() != 0.toByte(),
        animationUri = parcel.readString(),
        animX = parcel.readInt(),
        animY = parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeInt(textColor)
        parcel.writeInt(fontStyleIndex)
        parcel.writeInt(fontSizePx)
        parcel.writeInt(imageX)
        parcel.writeInt(imageY)
        parcel.writeInt(walkSpeed)
        parcel.writeByte(if (ltr) 1 else 0)
        parcel.writeString(animationUri)
        parcel.writeInt(animX)
        parcel.writeInt(animY)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DanMuDraft> {
        override fun createFromParcel(parcel: Parcel): DanMuDraft = DanMuDraft(parcel)
        override fun newArray(size: Int): Array<DanMuDraft?> = arrayOfNulls(size)
    }
}

internal fun formatDanMuCoordX(context: Context, spec: Int): String {
    val offset = DanMuCoord.getOffset(spec)
    return when (DanMuCoord.getMode(spec)) {
        DanMuCoord.LEFT -> context.getString(R.string.dial_custom_style_danmu_coord_x_left, offset)
        DanMuCoord.RIGHT -> context.getString(R.string.dial_custom_style_danmu_coord_x_right, offset)
        DanMuCoord.CENTER -> context.getString(R.string.dial_custom_style_danmu_coord_center_value, offset)
        else -> offset.toString()
    }
}

internal fun formatDanMuCoordY(context: Context, spec: Int): String {
    val offset = DanMuCoord.getOffset(spec)
    return when (DanMuCoord.getMode(spec)) {
        DanMuCoord.TOP -> context.getString(R.string.dial_custom_style_danmu_coord_y_top, offset)
        DanMuCoord.BOTTOM -> context.getString(R.string.dial_custom_style_danmu_coord_y_bottom, offset)
        DanMuCoord.CENTER -> context.getString(R.string.dial_custom_style_danmu_coord_center_value, offset)
        else -> offset.toString()
    }
}
