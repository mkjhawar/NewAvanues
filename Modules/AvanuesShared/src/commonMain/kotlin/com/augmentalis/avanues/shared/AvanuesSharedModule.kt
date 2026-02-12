package com.augmentalis.avanues.shared

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin DI module for the shared Avanues dependencies.
 *
 * This module provides cross-platform dependencies used by both
 * Android and iOS apps. Platform-specific bindings (e.g., SQLDelight
 * driver, speech engine) are provided by platform modules.
 */
val avanuesSharedModule: Module = module {
    // Platform-agnostic bindings are registered here.
    // Platform-specific bindings (SqlDriver, SpeechEngine, etc.)
    // are provided by iosPlatformModule or androidPlatformModule.
}
