package com.augmentalis.avaelements.input

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Reflection-based VoiceOS adapter implementation.
 *
 * Uses reflection to interact with VoiceOS without compile-time dependency.
 * Gracefully degrades when VoiceOS is not available.
 *
 * SOLID Compliance:
 * - SRP: Single responsibility - VoiceOS reflection interaction only
 * - DIP: Implements abstraction (VoiceOSAdapter)
 * - LSP: Can be substituted for any VoiceOSAdapter
 */
class ReflectionVoiceOSAdapter private constructor(
    context: Context
) : VoiceOSAdapter {

    private val contextRef = WeakReference(context.applicationContext)
    private var voiceCursorInstance: Any? = null
    private var voiceCursorClass: Class<*>? = null

    init {
        try {
            voiceCursorClass = Class.forName(
                "com.augmentalis.voiceos.voicecursor.VoiceCursor"
            )
            voiceCursorInstance = voiceCursorClass?.getMethod(
                "getInstance",
                Context::class.java
            )?.invoke(null, context)
        } catch (e: Exception) {
            // VoiceOS not available - all operations will be no-ops
            voiceCursorClass = null
            voiceCursorInstance = null
        }
    }

    override val isAvailable: Boolean
        get() {
            val context = contextRef.get() ?: return false
            return try {
                voiceCursorClass?.getMethod("isAvailable", Context::class.java)
                    ?.invoke(null, context) as? Boolean ?: false
            } catch (e: Exception) {
                false
            }
        }

    override fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    ) {
        val instance = voiceCursorInstance ?: return
        val clazz = voiceCursorClass ?: return

        try {
            clazz.getMethod(
                "registerClickTarget",
                String::class.java,
                String::class.java,
                FloatArray::class.java,
                Function0::class.java
            ).invoke(instance, targetId, voiceLabel, bounds, callback)
        } catch (e: Exception) {
            // Silently ignore - VoiceOS may have been unloaded
        }
    }

    override fun unregisterClickTarget(targetId: String) {
        val instance = voiceCursorInstance ?: return
        val clazz = voiceCursorClass ?: return

        try {
            clazz.getMethod(
                "unregisterClickTarget",
                String::class.java
            ).invoke(instance, targetId)
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    override fun updateTargetBounds(targetId: String, bounds: FloatArray) {
        val instance = voiceCursorInstance ?: return
        val clazz = voiceCursorClass ?: return

        try {
            clazz.getMethod(
                "updateTargetBounds",
                String::class.java,
                FloatArray::class.java
            ).invoke(instance, targetId, bounds)
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    override fun startCursor() {
        val instance = voiceCursorInstance ?: return
        val clazz = voiceCursorClass ?: return

        try {
            clazz.getMethod("startCursor").invoke(instance)
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    override fun stopCursor() {
        val instance = voiceCursorInstance ?: return
        val clazz = voiceCursorClass ?: return

        try {
            clazz.getMethod("stopCursor").invoke(instance)
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    companion object {
        /**
         * Create adapter with automatic fallback to NoOp if VoiceOS unavailable
         */
        fun create(context: Context): VoiceOSAdapter {
            val adapter = ReflectionVoiceOSAdapter(context)
            return if (adapter.voiceCursorInstance != null) {
                adapter
            } else {
                NoOpVoiceOSAdapter()
            }
        }
    }
}
