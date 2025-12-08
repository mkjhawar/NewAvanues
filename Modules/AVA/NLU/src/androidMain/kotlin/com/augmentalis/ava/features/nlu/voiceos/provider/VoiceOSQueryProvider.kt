package com.augmentalis.ava.features.nlu.voiceos.provider

import android.content.Context
import android.util.Log
import com.augmentalis.ava.features.actions.VoiceOSConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * VoiceOS Screen Data Provider
 *
 * Provides screen scraping data from VoiceOS via AIDL binding.
 * Replaces legacy ContentProvider approach with direct service calls.
 *
 * Data provided:
 * - App context (current foreground app)
 * - Clickable UI elements with coordinates
 * - Screen element hierarchy
 *
 * Updated: 2025-12-07 - Migrated from ContentProvider to AIDL
 */
class VoiceOSQueryProvider(private val context: Context) {

    companion object {
        private const val TAG = "VoiceOSQueryProvider"
    }

    // Use singleton VoiceOSConnection
    private val voiceOSConnection: VoiceOSConnection by lazy {
        VoiceOSConnection.getInstance(context)
    }

    /**
     * Query current app context from VoiceOS
     *
     * @return Package name of current foreground app, or null if unavailable
     */
    fun queryAppContext(): String? {
        return try {
            val screenJson = voiceOSConnection.scrapeCurrentScreen()
            if (screenJson == null) {
                Log.w(TAG, "Screen scrape returned null - VoiceOS may not be connected")
                return null
            }

            val json = JSONObject(screenJson as String)
            val packageName = json.optString("package_name", null)
                ?: json.optString("packageName", null)

            Log.d(TAG, "Current app context: $packageName")
            packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error querying app context: ${e.message}", e)
            null
        }
    }

    /**
     * Query current app context (suspend version)
     */
    suspend fun queryAppContextAsync(): String? = withContext(Dispatchers.IO) {
        // Ensure connected
        if (!voiceOSConnection.isReady()) {
            voiceOSConnection.bind()
        }
        queryAppContext()
    }

    /**
     * Query clickable elements from VoiceOS
     *
     * @return List of elements with properties (text, bounds, class, etc.)
     */
    fun queryClickableElements(): List<Map<String, String>> {
        return try {
            val screenJson = voiceOSConnection.scrapeCurrentScreen()
            if (screenJson == null) {
                Log.w(TAG, "Screen scrape returned null - VoiceOS may not be connected")
                return emptyList()
            }

            val elements = mutableListOf<Map<String, String>>()
            val json = JSONObject(screenJson as String)

            // Parse elements array
            val elementsArray = json.optJSONArray("elements")
                ?: json.optJSONArray("clickable_elements")
                ?: JSONArray()

            for (i in 0 until elementsArray.length()) {
                val elementJson = elementsArray.getJSONObject(i)
                val element = mutableMapOf<String, String>()

                // Extract all properties
                elementJson.keys().forEach { key ->
                    val value = elementJson.opt(key)
                    element[key] = when (value) {
                        is JSONObject -> value.toString()
                        is JSONArray -> value.toString()
                        null -> ""
                        else -> value.toString()
                    }
                }

                // Ensure required fields exist
                if (!element.containsKey("id")) {
                    element["id"] = "element_$i"
                }

                elements.add(element)
            }

            Log.d(TAG, "Found ${elements.size} clickable elements")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "Error querying clickable elements: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Query clickable elements (suspend version)
     */
    suspend fun queryClickableElementsAsync(): List<Map<String, String>> = withContext(Dispatchers.IO) {
        // Ensure connected
        if (!voiceOSConnection.isReady()) {
            voiceOSConnection.bind()
        }
        queryClickableElements()
    }

    /**
     * Get raw screen data JSON
     *
     * @return Full screen scrape JSON, or null if unavailable
     */
    fun getRawScreenData(): String? {
        return try {
            voiceOSConnection.scrapeCurrentScreen()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting raw screen data: ${e.message}", e)
            null
        }
    }

    /**
     * Get raw screen data (suspend version with auto-connect)
     */
    suspend fun getRawScreenDataAsync(): String? = withContext(Dispatchers.IO) {
        // Ensure connected
        if (!voiceOSConnection.isReady()) {
            voiceOSConnection.bind()
        }
        getRawScreenData()
    }

    /**
     * Query elements by selector
     *
     * @param selector CSS-like selector (e.g., "[text='OK']", ".Button", "#submit")
     * @return Matching elements
     */
    fun queryElementsBySelector(selector: String): List<Map<String, String>> {
        val allElements = queryClickableElements()

        return when {
            // ID selector: #id
            selector.startsWith("#") -> {
                val id = selector.substring(1)
                allElements.filter { it["id"] == id || it["resource-id"]?.endsWith(id) == true }
            }
            // Class selector: .ClassName
            selector.startsWith(".") -> {
                val className = selector.substring(1)
                allElements.filter { it["class"]?.contains(className, ignoreCase = true) == true }
            }
            // Attribute selector: [attr='value']
            selector.startsWith("[") && selector.endsWith("]") -> {
                val inner = selector.substring(1, selector.length - 1)
                val parts = inner.split("=", limit = 2)
                if (parts.size == 2) {
                    val attr = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("'").removeSurrounding("\"")
                    allElements.filter { it[attr]?.equals(value, ignoreCase = true) == true }
                } else {
                    emptyList()
                }
            }
            // Text contains
            else -> {
                allElements.filter {
                    it["text"]?.contains(selector, ignoreCase = true) == true ||
                    it["content-desc"]?.contains(selector, ignoreCase = true) == true
                }
            }
        }
    }

    /**
     * Check if VoiceOS connection is ready
     */
    fun isReady(): Boolean = voiceOSConnection.isReady()

    /**
     * Connect to VoiceOS (blocking)
     */
    fun connect(): Boolean = runBlocking {
        voiceOSConnection.bind()
    }

    /**
     * Connect to VoiceOS (suspend)
     */
    suspend fun connectAsync(): Boolean = voiceOSConnection.bind()

    /**
     * Disconnect from VoiceOS
     */
    fun disconnect() = voiceOSConnection.unbind()
}
