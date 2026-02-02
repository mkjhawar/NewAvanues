/**
 * ActionResult.kt - Result data class for command actions
 * Direct implementation without interfaces for zero overhead
 */

package com.augmentalis.voiceoscore.managers.commandmanager.models

/**
 * Result from executing a command action
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null,
    val error: String? = null
) {
    companion object {
        fun success(message: String? = null, data: Any? = null) = 
            ActionResult(true, message, data)
            
        fun error(error: String) = 
            ActionResult(false, error = error)
    }
}