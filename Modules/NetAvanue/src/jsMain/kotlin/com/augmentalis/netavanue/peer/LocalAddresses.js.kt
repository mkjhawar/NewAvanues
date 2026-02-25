package com.augmentalis.netavanue.peer

/**
 * JS/Browser actual for [getLocalAddresses].
 *
 * Browsers cannot enumerate local network interfaces for security reasons.
 * The WebRTC ICE agent discovers local candidates internally through
 * RTCPeerConnection without exposing IP addresses to JavaScript.
 *
 * Returns a fallback list with "0.0.0.0" to indicate unknown local addresses.
 */
actual fun getLocalAddresses(): List<String> = listOf("0.0.0.0")
