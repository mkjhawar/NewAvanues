package com.augmentalis.httpavanue.hpack

/**
 * HPACK dynamic table (RFC 7541 Section 2.3.2)
 * FIFO table where new entries are added at the front and oldest entries evicted from the back.
 */
class HpackDynamicTable(private var maxSize: Int = 4096) {
    private val table = ArrayDeque<HpackStaticTable.Entry>()
    private var currentSize = 0

    /** Add entry to the front of the dynamic table, evicting oldest if necessary */
    fun add(name: String, value: String) {
        val entrySize = name.length + value.length + 32 // RFC 7541 Section 4.1
        // Evict entries if needed
        while (currentSize + entrySize > maxSize && table.isNotEmpty()) {
            val evicted = table.removeLast()
            currentSize -= (evicted.name.length + evicted.value.length + 32)
        }
        // If entry itself is larger than table, just clear (per spec)
        if (entrySize <= maxSize) {
            table.addFirst(HpackStaticTable.Entry(name, value))
            currentSize += entrySize
        }
    }

    /** Get entry by dynamic table index (0-based within dynamic table) */
    fun get(index: Int): HpackStaticTable.Entry? = table.getOrNull(index)

    /** Get entry by combined index (static table size + dynamic index) */
    fun getByAbsoluteIndex(index: Int): HpackStaticTable.Entry? {
        val dynamicIndex = index - HpackStaticTable.size - 1
        return if (dynamicIndex >= 0) table.getOrNull(dynamicIndex) else null
    }

    /** Update maximum table size, evicting entries as needed */
    fun setMaxSize(newMaxSize: Int) {
        maxSize = newMaxSize
        while (currentSize > maxSize && table.isNotEmpty()) {
            val evicted = table.removeLast()
            currentSize -= (evicted.name.length + evicted.value.length + 32)
        }
    }

    val size: Int get() = table.size
    val byteSize: Int get() = currentSize

    /** Find index in dynamic table for name+value match, or 0 */
    fun findIndex(name: String, value: String): Int {
        for (i in table.indices) {
            if (table[i].name == name && table[i].value == value) return HpackStaticTable.size + i + 1
        }
        return 0
    }

    /** Find index in dynamic table for name-only match, or 0 */
    fun findNameIndex(name: String): Int {
        for (i in table.indices) {
            if (table[i].name == name) return HpackStaticTable.size + i + 1
        }
        return 0
    }
}
