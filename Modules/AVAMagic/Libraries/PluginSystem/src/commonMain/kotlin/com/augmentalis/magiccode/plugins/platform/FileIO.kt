package com.augmentalis.avacode.plugins.platform

/**
 * Platform-specific file I/O operations.
 *
 * Provides file system access for reading plugin manifests and validating directory structures.
 */
expect class FileIO {
    /**
     * Read file contents as string.
     *
     * @param path Absolute path to file
     * @return File contents as UTF-8 string
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if read fails
     */
    fun readFileAsString(path: String): String

    /**
     * Read file contents as byte array.
     *
     * @param path Absolute path to file
     * @return File contents as byte array
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if read fails
     */
    fun readFileAsBytes(path: String): ByteArray

    /**
     * Check if file exists.
     *
     * @param path Absolute path to file
     * @return true if file exists
     */
    fun fileExists(path: String): Boolean

    /**
     * Check if directory exists.
     *
     * @param path Absolute path to directory
     * @return true if directory exists
     */
    fun directoryExists(path: String): Boolean

    /**
     * Get parent directory path.
     *
     * @param path File or directory path
     * @return Parent directory path
     */
    fun getParentDirectory(path: String): String

    /**
     * List files in directory.
     *
     * @param path Directory path
     * @return List of file/directory names (not full paths)
     */
    fun listFiles(path: String): List<String>

    /**
     * Create directory (including parent directories).
     *
     * @param path Directory path to create
     * @return true if created or already exists
     */
    fun createDirectory(path: String): Boolean

    /**
     * Get file size in bytes.
     *
     * @param path File path
     * @return File size in bytes, or -1 if file doesn't exist
     */
    fun getFileSize(path: String): Long

    /**
     * Delete file or directory recursively.
     *
     * @param path Path to delete
     * @return true if deleted successfully
     */
    fun delete(path: String): Boolean

    /**
     * Write string content to file.
     *
     * @param path Absolute path to file
     * @param content Content to write as UTF-8 string
     * @throws IOException if write fails
     */
    fun writeFileAsString(path: String, content: String)

    /**
     * Write byte array to file.
     *
     * @param path Absolute path to file
     * @param content Content to write as byte array
     * @throws IOException if write fails
     */
    fun writeFileAsBytes(path: String, content: ByteArray)

    /**
     * Copy file or directory recursively.
     *
     * @param sourcePath Source path
     * @param destPath Destination path
     * @return true if copied successfully
     */
    fun copy(sourcePath: String, destPath: String): Boolean

    /**
     * Get last modified timestamp.
     *
     * @param path File or directory path
     * @return Last modified timestamp in milliseconds, or -1 if path doesn't exist
     */
    fun getLastModified(path: String): Long

    /**
     * List files recursively.
     *
     * @param path Directory path
     * @return List of relative paths from the given directory
     */
    fun listFilesRecursive(path: String): List<String>

    /**
     * Get available disk space.
     *
     * @param path Path to check (file or directory)
     * @return Available disk space in bytes, or -1 if unable to determine
     */
    fun getAvailableDiskSpace(path: String): Long
}
