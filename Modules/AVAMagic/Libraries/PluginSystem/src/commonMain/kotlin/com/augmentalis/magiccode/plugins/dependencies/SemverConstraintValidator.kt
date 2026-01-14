package com.augmentalis.avacode.plugins.dependencies

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import io.github.z4kn4fein.semver.toVersionOrNull

/**
 * Semantic versioning constraint validator for plugin dependencies.
 *
 * Supports multiple constraint formats:
 * - Exact: "1.2.3" - Exact version match
 * - Caret: "^1.0.0" - Compatible with version 1.x.x (major version locked)
 * - Tilde: "~2.3.0" - Compatible with version 2.3.x (minor version locked)
 * - Range: ">=1.5.0 <2.0.0" - Within specified range
 * - Wildcard: "1.*.0" - Pattern match with wildcards
 *
 * Examples:
 * ```kotlin
 * val validator = SemverConstraintValidator()
 *
 * // Parse constraints
 * val constraint = validator.parseConstraint("^1.0.0")
 *
 * // Check if version satisfies constraint
 * validator.satisfies("1.2.3", "^1.0.0") // true
 * validator.satisfies("2.0.0", "^1.0.0") // false
 *
 * // Find best matching version
 * val versions = listOf("1.0.0", "1.5.0", "2.0.0")
 * validator.findBestMatch(versions, "^1.0.0") // "1.5.0"
 * ```
 */
class SemverConstraintValidator {
    companion object {
        private const val TAG = "SemverConstraintValidator"
    }

    /**
     * Represents a parsed version constraint.
     */
    sealed class VersionConstraint {
        /**
         * Exact version match.
         * Example: "1.2.3"
         */
        data class Exact(val version: Version) : VersionConstraint()

        /**
         * Caret constraint - compatible with same major version.
         * Example: "^1.0.0" matches 1.x.x but not 2.0.0
         */
        data class Caret(val version: Version) : VersionConstraint()

        /**
         * Tilde constraint - compatible with same minor version.
         * Example: "~2.3.0" matches 2.3.x but not 2.4.0
         */
        data class Tilde(val version: Version) : VersionConstraint()

        /**
         * Range constraint with lower and upper bounds.
         * Example: ">=1.5.0 <2.0.0"
         */
        data class Range(
            val lowerBound: Bound?,
            val upperBound: Bound?
        ) : VersionConstraint() {
            data class Bound(
                val version: Version,
                val inclusive: Boolean
            )
        }

        /**
         * Wildcard constraint with pattern matching.
         * Example: "1.*.0" matches 1.0.0, 1.1.0, 1.2.0, etc.
         */
        data class Wildcard(
            val major: Int?,
            val minor: Int?,
            val patch: Int?
        ) : VersionConstraint()
    }

    /**
     * Parse a version constraint string into a structured constraint object.
     *
     * @param constraint Constraint string (e.g., "^1.0.0", "~2.3.0", ">=1.5.0 <2.0.0")
     * @return Parsed VersionConstraint
     * @throws IllegalArgumentException if constraint format is invalid
     */
    fun parseConstraint(constraint: String): VersionConstraint {
        val trimmed = constraint.trim()

        return when {
            // Wildcard pattern (e.g., "1.*.0", "*.2.3")
            trimmed.contains('*') -> parseWildcardConstraint(trimmed)

            // Caret constraint (e.g., "^1.0.0")
            trimmed.startsWith('^') -> {
                val versionStr = trimmed.substring(1).trim()
                val version = versionStr.toVersionOrNull()
                    ?: throw IllegalArgumentException("Invalid caret constraint: $constraint")
                VersionConstraint.Caret(version)
            }

            // Tilde constraint (e.g., "~2.3.0")
            trimmed.startsWith('~') -> {
                val versionStr = trimmed.substring(1).trim()
                val version = versionStr.toVersionOrNull()
                    ?: throw IllegalArgumentException("Invalid tilde constraint: $constraint")
                VersionConstraint.Tilde(version)
            }

            // Range constraint (e.g., ">=1.5.0 <2.0.0")
            trimmed.contains(' ') -> parseRangeConstraint(trimmed)

            // Exact version
            else -> {
                val version = trimmed.toVersionOrNull()
                    ?: throw IllegalArgumentException("Invalid version constraint: $constraint")
                VersionConstraint.Exact(version)
            }
        }
    }

    /**
     * Check if a version satisfies a constraint.
     *
     * @param version Version string to check (e.g., "1.2.3")
     * @param constraint Constraint string (e.g., "^1.0.0")
     * @return true if version satisfies the constraint
     */
    fun satisfies(version: String, constraint: String): Boolean {
        val versionObj = version.toVersionOrNull()
            ?: throw IllegalArgumentException("Invalid version: $version")

        val constraintObj = parseConstraint(constraint)

        return satisfies(versionObj, constraintObj)
    }

    /**
     * Check if a version satisfies a parsed constraint.
     *
     * @param version Version object
     * @param constraint Parsed constraint
     * @return true if version satisfies the constraint
     */
    private fun satisfies(version: Version, constraint: VersionConstraint): Boolean {
        return when (constraint) {
            is VersionConstraint.Exact -> version == constraint.version

            is VersionConstraint.Caret -> {
                // ^1.0.0 matches 1.x.x (major version locked)
                // ^0.1.0 matches 0.1.x (minor version locked for 0.x.x)
                // ^0.0.1 matches 0.0.1 (exact match for 0.0.x)
                when {
                    constraint.version.major > 0 -> {
                        version.major == constraint.version.major &&
                            version >= constraint.version
                    }
                    constraint.version.minor > 0 -> {
                        version.major == 0 &&
                            version.minor == constraint.version.minor &&
                            version >= constraint.version
                    }
                    else -> {
                        version == constraint.version
                    }
                }
            }

            is VersionConstraint.Tilde -> {
                // ~2.3.0 matches 2.3.x (minor version locked)
                version.major == constraint.version.major &&
                    version.minor == constraint.version.minor &&
                    version >= constraint.version
            }

            is VersionConstraint.Range -> {
                val lowerSatisfied = constraint.lowerBound?.let { bound ->
                    if (bound.inclusive) version >= bound.version else version > bound.version
                } ?: true

                val upperSatisfied = constraint.upperBound?.let { bound ->
                    if (bound.inclusive) version <= bound.version else version < bound.version
                } ?: true

                lowerSatisfied && upperSatisfied
            }

            is VersionConstraint.Wildcard -> {
                val majorMatches = constraint.major?.let { it == version.major } ?: true
                val minorMatches = constraint.minor?.let { it == version.minor } ?: true
                val patchMatches = constraint.patch?.let { it == version.patch } ?: true

                majorMatches && minorMatches && patchMatches
            }
        }
    }

    /**
     * Find the best matching version from a list of available versions.
     *
     * Returns the highest version that satisfies the constraint.
     *
     * @param availableVersions List of available version strings
     * @param constraint Constraint string
     * @return Best matching version, or null if none match
     */
    fun findBestMatch(availableVersions: List<String>, constraint: String): String? {
        val constraintObj = parseConstraint(constraint)

        val matchingVersions = availableVersions
            .mapNotNull { versionStr ->
                versionStr.toVersionOrNull()?.let { version ->
                    versionStr to version
                }
            }
            .filter { (_, version) -> satisfies(version, constraintObj) }
            .sortedByDescending { (_, version) -> version }

        return matchingVersions.firstOrNull()?.first
    }

    /**
     * Parse a wildcard constraint.
     *
     * Examples:
     * - "1.*.0" -> major=1, minor=null, patch=0
     * - "*.2.3" -> major=null, minor=2, patch=3
     * - "1.*.*" -> major=1, minor=null, patch=null
     */
    private fun parseWildcardConstraint(constraint: String): VersionConstraint {
        val parts = constraint.split('.')
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid wildcard constraint: $constraint")
        }

        val major = if (parts[0] == "*") null else parts[0].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid major version in wildcard: ${parts[0]}")

        val minor = if (parts[1] == "*") null else parts[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid minor version in wildcard: ${parts[1]}")

        val patch = if (parts[2] == "*") null else parts[2].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid patch version in wildcard: ${parts[2]}")

        return VersionConstraint.Wildcard(major, minor, patch)
    }

    /**
     * Parse a range constraint.
     *
     * Examples:
     * - ">=1.5.0 <2.0.0"
     * - ">1.0.0 <=2.0.0"
     * - ">=1.0.0"
     * - "<2.0.0"
     */
    private fun parseRangeConstraint(constraint: String): VersionConstraint {
        val parts = constraint.trim().split(Regex("\\s+"))

        var lowerBound: VersionConstraint.Range.Bound? = null
        var upperBound: VersionConstraint.Range.Bound? = null

        for (part in parts) {
            when {
                part.startsWith(">=") -> {
                    val versionStr = part.substring(2).trim()
                    val version = versionStr.toVersionOrNull()
                        ?: throw IllegalArgumentException("Invalid version in range: $versionStr")
                    lowerBound = VersionConstraint.Range.Bound(version, inclusive = true)
                }

                part.startsWith('>') -> {
                    val versionStr = part.substring(1).trim()
                    val version = versionStr.toVersionOrNull()
                        ?: throw IllegalArgumentException("Invalid version in range: $versionStr")
                    lowerBound = VersionConstraint.Range.Bound(version, inclusive = false)
                }

                part.startsWith("<=") -> {
                    val versionStr = part.substring(2).trim()
                    val version = versionStr.toVersionOrNull()
                        ?: throw IllegalArgumentException("Invalid version in range: $versionStr")
                    upperBound = VersionConstraint.Range.Bound(version, inclusive = true)
                }

                part.startsWith('<') -> {
                    val versionStr = part.substring(1).trim()
                    val version = versionStr.toVersionOrNull()
                        ?: throw IllegalArgumentException("Invalid version in range: $versionStr")
                    upperBound = VersionConstraint.Range.Bound(version, inclusive = false)
                }

                else -> throw IllegalArgumentException("Invalid range operator in constraint: $part")
            }
        }

        if (lowerBound == null && upperBound == null) {
            throw IllegalArgumentException("Range constraint must have at least one bound")
        }

        return VersionConstraint.Range(lowerBound, upperBound)
    }

    /**
     * Validate that a version string is valid semver.
     *
     * @param version Version string to validate
     * @return true if valid semver format
     */
    fun isValidVersion(version: String): Boolean {
        return version.toVersionOrNull() != null
    }

    /**
     * Validate that a constraint string is valid.
     *
     * @param constraint Constraint string to validate
     * @return true if valid constraint format
     */
    fun isValidConstraint(constraint: String): Boolean {
        return try {
            parseConstraint(constraint)
            true
        } catch (e: Exception) {
            false
        }
    }
}
