package com.augmentalis.netavanue.ice.candidate

import kotlinx.serialization.Serializable

/**
 * ICE candidate types per RFC 8445.
 *
 * Candidates represent potential network paths for peer communication:
 * - **Host**: Local interface IP:port (highest priority, direct on LAN)
 * - **ServerReflexive**: Public IP:port discovered via STUN (works through most NATs)
 * - **Relay**: TURN server relay address (always works, but adds latency)
 *
 * Priority formula (RFC 8445 Section 5.1.2):
 * ```
 * priority = (2^24 * typePreference) + (2^8 * localPreference) + (2^0 * (256 - componentId))
 * ```
 */
@Serializable
enum class CandidateType(val typePreference: Int) {
    HOST(126),
    SERVER_REFLEXIVE(100),
    PEER_REFLEXIVE(110),
    RELAY(0),
}

@Serializable
enum class TransportProtocol { UDP, TCP }

@Serializable
data class IceCandidate(
    val foundation: String,
    val componentId: Int,
    val protocol: TransportProtocol,
    val priority: Long,
    val address: String,
    val port: Int,
    val type: CandidateType,
    val relatedAddress: String? = null,
    val relatedPort: Int? = null,
) {
    companion object {
        /**
         * Calculate candidate priority per RFC 8445.
         *
         * @param typePreference Type preference (126=host, 100=srflx, 110=prflx, 0=relay)
         * @param localPreference Local preference (0-65535, typically based on interface)
         * @param componentId Component ID (1=RTP, 2=RTCP, we use 1 for data)
         */
        fun calculatePriority(
            typePreference: Int,
            localPreference: Int = 65535,
            componentId: Int = 1,
        ): Long {
            return (typePreference.toLong() shl 24) +
                (localPreference.toLong() shl 8) +
                (256L - componentId)
        }

        /**
         * Generate a foundation string for candidate deduplication.
         * Same foundation = same type + base address + STUN server.
         */
        fun generateFoundation(
            type: CandidateType,
            baseAddress: String,
            stunServer: String? = null,
        ): String {
            val input = "${type.name}:$baseAddress:${stunServer ?: "none"}"
            // Simple hash for foundation — just needs to be consistent, not cryptographic
            var hash = 0
            for (c in input) {
                hash = 31 * hash + c.code
            }
            return (hash and 0x7FFFFFFF).toString(16)
        }

        /**
         * Create a host candidate from a local interface address.
         */
        fun host(
            address: String,
            port: Int,
            componentId: Int = 1,
            localPreference: Int = 65535,
        ): IceCandidate {
            val foundation = generateFoundation(CandidateType.HOST, address)
            val priority = calculatePriority(CandidateType.HOST.typePreference, localPreference, componentId)
            return IceCandidate(
                foundation = foundation,
                componentId = componentId,
                protocol = TransportProtocol.UDP,
                priority = priority,
                address = address,
                port = port,
                type = CandidateType.HOST,
            )
        }

        /**
         * Create a server-reflexive candidate from a STUN binding response.
         */
        fun serverReflexive(
            address: String,
            port: Int,
            relatedAddress: String,
            relatedPort: Int,
            stunServer: String,
            componentId: Int = 1,
            localPreference: Int = 65535,
        ): IceCandidate {
            val foundation = generateFoundation(CandidateType.SERVER_REFLEXIVE, relatedAddress, stunServer)
            val priority = calculatePriority(CandidateType.SERVER_REFLEXIVE.typePreference, localPreference, componentId)
            return IceCandidate(
                foundation = foundation,
                componentId = componentId,
                protocol = TransportProtocol.UDP,
                priority = priority,
                address = address,
                port = port,
                type = CandidateType.SERVER_REFLEXIVE,
                relatedAddress = relatedAddress,
                relatedPort = relatedPort,
            )
        }

        /**
         * Create a relay candidate from a TURN allocation.
         */
        fun relay(
            address: String,
            port: Int,
            relatedAddress: String,
            relatedPort: Int,
            turnServer: String,
            componentId: Int = 1,
            localPreference: Int = 65535,
        ): IceCandidate {
            val foundation = generateFoundation(CandidateType.RELAY, relatedAddress, turnServer)
            val priority = calculatePriority(CandidateType.RELAY.typePreference, localPreference, componentId)
            return IceCandidate(
                foundation = foundation,
                componentId = componentId,
                protocol = TransportProtocol.UDP,
                priority = priority,
                address = address,
                port = port,
                type = CandidateType.RELAY,
                relatedAddress = relatedAddress,
                relatedPort = relatedPort,
            )
        }
    }

    /**
     * Encode to SDP candidate attribute format:
     * `candidate:{foundation} {componentId} {protocol} {priority} {address} {port} typ {type} [raddr {related} rport {rport}]`
     */
    fun toSdpAttribute(): String = buildString {
        append("candidate:$foundation $componentId ${protocol.name.lowercase()} $priority $address $port typ ${type.name.lowercase()}")
        if (relatedAddress != null && relatedPort != null) {
            append(" raddr $relatedAddress rport $relatedPort")
        }
    }
}
