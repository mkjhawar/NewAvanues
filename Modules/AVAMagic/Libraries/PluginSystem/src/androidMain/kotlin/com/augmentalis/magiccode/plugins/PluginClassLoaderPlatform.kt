package com.augmentalis.avacode.plugins

import dalvik.system.DexClassLoader
import java.io.File

/**
 * Android implementation of PluginClassLoader using DexClassLoader.
 *
 * Loads plugin code from APK or JAR files containing DEX bytecode.
 */
actual class PluginClassLoader {
    private var classLoader: DexClassLoader? = null

    actual fun loadClass(className: String, pluginPath: String): Any {
        if (classLoader == null) {
            // Create optimized dex output directory
            val optimizedDir = File(pluginPath).parentFile?.resolve("dex_opt")
                ?: throw IllegalStateException("Cannot create optimized dex directory")

            if (!optimizedDir.exists()) {
                optimizedDir.mkdirs()
            }

            // Create DexClassLoader for plugin
            classLoader = DexClassLoader(
                pluginPath,
                optimizedDir.absolutePath,
                null,
                this::class.java.classLoader
            )
        }

        val clazz = classLoader?.loadClass(className)
            ?: throw ClassNotFoundException("Class not found: $className")

        return clazz.getDeclaredConstructor().newInstance()
    }

    actual fun unload() {
        classLoader = null
        // Note: DexClassLoader doesn't have explicit close, will be GC'd
    }
}
