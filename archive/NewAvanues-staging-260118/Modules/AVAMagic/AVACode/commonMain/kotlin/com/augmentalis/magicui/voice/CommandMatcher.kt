package com.augmentalis.avamagic.voice

/**
 * Advanced fuzzy matching for voice commands.
 */
object CommandMatcher {

    /**
     * Calculate Levenshtein distance between two strings.
     */
    fun levenshteinDistance(a: String, b: String): Int {
        val costs = IntArray(b.length + 1) { it }

        for (i in 1..a.length) {
            var lastValue = i
            for (j in 1..b.length) {
                val newValue = if (a[i - 1] == b[j - 1]) {
                    costs[j - 1]
                } else {
                    1 + minOf(costs[j - 1], lastValue, costs[j])
                }
                costs[j - 1] = lastValue
                lastValue = newValue
            }
            costs[b.length] = lastValue
        }

        return costs[b.length]
    }

    /**
     * Calculate similarity score (0.0 to 1.0).
     */
    fun similarity(a: String, b: String): Float {
        val distance = levenshteinDistance(a, b)
        val maxLen = maxOf(a.length, b.length)
        return if (maxLen > 0) {
            1.0f - (distance.toFloat() / maxLen)
        } else {
            1.0f
        }
    }

    /**
     * Check if query contains trigger phrase.
     */
    fun contains(query: String, trigger: String): Boolean {
        return query.lowercase().contains(trigger.lowercase())
    }

    /**
     * Word overlap similarity.
     */
    fun wordOverlap(query: String, trigger: String): Float {
        val queryWords = query.lowercase().split(" ").filter { it.isNotBlank() }.toSet()
        val triggerWords = trigger.lowercase().split(" ").filter { it.isNotBlank() }.toSet()

        if (queryWords.isEmpty() && triggerWords.isEmpty()) return 1.0f
        if (queryWords.isEmpty() || triggerWords.isEmpty()) return 0.0f

        val intersection = queryWords.intersect(triggerWords).size
        val union = queryWords.union(triggerWords).size

        return intersection.toFloat() / union.toFloat()
    }
}
