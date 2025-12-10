# AvaConnect SQLDelight Workflow Generation - Complete Implementation

**Created:** 2025-11-28
**Purpose:** Complete SQLDelight-based workflow generation using AvaConnect UI scraping
**Status:** Implementation Guide
**Target:** MagicUI/MagicCode System

---

## Overview

This document provides the complete implementation for generating apps using:
- **AvaConnect** - UI element scraping via AccessibilityService
- **SQLDelight** - Cross-platform database (Android, iOS, Desktop)
- **AVA RAG** - Documentation retrieval for code generation
- **Gemma 3 LLM** - Code generation from templates and docs

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Voice Command Input                       │
│            "Create a task manager app"                       │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│              AvaConnect UI Template Extraction               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AccessibilityService.extractUITree()                │  │
│  │  → Parse existing apps (Google Tasks, Todoist, etc.) │  │
│  │  → Extract UI patterns (List, Detail, Form screens)  │  │
│  │  → Build UITemplate library                          │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                 AVA RAG Documentation Query                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Query: "How to create SQLDelight database?"        │  │
│  │  → Retrieve SQLDelight schema examples              │  │
│  │  → Retrieve Retrofit API patterns                   │  │
│  │  → Retrieve Compose UI best practices               │  │
│  │  → Assemble context from top 10 chunks              │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                  Gemma 3 LLM Code Generation                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Prompt: Generate app using:                        │  │
│  │    - SQLDelight schema from RAG context             │  │
│  │    - UI templates from AvaConnect                   │  │
│  │    - Retrofit patterns from RAG context             │  │
│  │  Output: Structured JSON with files and code        │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                  Workflow Execution Engine                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  1. Create project structure                        │  │
│  │  2. Write .sq schema files                          │  │
│  │  3. Write Kotlin data access code                   │  │
│  │  4. Write Composable UI screens                     │  │
│  │  5. Update build.gradle.kts                         │  │
│  │  6. Trigger build (optional)                        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Component 1: UI Template Extraction (AvaConnect)

### 1.1 UITemplateExtractor.kt

```kotlin
// File: modules/code-generation/src/commonMain/kotlin/com/augmentalis/codegen/UITemplateExtractor.kt

package com.augmentalis.codegen

import com.augmentalis.avaconnect.ui.UIElement
import com.augmentalis.avaconnect.ui.UIElementTree
import com.augmentalis.avaconnect.ui.ElementType
import com.augmentalis.avaconnect.service.AVAAccessibilityService

/**
 * Extracts UI templates from existing apps via AccessibilityService
 *
 * Purpose: Learn UI patterns from production apps to generate similar layouts
 */
class UITemplateExtractor {

    /**
     * Extract UI template from a reference app
     *
     * @param packageName Target app to scrape (e.g., "com.google.android.apps.tasks")
     * @return UITemplate with screens, components, and navigation patterns
     */
    suspend fun extractTemplate(packageName: String): UITemplate {
        val service = AVAAccessibilityService.getInstance()
            ?: throw IllegalStateException("AccessibilityService not available")

        // Launch app and wait for UI to stabilize
        launchApp(packageName)
        delay(2000)

        // Extract screens by navigating through app
        val screens = mutableListOf<ScreenTemplate>()

        // Screen 1: Main/List screen
        val mainScreen = extractCurrentScreen("main")
        screens.add(mainScreen)

        // Screen 2: Detail/Form screen (tap first item if available)
        val firstClickable = mainScreen.elements.find { it.isClickable }
        if (firstClickable != null) {
            service.click(firstClickable.bounds.centerX, firstClickable.bounds.centerY)
            delay(1000)
            val detailScreen = extractCurrentScreen("detail")
            screens.add(detailScreen)

            // Navigate back
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            delay(500)
        }

        // Screen 3: Add/Create screen (tap FAB if exists)
        val fab = mainScreen.elements.find {
            it.type == ElementType.BUTTON &&
            it.contentDescription?.contains("add", ignoreCase = true) == true
        }
        if (fab != null) {
            service.click(fab.bounds.centerX, fab.bounds.centerY)
            delay(1000)
            val createScreen = extractCurrentScreen("create")
            screens.add(createScreen)
        }

        return UITemplate(
            packageName = packageName,
            appName = getAppName(packageName),
            screens = screens,
            navigationPattern = analyzeNavigationPattern(screens)
        )
    }

    private suspend fun extractCurrentScreen(screenType: String): ScreenTemplate {
        val service = AVAAccessibilityService.getInstance()!!
        val uiTree = service.getCurrentUITree()

        return ScreenTemplate(
            type = screenType,
            layout = analyzeLayout(uiTree.root),
            components = extractComponents(uiTree.root),
            actions = extractActions(uiTree.root)
        )
    }

    private fun analyzeLayout(root: UIElement): LayoutPattern {
        // Detect common layout patterns
        val hasRecyclerView = root.getAllElements()
            .any { it.className?.contains("RecyclerView") == true }

        val hasScrollView = root.getAllElements()
            .any { it.className?.contains("ScrollView") == true }

        val hasFAB = root.getAllElements()
            .any { it.className?.contains("FloatingActionButton") == true }

        return when {
            hasRecyclerView && hasFAB -> LayoutPattern.LIST_WITH_FAB
            hasRecyclerView -> LayoutPattern.LIST
            hasScrollView -> LayoutPattern.FORM
            else -> LayoutPattern.SINGLE_VIEW
        }
    }

    private fun extractComponents(root: UIElement): List<ComponentPattern> {
        val components = mutableListOf<ComponentPattern>()

        root.getAllElements().forEach { element ->
            when {
                element.className?.contains("RecyclerView") == true -> {
                    components.add(ComponentPattern(
                        type = "RecyclerView",
                        properties = mapOf(
                            "orientation" to "vertical",
                            "itemLayout" to detectItemLayout(element)
                        )
                    ))
                }
                element.className?.contains("FloatingActionButton") == true -> {
                    components.add(ComponentPattern(
                        type = "FloatingActionButton",
                        properties = mapOf(
                            "position" to "bottom_end",
                            "icon" to (element.contentDescription ?: "add")
                        )
                    ))
                }
                element.className?.contains("EditText") == true -> {
                    components.add(ComponentPattern(
                        type = "TextField",
                        properties = mapOf(
                            "hint" to (element.text ?: element.contentDescription ?: ""),
                            "inputType" to detectInputType(element)
                        )
                    ))
                }
                element.className?.contains("Button") == true -> {
                    components.add(ComponentPattern(
                        type = "Button",
                        properties = mapOf(
                            "text" to (element.text ?: element.contentDescription ?: ""),
                            "style" to detectButtonStyle(element)
                        )
                    ))
                }
            }
        }

        return components
    }

    private fun extractActions(root: UIElement): List<ActionPattern> {
        val actions = mutableListOf<ActionPattern>()

        root.getAllElements()
            .filter { it.isClickable }
            .forEach { element ->
                val actionName = element.contentDescription ?: element.text ?: "unknown"
                actions.add(ActionPattern(
                    name = actionName,
                    trigger = "click",
                    target = element.resourceId ?: element.className ?: "unknown"
                ))
            }

        return actions
    }

    private fun analyzeNavigationPattern(screens: List<ScreenTemplate>): NavigationPattern {
        return when (screens.size) {
            1 -> NavigationPattern.SINGLE_SCREEN
            2 -> NavigationPattern.LIST_DETAIL
            3 -> NavigationPattern.LIST_DETAIL_CREATE
            else -> NavigationPattern.MULTI_SCREEN
        }
    }

    private fun detectItemLayout(recyclerView: UIElement): String {
        val firstChild = recyclerView.children.firstOrNull()
        return when {
            firstChild?.children?.any { it.className?.contains("ImageView") == true } == true -> "card_with_image"
            firstChild?.children?.size ?: 0 > 2 -> "card_multi_line"
            else -> "card_simple"
        }
    }

    private fun detectInputType(editText: UIElement): String {
        val hint = (editText.text ?: editText.contentDescription ?: "").lowercase()
        return when {
            hint.contains("email") -> "email"
            hint.contains("password") -> "password"
            hint.contains("phone") -> "phone"
            hint.contains("number") -> "number"
            hint.contains("date") -> "date"
            else -> "text"
        }
    }

    private fun detectButtonStyle(button: UIElement): String {
        val desc = (button.contentDescription ?: button.text ?: "").lowercase()
        return when {
            desc.contains("primary") || desc.contains("save") || desc.contains("submit") -> "filled"
            desc.contains("cancel") || desc.contains("dismiss") -> "outlined"
            else -> "text"
        }
    }
}

// Data classes
data class UITemplate(
    val packageName: String,
    val appName: String,
    val screens: List<ScreenTemplate>,
    val navigationPattern: NavigationPattern
)

data class ScreenTemplate(
    val type: String,
    val layout: LayoutPattern,
    val components: List<ComponentPattern>,
    val actions: List<ActionPattern>
)

data class ComponentPattern(
    val type: String,
    val properties: Map<String, String>
)

data class ActionPattern(
    val name: String,
    val trigger: String,
    val target: String
)

enum class LayoutPattern {
    LIST, LIST_WITH_FAB, FORM, SINGLE_VIEW
}

enum class NavigationPattern {
    SINGLE_SCREEN, LIST_DETAIL, LIST_DETAIL_CREATE, MULTI_SCREEN
}
```

---

## Component 2: SQLDelight Code Generator

### 2.1 SQLDelightCodeGenerator.kt

```kotlin
// File: modules/code-generation/src/commonMain/kotlin/com/augmentalis/codegen/SQLDelightCodeGenerator.kt

package com.augmentalis.codegen

import com.augmentalis.ava.features.rag.chat.RAGChatEngine

/**
 * Generates SQLDelight schema and Kotlin code using RAG documentation
 */
class SQLDelightCodeGenerator(
    private val ragEngine: RAGChatEngine
) {

    /**
     * Generate complete SQLDelight implementation
     *
     * @param spec App specification
     * @param template UI template from reference app
     * @return Generated SQLDelight code
     */
    suspend fun generate(
        spec: AppSpec,
        template: UITemplate
    ): SQLDelightWorkflow {

        // Step 1: Query RAG for SQLDelight documentation
        val sqlDelightContext = ragEngine.ask("""
            How to create SQLDelight database schema?
            Include:
            - Table creation syntax
            - INSERT OR REPLACE pattern
            - Query syntax
            - Platform-specific driver setup
            Show examples for Android, iOS, and Desktop.
        """.trimIndent()).collectAsString()

        // Step 2: Generate schema based on app spec
        val schemaFiles = generateSchemaFiles(spec, sqlDelightContext)

        // Step 3: Generate Kotlin data access code
        val dataAccessCode = generateDataAccessCode(spec, sqlDelightContext)

        // Step 4: Generate platform drivers
        val platformDrivers = generatePlatformDrivers(spec, sqlDelightContext)

        // Step 5: Generate UI screens using template
        val uiScreens = generateUIScreens(spec, template)

        // Step 6: Generate build configuration
        val buildConfig = generateBuildConfig(spec)

        return SQLDelightWorkflow(
            schemaFiles = schemaFiles,
            dataAccessCode = dataAccessCode,
            platformDrivers = platformDrivers,
            uiScreens = uiScreens,
            buildConfig = buildConfig
        )
    }

    private suspend fun generateSchemaFiles(
        spec: AppSpec,
        context: String
    ): List<GeneratedFile> {
        val prompt = """
            Based on this SQLDelight documentation:
            $context

            Generate SQLDelight schema for a ${spec.appName} with these entities:
            ${spec.entities.joinToString("\n") { "- ${it.name}: ${it.fields.joinToString()}" }}

            Requirements:
            - Use INSERT OR REPLACE for all inserts
            - Include foreign key constraints where appropriate
            - Add indexes for frequently queried fields
            - Include timestamp fields (created_at, updated_at)

            Output format: One .sq file per entity
        """.trimIndent()

        val llmOutput = ragEngine.ask(prompt).collectAsString()
        return parseSQLFiles(llmOutput)
    }

    private suspend fun generateDataAccessCode(
        spec: AppSpec,
        context: String
    ): List<GeneratedFile> {
        val prompt = """
            Generate Kotlin repository classes using SQLDelight generated queries.

            Entities: ${spec.entities.map { it.name }}

            For each entity, create a repository with:
            - suspend fun insert(entity: Entity)
            - suspend fun update(entity: Entity)
            - suspend fun delete(id: String)
            - fun getAll(): Flow<List<Entity>>
            - fun getById(id: String): Flow<Entity?>

            Use Flow for reactive updates.
            Include error handling with Result wrapper.
        """.trimIndent()

        val llmOutput = ragEngine.ask(prompt).collectAsString()
        return parseKotlinFiles(llmOutput)
    }

    private suspend fun generatePlatformDrivers(
        spec: AppSpec,
        context: String
    ): List<GeneratedFile> {
        return listOf(
            // Android driver
            GeneratedFile(
                path = "src/androidMain/kotlin/DatabaseDriverFactory.android.kt",
                content = """
                    package ${spec.packageName}.data

                    import android.content.Context
                    import app.cash.sqldelight.db.SqlDriver
                    import app.cash.sqldelight.driver.android.AndroidSqliteDriver
                    import ${spec.packageName}.${spec.databaseName}

                    actual class DatabaseDriverFactory(private val context: Context) {
                        actual fun createDriver(): SqlDriver {
                            return AndroidSqliteDriver(
                                schema = ${spec.databaseName}.Schema,
                                context = context,
                                name = "${spec.databaseName}.db"
                            )
                        }
                    }
                """.trimIndent()
            ),

            // iOS driver
            GeneratedFile(
                path = "src/iosMain/kotlin/DatabaseDriverFactory.ios.kt",
                content = """
                    package ${spec.packageName}.data

                    import app.cash.sqldelight.db.SqlDriver
                    import app.cash.sqldelight.driver.native.NativeSqliteDriver
                    import ${spec.packageName}.${spec.databaseName}

                    actual class DatabaseDriverFactory {
                        actual fun createDriver(): SqlDriver {
                            return NativeSqliteDriver(
                                schema = ${spec.databaseName}.Schema,
                                name = "${spec.databaseName}.db"
                            )
                        }
                    }
                """.trimIndent()
            ),

            // Desktop driver
            GeneratedFile(
                path = "src/desktopMain/kotlin/DatabaseDriverFactory.desktop.kt",
                content = """
                    package ${spec.packageName}.data

                    import app.cash.sqldelight.db.SqlDriver
                    import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
                    import ${spec.packageName}.${spec.databaseName}

                    actual class DatabaseDriverFactory {
                        actual fun createDriver(): SqlDriver {
                            return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
                                ${spec.databaseName}.Schema.create(this)
                            }
                        }
                    }
                """.trimIndent()
            )
        )
    }

    private suspend fun generateUIScreens(
        spec: AppSpec,
        template: UITemplate
    ): List<GeneratedFile> {
        val screens = mutableListOf<GeneratedFile>()

        template.screens.forEach { screen ->
            val screenCode = when (screen.type) {
                "main" -> generateListScreen(spec, screen)
                "detail" -> generateDetailScreen(spec, screen)
                "create" -> generateCreateScreen(spec, screen)
                else -> generateGenericScreen(spec, screen)
            }

            screens.add(GeneratedFile(
                path = "src/androidMain/kotlin/ui/${screen.type.capitalize()}Screen.kt",
                content = screenCode
            ))
        }

        return screens
    }

    private fun generateListScreen(spec: AppSpec, screen: ScreenTemplate): String {
        val hasFAB = screen.layout == LayoutPattern.LIST_WITH_FAB

        return """
            package ${spec.packageName}.ui

            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ${spec.entities.first().name}ListScreen(
                viewModel: ${spec.entities.first().name}ViewModel,
                onItemClick: (String) -> Unit,
                onAddClick: () -> Unit
            ) {
                val items by viewModel.items.collectAsState(initial = emptyList())

                Scaffold(
                    ${if (hasFAB) """
                    floatingActionButton = {
                        FloatingActionButton(onClick = onAddClick) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                    """ else ""}
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            ${spec.entities.first().name}Card(
                                item = item,
                                onClick = { onItemClick(item.id) }
                            )
                        }
                    }
                }
            }

            @Composable
            private fun ${spec.entities.first().name}Card(
                item: ${spec.entities.first().name},
                onClick: () -> Unit
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        ${if (spec.entities.first().fields.contains("description")) """
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        """ else ""}
                    }
                }
            }
        """.trimIndent()
    }

    private fun generateBuildConfig(spec: AppSpec): GeneratedFile {
        return GeneratedFile(
            path = "build.gradle.kts",
            content = """
                plugins {
                    alias(libs.plugins.kotlin.multiplatform)
                    alias(libs.plugins.android.library)
                    id("app.cash.sqldelight") version "2.0.1"
                    alias(libs.plugins.compose.compiler)
                }

                kotlin {
                    androidTarget()

                    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
                        it.binaries.framework {
                            baseName = "${spec.appName}"
                        }
                    }

                    jvm("desktop")

                    sourceSets {
                        commonMain.dependencies {
                            implementation("app.cash.sqldelight:runtime:2.0.1")
                            implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                        }

                        androidMain.dependencies {
                            implementation("app.cash.sqldelight:android-driver:2.0.1")
                            implementation("androidx.compose.material3:material3:1.1.2")
                        }

                        iosMain.dependencies {
                            implementation("app.cash.sqldelight:native-driver:2.0.1")
                        }

                        val desktopMain by getting {
                            dependencies {
                                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
                            }
                        }
                    }
                }

                sqldelight {
                    databases {
                        create("${spec.databaseName}") {
                            packageName.set("${spec.packageName}.data.db")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
                        }
                    }
                }
            """.trimIndent()
        )
    }
}

// Helper function
suspend fun Flow<ChatResponse>.collectAsString(): String {
    val builder = StringBuilder()
    collect { response ->
        when (response) {
            is ChatResponse.Streaming -> builder.append(response.text)
            is ChatResponse.Complete -> builder.append(response.fullText)
            else -> {}
        }
    }
    return builder.toString()
}
```

---

## Component 3: Complete Workflow Orchestrator

### 3.1 WorkflowOrchestrator.kt

```kotlin
// File: modules/code-generation/src/commonMain/kotlin/com/augmentalis/codegen/WorkflowOrchestrator.kt

package com.augmentalis.codegen

import com.augmentalis.ava.features.rag.chat.RAGChatEngine
import com.augmentalis.avaconnect.service.AVAAccessibilityService

/**
 * Orchestrates complete app generation workflow
 *
 * Steps:
 * 1. Extract UI template from reference app (AvaConnect)
 * 2. Query RAG for SQLDelight documentation (AVA)
 * 3. Generate code using LLM (Gemma 3)
 * 4. Write files to disk
 * 5. Update build configuration
 */
class WorkflowOrchestrator(
    private val templateExtractor: UITemplateExtractor,
    private val codeGenerator: SQLDelightCodeGenerator,
    private val fileWriter: FileSystemWriter
) {

    suspend fun generateApp(spec: AppSpec): WorkflowResult {
        val steps = mutableListOf<StepResult>()

        try {
            // Step 1: Extract UI template from reference app
            steps.add(StepResult.InProgress("Extracting UI template from ${spec.referenceApp}"))
            val template = templateExtractor.extractTemplate(spec.referenceApp)
            steps.add(StepResult.Success("UI template extracted: ${template.screens.size} screens"))

            // Step 2: Generate code
            steps.add(StepResult.InProgress("Generating SQLDelight code"))
            val workflow = codeGenerator.generate(spec, template)
            steps.add(StepResult.Success("Generated ${workflow.totalFiles} files"))

            // Step 3: Create project structure
            steps.add(StepResult.InProgress("Creating project structure"))
            fileWriter.createDirectory(spec.projectPath)
            fileWriter.createProjectStructure(spec.projectPath)
            steps.add(StepResult.Success("Project structure created"))

            // Step 4: Write schema files
            steps.add(StepResult.InProgress("Writing SQLDelight schema"))
            workflow.schemaFiles.forEach { file ->
                fileWriter.writeFile(
                    path = "${spec.projectPath}/${file.path}",
                    content = file.content
                )
            }
            steps.add(StepResult.Success("${workflow.schemaFiles.size} schema files written"))

            // Step 5: Write Kotlin code
            steps.add(StepResult.InProgress("Writing Kotlin code"))
            workflow.dataAccessCode.forEach { file ->
                fileWriter.writeFile(
                    path = "${spec.projectPath}/${file.path}",
                    content = file.content
                )
            }
            workflow.platformDrivers.forEach { file ->
                fileWriter.writeFile(
                    path = "${spec.projectPath}/${file.path}",
                    content = file.content
                )
            }
            workflow.uiScreens.forEach { file ->
                fileWriter.writeFile(
                    path = "${spec.projectPath}/${file.path}",
                    content = file.content
                )
            }
            steps.add(StepResult.Success("${workflow.dataAccessCode.size + workflow.platformDrivers.size + workflow.uiScreens.size} Kotlin files written"))

            // Step 6: Write build configuration
            steps.add(StepResult.InProgress("Configuring build"))
            fileWriter.writeFile(
                path = "${spec.projectPath}/${workflow.buildConfig.path}",
                content = workflow.buildConfig.content
            )
            steps.add(StepResult.Success("Build configured"))

            return WorkflowResult.Success(
                projectPath = spec.projectPath,
                steps = steps,
                summary = """
                    ✅ Generated ${spec.appName} successfully!

                    Files created:
                    - ${workflow.schemaFiles.size} SQLDelight schema files
                    - ${workflow.dataAccessCode.size} repository files
                    - ${workflow.platformDrivers.size} platform drivers (Android, iOS, Desktop)
                    - ${workflow.uiScreens.size} UI screens
                    - 1 build configuration

                    Total: ${workflow.totalFiles} files

                    Next steps:
                    1. Open project in Android Studio
                    2. Sync Gradle
                    3. Run app on Android/iOS/Desktop
                """.trimIndent()
            )

        } catch (e: Exception) {
            steps.add(StepResult.Error("Generation failed: ${e.message}"))
            return WorkflowResult.Failure(
                error = e,
                steps = steps
            )
        }
    }
}

// Data classes
data class AppSpec(
    val appName: String,
    val packageName: String,
    val projectPath: String,
    val databaseName: String,
    val entities: List<EntitySpec>,
    val referenceApp: String  // Package name of app to learn UI from
)

data class EntitySpec(
    val name: String,
    val fields: List<String>
)

data class SQLDelightWorkflow(
    val schemaFiles: List<GeneratedFile>,
    val dataAccessCode: List<GeneratedFile>,
    val platformDrivers: List<GeneratedFile>,
    val uiScreens: List<GeneratedFile>,
    val buildConfig: GeneratedFile
) {
    val totalFiles: Int
        get() = schemaFiles.size + dataAccessCode.size +
                platformDrivers.size + uiScreens.size + 1
}

data class GeneratedFile(
    val path: String,
    val content: String
)

sealed class StepResult {
    data class InProgress(val message: String) : StepResult()
    data class Success(val message: String) : StepResult()
    data class Error(val message: String) : StepResult()
}

sealed class WorkflowResult {
    data class Success(
        val projectPath: String,
        val steps: List<StepResult>,
        val summary: String
    ) : WorkflowResult()

    data class Failure(
        val error: Exception,
        val steps: List<StepResult>
    ) : WorkflowResult()
}
```

---

## Usage Example

```kotlin
// Initialize components
val templateExtractor = UITemplateExtractor()
val codeGenerator = SQLDelightCodeGenerator(ragEngine)
val fileWriter = FileSystemWriter()
val orchestrator = WorkflowOrchestrator(templateExtractor, codeGenerator, fileWriter)

// Define app specification
val spec = AppSpec(
    appName = "TaskManager",
    packageName = "com.example.taskmanager",
    projectPath = "/path/to/projects/TaskManager",
    databaseName = "TaskDatabase",
    entities = listOf(
        EntitySpec(
            name = "Task",
            fields = listOf("id", "title", "description", "completed", "dueDate")
        ),
        EntitySpec(
            name = "Category",
            fields = listOf("id", "name", "color")
        )
    ),
    referenceApp = "com.google.android.apps.tasks"  // Google Tasks as template
)

// Generate app
val result = orchestrator.generateApp(spec)

when (result) {
    is WorkflowResult.Success -> {
        println(result.summary)
        // Open in IDE or trigger build
    }
    is WorkflowResult.Failure -> {
        println("Generation failed: ${result.error.message}")
        result.steps.forEach { step ->
            if (step is StepResult.Error) {
                println("Error: ${step.message}")
            }
        }
    }
}
```

---

## Voice Integration (VoiceOS)

```kotlin
// VoiceOS command handler
voiceCommand.onCommand("create app (.*)", priority = CommandPriority.HIGH) { match ->
    val appName = match.groups[1]?.value ?: "MyApp"

    voiceFeedback.speak("Creating $appName. Please wait...")

    val spec = AppSpec(
        appName = appName.toPascalCase(),
        packageName = "com.example.${appName.toLowerCase()}",
        projectPath = "/sdcard/Projects/${appName}",
        databaseName = "${appName}Database",
        entities = listOf(
            // Extract entities from voice command or interactive dialog
            EntitySpec(
                name = appName.toPascalCase(),
                fields = listOf("id", "title", "description", "createdAt")
            )
        ),
        referenceApp = "com.google.android.apps.tasks"
    )

    val result = orchestrator.generateApp(spec)

    when (result) {
        is WorkflowResult.Success -> {
            voiceFeedback.speak("$appName created successfully with ${result.steps.size} steps completed")
        }
        is WorkflowResult.Failure -> {
            voiceFeedback.speak("Failed to create $appName: ${result.error.message}")
        }
    }
}
```

---

## Testing

```kotlin
@Test
fun `test complete workflow generation`() = runTest {
    // Given
    val spec = AppSpec(
        appName = "TestApp",
        packageName = "com.test.app",
        projectPath = tempDir.absolutePath,
        databaseName = "TestDatabase",
        entities = listOf(
            EntitySpec("Item", listOf("id", "name"))
        ),
        referenceApp = "com.google.android.apps.tasks"
    )

    // When
    val result = orchestrator.generateApp(spec)

    // Then
    assertTrue(result is WorkflowResult.Success)
    assertTrue(File(tempDir, "build.gradle.kts").exists())
    assertTrue(File(tempDir, "src/commonMain/sqldelight").exists())
    assertTrue(File(tempDir, "src/androidMain/kotlin").exists())
}
```

---

## Benefits

✅ **Cross-platform**: SQLDelight works on Android, iOS, Desktop
✅ **Type-safe**: Compile-time SQL validation
✅ **Template-based**: Learns UI patterns from production apps
✅ **RAG-powered**: Uses real documentation, not hallucinations
✅ **Voice-controlled**: Generate apps hands-free
✅ **Automated**: End-to-end workflow from spec to buildable project

---

**Next:** See `AVA-SQLDELIGHT-MIGRATION-PRIORITY.md` for AVA migration plan.
