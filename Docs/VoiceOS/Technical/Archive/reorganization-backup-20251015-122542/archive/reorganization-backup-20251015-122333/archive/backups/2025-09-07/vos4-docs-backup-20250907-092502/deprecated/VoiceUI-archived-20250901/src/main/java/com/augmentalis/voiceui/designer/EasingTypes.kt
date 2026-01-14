/**
 * EasingTypes.kt - Animation easing type definitions
 */

package com.augmentalis.voiceui.designer

/**
 * Animation easing types for VoiceUI themes
 */
enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT, 
    EASE_IN_OUT,
    SPRING,
    EMPHASIZED,
    EMPHASIZED_DECELERATE,
    EMPHASIZED_ACCELERATE,
    
    // Cubic variations
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    
    // Back variations (overshoot)
    EASE_IN_BACK,
    EASE_OUT_BACK,
    EASE_IN_OUT_BACK,
    
    // Bounce variations
    EASE_IN_BOUNCE,
    EASE_OUT_BOUNCE,
    EASE_IN_OUT_BOUNCE,
    
    // Elastic variations
    EASE_IN_ELASTIC,
    EASE_OUT_ELASTIC,
    EASE_IN_OUT_ELASTIC
}