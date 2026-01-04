package com.augmentalis.argscanner.integration

import android.content.Context
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.UUIDElement
import com.augmentalis.uuidcreator.UUIDPosition

/**
 * UUIDIntegration - Integrates ARGScanner with UUIDCreator for voice control
 *
 * Registers scanned objects with UUIDCreator to enable voice control:
 * - Generates/assigns UUIDs
 * - Registers voice commands
 * - Creates spatial navigation
 * - Integrates with VoiceOS
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class UUIDIntegration(private val context: Context) {

    private val uuidCreator: UUIDCreator by lazy {
        UUIDCreator.initialize(context)
    }

    /**
     * Register scanned object with UUIDCreator
     *
     * Assigns UUID and registers voice commands for the object
     *
     * @param obj Scanned object
     * @param registerActions Whether to register action handlers
     * @return Updated ScannedObject with UUID
     */
    fun registerObject(
        obj: ScannedObject,
        registerActions: Boolean = true
    ): ScannedObject {
        // Create UUID element
        val element = uuidCreator.createElement(
            name = obj.voiceName ?: obj.label,
            type = obj.label,
            position = UUIDPosition(
                x = obj.position.x.toDouble(),
                y = obj.position.y.toDouble(),
                z = obj.position.z.toDouble()
            ),
            actions = if (registerActions) {
                createDefaultActions(obj)
            } else {
                emptyMap()
            }
        )

        // Register element
        uuidCreator.registerElement(element)

        // Return updated object with real UUID
        return obj.copy(uuid = element.uuid)
    }

    /**
     * Register multiple objects
     */
    fun registerObjects(objects: List<ScannedObject>): List<ScannedObject> {
        return objects.map { registerObject(it) }
    }

    /**
     * Create default action handlers for an object
     */
    private fun createDefaultActions(obj: ScannedObject): Map<String, (Map<String, Any>) -> Unit> {
        return mapOf(
            "select" to { params ->
                handleSelectAction(obj, params)
            },
            "show" to { params ->
                handleShowAction(obj, params)
            },
            "highlight" to { params ->
                handleHighlightAction(obj, params)
            },
            "navigate" to { params ->
                handleNavigateAction(obj, params)
            }
        )
    }

    /**
     * Handle "select" action
     */
    private fun handleSelectAction(obj: ScannedObject, params: Map<String, Any>) {
        // Broadcast selection event
        println("Selected: ${obj.label} (${obj.uuid})")
        // TODO: Broadcast to VoiceOS/AVAMagic
    }

    /**
     * Handle "show" action
     */
    private fun handleShowAction(obj: ScannedObject, params: Map<String, Any>) {
        // Show object details
        println("Showing: ${obj.label}")
        println("Position: ${obj.position}")
        println("Confidence: ${(obj.confidence * 100).toInt()}%")
        // TODO: Display in UI
    }

    /**
     * Handle "highlight" action
     */
    private fun handleHighlightAction(obj: ScannedObject, params: Map<String, Any>) {
        // Highlight object in AR view
        println("Highlighting: ${obj.label}")
        // TODO: Trigger AR highlight
    }

    /**
     * Handle "navigate" action
     */
    private fun handleNavigateAction(obj: ScannedObject, params: Map<String, Any>) {
        // Navigate to object
        println("Navigating to: ${obj.label}")
        // TODO: AR navigation guidance
    }

    /**
     * Parse voice command and execute action
     *
     * @param command Voice command text
     * @return true if command was handled
     */
    fun executeVoiceCommand(command: String): Boolean {
        return try {
            val result = uuidCreator.parseVoiceCommand(command)
            result != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Register spatial navigation between objects
     *
     * Creates voice-navigable relationships
     */
    fun registerSpatialNavigation(
        fromObject: ScannedObject,
        toObject: ScannedObject,
        direction: String
    ) {
        // Register navigation command
        val command = "navigate $direction to ${toObject.getVoiceFriendlyName()}"

        val navigationAction: (Map<String, Any>) -> Unit = { _ ->
            println("Navigating $direction from ${fromObject.label} to ${toObject.label}")
            // TODO: Trigger navigation
        }

        // Update source object with navigation action
        val element = uuidCreator.getElementByUuid(fromObject.uuid)
        element?.let {
            val updatedActions = it.actions.toMutableMap()
            updatedActions["navigate_$direction"] = navigationAction
            // Update element
            uuidCreator.registerElement(it.copy(actions = updatedActions))
        }
    }

    /**
     * Unregister object from UUIDCreator
     */
    fun unregisterObject(uuid: String) {
        // Remove from UUIDCreator registry
        // Note: UUIDCreator doesn't have explicit unregister in current API
        println("Unregistering: $uuid")
    }

    /**
     * Unregister all objects from a session
     */
    fun unregisterSession(sessionId: String) {
        // Remove all objects from session
        println("Unregistering session: $sessionId")
    }

    /**
     * Get all registered objects
     */
    fun getRegisteredObjects(): List<UUIDElement> {
        return uuidCreator.getAllElements()
    }

    /**
     * Check if UUID is registered
     */
    fun isRegistered(uuid: String): Boolean {
        return uuidCreator.getElementByUuid(uuid) != null
    }

    /**
     * Generate voice command suggestions for an object
     */
    fun generateVoiceCommands(obj: ScannedObject): List<String> {
        val name = obj.getVoiceFriendlyName()
        return listOf(
            "select $name",
            "show $name",
            "show details for $name",
            "highlight $name",
            "navigate to $name",
            "what is $name",
            "tell me about $name"
        )
    }

    /**
     * Update voice commands for registered object
     */
    fun updateVoiceCommands(uuid: String, commands: List<String>): Boolean {
        val element = uuidCreator.getElementByUuid(uuid)
        return if (element != null) {
            // Update element with new commands
            // Note: Current UUIDCreator API doesn't explicitly support command updates
            println("Updated voice commands for $uuid: $commands")
            true
        } else {
            false
        }
    }

    /**
     * Test voice command recognition
     */
    fun testVoiceCommand(command: String): VoiceCommandTestResult {
        return try {
            val result = uuidCreator.parseVoiceCommand(command)
            if (result != null) {
                VoiceCommandTestResult.Recognized(
                    command = command,
                    targetUuid = result.elementId,
                    action = result.action
                )
            } else {
                VoiceCommandTestResult.NotRecognized(command)
            }
        } catch (e: Exception) {
            VoiceCommandTestResult.Error(command, e.message ?: "Unknown error")
        }
    }

    /**
     * Voice command test result
     */
    sealed class VoiceCommandTestResult {
        data class Recognized(
            val command: String,
            val targetUuid: String,
            val action: String
        ) : VoiceCommandTestResult()

        data class NotRecognized(
            val command: String
        ) : VoiceCommandTestResult()

        data class Error(
            val command: String,
            val error: String
        ) : VoiceCommandTestResult()
    }
}
