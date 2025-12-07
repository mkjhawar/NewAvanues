/**
 * UUIDManagerActivity.kt - Main UI for UUID Manager
 * 
 * Comprehensive interface for UUID registry management, element navigation,
 * and voice command processing
 * 
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 */
package com.augmentalis.uuidmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

class UUIDManagerActivity : ComponentActivity() {
    
    private val viewModel: UUIDViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            UUIDManagerTheme {
                UUIDManagerScreen(viewModel)
            }
        }
    }
}

@Composable
fun UUIDManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = UUIDColors.Primary,
            secondary = UUIDColors.Secondary,
            tertiary = UUIDColors.Accent,
            background = Color(0xFFF5F5F5),
            surface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UUIDManagerScreen(viewModel: UUIDViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by viewModel.uiState.observeAsState(UUIDUiState())
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showCommandDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "UUID Manager",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Universal Unique Identifier System",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRegistry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Badge(
                        containerColor = if (uiState.registeredElements.isNotEmpty()) 
                            UUIDColors.Success else UUIDColors.StatusInactive
                    ) {
                        Text("${uiState.registeredElements.size}")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Test Navigation FAB
                SmallFloatingActionButton(
                    onClick = { viewModel.testSpatialNavigation() },
                    containerColor = UUIDColors.NavForward
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Test Navigation")
                }
                
                // Voice Command FAB
                SmallFloatingActionButton(
                    onClick = { showCommandDialog = true },
                    containerColor = UUIDColors.TypeButton
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Command")
                }
                
                // Register Element FAB
                ExtendedFloatingActionButton(
                    onClick = { showRegisterDialog = true },
                    containerColor = UUIDColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Register")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("main_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Registry Statistics Card
            item {
                RegistryStatisticsCard(uiState.registryStats)
            }
            
            // Quick Actions Card
            item {
                QuickActionsCard(
                    onGenerateUUID = { viewModel.generateNewUUID() },
                    onClearRegistry = { viewModel.clearRegistry() },
                    onExport = { showExportDialog = true },
                    onTestNavigation = { viewModel.testSpatialNavigation() }
                )
            }
            
            // Selected Element Card
            uiState.selectedElement?.let { element ->
                item {
                    SelectedElementCard(
                        element = element,
                        navigationPath = uiState.navigationPath,
                        onNavigate = { direction -> viewModel.navigateToElement(direction) },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                }
            }
            
            // Command Result Card
            uiState.commandResult?.let { result ->
                item {
                    CommandResultCard(result)
                }
            }
            
            // Search Bar
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.searchElements(it) },
                    filterType = uiState.filterType,
                    onFilterChange = { viewModel.filterByType(it) }
                )
            }
            
            // Registered Elements Section
            item {
                Text(
                    "Registered Elements",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Elements Grid
            val displayElements = if (uiState.searchResults.isNotEmpty()) {
                uiState.searchResults
            } else {
                uiState.registeredElements
            }
            
            items(
                items = displayElements,
                key = { it.uuid }
            ) { element ->
                ElementCard(
                    element = element,
                    isSelected = element.uuid == uiState.selectedElement?.uuid,
                    onClick = { viewModel.selectElement(element) },
                    onUnregister = { viewModel.unregisterElement(element.uuid) }
                )
            }
            
            // Command History Section
            if (uiState.commandHistory.isNotEmpty()) {
                item {
                    Text(
                        "Command History",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(
                    items = uiState.commandHistory.take(10),
                    key = { it.id }
                ) { historyItem ->
                    CommandHistoryCard(historyItem)
                }
            }
        }
    }
    
    // Dialogs
    if (showRegisterDialog) {
        RegisterElementDialog(
            onDismiss = { showRegisterDialog = false },
            onRegister = { name, type ->
                viewModel.registerNewElement(name, type)
                showRegisterDialog = false
            }
        )
    }
    
    if (showCommandDialog) {
        VoiceCommandDialog(
            onDismiss = { showCommandDialog = false },
            onCommand = { command ->
                viewModel.processVoiceCommand(command)
                showCommandDialog = false
            }
        )
    }
    
    if (showExportDialog) {
        ExportDialog(
            exportText = viewModel.exportRegistry(),
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun RegistryStatisticsCard(stats: RegistryStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(UUIDGlassConfigs.StatisticsCard)
            .testTag("statistics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Registry Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = UUIDColors.Secondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", stats.totalElements, UUIDColors.Primary)
                StatItem("Active", stats.activeElements, UUIDColors.StatusActive)
                StatItem("Commands", stats.totalCommands, UUIDColors.TypeButton)
            }
            
            if (stats.elementsByType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Elements by Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stats.elementsByType.entries.toList()) { (type, count) ->
                        TypeChip(type, count)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TypeChip(type: String, count: Int) {
    val color = when (type) {
        "button" -> UUIDColors.TypeButton
        "text" -> UUIDColors.TypeText
        "image" -> UUIDColors.TypeImage
        "container" -> UUIDColors.TypeContainer
        "list" -> UUIDColors.TypeList
        "form" -> UUIDColors.TypeForm
        else -> UUIDColors.Primary
    }
    
    AssistChip(
        onClick = { },
        label = { Text("$type: $count") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
fun QuickActionsCard(
    onGenerateUUID: () -> Unit,
    onClearRegistry: () -> Unit,
    onExport: () -> Unit,
    onTestNavigation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(UUIDGlassConfigs.Primary),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Generate",
                    icon = Icons.Default.Fingerprint,
                    onClick = onGenerateUUID,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Test Nav",
                    icon = Icons.Default.Explore,
                    onClick = onTestNavigation,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Export",
                    icon = Icons.Default.Download,
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Clear",
                    icon = Icons.Default.Clear,
                    onClick = onClearRegistry,
                    modifier = Modifier.weight(1f),
                    color = UUIDColors.Error
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = UUIDColors.Primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun SelectedElementCard(
    element: UUIDElementInfo,
    navigationPath: List<String>,
    onNavigate: (String) -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(UUIDGlassConfigs.TargetCard),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Selected Element",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Element Details
            DetailRow("UUID", element.uuid.take(16) + "...")
            DetailRow("Name", element.name ?: "unnamed")
            DetailRow("Type", element.type)
            DetailRow("Status", if (element.isEnabled) "Enabled" else "Disabled")
            DetailRow("Actions", "${element.actionCount} available")
            
            // Navigation Path
            if (navigationPath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Navigation Path",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    itemsIndexed(navigationPath) { index, path ->
                        Text(path, fontSize = 12.sp)
                        if (index < navigationPath.size - 1) {
                            Text(" > ", fontSize = 12.sp)
                        }
                    }
                }
            }
            
            // Spatial Navigation
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Spatial Navigation",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            
            NavigationPad(onNavigate = onNavigate)
        }
    }
}

@Composable
fun NavigationPad(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center button
        IconButton(
            onClick = { },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Center",
                tint = UUIDColors.Primary
            )
        }
        
        // Up
        IconButton(
            onClick = { onNavigate("up") },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
        }
        
        // Down
        IconButton(
            onClick = { onNavigate("down") },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
        }
        
        // Left
        IconButton(
            onClick = { onNavigate("left") },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left")
        }
        
        // Right
        IconButton(
            onClick = { onNavigate("right") },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CommandResultCard(result: CommandResultInfo) {
    val color = if (result.success) UUIDColors.Success else UUIDColors.Error
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                if (result.success) UUIDGlassConfigs.Primary 
                else UUIDGlassConfigs.Error
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Execution: ${result.executionTime}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterType: String,
    onFilterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(UUIDGlassConfigs.Primary),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search elements...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UUIDColors.Primary,
                    unfocusedBorderColor = UUIDColors.Primary.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("all", "button", "text", "image", "container", "list", "form")
                items(types) { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { onFilterChange(type) },
                        label = { Text(type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElementCard(
    element: UUIDElementInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUnregister: () -> Unit
) {
    val typeColor = when (element.type) {
        "button" -> UUIDColors.TypeButton
        "text" -> UUIDColors.TypeText
        "image" -> UUIDColors.TypeImage
        "container" -> UUIDColors.TypeContainer
        "list" -> UUIDColors.TypeList
        "form" -> UUIDColors.TypeForm
        else -> UUIDColors.Primary
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                UUIDGlassConfigs.ElementCard.copy(
                    tintColor = if (isSelected) UUIDColors.StatusSelected else typeColor
                )
            )
            .testTag("element_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (element.type) {
                        "button" -> Icons.Default.SmartButton
                        "text" -> Icons.Default.TextFields
                        "image" -> Icons.Default.Image
                        "container" -> Icons.Default.ViewQuilt
                        "list" -> Icons.Default.List
                        "form" -> Icons.Default.DynamicForm
                        else -> Icons.Default.Widgets
                    },
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Element Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    element.name ?: "Unnamed Element",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        element.uuid.take(8),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (element.isEnabled) {
                        Badge(
                            containerColor = UUIDColors.StatusActive.copy(alpha = 0.2f),
                            contentColor = UUIDColors.StatusActive
                        ) {
                            Text("Active", fontSize = 10.sp)
                        }
                    }
                }
            }
            
            // Actions
            Row {
                if (element.actionCount > 0) {
                    Badge(
                        containerColor = UUIDColors.TypeButton.copy(alpha = 0.1f),
                        contentColor = UUIDColors.TypeButton
                    ) {
                        Text("${element.actionCount}")
                    }
                }
                IconButton(onClick = onUnregister) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Unregister",
                        tint = UUIDColors.Error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CommandHistoryCard(item: CommandHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(UUIDGlassConfigs.HistoryCard),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (item.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (item.success) UUIDColors.Success else UUIDColors.Error,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.command,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        dateFormat.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.targetName != null) {
                        Text(
                            " • ${item.targetName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Text(
                "${item.executionTime}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RegisterElementDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("button") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(UUIDGlassConfigs.Primary),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Register New Element",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Element Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Element Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("button", "text", "image", "container", "list", "form", "dialog", "menu")
                    items(types) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onRegister(name, selectedType)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Register")
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceCommandDialog(
    onDismiss: () -> Unit,
    onCommand: (String) -> Unit
) {
    var command by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(UUIDGlassConfigs.CommandCard),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Voice Command Test",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Enter voice command") },
                    placeholder = { Text("e.g., 'click submit button', 'select first item'") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Example Commands:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    CommandExample("click submit")
                    CommandExample("select first button")
                    CommandExample("move right")
                    CommandExample("navigate to settings")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (command.isNotBlank()) {
                                onCommand(command)
                            }
                        },
                        enabled = command.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute")
                    }
                }
            }
        }
    }
}

@Composable
fun CommandExample(command: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ", style = MaterialTheme.typography.bodySmall)
        Text(
            command,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExportDialog(
    exportText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .glassMorphism(UUIDGlassConfigs.Primary),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Export Registry",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        item {
                            Text(
                                exportText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}