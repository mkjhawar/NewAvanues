/**
 * ElementAnimation.kt - Animation properties for VoiceUI elements
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-24
 */
package com.augmentalis.voiceui.designer

// EasingType is in the same package, no import needed

/**
 * Animation configuration for VoiceUI elements
 * 
 * @property duration Animation duration in milliseconds
 * @property easing Easing function for animation
 * @property delay Delay before animation starts in milliseconds
 * @property repeat Number of times to repeat (-1 for infinite)
 * @property reverseOnRepeat Whether to reverse animation on repeat
 * @property startValue Starting value for animation (0.0 to 1.0)
 * @property endValue Ending value for animation (0.0 to 1.0)
 */
data class ElementAnimation(
    val duration: Long = 300,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    val delay: Long = 0,
    val repeat: Int = 0,
    val reverseOnRepeat: Boolean = false,
    val startValue: Float = 0f,
    val endValue: Float = 1f
) {
    
    /**
     * Check if animation should loop infinitely
     */
    val isInfinite: Boolean
        get() = repeat == -1
    
    /**
     * Get total animation duration including repeats
     */
    val totalDuration: Long
        get() = if (isInfinite) Long.MAX_VALUE else duration * (repeat + 1) + delay
    
    /**
     * Create a spring animation variant
     */
    fun withSpring(stiffness: Float = 1500f, damping: Float = 0.5f): ElementAnimation {
        return copy(
            easing = EasingType.SPRING,
            duration = (1000 / stiffness * 4).toLong()
        )
    }
    
    /**
     * Create a fade animation
     */
    fun withFade(fadeIn: Boolean = true): ElementAnimation {
        return copy(
            startValue = if (fadeIn) 0f else 1f,
            endValue = if (fadeIn) 1f else 0f,
            easing = EasingType.EASE_IN_OUT
        )
    }
    
    /**
     * Create a scale animation
     */
    fun withScale(from: Float = 0f, to: Float = 1f): ElementAnimation {
        return copy(
            startValue = from,
            endValue = to,
            easing = EasingType.EASE_OUT_BACK
        )
    }
    
    companion object {
        /**
         * No animation
         */
        val NONE = ElementAnimation(duration = 0)
        
        /**
         * Quick fade animation (150ms)
         */
        val FADE_QUICK = ElementAnimation(
            duration = 150,
            easing = EasingType.EASE_IN_OUT
        )
        
        /**
         * Standard fade animation (300ms)
         */
        val FADE_STANDARD = ElementAnimation(
            duration = 300,
            easing = EasingType.EASE_IN_OUT
        )
        
        /**
         * Slow fade animation (500ms)
         */
        val FADE_SLOW = ElementAnimation(
            duration = 500,
            easing = EasingType.EASE_IN_OUT
        )
        
        /**
         * Bounce animation
         */
        val BOUNCE = ElementAnimation(
            duration = 600,
            easing = EasingType.EASE_OUT_BOUNCE
        )
        
        /**
         * Elastic animation
         */
        val ELASTIC = ElementAnimation(
            duration = 800,
            easing = EasingType.EASE_OUT_ELASTIC
        )
        
        /**
         * Slide in animation
         */
        val SLIDE_IN = ElementAnimation(
            duration = 300,
            easing = EasingType.EASE_OUT_CUBIC
        )
        
        /**
         * Pop animation (scale with overshoot)
         */
        val POP = ElementAnimation(
            duration = 400,
            easing = EasingType.EASE_OUT_BACK,
            startValue = 0f,
            endValue = 1f
        )
        
        /**
         * Pulse animation (repeating scale)
         */
        val PULSE = ElementAnimation(
            duration = 1000,
            easing = EasingType.EASE_IN_OUT,
            repeat = -1,
            reverseOnRepeat = true,
            startValue = 0.95f,
            endValue = 1.05f
        )
        
        /**
         * Create animation for specific element type
         */
        fun forElementType(type: ElementType): ElementAnimation {
            return when (type) {
                ElementType.BUTTON -> FADE_QUICK
                ElementType.DIALOG -> FADE_STANDARD.withScale(0.95f, 1f)
                ElementType.CARD -> SLIDE_IN
                ElementType.MENU -> FADE_QUICK.withScale(0.9f, 1f)
                ElementType.SNACKBAR -> SLIDE_IN.copy(delay = 100)
                else -> FADE_STANDARD
            }
        }
    }
}
