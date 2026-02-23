package com.augmentalis.httpavanue.platform

/**
 * JS implementation of resource loading.
 *
 * Returns null because JS environments load resources via fetch/import
 * rather than classpath-based loading. Server-side templates and static
 * files should be served through the bundler or CDN in web deployments.
 */
actual fun readResource(path: String): String? = null
