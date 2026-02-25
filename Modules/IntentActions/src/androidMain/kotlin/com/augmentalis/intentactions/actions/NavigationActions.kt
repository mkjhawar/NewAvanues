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
 * Gets directions to a destination using Google Maps navigation.
 */
object GetDirectionsAction : IIntentAction {
    private const val TAG = "GetDirectionsAction"

    override val intentId = "get_directions"
    override val category = IntentCategory.NAVIGATION
    override val requiredEntities = listOf(EntityType.LOCATION)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Getting directions ${entities.toSafeString()}")

            val destination = entities.location ?: entities.query
            if (destination.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.LOCATION,
                    prompt = "Where would you like to go? Say a destination."
                )
            }

            val navigationUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
            val mapsIntent = Intent(Intent.ACTION_VIEW, navigationUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened navigation to: $destination")
            IntentResult.Success(message = "Getting directions to $destination")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get directions", e)
            IntentResult.Failed(
                reason = "Failed to open maps: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Finds nearby places using Google Maps search.
 */
object FindNearbyAction : IIntentAction {
    private const val TAG = "FindNearbyAction"

    override val intentId = "find_nearby"
    override val category = IntentCategory.NAVIGATION
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Finding nearby ${entities.toSafeString()}")

            val placeType = entities.query ?: entities.location
            if (placeType.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What are you looking for nearby? Say a place type like 'coffee' or 'restaurant'."
                )
            }

            val searchUri = Uri.parse("geo:0,0?q=${Uri.encode(placeType)}")
            val mapsIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Searching for nearby: $placeType")
            IntentResult.Success(message = "Finding $placeType near you")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find nearby", e)
            IntentResult.Failed(
                reason = "Failed to search nearby: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Shows traffic conditions using Google Maps with traffic layer.
 */
object ShowTrafficAction : IIntentAction {
    private const val TAG = "ShowTrafficAction"

    override val intentId = "show_traffic"
    override val category = IntentCategory.NAVIGATION
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Showing traffic")

            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/?traffic=1")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps with traffic")
            IntentResult.Success(message = "Showing traffic conditions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show traffic", e)
            IntentResult.Failed(
                reason = "Failed to show traffic: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens Google Maps to share the user's current location.
 */
object ShareLocationAction : IIntentAction {
    private const val TAG = "ShareLocationAction"

    override val intentId = "share_location"
    override val category = IntentCategory.NAVIGATION
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Sharing location")

            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps for location sharing")
            IntentResult.Success(message = "Open Maps to share your location")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share location", e)
            IntentResult.Failed(
                reason = "Failed to share location: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens Google Maps to save/bookmark the current location.
 */
object SaveLocationAction : IIntentAction {
    private const val TAG = "SaveLocationAction"

    override val intentId = "save_location"
    override val category = IntentCategory.NAVIGATION
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Saving location")

            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://maps.google.com/")
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(mapsIntent)

            Log.i(TAG, "Opened Maps for saving location")
            IntentResult.Success(message = "Open Maps to save this location")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save location", e)
            IntentResult.Failed(
                reason = "Failed to open maps: ${e.message}",
                exception = e
            )
        }
    }
}
