package com.augmentalis.magicui.components.argscanner

import kotlinx.serialization.Serializable

/**
 * Avanue Registry (ARG) File Format
 *
 * Describes app capabilities, intent filters, and IPC endpoints
 * for discovery by the VoiceOS ecosystem.
 *
 * @since 1.0.0
 */

/**
 * Root ARG file structure
 */
@Serializable
data class ARGFile(
    val version: String = "1.0",
    val app: AppInfo,
    val capabilities: List<Capability> = emptyList(),
    val intentFilters: List<IntentFilter> = emptyList(),
    val services: List<ServiceEndpoint> = emptyList(),
    val contentProviders: List<ContentProviderEndpoint> = emptyList()
)

/**
 * Basic app information
 */
@Serializable
data class AppInfo(
    val id: String,                    // e.g., "com.augmentalis.avanue.browser"
    val name: String,                  // e.g., "BrowserAvanue"
    val version: String,               // e.g., "1.0.0"
    val description: String,
    val packageName: String,           // Android package name
    val icon: String? = null,          // Icon resource path
    val category: AppCategory = AppCategory.UTILITY
)

@Serializable
enum class AppCategory {
    PRODUCTIVITY,
    UTILITY,
    COMMUNICATION,
    ACCESSIBILITY,
    EDUCATION,
    ENTERTAINMENT,
    SOCIAL,
    BUSINESS,
    FINANCE,
    HEALTH,
    LIFESTYLE,
    MEDIA,
    NEWS,
    SHOPPING,
    SPORTS,
    TRAVEL,
    OTHER
}

/**
 * Capability declaration
 *
 * Describes what the app can do (e.g., "browse web", "take notes", "translate text")
 */
@Serializable
data class Capability(
    val id: String,                    // e.g., "capability.browse_web"
    val name: String,                  // e.g., "Browse Web"
    val description: String,
    val type: CapabilityType,
    val voiceCommands: List<String> = emptyList(),  // e.g., ["open {url}", "browse to {url}"]
    val params: List<CapabilityParam> = emptyList(),
    val requiresPermissions: List<String> = emptyList()
)

@Serializable
enum class CapabilityType {
    ACTION,           // Performs an action
    QUERY,            // Returns data
    TRANSFORM,        // Transforms input to output
    PROVIDER,         // Provides continuous data
    HANDLER           // Handles specific events
}

@Serializable
data class CapabilityParam(
    val name: String,
    val type: ParamType,
    val required: Boolean = true,
    val defaultValue: String? = null,
    val description: String? = null
)

@Serializable
enum class ParamType {
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    URL,
    FILE_PATH,
    JSON,
    CUSTOM
}

/**
 * Android Intent filter declaration
 */
@Serializable
data class IntentFilter(
    val action: String,                // e.g., "android.intent.action.VIEW"
    val categories: List<String> = emptyList(),
    val dataSchemes: List<String> = emptyList(),  // e.g., ["http", "https"]
    val dataMimeTypes: List<String> = emptyList(),
    val priority: Int = 0
)

/**
 * IPC Service endpoint (AIDL-based)
 */
@Serializable
data class ServiceEndpoint(
    val id: String,
    val name: String,
    val description: String,
    val aidlInterface: String,         // e.g., "com.augmentalis.avanue.browser.IBrowserService"
    val methods: List<ServiceMethod> = emptyList(),
    val requiresPermission: String? = null
)

@Serializable
data class ServiceMethod(
    val name: String,
    val description: String,
    val params: List<MethodParam> = emptyList(),
    val returnType: String
)

@Serializable
data class MethodParam(
    val name: String,
    val type: String,
    val description: String? = null
)

/**
 * Content Provider endpoint
 */
@Serializable
data class ContentProviderEndpoint(
    val id: String,
    val name: String,
    val description: String,
    val authority: String,             // e.g., "com.augmentalis.avanue.notes.provider"
    val paths: List<ContentPath> = emptyList(),
    val mimeTypes: List<String> = emptyList(),
    val requiresPermission: String? = null
)

@Serializable
data class ContentPath(
    val path: String,                  // e.g., "/notes", "/notes/#"
    val type: PathType,
    val description: String
)

@Serializable
enum class PathType {
    COLLECTION,      // Returns multiple items
    ITEM,            // Returns single item
    QUERY            // Custom query endpoint
}
