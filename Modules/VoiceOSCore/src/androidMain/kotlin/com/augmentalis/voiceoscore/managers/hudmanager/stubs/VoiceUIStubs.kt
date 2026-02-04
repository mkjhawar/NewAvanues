/**
 * VoiceUI stub implementations for HUDManager
 * Minimal implementations to enable HUDManager compilation without full VoiceUI module
 */

package com.augmentalis.voiceoscore.managers.hudmanager.stubs

// VoiceUI stub classes
class DatabaseModule {
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun getInstance(context: Any? = null): DatabaseModule = DatabaseModule()
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun storeContextPattern(locationKey: String, environmentName: String, confidence: Float) {
        // Stub implementation
    }
}

// VoiceAccessibility stub
object VOSAccessibilitySvc {
    fun isEnabled(): Boolean = false
    fun start() {}
    fun stop() {}
    fun getInstance(): VOSAccessibilitySvc = this
    
    // TTS delegation methods
    @Suppress("UNUSED_PARAMETER")
    fun speakText(text: String) {
        // Stub implementation - delegates to DeviceManager.AccessibilityManager
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun setSpeechRate(rate: Float) {
        // Stub implementation - delegates to DeviceManager.AccessibilityManager
    }
}

// VoiceUI HUD components stubs
object HUDRenderer {
    fun render() {}
    @Suppress("UNUSED_PARAMETER")
    fun setVisible(visible: Boolean) {}
    @Suppress("UNUSED_PARAMETER")
    fun updatePosition(x: Float, y: Float) {}
    @Suppress("UNUSED_PARAMETER")
    fun startRendering(fps: Int) {}
    @Suppress("UNUSED_PARAMETER")
    fun updateModeRendering(mode: Any) {}
    @Suppress("UNUSED_PARAMETER")
    fun adjustForHeadMovement(data: OrientationData) {}
    fun getCurrentFPS(): Float = 60f
    
    // Constants
    const val TARGET_FPS_HIGH = 60
}

object HUDSystem {
    fun initialize() {}
    fun shutdown() {}
    fun isReady(): Boolean = true
    @Suppress("UNUSED_PARAMETER")
    fun setVisible(visible: Boolean) {}
    fun isVisible(): Boolean = true
    fun toggleVisibility() {}
    @Suppress("UNUSED_PARAMETER")
    fun showNotification(message: String, duration: Int, position: String, priority: String) {}
    @Suppress("UNUSED_PARAMETER")
    fun removeElement(elementId: String) {}
}

// Intent stub
class HUDIntent {
    companion object {
        const val ACTION_SHOW_HUD = "com.augmentalis.action.SHOW_HUD"
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun setPackage(packageName: String): HUDIntent {
        return this
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun setClassName(packageName: String, className: String): HUDIntent = this
}

// VoiceUI package stubs - these replace the missing voiceui imports
// VoiceUI HUD enums and classes
enum class HUDMode {
    STANDARD, MEETING, DRIVING, WORKSHOP, ACCESSIBILITY, GAMING, ENTERTAINMENT
}

enum class RenderMode {
    SPATIAL_AR, OVERLAY_2D, MIXED_REALITY
}

data class OrientationData(
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val roll: Float = 0f
)

object voiceui {
    object hud {
        // Enum references for the package-qualified usage
        object HUDMode {
            val STANDARD = com.augmentalis.hudmanager.stubs.HUDMode.STANDARD
            val MEETING = com.augmentalis.hudmanager.stubs.HUDMode.MEETING
            val DRIVING = com.augmentalis.hudmanager.stubs.HUDMode.DRIVING
            val WORKSHOP = com.augmentalis.hudmanager.stubs.HUDMode.WORKSHOP
            val ACCESSIBILITY = com.augmentalis.hudmanager.stubs.HUDMode.ACCESSIBILITY
            val GAMING = com.augmentalis.hudmanager.stubs.HUDMode.GAMING
            val ENTERTAINMENT = com.augmentalis.hudmanager.stubs.HUDMode.ENTERTAINMENT
        }
        
        object RenderMode {
            val SPATIAL_AR = com.augmentalis.hudmanager.stubs.RenderMode.SPATIAL_AR
            val OVERLAY_2D = com.augmentalis.hudmanager.stubs.RenderMode.OVERLAY_2D
            val MIXED_REALITY = com.augmentalis.hudmanager.stubs.RenderMode.MIXED_REALITY
        }
        
        fun OrientationData(pitch: Float, yaw: Float, roll: Float) = 
            com.augmentalis.hudmanager.stubs.OrientationData(pitch, yaw, roll)
    }
    
    object design {
        object theme {
            const val primaryColor = "#2196F3"
            const val backgroundColor = "#FFFFFF"
        }
        object layout {
            const val hudWidth = 320
            const val hudHeight = 240
        }
    }
    
    object components {
        object hud {
            fun createOverlay() {}
            @Suppress("UNUSED_PARAMETER")
            fun showNotification(message: String) {}
        }
    }
}

object voiceos {
    object core {
        fun getApplicationContext(): Any? = null
        fun isRunning(): Boolean = true
    }
}

object voiceaccessibility {
    object service {
        fun isEnabled(): Boolean = false
        fun requestPermission() {}
    }
}