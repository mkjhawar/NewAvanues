package com.augmentalis.httpavanue.platform

actual fun readResource(path: String): String? = try {
    val classLoader = Thread.currentThread().contextClassLoader ?: object {}.javaClass.classLoader
    classLoader.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
} catch (_: Exception) { null }
