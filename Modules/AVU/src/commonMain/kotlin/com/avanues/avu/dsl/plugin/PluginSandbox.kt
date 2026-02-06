package com.avanues.avu.dsl.plugin

import com.avanues.avu.dsl.interpreter.SandboxConfig

/**
 * Trust levels for plugin sandboxing.
 * Higher trust = more relaxed resource limits.
 */
enum class PluginTrustLevel(val displayName: String) {
    SYSTEM("System"),
    VERIFIED("Verified"),
    USER("User"),
    UNTRUSTED("Untrusted")
}

/**
 * Provides appropriate [SandboxConfig] based on plugin trust level.
 *
 * Trust is determined by plugin ID prefix and author verification:
 * - SYSTEM: `com.augmentalis.*`, `com.realwear.*`
 * - VERIFIED: Known/verified authors
 * - USER: Default for user-installed plugins
 * - UNTRUSTED: Unknown sources, strictest limits
 */
object PluginSandbox {

    /**
     * Get sandbox config for a given trust level.
     */
    fun configForTrustLevel(level: PluginTrustLevel): SandboxConfig = when (level) {
        PluginTrustLevel.SYSTEM -> SandboxConfig.SYSTEM
        PluginTrustLevel.VERIFIED -> SandboxConfig.DEFAULT
        PluginTrustLevel.USER -> SandboxConfig(
            maxExecutionTimeMs = 8_000,
            maxSteps = 800,
            maxLoopIterations = 80,
            maxNestingDepth = 8,
            maxVariables = 80
        )
        PluginTrustLevel.UNTRUSTED -> SandboxConfig.STRICT
    }

    /**
     * Determine trust level from plugin manifest metadata.
     */
    fun determineTrustLevel(manifest: PluginManifest): PluginTrustLevel {
        val pluginId = manifest.pluginId
        return when {
            pluginId.startsWith("com.augmentalis.") -> PluginTrustLevel.SYSTEM
            pluginId.startsWith("com.realwear.") -> PluginTrustLevel.SYSTEM
            manifest.author != null && isVerifiedAuthor(manifest.author) -> PluginTrustLevel.VERIFIED
            else -> PluginTrustLevel.USER
        }
    }

    private val verifiedAuthors = mutableSetOf(
        "Augmentalis Engineering",
        "RealWear Inc"
    )

    private fun isVerifiedAuthor(author: String): Boolean =
        verifiedAuthors.any { it.equals(author, ignoreCase = true) }

    fun addVerifiedAuthor(author: String) {
        verifiedAuthors.add(author)
    }
}
