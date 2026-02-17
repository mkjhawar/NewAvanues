package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Pre-built session template — instant productivity.
 *
 * Templates define a session layout with pre-configured frames
 * so users don't have to build from scratch. Each template targets
 * a specific use case (inspection, research, training, etc.).
 *
 * Part of the Modules/SessionTemplates reusable module.
 */
@Serializable
data class SessionTemplate(
    /** Unique template identifier */
    val id: String,
    /** Display name */
    val name: String,
    /** Short description of the use case */
    val description: String,
    /** Icon name (Material icon identifier) */
    val iconName: String = "dashboard",
    /** Category for organization */
    val category: TemplateCategory,
    /** Layout mode for the created session */
    val layoutMode: LayoutMode,
    /** Frame definitions (content type + default config) */
    val frameDefinitions: List<FrameDefinition>,
    /** Cross-frame links to auto-create */
    val autoLinks: List<CrossFrameLink> = emptyList(),
    /** Whether this is a built-in template (not user-created) */
    val isBuiltIn: Boolean = true,
)

/**
 * A frame definition within a template — describes what frame to create.
 */
@Serializable
data class FrameDefinition(
    /** Default frame title */
    val title: String,
    /** Content type to initialize */
    val contentType: String,
    /** Default content configuration JSON */
    val defaultContentJson: String = "{}",
    /** Default frame state */
    val defaultState: FrameState = FrameState(),
)

@Serializable
enum class TemplateCategory {
    /** Field work: inspections, maintenance, audits */
    FIELD_WORK,
    /** Research: multi-source comparison, literature review */
    RESEARCH,
    /** Training: instructional workflows, guided procedures */
    TRAINING,
    /** Meeting: notes, camera, whiteboard */
    MEETING,
    /** Medical: patient records, imaging, notes */
    MEDICAL,
    /** Development: code, terminal, docs, browser */
    DEVELOPMENT,
    /** General: user-created custom templates */
    CUSTOM,
}

/**
 * Built-in templates — shipped with the app.
 */
object BuiltInTemplates {

    val FIELD_INSPECTION = SessionTemplate(
        id = "tmpl_field_inspection",
        name = "Field Inspection",
        description = "Camera + checklist form + reference PDF + notes",
        iconName = "engineering",
        category = TemplateCategory.FIELD_WORK,
        layoutMode = LayoutMode.GRID,
        frameDefinitions = listOf(
            FrameDefinition("Camera", FrameContent.TYPE_CAMERA),
            FrameDefinition("Inspection Form", FrameContent.TYPE_FORM),
            FrameDefinition("Reference Manual", FrameContent.TYPE_PDF),
            FrameDefinition("Field Notes", FrameContent.TYPE_NOTE),
        ),
    )

    val RESEARCH = SessionTemplate(
        id = "tmpl_research",
        name = "Research",
        description = "3 browser tabs + note-taking with cross-frame search",
        iconName = "science",
        category = TemplateCategory.RESEARCH,
        layoutMode = LayoutMode.SPLIT_LEFT,
        frameDefinitions = listOf(
            FrameDefinition("Source 1", FrameContent.TYPE_WEB),
            FrameDefinition("Source 2", FrameContent.TYPE_WEB),
            FrameDefinition("Source 3", FrameContent.TYPE_WEB),
            FrameDefinition("Research Notes", FrameContent.TYPE_NOTE),
        ),
    )

    val TRAINING_WORKFLOW = SessionTemplate(
        id = "tmpl_training",
        name = "Training Workflow",
        description = "Step-by-step video + PDF manual + completion form",
        iconName = "school",
        category = TemplateCategory.TRAINING,
        layoutMode = LayoutMode.WORKFLOW,
        frameDefinitions = listOf(
            FrameDefinition("Training Video", FrameContent.TYPE_VIDEO),
            FrameDefinition("Procedure Manual", FrameContent.TYPE_PDF),
            FrameDefinition("Completion Form", FrameContent.TYPE_FORM),
            FrameDefinition("Sign-off", FrameContent.TYPE_SIGNATURE),
        ),
    )

    val MEETING = SessionTemplate(
        id = "tmpl_meeting",
        name = "Meeting",
        description = "Camera + voice notes + whiteboard + action items",
        iconName = "groups",
        category = TemplateCategory.MEETING,
        layoutMode = LayoutMode.GRID,
        frameDefinitions = listOf(
            FrameDefinition("Camera", FrameContent.TYPE_CAMERA),
            FrameDefinition("Voice Notes", FrameContent.TYPE_VOICE_NOTE),
            FrameDefinition("Whiteboard", FrameContent.TYPE_WHITEBOARD),
            FrameDefinition("AI Summary", FrameContent.TYPE_AI_SUMMARY),
        ),
    )

    val REMOTE_ASSIST = SessionTemplate(
        id = "tmpl_remote_assist",
        name = "Remote Assistance",
        description = "Screen cast + camera + voice + whiteboard for remote support",
        iconName = "support_agent",
        category = TemplateCategory.FIELD_WORK,
        layoutMode = LayoutMode.SPLIT_LEFT,
        frameDefinitions = listOf(
            FrameDefinition("Remote Screen", FrameContent.TYPE_SCREEN_CAST),
            FrameDefinition("My Camera", FrameContent.TYPE_CAMERA),
            FrameDefinition("Voice Channel", FrameContent.TYPE_VOICE),
            FrameDefinition("Annotations", FrameContent.TYPE_WHITEBOARD),
        ),
    )

    val DEVELOPER = SessionTemplate(
        id = "tmpl_developer",
        name = "Developer",
        description = "Browser + terminal + docs + notes for development",
        iconName = "code",
        category = TemplateCategory.DEVELOPMENT,
        layoutMode = LayoutMode.FREEFORM,
        frameDefinitions = listOf(
            FrameDefinition("Browser", FrameContent.TYPE_WEB),
            FrameDefinition("Console", FrameContent.TYPE_TERMINAL),
            FrameDefinition("Documentation", FrameContent.TYPE_PDF),
            FrameDefinition("Notes", FrameContent.TYPE_NOTE),
        ),
    )

    val ALL = listOf(
        FIELD_INSPECTION, RESEARCH, TRAINING_WORKFLOW,
        MEETING, REMOTE_ASSIST, DEVELOPER,
    )
}
