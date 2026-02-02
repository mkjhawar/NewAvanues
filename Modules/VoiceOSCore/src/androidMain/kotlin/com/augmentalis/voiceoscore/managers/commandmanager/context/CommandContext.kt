/**
 * CommandContext.kt - Context-aware command system data models
 * Sealed class hierarchy for different context types
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 */

package com.augmentalis.voiceoscore.managers.commandmanager.context

import java.util.Calendar

/**
 * Sealed class representing different types of command contexts
 * Each context type provides specific information about the environment
 * in which a command is executed
 */
sealed class CommandContext {

    /**
     * App context - Identifies the currently active application
     * @param packageName The package name of the app (e.g., "com.google.android.gm")
     * @param activityName The specific activity within the app (optional)
     * @param appCategory The category of the app (e.g., "productivity", "social")
     */
    data class App(
        val packageName: String,
        val activityName: String? = null,
        val appCategory: AppCategory = AppCategory.UNKNOWN
    ) : CommandContext() {

        /**
         * Check if this context matches another app context
         * Supports wildcard matching (e.g., any activity in the same package)
         */
        fun matches(other: App): Boolean {
            if (packageName != other.packageName) return false
            if (activityName != null && other.activityName != null) {
                return activityName == other.activityName
            }
            return true // If either activity is null, consider it a wildcard match
        }
    }

    /**
     * Screen context - Identifies the current screen and its elements
     * @param screenId Unique identifier for this screen
     * @param elements List of key UI elements on the screen
     * @param hasEditableFields Whether the screen has editable text fields
     * @param hasScrollableContent Whether the screen has scrollable content
     */
    data class Screen(
        val screenId: String,
        val elements: List<String> = emptyList(),
        val hasEditableFields: Boolean = false,
        val hasScrollableContent: Boolean = false,
        val hasClickableElements: Boolean = false
    ) : CommandContext()

    /**
     * Time context - Identifies the current time of day and day of week
     * @param hour Hour of day (0-23)
     * @param dayOfWeek Day of week (Calendar.SUNDAY to Calendar.SATURDAY)
     * @param timeOfDay General time of day category
     */
    data class Time(
        val hour: Int,
        val dayOfWeek: Int,
        val timeOfDay: TimeOfDay = TimeOfDay.fromHour(hour)
    ) : CommandContext() {

        companion object {
            /**
             * Create a Time context from current system time
             */
            fun now(): Time {
                val calendar = Calendar.getInstance()
                return Time(
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                )
            }

            /**
             * Create a Time context for a specific time range
             */
            fun range(startHour: Int, endHour: Int): TimeRange {
                return TimeRange(startHour, endHour)
            }
        }

        /**
         * Check if this time is within a time range
         */
        fun isInRange(range: TimeRange): Boolean {
            return if (range.startHour <= range.endHour) {
                // Normal range (e.g., 9-17)
                hour in range.startHour..range.endHour
            } else {
                // Overnight range (e.g., 22-6)
                hour >= range.startHour || hour <= range.endHour
            }
        }

        /**
         * Check if this is a weekday
         */
        fun isWeekday(): Boolean {
            return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        }

        /**
         * Check if this is a weekend
         */
        fun isWeekend(): Boolean {
            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }
    }

    /**
     * Location context - Identifies the user's current location type
     * @param type The type of location
     * @param confidence Confidence in location detection (0.0 to 1.0)
     */
    data class Location(
        val type: LocationType,
        val confidence: Float = 1.0f
    ) : CommandContext()

    /**
     * Activity context - Identifies the user's current physical activity
     * @param type The type of activity
     * @param confidence Confidence in activity detection (0.0 to 1.0)
     */
    data class Activity(
        val type: ActivityType,
        val confidence: Float
    ) : CommandContext() {

        /**
         * Check if confidence is above threshold
         */
        fun isConfident(threshold: Float = 0.7f): Boolean {
            return confidence >= threshold
        }
    }

    /**
     * Composite context - Combines multiple contexts
     * Allows for complex context matching (e.g., "Gmail + Morning + Home")
     * @param contexts List of contexts that must all match
     */
    data class Composite(
        val contexts: List<CommandContext>
    ) : CommandContext() {

        /**
         * Get all contexts of a specific type
         */
        inline fun <reified T : CommandContext> getContextsOfType(): List<T> {
            return contexts.filterIsInstance<T>()
        }

        /**
         * Check if composite contains a specific context type
         */
        inline fun <reified T : CommandContext> hasContextType(): Boolean {
            return contexts.any { it is T }
        }

        /**
         * Flatten nested composite contexts
         */
        fun flatten(): List<CommandContext> {
            val flattened = mutableListOf<CommandContext>()
            for (context in contexts) {
                when (context) {
                    is Composite -> flattened.addAll(context.flatten())
                    else -> flattened.add(context)
                }
            }
            return flattened
        }
    }
}

/**
 * Location types for location-based context
 */
enum class LocationType {
    HOME,       // User's home location
    WORK,       // User's work location
    PUBLIC,     // Public place (restaurant, store, etc.)
    VEHICLE,    // Inside a vehicle
    OUTDOOR,    // Outdoor location (park, street, etc.)
    UNKNOWN     // Location type unknown
}

/**
 * Activity types for activity-based context
 * These map to Google Activity Recognition API activity types
 */
enum class ActivityType {
    WALKING,     // User is walking
    RUNNING,     // User is running
    DRIVING,     // User is driving
    STATIONARY,  // User is still/stationary
    CYCLING,     // User is cycling
    TILTING,     // Device is tilting
    UNKNOWN      // Activity unknown
}

/**
 * App categories for app-based context
 */
enum class AppCategory {
    PRODUCTIVITY,  // Email, calendar, notes, etc.
    SOCIAL,        // Social media apps
    MEDIA,         // Music, video, podcasts
    COMMUNICATION, // Messaging, phone, video calls
    BROWSER,       // Web browsers
    SHOPPING,      // Shopping apps
    NAVIGATION,    // Maps, navigation
    GAMES,         // Gaming apps
    SYSTEM,        // System apps
    UNKNOWN        // Category unknown
}

/**
 * Time of day categories
 */
enum class TimeOfDay {
    EARLY_MORNING,  // 5-8 AM
    MORNING,        // 8-12 PM
    AFTERNOON,      // 12-5 PM
    EVENING,        // 5-9 PM
    NIGHT,          // 9 PM-12 AM
    LATE_NIGHT;     // 12-5 AM

    companion object {
        /**
         * Determine time of day from hour
         */
        fun fromHour(hour: Int): TimeOfDay {
            return when (hour) {
                in 5..7 -> EARLY_MORNING
                in 8..11 -> MORNING
                in 12..16 -> AFTERNOON
                in 17..20 -> EVENING
                in 21..23 -> NIGHT
                else -> LATE_NIGHT
            }
        }
    }
}

/**
 * Time range helper class
 */
data class TimeRange(
    val startHour: Int,
    val endHour: Int
) {
    init {
        require(startHour in 0..23) { "Start hour must be 0-23" }
        require(endHour in 0..23) { "End hour must be 0-23" }
    }

    /**
     * Check if a given hour is in this range
     */
    fun contains(hour: Int): Boolean {
        return if (startHour <= endHour) {
            hour in startHour..endHour
        } else {
            // Overnight range
            hour >= startHour || hour <= endHour
        }
    }
}

/**
 * Context builder helper functions
 */
object ContextBuilder {

    /**
     * Build app context
     */
    fun app(packageName: String, activityName: String? = null, category: AppCategory = AppCategory.UNKNOWN): CommandContext.App {
        return CommandContext.App(packageName, activityName, category)
    }

    /**
     * Build screen context
     */
    fun screen(
        screenId: String,
        elements: List<String> = emptyList(),
        hasEditableFields: Boolean = false,
        hasScrollableContent: Boolean = false
    ): CommandContext.Screen {
        return CommandContext.Screen(screenId, elements, hasEditableFields, hasScrollableContent)
    }

    /**
     * Build time context for current time
     */
    fun timeNow(): CommandContext.Time {
        return CommandContext.Time.now()
    }

    /**
     * Build time context for specific time
     */
    fun time(hour: Int, dayOfWeek: Int): CommandContext.Time {
        return CommandContext.Time(hour, dayOfWeek)
    }

    /**
     * Build location context
     */
    fun location(type: LocationType, confidence: Float = 1.0f): CommandContext.Location {
        return CommandContext.Location(type, confidence)
    }

    /**
     * Build activity context
     */
    fun activity(type: ActivityType, confidence: Float): CommandContext.Activity {
        return CommandContext.Activity(type, confidence)
    }

    /**
     * Build composite context from multiple contexts
     */
    fun composite(vararg contexts: CommandContext): CommandContext.Composite {
        return CommandContext.Composite(contexts.toList())
    }
}
