package com.augmentalis.ava.core.data.util

import com.augmentalis.avid.AvidGenerator

/**
 * AVA-specific AVID helper using shared KMP library
 *
 * Provides convenience methods for generating AVIDs specific to AVA entity types.
 * Delegates to the shared AvidGenerator from Modules:AVID.
 *
 * AVID Format: AVID-{platform}-{sequence}
 * Example: AVID-A-000001
 */
object VuidHelper {
    // All entity types use the unified AVID format
    fun randomMessageVUID(): String = AvidGenerator.generateMessageId()
    fun randomConversationVUID(): String = AvidGenerator.generate()
    fun randomDocumentVUID(): String = AvidGenerator.generate()
    fun randomChunkVUID(): String = AvidGenerator.generate()
    fun randomMemoryVUID(): String = AvidGenerator.generate()
    fun randomDecisionVUID(): String = AvidGenerator.generate()
    fun randomLearningVUID(): String = AvidGenerator.generate()
    fun randomIntentVUID(): String = AvidGenerator.generate()
    fun randomClusterVUID(): String = AvidGenerator.generate()
    fun randomBookmarkVUID(): String = AvidGenerator.generate()
    fun randomAnnotationVUID(): String = AvidGenerator.generate()
    fun randomFilterPresetVUID(): String = AvidGenerator.generate()
    fun randomUtteranceVUID(): String = AvidGenerator.generate()
    fun randomDialogVUID(): String = AvidGenerator.generate()

    // Legacy method for backward compatibility
    @Deprecated("Use specific type methods", ReplaceWith("randomMessageVUID()"))
    fun randomVUID(): String = AvidGenerator.generate()
}
