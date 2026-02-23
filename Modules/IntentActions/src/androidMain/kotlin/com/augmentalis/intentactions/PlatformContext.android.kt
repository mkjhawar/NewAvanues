package com.augmentalis.intentactions

import android.content.Context

/**
 * Android platform context wrapping [Context].
 *
 * Wrapper class satisfying Kotlin 2.1 expect/actual rules
 * (actual typealias for expect class is no longer permitted).
 */
actual class PlatformContext(val android: Context)
