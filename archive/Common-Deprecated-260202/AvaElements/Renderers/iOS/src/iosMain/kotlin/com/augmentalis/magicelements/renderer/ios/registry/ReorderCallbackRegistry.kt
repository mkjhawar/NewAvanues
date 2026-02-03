package com.augmentalis.magicelements.renderer.ios.registry

interface ReorderCallbackRegistry {
    fun registerReorderCallback(id: String, callback: (Int, Int) -> Unit)
    fun invokeReorder(id: String, oldIndex: Int, newIndex: Int)
    fun hasCallback(id: String): Boolean
    fun clear()
}

class DefaultReorderCallbackRegistry : ReorderCallbackRegistry {
    private val callbacks = mutableMapOf<String, (Int, Int) -> Unit>()

    override fun registerReorderCallback(id: String, callback: (Int, Int) -> Unit) {
        callbacks[id] = callback
    }

    override fun invokeReorder(id: String, oldIndex: Int, newIndex: Int) {
        callbacks[id]?.invoke(oldIndex, newIndex)
    }

    override fun hasCallback(id: String): Boolean {
        return callbacks.containsKey(id)
    }

    override fun clear() {
        callbacks.clear()
    }
}

object ReorderCallbackRegistryHolder {
    private var registry: ReorderCallbackRegistry = DefaultReorderCallbackRegistry()

    fun getRegistry(): ReorderCallbackRegistry = registry

    fun setRegistry(newRegistry: ReorderCallbackRegistry) {
        registry = newRegistry
    }

    fun reset() {
        registry = DefaultReorderCallbackRegistry()
    }
}
