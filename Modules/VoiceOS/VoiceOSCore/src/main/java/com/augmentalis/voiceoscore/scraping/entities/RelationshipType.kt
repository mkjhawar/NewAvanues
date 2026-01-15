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
    // Form relationships
    /** Label element provides description for input element */
    const val LABEL_FOR = "label_for"

    /** Button submits form (related to form input elements) */
    const val BUTTON_SUBMITS_FORM = "button_submits_form"

    /** Elements belong to same form group */
    const val FORM_GROUP_MEMBER = "form_group_member"

    /** Error message for an input element */
    const val ERROR_FOR = "error_for"

    // Navigation relationships
    /** Element navigates to another screen */
    const val NAVIGATES_TO = "navigates_to"

    /** Element navigates back */
    const val BACK_TO = "back_to"

    // Content relationships
    /** Element contains another element */
    const val CONTAINS = "contains"

    /** Element describes another element */
    const val DESCRIBES = "describes"

    /** Element expands to reveal more content */
    const val EXPANDS_TO = "expands_to"

    /** Element is a sibling of another element */
    const val SIBLING_OF = "sibling_of"

    // Action relationships
    /** Element triggers/opens another element */
    const val TRIGGERS = "triggers"

    /** Element toggles state of another element */
    const val TOGGLES = "toggles"

    /** Element filters content */
    const val FILTERS = "filters"
}
