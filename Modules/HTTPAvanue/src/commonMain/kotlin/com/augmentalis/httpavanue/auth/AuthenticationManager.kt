package com.augmentalis.httpavanue.auth

/**
 * Authentication manager interface for token validation.
 * Minimal local type replacing AvaConnect's avaconnect.auth.AuthenticationManager.
 */
interface AuthenticationManager {
    fun validateToken(token: String): TokenValidation
}

/**
 * Token validation result
 */
data class TokenValidation(
    val valid: Boolean,
    val deviceId: String? = null,
    val capabilities: List<String>? = null,
    val error: String? = null,
)
