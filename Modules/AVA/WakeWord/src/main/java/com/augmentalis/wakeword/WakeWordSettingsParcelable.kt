// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/WakeWordSettingsParcelable.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Parcelable version of WakeWordSettings for Intent extras
 *
 * Note: The main WakeWordSettings data class needs @Parcelize annotation
 * to be passed via Intent to WakeWordService.
 */

// Extension to make WakeWordSettings Parcelable
// Add this annotation to WakeWordSettings class in WakeWordModels.kt:
// @Parcelize
// data class WakeWordSettings(...) : Parcelable
