package com.augmentalis.universal.thememanager

import java.io.File

internal actual fun themeWriteFile(path: String, content: String) {
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    } catch (e: Exception) {
        println("Error writing file $path: ${e.message}")
    }
}

internal actual fun themeReadFile(path: String): String? {
    return try {
        val file = File(path)
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        println("Error reading file $path: ${e.message}")
        null
    }
}

internal actual fun themeDeleteFile(path: String) {
    try {
        val file = File(path)
        if (file.exists()) file.delete()
    } catch (e: Exception) {
        println("Error deleting file $path: ${e.message}")
    }
}

internal actual fun themeListFiles(dirPath: String): List<String> {
    return try {
        val dir = File(dirPath)
        if (!dir.exists()) return emptyList()
        dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".override.json") }
            ?.map { it.name }
            ?: emptyList()
    } catch (e: Exception) {
        println("Error listing files in $dirPath: ${e.message}")
        emptyList()
    }
}
