package com.augmentalis.argscanner.data

import androidx.room.TypeConverter
import com.augmentalis.argscanner.models.ARScanSession
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.argscanner.models.SpatialRelationship
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for complex data types
 *
 * Converts complex types to/from String for database storage:
 * - Position3D, Rotation3D, BoundingBox3D
 * - Enums
 * - Lists and Maps
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class Converters {
    private val gson = Gson()

    // Position3D converters
    @TypeConverter
    fun fromPosition3D(value: ScannedObject.Position3D?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPosition3D(value: String?): ScannedObject.Position3D? {
        return value?.let { gson.fromJson(it, ScannedObject.Position3D::class.java) }
    }

    // Rotation3D converters
    @TypeConverter
    fun fromRotation3D(value: ScannedObject.Rotation3D?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRotation3D(value: String?): ScannedObject.Rotation3D? {
        return value?.let { gson.fromJson(it, ScannedObject.Rotation3D::class.java) }
    }

    // BoundingBox3D converters
    @TypeConverter
    fun fromBoundingBox3D(value: ScannedObject.BoundingBox3D?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toBoundingBox3D(value: String?): ScannedObject.BoundingBox3D? {
        return value?.let { gson.fromJson(it, ScannedObject.BoundingBox3D::class.java) }
    }

    // List<String> converters
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // Map<String, Any> converters
    @TypeConverter
    fun fromStringAnyMap(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringAnyMap(value: String?): Map<String, Any>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyMap()
    }

    // TrackingQuality enum converters
    @TypeConverter
    fun fromTrackingQuality(value: ScannedObject.TrackingQuality?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTrackingQuality(value: String?): ScannedObject.TrackingQuality? {
        return value?.let { ScannedObject.TrackingQuality.valueOf(it) }
    }

    // RelativePosition enum converters
    @TypeConverter
    fun fromRelativePosition(value: SpatialRelationship.RelativePosition?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRelativePosition(value: String?): SpatialRelationship.RelativePosition? {
        return value?.let { SpatialRelationship.RelativePosition.valueOf(it) }
    }

    // ProximityLevel enum converters
    @TypeConverter
    fun fromProximityLevel(value: SpatialRelationship.ProximityLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toProximityLevel(value: String?): SpatialRelationship.ProximityLevel? {
        return value?.let { SpatialRelationship.ProximityLevel.valueOf(it) }
    }

    // GroupType enum converters
    @TypeConverter
    fun fromGroupType(value: SpatialRelationship.GroupType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGroupType(value: String?): SpatialRelationship.GroupType? {
        return value?.let { SpatialRelationship.GroupType.valueOf(it) }
    }

    // ARScanSession enum converters
    @TypeConverter
    fun fromEnvironment(value: ARScanSession.Environment?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEnvironment(value: String?): ARScanSession.Environment? {
        return value?.let { ARScanSession.Environment.valueOf(it) }
    }

    @TypeConverter
    fun fromRoomType(value: ARScanSession.RoomType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRoomType(value: String?): ARScanSession.RoomType? {
        return value?.let { ARScanSession.RoomType.valueOf(it) }
    }

    @TypeConverter
    fun fromTrackingSessionQuality(value: ARScanSession.TrackingSessionQuality?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTrackingSessionQuality(value: String?): ARScanSession.TrackingSessionQuality? {
        return value?.let { ARScanSession.TrackingSessionQuality.valueOf(it) }
    }

    @TypeConverter
    fun fromDSLFormat(value: ARScanSession.DSLFormat?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDSLFormat(value: String?): ARScanSession.DSLFormat? {
        return value?.let { ARScanSession.DSLFormat.valueOf(it) }
    }

    @TypeConverter
    fun fromSessionStatus(value: ARScanSession.SessionStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSessionStatus(value: String?): ARScanSession.SessionStatus? {
        return value?.let { ARScanSession.SessionStatus.valueOf(it) }
    }
}
