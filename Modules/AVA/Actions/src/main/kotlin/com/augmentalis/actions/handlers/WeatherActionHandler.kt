package com.augmentalis.actions.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Action handler for checking weather.
 *
 * Attempts to open a weather app, with fallback to weather.com web browser.
 *
 * Strategy:
 * 1. Try to launch a weather app (ACTION_VIEW with geo: URI)
 * 2. If no weather app installed, open weather.com in browser
 *
 * Current implementation:
 * - Opens browser to weather.com as universal fallback
 * - Future: Try to detect and launch popular weather apps (Google Weather, AccuWeather, etc.)
 * - Future: Parse location from utterance ("weather in Seattle")
 *
 * Intent classification examples:
 * - "What's the weather?"
 * - "Check weather"
 * - "Will it rain?"
 * - "Weather forecast"
 *
 * @see IntentActionHandler
 */
class WeatherActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "WeatherActionHandler"
        private const val WEATHER_WEB_URL = "https://weather.com"
    }

    override val intent = "check_weather"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening weather for utterance: '$utterance'")

            // Strategy 1: Try to open a weather app
            // We'll use an implicit intent that weather apps can handle
            val weatherAppIntent = Intent(Intent.ACTION_VIEW).apply {
                // Some weather apps respond to geo: URIs
                data = Uri.parse("geo:0,0?q=weather")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(
                weatherAppIntent,
                0
            )

            // Check if a weather app is available
            if (resolveInfo != null && resolveInfo.activityInfo.packageName != "android") {
                // Found a weather app, launch it
                context.startActivity(weatherAppIntent)
                Log.i(TAG, "Launched weather app: ${resolveInfo.activityInfo.packageName}")
                ActionResult.Success(message = "Opening weather app")
            } else {
                // Strategy 2: Fallback to weather.com in browser
                Log.d(TAG, "No dedicated weather app found, opening weather.com")
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(WEATHER_WEB_URL)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                // This should always succeed (every Android device has a browser)
                context.startActivity(browserIntent)
                Log.i(TAG, "Opened weather.com in browser")
                ActionResult.Success(message = "Opening weather.com")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open weather", e)
            ActionResult.Failure(
                message = "Failed to open weather: ${e.message}",
                exception = e
            )
        }
    }
}
