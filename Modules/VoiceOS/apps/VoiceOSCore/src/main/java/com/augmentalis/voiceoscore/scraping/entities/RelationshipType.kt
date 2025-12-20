/**
 * RelationshipType.kt - Element relationship type constants
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Relationship type constants
 *
 * Defines the types of semantic relationships between UI elements.
 */
object RelationshipType {
    /** Label element provides description for input element */
    const val LABEL_FOR = "label_for"

    /** Button submits form (related to form input elements) */
    const val BUTTON_SUBMITS_FORM = "button_submits_form"

    /** Element triggers/opens another element */
    const val TRIGGERS = "triggers"

    /** Element navigates to another screen */
    const val NAVIGATES_TO = "navigates_to"

    /** Element contains another element */
    const val CONTAINS = "contains"

    /** Element is a sibling of another element */
    const val SIBLING_OF = "sibling_of"
}
