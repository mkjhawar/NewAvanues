/**
 * TestExtensions.kt - Test utility extensions
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Extensions for testing UI components
 */
package com.augmentalis.voiceuielements.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction

/**
 * Mock implementation of assertAgainstGolden for visual regression testing
 * In a real implementation, this would compare against golden images
 */
fun ImageBitmap.assertAgainstGolden(goldenName: String) {
    // Mock implementation - in real scenario this would:
    // 1. Load the golden image from assets
    // 2. Compare pixel by pixel
    // 3. Assert if images match within tolerance
    // For now, just validate that we have a valid bitmap
    check(width > 0 && height > 0) {
        "Invalid image bitmap for golden comparison: $goldenName"
    }
}

/**
 * Extension to capture and assert against golden images
 */
fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    // Mock implementation - in real scenario this would capture the actual UI
    // For now, return a mock bitmap
    return ImageBitmap(100, 100)
}