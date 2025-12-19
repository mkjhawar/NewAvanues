package com.augmentalis.actions.handlers

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Action handler for playing music.
 */
class PlayMusicActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "PlayMusicHandler"
    }

    override val intent = "play_music"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Playing music for utterance: '$utterance'")

            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)

            Log.i(TAG, "Play command sent")
            ActionResult.Success(message = "Playing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play music", e)
            ActionResult.Failure(
                message = "Failed to play music: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for pausing music.
 */
class PauseMusicActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "PauseMusicHandler"
    }

    override val intent = "pause_music"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Pausing music for utterance: '$utterance'")

            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PAUSE)

            Log.i(TAG, "Pause command sent")
            ActionResult.Success(message = "Paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause music", e)
            ActionResult.Failure(
                message = "Failed to pause music: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for next track.
 */
class NextTrackActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "NextTrackHandler"
    }

    override val intent = "next_track"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Skipping to next track for utterance: '$utterance'")

            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)

            Log.i(TAG, "Next track command sent")
            ActionResult.Success(message = "Next track")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to skip to next track", e)
            ActionResult.Failure(
                message = "Failed to skip track: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for previous track.
 */
class PreviousTrackActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "PreviousTrackHandler"
    }

    override val intent = "previous_track"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Going to previous track for utterance: '$utterance'")

            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            Log.i(TAG, "Previous track command sent")
            ActionResult.Success(message = "Previous track")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go to previous track", e)
            ActionResult.Failure(
                message = "Failed to go to previous track: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for shuffle on.
 */
class ShuffleOnActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ShuffleOnHandler"
    }

    override val intent = "shuffle_on"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Enabling shuffle for utterance: '$utterance'")

            // Shuffle is app-specific, most media players don't have a standard intent
            ActionResult.Success(message = "Shuffle is app-specific. Please enable it in your music app.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable shuffle", e)
            ActionResult.Failure(
                message = "Failed to enable shuffle: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for repeat mode.
 */
class RepeatModeActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "RepeatModeHandler"
    }

    override val intent = "repeat_mode"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Changing repeat mode for utterance: '$utterance'")

            // Repeat is app-specific
            ActionResult.Success(message = "Repeat mode is app-specific. Please change it in your music app.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change repeat mode", e)
            ActionResult.Failure(
                message = "Failed to change repeat mode: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for resuming media playback.
 *
 * Resumes paused media by sending KEYCODE_MEDIA_PLAY to the active media session.
 *
 * Intent: resume_media (media.aot)
 * Utterances: "resume", "continue playing", "unpause", "keep playing"
 * Entities: none
 *
 * Examples:
 * - "resume" → Resumes paused music/video
 * - "continue playing" → Continues playback
 * - "unpause" → Resumes media
 *
 * Priority: P2 (Week 3)
 * Effort: 1 hour
 *
 * Note: This uses the same KEYCODE_MEDIA_PLAY as PlayMusicActionHandler.
 * Android media sessions handle this intelligently:
 * - If paused: resumes playback
 * - If stopped: starts playback
 * - If already playing: no effect (or restarts depending on app)
 */
class ResumeMusicActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ResumeMusicHandler"
    }

    override val intent = "resume_media"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Resuming media for utterance: '$utterance'")

            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)

            Log.i(TAG, "Resume command sent")
            ActionResult.Success(message = "Resuming playback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume media", e)
            ActionResult.Failure(
                message = "Failed to resume: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Helper function to send media key events.
 */
private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val eventTime = SystemClock.uptimeMillis()

    // Send key down
    val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
    audioManager.dispatchMediaKeyEvent(downEvent)

    // Send key up
    val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
    audioManager.dispatchMediaKeyEvent(upEvent)
}
