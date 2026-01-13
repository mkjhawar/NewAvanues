package com.augmentalis.avacode.plugins.platform

/**
 * Platform-specific ZIP extraction utilities.
 *
 * Provides cross-platform ZIP archive extraction functionality for plugin installation.
 */
expect class ZipExtractor {
    /**
     * Extract ZIP archive to destination directory.
     *
     * @param zipPath Path to ZIP file
     * @param destPath Destination directory path
     * @return true if extraction successful
     * @throws IOException if extraction fails
     */
    fun extractZip(zipPath: String, destPath: String): Boolean

    /**
     * List entries in ZIP archive.
     *
     * @param zipPath Path to ZIP file
     * @return List of entry paths in the ZIP
     * @throws IOException if reading ZIP fails
     */
    fun listZipEntries(zipPath: String): List<String>

    /**
     * Check if ZIP contains a specific entry.
     *
     * @param zipPath Path to ZIP file
     * @param entryPath Entry path to check for
     * @return true if entry exists in ZIP
     */
    fun containsEntry(zipPath: String, entryPath: String): Boolean
}
