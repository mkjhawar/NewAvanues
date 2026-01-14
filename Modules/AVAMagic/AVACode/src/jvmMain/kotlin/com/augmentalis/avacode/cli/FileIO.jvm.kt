package com.augmentalis.avacode.cli

import java.io.File

/**
 * FileIO - JVM implementation using java.io.File
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

actual object FileIO {

    actual fun readFile(path: String): String {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $path")
        }
        if (!file.isFile) {
            throw IllegalArgumentException("Path is not a file: $path")
        }
        return file.readText()
    }

    actual fun writeFile(path: String, content: String) {
        val file = File(path)

        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()

        file.writeText(content)
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun createDirectory(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    actual fun listFiles(path: String): List<String> {
        val dir = File(path)
        if (!dir.exists()) {
            throw IllegalArgumentException("Directory not found: $path")
        }
        if (!dir.isDirectory) {
            throw IllegalArgumentException("Path is not a directory: $path")
        }
        return dir.listFiles()?.map { it.name } ?: emptyList()
    }

    actual fun getFileExtension(path: String): String {
        val file = File(path)
        return file.extension
    }

    actual fun getAbsolutePath(path: String): String {
        return File(path).absolutePath
    }
}
