/**
 * VolumeActions.kt - Volume control command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/VolumeActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Volume control-related voice command actions
 */

package com.augmentalis.commandmanager.actions

import com.augmentalis.commandmanager.*
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager

/**
 * Volume control command actions
 * Handles volume adjustment, muting, and audio stream control
 */
object VolumeActions {
    
    /**
     * Volume Up Action
     */
    class VolumeUpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            val steps = getNumberParameter(command, "steps")?.toInt() ?: 1
            
            return try {
                repeat(steps) {
                    audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
                val currentVolume = audioManager.getStreamVolume(streamType)
                createSuccessResult(command, "Volume increased to $currentVolume")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to increase volume: ${e.message}")
            }
        }
    }
    
    /**
     * Volume Down Action
     */
    class VolumeDownAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            val steps = getNumberParameter(command, "steps")?.toInt() ?: 1
            
            return try {
                repeat(steps) {
                    audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
                val currentVolume = audioManager.getStreamVolume(streamType)
                createSuccessResult(command, "Volume decreased to $currentVolume")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to decrease volume: ${e.message}")
            }
        }
    }
    
    /**
     * Mute Action
     */
    class MuteAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            
            return try {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                createSuccessResult(command, "Audio muted")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to mute audio: ${e.message}")
            }
        }
    }
    
    /**
     * Unmute Action
     */
    class UnmuteAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            
            return try {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                createSuccessResult(command, "Audio unmuted")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to unmute audio: ${e.message}")
            }
        }
    }
    
    /**
     * Max Volume Action
     */
    class MaxVolumeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            
            return try {
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                audioManager.setStreamVolume(streamType, maxVolume, AudioManager.FLAG_SHOW_UI)
                createSuccessResult(command, "Volume set to maximum ($maxVolume)")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to set max volume: ${e.message}")
            }
        }
    }
    
    /**
     * Min Volume Action
     */
    class MinVolumeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            
            return try {
                audioManager.setStreamVolume(streamType, 0, AudioManager.FLAG_SHOW_UI)
                createSuccessResult(command, "Volume set to minimum (0)")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to set min volume: ${e.message}")
            }
        }
    }
    
    /**
     * Set Volume Level Action (1-15 scale)
     */
    class SetVolumeLevelAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            val level = getNumberParameter(command, "level")?.toInt()
            
            return if (level == null || level < 1 || level > 15) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Volume level must be between 1 and 15")
            } else {
                try {
                    val maxVolume = audioManager.getStreamMaxVolume(streamType)
                    val targetVolume = ((level / 15f) * maxVolume).toInt()
                    
                    audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
                    createSuccessResult(command, "Volume set to level $level ($targetVolume)")
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to set volume level: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Volume Level 1 Action
     */
    class VolumeLevel1Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 1)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 2 Action
     */
    class VolumeLevel2Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 2)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 3 Action
     */
    class VolumeLevel3Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 3)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 4 Action
     */
    class VolumeLevel4Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 4)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 5 Action
     */
    class VolumeLevel5Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 5)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 6 Action
     */
    class VolumeLevel6Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 6)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 7 Action
     */
    class VolumeLevel7Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 7)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 8 Action
     */
    class VolumeLevel8Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 8)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 9 Action
     */
    class VolumeLevel9Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 9)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 10 Action
     */
    class VolumeLevel10Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 10)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 11 Action
     */
    class VolumeLevel11Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 11)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 12 Action
     */
    class VolumeLevel12Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 12)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 13 Action
     */
    class VolumeLevel13Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 13)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 14 Action
     */
    class VolumeLevel14Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 14)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Volume Level 15 Action
     */
    class VolumeLevel15Action : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return SetVolumeLevelAction().execute(
                command.copy(parameters = command.parameters + ("level" to 15)),
                accessibilityService,
                context
            )
        }
    }
    
    /**
     * Get Current Volume Action
     */
    class GetVolumeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = getStreamType(command)
            
            return try {
                val currentVolume = audioManager.getStreamVolume(streamType)
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                val percentage = ((currentVolume.toFloat() / maxVolume) * 100).toInt()
                
                createSuccessResult(
                    command, 
                    "Current volume: $currentVolume/$maxVolume ($percentage%)",
                    mapOf(
                        "currentVolume" to currentVolume,
                        "maxVolume" to maxVolume,
                        "percentage" to percentage
                    )
                )
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get volume: ${e.message}")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Get audio stream type from command parameters
     */
    private fun getStreamType(command: Command): Int {
        val streamName = (command.parameters["stream"] as? String)?.lowercase()
        
        return when (streamName) {
            "music", "media" -> AudioManager.STREAM_MUSIC
            "ring", "ringtone" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "call", "voice" -> AudioManager.STREAM_VOICE_CALL
            "system" -> AudioManager.STREAM_SYSTEM
            "dtmf" -> AudioManager.STREAM_DTMF
            else -> AudioManager.STREAM_MUSIC // Default to music stream
        }
    }
}