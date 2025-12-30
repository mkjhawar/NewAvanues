package com.augmentalis.actions.handlers

import android.content.Context
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.VoiceOSConnection
import com.augmentalis.actions.handlers.VoiceOSRoutingHandler

/**
 * Expanded VoiceOS Handlers (Phase 3 Expansion)
 * 
 * These handlers cover the new intents added in expanded-voiceos.anl (now VoiceOS*.anl).
 * They route commands to the new VoiceOS categories:
 * - Clipboard (copy/paste/cut/select_all)
 * - App Interaction (app_search)
 * - Vision (screen_describe, screen_find)
 * - Media Casting (media_cast)
 * 
 * enhanced: Implements parameter extraction from utterance.
 */

// ========================================
// Clipboard Handlers (No params needed)
// ========================================

class CopyActionHandler : VoiceOSRoutingHandler("copy", "clipboard")
class PasteActionHandler : VoiceOSRoutingHandler("paste", "clipboard")
class CutActionHandler : VoiceOSRoutingHandler("cut", "clipboard")
class SelectAllActionHandler : VoiceOSRoutingHandler("select_all", "clipboard")

// ========================================
// App Interaction Handlers
// ========================================

class AppSearchActionHandler : VoiceOSRoutingHandler("app_search", "app_interaction") {
    override suspend fun execute(context: Context, utterance: String): ActionResult {
        // Extract query: "search for pizza" -> "pizza"
        // "find tacos in yelp" -> query="tacos", app="yelp" (simplified here to full query)
        val query = utterance
            .replace(Regex("^(search for|find|lookup|search|look for)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        
        Log.d("AppSearch", "Extracted query: '$query' from '$utterance'")
        
        val params = mapOf("query" to query)
        
        return executeWithParams(context, params)
    }
    
    private suspend fun executeWithParams(context: Context, params: Map<String, String>): ActionResult {
        return try {
            val voiceOS = VoiceOSConnection.getInstance(context)
            // Note: intent property is "app_search" from constructor
            val result = voiceOS.executeCommand(intent, "app_interaction", params)
            
            when (result) {
                is VoiceOSConnection.CommandResult.Success -> ActionResult.Success(result.message)
                is VoiceOSConnection.CommandResult.Failure -> ActionResult.Failure(result.error)
            }
        } catch (e: Exception) {
            ActionResult.Failure("Failed to execute $intent: ${e.message}")
        }
    }
}

// ========================================
// Vision / Meta-Cognitive Handlers
// ========================================

class ScreenDescribeActionHandler : VoiceOSRoutingHandler("screen_describe", "vision")
// Future enhancement: Request screenshot URI from VoiceOS and process with LocalLLMProvider

class ScreenFindActionHandler : VoiceOSRoutingHandler("screen_find", "vision") {
    override suspend fun execute(context: Context, utterance: String): ActionResult {
        // Extract element: "Find the send button" -> "send button"
        val element = utterance
            .replace(Regex("^(find|locate|where is|show me|search for)\\s+(the\\s+)?", RegexOption.IGNORE_CASE), "")
            .trim()
            
        val params = mapOf("element_description" to element)
        return executeWithParams(context, params)
    }
    
    private suspend fun executeWithParams(context: Context, params: Map<String, String>): ActionResult {
         return try {
            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, "vision", params)
            when (result) {
                is VoiceOSConnection.CommandResult.Success -> ActionResult.Success(result.message)
                is VoiceOSConnection.CommandResult.Failure -> ActionResult.Failure(result.error)
            }
        } catch (e: Exception) {
            ActionResult.Failure("Failed to execute $intent: ${e.message}")
        }
    }
}

// ========================================
// Media Casting Handlers
// ========================================

class MediaCastActionHandler : VoiceOSRoutingHandler("media_cast", "media_casting") {
     override suspend fun execute(context: Context, utterance: String): ActionResult {
        // Extract target: "Cast to Living Room TV" -> "Living Room TV"
        val target = utterance
            .replace(Regex("^(cast to|connect to|stream to)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        
        val params = if (target.isNotEmpty() && target != utterance) {
            mapOf("device_name" to target)
        } else {
            emptyMap()
        }
        
        return executeWithParams(context, params)
    }
    
    private suspend fun executeWithParams(context: Context, params: Map<String, String>): ActionResult {
         return try {
            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, "media_casting", params)
            when (result) {
                is VoiceOSConnection.CommandResult.Success -> ActionResult.Success(result.message)
                is VoiceOSConnection.CommandResult.Failure -> ActionResult.Failure(result.error)
            }
        } catch (e: Exception) {
            ActionResult.Failure("Failed to execute $intent: ${e.message}")
        }
    }
}
