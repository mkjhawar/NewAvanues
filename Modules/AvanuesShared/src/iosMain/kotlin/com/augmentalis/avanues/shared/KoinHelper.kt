package com.augmentalis.avanues.shared

import org.koin.core.context.startKoin

/**
 * Helper object called from Swift to initialize Koin DI.
 *
 * Usage from Swift (via SKIE):
 * ```swift
 * KoinHelper.shared.startKoin()
 * ```
 */
object KoinHelper {
    fun doStartKoin() {
        startKoin {
            modules(
                avanuesSharedModule,
                iosPlatformModule
            )
        }
    }
}
