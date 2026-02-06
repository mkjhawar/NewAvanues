package com.augmentalis.avacode.cli

/**
 * FileIO - Platform-agnostic file I/O interface
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

expect object FileIO {
    /**
     * Read file contents as string
     */
    fun readFile(path: String): String

    /**
     * Write string content to file
     */
    fun writeFile(path: String, content: String)

    /**
     * Check if file exists
     */
    fun fileExists(path: String): Boolean

    /**
     * Create directory
     */
    fun createDirectory(path: String)

    /**
     * List files in directory
     */
    fun listFiles(path: String): List<String>

    /**
     * Get file extension
     */
    fun getFileExtension(path: String): String

    /**
     * Get absolute path
     */
    fun getAbsolutePath(path: String): String
}
