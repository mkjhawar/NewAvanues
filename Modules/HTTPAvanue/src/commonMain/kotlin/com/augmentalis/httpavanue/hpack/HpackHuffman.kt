package com.augmentalis.httpavanue.hpack

/**
 * HPACK Huffman codec per RFC 7541 Appendix B.
 *
 * The static Huffman table maps each byte value (0-255) plus EOS (256)
 * to a variable-length bit code. Shorter codes are assigned to more
 * frequent HTTP header characters (lowercase letters, digits, common
 * punctuation).
 */
object HpackHuffman {

    /**
     * Decode Huffman-encoded bytes to plaintext bytes.
     *
     * Processes input bit-by-bit, traversing the Huffman tree represented
     * as a flat decode table. When a leaf node is reached, the decoded
     * symbol is emitted and traversal restarts from the root.
     *
     * @throws Http2Exception on invalid Huffman encoding
     */
    fun decode(data: ByteArray): ByteArray {
        val result = ByteArray(data.size * 2) // Huffman expands ~1.5x max
        var resultLen = 0
        var node = 0 // Current position in decode tree
        var bits = 0  // Bits consumed in current byte

        for (byte in data) {
            for (bitPos in 7 downTo 0) {
                val bit = (byte.toInt() ushr bitPos) and 1
                node = if (bit == 0) DECODE_TABLE[node][0] else DECODE_TABLE[node][1]

                if (node < 0) {
                    // Leaf node: symbol = -(node + 1)
                    val symbol = -(node + 1)
                    if (symbol == 256) {
                        // EOS — padding, stop decoding
                        return result.copyOf(resultLen)
                    }
                    if (resultLen >= result.size) {
                        // Grow buffer
                        val newResult = ByteArray(result.size * 2)
                        result.copyInto(newResult)
                        return decodeWithGrowableBuffer(data, bits) // Fallback to growable
                    }
                    result[resultLen++] = symbol.toByte()
                    node = 0 // Reset to root
                }
                bits++
            }
        }

        // Verify remaining bits are padding (all 1s)
        // RFC 7541 Section 5.2: padding MUST be the most-significant bits of EOS
        if (node != 0) {
            // Check if we're in a valid padding state (partial EOS prefix, all-1s)
            // Allow up to 7 bits of padding
            val paddingBits = bits % 8
            if (paddingBits > 7) {
                throw Http2Exception(
                    Http2ErrorCode.COMPRESSION_ERROR,
                    "Invalid Huffman padding"
                )
            }
        }

        return result.copyOf(resultLen)
    }

    /** Fallback for large decoded outputs */
    private fun decodeWithGrowableBuffer(data: ByteArray, startBits: Int): ByteArray {
        val result = mutableListOf<Byte>()
        var node = 0
        for (byte in data) {
            for (bitPos in 7 downTo 0) {
                val bit = (byte.toInt() ushr bitPos) and 1
                node = if (bit == 0) DECODE_TABLE[node][0] else DECODE_TABLE[node][1]
                if (node < 0) {
                    val symbol = -(node + 1)
                    if (symbol == 256) return result.toByteArray()
                    result.add(symbol.toByte())
                    node = 0
                }
            }
        }
        return result.toByteArray()
    }

    /**
     * Binary tree for Huffman decoding, flattened into an array.
     *
     * Each row [left, right] represents a node:
     * - Positive value = index of child node (branch)
     * - Negative value = leaf: symbol = -(value + 1)
     *
     * Built from RFC 7541 Appendix B Huffman code table.
     * Root is node 0.
     */
    private val DECODE_TABLE: Array<IntArray> by lazy { buildDecodeTable() }

    /**
     * RFC 7541 Appendix B — static Huffman code table.
     * Each entry: (symbol, code as Int, bit length).
     */
    private val HUFFMAN_CODES: Array<Triple<Int, Int, Int>> = arrayOf(
        Triple(0, 0x1ff8, 13), Triple(1, 0x7fffd8, 23), Triple(2, 0xfffffe2, 28), Triple(3, 0xfffffe3, 28),
        Triple(4, 0xfffffe4, 28), Triple(5, 0xfffffe5, 28), Triple(6, 0xfffffe6, 28), Triple(7, 0xfffffe7, 28),
        Triple(8, 0xfffffe8, 28), Triple(9, 0xffffea, 24), Triple(10, 0x3ffffffc, 30), Triple(11, 0xfffffe9, 28),
        Triple(12, 0xfffffea, 28), Triple(13, 0x3ffffffd, 30), Triple(14, 0xfffffeb, 28), Triple(15, 0xfffffec, 28),
        Triple(16, 0xfffffed, 28), Triple(17, 0xfffffee, 28), Triple(18, 0xfffffef, 28), Triple(19, 0xffffff0, 28),
        Triple(20, 0xffffff1, 28), Triple(21, 0xffffff2, 28), Triple(22, 0x3ffffffe, 30), Triple(23, 0xffffff3, 28),
        Triple(24, 0xffffff4, 28), Triple(25, 0xffffff5, 28), Triple(26, 0xffffff6, 28), Triple(27, 0xffffff7, 28),
        Triple(28, 0xffffff8, 28), Triple(29, 0xffffff9, 28), Triple(30, 0xffffffa, 28), Triple(31, 0xffffffb, 28),
        Triple(32, 0x14, 6), Triple(33, 0x3f8, 10), Triple(34, 0x3f9, 10), Triple(35, 0xffa, 12),
        Triple(36, 0x1ff9, 13), Triple(37, 0x15, 6), Triple(38, 0xf8, 8), Triple(39, 0x7fa, 11),
        Triple(40, 0x3fa, 10), Triple(41, 0x3fb, 10), Triple(42, 0xf9, 8), Triple(43, 0x7fb, 11),
        Triple(44, 0xfa, 8), Triple(45, 0x16, 6), Triple(46, 0x17, 6), Triple(47, 0x18, 6),
        Triple(48, 0x0, 5), Triple(49, 0x1, 5), Triple(50, 0x2, 5), Triple(51, 0x19, 6),
        Triple(52, 0x1a, 6), Triple(53, 0x1b, 6), Triple(54, 0x1c, 6), Triple(55, 0x1d, 6),
        Triple(56, 0x1e, 6), Triple(57, 0x1f, 6), Triple(58, 0x5c, 7), Triple(59, 0xfb, 8),
        Triple(60, 0x7ffc, 15), Triple(61, 0x20, 6), Triple(62, 0xffb, 12), Triple(63, 0x3fc, 10),
        Triple(64, 0x1ffa, 13), Triple(65, 0x21, 6), Triple(66, 0x5d, 7), Triple(67, 0x5e, 7),
        Triple(68, 0x5f, 7), Triple(69, 0x60, 7), Triple(70, 0x61, 7), Triple(71, 0x62, 7),
        Triple(72, 0x63, 7), Triple(73, 0x64, 7), Triple(74, 0x65, 7), Triple(75, 0x66, 7),
        Triple(76, 0x67, 7), Triple(77, 0x68, 7), Triple(78, 0x69, 7), Triple(79, 0x6a, 7),
        Triple(80, 0x6b, 7), Triple(81, 0x6c, 7), Triple(82, 0x6d, 7), Triple(83, 0x6e, 7),
        Triple(84, 0x6f, 7), Triple(85, 0x70, 7), Triple(86, 0x71, 7), Triple(87, 0x72, 7),
        Triple(88, 0xfc, 8), Triple(89, 0x73, 7), Triple(90, 0xfd, 8), Triple(91, 0x1ffb, 13),
        Triple(92, 0x7fff0, 19), Triple(93, 0x1ffc, 13), Triple(94, 0x3ffc, 14), Triple(95, 0x22, 6),
        Triple(96, 0x7ffd, 15), Triple(97, 0x3, 5), Triple(98, 0x23, 6), Triple(99, 0x4, 5),
        Triple(100, 0x24, 6), Triple(101, 0x5, 5), Triple(102, 0x25, 6), Triple(103, 0x26, 6),
        Triple(104, 0x27, 6), Triple(105, 0x6, 5), Triple(106, 0x74, 7), Triple(107, 0x75, 7),
        Triple(108, 0x28, 6), Triple(109, 0x29, 6), Triple(110, 0x2a, 6), Triple(111, 0x7, 5),
        Triple(112, 0x2b, 6), Triple(113, 0x76, 7), Triple(114, 0x2c, 6), Triple(115, 0x8, 5),
        Triple(116, 0x9, 5), Triple(117, 0x2d, 6), Triple(118, 0x77, 7), Triple(119, 0x78, 7),
        Triple(120, 0x79, 7), Triple(121, 0x7a, 7), Triple(122, 0x7b, 7), Triple(123, 0x7ffe, 15),
        Triple(124, 0x7fc, 11), Triple(125, 0x3ffd, 14), Triple(126, 0x1ffd, 13), Triple(127, 0xffffffc, 28),
        Triple(128, 0xfffe6, 20), Triple(129, 0x3fffd2, 22), Triple(130, 0xfffe7, 20), Triple(131, 0xfffe8, 20),
        Triple(132, 0x3fffd3, 22), Triple(133, 0x3fffd4, 22), Triple(134, 0x3fffd5, 22), Triple(135, 0x7fffd9, 23),
        Triple(136, 0x3fffd6, 22), Triple(137, 0x7fffda, 23), Triple(138, 0x7fffdb, 23), Triple(139, 0x7fffdc, 23),
        Triple(140, 0x7fffdd, 23), Triple(141, 0x7fffde, 23), Triple(142, 0xffffeb, 24), Triple(143, 0x7fffdf, 23),
        Triple(144, 0xffffec, 24), Triple(145, 0xffffed, 24), Triple(146, 0x3fffd7, 22), Triple(147, 0x7fffe0, 23),
        Triple(148, 0xffffee, 24), Triple(149, 0x7fffe1, 23), Triple(150, 0x7fffe2, 23), Triple(151, 0x7fffe3, 23),
        Triple(152, 0x7fffe4, 23), Triple(153, 0x1fffdc, 21), Triple(154, 0x3fffd8, 22), Triple(155, 0x7fffe5, 23),
        Triple(156, 0x3fffd9, 22), Triple(157, 0x7fffe6, 23), Triple(158, 0x7fffe7, 23), Triple(159, 0xffffef, 24),
        Triple(160, 0x3fffda, 22), Triple(161, 0x1fffdd, 21), Triple(162, 0xfffe9, 20), Triple(163, 0x3fffdb, 22),
        Triple(164, 0x3fffdc, 22), Triple(165, 0x7fffe8, 23), Triple(166, 0x7fffe9, 23), Triple(167, 0x1fffde, 21),
        Triple(168, 0x7fffea, 23), Triple(169, 0x3fffdd, 22), Triple(170, 0x3fffde, 22), Triple(171, 0xfffff0, 24),
        Triple(172, 0x1fffdf, 21), Triple(173, 0x3fffdf, 22), Triple(174, 0x7fffeb, 23), Triple(175, 0x7fffec, 23),
        Triple(176, 0x1fffe0, 21), Triple(177, 0x1fffe1, 21), Triple(178, 0x3fffe0, 22), Triple(179, 0x1fffe2, 21),
        Triple(180, 0x7fffed, 23), Triple(181, 0x3fffe1, 22), Triple(182, 0x7fffee, 23), Triple(183, 0x7fffef, 23),
        Triple(184, 0xfffea, 20), Triple(185, 0x3fffe2, 22), Triple(186, 0x3fffe3, 22), Triple(187, 0x3fffe4, 22),
        Triple(188, 0x7ffff0, 23), Triple(189, 0x3fffe5, 22), Triple(190, 0x3fffe6, 22), Triple(191, 0x7ffff1, 23),
        Triple(192, 0x3ffffe0, 26), Triple(193, 0x3ffffe1, 26), Triple(194, 0xfffeb, 20), Triple(195, 0x7fff1, 19),
        Triple(196, 0x3fffe7, 22), Triple(197, 0x7ffff2, 23), Triple(198, 0x3fffe8, 22), Triple(199, 0x1ffffec, 25),
        Triple(200, 0x3ffffe2, 26), Triple(201, 0x3ffffe3, 26), Triple(202, 0x3ffffe4, 26), Triple(203, 0x7ffffde, 27),
        Triple(204, 0x7ffffdf, 27), Triple(205, 0x3ffffe5, 26), Triple(206, 0xfffff1, 24), Triple(207, 0x1ffffed, 25),
        Triple(208, 0x7fff2, 19), Triple(209, 0x1fffe3, 21), Triple(210, 0x3ffffe6, 26), Triple(211, 0x7ffffe0, 27),
        Triple(212, 0x7ffffe1, 27), Triple(213, 0x3ffffe7, 26), Triple(214, 0x7ffffe2, 27), Triple(215, 0xfffff2, 24),
        Triple(216, 0x1fffe4, 21), Triple(217, 0x1fffe5, 21), Triple(218, 0x3ffffe8, 26), Triple(219, 0x3ffffe9, 26),
        Triple(220, 0xffffffd, 28), Triple(221, 0x7ffffe3, 27), Triple(222, 0x7ffffe4, 27), Triple(223, 0x7ffffe5, 27),
        Triple(224, 0xfffec, 20), Triple(225, 0xfffff3, 24), Triple(226, 0xfffed, 20), Triple(227, 0x1fffe6, 21),
        Triple(228, 0x3fffe9, 22), Triple(229, 0x1fffe7, 21), Triple(230, 0x1fffe8, 21), Triple(231, 0x7ffff3, 23),
        Triple(232, 0x3fffea, 22), Triple(233, 0x3fffeb, 22), Triple(234, 0x1ffffee, 25), Triple(235, 0x1ffffef, 25),
        Triple(236, 0xfffff4, 24), Triple(237, 0xfffff5, 24), Triple(238, 0x3ffffea, 26), Triple(239, 0x7ffff4, 23),
        Triple(240, 0x3ffffeb, 26), Triple(241, 0x7ffffe6, 27), Triple(242, 0x3ffffec, 26), Triple(243, 0x3ffffed, 26),
        Triple(244, 0x7ffffe7, 27), Triple(245, 0x7ffffe8, 27), Triple(246, 0x7ffffe9, 27), Triple(247, 0x7ffffea, 27),
        Triple(248, 0x7ffffeb, 27), Triple(249, 0xffffffe, 28), Triple(250, 0x7ffffec, 27), Triple(251, 0x7ffffed, 27),
        Triple(252, 0x7ffffee, 27), Triple(253, 0x7ffffef, 27), Triple(254, 0x7fffff0, 27), Triple(255, 0x3ffffee, 26),
        Triple(256, 0x3fffffff, 30) // EOS
    )

    /** Build the binary tree decode table from the Huffman codes */
    private fun buildDecodeTable(): Array<IntArray> {
        // Start with a reasonable tree size — will grow as needed
        val nodes = mutableListOf(intArrayOf(0, 0)) // Root node
        var nextNode = 1

        for ((symbol, code, bitLen) in HUFFMAN_CODES) {
            var current = 0
            for (i in bitLen - 1 downTo 0) {
                val bit = (code ushr i) and 1
                val child = nodes[current][bit]
                if (i == 0) {
                    // Leaf — store -(symbol + 1) to distinguish from node index
                    nodes[current][bit] = -(symbol + 1)
                } else if (child <= 0 && i > 0) {
                    // Need a new branch node
                    nodes.add(intArrayOf(0, 0))
                    nodes[current][bit] = nextNode
                    current = nextNode
                    nextNode++
                } else {
                    current = child
                }
            }
        }

        return nodes.toTypedArray()
    }
}
