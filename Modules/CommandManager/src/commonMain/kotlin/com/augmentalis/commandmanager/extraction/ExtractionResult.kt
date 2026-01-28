/**
 * ExtractionResult.kt - Result container for element extraction
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Result of an element extraction operation.
 *
 * @property elements The extracted elements
 * @property source The source that produced these elements
 * @property isSuccess Whether the extraction succeeded
 * @property errorMessage Error message if extraction failed
 */
data class ExtractionResult(
    val elements: List<ElementInfo>,
    val source: ExtractionSource,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {
    /** Number of elements extracted */
    val elementCount: Int get() = elements.size

    companion object {
        /**
         * Creates an empty successful result.
         */
        fun empty(source: ExtractionSource): ExtractionResult = ExtractionResult(
            elements = emptyList(),
            source = source,
            isSuccess = true
        )

        /**
         * Creates an error result.
         */
        fun error(message: String, source: ExtractionSource): ExtractionResult = ExtractionResult(
            elements = emptyList(),
            source = source,
            isSuccess = false,
            errorMessage = message
        )
    }
}
