package com.augmentalis.intentactions

/**
 * Categories of intent actions for grouping and filtering.
 */
enum class IntentCategory {
    /** Email, SMS, phone calls */
    COMMUNICATION,
    /** Maps, directions, nearby places, traffic */
    NAVIGATION,
    /** Alarms, timers, reminders, calendar events, todos, notes */
    PRODUCTIVITY,
    /** Web search, URL navigation, math calculation */
    SEARCH,
    /** Open app, play video, open browser, resume music */
    MEDIA_LAUNCH,
    /** Open specific settings subsections */
    SYSTEM_SETTINGS
}
