package com.augmentalis.avacode.plugins

import java.io.File
import java.net.URLClassLoader

/**
 * JVM implementation of PluginClassLoader using URLClassLoader.
 *
 * Loads plugin code from JAR files.
 */
actual class PluginClassLoader {
    private var classLoader: URLClassLoader? = null

    actual fun loadClass(className: String, pluginPath: String): Any {
        if (classLoader == null) {
            val jarFile = File(pluginPath)
            if (!jarFile.exists()) {
                throw IllegalArgumentException("Plugin JAR not found: $pluginPath")
            }

            classLoader = URLClassLoader(
                arrayOf(jarFile.toURI().toURL()),
                this::class.java.classLoader
            )
        }

        val clazz = classLoader?.loadClass(className)
            ?: throw ClassNotFoundException("Class not found: $className")

        return clazz.getDeclaredConstructor().newInstance()
    }

    actual fun unload() {
        classLoader?.close()
        classLoader = null
    }
}
