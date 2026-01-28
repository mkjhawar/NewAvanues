/**
 * AvatarHandler.kt - Voice handler for Avatar/Profile image interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven avatar control for profile images and user representations
 * Features:
 * - Show/view avatar details and profile information
 * - Change/update avatar image via picker or camera
 * - Remove/clear avatar to default state
 * - Named avatar targeting (e.g., "show profile for John")
 * - Focused avatar targeting for active elements
 * - AVID-based targeting for precise element selection
 * - Voice feedback for avatar state changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Show/View avatar:
 * - "show profile" - Display profile details for focused avatar
 * - "view avatar" - View avatar image in detail
 * - "show avatar" - Show avatar details
 * - "open profile" - Open full profile view
 * - "view profile for [name]" - View specific user's profile
 *
 * Change avatar:
 * - "change avatar" - Trigger avatar change picker
 * - "update picture" - Update profile picture
 * - "change profile picture" - Change profile image
 * - "take photo" - Capture new avatar via camera
 * - "choose photo" - Select avatar from gallery
 *
 * Remove avatar:
 * - "remove avatar" - Remove current avatar image
 * - "clear picture" - Clear profile picture
 * - "delete avatar" - Delete avatar image
 * - "reset avatar" - Reset to default avatar
 *
 * ## Avatar Source Options
 *
 * Supports:
 * - Gallery selection
 * - Camera capture
 * - Default/placeholder reset
 */

package com.augmentalis.avamagic.voice.handlers.display

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Avatar/Profile image interactions.
 *
 * Provides comprehensive voice control for avatar components including:
 * - Viewing avatar details and profile information
 * - Changing avatar images via picker or camera
 * - Removing avatars to restore default state
 * - Named avatar targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for avatar operations
 */
class AvatarHandler(
    private val executor: AvatarExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "AvatarHandler"

        // Patterns for parsing commands
        private val VIEW_PROFILE_FOR_PATTERN = Regex(
            """^(?:view|show|open)\s+(?:profile|avatar)\s+(?:for|of)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val CHANGE_AVATAR_FOR_PATTERN = Regex(
            """^(?:change|update)\s+(?:avatar|picture|profile\s+picture)\s+(?:for|of)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val REMOVE_AVATAR_FOR_PATTERN = Regex(
            """^(?:remove|clear|delete|reset)\s+(?:avatar|picture|profile\s+picture)\s+(?:for|of)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Show/View avatar
        "show profile", "view avatar", "show avatar",
        "open profile", "view profile", "view profile for",
        // Change avatar
        "change avatar", "update picture", "change profile picture",
        "update avatar", "change picture",
        "take photo", "choose photo", "select photo",
        // Remove avatar
        "remove avatar", "clear picture", "delete avatar",
        "reset avatar", "remove picture", "clear avatar"
    )

    /**
     * Callback for voice feedback when avatar state changes.
     */
    var onAvatarChanged: ((userName: String, action: AvatarAction) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing avatar command: $normalizedAction")

        return try {
            when {
                // View profile for specific user
                VIEW_PROFILE_FOR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleViewProfileFor(normalizedAction, command)
                }

                // Change avatar for specific user
                CHANGE_AVATAR_FOR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleChangeAvatarFor(normalizedAction, command)
                }

                // Remove avatar for specific user
                REMOVE_AVATAR_FOR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRemoveAvatarFor(normalizedAction, command)
                }

                // Simple view/show commands
                normalizedAction in listOf(
                    "show profile", "view avatar", "show avatar",
                    "open profile", "view profile"
                ) -> {
                    handleViewAvatar(command)
                }

                // Change avatar via camera
                normalizedAction == "take photo" -> {
                    handleChangeAvatar(command, AvatarSource.CAMERA)
                }

                // Change avatar via gallery
                normalizedAction in listOf("choose photo", "select photo") -> {
                    handleChangeAvatar(command, AvatarSource.GALLERY)
                }

                // Change avatar (default picker)
                normalizedAction in listOf(
                    "change avatar", "update picture", "change profile picture",
                    "update avatar", "change picture"
                ) -> {
                    handleChangeAvatar(command, AvatarSource.PICKER)
                }

                // Remove avatar
                normalizedAction in listOf(
                    "remove avatar", "clear picture", "delete avatar",
                    "reset avatar", "remove picture", "clear avatar"
                ) -> {
                    handleRemoveAvatar(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing avatar command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "view profile for [name]" command.
     */
    private suspend fun handleViewProfileFor(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = VIEW_PROFILE_FOR_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse view command")

        val userName = matchResult.groupValues[1].trim()

        val avatarInfo = findAvatar(name = userName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Avatar for '$userName' not found",
                recoverable = true,
                suggestedAction = "Check the user name and try again"
            )

        return performViewAction(avatarInfo)
    }

    /**
     * Handle "change avatar for [name]" command.
     */
    private suspend fun handleChangeAvatarFor(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = CHANGE_AVATAR_FOR_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse change command")

        val userName = matchResult.groupValues[1].trim()

        val avatarInfo = findAvatar(name = userName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Avatar for '$userName' not found",
                recoverable = true,
                suggestedAction = "Check the user name and try again"
            )

        return performChangeAction(avatarInfo, AvatarSource.PICKER)
    }

    /**
     * Handle "remove avatar for [name]" command.
     */
    private suspend fun handleRemoveAvatarFor(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = REMOVE_AVATAR_FOR_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse remove command")

        val userName = matchResult.groupValues[1].trim()

        val avatarInfo = findAvatar(name = userName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Avatar for '$userName' not found",
                recoverable = true,
                suggestedAction = "Check the user name and try again"
            )

        return performRemoveAction(avatarInfo)
    }

    /**
     * Handle simple "view avatar" / "show profile" command.
     */
    private suspend fun handleViewAvatar(command: QuantizedCommand): HandlerResult {
        val avatarInfo = findAvatar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No avatar focused",
                recoverable = true,
                suggestedAction = "Focus on an avatar or say 'view profile for John'"
            )

        return performViewAction(avatarInfo)
    }

    /**
     * Handle "change avatar" command with specified source.
     */
    private suspend fun handleChangeAvatar(
        command: QuantizedCommand,
        source: AvatarSource
    ): HandlerResult {
        val avatarInfo = findAvatar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No avatar focused",
                recoverable = true,
                suggestedAction = "Focus on an avatar or say 'change avatar for John'"
            )

        return performChangeAction(avatarInfo, source)
    }

    /**
     * Handle "remove avatar" command.
     */
    private suspend fun handleRemoveAvatar(command: QuantizedCommand): HandlerResult {
        val avatarInfo = findAvatar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No avatar focused",
                recoverable = true,
                suggestedAction = "Focus on an avatar or say 'remove avatar for John'"
            )

        return performRemoveAction(avatarInfo)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Action Performers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Perform view action on avatar.
     */
    private suspend fun performViewAction(avatarInfo: AvatarInfo): HandlerResult {
        val result = executor.viewAvatar(avatarInfo)

        return if (result.success) {
            onAvatarChanged?.invoke(avatarInfo.userName, AvatarAction.VIEW)

            val feedback = if (avatarInfo.userName.isNotBlank()) {
                "Showing profile for ${avatarInfo.userName}"
            } else {
                "Showing profile"
            }

            Log.i(TAG, "Avatar viewed: ${avatarInfo.userName}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "userName" to avatarInfo.userName,
                    "avatarAvid" to avatarInfo.avid,
                    "action" to "view",
                    "hasCustomImage" to avatarInfo.hasCustomImage,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not view avatar",
                recoverable = true
            )
        }
    }

    /**
     * Perform change action on avatar.
     */
    private suspend fun performChangeAction(
        avatarInfo: AvatarInfo,
        source: AvatarSource
    ): HandlerResult {
        val result = executor.changeAvatar(avatarInfo, source)

        return if (result.success) {
            onAvatarChanged?.invoke(avatarInfo.userName, AvatarAction.CHANGE)

            val sourceText = when (source) {
                AvatarSource.CAMERA -> "camera"
                AvatarSource.GALLERY -> "gallery"
                AvatarSource.PICKER -> "picker"
            }

            val feedback = if (avatarInfo.userName.isNotBlank()) {
                "Opening $sourceText for ${avatarInfo.userName}'s avatar"
            } else {
                "Opening $sourceText for avatar"
            }

            Log.i(TAG, "Avatar change initiated: ${avatarInfo.userName} via $source")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "userName" to avatarInfo.userName,
                    "avatarAvid" to avatarInfo.avid,
                    "action" to "change",
                    "source" to source.name,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not change avatar",
                recoverable = true
            )
        }
    }

    /**
     * Perform remove action on avatar.
     */
    private suspend fun performRemoveAction(avatarInfo: AvatarInfo): HandlerResult {
        // Check if avatar already has no custom image
        if (!avatarInfo.hasCustomImage) {
            return HandlerResult.Success(
                message = "Avatar is already using the default image",
                data = mapOf(
                    "userName" to avatarInfo.userName,
                    "avatarAvid" to avatarInfo.avid,
                    "action" to "remove",
                    "noChange" to true
                )
            )
        }

        val result = executor.removeAvatar(avatarInfo)

        return if (result.success) {
            onAvatarChanged?.invoke(avatarInfo.userName, AvatarAction.REMOVE)

            val feedback = if (avatarInfo.userName.isNotBlank()) {
                "Avatar removed for ${avatarInfo.userName}"
            } else {
                "Avatar removed"
            }

            Log.i(TAG, "Avatar removed: ${avatarInfo.userName}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "userName" to avatarInfo.userName,
                    "avatarAvid" to avatarInfo.avid,
                    "action" to "remove",
                    "hadCustomImage" to true,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not remove avatar",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find avatar by name, AVID, or focus state.
     */
    private suspend fun findAvatar(
        name: String? = null,
        avid: String? = null
    ): AvatarInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val avatar = executor.findAvatarByAvid(avid)
            if (avatar != null) return avatar
        }

        // Priority 2: Name lookup
        if (name != null) {
            val avatar = executor.findAvatarByName(name)
            if (avatar != null) return avatar
        }

        // Priority 3: Focused avatar
        return executor.findFocusedAvatar()
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Actions that can be performed on an avatar.
 */
enum class AvatarAction {
    /** View avatar details or profile */
    VIEW,
    /** Change avatar image */
    CHANGE,
    /** Remove avatar image */
    REMOVE
}

/**
 * Source for new avatar image.
 */
enum class AvatarSource {
    /** Open system picker (gallery + camera options) */
    PICKER,
    /** Capture from camera directly */
    CAMERA,
    /** Select from gallery directly */
    GALLERY
}

/**
 * Information about an avatar component.
 *
 * @property avid AVID fingerprint for the avatar (format: AVT:{hash8})
 * @property userName Associated user name or display name
 * @property hasCustomImage Whether avatar has a custom image (vs default/placeholder)
 * @property imageUrl URL or path to the current avatar image
 * @property initials Initials to display when no image (e.g., "JD" for John Doe)
 * @property size Avatar size (small, medium, large)
 * @property shape Avatar shape (circle, rounded, square)
 * @property bounds Screen bounds for the avatar
 * @property isFocused Whether this avatar currently has focus
 * @property isEditable Whether the avatar can be changed by the user
 * @property node Platform-specific node reference
 */
data class AvatarInfo(
    val avid: String,
    val userName: String = "",
    val hasCustomImage: Boolean = false,
    val imageUrl: String? = null,
    val initials: String = "",
    val size: AvatarSize = AvatarSize.MEDIUM,
    val shape: AvatarShape = AvatarShape.CIRCLE,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val isEditable: Boolean = true,
    val node: Any? = null
) {
    /**
     * Display text when no image is available.
     */
    val displayText: String
        get() = initials.ifBlank {
            userName.take(2).uppercase()
        }

    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Avatar",
        text = userName,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = if (hasCustomImage) "Custom avatar" else "Default avatar"
    )
}

/**
 * Avatar size options.
 */
enum class AvatarSize {
    SMALL,
    MEDIUM,
    LARGE,
    XLARGE
}

/**
 * Avatar shape options.
 */
enum class AvatarShape {
    CIRCLE,
    ROUNDED,
    SQUARE
}

/**
 * Result of an avatar operation.
 */
data class AvatarOperationResult(
    val success: Boolean,
    val error: String? = null,
    val action: AvatarAction? = null,
    val newImageUrl: String? = null
) {
    companion object {
        fun success(action: AvatarAction, newImageUrl: String? = null) = AvatarOperationResult(
            success = true,
            action = action,
            newImageUrl = newImageUrl
        )

        fun error(message: String) = AvatarOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for avatar operations.
 *
 * Implementations should:
 * 1. Find avatar components by AVID, name, or focus state
 * 2. Read current avatar states and images
 * 3. Trigger avatar viewing, changing, and removal actions
 * 4. Handle various avatar implementations (ImageView, custom views)
 *
 * ## Avatar Detection Algorithm
 *
 * ```kotlin
 * fun findAvatarNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - contentDescription containing "avatar", "profile picture", "photo"
 *     // - className containing "AvatarView", "ProfileImageView"
 *     // - Views with circular/rounded shape heuristics
 * }
 * ```
 *
 * ## Avatar Change Algorithm
 *
 * ```kotlin
 * fun changeAvatar(node: AccessibilityNodeInfo, source: AvatarSource) {
 *     when (source) {
 *         PICKER -> launchImagePicker()
 *         CAMERA -> launchCamera()
 *         GALLERY -> launchGallery()
 *     }
 * }
 * ```
 */
interface AvatarExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Avatar Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find an avatar by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: AVT:{hash8})
     * @return AvatarInfo if found, null otherwise
     */
    suspend fun findAvatarByAvid(avid: String): AvatarInfo?

    /**
     * Find an avatar by user name or associated label.
     *
     * Searches for:
     * 1. Avatar with matching user name
     * 2. Avatar with contentDescription matching name
     * 3. Avatar near label text matching name
     *
     * @param name The user name to search for (case-insensitive)
     * @return AvatarInfo if found, null otherwise
     */
    suspend fun findAvatarByName(name: String): AvatarInfo?

    /**
     * Find the currently focused avatar.
     *
     * @return AvatarInfo if an avatar has focus, null otherwise
     */
    suspend fun findFocusedAvatar(): AvatarInfo?

    /**
     * Get all avatars on the current screen.
     *
     * @return List of all visible avatar components
     */
    suspend fun getAllAvatars(): List<AvatarInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Avatar Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * View avatar details or open profile.
     *
     * This may:
     * - Show enlarged avatar view
     * - Open user profile screen
     * - Display avatar options menu
     *
     * @param avatar The avatar to view
     * @return Operation result
     */
    suspend fun viewAvatar(avatar: AvatarInfo): AvatarOperationResult

    /**
     * Initiate avatar change with specified source.
     *
     * @param avatar The avatar to change
     * @param source Where to get the new image from
     * @return Operation result
     */
    suspend fun changeAvatar(avatar: AvatarInfo, source: AvatarSource): AvatarOperationResult

    /**
     * Remove the avatar image (reset to default).
     *
     * @param avatar The avatar to remove
     * @return Operation result
     */
    suspend fun removeAvatar(avatar: AvatarInfo): AvatarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Convenience Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if an avatar has a custom image.
     *
     * @param avatar The avatar to check
     * @return true if avatar has custom image, false if using default
     */
    suspend fun hasCustomImage(avatar: AvatarInfo): Boolean

    /**
     * Get the current image URL for an avatar.
     *
     * @param avatar The avatar to query
     * @return Image URL or null if no custom image
     */
    suspend fun getImageUrl(avatar: AvatarInfo): String?

    /**
     * Set a new image URL directly (for programmatic updates).
     *
     * @param avatar The avatar to update
     * @param imageUrl The new image URL
     * @return Operation result
     */
    suspend fun setImageUrl(avatar: AvatarInfo, imageUrl: String): AvatarOperationResult

    /**
     * Check if the avatar is editable by the current user.
     *
     * @param avatar The avatar to check
     * @return true if avatar can be edited, false otherwise
     */
    suspend fun isEditable(avatar: AvatarInfo): Boolean
}
