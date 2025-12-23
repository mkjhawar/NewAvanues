/**
 * AIContextSerializer.kt - AI context serialization utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Serialization utilities for AI context data.
 */

package com.augmentalis.voiceoscore.learnapp.ai

import android.content.Context
import android.os.Environment
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.navigation.GraphElement
import com.augmentalis.voiceoscore.learnapp.navigation.GraphTransition
import com.augmentalis.voiceoscore.learnapp.navigation.NavigationGraph
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * AI Context - Represents the AI-readable context of an app
 *
 * @property packageName App package name
 * @property screens List of screen contexts
 * @property stats Overall statistics
 */
data class AIContext(
    val packageName: String,
    val screens: List<ScreenContext>,
    val stats: AIContextStats
)

/**
 * Screen Context for AI processing
 */
data class ScreenContext(
    val screenHash: String,
    val activityName: String?,
    val elements: List<ElementContext>,
    val transitions: List<TransitionContext>
)

/**
 * Element Context for AI processing
 */
data class ElementContext(
    val uuid: String,
    val alias: String?,
    val type: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean
)

/**
 * Transition Context for AI processing
 */
data class TransitionContext(
    val fromScreen: String,
    val toScreen: String,
    val triggerElement: String
)

/**
 * AI Context Statistics
 */
data class AIContextStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalTransitions: Int
)

/**
 * AI Context Serializer
 *
 * Generates AI-readable context from exploration results.
 *
 * @param context Android context
 * @param databaseManager Database manager for element lookup
 */
class AIContextSerializer(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager
) {
    /**
     * Generate AI context from navigation graph
     *
     * @param graph Navigation graph
     * @return AI context
     */
    fun generateContext(graph: NavigationGraph): AIContext {
        val screens = graph.getScreens().map { screenHash ->
            val elements = graph.getElementsForScreen(screenHash).map { element ->
                ElementContext(
                    uuid = element.uuid ?: "",
                    alias = element.alias,
                    type = element.type,
                    text = element.text,
                    contentDescription = element.contentDescription,
                    isClickable = element.isClickable
                )
            }

            val transitions = graph.getTransitionsFrom(screenHash).map { transition ->
                TransitionContext(
                    fromScreen = transition.fromScreen,
                    toScreen = transition.toScreen,
                    triggerElement = transition.elementUuid
                )
            }

            ScreenContext(
                screenHash = screenHash,
                activityName = graph.getActivityName(screenHash),
                elements = elements,
                transitions = transitions
            )
        }

        return AIContext(
            packageName = graph.packageName,
            screens = screens,
            stats = AIContextStats(
                totalScreens = screens.size,
                totalElements = screens.sumOf { it.elements.size },
                totalTransitions = screens.sumOf { it.transitions.size }
            )
        )
    }

    /**
     * Convert AI context to JSON string
     *
     * @param aiContext AI context
     * @return JSON string
     */
    fun toJSON(aiContext: AIContext): String {
        val json = JSONObject()
        json.put("packageName", aiContext.packageName)
        json.put("stats", JSONObject().apply {
            put("totalScreens", aiContext.stats.totalScreens)
            put("totalElements", aiContext.stats.totalElements)
            put("totalTransitions", aiContext.stats.totalTransitions)
        })

        val screensArray = JSONArray()
        aiContext.screens.forEach { screen ->
            val screenJson = JSONObject()
            screenJson.put("screenHash", screen.screenHash)
            screenJson.put("activityName", screen.activityName)

            val elementsArray = JSONArray()
            screen.elements.forEach { element ->
                elementsArray.put(JSONObject().apply {
                    put("uuid", element.uuid)
                    put("alias", element.alias)
                    put("type", element.type)
                    put("text", element.text)
                    put("contentDescription", element.contentDescription)
                    put("isClickable", element.isClickable)
                })
            }
            screenJson.put("elements", elementsArray)

            val transitionsArray = JSONArray()
            screen.transitions.forEach { transition ->
                transitionsArray.put(JSONObject().apply {
                    put("fromScreen", transition.fromScreen)
                    put("toScreen", transition.toScreen)
                    put("triggerElement", transition.triggerElement)
                })
            }
            screenJson.put("transitions", transitionsArray)

            screensArray.put(screenJson)
        }
        json.put("screens", screensArray)

        return json.toString(2)
    }

    /**
     * Save AI context to .vos file
     *
     * @param aiContext AI context
     * @return File or null if failed
     */
    fun saveToFile(aiContext: AIContext): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "${aiContext.packageName.replace('.', '_')}_${System.currentTimeMillis()}.vos"
            val file = File(downloadsDir, fileName)
            file.writeText(toJSON(aiContext))
            file
        } catch (e: Exception) {
            android.util.Log.e("AIContextSerializer", "Failed to save context", e)
            null
        }
    }

    /**
     * Generate LLM prompt from context
     *
     * @param aiContext AI context
     * @param goal User goal
     * @return LLM prompt string
     */
    fun toLLMPrompt(aiContext: AIContext, goal: String): String {
        return buildString {
            appendLine("# App Navigation Context")
            appendLine("Package: ${aiContext.packageName}")
            appendLine("Screens: ${aiContext.stats.totalScreens}")
            appendLine("Elements: ${aiContext.stats.totalElements}")
            appendLine()
            appendLine("## User Goal")
            appendLine(goal)
            appendLine()
            appendLine("## Available Screens")
            aiContext.screens.forEach { screen ->
                appendLine("### ${screen.activityName ?: screen.screenHash}")
                screen.elements.filter { it.isClickable }.take(10).forEach { element ->
                    appendLine("- ${element.alias ?: element.text ?: element.uuid} (${element.type})")
                }
            }
        }
    }

    companion object {
        /**
         * Serialize context map to JSON string (static utility)
         */
        fun serialize(context: Map<String, Any?>): String {
            val json = JSONObject()
            context.forEach { (key, value) ->
                when (value) {
                    null -> json.put(key, JSONObject.NULL)
                    is Number -> json.put(key, value)
                    is Boolean -> json.put(key, value)
                    is String -> json.put(key, value)
                    is Map<*, *> -> json.put(key, JSONObject(value as Map<String, Any?>))
                    is List<*> -> json.put(key, value)
                    else -> json.put(key, value.toString())
                }
            }
            return json.toString()
        }

        /**
         * Deserialize JSON string to context map (static utility)
         */
        fun deserialize(jsonString: String): Map<String, Any?> {
            val result = mutableMapOf<String, Any?>()
            try {
                val json = JSONObject(jsonString)
                json.keys().forEach { key ->
                    result[key] = json.opt(key)
                }
            } catch (e: Exception) {
                android.util.Log.e("AIContextSerializer", "Failed to deserialize JSON context", e)
                // Return empty map on parse error
            }
            return result
        }
    }
}
