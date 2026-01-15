/**
 * CursorAnimator.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/CursorAnimator.kt
 * 
 * Created: 2025-01-26 02:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Handles smooth cursor animations and transitions
 * Module: VoiceCursor System
 * 
 * Features:
 * - Smooth position transitions using ValueAnimator
 * - Cursor state change animations (hover, click, drag)
 * - Pulse animations for visual feedback
 * - Visibility fade in/out animations
 * - Customizable easing and duration
 */

package com.augmentalis.voiceos.cursor.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import kotlinx.coroutines.*

/**
 * Animation types for different cursor behaviors
 */
enum class AnimationType {
    POSITION_SMOOTH,    // Smooth movement animation
    POSITION_INSTANT,   // No animation, instant movement
    CLICK_PULSE,        // Click feedback pulse
    HOVER_GLOW,         // Hover state glow
    DRAG_FEEDBACK,      // Drag visual feedback
    VISIBILITY_FADE,    // Fade in/out
    STATE_TRANSITION    // General state transitions
}

/**
 * Animation configuration for different states
 */
data class AnimationConfig(
    val duration: Long = 200L,
    val interpolator: android.view.animation.Interpolator = AccelerateDecelerateInterpolator(),
    val repeatCount: Int = 0,
    val repeatMode: Int = ValueAnimator.RESTART,
    val startDelay: Long = 0L,
    val scaleFrom: Float = 1.0f,
    val scaleTo: Float = 1.0f,
    val alphaFrom: Float = 1.0f,
    val alphaTo: Float = 1.0f
) {
    companion object {
        val SMOOTH_MOVEMENT = AnimationConfig(
            duration = 150L,
            interpolator = DecelerateInterpolator(1.5f)
        )
        
        val CLICK_PULSE = AnimationConfig(
            duration = 120L,
            interpolator = OvershootInterpolator(1.2f),
            scaleFrom = 1.0f,
            scaleTo = 1.3f,
            repeatCount = 1,
            repeatMode = ValueAnimator.REVERSE
        )
        
        val HOVER_GLOW = AnimationConfig(
            duration = 300L,
            interpolator = AccelerateDecelerateInterpolator(),
            scaleFrom = 1.0f,
            scaleTo = 1.1f,
            alphaFrom = 1.0f,
            alphaTo = 0.8f,
            repeatCount = ValueAnimator.INFINITE,
            repeatMode = ValueAnimator.REVERSE
        )
        
        val DRAG_FEEDBACK = AnimationConfig(
            duration = 100L,
            interpolator = AccelerateInterpolator(),
            scaleFrom = 1.0f,
            scaleTo = 0.9f,
            alphaFrom = 1.0f,
            alphaTo = 0.7f
        )
        
        val FADE_IN = AnimationConfig(
            duration = 250L,
            interpolator = DecelerateInterpolator(),
            alphaFrom = 0.0f,
            alphaTo = 1.0f
        )
        
        val FADE_OUT = AnimationConfig(
            duration = 200L,
            interpolator = AccelerateInterpolator(),
            alphaFrom = 1.0f,
            alphaTo = 0.0f
        )
    }
}

/**
 * Animation state for cursor
 */
data class AnimationState(
    val currentPosition: CursorOffset = CursorOffset(0f, 0f),
    val targetPosition: CursorOffset = CursorOffset(0f, 0f),
    val scale: Float = 1.0f,
    val alpha: Float = 1.0f,
    val isAnimating: Boolean = false,
    val animationType: AnimationType = AnimationType.POSITION_SMOOTH
)

/**
 * Main cursor animator class
 */
class CursorAnimator {
    
    companion object {
        private const val TAG = "CursorAnimator"
        private const val DEFAULT_SMOOTH_DURATION = 150L
        private const val MIN_ANIMATION_DISTANCE = 5.0f // Minimum pixels to animate
    }
    
    // Animation scope for coroutines
    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Current animation state
    @Volatile
    private var animationState = AnimationState()
    
    // Active animators
    private val activeAnimators = mutableSetOf<ValueAnimator>()
    private var positionAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null
    private var alphaAnimator: ValueAnimator? = null
    
    // Animation listeners
    var onPositionUpdate: ((CursorOffset) -> Unit)? = null
    var onScaleUpdate: ((Float) -> Unit)? = null
    var onAlphaUpdate: ((Float) -> Unit)? = null
    var onAnimationComplete: ((AnimationType) -> Unit)? = null
    var onAnimationStart: ((AnimationType) -> Unit)? = null
    
    // Settings
    private var _isEnabled = false
    val isEnabled: Boolean get() = _isEnabled
    var smoothMovementEnabled = true
    var movementThreshold = MIN_ANIMATION_DISTANCE
    
    /**
     * Animate cursor to new position with smooth transition
     */
    fun animateToPosition(
        targetPosition: CursorOffset,
        config: AnimationConfig = AnimationConfig.SMOOTH_MOVEMENT,
        force: Boolean = false
    ) {
        if (!_isEnabled && !force) return
        
        val currentPos = animationState.currentPosition
        val distance = calculateDistance(currentPos, targetPosition)
        
        // Skip animation for very small movements unless forced
        if (!force && distance < movementThreshold) {
            updatePosition(targetPosition, instant = true)
            return
        }
        
        // Cancel existing position animation
        positionAnimator?.cancel()
        
        // Don't animate if smoothMovementEnabled is false unless forced
        if (!smoothMovementEnabled && !force) {
            updatePosition(targetPosition, instant = true)
            return
        }
        
        animationState = animationState.copy(
            targetPosition = targetPosition,
            isAnimating = true,
            animationType = AnimationType.POSITION_SMOOTH
        )
        
        onAnimationStart?.invoke(AnimationType.POSITION_SMOOTH)
        
        positionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.duration
            interpolator = config.interpolator
            startDelay = config.startDelay
            
            val startX = currentPos.x
            val startY = currentPos.y
            val deltaX = targetPosition.x - startX
            val deltaY = targetPosition.y - startY
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val newX = startX + deltaX * progress
                val newY = startY + deltaY * progress
                val newPosition = CursorOffset(newX, newY)
                
                updatePosition(newPosition, instant = true)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    positionAnimator = null
                    animationState = animationState.copy(
                        currentPosition = targetPosition,
                        isAnimating = false
                    )
                    onAnimationComplete?.invoke(AnimationType.POSITION_SMOOTH)
                    activeAnimators.remove(this@apply)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    positionAnimator = null
                    animationState = animationState.copy(isAnimating = false)
                    activeAnimators.remove(this@apply)
                }
            })
            
            activeAnimators.add(this)
            start()
        }
    }
    
    /**
     * Animate click pulse effect
     */
    fun animateClickPulse(config: AnimationConfig = AnimationConfig.CLICK_PULSE) {
        if (!_isEnabled) return
        
        onAnimationStart?.invoke(AnimationType.CLICK_PULSE)
        
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(config.scaleFrom, config.scaleTo).apply {
            duration = config.duration
            interpolator = config.interpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                animationState = animationState.copy(scale = scale)
                onScaleUpdate?.invoke(scale)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleAnimator = null
                    animationState = animationState.copy(scale = 1.0f)
                    onScaleUpdate?.invoke(1.0f)
                    onAnimationComplete?.invoke(AnimationType.CLICK_PULSE)
                    activeAnimators.remove(this@apply)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    scaleAnimator = null
                    animationState = animationState.copy(scale = 1.0f)
                    onScaleUpdate?.invoke(1.0f)
                    activeAnimators.remove(this@apply)
                }
            })
            
            activeAnimators.add(this)
            start()
        }
    }
    
    /**
     * Animate hover glow effect
     */
    fun animateHoverGlow(enable: Boolean, config: AnimationConfig = AnimationConfig.HOVER_GLOW) {
        if (!_isEnabled) return
        
        if (enable) {
            onAnimationStart?.invoke(AnimationType.HOVER_GLOW)
            
            val animatorSet = AnimatorSet()
            
            // Scale animator
            val scaleAnim = ValueAnimator.ofFloat(config.scaleFrom, config.scaleTo).apply {
                duration = config.duration
                interpolator = config.interpolator
                repeatCount = config.repeatCount
                repeatMode = config.repeatMode
                
                addUpdateListener { animator ->
                    val scale = animator.animatedValue as Float
                    animationState = animationState.copy(scale = scale)
                    onScaleUpdate?.invoke(scale)
                }
            }
            
            // Alpha animator
            val alphaAnim = ValueAnimator.ofFloat(config.alphaFrom, config.alphaTo).apply {
                duration = config.duration
                interpolator = config.interpolator
                repeatCount = config.repeatCount
                repeatMode = config.repeatMode
                
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Float
                    animationState = animationState.copy(alpha = alpha)
                    onAlphaUpdate?.invoke(alpha)
                }
            }
            
            animatorSet.playTogether(scaleAnim, alphaAnim)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationComplete?.invoke(AnimationType.HOVER_GLOW)
                    activeAnimators.removeAll(setOf(scaleAnim, alphaAnim))
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    activeAnimators.removeAll(setOf(scaleAnim, alphaAnim))
                }
            })
            
            activeAnimators.addAll(setOf(scaleAnim, alphaAnim))
            animatorSet.start()
        } else {
            // Stop hover animation and return to normal
            animateToNormalState()
        }
    }
    
    /**
     * Animate drag feedback
     */
    fun animateDragFeedback(
        isDragging: Boolean,
        config: AnimationConfig = AnimationConfig.DRAG_FEEDBACK
    ) {
        if (!_isEnabled) return
        
        onAnimationStart?.invoke(AnimationType.DRAG_FEEDBACK)
        
        val targetScale = if (isDragging) config.scaleTo else 1.0f
        val targetAlpha = if (isDragging) config.alphaTo else 1.0f
        
        // Cancel existing animations
        scaleAnimator?.cancel()
        alphaAnimator?.cancel()
        
        // Scale animation
        scaleAnimator = ValueAnimator.ofFloat(animationState.scale, targetScale).apply {
            duration = config.duration
            interpolator = config.interpolator
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                animationState = animationState.copy(scale = scale)
                onScaleUpdate?.invoke(scale)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleAnimator = null
                    onAnimationComplete?.invoke(AnimationType.DRAG_FEEDBACK)
                    activeAnimators.remove(this@apply)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    scaleAnimator = null
                    activeAnimators.remove(this@apply)
                }
            })
            
            activeAnimators.add(this)
            start()
        }
        
        // Alpha animation
        alphaAnimator = ValueAnimator.ofFloat(animationState.alpha, targetAlpha).apply {
            duration = config.duration
            interpolator = config.interpolator
            
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                animationState = animationState.copy(alpha = alpha)
                onAlphaUpdate?.invoke(alpha)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alphaAnimator = null
                    activeAnimators.remove(this@apply)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    alphaAnimator = null
                    activeAnimators.remove(this@apply)
                }
            })
            
            activeAnimators.add(this)
            start()
        }
    }
    
    /**
     * Animate visibility fade
     */
    fun animateVisibility(
        visible: Boolean,
        config: AnimationConfig = if (visible) AnimationConfig.FADE_IN else AnimationConfig.FADE_OUT
    ) {
        if (!_isEnabled) return
        
        onAnimationStart?.invoke(AnimationType.VISIBILITY_FADE)
        
        alphaAnimator?.cancel()
        alphaAnimator = ValueAnimator.ofFloat(config.alphaFrom, config.alphaTo).apply {
            duration = config.duration
            interpolator = config.interpolator
            
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                animationState = animationState.copy(alpha = alpha)
                onAlphaUpdate?.invoke(alpha)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alphaAnimator = null
                    onAnimationComplete?.invoke(AnimationType.VISIBILITY_FADE)
                    activeAnimators.remove(this@apply)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    alphaAnimator = null
                    activeAnimators.remove(this@apply)
                }
            })
            
            activeAnimators.add(this)
            start()
        }
    }
    
    /**
     * Return cursor to normal state (scale=1.0, alpha=1.0)
     */
    fun animateToNormalState(duration: Long = 200L) {
        if (!_isEnabled) return
        
        val needsScaleAnimation = animationState.scale != 1.0f
        val needsAlphaAnimation = animationState.alpha != 1.0f
        
        if (!needsScaleAnimation && !needsAlphaAnimation) return
        
        onAnimationStart?.invoke(AnimationType.STATE_TRANSITION)
        
        val animators = mutableListOf<ValueAnimator>()
        
        if (needsScaleAnimation) {
            scaleAnimator?.cancel()
            scaleAnimator = ValueAnimator.ofFloat(animationState.scale, 1.0f).apply {
                this.duration = duration
                interpolator = DecelerateInterpolator()
                
                addUpdateListener { animator ->
                    val scale = animator.animatedValue as Float
                    animationState = animationState.copy(scale = scale)
                    onScaleUpdate?.invoke(scale)
                }
                
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        scaleAnimator = null
                        activeAnimators.remove(this@apply)
                    }
                    
                    override fun onAnimationCancel(animation: Animator) {
                        scaleAnimator = null
                        activeAnimators.remove(this@apply)
                    }
                })
                
                activeAnimators.add(this)
                animators.add(this)
            }
        }
        
        if (needsAlphaAnimation) {
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(animationState.alpha, 1.0f).apply {
                this.duration = duration
                interpolator = DecelerateInterpolator()
                
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Float
                    animationState = animationState.copy(alpha = alpha)
                    onAlphaUpdate?.invoke(alpha)
                }
                
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        alphaAnimator = null
                        activeAnimators.remove(this@apply)
                    }
                    
                    override fun onAnimationCancel(animation: Animator) {
                        alphaAnimator = null
                        activeAnimators.remove(this@apply)
                    }
                })
                
                activeAnimators.add(this)
                animators.add(this)
            }
        }
        
        // Start all animations
        animators.forEach { it.start() }
        
        // Set completion callback for the last animator
        animators.lastOrNull()?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationComplete?.invoke(AnimationType.STATE_TRANSITION)
            }
        })
    }
    
    /**
     * Update position directly without animation
     */
    private fun updatePosition(position: CursorOffset, instant: Boolean = false) {
        animationState = animationState.copy(currentPosition = position)
        
        if (instant) {
            onPositionUpdate?.invoke(position)
        }
    }
    
    /**
     * Calculate distance between two positions
     */
    private fun calculateDistance(pos1: CursorOffset, pos2: CursorOffset): Float {
        val dx = pos2.x - pos1.x
        val dy = pos2.y - pos1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Cancel all active animations
     */
    fun cancelAllAnimations() {
        try {
            activeAnimators.forEach { it.cancel() }
            activeAnimators.clear()
            
            positionAnimator = null
            scaleAnimator = null
            alphaAnimator = null
            
            animationState = animationState.copy(isAnimating = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling animations", e)
        }
    }
    
    /**
     * Get current animation state
     */
    fun getCurrentState(): AnimationState = animationState
    
    /**
     * Check if any animation is currently running
     */
    fun isAnimating(): Boolean = animationState.isAnimating || activeAnimators.isNotEmpty()
    
    /**
     * Set current position without animation
     */
    fun setCurrentPosition(position: CursorOffset) {
        animationState = animationState.copy(
            currentPosition = position,
            targetPosition = position
        )
    }
    
    /**
     * Set animation enabled state
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled
        if (!enabled) {
            cancelAllAnimations()
        }
    }
    
    /**
     * Dispose of all resources
     */
    fun dispose() {
        try {
            cancelAllAnimations()
            animationScope.cancel()
            
            // Clear all listeners
            onPositionUpdate = null
            onScaleUpdate = null
            onAlphaUpdate = null
            onAnimationComplete = null
            onAnimationStart = null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing CursorAnimator", e)
        }
    }
}