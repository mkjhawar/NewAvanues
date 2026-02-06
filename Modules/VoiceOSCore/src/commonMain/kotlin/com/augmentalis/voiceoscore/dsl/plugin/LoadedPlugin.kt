package com.augmentalis.voiceoscore.dsl.plugin

import com.augmentalis.voiceoscore.dsl.ast.AvuDslFile
import com.augmentalis.voiceoscore.dsl.interpreter.SandboxConfig

/**
 * A fully loaded plugin combining manifest, parsed AST, and runtime config.
 *
 * Immutable — state transitions produce new instances via [withState].
 */
data class LoadedPlugin(
    val manifest: PluginManifest,
    val ast: AvuDslFile,
    val sandboxConfig: SandboxConfig,
    val state: PluginState = PluginState.VALIDATED,
    val errorMessage: String? = null,
    val loadedAtMs: Long = 0
) {
    val pluginId: String get() = manifest.pluginId
    val name: String get() = manifest.name
    val isActive: Boolean get() = state.isUsable

    fun withState(newState: PluginState, error: String? = null): LoadedPlugin =
        copy(state = newState, errorMessage = error)
}
