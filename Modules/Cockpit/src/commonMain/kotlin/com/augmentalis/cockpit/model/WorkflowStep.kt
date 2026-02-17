package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * A step in a workflow sequence.
 *
 * Workflows are sequential instructional procedures where each step
 * is linked to a specific frame. Used for guided tasks like equipment
 * inspection, assembly procedures, or training sequences.
 */
@Serializable
data class WorkflowStep(
    /** Unique identifier */
    val id: String,
    /** Parent session ID */
    val sessionId: String,
    /** ID of the frame this step displays */
    val frameId: String,
    /** Order in the workflow (0-based) */
    val stepNumber: Int,
    /** Step title (e.g., "Step 1: Open valve") */
    val name: String,
    /** Detailed instructions for this step */
    val description: String = "",
)
