/**
 * PluginVersioning.kt - Enhancement 4: Action Versioning & Migration
 *
 * Supports plugin API evolution with backward compatibility.
 * Handles version migrations when plugins are updated.
 *
 * Part of Q12 Enhancement 4
 *
 * @since VOS4 Phase 4.1
 * @author VOS4 Development Team
 */

package com.augmentalis.commandmanager.plugins

import android.util.Log

/**
 * Plugin versioning and migration service
 *
 * Features:
 * - Semantic versioning (MAJOR.MINOR.PATCH)
 * - Version compatibility checking
 * - Data migration between versions
 * - Deprecation warnings
 * - Breaking change detection
 *
 * Version format: X.Y.Z
 * - X (major): Breaking changes, incompatible API changes
 * - Y (minor): New features, backward compatible
 * - Z (patch): Bug fixes, backward compatible
 */
class PluginVersioning {
    companion object {
        private const val TAG = "PluginVersioning"

        /** Current plugin API version */
        const val CURRENT_API_VERSION = 1

        /** Minimum supported API version */
        const val MIN_API_VERSION = 1
    }

    /**
     * Check if plugin version is compatible with current VOS version
     *
     * @param pluginVersion Plugin version string (e.g., "1.2.3")
     * @param vosVersion Current VOS version code
     * @param minVOSVersion Minimum VOS version required by plugin
     * @param pluginApiVersion API version from plugin manifest (default: 1)
     * @return Compatibility result
     */
    fun checkCompatibility(
        pluginVersion: String,
        vosVersion: Int,
        minVOSVersion: Int,
        pluginApiVersion: Int = 1
    ): CompatibilityResult {
        // Check VOS version requirement
        if (vosVersion < minVOSVersion) {
            return CompatibilityResult.Incompatible(
                "Plugin requires VOS version $minVOSVersion, but current version is $vosVersion"
            )
        }

        // Parse semantic version
        @Suppress("UNUSED_VARIABLE") val version = parseSemanticVersion(pluginVersion)
            ?: return CompatibilityResult.Incompatible("Invalid version format: $pluginVersion")

        // Check API version compatibility using actual plugin apiVersion
        if (pluginApiVersion > CURRENT_API_VERSION) {
            return CompatibilityResult.Incompatible(
                "Plugin requires API version $pluginApiVersion, but current API is $CURRENT_API_VERSION"
            )
        }

        if (pluginApiVersion < MIN_API_VERSION) {
            return CompatibilityResult.Deprecated(
                "Plugin uses deprecated API version $pluginApiVersion (minimum: $MIN_API_VERSION)",
                pluginApiVersion
            )
        }

        return CompatibilityResult.Compatible
    }

    /**
     * Check compatibility using PluginMetadata
     *
     * Convenience method that extracts values from metadata.
     *
     * @param metadata Plugin metadata
     * @param currentVOSVersion Current VOS version code
     * @return Compatibility result
     */
    fun checkCompatibility(
        metadata: PluginMetadata,
        currentVOSVersion: Int
    ): CompatibilityResult {
        return checkCompatibility(
            pluginVersion = metadata.version,
            vosVersion = currentVOSVersion,
            minVOSVersion = metadata.minVOSVersion,
            pluginApiVersion = metadata.apiVersion
        )
    }

    /**
     * Parse semantic version string
     *
     * @param version Version string (e.g., "1.2.3")
     * @return Parsed version or null if invalid
     */
    fun parseSemanticVersion(version: String): SemanticVersion? {
        return try {
            val parts = version.split(".")
            if (parts.size != 3) return null

            SemanticVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt()
            )
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Invalid version format: $version", e)
            null
        }
    }

    /**
     * Compare two semantic versions
     *
     * @return -1 if v1 < v2, 0 if v1 == v2, 1 if v1 > v2
     */
    fun compareVersions(v1: SemanticVersion, v2: SemanticVersion): Int {
        return when {
            v1.major != v2.major -> v1.major.compareTo(v2.major)
            v1.minor != v2.minor -> v1.minor.compareTo(v2.minor)
            else -> v1.patch.compareTo(v2.patch)
        }
    }

    /**
     * Check if upgrade from oldVersion to newVersion is breaking
     *
     * Breaking changes occur when major version increases.
     *
     * @param oldVersion Old version
     * @param newVersion New version
     * @return true if breaking change, false otherwise
     */
    fun isBreakingChange(oldVersion: SemanticVersion, newVersion: SemanticVersion): Boolean {
        return newVersion.major > oldVersion.major
    }

    /**
     * Check if upgrade introduces new features
     *
     * New features occur when minor version increases.
     *
     * @param oldVersion Old version
     * @param newVersion New version
     * @return true if new features added, false otherwise
     */
    fun hasNewFeatures(oldVersion: SemanticVersion, newVersion: SemanticVersion): Boolean {
        return newVersion.major == oldVersion.major && newVersion.minor > oldVersion.minor
    }

    /**
     * Create migration plan from old version to new version
     *
     * @param oldVersion Old version
     * @param newVersion New version
     * @return Migration plan
     */
    fun createMigrationPlan(
        oldVersion: SemanticVersion,
        newVersion: SemanticVersion
    ): MigrationPlan {
        val steps = mutableListOf<MigrationStep>()

        // Check for breaking changes
        if (isBreakingChange(oldVersion, newVersion)) {
            steps.add(MigrationStep.BreakingChange(
                "Major version change: ${oldVersion.major} → ${newVersion.major}",
                "Review plugin documentation for breaking changes"
            ))
        }

        // Check for new features
        if (hasNewFeatures(oldVersion, newVersion)) {
            steps.add(MigrationStep.NewFeatures(
                "Minor version change: ${oldVersion.minor} → ${newVersion.minor}",
                "New features available - check release notes"
            ))
        }

        // Check for bug fixes
        if (newVersion.patch > oldVersion.patch) {
            steps.add(MigrationStep.BugFixes(
                "Patch version change: ${oldVersion.patch} → ${newVersion.patch}",
                "Bug fixes included"
            ))
        }

        return MigrationPlan(
            fromVersion = oldVersion,
            toVersion = newVersion,
            steps = steps,
            isBackwardCompatible = !isBreakingChange(oldVersion, newVersion)
        )
    }
}

/**
 * Semantic version representation
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun toString(): String = "$major.$minor.$patch"

    override fun compareTo(other: SemanticVersion): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }
    }
}

/**
 * Version compatibility result
 */
sealed class CompatibilityResult {
    /** Plugin is compatible with current VOS version */
    object Compatible : CompatibilityResult()

    /**
     * Plugin is incompatible
     *
     * @property reason Why plugin is incompatible
     */
    data class Incompatible(val reason: String) : CompatibilityResult()

    /**
     * Plugin uses deprecated API but still works
     *
     * @property warning Deprecation warning message
     * @property deprecatedApiVersion API version that is deprecated
     */
    data class Deprecated(
        val warning: String,
        val deprecatedApiVersion: Int
    ) : CompatibilityResult()
}

/**
 * Migration plan for version upgrade
 */
data class MigrationPlan(
    /** Version being upgraded from */
    val fromVersion: SemanticVersion,

    /** Version being upgraded to */
    val toVersion: SemanticVersion,

    /** Migration steps required */
    val steps: List<MigrationStep>,

    /** Whether upgrade is backward compatible */
    val isBackwardCompatible: Boolean
) {
    /**
     * Check if migration requires user action
     */
    fun requiresUserAction(): Boolean =
        steps.any { it is MigrationStep.BreakingChange }
}

/**
 * Single migration step
 */
sealed class MigrationStep {
    abstract val description: String
    abstract val guidance: String

    /**
     * Breaking change that requires manual intervention
     */
    data class BreakingChange(
        override val description: String,
        override val guidance: String
    ) : MigrationStep()

    /**
     * New features added (backward compatible)
     */
    data class NewFeatures(
        override val description: String,
        override val guidance: String
    ) : MigrationStep()

    /**
     * Bug fixes (backward compatible)
     */
    data class BugFixes(
        override val description: String,
        override val guidance: String
    ) : MigrationStep()

    /**
     * Data migration required
     */
    data class DataMigration(
        override val description: String,
        override val guidance: String,
        val migrationScript: suspend () -> Unit
    ) : MigrationStep()
}

/**
 * Interface for plugins that support version migration
 *
 * Plugins can implement this interface to handle data migration
 * when upgrading from old versions.
 */
interface MigratablePlugin : ActionPlugin {
    /**
     * Plugin API version
     *
     * Used for compatibility checking.
     */
    override val apiVersion: Int

    /**
     * Migrate data from old version to new version
     *
     * Called automatically when plugin is upgraded.
     *
     * @param oldVersion Old plugin version
     * @param oldData Data from old version
     * @return Migrated data for new version
     */
    suspend fun migrate(oldVersion: String, oldData: Map<String, Any>): Map<String, Any>

    /**
     * Check if migration is needed from old version
     *
     * @param oldVersion Old version string
     * @return true if migration required, false otherwise
     */
    fun needsMigration(oldVersion: String): Boolean = oldVersion != version
}
