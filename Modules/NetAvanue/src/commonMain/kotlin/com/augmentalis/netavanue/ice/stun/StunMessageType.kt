package com.augmentalis.netavanue.ice.stun

/**
 * STUN message method identifiers (RFC 5389, Section 18.1).
 *
 * The method occupies 12 bits of the 16-bit STUN message type field,
 * but those bits are NOT contiguous -- they are split around the two
 * class bits at positions 4 and 8.
 */
enum class StunMethod(val code: Int) {
    BINDING(0x0001),
}

/**
 * STUN message class (RFC 5389, Section 6).
 *
 * The class is encoded in two bits of the 16-bit message type field:
 *   - C0 at bit position 4 (0x0010)
 *   - C1 at bit position 8 (0x0100)
 *
 * @param c0 Least significant class bit (bit 4)
 * @param c1 Most significant class bit (bit 8)
 */
enum class StunClass(val c0: Int, val c1: Int) {
    REQUEST(0, 0),     // 0b00
    INDICATION(1, 0),  // 0b01 — C0=1 at bit 4, C1=0 at bit 8
    SUCCESS(0, 1),     // 0b10 — C0=0 at bit 4, C1=1 at bit 8
    ERROR(1, 1),       // 0b11
}

/**
 * Decoded STUN message type combining method and class.
 *
 * The 16-bit STUN type field encodes both the method (12 bits) and class (2 bits)
 * with interleaved bit positions. The top two bits are always zero (distinguishing
 * STUN from other protocols like RTP that use the channel numbers space).
 *
 * Bit layout of the 16-bit type field:
 * ```
 *   0                 1
 *   2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |M |M |M |M |M |C1|M |M |M |C0|M |M |M |M |
 *  |11|10|9 |8 |7 |  |6 |5 |4 |  |3 |2 |1 |0 |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * ```
 *
 * Where M0-M11 are the 12 method bits and C0-C1 are the 2 class bits.
 */
data class StunMessageType(
    val method: StunMethod,
    val clazz: StunClass,
) {
    /**
     * Encode this type into the 16-bit STUN message type field.
     *
     * The method bits are split into three groups and shifted to make room
     * for the two interleaved class bits:
     *   - M[3:0]  (bits 0-3)   stay at positions 0-3
     *   - M[6:4]  (bits 4-6)   shift left 1 to positions 5-7
     *   - M[11:7] (bits 7-11)  shift left 2 to positions 9-13
     *
     * The class bits are placed at:
     *   - C0 at position 4  (0x0010)
     *   - C1 at position 8  (0x0100)
     */
    fun encode(): Int {
        val m = method.code
        // Split method into three groups and shift around class bit positions
        val m0_3 = m and 0x000F          // bits 0-3: no shift
        val m4_6 = (m and 0x0070) shl 1  // bits 4-6: shift left 1 (skip C0 at pos 4)
        val m7_11 = (m and 0x0F80) shl 2 // bits 7-11: shift left 2 (skip C0 and C1)

        // Place class bits at their interleaved positions
        val c0 = clazz.c0 shl 4   // C0 at bit 4
        val c1 = clazz.c1 shl 8   // C1 at bit 8

        return m0_3 or m4_6 or m7_11 or c0 or c1
    }

    companion object {
        /**
         * Decode a 16-bit STUN message type field into method and class.
         *
         * Extracts the two class bits from their interleaved positions, then
         * reassembles the 12 method bits by removing the gaps.
         *
         * @param value The raw 16-bit type field from the STUN header
         * @return Decoded [StunMessageType]
         * @throws IllegalArgumentException if the method code is unrecognized
         */
        fun decode(value: Int): StunMessageType {
            // Extract class bits
            val c0 = (value shr 4) and 0x01
            val c1 = (value shr 8) and 0x01

            // Extract method bits from three groups and reassemble
            val m0_3 = value and 0x000F                  // bits 0-3
            val m4_6 = (value and 0x00E0) shr 1          // bits 5-7 -> method bits 4-6
            val m7_11 = (value and 0x3E00) shr 2         // bits 9-13 -> method bits 7-11
            val methodCode = m0_3 or m4_6 or m7_11

            val method = StunMethod.entries.find { it.code == methodCode }
                ?: throw IllegalArgumentException(
                    "Unknown STUN method: 0x${methodCode.toString(16).padStart(4, '0')}"
                )

            val clazz = StunClass.entries.find { it.c0 == c0 && it.c1 == c1 }
                ?: throw IllegalArgumentException("Invalid STUN class bits: c0=$c0, c1=$c1")

            return StunMessageType(method = method, clazz = clazz)
        }
    }
}
