package com.augmentalis.ava.platform

/**
 * Cross-platform file system abstraction.
 *
 * Platform implementations:
 * - Android: Context.filesDir, Context.cacheDir, etc.
 * - iOS: NSFileManager with standard directories
 * - Desktop: System.getProperty paths
 */
expect class FileSystem() {

    /**
     * Get the application's private files directory.
     * Files here persist until app is uninstalled.
     */
    fun getFilesDirectory(): String

    /**
     * Get the application's cache directory.
     * Files here may be deleted by system when storage is low.
     */
    fun getCacheDirectory(): String

    /**
     * Get the application's external/documents directory.
     * On Android: External files dir
     * On iOS: Documents directory
     * On Desktop: User home directory
     */
    fun getDocumentsDirectory(): String

    /**
     * Check if a file exists at the given path.
     */
    fun exists(path: String): Boolean

    /**
     * Read file contents as bytes.
     * @throws FileNotFoundException if file doesn't exist
     */
    fun readBytes(path: String): ByteArray

    /**
     * Read file contents as string (UTF-8).
     */
    fun readText(path: String): String

    /**
     * Write bytes to file, creating parent directories if needed.
     */
    fun writeBytes(path: String, data: ByteArray)

    /**
     * Write string to file (UTF-8).
     */
    fun writeText(path: String, text: String)

    /**
     * Delete file at path.
     * @return true if deleted, false if didn't exist
     */
    fun delete(path: String): Boolean

    /**
     * Create directory and parent directories.
     * @return true if created or already exists
     */
    fun mkdirs(path: String): Boolean

    /**
     * List files in directory.
     * @return list of file names (not full paths)
     */
    fun listFiles(directory: String): List<String>

    /**
     * Get file size in bytes, or -1 if doesn't exist.
     */
    fun getFileSize(path: String): Long

    /**
     * Get available storage space in bytes.
     */
    fun getAvailableSpace(): Long
}

/**
 * Factory for creating FileSystem instances.
 */
expect object FileSystemFactory {
    fun create(): FileSystem
}
