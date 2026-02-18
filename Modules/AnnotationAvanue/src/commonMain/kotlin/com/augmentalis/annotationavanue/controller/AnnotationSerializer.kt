package com.augmentalis.annotationavanue.controller

import com.augmentalis.annotationavanue.model.AnnotationState
import com.augmentalis.annotationavanue.model.Stroke
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serializer for annotation state and strokes.
 *
 * This is the FIX for the persistence bug: Cockpit's FrameContent.Whiteboard
 * stores strokesJson as a String. This serializer properly encodes/decodes
 * the full Stroke model including all StrokePoints, tool type, color, and width.
 *
 * The old code lost data because it tried to serialize Bezier control points
 * that didn't exist as data — we now store raw points only and compute Bezier at render time.
 */
object AnnotationSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    /** Encode list of strokes to JSON string for persistence in Cockpit strokesJson. */
    fun strokesToJson(strokes: List<Stroke>): String {
        return json.encodeToString(strokes)
    }

    /** Decode JSON string back to list of strokes. Returns empty list on parse failure. */
    fun strokesFromJson(jsonString: String): List<Stroke> {
        return try {
            json.decodeFromString<List<Stroke>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Encode full annotation state to JSON. */
    fun stateToJson(state: AnnotationState): String {
        return json.encodeToString(state)
    }

    /** Decode JSON to annotation state. Returns default state on parse failure. */
    fun stateFromJson(jsonString: String): AnnotationState {
        return try {
            json.decodeFromString<AnnotationState>(jsonString)
        } catch (_: Exception) {
            AnnotationState()
        }
    }
}
