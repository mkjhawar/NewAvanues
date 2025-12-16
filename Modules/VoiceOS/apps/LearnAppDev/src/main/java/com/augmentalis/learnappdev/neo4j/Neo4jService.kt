/**
 * Neo4jService.kt - Neo4j graph database service
 *
 * Provides connection management and graph export functionality.
 * Exports VoiceOS learned data to Neo4j for visualization.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (P2 Feature - Neo4j Graph Export)
 */

package com.augmentalis.learnappdev.neo4j

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.neo4j.driver.*
import org.neo4j.driver.exceptions.Neo4jException

/**
 * Neo4j connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverInfo: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Graph node types
 */
object NodeLabels {
    const val SCREEN = "Screen"
    const val ELEMENT = "Element"
    const val APP = "App"
    const val ACTION = "Action"
}

/**
 * Graph relationship types
 */
object RelationshipTypes {
    const val NAVIGATES_TO = "NAVIGATES_TO"
    const val HAS_ELEMENT = "HAS_ELEMENT"
    const val TRIGGERS = "TRIGGERS"
    const val BELONGS_TO = "BELONGS_TO"
}

/**
 * Screen data for export
 */
data class ScreenExport(
    val screenHash: String,
    val packageName: String,
    val activityName: String?,
    val elementCount: Int,
    val visitCount: Int
)

/**
 * Element data for export
 */
data class ElementExport(
    val stableId: String,
    val vuid: String?,
    val className: String,
    val text: String?,
    val resourceId: String?,
    val isClickable: Boolean,
    val screenHash: String
)

/**
 * Navigation edge for export
 */
data class NavigationExport(
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementId: String?,
    val count: Int
)

/**
 * Neo4j Service
 *
 * Manages Neo4j connection and provides export functionality.
 */
class Neo4jService(
    private val context: Context,
    private val config: Neo4jConfig = Neo4jConfig()
) {
    companion object {
        private const val TAG = "Neo4jService"
    }

    private var driver: Driver? = null
    private var _connectionState: ConnectionState = ConnectionState.Disconnected
    val connectionState: ConnectionState get() = _connectionState

    /**
     * Connect to Neo4j database
     */
    suspend fun connect(): ConnectionState = withContext(Dispatchers.IO) {
        if (driver != null) {
            return@withContext _connectionState
        }

        _connectionState = ConnectionState.Connecting

        try {
            Log.i(TAG, "Connecting to Neo4j at ${config.uri}")

            driver = GraphDatabase.driver(
                config.uri,
                AuthTokens.basic(config.username, config.password),
                Config.builder()
                    .withConnectionTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .withMaxConnectionPoolSize(5)
                    .build()
            )

            // Verify connection
            driver?.verifyConnectivity()

            val serverInfo = driver?.session()?.use { session ->
                session.run("CALL dbms.components()").single().get("name").asString()
            } ?: "Neo4j"

            _connectionState = ConnectionState.Connected(serverInfo)
            Log.i(TAG, "Connected to Neo4j: $serverInfo")

        } catch (e: Neo4jException) {
            Log.e(TAG, "Failed to connect to Neo4j", e)
            _connectionState = ConnectionState.Error(e.message ?: "Connection failed")
            driver = null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error connecting to Neo4j", e)
            _connectionState = ConnectionState.Error(e.message ?: "Unknown error")
            driver = null
        }

        _connectionState
    }

    /**
     * Disconnect from Neo4j
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            driver?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing Neo4j driver", e)
        } finally {
            driver = null
            _connectionState = ConnectionState.Disconnected
        }
    }

    /**
     * Export screens to Neo4j
     */
    suspend fun exportScreens(screens: List<ScreenExport>): Int = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext 0
        var count = 0

        try {
            driver.session().use { session ->
                screens.forEach { screen ->
                    val query = """
                        MERGE (s:${NodeLabels.SCREEN} {hash: ${'$'}hash})
                        SET s.packageName = ${'$'}packageName,
                            s.activityName = ${'$'}activityName,
                            s.elementCount = ${'$'}elementCount,
                            s.visitCount = ${'$'}visitCount
                        RETURN s
                    """.trimIndent()

                    session.run(query, mapOf(
                        "hash" to screen.screenHash,
                        "packageName" to screen.packageName,
                        "activityName" to screen.activityName,
                        "elementCount" to screen.elementCount,
                        "visitCount" to screen.visitCount
                    ))
                    count++
                }
            }
            Log.i(TAG, "Exported $count screens to Neo4j")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting screens", e)
        }

        count
    }

    /**
     * Export elements to Neo4j
     */
    suspend fun exportElements(elements: List<ElementExport>): Int = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext 0
        var count = 0

        try {
            driver.session().use { session ->
                elements.forEach { element ->
                    // Create element node
                    val createQuery = """
                        MERGE (e:${NodeLabels.ELEMENT} {stableId: ${'$'}stableId})
                        SET e.vuid = ${'$'}vuid,
                            e.className = ${'$'}className,
                            e.text = ${'$'}text,
                            e.resourceId = ${'$'}resourceId,
                            e.isClickable = ${'$'}isClickable
                        RETURN e
                    """.trimIndent()

                    session.run(createQuery, mapOf(
                        "stableId" to element.stableId,
                        "vuid" to element.vuid,
                        "className" to element.className,
                        "text" to element.text,
                        "resourceId" to element.resourceId,
                        "isClickable" to element.isClickable
                    ))

                    // Create relationship to screen
                    val relQuery = """
                        MATCH (e:${NodeLabels.ELEMENT} {stableId: ${'$'}stableId})
                        MATCH (s:${NodeLabels.SCREEN} {hash: ${'$'}screenHash})
                        MERGE (s)-[:${RelationshipTypes.HAS_ELEMENT}]->(e)
                    """.trimIndent()

                    session.run(relQuery, mapOf(
                        "stableId" to element.stableId,
                        "screenHash" to element.screenHash
                    ))

                    count++
                }
            }
            Log.i(TAG, "Exported $count elements to Neo4j")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting elements", e)
        }

        count
    }

    /**
     * Export navigation relationships to Neo4j
     */
    suspend fun exportNavigations(navigations: List<NavigationExport>): Int = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext 0
        var count = 0

        try {
            driver.session().use { session ->
                navigations.forEach { nav ->
                    val query = """
                        MATCH (from:${NodeLabels.SCREEN} {hash: ${'$'}fromHash})
                        MATCH (to:${NodeLabels.SCREEN} {hash: ${'$'}toHash})
                        MERGE (from)-[r:${RelationshipTypes.NAVIGATES_TO}]->(to)
                        SET r.triggerElement = ${'$'}triggerElement,
                            r.count = ${'$'}count
                    """.trimIndent()

                    session.run(query, mapOf(
                        "fromHash" to nav.fromScreenHash,
                        "toHash" to nav.toScreenHash,
                        "triggerElement" to nav.triggerElementId,
                        "count" to nav.count
                    ))
                    count++
                }
            }
            Log.i(TAG, "Exported $count navigations to Neo4j")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting navigations", e)
        }

        count
    }

    /**
     * Execute a custom Cypher query
     */
    suspend fun executeQuery(cypher: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext emptyList()
        val results = mutableListOf<Map<String, Any>>()

        try {
            driver.session().use { session ->
                val result = session.run(cypher)
                result.forEach { record ->
                    val map = mutableMapOf<String, Any>()
                    record.keys().forEach { key ->
                        map[key] = record.get(key).asObject() ?: "null"
                    }
                    results.add(map)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: $cypher", e)
        }

        results
    }

    /**
     * Get graph statistics
     */
    suspend fun getStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext emptyMap()
        val stats = mutableMapOf<String, Int>()

        try {
            driver.session().use { session ->
                // Count screens
                stats["screens"] = session.run("MATCH (s:${NodeLabels.SCREEN}) RETURN count(s) as count")
                    .single().get("count").asInt()

                // Count elements
                stats["elements"] = session.run("MATCH (e:${NodeLabels.ELEMENT}) RETURN count(e) as count")
                    .single().get("count").asInt()

                // Count navigations
                stats["navigations"] = session.run("MATCH ()-[r:${RelationshipTypes.NAVIGATES_TO}]->() RETURN count(r) as count")
                    .single().get("count").asInt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stats", e)
        }

        stats
    }

    /**
     * Clear all data from the database
     */
    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        val driver = driver ?: return@withContext

        try {
            driver.session().use { session ->
                session.run("MATCH (n) DETACH DELETE n")
            }
            Log.i(TAG, "Cleared Neo4j database")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing database", e)
        }
    }
}
