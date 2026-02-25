package com.augmentalis.httpavanue.mdns

import com.augmentalis.httpavanue.io.AvanueBuffer

/**
 * DNS record types used for mDNS service advertisement (RFC 6762).
 * Encodes records in RFC 1035 wire format.
 */

/** DNS record type codes */
object DnsType {
    const val A: UShort = 1u       // IPv4 address
    const val PTR: UShort = 12u    // Pointer (service enumeration)
    const val TXT: UShort = 16u    // Text (service metadata)
    const val SRV: UShort = 33u    // Service locator
}

/** DNS class codes */
object DnsClass {
    const val IN: UShort = 1u           // Internet
    const val CACHE_FLUSH: UShort = 0x8001u  // Cache flush flag (mDNS-specific)
}

/**
 * A single DNS resource record.
 */
data class DnsRecord(
    val name: String,
    val type: UShort,
    val clazz: UShort,
    val ttl: Int,
    val rdata: ByteArray,
) {
    /** Encode this record to wire format bytes. */
    fun encode(): ByteArray {
        val buffer = AvanueBuffer()
        encodeName(buffer, name)
        buffer.writeShort(type.toInt())
        buffer.writeShort(clazz.toInt())
        buffer.writeInt(ttl)
        buffer.writeShort(rdata.size)
        buffer.write(rdata)
        return buffer.toByteArray()
    }

    override fun equals(other: Any?) = other is DnsRecord &&
        name == other.name && type == other.type && clazz == other.clazz &&
        ttl == other.ttl && rdata.contentEquals(other.rdata)
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + clazz.hashCode()
        result = 31 * result + ttl
        result = 31 * result + rdata.contentHashCode()
        return result
    }

    companion object {
        /** Create a PTR record (service type → service name). */
        fun ptr(typeDomain: String, fullName: String, ttl: Int = 4500): DnsRecord {
            val buffer = AvanueBuffer()
            encodeName(buffer, fullName)
            return DnsRecord(typeDomain, DnsType.PTR, DnsClass.IN, ttl, buffer.toByteArray())
        }

        /** Create an SRV record (service name → host + port). */
        fun srv(fullName: String, host: String, port: Int, ttl: Int = 120): DnsRecord {
            val buffer = AvanueBuffer()
            buffer.writeShort(0)     // priority
            buffer.writeShort(0)     // weight
            buffer.writeShort(port)
            encodeName(buffer, host)
            return DnsRecord(fullName, DnsType.SRV, DnsClass.CACHE_FLUSH, ttl, buffer.toByteArray())
        }

        /** Create a TXT record (service metadata). */
        fun txt(fullName: String, txt: Map<String, String>, ttl: Int = 4500): DnsRecord {
            val buffer = AvanueBuffer()
            for ((key, value) in txt) {
                val entry = "$key=$value".encodeToByteArray()
                buffer.writeByte(entry.size)
                buffer.write(entry)
            }
            if (txt.isEmpty()) buffer.writeByte(0) // Empty TXT record
            return DnsRecord(fullName, DnsType.TXT, DnsClass.CACHE_FLUSH, ttl, buffer.toByteArray())
        }

        /** Create an A record (host → IPv4 address). */
        fun a(host: String, ipv4: ByteArray, ttl: Int = 120): DnsRecord {
            require(ipv4.size == 4) { "IPv4 address must be 4 bytes" }
            return DnsRecord(host, DnsType.A, DnsClass.CACHE_FLUSH, ttl, ipv4)
        }
    }
}

/**
 * Encode a DNS name in wire format (RFC 1035 Section 4.1.2).
 * Each label is prefixed with its length, terminated by a zero-length label.
 */
internal fun encodeName(buffer: AvanueBuffer, name: String) {
    val cleanName = name.trimEnd('.')
    for (label in cleanName.split('.')) {
        val bytes = label.encodeToByteArray()
        buffer.writeByte(bytes.size)
        buffer.write(bytes)
    }
    buffer.writeByte(0) // Root label (end of name)
}
