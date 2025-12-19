package com.augmentalis.avanues.avamagic.components.voicerouter

import com.augmentalis.avanues.avamagic.components.argscanner.ARGRegistry
import com.augmentalis.avanues.avamagic.components.argscanner.Capability

/**
 * Fake IntentRouter for testing
 *
 * Returns a simple Map representing the intent instead of Android Intent objects
 */
class FakeIntentRouter(private val registry: ARGRegistry) {

    fun createIntent(match: VoiceCommandMatch): Any {
        return createIntent(match.app.packageName, match.capability, match.parameters)
    }

    fun createIntent(
        packageName: String,
        capability: Capability,
        parameters: Map<String, String>
    ): Any {
        // Return a simple map instead of Android Intent
        return mapOf(
            "packageName" to packageName,
            "action" to "com.augmentalis.voiceos.action.${capability.id.uppercase()}",
            "capabilityId" to capability.id,
            "parameters" to parameters
        )
    }

    fun canHandleIntent(intent: Any): Boolean {
        return intent is Map<*, *> && intent.containsKey("packageName")
    }

    fun getHandlers(intent: Any): List<String> {
        if (intent !is Map<*, *>) return emptyList()
        val packageName = intent["packageName"] as? String ?: return emptyList()
        return listOf(packageName)
    }
}
