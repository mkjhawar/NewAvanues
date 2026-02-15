/**
 * IFileSystem.kt - Cross-platform file system operations interface
 *
 * Abstraction for common file I/O operations.
 * Android implements via Environment + ContentResolver.
 * iOS implements via FileManager.
 * Desktop implements via java.nio.file.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.platform

/**
 * Platform-agnostic file system operations.
 *
 * Provides basic file I/O and path resolution for VOS file management,
 * settings export/import, and other cross-platform file needs.
 */
interface IFileSystem {

    /**
     * Get the platform-specific external storage path.
     *
     * On Android, returns Environment.getExternalStorageDirectory() path.
     * On iOS, returns the Documents directory path.
     * On Desktop, returns the user home directory.
     *
     * @return External storage path, or null if unavailable (e.g., no SD card)
     */
    fun getExternalStoragePath(): String?

    /**
     * Get the platform-specific documents directory path.
     *
     * On Android, returns context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).
     * On iOS, returns NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).
     * On Desktop, returns user.home/Documents.
     *
     * @return Absolute path to the documents directory
     */
    fun getDocumentsPath(): String

    /**
     * Get the platform-specific app-private files directory.
     *
     * On Android, returns context.filesDir.
     * On iOS, returns the app sandbox Library directory.
     *
     * @return Absolute path to the app-private files directory
     */
    fun getAppFilesPath(): String

    /**
     * Check whether a file or directory exists at the given path.
     *
     * @param path Absolute path to check
     * @return true if the path exists
     */
    fun exists(path: String): Boolean

    /**
     * Read the entire contents of a text file.
     *
     * @param path Absolute path to the file
     * @return File contents as a string
     * @throws Exception if the file doesn't exist or can't be read
     */
    suspend fun readText(path: String): String

    /**
     * Write text content to a file, creating it if it doesn't exist
     * or overwriting if it does.
     *
     * @param path Absolute path to the file
     * @param content The text content to write
     * @throws Exception if the file can't be written
     */
    suspend fun writeText(path: String, content: String)

    /**
     * Delete a file at the given path.
     *
     * @param path Absolute path to the file to delete
     * @return true if the file was deleted, false if it didn't exist
     */
    suspend fun delete(path: String): Boolean

    /**
     * List files in a directory.
     *
     * @param directoryPath Absolute path to the directory
     * @return List of file names (not full paths) in the directory
     * @throws Exception if the path is not a directory or can't be read
     */
    suspend fun listFiles(directoryPath: String): List<String>

    /**
     * Create a directory (and any necessary parent directories).
     *
     * @param path Absolute path to the directory to create
     * @return true if the directory was created (or already existed)
     */
    suspend fun createDirectories(path: String): Boolean
}
