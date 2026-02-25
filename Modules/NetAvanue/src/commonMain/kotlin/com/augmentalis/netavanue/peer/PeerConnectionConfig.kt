package com.augmentalis.netavanue.peer

/**
 * Configuration for a peer connection.
 *
 * Specifies STUN/TURN servers for ICE candidate gathering and
 * connection parameters like timeouts and retry behavior.
 */
data class PeerConnectionConfig(
    /** STUN server URLs for server-reflexive candidate discovery */
    val stunServers: List<String> = listOf(
        "stun:stun.avanues.com:3478",
        "stun:stun.l.google.com:19302", // Fallback public STUN
    ),
    /** TURN server credentials (issued by signaling server) */
    val turnServers: List<TurnServerConfig> = emptyList(),
    /** ICE candidate gathering timeout in milliseconds */
    val gatheringTimeoutMs: Long = 10_000,
    /** ICE connectivity check timeout per pair in milliseconds */
    val checkTimeoutMs: Long = 5_000,
    /** Maximum number of connectivity check retries per pair */
    val maxCheckRetries: Int = 3,
    /** Whether this agent is the controlling agent (offerer = controlling) */
    val isControlling: Boolean = true,
    /** ICE username fragment for this session */
    val iceUfrag: String = generateIceUfrag(),
    /** ICE password for this session */
    val icePwd: String = generateIcePwd(),
)

data class TurnServerConfig(
    val url: String,
    val username: String,
    val password: String,
)

/** Parse a STUN/TURN URL: "stun:host:port" or "turn:host:port" */
fun parseStunUrl(url: String): Pair<String, Int> {
    val withoutScheme = url.substringAfter(":")
    val parts = withoutScheme.split(":")
    val host = parts[0]
    val port = parts.getOrNull(1)?.substringBefore("?")?.toIntOrNull() ?: 3478
    return host to port
}

private val ICE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/"

private fun generateIceUfrag(): String =
    (1..4).map { ICE_CHARS[kotlin.random.Random.nextInt(ICE_CHARS.length)] }.joinToString("")

private fun generateIcePwd(): String =
    (1..22).map { ICE_CHARS[kotlin.random.Random.nextInt(ICE_CHARS.length)] }.joinToString("")
