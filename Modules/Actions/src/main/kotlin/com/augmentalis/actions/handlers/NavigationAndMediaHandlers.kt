package com.augmentalis.actions.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Action handler for getting directions.
 *
 * Extracts destination from utterance and launches Google Maps for navigation.
 *
 * Intent: get_directions (navigation.aot)
 * Utterances: "directions to work", "navigate to home", "how do I get to downtown"
 * Entities: destination (required), travel_mode (optional: driving, walking, transit)
 *
 * Examples:
 * - "directions to work" → Google Maps navigation
 * - "navigate to 123 Main St" → Directions to address
 * - "how do I get to Starbucks" → Navigation to place
 *
 * Priority: P1 (Week 2)
 * Effort: 5 hours
 */
class GetDirectionsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "GetDirectionsHandler"
    }

    override val intent = "get_directions"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Getting directions for utterance: '$utterance'")

            // Extract destination using pattern matching
            val destinationPatterns = listOf(
                Regex("directions to (.+)", RegexOption.IGNORE_CASE),
                Regex("navigate to (.+)", RegexOption.IGNORE_CASE),
                Regex("how (?:do i|to) get to (.+)", RegexOption.IGNORE_CASE),
                Regex("take me to (.+)", RegexOption.IGNORE_CASE),
                Regex("drive to (.+)", RegexOption.IGNORE_CASE),
                Regex("walk to (.+)", RegexOption.IGNORE_CASE)
            )

            var destination: String? = null
            destinationPatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    destination = match.trim()
                    return@forEach
                }
            }

            if (destination.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract destination from: $utterance")
                return ActionResult.Failure("I couldn't figure out where you want to go. Try: 'directions to downtown'")
            }

            // Build Google Maps navigation intent
            // Format: google.navigation:q=destination
            val navigationUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
            val mapsIntent = Intent(Intent.ACTION_VIEW, navigationUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened navigation to: $destination")
            ActionResult.Success(message = "Getting directions to $destination")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get directions", e)
            ActionResult.Failure(
                message = "Failed to open maps: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for finding nearby places.
 *
 * Extracts place type from utterance and searches nearby in Google Maps.
 *
 * Intent: find_nearby (navigation.aot)
 * Utterances: "find coffee near me", "nearby restaurants", "where's the closest gas station"
 * Entities: place_type (required), query (optional)
 *
 * Examples:
 * - "find coffee near me" → Search for coffee shops
 * - "nearby restaurants" → Search for restaurants
 * - "where's the closest gas station" → Find gas stations
 *
 * Priority: P1 (Week 2)
 * Effort: 4 hours
 */
class FindNearbyActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "FindNearbyHandler"
    }

    override val intent = "find_nearby"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Finding nearby for utterance: '$utterance'")

            // Extract place type using pattern matching
            val placePatterns = listOf(
                Regex("find (.+?) near me", RegexOption.IGNORE_CASE),
                Regex("find (.+?) nearby", RegexOption.IGNORE_CASE),
                Regex("nearby (.+)", RegexOption.IGNORE_CASE),
                Regex("where(?:'s| is) (?:the )?(?:closest|nearest) (.+)", RegexOption.IGNORE_CASE),
                Regex("find a (.+)", RegexOption.IGNORE_CASE),
                Regex("search for (.+?) near me", RegexOption.IGNORE_CASE)
            )

            var placeType: String? = null
            placePatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    placeType = match.trim()
                    return@forEach
                }
            }

            if (placeType.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract place type from: $utterance")
                return ActionResult.Failure("I couldn't understand what to find. Try: 'find coffee near me'")
            }

            // Build Google Maps search intent for nearby places
            // Format: geo:0,0?q=place_type
            val searchUri = Uri.parse("geo:0,0?q=${Uri.encode(placeType)}")
            val mapsIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Searching for nearby: $placeType")
            ActionResult.Success(message = "Finding $placeType near you")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find nearby", e)
            ActionResult.Failure(
                message = "Failed to search nearby: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for playing videos.
 *
 * Extracts video title/search query and opens YouTube or video player.
 *
 * Intent: play_video (media.aot)
 * Utterances: "play video cats", "watch movie inception", "open youtube search dogs"
 * Entities: title (optional), platform (optional: YouTube, Netflix, etc.)
 *
 * Examples:
 * - "play video cats" → YouTube search for "cats"
 * - "watch funny videos" → YouTube search
 * - "play video" → Opens YouTube
 *
 * Priority: P1 (Week 2)
 * Effort: 3 hours
 */
class PlayVideoActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "PlayVideoHandler"
    }

    override val intent = "play_video"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        Log.d(TAG, "Playing video for utterance: '$utterance'")

        // Extract video query using pattern matching (before try-catch for scope)
        val videoPatterns = listOf(
            Regex("play video (.+)", RegexOption.IGNORE_CASE),
            Regex("watch video (.+)", RegexOption.IGNORE_CASE),
            Regex("watch (.+) video", RegexOption.IGNORE_CASE),
            Regex("play (.+) on youtube", RegexOption.IGNORE_CASE),
            Regex("watch (.+) on youtube", RegexOption.IGNORE_CASE),
            Regex("youtube (.+)", RegexOption.IGNORE_CASE)
        )

        var query: String? = null
        videoPatterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                query = match.trim()
                return@forEach
            }
        }

        return try {

            // Build YouTube intent
            val youtubeIntent = if (query.isNullOrEmpty()) {
                // No query - just open YouTube app
                Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Search YouTube for query
                val searchUri = Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}")
                Intent(Intent.ACTION_VIEW, searchUri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(youtubeIntent)

            val responseMessage = if (query != null) {
                "Playing video: $query"
            } else {
                "Opening YouTube"
            }

            Log.i(TAG, responseMessage)
            ActionResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play video", e)
            // Fallback to web if YouTube app not installed
            val webQuery = query ?: ""
            val fallbackUrl = if (webQuery.isNotEmpty()) {
                "https://www.youtube.com/results?search_query=${Uri.encode(webQuery)}"
            } else {
                "https://www.youtube.com"
            }

            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                ActionResult.Success(message = "Opening YouTube in browser")
            } catch (fallbackError: Exception) {
                ActionResult.Failure(
                    message = "Failed to play video: ${fallbackError.message}",
                    exception = fallbackError
                )
            }
        }
    }
}

/**
 * Action handler for showing traffic conditions.
 *
 * Opens Google Maps with traffic layer enabled.
 *
 * Intent: show_traffic (navigation.aot)
 * Utterances: "show traffic", "how is traffic", "check traffic"
 * Entities: location (optional), route (optional)
 *
 * Examples:
 * - "show traffic" → Opens Maps with traffic layer
 * - "how is traffic to work" → Traffic to destination
 * - "check traffic" → Current traffic conditions
 *
 * Priority: P3 (Week 4)
 * Effort: 2 hours
 */
class ShowTrafficActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ShowTrafficHandler"
    }

    override val intent = "show_traffic"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Showing traffic for utterance: '$utterance'")

            // Open Google Maps with traffic layer
            // Format: google.navigation:q=... or just open Maps app
            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/?traffic=1")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps with traffic")
            ActionResult.Success(message = "Showing traffic conditions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show traffic", e)
            ActionResult.Failure(
                message = "Failed to show traffic: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for sharing current location.
 *
 * Opens a share dialog to send current GPS location.
 *
 * Intent: share_location (navigation.aot)
 * Utterances: "share my location", "send my location", "where am I"
 * Entities: recipient (optional)
 *
 * Examples:
 * - "share my location" → Opens location share dialog
 * - "send my location" → Share current position
 * - "where am I" → Get and share current location
 *
 * Priority: P3 (Week 4)
 * Effort: 3 hours
 *
 * Note: This opens Google Maps location sharing. A more advanced implementation
 * would get GPS coordinates and share via Intent.ACTION_SEND.
 */
class ShareLocationActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ShareLocationHandler"
    }

    override val intent = "share_location"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Sharing location for utterance: '$utterance'")

            // Open Google Maps location sharing
            // Note: This requires Google Maps. A more complete implementation
            // would use LocationManager to get coordinates and share via SMS/email
            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps for location sharing")
            ActionResult.Success(message = "Open Maps to share your location")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share location", e)
            ActionResult.Failure(
                message = "Failed to share location: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for saving a location.
 *
 * Opens Google Maps to save/bookmark the current location.
 *
 * Intent: save_location (navigation.aot)
 * Utterances: "save location", "bookmark this place", "remember this location"
 * Entities: location (optional), label (optional)
 *
 * Examples:
 * - "save location" → Opens Maps to save current spot
 * - "bookmark this place" → Save place in Maps
 * - "save this location as home" → Save with label
 *
 * Priority: P3 (Week 4)
 * Effort: 2 hours
 */
class SaveLocationActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "SaveLocationHandler"
    }

    override val intent = "save_location"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Saving location for utterance: '$utterance'")

            // Open Google Maps (user can then tap to save location)
            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps for saving location")
            ActionResult.Success(message = "Open Maps to save this location")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save location", e)
            ActionResult.Failure(
                message = "Failed to open maps: ${e.message}",
                exception = e
            )
        }
    }
}
