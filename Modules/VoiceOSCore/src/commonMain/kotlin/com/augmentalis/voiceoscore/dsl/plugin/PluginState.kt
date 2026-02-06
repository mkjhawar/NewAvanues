package com.augmentalis.voiceoscore.dsl.plugin

/**
 * Lifecycle states for AVU DSL plugins.
 *
 * ```
 * DISCOVERED → VALIDATED → REGISTERED → ACTIVE ⇄ INACTIVE
 *                                ↓                   ↓
 *                              ERROR               ERROR
 * ```
 */
enum class PluginState(val description: String) {
    DISCOVERED("Plugin file found but not yet parsed"),
    VALIDATED("Parsed and validated, ready for registration"),
    REGISTERED("Triggers registered, awaiting activation"),
    ACTIVE("Plugin is active and can handle triggers"),
    INACTIVE("Plugin is temporarily disabled"),
    ERROR("Plugin encountered an error");

    val isUsable: Boolean get() = this == ACTIVE
    val isRegistered: Boolean get() = this in setOf(REGISTERED, ACTIVE, INACTIVE)
}
