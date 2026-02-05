/**
 * VoiceAvanue - Unified Voice Control + Browser Module
 *
 * Entry point for the combined VoiceOSCore and WebAvanue functionality.
 * Provides unified voice command processing, browser control, and RPC communication.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-05
 */

package com.augmentalis.voiceavanue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entry point for VoiceAvanue functionality
 */
object VoiceAvanue {
    const val VERSION = "1.0.0-alpha"
    const val MODULE_NAME = "VoiceAvanue"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initialize the VoiceAvanue module
     */
    fun initialize(config: VoiceAvanueConfig = VoiceAvanueConfig.Default) {
        if (_isInitialized.value) return

        // Initialize subsystems
        CommandSystem.initialize(config.commandConfig)
        BrowserSystem.initialize(config.browserConfig)
        RpcSystem.initialize(config.rpcConfig)

        _isInitialized.value = true
    }

    /**
     * Shutdown and cleanup resources
     */
    fun shutdown() {
        if (!_isInitialized.value) return

        RpcSystem.shutdown()
        BrowserSystem.shutdown()
        CommandSystem.shutdown()

        _isInitialized.value = false
    }
}

/**
 * Configuration for VoiceAvanue module
 */
data class VoiceAvanueConfig(
    val commandConfig: CommandConfig = CommandConfig.Default,
    val browserConfig: BrowserConfig = BrowserConfig.Default,
    val rpcConfig: RpcConfig = RpcConfig.Default,
    val enableLogging: Boolean = true
) {
    companion object {
        val Default = VoiceAvanueConfig()
    }
}

/**
 * Command system configuration
 */
data class CommandConfig(
    val enableVoiceCommands: Boolean = true,
    val enableTextCommands: Boolean = true,
    val commandTimeout: Long = 5000L,
    val enableFuzzyMatching: Boolean = true
) {
    companion object {
        val Default = CommandConfig()
    }
}

/**
 * Browser system configuration
 */
data class BrowserConfig(
    val enableJavaScript: Boolean = true,
    val enableDesktopMode: Boolean = false,
    val enableAdBlocking: Boolean = true,
    val startPage: String = "about:blank"
) {
    companion object {
        val Default = BrowserConfig()
    }
}

/**
 * RPC system configuration
 */
data class RpcConfig(
    val enableJsonRpc: Boolean = true,
    val enableAvuProtocol: Boolean = true,
    val rpcPort: Int = 8765,
    val enableWebSocket: Boolean = true
) {
    companion object {
        val Default = RpcConfig()
    }
}

/**
 * Command system stub - to be populated from VoiceOSCore
 */
internal object CommandSystem {
    fun initialize(config: CommandConfig) {
        // TODO: Migrate from VoiceOSCore
    }

    fun shutdown() {
        // TODO: Cleanup
    }
}

/**
 * Browser system stub - to be populated from WebAvanue
 */
internal object BrowserSystem {
    fun initialize(config: BrowserConfig) {
        // TODO: Migrate from WebAvanue
    }

    fun shutdown() {
        // TODO: Cleanup
    }
}

/**
 * RPC system stub - combines RPC from both modules
 */
internal object RpcSystem {
    fun initialize(config: RpcConfig) {
        // TODO: Migrate from VoiceOSCore and WebAvanue RPC
    }

    fun shutdown() {
        // TODO: Cleanup
    }
}
