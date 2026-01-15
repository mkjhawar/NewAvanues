package com.augmentalis.avacode.plugins

import kotlinx.serialization.Serializable

/**
 * Plugin manifest data model.
 *
 * Represents the plugin.yaml manifest file that defines plugin metadata,
 * capabilities, dependencies, permissions, and resources. The manifest is
 * the primary source of truth for plugin configuration and must be present
 * in every plugin's root directory.
 *
 * ## Manifest Structure
 * The manifest uses YAML format and contains:
 * - **Identity**: Unique ID, name, version, and author
 * - **Technical**: Entrypoint class and capabilities
 * - **Dependencies**: Required plugins and version constraints
 * - **Security**: Permissions and verification level
 * - **Resources**: Assets, themes, and custom files
 *
 * ## Validation
 * All manifests are validated against schema rules by [ManifestValidator]:
 * - ID must be reverse-domain notation (e.g., com.example.plugin)
 * - Version must be valid semver (e.g., 1.2.3)
 * - Entrypoint must be valid Kotlin class path
 * - Permissions must be from predefined set
 * - Source and verification level must be valid enum values
 *
 * ## Example YAML
 * ```yaml
 * id: com.example.hello-world
 * name: Hello World Plugin
 * version: 1.0.0
 * author: Your Name
 * description: A simple example plugin
 * entrypoint: com.example.helloworld.HelloWorldPlugin
 * capabilities:
 *   - ui_components
 *   - themes
 * dependencies:
 *   - pluginId: com.augmentalis.magicui
 *     version: ^1.0.0
 * permissions:
 *   - NETWORK
 *   - STORAGE_READ
 * permissionRationales:
 *   NETWORK: "Required to download theme updates"
 *   STORAGE_READ: "Needed to access user's custom themes"
 * source: THIRD_PARTY
 * verificationLevel: UNVERIFIED
 * assets:
 *   images:
 *     - icon.png
 *     - background.jpg
 *   themes:
 *     - dark-theme.yaml
 * homepage: https://example.com/plugin
 * license: MIT
 * ```
 *
 * @since 1.0.0
 * @see ManifestValidator
 * @see PluginLoader
 */
@Serializable
data class PluginManifest(
    /**
     * Unique plugin identifier in reverse-domain notation.
     * Example: "com.example.image-filters"
     *
     * Validation: Must match pattern ^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)+$
     */
    val id: String,

    /**
     * Human-readable plugin name.
     * Example: "Image Filters"
     *
     * Validation: 1-100 characters, no special characters except spaces, hyphens, underscores
     */
    val name: String,

    /**
     * Semantic version.
     * Example: "1.2.3" or "2.0.0-beta.1"
     *
     * Validation: Must be valid semver format
     */
    val version: String,

    /**
     * Plugin author or organization.
     * Example: "John Doe" or "Acme Corporation"
     */
    val author: String,

    /**
     * Brief description of plugin functionality.
     * Example: "Provides advanced image filtering capabilities"
     */
    val description: String? = null,

    /**
     * Main plugin class implementing the plugin interface.
     * Example: "com.example.imagefilters.ImageFiltersPlugin"
     *
     * Validation: Must be valid Kotlin class path
     */
    val entrypoint: String,

    /**
     * Plugin capabilities for discovery.
     * Examples: "nlp.sentiment", "llm.text-generation", "ui_components", "themes"
     */
    val capabilities: List<String> = emptyList(),

    /**
     * Plugin dependencies with version constraints.
     */
    val dependencies: List<PluginDependency> = emptyList(),

    /**
     * Permissions required by the plugin.
     *
     * Can be either:
     * - Simple list of permission names: ["NETWORK", "STORAGE_READ"]
     * - Map of permission to rationale (via permissionRationales field)
     */
    val permissions: List<String> = emptyList(),

    /**
     * Optional rationales explaining why each permission is needed.
     * Map of permission name to human-readable explanation.
     *
     * Example:
     * ```yaml
     * permissionRationales:
     *   NETWORK: "Required to download theme updates from the cloud"
     *   STORAGE_READ: "Needed to access user's custom color palettes"
     * ```
     */
    val permissionRationales: Map<String, String> = emptyMap(),

    /**
     * Plugin source type.
     * Values: "PRE_BUNDLED", "APPAVENUE_STORE", "THIRD_PARTY"
     */
    val source: String,

    /**
     * Developer verification level.
     * Values: "VERIFIED", "REGISTERED", "UNVERIFIED"
     */
    val verificationLevel: String,

    /**
     * Assets provided by the plugin, organized by category.
     */
    val assets: PluginAssets? = null,

    /**
     * Manifest schema version for backward compatibility.
     * Default: "1.0"
     */
    val manifestVersion: String = "1.0",

    /**
     * Plugin homepage URL (optional).
     */
    val homepage: String? = null,

    /**
     * License identifier (optional).
     * Example: "MIT", "Apache-2.0"
     */
    val license: String? = null
)

/**
 * Plugin dependency specification.
 *
 * Defines a dependency on another plugin using semantic versioning constraints.
 * Dependencies are resolved before plugin loading and can be marked as optional.
 *
 * ## Version Constraint Formats
 * - **Exact**: `"1.2.3"` - Exactly version 1.2.3
 * - **Caret**: `"^1.2.3"` - Compatible with 1.2.3 (>=1.2.3 <2.0.0)
 * - **Tilde**: `"~1.2.3"` - Approximately 1.2.3 (>=1.2.3 <1.3.0)
 * - **Range**: `">=1.5.0 <2.0.0"` - Between 1.5.0 and 2.0.0
 * - **Wildcard**: `"1.2.*"` - Any patch version of 1.2
 *
 * ## Example
 * ```yaml
 * dependencies:
 *   - pluginId: com.augmentalis.magicui
 *     version: ^1.0.0
 *   - pluginId: com.example.theme-engine
 *     version: ~2.3.0
 *     optional: true
 * ```
 *
 * @since 1.0.0
 * @see PluginManifest
 */
@Serializable
data class PluginDependency(
    /**
     * Plugin ID that this plugin depends on.
     * Must be a valid plugin ID in reverse-domain notation.
     */
    val pluginId: String,

    /**
     * Semver version constraint.
     *
     * Examples:
     * - `"^1.0.0"` - Caret range (compatible with)
     * - `"~2.3.0"` - Tilde range (approximately)
     * - `">=1.5.0 <2.0.0"` - Explicit range
     */
    val version: String,

    /**
     * If true, installation can proceed without this dependency.
     * The plugin should handle the absence of optional dependencies gracefully.
     */
    val optional: Boolean = false
)

/**
 * Plugin assets organized by category.
 *
 * Declares all static resources provided by the plugin. Assets are organized
 * by type for efficient discovery and validation. All paths are relative to
 * the plugin root directory.
 *
 * ## Asset Categories
 * - **images**: General image resources (backgrounds, logos, etc.)
 * - **fonts**: Typography resources
 * - **icons**: UI icon resources
 * - **themes**: Theme definition YAML files
 * - **custom**: Any other asset types
 *
 * ## Directory Structure
 * Assets should be organized in standard directories:
 * ```
 * plugin-root/
 *   assets/
 *     images/      (for images list)
 *     fonts/       (for fonts list)
 *     icons/       (for icons list)
 *   themes/        (for themes list)
 * ```
 *
 * ## Example
 * ```yaml
 * assets:
 *   images:
 *     - icon.png
 *     - background.jpg
 *   fonts:
 *     - custom-font.ttf
 *   icons:
 *     - check.svg
 *     - close.svg
 *   themes:
 *     - dark-theme.yaml
 *     - light-theme.yaml
 * ```
 *
 * @since 1.0.0
 * @see PluginManifest
 */
@Serializable
data class PluginAssets(
    /**
     * Image files (PNG, JPG, SVG, etc.)
     * Paths relative to assets/images/ directory.
     */
    val images: List<String> = emptyList(),

    /**
     * Font files (TTF, OTF, WOFF, etc.)
     * Paths relative to assets/fonts/ directory.
     */
    val fonts: List<String> = emptyList(),

    /**
     * Icon files (SVG, PNG, etc.)
     * Paths relative to assets/icons/ directory.
     */
    val icons: List<String> = emptyList(),

    /**
     * Theme YAML files.
     * Paths relative to themes/ directory.
     */
    val themes: List<String> = emptyList(),

    /**
     * Custom asset files of any type.
     * Full paths relative to plugin root.
     */
    val custom: List<String> = emptyList()
)
