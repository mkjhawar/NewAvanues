package com.augmentalis.avaelements.input

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Android VoiceCursor Implementation
 *
 * Integrates AvaElements with VoiceOS VoiceCursor system for
 * voice-controlled UI navigation on Android devices.
 *
 * Features:
 * - Registers UI components as voice targets
 * - Tracks cursor position for hover states
 * - Handles IMU-based head tracking
 * - Processes voice commands
 */

// ═══════════════════════════════════════════════════════════════
// Android VoiceCursor Manager
// ═══════════════════════════════════════════════════════════════

/**
 * Android implementation of VoiceCursorManager.
 * Bridges AvaElements to VoiceOS VoiceCursor system.
 *
 * SOLID Compliance:
 * - SRP: Manages cursor state and target registration only
 * - DIP: Depends on VoiceOSAdapter abstraction, not concrete implementation
 * - OCP: Can use different adapters without modifying this class
 */
class AndroidVoiceCursorManager private constructor(
    context: Context,
    private val voiceOSAdapter: VoiceOSAdapter
) : VoiceCursorManager {

    private val contextRef = WeakReference(context.applicationContext)
    private val registeredTargets = mutableMapOf<String, VoiceTarget>()
    private val listeners = mutableListOf<VoiceCursorListener>()

    private var _isActive = false
    private var _cursorPosition: Offset? = null

    // ─────────────────────────────────────────────────────────────
    // VoiceCursorManager Implementation
    // ─────────────────────────────────────────────────────────────

    override val isAvailable: Boolean
        get() = voiceOSAdapter.isAvailable

    override val isActive: Boolean
        get() = _isActive

    override val cursorPosition: Offset?
        get() = _cursorPosition

    override fun registerTarget(target: VoiceTarget) {
        registeredTargets[target.id] = target

        // Register with VoiceOS via adapter
        voiceOSAdapter.registerClickTarget(
            targetId = target.id,
            voiceLabel = target.label,
            bounds = floatArrayOf(
                target.bounds.left,
                target.bounds.top,
                target.bounds.right,
                target.bounds.bottom
            ),
            callback = target.onSelect
        )
    }

    override fun unregisterTarget(id: String) {
        registeredTargets.remove(id)

        // Unregister from VoiceOS via adapter
        voiceOSAdapter.unregisterClickTarget(id)
    }

    override fun updateTargetBounds(id: String, bounds: Rect) {
        val target = registeredTargets[id] ?: return
        registeredTargets[id] = target.copy(bounds = bounds)

        // Update VoiceOS registration via adapter
        voiceOSAdapter.updateTargetBounds(
            targetId = id,
            bounds = floatArrayOf(bounds.left, bounds.top, bounds.right, bounds.bottom)
        )
    }

    override fun handleVoiceCommand(command: String, parameters: Map<String, Any>): Boolean {
        // Find target by label
        val targetLabel = parameters["target"] as? String
        if (targetLabel != null) {
            val target = registeredTargets.values.find {
                it.label.equals(targetLabel, ignoreCase = true) && it.isEnabled
            }
            if (target != null) {
                when (command.lowercase()) {
                    VoiceCommands.CLICK, VoiceCommands.SELECT -> {
                        target.onSelect()
                        listeners.forEach { it.onTargetSelected(target) }
                        return true
                    }
                }
            }
        }

        // Handle cursor commands
        when (command.lowercase()) {
            VoiceCommands.CLICK -> {
                // Click at current cursor position
                val position = _cursorPosition ?: return false
                val target = findTargetAt(position)
                if (target != null && target.isEnabled) {
                    target.onSelect()
                    listeners.forEach { it.onTargetSelected(target) }
                    return true
                }
            }
        }

        return false
    }

    override fun start() {
        voiceOSAdapter.startCursor()
        _isActive = true
        listeners.forEach { it.onActivated() }
    }

    override fun stop() {
        voiceOSAdapter.stopCursor()
        _isActive = false
        listeners.forEach { it.onDeactivated() }
    }

    override fun addListener(listener: VoiceCursorListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: VoiceCursorListener) {
        listeners.remove(listener)
    }

    // ─────────────────────────────────────────────────────────────
    // Cursor Position Updates
    // ─────────────────────────────────────────────────────────────

    /**
     * Called by VoiceOS when cursor position changes.
     * Updates hover states on registered targets.
     */
    internal fun onCursorPositionChanged(x: Float, y: Float) {
        val newPosition = Offset(x, y)
        _cursorPosition = newPosition

        // Notify listeners
        listeners.forEach { it.onCursorMoved(newPosition) }

        // Update hover states
        registeredTargets.values.forEach { target ->
            val wasHovered = target.isHovered
            val isNowHovered = target.bounds.contains(newPosition)

            if (wasHovered != isNowHovered) {
                target.isHovered = isNowHovered
                target.onHover?.invoke(isNowHovered)

                if (isNowHovered) {
                    listeners.forEach { it.onTargetEntered(target) }
                } else {
                    listeners.forEach { it.onTargetExited(target) }
                }
            }

            // Notify cursor position within bounds
            if (isNowHovered) {
                target.onCursorMove?.invoke(newPosition)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────

    private fun findTargetAt(position: Offset): VoiceTarget? {
        return registeredTargets.values
            .filter { it.bounds.contains(position) && it.isEnabled }
            .maxByOrNull { it.priority }
    }

    // ─────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────

    companion object {
        @Volatile
        internal var instance: AndroidVoiceCursorManager? = null

        /**
         * Get or create singleton instance.
         *
         * @param context Android application context
         * @param adapter VoiceOS adapter implementation (defaults to reflection-based)
         */
        fun getInstance(
            context: Context,
            adapter: VoiceOSAdapter = ReflectionVoiceOSAdapter.create(context)
        ): AndroidVoiceCursorManager {
            return instance ?: synchronized(this) {
                instance ?: AndroidVoiceCursorManager(context, adapter).also { instance = it }
            }
        }

        /**
         * Initialize with custom adapter (for testing or custom integrations)
         */
        fun initialize(context: Context, adapter: VoiceOSAdapter) {
            synchronized(this) {
                instance = AndroidVoiceCursorManager(context, adapter)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Platform Actual Implementations
// ═══════════════════════════════════════════════════════════════

/**
 * Initialize VoiceCursor with Android context.
 * Creates appropriate adapter based on VoiceOS availability.
 *
 * Call this in Application.onCreate() or Activity.onCreate().
 *
 * SOLID Compliance:
 * - DIP: Uses VoiceOSAdapter abstraction
 * - Factory pattern: Auto-creates appropriate adapter
 */
fun initializeVoiceCursor(context: Context) {
    val adapter = ReflectionVoiceOSAdapter.create(context)
    AndroidVoiceCursorManager.initialize(context, adapter)
}

/**
 * Initialize VoiceCursor with custom adapter.
 * Useful for testing or custom VoiceOS integrations.
 *
 * @param context Android application context
 * @param adapter Custom VoiceOS adapter implementation
 */
fun initializeVoiceCursor(context: Context, adapter: VoiceOSAdapter) {
    AndroidVoiceCursorManager.initialize(context, adapter)
}

actual fun getVoiceCursorManager(): VoiceCursorManager {
    return AndroidVoiceCursorManager.instance ?: NoOpVoiceCursorManager()
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
