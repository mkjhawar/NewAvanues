/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

/**
 * JS implementation of PlatformIdentity.
 *
 * Runtime detection:
 * - Browser: Uses `window.location.origin` (e.g., "https://app.avanues.com")
 * - Node.js: Uses `process.env.AON_APP_ID` or `process.title`
 *
 * The origin-based identity for browsers is the web equivalent of
 * Android package whitelisting — it ties AON file authorization
 * to the serving domain.
 */
actual object PlatformIdentity {

    private val isNodeJs: Boolean = js(
        "typeof process !== 'undefined' && typeof process.versions !== 'undefined' && typeof process.versions.node !== 'undefined'"
    ) as Boolean

    actual fun getAppIdentifier(): String {
        return if (isNodeJs) {
            getNodeIdentity()
        } else {
            getBrowserIdentity()
        }
    }

    actual fun getIdentityType(): String {
        return if (isNodeJs) "nodejs_env" else "js_origin"
    }

    private fun getNodeIdentity(): String {
        // Priority 1: Explicit env var
        val envId: dynamic = js("typeof process !== 'undefined' && process.env ? process.env.AON_APP_ID : undefined")
        if (envId != null && envId != undefined) {
            return envId as String
        }

        // Priority 2: Process title
        val title: dynamic = js("typeof process !== 'undefined' ? process.title : undefined")
        if (title != null && title != undefined) {
            val titleStr = title as String
            if (titleStr.isNotBlank() && titleStr != "undefined") return titleStr
        }

        return "nodejs.unknown"
    }

    private fun getBrowserIdentity(): String {
        // Priority 1: window.location.origin
        val origin: dynamic = js("typeof window !== 'undefined' && window.location ? window.location.origin : undefined")
        if (origin != null && origin != undefined) {
            val originStr = origin as String
            if (originStr.isNotBlank() && originStr != "null") return originStr
        }

        // Priority 2: document.domain
        val domain: dynamic = js("typeof document !== 'undefined' ? document.domain : undefined")
        if (domain != null && domain != undefined) {
            val domainStr = domain as String
            if (domainStr.isNotBlank()) return domainStr
        }

        return "browser.unknown"
    }
}
