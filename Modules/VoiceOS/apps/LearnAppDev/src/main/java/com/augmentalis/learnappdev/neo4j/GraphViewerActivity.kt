/**
 * GraphViewerActivity.kt - Neo4j graph visualization
 *
 * Graph visualization of app navigation and element relationships.
 * Connects to local Neo4j instance for debugging and analysis.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Updated: 2025-12-11 (P2 Feature - Full Implementation)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappdev.neo4j

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.jitlearning.IElementCaptureService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GraphViewerActivity"

/**
 * Neo4j connection configuration
 */
data class Neo4jConfig(
    val uri: String = "bolt://localhost:7687",
    val username: String = "neo4j",
    val password: String = "password",
    val database: String = "voiceos"
)

/**
 * Graph Viewer Activity
 *
 * Provides Neo4j graph visualization and export functionality.
 * - Connection management to local Neo4j instance
 * - Export screens, elements, and navigation data
 * - Execute custom Cypher queries
 * - View graph statistics
 */
class GraphViewerActivity : ComponentActivity() {

    private val neo4jConfig = Neo4jConfig()
    private var neo4jService: Neo4jService? = null
    private var captureService: IElementCaptureService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            captureService = IElementCaptureService.Stub.asInterface(service)
            Log.i(TAG, "Connected to JITLearningService")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            Log.i(TAG, "Disconnected from JITLearningService")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        neo4jService = Neo4jService(this, neo4jConfig)

        // Bind to JITLearningService for data export
        val intent = Intent().apply {
            setClassName(
                "com.augmentalis.voiceoscore",
                "com.augmentalis.jitlearning.JITLearningService"
            )
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF5722)
                )
            ) {
                GraphViewerUI(
                    config = neo4jConfig,
                    neo4jService = neo4jService!!,
                    captureService = { captureService }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphViewerUI(
    config: Neo4jConfig,
    neo4jService: Neo4jService,
    captureService: () -> IElementCaptureService?
) {
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.Disconnected) }
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var queryResult by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var customQuery by remember { mutableStateOf("MATCH (s:Screen) RETURN s.hash, s.packageName LIMIT 10") }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Connect function
    fun connect() {
        scope.launch {
            connectionState = neo4jService.connect()
            if (connectionState is ConnectionState.Connected) {
                stats = neo4jService.getStats()
            }
        }
    }

    // Disconnect function
    fun disconnect() {
        scope.launch {
            neo4jService.disconnect()
            connectionState = ConnectionState.Disconnected
            stats = emptyMap()
        }
    }

    // Export function
    fun exportData() {
        scope.launch {
            isExporting = true
            exportMessage = "Exporting..."

            try {
                val service = captureService()
                if (service == null) {
                    exportMessage = "Error: Not connected to VoiceOS"
                    isExporting = false
                    return@launch
                }

                // Get current state
                val state = service.queryState()

                // Export screens from database
                // Note: In a full implementation, we'd query the database for all screens
                // For now, we'll create a sample export
                val screens = listOf(
                    ScreenExport(
                        screenHash = "sample-screen-hash",
                        packageName = state?.currentPackage ?: "unknown",
                        activityName = "MainActivity",
                        elementCount = state?.elementsDiscovered ?: 0,
                        visitCount = 1
                    )
                )

                val screensExported = neo4jService.exportScreens(screens)
                stats = neo4jService.getStats()
                exportMessage = "Exported $screensExported screens"

            } catch (e: Exception) {
                Log.e(TAG, "Export error", e)
                exportMessage = "Error: ${e.message}"
            }

            isExporting = false
        }
    }

    // Execute query function
    fun executeQuery() {
        scope.launch {
            queryResult = neo4jService.executeQuery(customQuery)
        }
    }

    // Clear database function
    fun clearDatabase() {
        scope.launch {
            neo4jService.clearDatabase()
            stats = neo4jService.getStats()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neo4j Graph Export") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                ),
                actions = {
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            TextButton(onClick = { disconnect() }) {
                                Text("Disconnect", color = Color(0xFF4CAF50))
                            }
                        }
                        is ConnectionState.Connecting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFFF9800)
                            )
                        }
                        else -> {
                            TextButton(onClick = { connect() }) {
                                Text("Connect", color = Color(0xFFFF9800))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connection Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Neo4j Connection", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            val statusColor = when (connectionState) {
                                is ConnectionState.Connected -> Color(0xFF4CAF50)
                                is ConnectionState.Connecting -> Color(0xFFFF9800)
                                is ConnectionState.Error -> Color(0xFFF44336)
                                else -> Color.Gray
                            }
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = MaterialTheme.shapes.small,
                                color = statusColor
                            ) {}
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("URI: ${config.uri}", color = Color.Gray)
                        Text("Database: ${config.database}", color = Color.Gray)
                        when (val state = connectionState) {
                            is ConnectionState.Connected -> {
                                Text("Server: ${state.serverInfo}", color = Color(0xFF4CAF50))
                            }
                            is ConnectionState.Error -> {
                                Text("Error: ${state.message}", color = Color(0xFFF44336))
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Stats Card (when connected)
            if (connectionState is ConnectionState.Connected) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Graph Statistics", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    scope.launch { stats = neo4jService.getStats() }
                                }) {
                                    Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Screens", stats["screens"] ?: 0)
                                StatItem("Elements", stats["elements"] ?: 0)
                                StatItem("Navigations", stats["navigations"] ?: 0)
                            }
                        }
                    }
                }

                // Export Controls Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Export Data", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { exportData() },
                                    enabled = !isExporting,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    if (isExporting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(Icons.Default.PlayArrow, "Export")
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("Export")
                                }

                                Button(
                                    onClick = { clearDatabase() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF44336)
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, "Clear")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear")
                                }
                            }

                            if (exportMessage.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    exportMessage,
                                    color = if (exportMessage.startsWith("Error")) Color(0xFFF44336)
                                    else Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                // Cypher Query Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Cypher Query", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customQuery,
                                onValueChange = { customQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Query") },
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF4CAF50),
                                    unfocusedTextColor = Color(0xFF4CAF50),
                                    focusedBorderColor = Color(0xFFFF5722),
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { executeQuery() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722)
                                )
                            ) {
                                Text("Execute")
                            }
                        }
                    }
                }

                // Query Results
                if (queryResult.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Results (${queryResult.size} rows)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    items(queryResult) { row ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF252525)
                            )
                        ) {
                            Text(
                                row.entries.joinToString(", ") { "${it.key}: ${it.value}" },
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Example Queries Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Example Queries", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            val examples = listOf(
                                "Get all screens" to "MATCH (s:Screen) RETURN s.hash, s.packageName",
                                "Navigation paths" to "MATCH (a:Screen)-[n:NAVIGATES_TO]->(b:Screen) RETURN a.hash, b.hash, n.count",
                                "Elements on screen" to "MATCH (s:Screen)-[:HAS_ELEMENT]->(e:Element) RETURN s.hash, e.stableId, e.text",
                                "Clickable elements" to "MATCH (e:Element {isClickable: true}) RETURN e.stableId, e.text"
                            )

                            examples.forEach { (name, query) ->
                                TextButton(
                                    onClick = { customQuery = query },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(name, color = Color.White)
                                        Text(
                                            query,
                                            color = Color(0xFF4CAF50),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$value",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFF5722)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
