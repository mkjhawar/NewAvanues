package com.augmentalis.avacode.plugins

/**
 * Plugin lifecycle state enumeration.
 *
 * Represents the current state of a plugin in the system.
 */
enum class PluginState {
    /** Awaiting installation */
    PENDING,

    /** Installation in progress */
    INSTALLING,

    /** Successfully installed */
    INSTALLED,

    /** Active and running */
    ENABLED,

    /** Installed but disabled */
    DISABLED,

    /** Update in progress */
    UPDATING,

    /** Uninstall in progress */
    UNINSTALLING,

    /** Installation/operation failed */
    FAILED
}

/**
 * Plugin source enumeration.
 *
 * Indicates where the plugin originated from.
 */
enum class PluginSource {
    /** Pre-bundled with application */
    PRE_BUNDLED,

    /** Downloaded from AppAvanue store */
    APPAVENUE_STORE,

    /** Installed from third-party source */
    THIRD_PARTY
}

/**
 * Developer verification level enumeration.
 *
 * Indicates the security verification level for the plugin developer.
 */
enum class DeveloperVerificationLevel {
    /** Manual review passed - highest trust */
    VERIFIED,

    /** Code signing with selective review */
    REGISTERED,

    /** Sandboxing only - lowest trust */
    UNVERIFIED
}

/**
 * Permission enumeration.
 *
 * Granular permission groups that plugins can request.
 */
enum class Permission {
    CAMERA,
    LOCATION,
    STORAGE_READ,
    STORAGE_WRITE,
    NETWORK,
    MICROPHONE,
    CONTACTS,
    CALENDAR,
    BLUETOOTH,
    SENSORS,
    ACCESSIBILITY_SERVICES,
    PAYMENTS
}

/**
 * Permission grant status enumeration.
 */
enum class GrantStatus {
    /** Awaiting user decision */
    PENDING,

    /** User granted permission */
    GRANTED,

    /** User denied permission */
    DENIED,

    /** Previously granted, now revoked */
    REVOKED
}

/**
 * Transaction type enumeration.
 *
 * Type of plugin operation that creates a checkpoint.
 */
enum class TransactionType {
    INSTALL,
    UPDATE,
    UNINSTALL,
    ENABLE,
    DISABLE
}

/**
 * Asset category enumeration.
 *
 * Categories of assets that plugins can provide.
 * Each category maps to a subdirectory within the plugin's assets/ folder.
 */
enum class AssetCategory {
    FONTS,
    ICONS,
    IMAGES,
    THEMES,
    CUSTOM;

    /**
     * Get subdirectory path for this category within plugin root.
     */
    fun getSubdirectoryPath(): String {
        return when (this) {
            FONTS -> "assets/fonts"
            ICONS -> "assets/icons"
            IMAGES -> "assets/images"
            THEMES -> "themes"
            CUSTOM -> "assets/custom"
        }
    }

    /**
     * Get common file extensions for this category.
     */
    fun getCommonExtensions(): List<String> {
        return when (this) {
            FONTS -> listOf("ttf", "otf", "woff", "woff2")
            ICONS -> listOf("svg", "png", "ico")
            IMAGES -> listOf("png", "jpg", "jpeg", "svg", "gif", "webp")
            THEMES -> listOf("yaml", "yml", "json")
            CUSTOM -> emptyList() // No restrictions
        }
    }

    /**
     * Validate file extension for this category.
     */
    fun isValidExtension(filename: String): Boolean {
        if (this == CUSTOM) return true
        val extension = filename.substringAfterLast(".", "").lowercase()
        return extension in getCommonExtensions()
    }
}
