/**
 * ExplorationProgress.kt - Exploration progress parcelable
 *
 * Parcelable data class for sending exploration progress across IPC.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 *
 * @since 2.1.0 (P2 - Exploration Sync)
 */

package com.augmentalis.jitlearning

import android.os.Parcel
import android.os.Parcelable

/**
 * Exploration progress data
 *
 * Contains progress metrics for an ongoing exploration session.
 */
data class ExplorationProgress(
    /** Number of screens explored */
    val screensExplored: Int = 0,

    /** Number of elements discovered */
    val elementsDiscovered: Int = 0,

    /** Current exploration depth */
    val currentDepth: Int = 0,

    /** Package being explored */
    val packageName: String = "",

    /** Current exploration state (idle, running, paused, completed, failed) */
    val state: String = "idle",

    /** Pause reason if paused */
    val pauseReason: String? = null,

    /** Progress percentage (0-100) */
    val progressPercent: Int = 0,

    /** Time elapsed in milliseconds */
    val elapsedMs: Long = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        screensExplored = parcel.readInt(),
        elementsDiscovered = parcel.readInt(),
        currentDepth = parcel.readInt(),
        packageName = parcel.readString() ?: "",
        state = parcel.readString() ?: "idle",
        pauseReason = parcel.readString(),
        progressPercent = parcel.readInt(),
        elapsedMs = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(screensExplored)
        parcel.writeInt(elementsDiscovered)
        parcel.writeInt(currentDepth)
        parcel.writeString(packageName)
        parcel.writeString(state)
        parcel.writeString(pauseReason)
        parcel.writeInt(progressPercent)
        parcel.writeLong(elapsedMs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExplorationProgress> {
        override fun createFromParcel(parcel: Parcel): ExplorationProgress {
            return ExplorationProgress(parcel)
        }

        override fun newArray(size: Int): Array<ExplorationProgress?> {
            return arrayOfNulls(size)
        }

        /**
         * Create an idle progress instance
         */
        fun idle(): ExplorationProgress {
            return ExplorationProgress(state = "idle")
        }

        /**
         * Create a running progress instance
         */
        fun running(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            currentDepth: Int,
            progressPercent: Int,
            elapsedMs: Long
        ): ExplorationProgress {
            return ExplorationProgress(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                currentDepth = currentDepth,
                packageName = packageName,
                state = "running",
                progressPercent = progressPercent,
                elapsedMs = elapsedMs
            )
        }

        /**
         * Create a paused progress instance
         */
        fun paused(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            pauseReason: String
        ): ExplorationProgress {
            return ExplorationProgress(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                packageName = packageName,
                state = "paused",
                pauseReason = pauseReason
            )
        }

        /**
         * Create a completed progress instance
         */
        fun completed(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int
        ): ExplorationProgress {
            return ExplorationProgress(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                packageName = packageName,
                state = "completed",
                progressPercent = 100
            )
        }
    }
}
