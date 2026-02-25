package com.augmentalis.nlu.ava.converter

import com.augmentalis.nlu.ava.model.AvaIntent
import com.augmentalis.nlu.nluLogInfo
import java.security.MessageDigest

/**
 * Converter: AvaIntent → SQLDelight insert parameters
 *
 * Pure data transformation - no I/O, no side effects
 * Converts .ava format (Universal v2.0) to database insert parameters
 *
 * Updated: Room removed, now creates insert parameter data class for SQLDelight
 */
object AvaToEntityConverter {

    private const val TAG = "AvaToEntityConverter"

    /**
     * Parameters for inserting an intent example via SQLDelight
     */
    data class IntentExampleInsertParams(
        val exampleHash: String,
        val intentId: String,
        val exampleText: String,
        val isPrimary: Boolean,
        val source: String,
        val formatVersion: String,
        val ipcCode: String?,
        val locale: String,
        val createdAt: Long,
        val usageCount: Long,
        val lastUsed: Long?
    )

    /**
     * Convert list of AvaIntents to IntentExampleInsertParams list
     */
    fun convertToInsertParams(intents: List<AvaIntent>): List<IntentExampleInsertParams> {
        val params = mutableListOf<IntentExampleInsertParams>()
        val timestamp = System.currentTimeMillis()

        intents.forEach { intent ->
            // Add canonical example
            params.add(
                createInsertParams(intent, intent.canonical, isPrimary = true, timestamp)
            )

            // Add synonym examples
            intent.synonyms.forEach { synonym ->
                params.add(
                    createInsertParams(intent, synonym, isPrimary = false, timestamp)
                )
            }
        }

        nluLogInfo(TAG, "Converted ${intents.size} intents to ${params.size} examples")
        return params
    }

    /**
     * Create single insert params from AvaIntent + example text
     */
    fun createInsertParams(
        intent: AvaIntent,
        exampleText: String,
        isPrimary: Boolean,
        timestamp: Long
    ): IntentExampleInsertParams {
        return IntentExampleInsertParams(
            exampleHash = generateHash(intent.id, exampleText),
            intentId = intent.id,
            exampleText = exampleText,
            isPrimary = isPrimary,
            source = "AVA_FILE_${intent.source}",
            formatVersion = "v2.0",  // All files now v2.0
            ipcCode = intent.getIPCCode(),
            locale = intent.locale,
            createdAt = timestamp,
            usageCount = 0,
            lastUsed = null
        )
    }

    /**
     * Generate MD5 hash for deduplication
     */
    fun generateHash(intentId: String, exampleText: String): String {
        val input = "$intentId|$exampleText"
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
