/**
 * CommandManager.kt - Simplified commands manager
 * Direct implementation - no unnecessary abstractions
 */

package com.augmentalis.commandmanager

import android.content.Context
import android.util.Log
import com.augmentalis.commandmanager.models.*
import com.augmentalis.commandmanager.actions.*

/**
 * Commands Manager
 * Zero overhead direct implementation
 */
class CommandManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CommandManager"
        
        @Volatile
        private var instance: CommandManager? = null
        
        fun getInstance(context: Context): CommandManager {
            return instance ?: synchronized(this) {
                instance ?: CommandManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Direct command handlers - no registry needed
    private val navigationActions = mapOf(
        "nav_back" to NavigationActions.BackAction(),
        "nav_home" to NavigationActions.HomeAction(),
        "nav_recent" to NavigationActions.RecentAppsAction()
    )
    
    private val volumeActions = mapOf(
        "volume_up" to VolumeActions.VolumeUpAction(),
        "volume_down" to VolumeActions.VolumeDownAction(),
        "mute" to VolumeActions.MuteAction()
    )
    
    private val systemActions = mapOf(
        "wifi_toggle" to SystemActions.WifiToggleAction(),
        "bluetooth_toggle" to SystemActions.BluetoothToggleAction(),
        "open_settings" to SystemActions.OpenSettingsAction()
    )
    
    /**
     * Execute command directly
     */
    suspend fun executeCommand(command: Command): CommandResult {
        Log.d(TAG, "Executing command: ${command.id}")
        
        // Find and execute action directly
        val action = when {
            command.id.startsWith("nav_") -> navigationActions[command.id]
            command.id.startsWith("volume_") || command.id == "mute" -> volumeActions[command.id]
            command.id.startsWith("wifi_") || command.id.startsWith("bluetooth_") || command.id == "open_settings" -> systemActions[command.id]
            else -> null
        }
        
        return if (action != null) {
            try {
                action.invoke(command)
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                CommandResult(
                    success = false,
                    command = command,
                    error = CommandError(ErrorCode.EXECUTION_FAILED, e.message ?: "Unknown error")
                )
            }
        } else {
            CommandResult(
                success = false,
                command = command,
                error = CommandError(ErrorCode.COMMAND_NOT_FOUND, "Unknown command: ${command.id}")
            )
        }
    }
    
    /**
     * Initialize manager
     */
    fun initialize() {
        Log.d(TAG, "CommandManager initialized")
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        Log.d(TAG, "CommandManager cleaned up")
    }
}