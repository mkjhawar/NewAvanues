package com.augmentalis.ava.platform

import java.io.File
import java.io.FileNotFoundException

/**
 * Desktop (JVM) implementation of FileSystem.
 *
 * Uses java.io.File and System properties for directory paths.
 * Directories are based on user home and standard application data locations.
 */
actual class FileSystem actual constructor() {

    companion object {
        private val APP_NAME = "Augmentalis.AVA"

        private val HOME_DIR = System.getProperty("user.home")
        private val OS_NAME = System.getProperty("os.name").lowercase()

        // Platform-specific app data directory
        private val APP_DATA_DIR = when {
            OS_NAME.contains("win") -> {
                // Windows: %APPDATA%\Augmentalis.AVA
                System.getenv("APPDATA")?.let { "$it/$APP_NAME" }
                    ?: "$HOME_DIR/AppData/Roaming/$APP_NAME"
            }
            OS_NAME.contains("mac") -> {
                // macOS: ~/Library/Application Support/Augmentalis.AVA
                "$HOME_DIR/Library/Application Support/$APP_NAME"
            }
            else -> {
                // Linux/Unix: ~/.local/share/Augmentalis.AVA
                "$HOME_DIR/.local/share/$APP_NAME"
            }
        }

        private val CACHE_DIR = when {
            OS_NAME.contains("win") -> {
                System.getenv("TEMP")?.let { "$it/$APP_NAME" }
                    ?: "$HOME_DIR/AppData/Local/$APP_NAME/Cache"
            }
            OS_NAME.contains("mac") -> {
                "$HOME_DIR/Library/Caches/$APP_NAME"
            }
            else -> {
                "$HOME_DIR/.cache/$APP_NAME"
            }
        }

        private val DOCUMENTS_DIR = when {
            OS_NAME.contains("win") -> {
                "$HOME_DIR/Documents/$APP_NAME"
            }
            OS_NAME.contains("mac") -> {
                "$HOME_DIR/Documents/$APP_NAME"
            }
            else -> {
                "$HOME_DIR/Documents/$APP_NAME"
            }
        }
    }

    actual fun getFilesDirectory(): String {
        val dir = File(APP_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return APP_DATA_DIR
    }

    actual fun getCacheDirectory(): String {
        val dir = File(CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return CACHE_DIR
    }

    actual fun getDocumentsDirectory(): String {
        val dir = File(DOCUMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return DOCUMENTS_DIR
    }

    actual fun exists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun readBytes(path: String): ByteArray {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        return file.readBytes()
    }

    actual fun readText(path: String): String {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        return file.readText(Charsets.UTF_8)
    }

    actual fun writeBytes(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    actual fun writeText(path: String, text: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }

    actual fun delete(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    actual fun mkdirs(path: String): Boolean {
        return File(path).mkdirs()
    }

    actual fun listFiles(directory: String): List<String> {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }
        return dir.list()?.toList() ?: emptyList()
    }

    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) {
            file.length()
        } else {
            -1L
        }
    }

    actual fun getAvailableSpace(): Long {
        return File(APP_DATA_DIR).usableSpace
    }
}

/**
 * Factory for creating FileSystem instances on Desktop.
 */
actual object FileSystemFactory {
    actual fun create(): FileSystem {
        return FileSystem()
    }
}
