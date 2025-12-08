/**
 * ElementStatusReason.kt - Element status tracking and diagnostic reasons
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/models/ElementStatusReason.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (Swarm Agent 1)
 * Created: 2025-12-08
 *
 * Comprehensive status and reason tracking for all elements during exploration.
 * Enables detailed diagnostics: why clicked, why blocked, why skipped.
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Element Status
 *
 * High-level status of an element during exploration.
 */
enum class ElementStatus {
    /** Element has been successfully clicked */
    CLICKED,

    /** Element was discovered but not clicked (optimization or constraint) */
    NOT_CLICKED,

    /** Element was blocked due to dangerous pattern (critical safety) */
    BLOCKED,

    /** Element discovered but not yet processed */
    PENDING
}

/**
 * Element Status Reason
 *
 * Detailed reason codes explaining WHY an element has its current status.
 * Used for diagnostics, debugging, and user-facing reports.
 */
enum class ElementStatusReason(
    val displayName: String,
    val description: String,
    val category: ReasonCategory
) {
    // ═══════════════════════════════════════════════════════════════
    // SUCCESS REASONS
    // ═══════════════════════════════════════════════════════════════

    CLICKED_SUCCESSFULLY(
        "Clicked Successfully",
        "Element was clicked and interaction completed",
        ReasonCategory.SUCCESS
    ),

    // ═══════════════════════════════════════════════════════════════
    // NOT CLICKED - OPTIMIZATION REASONS
    // ═══════════════════════════════════════════════════════════════

    NOT_YET_REACHED(
        "Not Yet Reached",
        "Element discovered but not yet attempted (pending in queue)",
        ReasonCategory.OPTIMIZATION
    ),

    CLICK_CAP_REACHED(
        "Click Cap Reached",
        "Maximum clicks per screen limit reached - prevents over-clicking",
        ReasonCategory.OPTIMIZATION
    ),

    NAVIGATION_LOOP_DETECTED(
        "Navigation Loop Detected",
        "Element leads to already-visited screen (prevents infinite loops)",
        ReasonCategory.OPTIMIZATION
    ),

    DESTINATION_VISITED(
        "Destination Already Explored",
        "Element destination screen already fully explored",
        ReasonCategory.OPTIMIZATION
    ),

    EXPLORATION_TIMEOUT(
        "Exploration Timeout",
        "Exploration time limit reached before processing element",
        ReasonCategory.CONSTRAINT
    ),

    EXPLORATION_DEPTH_LIMIT(
        "Depth Limit Reached",
        "Maximum exploration depth reached (prevents excessive nesting)",
        ReasonCategory.CONSTRAINT
    ),

    // ═══════════════════════════════════════════════════════════════
    // BLOCKED - CRITICAL DANGEROUS REASONS
    // ═══════════════════════════════════════════════════════════════

    BLOCKED_CALL_ACTION(
        "Blocked: Call Action",
        "Would initiate phone call (audio/video/dial) - CRITICAL safety block",
        ReasonCategory.BLOCKED_CRITICAL
    ),

    BLOCKED_SEND_ACTION(
        "Blocked: Send Action",
        "Would send message/email/post - prevents unintended communication",
        ReasonCategory.BLOCKED_HIGH
    ),

    BLOCKED_DELETE_ACTION(
        "Blocked: Delete Action",
        "Would delete data/account - prevents destructive action",
        ReasonCategory.BLOCKED_CRITICAL
    ),

    BLOCKED_PAYMENT_ACTION(
        "Blocked: Payment Action",
        "Would initiate payment/purchase/checkout - prevents financial action",
        ReasonCategory.BLOCKED_CRITICAL
    ),

    BLOCKED_POWER_ACTION(
        "Blocked: Power Action",
        "Would power off/restart/shutdown device - CRITICAL system action",
        ReasonCategory.BLOCKED_CRITICAL
    ),

    BLOCKED_LOGOUT_ACTION(
        "Blocked: Logout Action",
        "Would sign out/logout - prevents session termination",
        ReasonCategory.BLOCKED_HIGH
    ),

    BLOCKED_DOWNLOAD_ACTION(
        "Blocked: Download Action",
        "Would initiate download - generates UUID but click skipped",
        ReasonCategory.BLOCKED_MEDIUM
    ),

    BLOCKED_ADMIN_ACTION(
        "Blocked: Admin Action",
        "Would change roles/permissions/membership - prevents privilege changes",
        ReasonCategory.BLOCKED_HIGH
    ),

    BLOCKED_MICROPHONE_ACTION(
        "Blocked: Microphone Action",
        "Would change microphone settings - CRITICAL audio setting",
        ReasonCategory.BLOCKED_CRITICAL
    ),

    BLOCKED_CUSTOM(
        "Blocked: Custom Pattern",
        "Matches custom dangerous pattern - see details",
        ReasonCategory.BLOCKED_MEDIUM
    ),

    // ═══════════════════════════════════════════════════════════════
    // APP-LEVEL RECOGNITION ISSUES
    // ═══════════════════════════════════════════════════════════════

    APP_NO_ELEMENTS_FOUND(
        "No Elements Found",
        "App has no clickable elements - may be loading or empty state",
        ReasonCategory.APP_ISSUE
    ),

    APP_PERMISSION_DENIED(
        "Permission Denied",
        "Accessibility permission blocked - cannot inspect app UI",
        ReasonCategory.APP_ISSUE
    ),

    APP_DYNAMIC_ONLY(
        "All Dynamic Content",
        "App contains only dynamic lists/feeds - no stable elements to click",
        ReasonCategory.APP_ISSUE
    ),

    APP_WEBVIEW_ONLY(
        "WebView Only",
        "App is WebView wrapper - web content not inspectable",
        ReasonCategory.APP_ISSUE
    ),

    APP_CRASHED(
        "App Crashed",
        "App crashed during exploration - learning incomplete",
        ReasonCategory.APP_ISSUE
    ),

    APP_AUTH_WALL(
        "Authentication Required",
        "App stuck at login screen - cannot proceed without credentials",
        ReasonCategory.APP_ISSUE
    );

    /**
     * Get color for visual overlay (Android color int)
     */
    fun getOverlayColor(): Int {
        return when (category) {
            ReasonCategory.SUCCESS -> 0xFF4CAF50.toInt()  // Green
            ReasonCategory.OPTIMIZATION -> 0xFFFF9800.toInt()  // Orange
            ReasonCategory.CONSTRAINT -> 0xFFFF9800.toInt()  // Orange
            ReasonCategory.BLOCKED_CRITICAL -> 0xFFD32F2F.toInt()  // Dark Red
            ReasonCategory.BLOCKED_HIGH -> 0xFFE91E63.toInt()  // Pink
            ReasonCategory.BLOCKED_MEDIUM -> 0xFFFFC107.toInt()  // Amber
            ReasonCategory.APP_ISSUE -> 0xFF9E9E9E.toInt()  // Gray
        }
    }

    /**
     * Get status icon emoji
     */
    fun getIcon(): String {
        return when (category) {
            ReasonCategory.SUCCESS -> "✅"
            ReasonCategory.OPTIMIZATION -> "⏭️"
            ReasonCategory.CONSTRAINT -> "⏸️"
            ReasonCategory.BLOCKED_CRITICAL -> "🚫"
            ReasonCategory.BLOCKED_HIGH -> "⚠️"
            ReasonCategory.BLOCKED_MEDIUM -> "⚡"
            ReasonCategory.APP_ISSUE -> "❌"
        }
    }
}

/**
 * Reason Category
 *
 * High-level grouping of reason codes for filtering and display.
 */
enum class ReasonCategory {
    /** Successfully clicked */
    SUCCESS,

    /** Not clicked due to optimization strategy */
    OPTIMIZATION,

    /** Not clicked due to external constraint (time, depth) */
    CONSTRAINT,

    /** Blocked - Critical danger (call, power, delete, payment) */
    BLOCKED_CRITICAL,

    /** Blocked - High danger (send, logout, admin) */
    BLOCKED_HIGH,

    /** Blocked - Medium danger (download, custom) */
    BLOCKED_MEDIUM,

    /** App-level recognition issue */
    APP_ISSUE
}

/**
 * Dangerous Category
 *
 * Severity classification for blocked elements.
 */
enum class DangerousCategory {
    /** Critical - Call, power off, delete account, payment */
    CRITICAL,

    /** High - Send message, logout, admin actions */
    HIGH,

    /** Medium - Download, share */
    MEDIUM
}

/**
 * Element Diagnostic Record
 *
 * Complete diagnostic information for a single element.
 * Tracks why element has its current status and provides all context
 * needed for debugging, reports, and user-facing explanations.
 */
data class ElementDiagnostic(
    // ═══════════════════════════════════════════════════════════════
    // IDENTITY
    // ═══════════════════════════════════════════════════════════════

    /** Element UUID (VUID) */
    val elementUuid: String,

    /** Screen hash where element was found */
    val screenHash: String,

    /** App package name */
    val appId: String,

    /** Exploration session ID */
    val sessionId: String,

    // ═══════════════════════════════════════════════════════════════
    // STATUS & REASON
    // ═══════════════════════════════════════════════════════════════

    /** Current status */
    val status: ElementStatus,

    /** Reason code */
    val reason: ElementStatusReason,

    /** Human-readable reason detail (context-specific) */
    val reasonDetail: String = "",

    // ═══════════════════════════════════════════════════════════════
    // DANGEROUS ELEMENT INFO (if blocked)
    // ═══════════════════════════════════════════════════════════════

    /** Regex pattern that matched (if dangerous) */
    val dangerousPattern: String? = null,

    /** Dangerous category (if blocked) */
    val dangerousCategory: DangerousCategory? = null,

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT SNAPSHOT
    // ═══════════════════════════════════════════════════════════════

    /** Element text */
    val elementText: String = "",

    /** Element content description */
    val elementContentDesc: String = "",

    /** Element resource ID */
    val elementResourceId: String = "",

    /** Element class name */
    val elementClassName: String = "",

    /** Element bounds (JSON serialized Rect) */
    val elementBounds: String = "",

    // ═══════════════════════════════════════════════════════════════
    // TIMING
    // ═══════════════════════════════════════════════════════════════

    /** When element was first discovered (epoch ms) */
    val discoveredAt: Long,

    /** When element was last attempted to click (epoch ms, null if never attempted) */
    val lastAttemptAt: Long? = null,

    /** When decision was made about this element (epoch ms) */
    val decisionMadeAt: Long = System.currentTimeMillis()
) {
    /**
     * Get summary string for logging
     */
    fun toLogString(): String {
        val label = elementText.ifBlank {
            elementContentDesc.ifBlank { elementResourceId.ifBlank { "unlabeled" } }
        }
        return "${reason.getIcon()} [$status] $label - ${reason.displayName}"
    }

    /**
     * Get detailed explanation for user-facing display
     */
    fun getExplanation(): String {
        val base = reason.description
        return if (reasonDetail.isNotBlank()) {
            "$base\n\nDetails: $reasonDetail"
        } else {
            base
        }
    }

    /**
     * Convert to map for database insertion
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "element_uuid" to elementUuid,
            "screen_hash" to screenHash,
            "app_id" to appId,
            "session_id" to sessionId,
            "status" to status.name,
            "reason_code" to reason.name,
            "reason_detail" to reasonDetail,
            "dangerous_pattern" to dangerousPattern,
            "dangerous_category" to dangerousCategory?.name,
            "element_text" to elementText,
            "element_content_desc" to elementContentDesc,
            "element_resource_id" to elementResourceId,
            "element_class_name" to elementClassName,
            "element_bounds" to elementBounds,
            "discovered_at" to discoveredAt,
            "last_attempt_at" to lastAttemptAt,
            "decision_made_at" to decisionMadeAt
        )
    }
}

/**
 * Session Diagnostic Report
 *
 * Aggregate diagnostics for an entire exploration session.
 */
data class SessionDiagnosticReport(
    val sessionId: String,
    val appId: String,
    val startedAt: Long,
    val completedAt: Long?,

    // Element counts by status
    val totalElements: Int,
    val clickedCount: Int,
    val blockedCount: Int,
    val skippedCount: Int,
    val pendingCount: Int,

    // Reason breakdown
    val reasonCounts: Map<ElementStatusReason, Int>,

    // Dangerous category breakdown (for blocked elements)
    val dangerousCategoryCounts: Map<DangerousCategory, Int>,

    // All diagnostics
    val diagnostics: List<ElementDiagnostic>,

    // Session completion reason
    val completionReason: String? = null
) {
    /**
     * Get completion percentage
     */
    fun getCompletionPercent(): Float {
        return if (totalElements > 0) {
            (clickedCount.toFloat() / totalElements.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Get summary text
     */
    fun getSummaryText(): String {
        return """
            |Total: $totalElements elements
            |✅ Clicked: $clickedCount (${String.format("%.1f", getCompletionPercent())}%)
            |🚫 Blocked: $blockedCount
            |⏭️ Skipped: $skippedCount
            |⏳ Pending: $pendingCount
        """.trimMargin()
    }
}
