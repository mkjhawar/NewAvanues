package com.augmentalis.voiceoscore

/**
 * Speed levels for cursor movement in VoiceOS cursor system.
 *
 * Each level corresponds to a different movement speed:
 * - SLOW: Half speed (0.5x) for precise positioning
 * - NORMAL: Default speed (1.0x) for regular use
 * - FAST: Double speed (2.0x) for quick navigation
 * - VERY_FAST: Quadruple speed (4.0x) for rapid traversal
 */
enum class SpeedLevel {
    SLOW,
    NORMAL,
    FAST,
    VERY_FAST
}

/**
 * Controls cursor movement speed for the VoiceOS cursor system.
 *
 * SpeedController manages different speed levels and provides methods to:
 * - Get/set the current speed level
 * - Calculate the multiplier for the current level
 * - Calculate pixels per step based on speed
 * - Increase/decrease speed through levels
 * - Reset to default (NORMAL) speed
 *
 * @param initialLevel The initial speed level (defaults to NORMAL)
 */
class SpeedController(
    initialLevel: SpeedLevel = SpeedLevel.NORMAL
) {
    @Volatile
    private var currentLevel: SpeedLevel = initialLevel

    companion object {
        /**
         * Mapping of speed levels to their corresponding multipliers.
         */
        private val SPEED_MULTIPLIERS = mapOf(
            SpeedLevel.SLOW to 0.5f,
            SpeedLevel.NORMAL to 1.0f,
            SpeedLevel.FAST to 2.0f,
            SpeedLevel.VERY_FAST to 4.0f
        )

        /**
         * Base speed in pixels per step.
         * This value is multiplied by the speed level multiplier.
         */
        const val BASE_SPEED = 10  // pixels per step
    }

    /**
     * Returns the current speed level.
     */
    fun getLevel(): SpeedLevel = currentLevel

    /**
     * Sets the current speed level.
     *
     * @param level The new speed level to set
     */
    fun setLevel(level: SpeedLevel) {
        currentLevel = level
    }

    /**
     * Returns the speed multiplier for the current level.
     *
     * @return The multiplier value (0.5f, 1.0f, 2.0f, or 4.0f)
     */
    fun getMultiplier(): Float {
        return SPEED_MULTIPLIERS[currentLevel] ?: 1.0f
    }

    /**
     * Calculates and returns the pixels per step based on current speed level.
     *
     * @return The number of pixels to move per step (5, 10, 20, or 40)
     */
    fun getPixelsPerStep(): Int {
        return (BASE_SPEED * getMultiplier()).toInt()
    }

    /**
     * Increases the speed to the next level.
     * If already at VERY_FAST, the level remains unchanged.
     *
     * @return The new speed level after increase
     */
    fun increaseSpeed(): SpeedLevel {
        currentLevel = when (currentLevel) {
            SpeedLevel.SLOW -> SpeedLevel.NORMAL
            SpeedLevel.NORMAL -> SpeedLevel.FAST
            SpeedLevel.FAST -> SpeedLevel.VERY_FAST
            SpeedLevel.VERY_FAST -> SpeedLevel.VERY_FAST
        }
        return currentLevel
    }

    /**
     * Decreases the speed to the previous level.
     * If already at SLOW, the level remains unchanged.
     *
     * @return The new speed level after decrease
     */
    fun decreaseSpeed(): SpeedLevel {
        currentLevel = when (currentLevel) {
            SpeedLevel.VERY_FAST -> SpeedLevel.FAST
            SpeedLevel.FAST -> SpeedLevel.NORMAL
            SpeedLevel.NORMAL -> SpeedLevel.SLOW
            SpeedLevel.SLOW -> SpeedLevel.SLOW
        }
        return currentLevel
    }

    /**
     * Resets the speed level to NORMAL (default).
     */
    fun reset() {
        currentLevel = SpeedLevel.NORMAL
    }
}
