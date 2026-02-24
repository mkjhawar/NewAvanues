package com.augmentalis.httpavanue.avid

import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AVID-aware HTTP responses — wraps JSON payloads with AVID metadata
 * so the VoiceOS overlay system can render voice-targetable badges.
 *
 * Branch B (Avanues internal): Uses ONLY HTTPAvanue and kotlinx types.
 * Zero imports from VoiceOSCore or any external module.
 *
 * AVID format:
 * ```json
 * {
 *   "data": { ... },
 *   "_avid": { "type": "BTN", "label": "Save", "id": "BTN:save" }
 * }
 * ```
 */

/** AVID element type codes — string constants, not imported from VoiceOSCore. */
object AvidType {
    const val BTN = "BTN"
    const val INP = "INP"
    const val SEL = "SEL"
    const val LNK = "LNK"
    const val NAV = "NAV"
    const val TAB = "TAB"
    const val MNU = "MNU"
    const val FAB = "FAB"
}

@PublishedApi internal val avidJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Create a JSON response with AVID metadata for a single item.
 */
fun HttpResponse.Companion.avidJson(
    body: String,
    avidType: String,
    avidLabel: String,
): HttpResponse {
    val avidId = "${avidType}:${avidLabel.lowercase().replace(' ', '_')}"
    val wrapped = buildJsonObject {
        put("data", avidJson.parseToJsonElement(body))
        put("_avid", buildJsonObject {
            put("type", avidType)
            put("label", avidLabel)
            put("id", avidId)
        })
    }
    return HttpResponse(
        status = HttpStatus.OK.code,
        statusMessage = HttpStatus.OK.message,
        headers = mapOf("Content-Type" to "application/json"),
        body = avidJson.encodeToString(wrapped).encodeToByteArray(),
    )
}

/**
 * Create a JSON array response with indexed AVID identifiers.
 * Each item gets a unique AVID ID based on its index.
 *
 * Output format:
 * ```json
 * {
 *   "data": [...],
 *   "_avid_list": [
 *     { "type": "BTN", "label": "Item 1", "id": "BTN:item_1", "index": 0 },
 *     { "type": "BTN", "label": "Item 2", "id": "BTN:item_2", "index": 1 }
 *   ]
 * }
 * ```
 */
inline fun <reified T> HttpResponse.Companion.avidJsonList(
    items: List<T>,
    avidType: String,
    crossinline labelOf: (T) -> String,
): HttpResponse {
    val dataJson = avidJson.encodeToString(items)
    val avidList = items.mapIndexed { index, item ->
        val label = labelOf(item)
        val avidId = "${avidType}:${label.lowercase().replace(' ', '_')}"
        buildJsonObject {
            put("type", avidType)
            put("label", label)
            put("id", avidId)
            put("index", index)
        }
    }
    val wrapped = buildJsonObject {
        put("data", avidJson.parseToJsonElement(dataJson))
        put("_avid_list", avidJson.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(JsonObject.serializer()), avidList))
    }
    return HttpResponse(
        status = HttpStatus.OK.code,
        statusMessage = HttpStatus.OK.message,
        headers = mapOf("Content-Type" to "application/json"),
        body = avidJson.encodeToString(wrapped).encodeToByteArray(),
    )
}
