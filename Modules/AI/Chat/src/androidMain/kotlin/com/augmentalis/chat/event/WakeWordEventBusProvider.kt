package com.augmentalis.chat.event

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable wrapper for WakeWordEventBus.
 * Provides DI integration for Android platform.
 *
 * This wrapper allows the KMP-compatible WakeWordEventBus to be injected
 * via Hilt in Android-specific code while keeping the core event bus
 * platform-agnostic.
 *
 * Usage:
 * - Inject WakeWordEventBusProvider in Android components
 * - Access the event bus via provider.eventBus
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
@Singleton
class WakeWordEventBusProvider @Inject constructor() {
    val eventBus = WakeWordEventBus()
}
