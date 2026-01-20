package com.augmentalis.webavanue

import kotlinx.datetime.Instant

/**
 * SitePermission - Represents a remembered permission choice for a website
 *
 * Used to store user's permission decisions (camera, microphone, location, etc.)
 * so they don't have to be asked again on future visits.
 *
 * @property domain The website domain (e.g., "example.com")
 * @property permissionType The permission type (e.g., "CAMERA", "MICROPHONE", "LOCATION")
 * @property granted Whether the permission was granted (true) or denied (false)
 * @property timestamp When the permission was set
 */
data class SitePermission(
    val domain: String,
    val permissionType: String,
    val granted: Boolean,
    val timestamp: Instant
)
