package com.augmentalis.ava.core.data.util

import com.augmentalis.avid.core.AvidGenerator

/**
 * AVA-specific AVID helper using shared KMP library
 *
 * Provides convenience methods for generating AVIDs specific to AVA entity types.
 * Delegates to the shared AvidGenerator from Modules:AVID module.
 *
 * AVID Format: {module}:{typeAbbrev}:{hash8}
 * Example: ava:msg:a7f3e2c1
 */
object AvidHelper {
    // Convenience methods using shared generator
    fun randomMessageAVID(): String = AvidGenerator.generateMessageAvid()
    fun randomConversationAVID(): String = AvidGenerator.generateConversationAvid()
    fun randomDocumentAVID(): String = AvidGenerator.generateDocumentAvid()
    fun randomChunkAVID(): String = AvidGenerator.generateChunkAvid()
    fun randomMemoryAVID(): String = AvidGenerator.generateMemoryAvid()
    fun randomDecisionAVID(): String = AvidGenerator.generateDecisionAvid()
    fun randomLearningAVID(): String = AvidGenerator.generateLearningAvid()
    fun randomIntentAVID(): String = AvidGenerator.generateIntentAvid()
    fun randomClusterAVID(): String = AvidGenerator.generateClusterAvid()
    fun randomBookmarkAVID(): String = AvidGenerator.generateBookmarkAvid()
    fun randomAnnotationAVID(): String = AvidGenerator.generateAnnotationAvid()
    fun randomFilterPresetAVID(): String = AvidGenerator.generateFilterPresetAvid()
    fun randomUtteranceAVID(): String = AvidGenerator.generateUtteranceAvid()
    fun randomDialogAVID(): String = AvidGenerator.generateDialogAvid()

    // Legacy method for backward compatibility
    @Deprecated("Use specific type methods", ReplaceWith("randomMessageAVID()"))
    fun randomAVID(): String = AvidGenerator.generate()
}

/**
 * Legacy VuidHelper - Deprecated
 * Use AvidHelper instead
 */
@Deprecated("Use AvidHelper instead", ReplaceWith("AvidHelper"))
typealias VuidHelper = AvidHelper
