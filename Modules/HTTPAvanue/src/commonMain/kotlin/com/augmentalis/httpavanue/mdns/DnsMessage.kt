package com.augmentalis.httpavanue.mdns

import com.augmentalis.httpavanue.io.AvanueBuffer

/**
 * DNS message encoder for mDNS advertisement (RFC 1035 / RFC 6762).
 *
 * Encodes DNS response messages containing service records for
 * multicast advertisement on 224.0.0.251:5353.
 */
data class DnsMessage(
    val id: UShort = 0u,     // Always 0 for mDNS
    val flags: UShort = 0x8400u, // Standard response, authoritative
    val answers: List<DnsRecord> = emptyList(),
    val additionals: List<DnsRecord> = emptyList(),
) {
    /** Encode the full DNS message to wire format bytes. */
    fun encode(): ByteArray {
        val buffer = AvanueBuffer()

        // Header (12 bytes)
        buffer.writeShort(id.toInt())             // ID
        buffer.writeShort(flags.toInt())           // Flags
        buffer.writeShort(0)                       // QDCOUNT (no questions in response)
        buffer.writeShort(answers.size)            // ANCOUNT
        buffer.writeShort(0)                       // NSCOUNT (no authority)
        buffer.writeShort(additionals.size)        // ARCOUNT

        // Answer records
        for (record in answers) {
            buffer.write(record.encode())
        }

        // Additional records
        for (record in additionals) {
            buffer.write(record.encode())
        }

        return buffer.toByteArray()
    }

    companion object {
        /** mDNS multicast address */
        const val MDNS_ADDRESS = "224.0.0.251"
        const val MDNS_PORT = 5353

        /**
         * Build a complete mDNS advertisement message for a service.
         * Includes PTR → SRV → TXT → A records.
         */
        fun advertise(service: MdnsService, hostIpv4: ByteArray): DnsMessage {
            val hostName = "${service.name.replace(' ', '-')}.local."
            return DnsMessage(
                answers = listOf(
                    DnsRecord.ptr(service.typeDomain, service.fullName),
                ),
                additionals = listOf(
                    DnsRecord.srv(service.fullName, hostName, service.port),
                    DnsRecord.txt(service.fullName, service.txt),
                    DnsRecord.a(hostName, hostIpv4),
                ),
            )
        }
    }
}
