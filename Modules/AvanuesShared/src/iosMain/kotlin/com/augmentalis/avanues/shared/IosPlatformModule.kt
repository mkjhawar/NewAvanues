package com.augmentalis.avanues.shared

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS platform-specific Koin DI bindings.
 *
 * Provides:
 * - SQLDelight NativeSqliteDriver for on-device persistence
 * - Platform services (speech, keychain, etc.) as they are implemented
 *
 * Called from Swift at app startup via KoinHelper.
 */
val iosPlatformModule: Module = module {
    // SQLDelight driver for iOS (NativeSqliteDriver)
    // Individual database drivers will be registered per-database
    // as VoiceOSCore and WebAvanue database schemas are separate.
}
