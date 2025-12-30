/**
 * RecognitionData.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-11
 */
package com.augmentalis.voiceoscore.voicerecognition

import android.os.Parcel
import android.os.Parcelable

/**
 * Recognition Data Parcelable
 * 
 * Parcelable data class for passing complex recognition data across process boundaries.
 * Used with AIDL interfaces for efficient data transfer.
 */
data class RecognitionData(
    val text: String,
    val confidence: Float,
    val timestamp: Long,
    val engineUsed: String,
    val isFinal: Boolean
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeFloat(confidence)
        parcel.writeLong(timestamp)
        parcel.writeString(engineUsed)
        parcel.writeByte(if (isFinal) 1 else 0)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object CREATOR : Parcelable.Creator<RecognitionData> {
        override fun createFromParcel(parcel: Parcel): RecognitionData {
            return RecognitionData(parcel)
        }
        
        override fun newArray(size: Int): Array<RecognitionData?> {
            return arrayOfNulls(size)
        }
    }
}