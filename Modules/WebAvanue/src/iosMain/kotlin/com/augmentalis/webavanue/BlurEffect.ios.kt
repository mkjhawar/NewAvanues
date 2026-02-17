package com.augmentalis.webavanue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * iOS blur support check
 *
 * iOS supports blur via UIVisualEffectView
 */
actual fun supportsBlur(): Boolean {
    return true
}

/**
 * iOS glassmorphism modifier
 *
 * Note: Proper implementation requires UIVisualEffectView integration
 * For now, returns modifier unchanged (blur would need native iOS view)
 */
actual fun Modifier.glassmorphism(
    blurRadius: Float,
    backgroundColor: Color,
    alpha: Float,
    borderColor: Color,
    borderWidth: Float
): Modifier {
    // iOS blur requires UIVisualEffectView which can't be applied to Compose modifier
    // Would need to wrap content in UIKitView with UIVisualEffectView
    return this
}
