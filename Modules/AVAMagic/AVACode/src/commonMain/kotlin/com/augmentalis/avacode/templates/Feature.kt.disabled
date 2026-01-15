package com.augmentalis.avacode.templates

/**
 * Represents feature flags that can be enabled/disabled in generated apps.
 *
 * Each template includes a default set of features, which can be customized
 * during app configuration.
 *
 * **Usage**:
 * ```kotlin
 * val app = generateApp {
 *     template = AppTemplate.ECOMMERCE
 *     features {
 *         enable(Feature.PRODUCT_CATALOG)
 *         enable(Feature.SHOPPING_CART)
 *         disable(Feature.WISHLIST)  // Optional feature
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
enum class Feature(val description: String, val category: FeatureCategory) {
    // ========== E-Commerce Features ==========
    PRODUCT_CATALOG(
        "Product listing with filtering and search",
        FeatureCategory.ECOMMERCE
    ),
    SHOPPING_CART(
        "Shopping cart with add/remove/update functionality",
        FeatureCategory.ECOMMERCE
    ),
    CHECKOUT(
        "Multi-step checkout workflow",
        FeatureCategory.ECOMMERCE
    ),
    PAYMENT_PROCESSING(
        "Payment integration (Stripe, PayPal, etc.)",
        FeatureCategory.ECOMMERCE
    ),
    ORDER_MANAGEMENT(
        "Order tracking and management",
        FeatureCategory.ECOMMERCE
    ),
    ORDER_TRACKING(
        "Customer order status tracking",
        FeatureCategory.ECOMMERCE
    ),
    USER_REVIEWS(
        "Product reviews and ratings",
        FeatureCategory.ECOMMERCE
    ),
    WISHLIST(
        "User wishlist functionality",
        FeatureCategory.ECOMMERCE
    ),
    INVENTORY_MANAGEMENT(
        "Stock tracking and low-stock alerts",
        FeatureCategory.ECOMMERCE
    ),
    DISCOUNT_CODES(
        "Coupon and discount code system",
        FeatureCategory.ECOMMERCE
    ),

    // ========== Task Management Features ==========
    TASK_BOARDS(
        "Task board with list/grid views",
        FeatureCategory.TASK_MANAGEMENT
    ),
    KANBAN_VIEW(
        "Drag-and-drop Kanban board",
        FeatureCategory.TASK_MANAGEMENT
    ),
    GANTT_CHART(
        "Project timeline Gantt chart",
        FeatureCategory.TASK_MANAGEMENT
    ),
    TIME_TRACKING(
        "Time tracking for tasks",
        FeatureCategory.TASK_MANAGEMENT
    ),
    TEAM_COLLABORATION(
        "Comments, mentions, and assignments",
        FeatureCategory.TASK_MANAGEMENT
    ),
    TASK_DEPENDENCIES(
        "Task dependency management",
        FeatureCategory.TASK_MANAGEMENT
    ),
    SPRINTS(
        "Sprint planning and tracking",
        FeatureCategory.TASK_MANAGEMENT
    ),
    MILESTONES(
        "Project milestone tracking",
        FeatureCategory.TASK_MANAGEMENT
    ),

    // ========== Social Media Features ==========
    NEWS_FEED(
        "Infinite-scroll news feed",
        FeatureCategory.SOCIAL_MEDIA
    ),
    USER_PROFILES(
        "User profiles with customization",
        FeatureCategory.SOCIAL_MEDIA
    ),
    FOLLOW_SYSTEM(
        "Follow/unfollow users",
        FeatureCategory.SOCIAL_MEDIA
    ),
    LIKES_COMMENTS(
        "Like and comment on posts",
        FeatureCategory.SOCIAL_MEDIA
    ),
    MEDIA_UPLOAD(
        "Upload images and videos",
        FeatureCategory.SOCIAL_MEDIA
    ),
    HASHTAGS(
        "Hashtag system for posts",
        FeatureCategory.SOCIAL_MEDIA
    ),
    DIRECT_MESSAGING(
        "Private messaging between users",
        FeatureCategory.SOCIAL_MEDIA
    ),
    STORIES(
        "Ephemeral stories feature",
        FeatureCategory.SOCIAL_MEDIA
    ),
    CONTENT_MODERATION(
        "Content moderation workflow",
        FeatureCategory.SOCIAL_MEDIA
    ),

    // ========== Learning Management Features ==========
    COURSE_CATALOG(
        "Course listing with categories",
        FeatureCategory.LEARNING
    ),
    VIDEO_PLAYER(
        "Video lessons with progress tracking",
        FeatureCategory.LEARNING
    ),
    QUIZZES(
        "Quiz engine with auto-grading",
        FeatureCategory.LEARNING
    ),
    PROGRESS_TRACKING(
        "Student progress dashboard",
        FeatureCategory.LEARNING
    ),
    CERTIFICATES(
        "Certificate generation on completion",
        FeatureCategory.LEARNING
    ),
    DISCUSSION_FORUMS(
        "Course discussion forums",
        FeatureCategory.LEARNING
    ),
    ASSIGNMENTS(
        "Assignment submission and grading",
        FeatureCategory.LEARNING
    ),
    LIVE_CLASSES(
        "Live video class integration",
        FeatureCategory.LEARNING
    ),
    LEARNING_PATHS(
        "Curated learning paths",
        FeatureCategory.LEARNING
    ),

    // ========== Healthcare Features ==========
    APPOINTMENT_SCHEDULING(
        "Book and manage appointments",
        FeatureCategory.HEALTHCARE
    ),
    CALENDAR_VIEW(
        "Calendar view for appointments",
        FeatureCategory.HEALTHCARE
    ),
    PATIENT_RECORDS(
        "Electronic health records",
        FeatureCategory.HEALTHCARE
    ),
    PRESCRIPTION_MANAGEMENT(
        "Prescription tracking",
        FeatureCategory.HEALTHCARE
    ),
    TELEMEDICINE(
        "Video consultation integration",
        FeatureCategory.HEALTHCARE
    ),
    APPOINTMENT_REMINDERS(
        "SMS/email appointment reminders",
        FeatureCategory.HEALTHCARE
    ),
    INSURANCE_MANAGEMENT(
        "Insurance information tracking",
        FeatureCategory.HEALTHCARE
    ),
    LAB_RESULTS(
        "Lab results portal",
        FeatureCategory.HEALTHCARE
    ),

    // ========== Common Features (All Templates) ==========
    AUTHENTICATION(
        "User authentication (email/social)",
        FeatureCategory.COMMON
    ),
    USER_PROFILES_COMMON(
        "Basic user profile management",
        FeatureCategory.COMMON
    ),
    NOTIFICATIONS(
        "Push and in-app notifications",
        FeatureCategory.COMMON
    ),
    SEARCH(
        "Global search functionality",
        FeatureCategory.COMMON
    ),
    ANALYTICS(
        "Usage analytics and tracking",
        FeatureCategory.COMMON
    ),
    SETTINGS(
        "App settings and preferences",
        FeatureCategory.COMMON
    ),
    DARK_MODE(
        "Dark mode theme support",
        FeatureCategory.COMMON
    ),
    OFFLINE_MODE(
        "Offline data sync",
        FeatureCategory.COMMON
    ),
    EXPORT_DATA(
        "Export data to CSV/PDF",
        FeatureCategory.COMMON
    ),
    MULTI_LANGUAGE(
        "Multi-language support (i18n)",
        FeatureCategory.COMMON
    ),
    ADMIN_PANEL(
        "Administrator dashboard",
        FeatureCategory.COMMON
    ),
    REPORTS(
        "Reporting and dashboards",
        FeatureCategory.COMMON
    );

    companion object {
        /**
         * Get all features for a specific category.
         */
        fun forCategory(category: FeatureCategory): List<Feature> =
            entries.filter { it.category == category }

        /**
         * Get default features for e-commerce template.
         */
        val ECOMMERCE_DEFAULTS = setOf(
            PRODUCT_CATALOG,
            SHOPPING_CART,
            CHECKOUT,
            PAYMENT_PROCESSING,
            ORDER_MANAGEMENT,
            ORDER_TRACKING,
            USER_REVIEWS,
            AUTHENTICATION,
            NOTIFICATIONS,
            SEARCH
        )

        /**
         * Get default features for task management template.
         */
        val TASK_MANAGEMENT_DEFAULTS = setOf(
            TASK_BOARDS,
            KANBAN_VIEW,
            TIME_TRACKING,
            TEAM_COLLABORATION,
            AUTHENTICATION,
            NOTIFICATIONS,
            SEARCH
        )

        /**
         * Get default features for social media template.
         */
        val SOCIAL_MEDIA_DEFAULTS = setOf(
            NEWS_FEED,
            USER_PROFILES,
            FOLLOW_SYSTEM,
            LIKES_COMMENTS,
            MEDIA_UPLOAD,
            HASHTAGS,
            DIRECT_MESSAGING,
            AUTHENTICATION,
            NOTIFICATIONS,
            SEARCH
        )

        /**
         * Get default features for LMS template.
         */
        val LMS_DEFAULTS = setOf(
            COURSE_CATALOG,
            VIDEO_PLAYER,
            QUIZZES,
            PROGRESS_TRACKING,
            CERTIFICATES,
            DISCUSSION_FORUMS,
            AUTHENTICATION,
            NOTIFICATIONS,
            SEARCH
        )

        /**
         * Get default features for healthcare template.
         */
        val HEALTHCARE_DEFAULTS = setOf(
            APPOINTMENT_SCHEDULING,
            CALENDAR_VIEW,
            PATIENT_RECORDS,
            PRESCRIPTION_MANAGEMENT,
            APPOINTMENT_REMINDERS,
            AUTHENTICATION,
            NOTIFICATIONS,
            SEARCH
        )
    }
}

/**
 * Feature categories for organizing features by template type.
 */
enum class FeatureCategory {
    ECOMMERCE,
    TASK_MANAGEMENT,
    SOCIAL_MEDIA,
    LEARNING,
    HEALTHCARE,
    COMMON
}
