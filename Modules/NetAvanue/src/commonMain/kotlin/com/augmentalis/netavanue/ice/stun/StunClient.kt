package com.augmentalis.netavanue.ice.stun

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.ice.UdpSocket
import kotlinx.coroutines.delay

/**
 * STUN client for discovering the server-reflexive (public) address.
 *
 * Sends a STUN Binding Request to a STUN server and parses the
 * XOR-MAPPED-ADDRESS (or MAPPED-ADDRESS) from the response.
 *
 * Retransmission follows RFC 5389 Section 7.2.1:
 * - Initial RTO: 500ms
 * - Doubles each retry
 * - Max retries: 7 (total wait ~39.5 seconds)
 *
 * Usage:
 * ```
 * val socket = UdpSocket()
 * socket.bind()
 * val client = StunClient(socket)
 * val result = client.bindingRequest("stun.l.google.com", 19302)
 * println("Public address: ${result.address}:${result.port}")
 * ```
 */
data class StunResponse(
    val address: String,
    val port: Int,
    val serverReflexive: Boolean,
)

class StunClient(
    private val socket: UdpSocket,
    private val maxRetries: Int = 7,
    private val initialRtoMs: Long = 500,
) {
    private val logger = LoggerFactory.getLogger("StunClient")

    /**
     * Send a STUN Binding Request and return the server-reflexive address.
     *
     * @param host STUN server hostname or IP
     * @param port STUN server port (default 3478)
     * @return The discovered public address
     * @throws StunException if all retries exhausted or response is an error
     */
    suspend fun bindingRequest(host: String, port: Int = 3478): StunResponse {
        val transactionId = StunMessage.generateTransactionId()
        val request = StunMessage(
            type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST),
            transactionId = transactionId,
            attributes = listOf(
                StunAttribute.Software("NetAvanue/1.0"),
            ),
        )
        val requestBytes = request.encode()

        var rtoMs = initialRtoMs
        val receiveBuffer = ByteArray(2048)

        for (attempt in 0..maxRetries) {
            logger.d { "STUN Binding Request to $host:$port (attempt ${attempt + 1}/${maxRetries + 1}, rto=${rtoMs}ms)" }

            try {
                socket.send(requestBytes, host, port)
                val packet = socket.receive(receiveBuffer, timeoutMs = rtoMs)

                if (packet != null && packet.length >= StunMessage.HEADER_SIZE) {
                    val responseData = receiveBuffer.copyOf(packet.length)
                    val response = StunMessage.decode(responseData)

                    // Verify transaction ID matches
                    if (!response.transactionId.contentEquals(transactionId)) {
                        logger.w { "Transaction ID mismatch — ignoring stale response" }
                        continue
                    }

                    // Check for error response
                    if (response.type.clazz == StunClass.ERROR) {
                        val errorAttr = response.attributes.filterIsInstance<StunAttribute.ErrorCode>().firstOrNull()
                        val errorMsg = errorAttr?.let { "STUN error ${it.code}: ${it.reason}" }
                            ?: "STUN error response (no error code)"
                        throw StunException(errorMsg)
                    }

                    // Extract address from XOR-MAPPED-ADDRESS (preferred) or MAPPED-ADDRESS
                    val xorMapped = response.attributes.filterIsInstance<StunAttribute.XorMappedAddress>().firstOrNull()
                    if (xorMapped != null) {
                        val addr = formatAddress(xorMapped.family, xorMapped.address)
                        logger.i { "STUN response: $addr:${xorMapped.port} (XOR-MAPPED)" }
                        return StunResponse(addr, xorMapped.port, serverReflexive = true)
                    }

                    val mapped = response.attributes.filterIsInstance<StunAttribute.MappedAddress>().firstOrNull()
                    if (mapped != null) {
                        val addr = formatAddress(mapped.family, mapped.address)
                        logger.i { "STUN response: $addr:${mapped.port} (MAPPED)" }
                        return StunResponse(addr, mapped.port, serverReflexive = true)
                    }

                    throw StunException("STUN response has no address attribute")
                }
            } catch (e: StunException) {
                throw e // Don't retry protocol errors
            } catch (e: Exception) {
                logger.d { "STUN attempt ${attempt + 1} failed: ${e.message}" }
            }

            // Exponential backoff
            if (attempt < maxRetries) {
                delay(rtoMs)
                rtoMs = minOf(rtoMs * 2, 16_000)
            }
        }

        throw StunException("STUN Binding Request to $host:$port failed after ${maxRetries + 1} attempts")
    }

    /** Format a raw address byte array to a human-readable string */
    private fun formatAddress(family: AddressFamily, address: ByteArray): String {
        return when (family) {
            AddressFamily.IPv4 -> {
                require(address.size == 4) { "IPv4 address must be 4 bytes" }
                "${address[0].toInt() and 0xFF}.${address[1].toInt() and 0xFF}.${address[2].toInt() and 0xFF}.${address[3].toInt() and 0xFF}"
            }
            AddressFamily.IPv6 -> {
                require(address.size == 16) { "IPv6 address must be 16 bytes" }
                (0 until 16 step 2).joinToString(":") { i ->
                    "%02x%02x".format(address[i].toInt() and 0xFF, address[i + 1].toInt() and 0xFF)
                }
            }
        }
    }
}

class StunException(message: String, cause: Throwable? = null) : Exception(message, cause)
