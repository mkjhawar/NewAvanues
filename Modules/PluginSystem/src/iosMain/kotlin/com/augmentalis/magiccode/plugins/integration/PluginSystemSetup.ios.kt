/**
 * PluginSystemSetup.ios.kt - iOS implementation of plugin system setup
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * iOS-specific plugin system integration.
 * Currently a stub - to be implemented when iOS support is added.
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS implementation of IPluginSystemSetup.
 *
 * TODO: Implement iOS-specific plugin host and executors.
 */
class IosPluginSystemSetup : IPluginSystemSetup {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    override val pluginHost: IPluginHost<*>? = null
    override val commandDispatcher: ICommandDispatcher? = null
    override val performanceMonitor: PluginPerformanceMonitor? = null

    override suspend fun initialize(config: PluginSystemConfig): PluginSystemSetupResult {
        // TODO: Implement iOS plugin system initialization
        return PluginSystemSetupResult(
            success = false,
            message = "iOS plugin system not yet implemented"
        )
    }

    override suspend fun shutdown() {
        _isInitialized.value = false
    }

    override fun registerPluginFactory(pluginId: String, factory: () -> Plugin) {
        // TODO: Implement
    }

    override fun getPlugin(pluginId: String): Plugin? = null

    override fun getLoadedPlugins(): List<Plugin> = emptyList()
}

/**
 * Factory implementation for iOS.
 */
actual object PluginSystemSetup {
    actual fun create(platformContext: Any): IPluginSystemSetup {
        return IosPluginSystemSetup()
    }
}
