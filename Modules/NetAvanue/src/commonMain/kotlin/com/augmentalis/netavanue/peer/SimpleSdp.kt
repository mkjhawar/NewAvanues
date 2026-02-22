package com.augmentalis.netavanue.peer

import com.augmentalis.netavanue.ice.candidate.IceCandidate

/**
 * Minimal SDP (Session Description Protocol) for data-only peer connections.
 *
 * We don't need full SDP with media codec negotiation — only ICE credentials
 * and candidate exchange. This is a simplified subset sufficient for
 * establishing a DataChannel.
 *
 * Format:
 * ```
 * v=0
 * o=- {sessionId} 1 IN IP4 0.0.0.0
 * s=-
 * t=0 0
 * a=ice-ufrag:{ufrag}
 * a=ice-pwd:{pwd}
 * m=application 9 UDP/DTLS/SCTP webrtc-datachannel
 * c=IN IP4 0.0.0.0
 * a=candidate:{...}
 * a=candidate:{...}
 * ```
 */
data class SimpleSdp(
    val sessionId: String,
    val iceUfrag: String,
    val icePwd: String,
    val candidates: List<IceCandidate>,
    val isOffer: Boolean,
) {
    fun encode(): String = buildString {
        appendLine("v=0")
        appendLine("o=- $sessionId 1 IN IP4 0.0.0.0")
        appendLine("s=-")
        appendLine("t=0 0")
        appendLine("a=ice-ufrag:$iceUfrag")
        appendLine("a=ice-pwd:$icePwd")
        appendLine("a=setup:${if (isOffer) "actpass" else "active"}")
        appendLine("m=application 9 UDP/DTLS/SCTP webrtc-datachannel")
        appendLine("c=IN IP4 0.0.0.0")
        for (candidate in candidates) {
            appendLine("a=${candidate.toSdpAttribute()}")
        }
    }

    companion object {
        fun decode(sdp: String): SimpleSdp {
            var sessionId = ""
            var iceUfrag = ""
            var icePwd = ""
            var isOffer = true
            val candidates = mutableListOf<IceCandidate>()

            for (line in sdp.lines()) {
                when {
                    line.startsWith("o=") -> {
                        // o=- {sessionId} {version} IN IP4 ...
                        sessionId = line.substringAfter("- ").substringBefore(" ")
                    }
                    line.startsWith("a=ice-ufrag:") -> iceUfrag = line.substringAfter("a=ice-ufrag:")
                    line.startsWith("a=ice-pwd:") -> icePwd = line.substringAfter("a=ice-pwd:")
                    line.startsWith("a=setup:") -> isOffer = line.contains("actpass")
                    line.startsWith("a=candidate:") -> {
                        parseSdpCandidate(line.substringAfter("a="))?.let { candidates.add(it) }
                    }
                }
            }

            return SimpleSdp(sessionId, iceUfrag, icePwd, candidates, isOffer)
        }

        /**
         * Parse an SDP candidate attribute line:
         * `candidate:{foundation} {componentId} {protocol} {priority} {address} {port} typ {type} [raddr {addr} rport {port}]`
         */
        private fun parseSdpCandidate(line: String): IceCandidate? {
            val parts = line.removePrefix("candidate:").split(" ")
            if (parts.size < 8) return null

            val foundation = parts[0]
            val componentId = parts[1].toIntOrNull() ?: return null
            val protocol = when (parts[2].uppercase()) {
                "UDP" -> com.augmentalis.netavanue.ice.candidate.TransportProtocol.UDP
                "TCP" -> com.augmentalis.netavanue.ice.candidate.TransportProtocol.TCP
                else -> return null
            }
            val priority = parts[3].toLongOrNull() ?: return null
            val address = parts[4]
            val port = parts[5].toIntOrNull() ?: return null
            // parts[6] = "typ"
            val typeStr = parts[7]
            val type = when (typeStr) {
                "host" -> com.augmentalis.netavanue.ice.candidate.CandidateType.HOST
                "srflx", "server_reflexive" -> com.augmentalis.netavanue.ice.candidate.CandidateType.SERVER_REFLEXIVE
                "prflx", "peer_reflexive" -> com.augmentalis.netavanue.ice.candidate.CandidateType.PEER_REFLEXIVE
                "relay" -> com.augmentalis.netavanue.ice.candidate.CandidateType.RELAY
                else -> return null
            }

            var relatedAddress: String? = null
            var relatedPort: Int? = null
            val raddrIdx = parts.indexOf("raddr")
            if (raddrIdx >= 0 && raddrIdx + 1 < parts.size) relatedAddress = parts[raddrIdx + 1]
            val rportIdx = parts.indexOf("rport")
            if (rportIdx >= 0 && rportIdx + 1 < parts.size) relatedPort = parts[rportIdx + 1].toIntOrNull()

            return IceCandidate(
                foundation = foundation,
                componentId = componentId,
                protocol = protocol,
                priority = priority,
                address = address,
                port = port,
                type = type,
                relatedAddress = relatedAddress,
                relatedPort = relatedPort,
            )
        }
    }
}
