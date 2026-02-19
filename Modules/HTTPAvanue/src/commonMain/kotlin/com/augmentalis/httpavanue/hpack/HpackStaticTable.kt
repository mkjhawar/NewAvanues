package com.augmentalis.httpavanue.hpack

/**
 * HPACK static table (RFC 7541 Appendix A) — 61 pre-defined header field entries
 */
object HpackStaticTable {
    data class Entry(val name: String, val value: String = "")

    val entries: List<Entry> = listOf(
        Entry(":authority"),                       // 1
        Entry(":method", "GET"),                   // 2
        Entry(":method", "POST"),                  // 3
        Entry(":path", "/"),                       // 4
        Entry(":path", "/index.html"),             // 5
        Entry(":scheme", "http"),                  // 6
        Entry(":scheme", "https"),                 // 7
        Entry(":status", "200"),                   // 8
        Entry(":status", "204"),                   // 9
        Entry(":status", "206"),                   // 10
        Entry(":status", "304"),                   // 11
        Entry(":status", "400"),                   // 12
        Entry(":status", "404"),                   // 13
        Entry(":status", "500"),                   // 14
        Entry("accept-charset"),                   // 15
        Entry("accept-encoding", "gzip, deflate"), // 16
        Entry("accept-language"),                  // 17
        Entry("accept-ranges"),                    // 18
        Entry("accept"),                           // 19
        Entry("access-control-allow-origin"),      // 20
        Entry("age"),                              // 21
        Entry("allow"),                            // 22
        Entry("authorization"),                    // 23
        Entry("cache-control"),                    // 24
        Entry("content-disposition"),              // 25
        Entry("content-encoding"),                 // 26
        Entry("content-language"),                 // 27
        Entry("content-length"),                   // 28
        Entry("content-location"),                 // 29
        Entry("content-range"),                    // 30
        Entry("content-type"),                     // 31
        Entry("cookie"),                           // 32
        Entry("date"),                             // 33
        Entry("etag"),                             // 34
        Entry("expect"),                           // 35
        Entry("expires"),                          // 36
        Entry("from"),                             // 37
        Entry("host"),                             // 38
        Entry("if-match"),                         // 39
        Entry("if-modified-since"),                // 40
        Entry("if-none-match"),                    // 41
        Entry("if-range"),                         // 42
        Entry("if-unmodified-since"),              // 43
        Entry("last-modified"),                    // 44
        Entry("link"),                             // 45
        Entry("location"),                         // 46
        Entry("max-forwards"),                     // 47
        Entry("proxy-authenticate"),               // 48
        Entry("proxy-authorization"),              // 49
        Entry("range"),                            // 50
        Entry("referer"),                          // 51
        Entry("refresh"),                          // 52
        Entry("retry-after"),                      // 53
        Entry("server"),                           // 54
        Entry("set-cookie"),                       // 55
        Entry("strict-transport-security"),         // 56
        Entry("transfer-encoding"),                // 57
        Entry("user-agent"),                       // 58
        Entry("vary"),                             // 59
        Entry("via"),                              // 60
        Entry("www-authenticate"),                 // 61
    )

    /** Find static table index for name+value match (1-based), or 0 if not found */
    fun findIndex(name: String, value: String): Int {
        for (i in entries.indices) {
            if (entries[i].name == name && entries[i].value == value) return i + 1
        }
        return 0
    }

    /** Find static table index for name-only match (1-based), or 0 if not found */
    fun findNameIndex(name: String): Int {
        for (i in entries.indices) {
            if (entries[i].name == name) return i + 1
        }
        return 0
    }

    fun get(index: Int): Entry? = entries.getOrNull(index - 1)
    val size: Int get() = entries.size
}
