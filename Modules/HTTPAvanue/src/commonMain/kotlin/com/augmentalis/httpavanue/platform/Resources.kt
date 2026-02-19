package com.augmentalis.httpavanue.platform

/**
 * Platform-specific resource loading.
 * Reads files from application resources/assets.
 */
expect fun readResource(path: String): String?
