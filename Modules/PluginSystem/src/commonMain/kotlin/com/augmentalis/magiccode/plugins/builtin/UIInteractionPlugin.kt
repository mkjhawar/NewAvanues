/**
 * UIInteractionPlugin.kt - UI Interaction Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Migrates the UIHandler from VoiceOSCore to the Universal Plugin Architecture.
 * This plugin handles all UI element interactions including clicks, taps, long press,
 * double tap, expand/collapse, check/uncheck, toggle, focus, and dismiss actions.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../UIHandler.kt
 *
 * ## Key Features
 * - AVID-based click path (fastest path) for dynamic commands
 * - Text search fallback path with disambiguation support
 * - Full disambiguation flow for duplicate elements
 * - Numbered badge overlay for user selection
 *
 * ## Execution Priority
 * 1. **AVID path (fastest)**: If command.targetAvid is set (from dynamic commands),
 *    execute directly via clickByAvid() - no tree search needed.
 * 2. **Text search path**: If no AVID, fall back to UI tree search with disambiguation.
 *
 * ## Disambiguation Flow
 * When a command like "click Submit" matches multiple elements:
 * 1. System finds all matching elements
 * 2. Numbers (1, 2, 3...) are shown ONLY on matching elements
 * 3. User says a number to select
 * 4. Action executes on selected element
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.commandmanager.ActionResult
import com.augmentalis.commandmanager.Bounds
import com.augmentalis.commandmanager.DisambiguationResult
import com.augmentalis.commandmanager.ElementDisambiguator
import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.NumberedMatch
import com.augmentalis.commandmanager.QuantizedCommand

/**
 * UI Interaction Handler Plugin - Handles all UI element interactions.
 *
 * Supports:
 * - Click actions: click, tap, press
 * - Long click: long click, long press
 * - Double tap: double tap, double click
 * - Toggle actions: expand, collapse, check, uncheck, toggle
 * - Focus/dismiss: focus, dismiss, close
 *
 * ## Usage
 * ```kotlin
 * val executor = AndroidUIInteractionExecutor(accessibilityService)
 * val plugin = UIInteractionPlugin { executor }
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "click submit", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * ## Disambiguation Example
 * ```kotlin
 * // User says "click Submit" - multiple matches
 * val result = plugin.handle(command, context)
 * // result is ActionResult.Ambiguous with 3 candidates
 *
 * // Plugin shows disambiguation overlay with numbers
 * // plugin.isDisambiguationActive() returns true
 *
 * // User says "two"
 * val selectionResult = plugin.handleNumberSelection(2)
 * // selectionResult is ActionResult.Success("Clicked Submit (Payment)")
 * ```
 *
 * @param executorProvider Provider for the platform-specific executor (lazy initialization)
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see UIInteractionExecutor
 */
class UIInteractionPlugin(
    private val executorProvider: () -> UIInteractionExecutor
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "UI Interaction Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "UI Interaction Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin"),
            metadata = mapOf(
                "handlerType" to "UI_INTERACTION",
                "supportsClick" to "true",
                "supportsLongClick" to "true",
                "supportsDoubleClick" to "true",
                "supportsToggle" to "true",
                "supportsExpand" to "true",
                "supportsFocus" to "true",
                "supportsDisambiguation" to "true",
                "supportsAvidPath" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.UI_INTERACTION

    override val patterns: List<CommandPattern> = listOf(
        // Click/tap/press patterns
        CommandPattern(
            regex = Regex("^(click|tap|press)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "CLICK",
            requiredEntities = setOf("target"),
            examples = listOf("click submit", "tap next", "press button", "click BTN:a3f2e1c9")
        ),
        // Long click/press patterns
        CommandPattern(
            regex = Regex("^long\\s+(click|press)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "LONG_CLICK",
            requiredEntities = setOf("target"),
            examples = listOf("long click item", "long press photo")
        ),
        // Double tap/click patterns
        CommandPattern(
            regex = Regex("^double\\s+(tap|click)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "DOUBLE_CLICK",
            requiredEntities = setOf("target"),
            examples = listOf("double tap image", "double click text")
        ),
        // Expand/collapse patterns
        CommandPattern(
            regex = Regex("^expand\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "EXPAND",
            requiredEntities = setOf("target"),
            examples = listOf("expand details", "expand menu")
        ),
        CommandPattern(
            regex = Regex("^collapse\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "COLLAPSE",
            requiredEntities = setOf("target"),
            examples = listOf("collapse details", "collapse section")
        ),
        // Check/uncheck/toggle patterns
        CommandPattern(
            regex = Regex("^check\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "CHECK",
            requiredEntities = setOf("target"),
            examples = listOf("check remember me", "check agree")
        ),
        CommandPattern(
            regex = Regex("^uncheck\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "UNCHECK",
            requiredEntities = setOf("target"),
            examples = listOf("uncheck subscribe", "uncheck notifications")
        ),
        CommandPattern(
            regex = Regex("^toggle\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "TOGGLE",
            requiredEntities = setOf("target"),
            examples = listOf("toggle dark mode", "toggle wifi")
        ),
        // Focus pattern
        CommandPattern(
            regex = Regex("^focus\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "FOCUS",
            requiredEntities = setOf("target"),
            examples = listOf("focus search", "focus email field")
        ),
        // Dismiss/close patterns (no target)
        CommandPattern(
            regex = Regex("^(dismiss|close)$", RegexOption.IGNORE_CASE),
            intent = "DISMISS",
            requiredEntities = emptySet(),
            examples = listOf("dismiss", "close")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action phrases.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "click", "tap", "press",
        "long click", "long press",
        "double tap", "double click",
        "expand", "collapse",
        "check", "uncheck", "toggle",
        "focus", "dismiss", "close"
    )

    // =========================================================================
    // Executor and Disambiguator
    // =========================================================================

    private lateinit var executor: UIInteractionExecutor
    private val disambiguator: ElementDisambiguator = ElementDisambiguator.default

    // =========================================================================
    // Disambiguation State
    // =========================================================================

    /**
     * Current disambiguation state.
     * When non-null, the system is waiting for user to say a number.
     */
    private var activeDisambiguation: ActiveDisambiguation? = null

    /**
     * Callback invoked when disambiguation overlay should be shown.
     * Platform implementations should show numbered badges on matching elements.
     */
    var onShowDisambiguation: ((DisambiguationResult) -> Unit)? = null

    /**
     * Callback invoked when disambiguation is complete or cancelled.
     * Platform implementations should hide numbered badges.
     */
    var onHideDisambiguation: (() -> Unit)? = null

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            executor = executorProvider()
            InitResult.success("UIInteractionPlugin initialized")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        // Clear any active disambiguation
        clearDisambiguation()
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "executorInitialized" to (::executor.isInitialized).toString(),
        "disambiguationActive" to isDisambiguationActive().toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()
        return patterns.any { it.matches(phrase) } ||
                supportedActions.any { phrase.startsWith(it.lowercase()) }
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when {
            // Click/Tap/Press actions with target
            normalizedAction.startsWith("click ") ||
            normalizedAction.startsWith("tap ") ||
            normalizedAction.startsWith("press ") -> {
                val target = normalizedAction
                    .removePrefix("click ")
                    .removePrefix("tap ")
                    .removePrefix("press ")
                    .trim()

                // Check if target is an AVID (direct reference, no disambiguation needed)
                val avid = command.targetAvid ?: extractAvid(target)
                if (avid != null) {
                    if (executor.clickByAvid(avid)) {
                        ActionResult.Success("Clicked element")
                    } else {
                        ActionResult.Error("Could not click element with AVID: $avid")
                    }
                } else {
                    // Find matching elements for disambiguation
                    handleClickWithDisambiguation(target, UIAction.CLICK)
                }
            }

            // Long click/press
            normalizedAction.startsWith("long click ") ||
            normalizedAction.startsWith("long press ") -> {
                val target = normalizedAction
                    .removePrefix("long click ")
                    .removePrefix("long press ")
                    .trim()

                val avid = command.targetAvid ?: extractAvid(target)
                if (avid != null) {
                    if (executor.longClickByAvid(avid)) {
                        ActionResult.Success("Long clicked element")
                    } else {
                        ActionResult.Error("Could not long click element with AVID: $avid")
                    }
                } else {
                    handleClickWithDisambiguation(target, UIAction.LONG_CLICK)
                }
            }

            // Double tap/click
            normalizedAction.startsWith("double tap ") ||
            normalizedAction.startsWith("double click ") -> {
                val target = normalizedAction
                    .removePrefix("double tap ")
                    .removePrefix("double click ")
                    .trim()

                handleClickWithDisambiguation(target, UIAction.DOUBLE_CLICK)
            }

            // Expand
            normalizedAction.startsWith("expand ") -> {
                val target = normalizedAction.removePrefix("expand ").trim()
                if (executor.expand(target)) {
                    ActionResult.Success("Expanded $target")
                } else {
                    ActionResult.Error("Could not expand: $target")
                }
            }

            // Collapse
            normalizedAction.startsWith("collapse ") -> {
                val target = normalizedAction.removePrefix("collapse ").trim()
                if (executor.collapse(target)) {
                    ActionResult.Success("Collapsed $target")
                } else {
                    ActionResult.Error("Could not collapse: $target")
                }
            }

            // Check
            normalizedAction.startsWith("check ") -> {
                val target = normalizedAction.removePrefix("check ").trim()
                if (executor.setChecked(target, true)) {
                    ActionResult.Success("Checked $target")
                } else {
                    ActionResult.Error("Could not check: $target")
                }
            }

            // Uncheck
            normalizedAction.startsWith("uncheck ") -> {
                val target = normalizedAction.removePrefix("uncheck ").trim()
                if (executor.setChecked(target, false)) {
                    ActionResult.Success("Unchecked $target")
                } else {
                    ActionResult.Error("Could not uncheck: $target")
                }
            }

            // Toggle
            normalizedAction.startsWith("toggle ") -> {
                val target = normalizedAction.removePrefix("toggle ").trim()
                if (executor.toggle(target)) {
                    ActionResult.Success("Toggled $target")
                } else {
                    ActionResult.Error("Could not toggle: $target")
                }
            }

            // Focus
            normalizedAction.startsWith("focus ") -> {
                val target = normalizedAction.removePrefix("focus ").trim()
                if (executor.focus(target)) {
                    ActionResult.Success("Focused $target")
                } else {
                    ActionResult.Error("Could not focus: $target")
                }
            }

            // Dismiss/Close
            normalizedAction == "dismiss" || normalizedAction == "close" -> {
                if (executor.dismiss()) {
                    ActionResult.Success("Dismissed")
                } else {
                    ActionResult.Error("Could not dismiss")
                }
            }

            else -> ActionResult.Error("Unknown UI action: $normalizedAction")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Check for AVID format in the command - highest confidence
        if (command.targetAvid != null || extractAvid(phrase.substringAfter(" ")) != null) {
            return 1.0f
        }

        // Check if it starts with a supported action verb
        for (action in supportedActions) {
            val actionLower = action.lowercase()
            if (phrase == actionLower || phrase.startsWith("$actionLower ")) {
                return 0.95f
            }
        }

        // Pattern match
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.9f
            }
        }

        // Partial match (contains UI action verbs)
        val uiVerbs = listOf("click", "tap", "press", "toggle", "check", "expand", "focus")
        if (uiVerbs.any { phrase.contains(it) }) {
            return 0.6f
        }

        return 0.0f
    }

    // =========================================================================
    // AVID Extraction
    // =========================================================================

    /**
     * Extract AVID fingerprint from target string.
     * AVIDs are in format "{TypeCode}:{hash8}" (e.g., "BTN:a3f2e1c9")
     * or legacy formats like "avid:" prefix or 8-character hex strings.
     *
     * @param target The target string to parse
     * @return AVID if found, null otherwise
     */
    private fun extractAvid(target: String): String? {
        return when {
            // New AVID format: BTN:a3f2e1c9
            target.matches(Regex("^[A-Z]{3}:[a-f0-9]{8}$")) -> target
            // Legacy prefix format
            target.startsWith("avid:") -> target.removePrefix("avid:")
            // Legacy 8-char hex format
            target.matches(Regex("^[a-f0-9]{8}$")) -> target
            else -> null
        }
    }

    // =========================================================================
    // Disambiguation Support
    // =========================================================================

    /**
     * Handle click action with disambiguation for duplicate elements.
     *
     * Flow:
     * 1. Get all screen elements from executor
     * 2. ElementDisambiguator matches on TEXT first, then contentDescription fallback
     * 3. If single match: execute directly
     * 4. If multiple matches: show numbered badges with highlighting, popup fades
     * 5. If no matches: return failure
     *
     * For short targets (digits like "4"), use EXACT match to avoid false positives.
     *
     * @param target The target element text to find
     * @param action The UI action to perform
     * @return ActionResult indicating success, failure, or awaiting selection
     */
    private suspend fun handleClickWithDisambiguation(
        target: String,
        action: UIAction
    ): ActionResult {
        // Get current screen elements
        val screenElements = executor.getScreenElements()

        // For short targets (single digits, letters), use EXACT match
        // For longer targets, use CONTAINS for flexibility
        val matchMode = if (target.length <= 2) {
            ElementDisambiguator.MatchMode.EXACT
        } else {
            ElementDisambiguator.MatchMode.CONTAINS
        }

        // Find matches - ElementDisambiguator prioritizes TEXT over contentDescription
        val result = disambiguator.findMatches(
            query = target,
            elements = screenElements,
            matchMode = matchMode
        )

        return when {
            // No matches found
            result.noMatches -> {
                ActionResult.Error("Could not find element: $target")
            }

            // Single match - execute directly
            result.singleMatch != null -> {
                executeActionOnElement(result.singleMatch!!, action)
            }

            // Multiple matches - need disambiguation
            result.needsDisambiguation -> {
                // Store active disambiguation state
                activeDisambiguation = ActiveDisambiguation(
                    result = result,
                    pendingAction = action
                )

                // Notify platform to show numbered badges ONLY on matching elements
                onShowDisambiguation?.invoke(result)

                // Return awaiting selection result
                ActionResult.Ambiguous(
                    candidates = result.numberedItems.map { it.displayLabel },
                    message = "${result.matchCount} '${target}' elements found. Say a number to select."
                )
            }

            else -> ActionResult.Error("Unexpected disambiguation state")
        }
    }

    /**
     * Handle number selection during disambiguation.
     *
     * Call this when user says "one", "two", "three", etc.
     *
     * @param number The number spoken (1-based)
     * @return Result of the action, or failure if no disambiguation active
     */
    suspend fun handleNumberSelection(number: Int): ActionResult {
        val disambiguation = activeDisambiguation
            ?: return ActionResult.Error("No disambiguation active")

        val selectedElement = disambiguator.selectByNumber(disambiguation.result, number)
            ?: return ActionResult.Error("Invalid selection: $number")

        // Clear disambiguation state
        clearDisambiguation()

        // Execute the pending action
        return executeActionOnElement(selectedElement, disambiguation.pendingAction)
    }

    /**
     * Cancel active disambiguation.
     *
     * Call this when user says "cancel" or times out.
     */
    fun cancelDisambiguation() {
        clearDisambiguation()
    }

    /**
     * Check if disambiguation is currently active.
     *
     * @return true if waiting for user number selection
     */
    fun isDisambiguationActive(): Boolean = activeDisambiguation != null

    /**
     * Get current disambiguation matches (for overlay display).
     *
     * @return List of numbered matches, or null if no active disambiguation
     */
    fun getActiveDisambiguationMatches(): List<NumberedMatch>? =
        activeDisambiguation?.result?.numberedItems

    /**
     * Get the current disambiguation result (for advanced UI handling).
     *
     * @return The full disambiguation result, or null if not active
     */
    fun getActiveDisambiguationResult(): DisambiguationResult? =
        activeDisambiguation?.result

    private fun clearDisambiguation() {
        activeDisambiguation = null
        onHideDisambiguation?.invoke()
    }

    /**
     * Execute a UI action on a specific element.
     *
     * @param element The element to act on
     * @param action The action to perform
     * @return ActionResult indicating success or failure
     */
    private suspend fun executeActionOnElement(element: ElementInfo, action: UIAction): ActionResult {
        val success = when (action) {
            UIAction.CLICK -> executor.clickElement(element)
            UIAction.LONG_CLICK -> executor.longClickElement(element)
            UIAction.DOUBLE_CLICK -> executor.doubleClickElement(element)
        }

        return if (success) {
            ActionResult.Success("${action.displayName} ${element.voiceLabel}")
        } else {
            ActionResult.Error("Could not ${action.displayName.lowercase()} ${element.voiceLabel}")
        }
    }

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.commandmanager.handler.ui"
    }
}

// =============================================================================
// UI Action Types
// =============================================================================

/**
 * UI action types for disambiguation and execution.
 *
 * @property displayName Human-readable name for success messages
 */
enum class UIAction(val displayName: String) {
    /** Standard click/tap/press action */
    CLICK("Clicked"),
    /** Long click/long press action */
    LONG_CLICK("Long clicked"),
    /** Double tap/double click action */
    DOUBLE_CLICK("Double clicked")
}

// =============================================================================
// Active Disambiguation State
// =============================================================================

/**
 * Active disambiguation state tracking.
 *
 * Stores the disambiguation result and pending action while waiting
 * for user to say a number selection.
 *
 * @property result The disambiguation result with numbered matches
 * @property pendingAction The action to execute once user selects
 */
private data class ActiveDisambiguation(
    val result: DisambiguationResult,
    val pendingAction: UIAction
)

// =============================================================================
// Executor Interface
// =============================================================================

/**
 * Platform-specific executor interface for UI interaction actions.
 *
 * This interface provides the contract for executing UI actions on different platforms.
 * Implementations are platform-specific:
 * - Android: Uses AccessibilityService for UI automation
 * - iOS: Uses UIAccessibility APIs
 * - Desktop: Uses native accessibility APIs or simulated input
 *
 * ## Implementation Notes
 * - All methods are suspending to allow for async platform operations
 * - Return false on failure rather than throwing exceptions
 * - AVID-based methods are the fastest path (no tree search needed)
 * - Text-based methods may require UI tree traversal
 *
 * ## Example Android Implementation
 * ```kotlin
 * class AndroidUIInteractionExecutor(
 *     private val service: AccessibilityService
 * ) : UIInteractionExecutor {
 *     override suspend fun getScreenElements(): List<ElementInfo> {
 *         return UITreeScraper.scrape(service.rootInActiveWindow)
 *     }
 *
 *     override suspend fun clickByAvid(avid: String): Boolean {
 *         val node = AvidResolver.findByAvid(service.rootInActiveWindow, avid)
 *         return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
 *     }
 *     // ... other implementations
 * }
 * ```
 */
interface UIInteractionExecutor {

    // =========================================================================
    // Element Discovery (for disambiguation)
    // =========================================================================

    /**
     * Get all interactive elements currently on screen.
     * Used for disambiguation when multiple elements match a voice command.
     *
     * @return List of all actionable elements on current screen
     */
    suspend fun getScreenElements(): List<ElementInfo>

    // =========================================================================
    // Direct Element Actions (used after disambiguation)
    // =========================================================================

    /**
     * Click a specific element.
     *
     * @param element The element to click (from disambiguation result)
     * @return true if click was performed successfully
     */
    suspend fun clickElement(element: ElementInfo): Boolean

    /**
     * Long click a specific element.
     *
     * @param element The element to long click
     * @return true if long click was performed successfully
     */
    suspend fun longClickElement(element: ElementInfo): Boolean

    /**
     * Double click a specific element.
     *
     * @param element The element to double click
     * @return true if double click was performed successfully
     */
    suspend fun doubleClickElement(element: ElementInfo): Boolean

    // =========================================================================
    // AVID-based Actions (primary path for dynamic commands)
    // =========================================================================

    /**
     * Click element by AVID fingerprint.
     *
     * This is the fastest execution path - directly locates element by AVID
     * without searching the entire UI tree.
     *
     * AVID format: {TypeCode}:{hash8} (e.g., "BTN:a3f2e1c9")
     *
     * @param avid The AVID fingerprint
     * @return true if click was performed successfully
     */
    suspend fun clickByAvid(avid: String): Boolean

    /**
     * Long click element by AVID fingerprint.
     *
     * @param avid The AVID fingerprint
     * @return true if long click was performed successfully
     */
    suspend fun longClickByAvid(avid: String): Boolean

    // =========================================================================
    // Text-based Actions (fallback path)
    // =========================================================================

    /**
     * Click element by visible text.
     *
     * Searches the UI tree for an element with matching text.
     * Use clickByAvid() when AVID is available for better performance.
     *
     * @param text The text to search for
     * @return true if element found and clicked
     */
    suspend fun clickByText(text: String): Boolean

    /**
     * Long click element by visible text.
     *
     * @param text The text to search for
     * @return true if element found and long clicked
     */
    suspend fun longClickByText(text: String): Boolean

    /**
     * Double click element by visible text.
     *
     * @param text The text to search for
     * @return true if element found and double clicked
     */
    suspend fun doubleClickByText(text: String): Boolean

    // =========================================================================
    // Expand/Collapse Actions
    // =========================================================================

    /**
     * Expand an expandable element.
     *
     * Used for collapsible sections, dropdown menus, tree nodes, etc.
     *
     * @param target The target element identifier (text or resourceId)
     * @return true if expand action was performed
     */
    suspend fun expand(target: String): Boolean

    /**
     * Collapse an expanded element.
     *
     * @param target The target element identifier
     * @return true if collapse action was performed
     */
    suspend fun collapse(target: String): Boolean

    // =========================================================================
    // Check/Toggle Actions
    // =========================================================================

    /**
     * Set the checked state of a checkbox, switch, or toggle.
     *
     * @param target The target element identifier
     * @param checked true to check, false to uncheck
     * @return true if state was changed successfully
     */
    suspend fun setChecked(target: String, checked: Boolean): Boolean

    /**
     * Toggle the state of a checkbox, switch, or toggle.
     *
     * Inverts the current checked state.
     *
     * @param target The target element identifier
     * @return true if toggle was performed
     */
    suspend fun toggle(target: String): Boolean

    // =========================================================================
    // Focus/Dismiss Actions
    // =========================================================================

    /**
     * Set focus on an element.
     *
     * Moves accessibility focus to the target element.
     * Typically used for input fields.
     *
     * @param target The target element identifier
     * @return true if focus was set successfully
     */
    suspend fun focus(target: String): Boolean

    /**
     * Dismiss the current dialog, popup, or overlay.
     *
     * Performs the platform-appropriate dismiss action (back button, escape, etc.)
     *
     * @return true if dismiss was performed
     */
    suspend fun dismiss(): Boolean
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a UIInteractionPlugin with a pre-configured executor.
 *
 * @param executor The UI interaction executor implementation
 * @return Configured UIInteractionPlugin
 */
fun createUIInteractionPlugin(
    executor: UIInteractionExecutor
): UIInteractionPlugin {
    return UIInteractionPlugin { executor }
}

/**
 * Create a UIInteractionPlugin with a lazy executor provider.
 *
 * Useful when the executor depends on platform services that may
 * not be available at plugin creation time (e.g., AccessibilityService).
 *
 * @param executorProvider Function that returns the executor when needed
 * @return Configured UIInteractionPlugin
 */
fun createUIInteractionPlugin(
    executorProvider: () -> UIInteractionExecutor
): UIInteractionPlugin {
    return UIInteractionPlugin(executorProvider)
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock executor for testing UIInteractionPlugin.
 *
 * Records all actions and can be configured to succeed or fail.
 * Also allows setting up mock screen elements for disambiguation testing.
 *
 * ## Usage
 * ```kotlin
 * val mockExecutor = MockUIInteractionExecutor(shouldSucceed = true)
 *
 * // Set up test elements
 * mockExecutor.setScreenElements(listOf(
 *     ElementInfo.button("Submit", bounds = Bounds(0, 0, 100, 50)),
 *     ElementInfo.button("Submit", bounds = Bounds(0, 100, 100, 150)),
 *     ElementInfo.button("Cancel", bounds = Bounds(0, 200, 100, 250))
 * ))
 *
 * val plugin = createUIInteractionPlugin(mockExecutor)
 * plugin.initialize(config, context)
 *
 * // Test click
 * val result = plugin.handle(
 *     QuantizedCommand(phrase = "click submit"),
 *     handlerContext
 * )
 *
 * // Verify - should need disambiguation (2 Submit buttons)
 * assert(result is ActionResult.Ambiguous)
 * assert(mockExecutor.actions.isEmpty()) // No action yet
 *
 * // Select first option
 * val selectResult = plugin.handleNumberSelection(1)
 * assert(selectResult.isSuccess)
 * assert(mockExecutor.actions.contains("clickElement:Submit"))
 * ```
 *
 * @param shouldSucceed Whether actions should succeed (default true)
 */
class MockUIInteractionExecutor(
    private val shouldSucceed: Boolean = true
) : UIInteractionExecutor {

    private val _actions = mutableListOf<String>()
    private var _screenElements = mutableListOf<ElementInfo>()

    /** List of recorded actions */
    val actions: List<String> get() = _actions.toList()

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    /**
     * Set up mock screen elements for disambiguation testing.
     *
     * @param elements List of elements to return from getScreenElements()
     */
    fun setScreenElements(elements: List<ElementInfo>) {
        _screenElements.clear()
        _screenElements.addAll(elements)
    }

    /**
     * Add a single mock element.
     *
     * @param element Element to add
     */
    fun addElement(element: ElementInfo) {
        _screenElements.add(element)
    }

    /** Clear all mock elements */
    fun clearElements() = _screenElements.clear()

    // =========================================================================
    // UIInteractionExecutor Implementation
    // =========================================================================

    override suspend fun getScreenElements(): List<ElementInfo> {
        _actions.add("getScreenElements")
        return _screenElements.toList()
    }

    override suspend fun clickElement(element: ElementInfo): Boolean {
        _actions.add("clickElement:${element.voiceLabel}")
        return shouldSucceed
    }

    override suspend fun longClickElement(element: ElementInfo): Boolean {
        _actions.add("longClickElement:${element.voiceLabel}")
        return shouldSucceed
    }

    override suspend fun doubleClickElement(element: ElementInfo): Boolean {
        _actions.add("doubleClickElement:${element.voiceLabel}")
        return shouldSucceed
    }

    override suspend fun clickByAvid(avid: String): Boolean {
        _actions.add("clickByAvid:$avid")
        return shouldSucceed
    }

    override suspend fun longClickByAvid(avid: String): Boolean {
        _actions.add("longClickByAvid:$avid")
        return shouldSucceed
    }

    override suspend fun clickByText(text: String): Boolean {
        _actions.add("clickByText:$text")
        return shouldSucceed
    }

    override suspend fun longClickByText(text: String): Boolean {
        _actions.add("longClickByText:$text")
        return shouldSucceed
    }

    override suspend fun doubleClickByText(text: String): Boolean {
        _actions.add("doubleClickByText:$text")
        return shouldSucceed
    }

    override suspend fun expand(target: String): Boolean {
        _actions.add("expand:$target")
        return shouldSucceed
    }

    override suspend fun collapse(target: String): Boolean {
        _actions.add("collapse:$target")
        return shouldSucceed
    }

    override suspend fun setChecked(target: String, checked: Boolean): Boolean {
        _actions.add("setChecked:$target:$checked")
        return shouldSucceed
    }

    override suspend fun toggle(target: String): Boolean {
        _actions.add("toggle:$target")
        return shouldSucceed
    }

    override suspend fun focus(target: String): Boolean {
        _actions.add("focus:$target")
        return shouldSucceed
    }

    override suspend fun dismiss(): Boolean {
        _actions.add("dismiss")
        return shouldSucceed
    }
}

// =============================================================================
// Testing Helpers
// =============================================================================

/**
 * Create a mock plugin for testing with pre-configured elements.
 *
 * @param elements Elements to populate the mock executor with
 * @param shouldSucceed Whether actions should succeed
 * @return Pair of (plugin, mockExecutor) for testing
 */
fun createMockUIInteractionPlugin(
    elements: List<ElementInfo> = emptyList(),
    shouldSucceed: Boolean = true
): Pair<UIInteractionPlugin, MockUIInteractionExecutor> {
    val mockExecutor = MockUIInteractionExecutor(shouldSucceed)
    mockExecutor.setScreenElements(elements)
    val plugin = UIInteractionPlugin { mockExecutor }
    return plugin to mockExecutor
}

/**
 * Create test elements for disambiguation testing.
 *
 * Creates a list of elements with the specified label, useful for
 * testing disambiguation scenarios.
 *
 * @param label The text label for elements
 * @param count Number of elements to create
 * @param startY Starting Y position (elements are stacked vertically)
 * @return List of test elements
 */
fun createTestElements(
    label: String,
    count: Int,
    startY: Int = 0
): List<ElementInfo> {
    return (0 until count).map { index ->
        ElementInfo.button(
            text = label,
            resourceId = "com.test:id/${label.lowercase()}_$index",
            bounds = Bounds(0, startY + (index * 100), 200, startY + (index * 100) + 80)
        )
    }
}
