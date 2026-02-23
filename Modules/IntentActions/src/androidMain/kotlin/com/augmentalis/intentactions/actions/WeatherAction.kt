package com.augmentalis.intentactions.actions

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext

/**
 * Checks weather by opening a weather app or falling back to weather.com.
 *
 * Strategy:
 * 1. Try to launch a weather app using geo: URI
 * 2. If no weather app installed, open weather.com in browser
 */
object GetWeatherAction : IIntentAction {
    private const val TAG = "GetWeatherAction"
    private const val WEATHER_WEB_URL = "https://weather.com"

    override val intentId = "check_weather"
    override val category = IntentCategory.SEARCH
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Checking weather")

            // Strategy 1: Try to open a weather app via geo: URI
            val weatherAppIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=weather")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val resolveInfo = context.packageManager.resolveActivity(weatherAppIntent, 0)

            if (resolveInfo != null && resolveInfo.activityInfo.packageName != "android") {
                context.startActivity(weatherAppIntent)
                Log.i(TAG, "Launched weather app: ${resolveInfo.activityInfo.packageName}")
                IntentResult.Success(message = "Opening weather app")
            } else {
                // Strategy 2: Fallback to weather.com in browser
                Log.d(TAG, "No dedicated weather app found, opening weather.com")
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(WEATHER_WEB_URL)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(browserIntent)
                Log.i(TAG, "Opened weather.com in browser")
                IntentResult.Success(message = "Opening weather.com")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open weather", e)
            IntentResult.Failed(
                reason = "Failed to open weather: ${e.message}",
                exception = e
            )
        }
    }
}
