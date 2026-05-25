package com.autoclicker.app.model

import android.os.Parcel
import android.os.Parcelable

data class SwipePoint(
    val id: Long = System.currentTimeMillis(),
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val duration: Long = 300,       // thời gian vuốt (ms)
    val delayBefore: Long = 500,    // độ trễ trước khi vuốt (ms)
    val label: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeFloat(startX)
        parcel.writeFloat(startY)
        parcel.writeFloat(endX)
        parcel.writeFloat(endY)
        parcel.writeLong(duration)
        parcel.writeLong(delayBefore)
        parcel.writeString(label)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<SwipePoint> {
        override fun createFromParcel(parcel: Parcel) = SwipePoint(parcel)
        override fun newArray(size: Int) = arrayOfNulls<SwipePoint>(size)
    }
}
