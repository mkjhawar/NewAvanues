package com.augmentalis.magicui.templates

/**
 * Metadata about an app template.
 *
 * Provides information about the template including its name, description,
 * author, version, and supported platforms.
 *
 * @property name Template display name (e.g., "E-Commerce Store")
 * @property id Unique template identifier (e.g., "ecommerce")
 * @property description Brief description of what the template generates
 * @property author Template author/organization
 * @property version Template version (SemVer format)
 * @property platforms Supported platforms for this template
 * @property estimatedLOC Estimated lines of code generated
 * @property generationTime Estimated time to generate app (minutes)
 * @property tags Tags for template categorization and search
 *
 * @since 1.0.0
 */
data class TemplateMetadata(
    val name: String,
    val id: String,
    val description: String,
    val author: String = "IDEAMagic",
    val version: String = "1.0.0",
    val platforms: Set<Platform> = setOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
    val estimatedLOC: Int = 8000,
    val generationTime: Int = 5,  // minutes
    val tags: Set<String> = emptySet()
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank" }
        require(id.isNotBlank()) { "Template ID cannot be blank" }
        require(id.matches(Regex("[a-z0-9_-]+"))) {
            "Template ID must contain only lowercase letters, numbers, hyphens, and underscores"
        }
        require(description.isNotBlank()) { "Template description cannot be blank" }
        require(version.matches(Regex("\\d+\\.\\d+\\.\\d+"))) {
            "Template version must follow SemVer format (e.g., 1.0.0)"
        }
        require(platforms.isNotEmpty()) { "Template must support at least one platform" }
        require(estimatedLOC > 0) { "Estimated LOC must be positive" }
        require(generationTime > 0) { "Generation time must be positive" }
    }

    /**
     * Returns a formatted string with template information.
     */
    fun toDisplayString(): String = buildString {
        appendLine("Template: $name (v$version)")
        appendLine("ID: $id")
        appendLine("Description: $description")
        appendLine("Author: $author")
        appendLine("Platforms: ${platforms.joinToString(", ") { it.displayName }}")
        appendLine("Estimated LOC: $estimatedLOC")
        appendLine("Generation Time: ~$generationTime minutes")
        if (tags.isNotEmpty()) {
            appendLine("Tags: ${tags.joinToString(", ")}")
        }
    }

    companion object {
        /**
         * E-Commerce template metadata.
         */
        val ECOMMERCE = TemplateMetadata(
            name = "E-Commerce Store",
            id = "ecommerce",
            description = "Full-featured online store with product catalog, shopping cart, checkout, and order management",
            estimatedLOC = 8000,
            generationTime = 8,
            tags = setOf("retail", "shopping", "payments", "products", "orders")
        )

        /**
         * Task Management template metadata.
         */
        val TASK_MANAGEMENT = TemplateMetadata(
            name = "Task & Project Manager",
            id = "task-management",
            description = "Project management app with Kanban boards, Gantt charts, time tracking, and team collaboration",
            estimatedLOC = 7500,
            generationTime = 7,
            tags = setOf("productivity", "projects", "tasks", "kanban", "collaboration")
        )

        /**
         * Social Media template metadata.
         */
        val SOCIAL_MEDIA = TemplateMetadata(
            name = "Social Network",
            id = "social-media",
            description = "Social networking platform with news feed, profiles, follows, likes, comments, and media uploads",
            estimatedLOC = 9000,
            generationTime = 9,
            tags = setOf("social", "networking", "posts", "media", "community")
        )

        /**
         * Learning Management System template metadata.
         */
        val LMS = TemplateMetadata(
            name = "Learning Platform",
            id = "lms",
            description = "Online learning platform with courses, video lessons, quizzes, progress tracking, and certificates",
            estimatedLOC = 10000,
            generationTime = 10,
            tags = setOf("education", "courses", "learning", "videos", "quizzes")
        )

        /**
         * Healthcare template metadata.
         */
        val HEALTHCARE = TemplateMetadata(
            name = "Appointment Booking System",
            id = "healthcare",
            description = "Healthcare appointment system with booking, patient records, prescriptions, and telemedicine",
            estimatedLOC = 9500,
            generationTime = 9,
            tags = setOf("healthcare", "appointments", "medical", "patients", "HIPAA")
        )

        /**
         * All available template metadata.
         */
        val ALL = listOf(ECOMMERCE, TASK_MANAGEMENT, SOCIAL_MEDIA, LMS, HEALTHCARE)

        /**
         * Find template metadata by ID.
         */
        fun findById(id: String): TemplateMetadata? = ALL.find { it.id == id }
    }
}

/**
 * Supported platforms for app generation.
 */
enum class Platform(val displayName: String) {
    ANDROID("Android"),
    IOS("iOS"),
    DESKTOP("Desktop (JVM)"),
    WEB("Web (JS)");

    companion object {
        /**
         * All mobile platforms.
         */
        val MOBILE = setOf(ANDROID, IOS)

        /**
         * All platforms.
         */
        val ALL = entries.toSet()
    }
}
