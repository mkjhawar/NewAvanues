/**
 * RepositoryResults.kt - Result types for repository operations
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/repository/RepositoryResults.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-28
 *
 * Type-safe result types for repository operations
 */

package com.augmentalis.learnapp.database.repository

/**
 * Session Creation Result
 *
 * Result type for exploration session creation operations.
 * Provides detailed outcome information including whether parent app was created.
 *
 * @since 1.0.0
 */
sealed class SessionCreationResult {
    /**
     * Session created successfully
     *
     * @property sessionId Generated session ID
     * @property appWasCreated Whether parent LearnedAppEntity was auto-created
     * @property metadataSource Where app metadata came from (if created)
     */
    data class Created(
        val sessionId: String,
        val appWasCreated: Boolean,
        val metadataSource: MetadataSource? = null
    ) : SessionCreationResult()

    /**
     * Session creation failed
     *
     * @property reason Human-readable failure reason
     * @property cause Original exception if available
     */
    data class Failed(
        val reason: String,
        val cause: Throwable? = null
    ) : SessionCreationResult()
}

/**
 * Repository Result
 *
 * Generic result type for repository operations.
 *
 * @param T Success value type
 *
 * @since 1.0.0
 */
sealed class RepositoryResult<out T> {
    /**
     * Operation succeeded
     *
     * @property value Result value
     */
    data class Success<T>(val value: T) : RepositoryResult<T>()

    /**
     * Operation failed
     *
     * @property reason Human-readable failure reason
     * @property cause Original exception if available
     */
    data class Failure(
        val reason: String,
        val cause: Throwable? = null
    ) : RepositoryResult<Nothing>()
}

/**
 * Learned App Not Found Exception
 *
 * Thrown when strict mode requires parent app to exist but it doesn't.
 *
 * @property packageName Package name that was not found
 * @property message Error message
 *
 * @since 1.0.0
 */
class LearnedAppNotFoundException(
    val packageName: String,
    message: String = "LearnedAppEntity with packageName='$packageName' not found. " +
            "Insert app first using saveLearnedApp() or use createExplorationSessionSafe()."
) : IllegalStateException(message)
