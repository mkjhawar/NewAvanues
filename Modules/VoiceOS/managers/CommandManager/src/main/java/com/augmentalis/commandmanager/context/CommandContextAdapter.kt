/**
 * CommandContextAdapter - Migration adapter for CommandContext
 *
 * Provides conversion between the legacy sealed class CommandContext
 * and the new unified data class CommandContext for KMP compatibility.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.commandmanager.context

import java.util.Calendar
import com.augmentalis.commandmanager.CommandContext as UnifiedCommandContext

/**
 * Adapter for migrating between legacy sealed class and unified data class.
 */
@Suppress("DEPRECATION")
object CommandContextAdapter {

    /**
     * Convert legacy sealed class context to unified data class
     */
    fun toUnified(legacyContext: CommandContext): UnifiedCommandContext {
        return when (legacyContext) {
            is CommandContext.App -> UnifiedCommandContext(
                packageName = legacyContext.packageName,
                activityName = legacyContext.activityName,
                appCategory = legacyContext.appCategory.toUnifiedCategory()
            )
            is CommandContext.Screen -> UnifiedCommandContext(
                screenElements = legacyContext.elements,
                hasEditableFields = legacyContext.hasEditableFields,
                hasScrollableContent = legacyContext.hasScrollableContent,
                hasClickableElements = legacyContext.hasClickableElements,
                customData = mapOf("screenId" to legacyContext.screenId)
            )
            is CommandContext.Time -> UnifiedCommandContext(
                timeOfDay = legacyContext.timeOfDay.toUnifiedTimeOfDay(),
                hour = legacyContext.hour,
                dayOfWeek = legacyContext.dayOfWeek
            )
            is CommandContext.Location -> UnifiedCommandContext(
                userLocation = legacyContext.type.toUnifiedLocation(),
                locationConfidence = legacyContext.confidence
            )
            is CommandContext.Activity -> UnifiedCommandContext(
                activityType = legacyContext.type.toUnifiedActivity(),
                activityConfidence = legacyContext.confidence
            )
            is CommandContext.Composite -> {
                // Merge all composite contexts into one
                mergeComposite(legacyContext)
            }
        }
    }

    /**
     * Convert unified data class to legacy App context
     */
    fun toLegacyApp(unified: UnifiedCommandContext): CommandContext.App? {
        val packageName = unified.packageName ?: return null
        return CommandContext.App(
            packageName = packageName,
            activityName = unified.activityName,
            appCategory = unified.appCategory?.toAppCategory() ?: AppCategory.UNKNOWN
        )
    }

    /**
     * Convert unified data class to legacy Screen context
     */
    fun toLegacyScreen(unified: UnifiedCommandContext): CommandContext.Screen {
        val screenId = unified.customData["screenId"]?.toString() ?: "unknown"
        return CommandContext.Screen(
            screenId = screenId,
            elements = unified.screenElements,
            hasEditableFields = unified.hasEditableFields,
            hasScrollableContent = unified.hasScrollableContent,
            hasClickableElements = unified.hasClickableElements
        )
    }

    /**
     * Convert unified data class to legacy Time context
     */
    fun toLegacyTime(unified: UnifiedCommandContext): CommandContext.Time {
        val hour = unified.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = unified.dayOfWeek ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return CommandContext.Time(
            hour = hour,
            dayOfWeek = dayOfWeek,
            timeOfDay = unified.timeOfDay?.toTimeOfDay() ?: TimeOfDay.fromHour(hour)
        )
    }

    /**
     * Convert unified data class to legacy Location context
     */
    fun toLegacyLocation(unified: UnifiedCommandContext): CommandContext.Location? {
        val location = unified.userLocation ?: return null
        return CommandContext.Location(
            type = location.toLocationType(),
            confidence = unified.locationConfidence
        )
    }

    /**
     * Convert unified data class to legacy Activity context
     */
    fun toLegacyActivity(unified: UnifiedCommandContext): CommandContext.Activity? {
        val activity = unified.activityType ?: return null
        return CommandContext.Activity(
            type = activity.toActivityType(),
            confidence = unified.activityConfidence
        )
    }

    /**
     * Create current time context (unified format)
     */
    fun currentTimeContext(): UnifiedCommandContext {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return UnifiedCommandContext(
            timeOfDay = UnifiedCommandContext.TimeOfDay.fromHour(hour),
            hour = hour,
            dayOfWeek = dayOfWeek
        )
    }

    /**
     * Merge composite context into unified format
     */
    private fun mergeComposite(composite: CommandContext.Composite): UnifiedCommandContext {
        var result = UnifiedCommandContext()

        for (context in composite.flatten()) {
            val converted = toUnified(context)
            result = result.copy(
                packageName = result.packageName ?: converted.packageName,
                activityName = result.activityName ?: converted.activityName,
                appCategory = result.appCategory ?: converted.appCategory,
                screenElements = if (result.screenElements.isEmpty()) converted.screenElements else result.screenElements,
                hasEditableFields = result.hasEditableFields || converted.hasEditableFields,
                hasScrollableContent = result.hasScrollableContent || converted.hasScrollableContent,
                hasClickableElements = result.hasClickableElements || converted.hasClickableElements,
                userLocation = result.userLocation ?: converted.userLocation,
                locationConfidence = if (converted.userLocation != null) converted.locationConfidence else result.locationConfidence,
                activityType = result.activityType ?: converted.activityType,
                activityConfidence = if (converted.activityType != null) converted.activityConfidence else result.activityConfidence,
                timeOfDay = result.timeOfDay ?: converted.timeOfDay,
                hour = result.hour ?: converted.hour,
                dayOfWeek = result.dayOfWeek ?: converted.dayOfWeek,
                customData = result.customData + converted.customData
            )
        }

        return result
    }

    // Extension functions for type conversion

    private fun AppCategory.toUnifiedCategory(): String = when (this) {
        AppCategory.PRODUCTIVITY -> UnifiedCommandContext.AppCategories.PRODUCTIVITY
        AppCategory.SOCIAL -> UnifiedCommandContext.AppCategories.SOCIAL
        AppCategory.MEDIA -> UnifiedCommandContext.AppCategories.MEDIA
        AppCategory.COMMUNICATION -> UnifiedCommandContext.AppCategories.COMMUNICATION
        AppCategory.BROWSER -> UnifiedCommandContext.AppCategories.BROWSER
        AppCategory.SHOPPING -> UnifiedCommandContext.AppCategories.SHOPPING
        AppCategory.NAVIGATION -> UnifiedCommandContext.AppCategories.NAVIGATION
        AppCategory.GAMES -> UnifiedCommandContext.AppCategories.GAMES
        AppCategory.SYSTEM -> UnifiedCommandContext.AppCategories.SYSTEM
        AppCategory.UNKNOWN -> UnifiedCommandContext.AppCategories.UNKNOWN
    }

    private fun String.toAppCategory(): AppCategory = when (this.lowercase()) {
        "productivity" -> AppCategory.PRODUCTIVITY
        "social" -> AppCategory.SOCIAL
        "media" -> AppCategory.MEDIA
        "communication" -> AppCategory.COMMUNICATION
        "browser" -> AppCategory.BROWSER
        "shopping" -> AppCategory.SHOPPING
        "navigation" -> AppCategory.NAVIGATION
        "games" -> AppCategory.GAMES
        "system" -> AppCategory.SYSTEM
        else -> AppCategory.UNKNOWN
    }

    private fun LocationType.toUnifiedLocation(): String = when (this) {
        LocationType.HOME -> UnifiedCommandContext.LocationTypes.HOME
        LocationType.WORK -> UnifiedCommandContext.LocationTypes.WORK
        LocationType.PUBLIC -> UnifiedCommandContext.LocationTypes.PUBLIC
        LocationType.VEHICLE -> UnifiedCommandContext.LocationTypes.VEHICLE
        LocationType.OUTDOOR -> UnifiedCommandContext.LocationTypes.OUTDOOR
        LocationType.UNKNOWN -> UnifiedCommandContext.LocationTypes.UNKNOWN
    }

    private fun String.toLocationType(): LocationType = when (this.lowercase()) {
        "home" -> LocationType.HOME
        "work" -> LocationType.WORK
        "public" -> LocationType.PUBLIC
        "vehicle" -> LocationType.VEHICLE
        "outdoor" -> LocationType.OUTDOOR
        else -> LocationType.UNKNOWN
    }

    private fun ActivityType.toUnifiedActivity(): String = when (this) {
        ActivityType.WALKING -> UnifiedCommandContext.ActivityTypes.WALKING
        ActivityType.RUNNING -> UnifiedCommandContext.ActivityTypes.RUNNING
        ActivityType.DRIVING -> UnifiedCommandContext.ActivityTypes.DRIVING
        ActivityType.STATIONARY -> UnifiedCommandContext.ActivityTypes.STATIONARY
        ActivityType.CYCLING -> UnifiedCommandContext.ActivityTypes.CYCLING
        ActivityType.TILTING -> UnifiedCommandContext.ActivityTypes.UNKNOWN
        ActivityType.UNKNOWN -> UnifiedCommandContext.ActivityTypes.UNKNOWN
    }

    private fun String.toActivityType(): ActivityType = when (this.lowercase()) {
        "walking" -> ActivityType.WALKING
        "running" -> ActivityType.RUNNING
        "driving" -> ActivityType.DRIVING
        "stationary" -> ActivityType.STATIONARY
        "cycling" -> ActivityType.CYCLING
        else -> ActivityType.UNKNOWN
    }

    private fun TimeOfDay.toUnifiedTimeOfDay(): String = when (this) {
        TimeOfDay.EARLY_MORNING -> UnifiedCommandContext.TimeOfDay.EARLY_MORNING
        TimeOfDay.MORNING -> UnifiedCommandContext.TimeOfDay.MORNING
        TimeOfDay.AFTERNOON -> UnifiedCommandContext.TimeOfDay.AFTERNOON
        TimeOfDay.EVENING -> UnifiedCommandContext.TimeOfDay.EVENING
        TimeOfDay.NIGHT -> UnifiedCommandContext.TimeOfDay.NIGHT
        TimeOfDay.LATE_NIGHT -> UnifiedCommandContext.TimeOfDay.LATE_NIGHT
    }

    private fun String.toTimeOfDay(): TimeOfDay = when (this.lowercase()) {
        "early_morning" -> TimeOfDay.EARLY_MORNING
        "morning" -> TimeOfDay.MORNING
        "afternoon" -> TimeOfDay.AFTERNOON
        "evening" -> TimeOfDay.EVENING
        "night" -> TimeOfDay.NIGHT
        "late_night" -> TimeOfDay.LATE_NIGHT
        else -> TimeOfDay.fromHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
}
