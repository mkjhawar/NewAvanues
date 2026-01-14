package com.augmentalis.voiceavanue.models

/**
 * Mapping extensions between Parcelable models and internal database entities.
 * These mappers enable cross-process communication via AIDL while maintaining
 * internal database structure.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

// Note: Since the Database module uses a custom implementation (not Room),
// these mappers will need to be adapted based on actual internal entity structure.
// For now, providing placeholder implementations that can be customized.

/**
 * Convert Parcelable User to internal database representation.
 * TODO: Adapt to actual internal User entity structure
 */
fun User.toInternalEntity(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "email" to email,
        "createdAt" to createdAt,
        "lastLoginAt" to (lastLoginAt ?: 0L)
    )
}

/**
 * Convert internal database User to Parcelable.
 * TODO: Adapt to actual internal User entity structure
 */
fun Map<String, Any>.toParcelableUser(): User {
    return User(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        name = this["name"] as? String ?: "",
        email = this["email"] as? String ?: "",
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        lastLoginAt = (this["lastLoginAt"] as? Number)?.toLong()?.takeIf { it > 0 }
    )
}

/**
 * Convert Parcelable VoiceCommand to internal database representation.
 * TODO: Adapt to actual internal VoiceCommand entity structure
 */
fun VoiceCommand.toInternalEntity(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "command" to command,
        "action" to action,
        "category" to category,
        "enabled" to enabled,
        "usageCount" to usageCount
    )
}

/**
 * Convert internal database VoiceCommand to Parcelable.
 * TODO: Adapt to actual internal VoiceCommand entity structure
 */
fun Map<String, Any>.toParcelableVoiceCommand(): VoiceCommand {
    return VoiceCommand(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        command = this["command"] as? String ?: "",
        action = this["action"] as? String ?: "",
        category = this["category"] as? String ?: "",
        enabled = this["enabled"] as? Boolean ?: true,
        usageCount = (this["usageCount"] as? Number)?.toInt() ?: 0
    )
}

/**
 * Convert Parcelable AppSettings to internal database representation.
 * TODO: Adapt to actual internal Settings entity structure
 */
fun AppSettings.toInternalEntity(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "voiceEnabled" to voiceEnabled,
        "theme" to theme,
        "language" to language,
        "notificationsEnabled" to notificationsEnabled
    )
}

/**
 * Convert internal database Settings to Parcelable.
 * TODO: Adapt to actual internal Settings entity structure
 */
fun Map<String, Any>.toParcelableAppSettings(): AppSettings {
    return AppSettings(
        id = (this["id"] as? Number)?.toInt() ?: 1,
        voiceEnabled = this["voiceEnabled"] as? Boolean ?: true,
        theme = this["theme"] as? String ?: "system",
        language = this["language"] as? String ?: "en",
        notificationsEnabled = this["notificationsEnabled"] as? Boolean ?: true
    )
}

/**
 * List conversion helpers
 */
fun List<Map<String, Any>>.toParcelableUsers(): List<User> = map { it.toParcelableUser() }
fun List<Map<String, Any>>.toParcelableVoiceCommands(): List<VoiceCommand> = map { it.toParcelableVoiceCommand() }
