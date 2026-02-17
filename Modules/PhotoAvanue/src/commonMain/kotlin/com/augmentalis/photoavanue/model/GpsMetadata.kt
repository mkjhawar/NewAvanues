package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Cross-platform GPS metadata for photo/video EXIF tagging.
 *
 * Provides DMS (degrees/minutes/seconds) and rational-number formatting
 * required by EXIF GPS tags. Ported from Avenue-Redux GpsParser.
 */
@Serializable
data class GpsMetadata(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long = 0L
) {
    val latitudeRef: String get() = if (latitude >= 0) "N" else "S"
    val longitudeRef: String get() = if (longitude >= 0) "E" else "W"
    val altitudeRef: String get() = if (altitude >= 0) "0" else "1"

    /** Convert latitude to EXIF DMS rational string: "DD/1,MM/1,SSSS/1000" */
    fun latitudeDms(): String = toDms(latitude)

    /** Convert longitude to EXIF DMS rational string: "DD/1,MM/1,SSSS/1000" */
    fun longitudeDms(): String = toDms(longitude)

    /** Convert altitude to EXIF rational string: "NN/precision" */
    fun altitudeRational(precision: Long = 100): String = toRational(altitude, precision)

    companion object {
        /**
         * Convert decimal degrees to EXIF DMS rational string.
         * Format: "DD/1,MM/1,SSSS/1000" where SSSS = fractional seconds * 1000
         */
        fun toDms(value: Double): String {
            val absolute = abs(value)
            val deg = absolute.toInt()
            val minFrac = (absolute % 1) * 60.0
            val min = minFrac.toInt()
            val sec = ((minFrac % 1) * 60000.0).toInt()
            return "$deg/1,$min/1,$sec/1000"
        }

        /** Convert a double to EXIF rational string with given precision denominator. */
        fun toRational(value: Double, precision: Long): String {
            val absolute = abs(value)
            return "${(absolute * precision).toLong()}/$precision"
        }
    }
}
