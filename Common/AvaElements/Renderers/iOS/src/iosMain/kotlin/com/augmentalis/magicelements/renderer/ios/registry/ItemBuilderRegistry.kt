package com.augmentalis.magicelements.renderer.ios.registry

import com.augmentalis.magicelements.core.Component

interface ItemBuilderRegistry {
    fun registerBuilder(id: String, builder: (Int) -> Component)
    fun registerSeparator(id: String, separator: (Int) -> Component)
    fun resolveBuilder(id: String, index: Int): Component?
    fun resolveSeparator(id: String, index: Int): Component?
    fun clear()
    fun hasBuilder(id: String): Boolean
}

class DefaultItemBuilderRegistry : ItemBuilderRegistry {
    private val builders = mutableMapOf<String, (Int) -> Component>()
    private val separators = mutableMapOf<String, (Int) -> Component>()

    override fun registerBuilder(id: String, builder: (Int) -> Component) {
        builders[id] = builder
    }

    override fun registerSeparator(id: String, separator: (Int) -> Component) {
        separators[id] = separator
    }

    override fun resolveBuilder(id: String, index: Int): Component? {
        return builders[id]?.invoke(index)
    }

    override fun resolveSeparator(id: String, index: Int): Component? {
        return separators[id]?.invoke(index)
    }

    override fun clear() {
        builders.clear()
        separators.clear()
    }

    override fun hasBuilder(id: String): Boolean {
        return builders.containsKey(id)
    }
}

object ItemBuilderRegistryHolder {
    private var registry: ItemBuilderRegistry = DefaultItemBuilderRegistry()

    fun getRegistry(): ItemBuilderRegistry = registry

    fun setRegistry(newRegistry: ItemBuilderRegistry) {
        registry = newRegistry
    }

    fun reset() {
        registry = DefaultItemBuilderRegistry()
    }
}
