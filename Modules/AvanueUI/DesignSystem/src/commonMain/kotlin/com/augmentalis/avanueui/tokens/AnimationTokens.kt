package com.augmentalis.avanueui.tokens

/**
 * Animation duration constants (ms). Static and universal across all themes.
 */
object AnimationTokens {
    const val fast: Int = 100
    const val normal: Int = 200
    const val medium: Int = 300
    const val slow: Int = 500
    const val extraSlow: Int = 1000

    fun resolve(id: String): Int? = when (id) {
        "animation.fast" -> fast
        "animation.normal" -> normal
        "animation.medium" -> medium
        "animation.slow" -> slow
        "animation.extraSlow" -> extraSlow
        else -> null
    }
}
