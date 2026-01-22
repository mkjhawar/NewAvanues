package com.augmentalis.avamagic.components.argscanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ARG Registry
 *
 * In-memory registry of discovered ARG files with search and discovery capabilities.
 *
 * ## Usage
 * ```kotlin
 * val registry = ARGRegistry()
 * registry.register(argFile)
 *
 * // Search capabilities
 * val results = registry.searchCapabilities("browse web")
 *
 * // Find by app ID
 * val app = registry.findByAppId("com.augmentalis.avanue.browser")
 * ```
 *
 * @since 1.0.0
 */
class ARGRegistry {

    private val _apps = MutableStateFlow<Map<String, ARGFile>>(emptyMap())
    val apps: StateFlow<Map<String, ARGFile>> = _apps.asStateFlow()

    private val _capabilityIndex = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private val _intentIndex = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    /**
     * Register an ARG file
     *
     * @param argFile ARG file to register
     */
    fun register(argFile: ARGFile) {
        val appId = argFile.app.id

        // Update apps map
        _apps.value = _apps.value + (appId to argFile)

        // Update capability index
        argFile.capabilities.forEach { capability ->
            val existing = _capabilityIndex.value[capability.id] ?: emptyList()
            _capabilityIndex.value = _capabilityIndex.value + (capability.id to (existing + appId))

            // Index by voice commands
            capability.voiceCommands.forEach { command ->
                val keywords = extractKeywords(command)
                keywords.forEach { keyword ->
                    val apps = _capabilityIndex.value[keyword] ?: emptyList()
                    _capabilityIndex.value = _capabilityIndex.value + (keyword to (apps + appId).distinct())
                }
            }
        }

        // Update intent index
        argFile.intentFilters.forEach { filter ->
            val existing = _intentIndex.value[filter.action] ?: emptyList()
            _intentIndex.value = _intentIndex.value + (filter.action to (existing + appId))
        }
    }

    /**
     * Unregister an app
     *
     * @param appId App ID to remove
     */
    fun unregister(appId: String) {
        _apps.value = _apps.value - appId

        // Rebuild indices
        rebuildIndices()
    }

    /**
     * Find ARG file by app ID
     *
     * @param appId App ID to find
     * @return ARG file or null
     */
    fun findByAppId(appId: String): ARGFile? {
        return _apps.value[appId]
    }

    /**
     * Find apps by package name
     *
     * @param packageName Android package name
     * @return List of matching ARG files
     */
    fun findByPackageName(packageName: String): List<ARGFile> {
        return _apps.value.values.filter { it.app.packageName == packageName }
    }

    /**
     * Search capabilities by query
     *
     * Supports:
     * - Capability ID matching
     * - Voice command matching
     * - Keyword matching
     *
     * @param query Search query
     * @return List of capability search results
     */
    fun searchCapabilities(query: String): List<CapabilitySearchResult> {
        val results = mutableListOf<CapabilitySearchResult>()
        val queryLower = query.lowercase()

        _apps.value.values.forEach { argFile ->
            argFile.capabilities.forEach { capability ->
                var score = 0f

                // Exact ID match
                if (capability.id.lowercase() == queryLower) {
                    score += 1.0f
                } else if (capability.id.lowercase().contains(queryLower)) {
                    score += 0.5f
                }

                // Name match
                if (capability.name.lowercase() == queryLower) {
                    score += 0.9f
                } else if (capability.name.lowercase().contains(queryLower)) {
                    score += 0.4f
                }

                // Voice command match
                capability.voiceCommands.forEach { command ->
                    if (command.lowercase().contains(queryLower)) {
                        score += 0.6f
                    }
                }

                // Description match
                if (capability.description.lowercase().contains(queryLower)) {
                    score += 0.3f
                }

                if (score > 0) {
                    results.add(CapabilitySearchResult(
                        app = argFile.app,
                        capability = capability,
                        score = score
                    ))
                }
            }
        }

        return results.sortedByDescending { it.score }
    }

    /**
     * Find apps that can handle an Android Intent
     *
     * @param action Intent action (e.g., "android.intent.action.VIEW")
     * @param dataUri Optional data URI
     * @param mimeType Optional MIME type
     * @return List of matching ARG files
     */
    fun findByIntent(
        action: String,
        dataUri: String? = null,
        mimeType: String? = null
    ): List<ARGFile> {
        val appIds = _intentIndex.value[action] ?: return emptyList()

        return appIds.mapNotNull { _apps.value[it] }.filter { argFile ->
            argFile.intentFilters.any { filter ->
                filter.action == action &&
                (dataUri == null || matchesDataUri(filter, dataUri)) &&
                (mimeType == null || matchesMimeType(filter, mimeType))
            }
        }
    }

    /**
     * Get all registered apps
     *
     * @return List of all ARG files
     */
    fun getAll(): List<ARGFile> {
        return _apps.value.values.toList()
    }

    /**
     * Get apps by category
     *
     * @param category App category
     * @return List of matching ARG files
     */
    fun getByCategory(category: AppCategory): List<ARGFile> {
        return _apps.value.values.filter { it.app.category == category }
    }

    /**
     * Get total number of registered apps
     */
    fun count(): Int = _apps.value.size

    /**
     * Clear all registered apps
     */
    fun clear() {
        _apps.value = emptyMap()
        _capabilityIndex.value = emptyMap()
        _intentIndex.value = emptyMap()
    }

    /**
     * Extract keywords from voice command
     *
     * Removes placeholders and common words
     */
    private fun extractKeywords(command: String): List<String> {
        // Remove placeholders
        val withoutPlaceholders = command.replace(Regex("\\{[^}]+\\}"), "")

        // Split into words
        val words = withoutPlaceholders.lowercase().split(Regex("\\s+"))

        // Filter out common words
        val stopWords = setOf("a", "an", "the", "to", "in", "on", "at", "for", "with", "by")
        return words.filter { it.isNotBlank() && it !in stopWords }
    }

    /**
     * Check if intent filter matches data URI
     */
    private fun matchesDataUri(filter: IntentFilter, dataUri: String): Boolean {
        if (filter.dataSchemes.isEmpty()) return true

        val scheme = dataUri.substringBefore("://", "")
        return scheme.isNotEmpty() && scheme in filter.dataSchemes
    }

    /**
     * Check if intent filter matches MIME type
     */
    private fun matchesMimeType(filter: IntentFilter, mimeType: String): Boolean {
        if (filter.dataMimeTypes.isEmpty()) return true

        return filter.dataMimeTypes.any { filterType ->
            when {
                filterType == mimeType -> true
                filterType.endsWith("/*") -> {
                    val filterPrefix = filterType.substringBefore("/*")
                    mimeType.startsWith(filterPrefix)
                }
                else -> false
            }
        }
    }

    /**
     * Rebuild all indices from scratch
     */
    private fun rebuildIndices() {
        val newCapabilityIndex = mutableMapOf<String, List<String>>()
        val newIntentIndex = mutableMapOf<String, List<String>>()

        _apps.value.forEach { (appId, argFile) ->
            argFile.capabilities.forEach { capability ->
                val existing = newCapabilityIndex[capability.id] ?: emptyList()
                newCapabilityIndex[capability.id] = existing + appId
            }

            argFile.intentFilters.forEach { filter ->
                val existing = newIntentIndex[filter.action] ?: emptyList()
                newIntentIndex[filter.action] = existing + appId
            }
        }

        _capabilityIndex.value = newCapabilityIndex
        _intentIndex.value = newIntentIndex
    }
}

/**
 * Capability search result
 */
data class CapabilitySearchResult(
    val app: AppInfo,
    val capability: Capability,
    val score: Float
)
