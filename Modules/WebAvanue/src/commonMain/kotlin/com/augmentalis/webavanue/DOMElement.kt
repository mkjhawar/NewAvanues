package com.augmentalis.webavanue

import kotlinx.serialization.Serializable

/**
 * Represents a scraped DOM element from WebAvanue.
 *
 * Contains all information needed for VoiceOS command generation.
 */
@Serializable
data class DOMElement(
    /** Unique VoiceOS ID for this element (e.g., "vos_42") */
    val id: String,

    /** HTML tag name (lowercase, e.g., "button", "a", "input") */
    val tag: String,

    /** Element type for command generation (button, link, input, etc.) */
    val type: String,

    /** Accessible name (aria-label, label text, or inner text) */
    val name: String,

    /** ARIA role attribute */
    val role: String,

    /** aria-label attribute if present */
    val ariaLabel: String,

    /** Placeholder text for inputs */
    val placeholder: String,

    /** Current value for inputs */
    val value: String,

    /** href for links */
    val href: String,

    /** CSS selector for targeting */
    val selector: String,

    /** XPath for targeting */
    val xpath: String,

    /** Bounding box in page coordinates */
    val bounds: ElementBounds,

    /** Depth in DOM tree */
    val depth: Int,

    /** Whether element is disabled */
    val isDisabled: Boolean,

    /** Whether checkbox/radio is checked */
    val isChecked: Boolean,

    /** Whether expandable element is expanded */
    val isExpanded: Boolean,

    /** aria-haspopup value */
    val hasPopup: String,

    /** Input type for input elements */
    val inputType: String
)

/**
 * Bounding box for an element.
 */
@Serializable
data class ElementBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int
)

/**
 * Viewport information.
 */
@Serializable
data class ViewportInfo(
    val width: Int,
    val height: Int,
    val scrollX: Int,
    val scrollY: Int,
    val pageWidth: Int,
    val pageHeight: Int
)

/**
 * Complete DOM scrape result.
 */
@Serializable
data class DOMScrapeResult(
    /** Page URL */
    val url: String,

    /** Page title */
    val title: String,

    /** Timestamp when scraped */
    val timestamp: Long,

    /** Viewport information */
    val viewport: ViewportInfo,

    /** List of interactive elements */
    val elements: List<DOMElement>,

    /** Total element count */
    val elementCount: Int
)

/**
 * Response wrapper from JS bridge.
 */
@Serializable
data class ScraperResponse(
    val scrape: DOMScrapeResult,
    val version: String
)

/**
 * Action result from JS bridge.
 */
@Serializable
data class ActionResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)
