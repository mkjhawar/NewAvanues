/**
 * LearnAppDao.kt - DAO interface for LearnApp database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Interface defining database operations for LearnApp.
 * Implementation uses SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.ExplorationSessionEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.LearnedAppEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.NavigationEdgeEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.ScreenStateEntity

/**
 * LearnApp DAO Interface
 *
 * Aggregate interface combining all LearnApp database operations.
 * This is an interface layer over SQLDelight - implementations use
 * SQLDelight queries rather than Room annotations.
 *
 * ## Interface Segregation Principle (ISP)
 *
 * Extends 4 segregated interfaces following Interface Segregation Principle:
 * - ILearnedAppOperations: Learned app CRUD operations
 * - ISessionOperations: Exploration session operations
 * - INavigationOperations: Navigation graph operations
 * - IScreenStateOperations: Screen state operations
 *
 * Phase 1: SOLID Refactoring - Interface Segregation
 * Clients can depend on specific interfaces instead of this aggregate interface.
 *
 * ## Liskov Substitution Principle (LSP) Contract
 *
 * All implementations MUST adhere to the following behavioral contracts:
 *
 * ### Thread Safety
 * - All methods are suspend functions that MUST be called from coroutines
 * - Implementations MUST handle concurrent access safely
 * - Transaction blocks MUST provide ACID guarantees
 *
 * ### Exception Behavior
 * - Implementations MUST NOT throw exceptions for normal operation failures
 * - Query methods return null when entity not found (NOT throwing)
 * - Transaction failures should propagate exceptions to caller
 * - Database errors should throw appropriate exceptions with clear messages
 *
 * ### Nullable Return Contracts
 * - Methods returning nullable types return null when entity doesn't exist
 * - Methods returning collections return empty list (NOT null) when no results
 * - Primary key lookups (getLearnedApp, getScreenState, etc.) return null if not found
 *
 * ### Side Effects
 * - Insert operations may fail silently if entity already exists (conflict handling)
 * - Update operations affect only existing entities (no-op if entity doesn't exist)
 * - Delete operations are idempotent (no error if entity doesn't exist)
 *
 * @see com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
 * @see ILearnedAppOperations
 * @see ISessionOperations
 * @see INavigationOperations
 * @see IScreenStateOperations
 */
interface LearnAppDao :
    ILearnedAppOperations,
    ISessionOperations,
    INavigationOperations,
    IScreenStateOperations {

    // ========== Transactions ==========

    /**
     * Execute operations in a database transaction
     *
     * Provides ACID guarantees for the block of operations.
     *
     * @param block Suspend block to execute within transaction
     * @return Result of the block execution
     * @throws Exception if transaction fails or is rolled back
     *
     * ## LSP Contract:
     * - MUST provide ACID guarantees (Atomicity, Consistency, Isolation, Durability)
     * - MUST rollback all changes if any operation fails
     * - MUST be re-entrant safe (nested transactions handled correctly)
     * - Implementation MUST run on appropriate dispatcher (not Main thread)
     */
    suspend fun <R> transaction(block: suspend LearnAppDao.() -> R): R

    // ========== Learned Apps ==========
    // Note: Core CRUD operations (insertLearnedApp, getLearnedApp, getAllLearnedApps, deleteLearnedApp)
    // are inherited from ILearnedAppOperations interface

    // Additional learned app operations:

    /**
     * Insert a learned app with minimal fields (for auto-detect mode)
     *
     * @param app LearnedAppEntity to insert
     * @return Row ID of inserted record, or 0 if conflict
     *
     * ## LSP Contract:
     * - MUST return 0 if app already exists (conflict detection)
     * - MUST return positive value (1+) if insertion succeeds
     * - MUST NOT throw on conflict
     * - Used for auto-detection where conflicts are expected
     */
    suspend fun insertLearnedAppMinimal(app: LearnedAppEntity): Long

    /**
     * Update app hash after re-learning
     *
     * @param packageName Package name to update
     * @param newHash New app hash
     * @param timestamp Update timestamp
     *
     * ## LSP Contract:
     * - MUST be no-op if package doesn't exist (NOT throw exception)
     * - MUST update both hash and timestamp atomically
     * - MUST be thread-safe
     * - MUST NOT affect other fields
     */
    suspend fun updateAppHash(packageName: String, newHash: String, timestamp: Long)

    /**
     * Update app statistics
     *
     * @param packageName Package name to update
     * @param totalScreens New total screens count
     * @param totalElements New total elements count
     *
     * ## LSP Contract:
     * - MUST be no-op if package doesn't exist
     * - MUST update statistics atomically
     * - MUST update lastUpdatedAt timestamp automatically
     * - MUST accept non-negative values only (0+)
     * - MUST be thread-safe
     */
    suspend fun updateAppStats(packageName: String, totalScreens: Int, totalElements: Int)

    /**
     * Update a learned app
     *
     * @param app LearnedAppEntity with updated values
     *
     * ## LSP Contract:
     * - MUST be no-op if package doesn't exist
     * - MUST update all fields except package name (immutable primary key)
     * - MUST be thread-safe
     * - Package name is used as lookup key
     */
    suspend fun updateLearnedApp(app: LearnedAppEntity)

    // ========== Exploration Sessions ==========
    // Note: Core CRUD operations (insertExplorationSession, getExplorationSession,
    // getSessionsForPackage, updateSessionStatus, deleteSessionsForPackage)
    // are inherited from ISessionOperations interface

    // Additional session operations:

    /**
     * Delete an exploration session
     *
     * @param session ExplorationSessionEntity to delete
     *
     * ## LSP Contract:
     * - MUST be idempotent (no error if session doesn't exist)
     * - MUST cascade delete related navigation edges
     * - MUST be thread-safe
     */
    suspend fun deleteExplorationSession(session: ExplorationSessionEntity)

    /**
     * Get latest session for a package with specific status
     *
     * @param packageName Package name
     * @param status Status to filter by
     * @return Latest ExplorationSessionEntity or null
     */
    suspend fun getLatestSessionForPackage(packageName: String, status: String): ExplorationSessionEntity?

    // ========== Navigation Edges ==========
    // Note: Core CRUD operations (insertNavigationEdge, getNavigationGraph, deleteNavigationGraph)
    // are inherited from INavigationOperations interface

    // Additional navigation operations:

    /**
     * Insert multiple navigation edges in batch
     *
     * @param edges List of NavigationEdgeEntity to insert
     */
    suspend fun insertNavigationEdges(edges: List<NavigationEdgeEntity>)

    /**
     * Get outgoing edges from a screen
     *
     * @param screenHash Screen hash to get edges from
     * @return List of outgoing NavigationEdgeEntity records
     */
    suspend fun getOutgoingEdges(screenHash: String): List<NavigationEdgeEntity>

    /**
     * Get incoming edges to a screen
     *
     * @param screenHash Screen hash to get edges to
     * @return List of incoming NavigationEdgeEntity records
     */
    suspend fun getIncomingEdges(screenHash: String): List<NavigationEdgeEntity>

    /**
     * Get edges for a session
     *
     * @param sessionId Session ID
     * @return List of NavigationEdgeEntity records for the session
     */
    suspend fun getEdgesForSession(sessionId: String): List<NavigationEdgeEntity>

    /**
     * Delete navigation edges for a session
     *
     * @param sessionId Session ID
     */
    suspend fun deleteNavigationEdgesForSession(sessionId: String)

    // ========== Screen States ==========
    // Note: Core CRUD operations (insertScreenState, getScreenState, getScreenStatesForPackage)
    // are inherited from IScreenStateOperations interface

    // Additional screen state operations:

    /**
     * Delete a screen state
     *
     * @param state ScreenStateEntity to delete
     */
    suspend fun deleteScreenState(state: ScreenStateEntity)

    /**
     * Delete all screen states for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteScreenStatesForPackage(packageName: String)

    // ========== Complex Queries ==========

    /**
     * Get total screens discovered for a package
     *
     * @param packageName Package name
     * @return Total screen count
     */
    suspend fun getTotalScreensForPackage(packageName: String): Int

    /**
     * Get total edges for a package
     *
     * @param packageName Package name
     * @return Total edge count
     */
    suspend fun getTotalEdgesForPackage(packageName: String): Int
}
