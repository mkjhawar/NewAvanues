package com.augmentalis.netavanue.ice

/**
 * Platform-agnostic UDP datagram received from the network.
 *
 * @param data    Raw bytes of the datagram payload
 * @param length  Number of valid bytes in [data] (may be less than data.size)
 * @param remoteHost  IP address of the sender (dotted-quad or IPv6 text form)
 * @param remotePort  Source port of the sender
 */
data class UdpPacket(
    val data: ByteArray,
    val length: Int,
    val remoteHost: String,
    val remotePort: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UdpPacket) return false
        return length == other.length &&
            remoteHost == other.remoteHost &&
            remotePort == other.remotePort &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + length
        result = 31 * result + remoteHost.hashCode()
        result = 31 * result + remotePort.hashCode()
        return result
    }
}

/**
 * Cross-platform UDP socket abstraction for ICE connectivity checks.
 *
 * Each platform provides an `actual` implementation:
 * - Android / Desktop (JVM): `java.net.DatagramSocket`
 * - iOS: POSIX `socket(AF_INET, SOCK_DGRAM, 0)` via `kotlinx.cinterop`
 *
 * All blocking I/O is dispatched onto the appropriate coroutine dispatcher
 * so callers can invoke these suspending functions from any coroutine context.
 */
expect class UdpSocket() {
    /**
     * Bind the socket to a local port.
     *
     * @param port Local port to bind. Pass 0 (default) to let the OS assign an
     *             ephemeral port -- read [localPort] afterwards to discover it.
     */
    suspend fun bind(port: Int = 0)

    /**
     * Send a UDP datagram.
     *
     * @param data  Payload bytes to send
     * @param host  Destination IP address or hostname
     * @param port  Destination port
     */
    suspend fun send(data: ByteArray, host: String, port: Int)

    /**
     * Receive a single UDP datagram, blocking up to [timeoutMs] milliseconds.
     *
     * @param buffer  Pre-allocated receive buffer; incoming data is written here
     * @param timeoutMs  Maximum wait time in milliseconds (default 5 000)
     * @return A [UdpPacket] describing the received datagram, or `null` if the
     *         timeout elapsed without receiving data
     */
    suspend fun receive(buffer: ByteArray, timeoutMs: Long = 5000): UdpPacket?

    /**
     * Close the socket and release the underlying OS resource.
     * Subsequent send/receive calls will fail. Safe to call multiple times.
     */
    fun close()

    /** The local port this socket is bound to, or -1 if not yet bound. */
    val localPort: Int

    /** `true` after [close] has been called. */
    val isClosed: Boolean
}
