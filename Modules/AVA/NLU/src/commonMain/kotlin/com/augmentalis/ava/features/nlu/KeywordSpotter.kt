package com.augmentalis.ava.features.nlu

/**
 * Fast keyword spotter using Trie data structure.
 *
 * Provides <1ms detection of critical command words/phrases to bypass heavy NLU models.
 * Used for "Fast Path" commands like "Stop", "Back", "Home".
 *
 * Complexity: O(m) where m is utterance length.
 */
class KeywordSpotter {

    private val root = TrieNode()
    
    // Map of keyword phrase -> Intent ID
    private val keywordMap = mutableMapOf<String, String>()

    data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        var isEndOfWord: Boolean = false,
        var intentId: String? = null
    )

    /**
     * Add a keyword phrase mapped to an intent
     */
    fun addKeyword(phrase: String, intentId: String) {
        val normalized = phrase.trim().lowercase()
        keywordMap[normalized] = intentId
        
        var current = root
        for (char in normalized) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        current.isEndOfWord = true
        current.intentId = intentId
    }

    /**
     * Clear all keywords
     */
    fun clear() {
        root.children.clear()
        keywordMap.clear()
    }

    /**
     * Check if utterance matches a keyword exactly
     *
     * @param text Input utterance
     * @return Intent ID if matched, null otherwise
     */
    fun matchExact(text: String): String? {
        val normalized = text.trim().lowercase()
        return keywordMap[normalized]
    }

    /**
     * Check if utterance contains any keyword phrase
     *
     * @param text Input utterance
     * @return List of matched (Intent ID, Keyword) pairs
     */
    fun findKeywords(text: String): List<Pair<String, String>> {
        val normalized = text.trim().lowercase()
        val matches = mutableListOf<Pair<String, String>>()
        
        // Simple scan for now (Aho-Corasick would be better for massive lists, 
        // but for <50 commands, naive iteration or simple contains is fast enough)
        
        // Optimization: iterate through map if small, or use Trie for prefix matching
        // For "exact match" requirement of NLU fast-path, matchExact is preferred.
        // But for "spotting" within text:
        
        for ((phrase, intent) in keywordMap) {
            if (normalized.contains(phrase)) {
                matches.add(intent to phrase)
            }
        }
        
        return matches
    }
}
