package com.augmentalis.ava.core.data.util

import com.augmentalis.vuid.core.VUIDGenerator

/**
 * AVA-specific VUID helper using shared KMP library
 *
 * Provides convenience methods for generating VUIDs specific to AVA entity types.
 * Delegates to the shared VUIDGenerator from Common:VUID module.
 *
 * VUID Format: {module}:{typeAbbrev}:{hash8}
 * Example: ava:msg:a7f3e2c1
 */
object VuidHelper {
    // Convenience methods using shared generator
    fun randomMessageVUID(): String = VUIDGenerator.generateMessageVuid()
    fun randomConversationVUID(): String = VUIDGenerator.generateConversationVuid()
    fun randomDocumentVUID(): String = VUIDGenerator.generateDocumentVuid()
    fun randomChunkVUID(): String = VUIDGenerator.generateChunkVuid()
    fun randomMemoryVUID(): String = VUIDGenerator.generateMemoryVuid()
    fun randomDecisionVUID(): String = VUIDGenerator.generateDecisionVuid()
    fun randomLearningVUID(): String = VUIDGenerator.generateLearningVuid()
    fun randomIntentVUID(): String = VUIDGenerator.generateIntentVuid()
    fun randomClusterVUID(): String = VUIDGenerator.generateClusterVuid()
    fun randomBookmarkVUID(): String = VUIDGenerator.generateBookmarkVuid()
    fun randomAnnotationVUID(): String = VUIDGenerator.generateAnnotationVuid()
    fun randomFilterPresetVUID(): String = VUIDGenerator.generateFilterPresetVuid()
    fun randomUtteranceVUID(): String = VUIDGenerator.generateUtteranceVuid()
    fun randomDialogVUID(): String = VUIDGenerator.generateDialogVuid()

    // Legacy method for backward compatibility
    @Deprecated("Use specific type methods", ReplaceWith("randomMessageVUID()"))
    fun randomVUID(): String = VUIDGenerator.generate()
}
