/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

/**
 * Configuration for wrapping raw ONNX model bytes into the AON format.
 *
 * Passed to [AONCodec.wrap] to control metadata, encryption, and
 * package restrictions embedded in the AON header/footer.
 */
data class AONWrapConfig(
    /** Model identifier (max 32 ASCII chars, null-padded) */
    val modelId: String,

    /** Model version number */
    val modelVersion: Int = 1,

    /** License tier: 0=free, 1=pro, 2=enterprise */
    val licenseTier: Byte = 0,

    /**
     * Platform bitfield: 0=all, or OR of:
     * [AONFormat.PLATFORM_ANDROID], [AONFormat.PLATFORM_IOS],
     * [AONFormat.PLATFORM_DESKTOP], [AONFormat.PLATFORM_WEB],
     * [AONFormat.PLATFORM_NODEJS]
     */
    val platformFlags: Byte = 0,

    /** Whether to encrypt the payload with AES-256-GCM */
    val encrypt: Boolean = false,

    /** Unix seconds expiry timestamp (0 = no expiry) */
    val expiryTimestamp: Long = 0,

    /**
     * Up to 3 package/bundle identifiers allowed to unwrap this file.
     * Each is MD5-hashed before storage. Empty list = no restriction.
     */
    val allowedPackages: List<String> = emptyList(),

    /** Build number embedded in footer */
    val buildNumber: Int = 0,

    /** Creator signature (max 16 ASCII chars) */
    val creatorSignature: String = "AVA-CLI",

    /** Override created timestamp (0 = use current time from platform) */
    val createdTimestamp: Long = 0
)
