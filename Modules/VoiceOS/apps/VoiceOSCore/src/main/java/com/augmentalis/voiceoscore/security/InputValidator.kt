/**
 * InputValidator.kt - Input validation for security
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Responsibility: Validates all user and external inputs for security vulnerabilities
 * Ported from JITLearning SecurityValidator to VoiceOSCore for centralized validation
 */
package com.augmentalis.voiceoscore.security

import java.util.regex.Pattern

/**
 * Validates all inputs for security vulnerabilities including:
 * - SQL injection attempts
 * - XSS attacks
 * - Path traversal
 * - Format validation
 * - Length limits
 */
object InputValidator {
    private const val TAG = "InputValidator"

    // Validation limits
    private const val MAX_PACKAGE_NAME_LENGTH = 255
    private const val MAX_UUID_LENGTH = 64  // Compact VUIDs are ~30 chars, legacy up to 52
    private const val MAX_SCREEN_HASH_LENGTH = 64
    private const val MAX_TEXT_INPUT_LENGTH = 10000
    private const val MAX_SELECTOR_LENGTH = 512
    private const val MAX_NODE_ID_LENGTH = 512
    private const val MAX_DISTANCE = 10000
    private const val MAX_BOUNDS_DIMENSION = 100000

    // Package name regex: standard Android package format
    private val PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    // VUID regex: supports both compact and legacy formats
    // Compact format: android.instagram.com:12.0.0:btn:a7f3e2c1 (uses colons)
    // Legacy format: com.package.name.element-hexhash (uses dots and hyphens)
    // Module format: ava:msg:a7f3e2c1 (short module codes)
    private val UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9.:-]+$")

    // Screen hash regex: hexadecimal only
    private val SCREEN_HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]+$")

    // Node ID regex: standard resource ID format
    private val NODE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_./:*-]+$")

    /**
     * Validate package name
     * @throws IllegalArgumentException if invalid
     */
    fun validatePackageName(packageName: String?) {
        require(!packageName.isNullOrBlank()) { "Package name cannot be null or empty" }
        require(packageName.length <= MAX_PACKAGE_NAME_LENGTH) {
            "Package name too long: ${packageName.length} > $MAX_PACKAGE_NAME_LENGTH"
        }
        require(PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
            "Invalid package name format: $packageName"
        }
        require(!packageName.contains("..")) {
            "Package name contains path traversal pattern"
        }
        require(!packageName.contains("'") && !packageName.contains("\"")) {
            "Package name contains SQL injection characters"
        }
    }

    /**
     * Validate UUID
     * @throws IllegalArgumentException if invalid
     */
    fun validateUuid(uuid: String?) {
        require(!uuid.isNullOrBlank()) { "UUID cannot be null or empty" }
        require(uuid.length <= MAX_UUID_LENGTH) {
            "UUID too long: ${uuid.length} > $MAX_UUID_LENGTH"
        }
        require(UUID_PATTERN.matcher(uuid).matches()) {
            "Invalid UUID format: $uuid"
        }
    }

    /**
     * Validate screen hash
     * @throws IllegalArgumentException if invalid
     */
    fun validateScreenHash(screenHash: String?) {
        require(!screenHash.isNullOrBlank()) { "Screen hash cannot be null or empty" }
        require(screenHash.length <= MAX_SCREEN_HASH_LENGTH) {
            "Screen hash too long: ${screenHash.length} > $MAX_SCREEN_HASH_LENGTH"
        }
        require(SCREEN_HASH_PATTERN.matcher(screenHash).matches()) {
            "Invalid screen hash format (must be hexadecimal): $screenHash"
        }
    }

    /**
     * Validate text input
     * @throws IllegalArgumentException if invalid
     */
    fun validateTextInput(text: String?) {
        // Null is allowed for clearing text
        if (text == null) return

        require(text.length <= MAX_TEXT_INPUT_LENGTH) {
            "Text input too long: ${text.length} > $MAX_TEXT_INPUT_LENGTH"
        }

        // Check for XSS attempts
        require(!text.contains("<script", ignoreCase = true)) {
            "Text contains potential XSS attack"
        }
        require(!text.contains("javascript:", ignoreCase = true)) {
            "Text contains potential XSS attack"
        }

        // Check for SQL injection attempts
        val sqlKeywords = listOf("DROP", "DELETE", "INSERT", "UPDATE", "SELECT", "';", "--;", "/*", "*/")
        for (keyword in sqlKeywords) {
            require(!text.contains(keyword, ignoreCase = true)) {
                "Text contains potential SQL injection pattern: $keyword"
            }
        }
    }

    /**
     * Validate selector string
     * @throws IllegalArgumentException if invalid
     */
    fun validateSelector(selector: String?) {
        require(!selector.isNullOrBlank()) { "Selector cannot be null or empty" }
        require(selector.length <= MAX_SELECTOR_LENGTH) {
            "Selector too long: ${selector.length} > $MAX_SELECTOR_LENGTH"
        }

        // Must be in format "type:pattern"
        val parts = selector.split(":", limit = 2)
        require(parts.size == 2) {
            "Invalid selector format. Expected 'type:pattern', got: $selector"
        }

        val type = parts[0].lowercase()
        require(type in listOf("class", "id", "text", "desc")) {
            "Invalid selector type. Expected class/id/text/desc, got: $type"
        }

        // Check for path traversal
        require(!selector.contains("../")) {
            "Selector contains path traversal pattern"
        }
    }

    /**
     * Validate node ID
     * @throws IllegalArgumentException if invalid
     */
    fun validateNodeId(nodeId: String?) {
        require(!nodeId.isNullOrBlank()) { "Node ID cannot be null or empty" }
        require(nodeId.length <= MAX_NODE_ID_LENGTH) {
            "Node ID too long: ${nodeId.length} > $MAX_NODE_ID_LENGTH"
        }
        require(NODE_ID_PATTERN.matcher(nodeId).matches()) {
            "Invalid node ID format: $nodeId"
        }
        require(!nodeId.contains("../")) {
            "Node ID contains path traversal pattern"
        }
    }

    /**
     * Validate scroll direction
     * @throws IllegalArgumentException if invalid
     */
    fun validateScrollDirection(direction: String?) {
        require(!direction.isNullOrBlank()) { "Scroll direction cannot be null or empty" }
        val normalized = direction.lowercase()
        require(normalized in listOf("up", "down", "left", "right")) {
            "Invalid scroll direction. Expected up/down/left/right, got: $direction"
        }
    }

    /**
     * Validate scroll distance
     * @throws IllegalArgumentException if invalid
     */
    fun validateDistance(distance: Int) {
        require(distance >= 0) { "Distance cannot be negative: $distance" }
        require(distance <= MAX_DISTANCE) {
            "Distance too large: $distance > $MAX_DISTANCE"
        }
    }

    /**
     * Validate bounds
     * @throws IllegalArgumentException if invalid
     */
    fun validateBounds(left: Int, top: Int, right: Int, bottom: Int) {
        require(left >= 0) { "Left bound cannot be negative: $left" }
        require(top >= 0) { "Top bound cannot be negative: $top" }
        require(right >= left) { "Right bound must be >= left: right=$right, left=$left" }
        require(bottom >= top) { "Bottom bound must be >= top: bottom=$bottom, top=$top" }

        require(right <= MAX_BOUNDS_DIMENSION) {
            "Right bound too large: $right > $MAX_BOUNDS_DIMENSION"
        }
        require(bottom <= MAX_BOUNDS_DIMENSION) {
            "Bottom bound too large: $bottom > $MAX_BOUNDS_DIMENSION"
        }

        // Sanity check: bounds shouldn't be absurdly large
        val width = right - left
        val height = bottom - top
        require(width <= MAX_BOUNDS_DIMENSION) {
            "Width too large: $width > $MAX_BOUNDS_DIMENSION"
        }
        require(height <= MAX_BOUNDS_DIMENSION) {
            "Height too large: $height > $MAX_BOUNDS_DIMENSION"
        }
    }
}
