package com.augmentalis.avacode.templates

/**
 * Core engine for generating applications from templates.
 *
 * Takes an `AppConfig` and generates a complete Kotlin Multiplatform project with:
 * - Project structure (gradle files, modules, source directories)
 * - Database schema and migrations
 * - UI components and screens
 * - Forms and workflows
 * - Build configuration
 * - Platform-specific code
 *
 * **Usage**:
 * ```kotlin
 * val config = generateApp {
 *     template = AppTemplate.ECOMMERCE
 *     branding { name = "My Shop"; package = "com.myshop" }
 *     database { dialect = SQLDialect.POSTGRESQL }
 * }
 *
 * TemplateGenerator.generate(config.build(), "/path/to/output")
 * ```
 *
 * @since 1.0.0
 */
object TemplateGenerator {
    /**
     * Generate a complete project from configuration.
     *
     * @param config App configuration
     * @param outputPath Output directory path
     * @return Generated project structure
     */
    fun generate(config: AppConfig, outputPath: String): GeneratedProject {
        // Validate configuration
        config.validate()

        // Create project structure
        val project = createProjectStructure(config, outputPath)

        // Generate files
        generateGradleFiles(config, project)
        generateDatabaseFiles(config, project)
        generateSharedModule(config, project)
        generateAndroidModule(config, project)
        generateIOSModule(config, project)
        generateDesktopModule(config, project)
        generateDocumentation(config, project)

        return project
    }

    /**
     * Create base project directory structure.
     */
    private fun createProjectStructure(config: AppConfig, outputPath: String): GeneratedProject {
        val name = config.branding.name.replace(" ", "")
        val packagePath = config.branding.package.replace(".", "/")

        return GeneratedProject(
            rootPath = "$outputPath/$name",
            name = name,
            packageName = config.branding.package,
            modules = buildList {
                add(Module("shared", "commonMain/kotlin/$packagePath"))
                if (Platform.ANDROID in config.platforms.targets) {
                    add(Module("android", "main/kotlin/$packagePath"))
                }
                if (Platform.IOS in config.platforms.targets) {
                    add(Module("ios", ""))
                }
                if (Platform.DESKTOP in config.platforms.targets) {
                    add(Module("desktop", "main/kotlin/$packagePath"))
                }
            }
        )
    }

    /**
     * Generate Gradle build files.
     */
    private fun generateGradleFiles(config: AppConfig, project: GeneratedProject) {
        // Root build.gradle.kts
        project.addFile(
            path = "build.gradle.kts",
            content = generateRootBuildGradle(config)
        )

        // settings.gradle.kts
        project.addFile(
            path = "settings.gradle.kts",
            content = generateSettingsGradle(project)
        )

        // gradle.properties
        project.addFile(
            path = "gradle.properties",
            content = generateGradleProperties(config)
        )

        // Module build files
        project.modules.forEach { module ->
            project.addFile(
                path = "${module.name}/build.gradle.kts",
                content = generateModuleBuildGradle(config, module)
            )
        }
    }

    /**
     * Generate database schema and migration files.
     */
    private fun generateDatabaseFiles(config: AppConfig, project: GeneratedProject) {
        // Generate SQL schema
        val schema = config.template.generateDatabaseSchema(config.database.dialect)

        project.addFile(
            path = "shared/src/commonMain/resources/db/schema.sql",
            content = schema
        )

        // Generate migration files if enabled
        if (config.database.migrations) {
            project.addFile(
                path = "shared/src/commonMain/resources/db/migration/V1__initial_schema.sql",
                content = schema
            )
        }

        // Generate repository classes
        config.template.database.tables.forEach { table ->
            val repositoryCode = generateRepository(config, table)
            project.addFile(
                path = "shared/src/commonMain/kotlin/${config.branding.package.replace(".", "/")}/data/${table.name.capitalize()}Repository.kt",
                content = repositoryCode
            )
        }
    }

    /**
     * Generate shared module (common code).
     */
    private fun generateSharedModule(config: AppConfig, project: GeneratedProject) {
        val packagePath = config.branding.package.replace(".", "/")

        // Generate App.kt (main entry point)
        project.addFile(
            path = "shared/src/commonMain/kotlin/$packagePath/App.kt",
            content = generateAppKt(config)
        )

        // Generate forms
        config.template.forms.forEach { form ->
            project.addFile(
                path = "shared/src/commonMain/kotlin/$packagePath/forms/${form.name.capitalize()}Form.kt",
                content = generateFormCode(config, form)
            )
        }

        // Generate workflows
        config.template.workflows.forEach { workflow ->
            project.addFile(
                path = "shared/src/commonMain/kotlin/$packagePath/workflows/${workflow.name.capitalize()}Workflow.kt",
                content = generateWorkflowCode(config, workflow)
            )
        }

        // Generate UI components
        config.template.components.forEach { component ->
            project.addFile(
                path = "shared/src/commonMain/kotlin/$packagePath/ui/${component.name}.kt",
                content = generateComponentCode(config, component)
            )
        }
    }

    /**
     * Generate Android-specific module.
     */
    private fun generateAndroidModule(config: AppConfig, project: GeneratedProject) {
        if (Platform.ANDROID !in config.platforms.targets) return

        val packagePath = config.branding.package.replace(".", "/")

        // Generate AndroidManifest.xml
        project.addFile(
            path = "android/src/main/AndroidManifest.xml",
            content = generateAndroidManifest(config)
        )

        // Generate MainActivity.kt
        project.addFile(
            path = "android/src/main/kotlin/$packagePath/MainActivity.kt",
            content = generateMainActivity(config)
        )
    }

    /**
     * Generate iOS-specific module.
     */
    private fun generateIOSModule(config: AppConfig, project: GeneratedProject) {
        if (Platform.IOS !in config.platforms.targets) return

        // Generate ContentView.swift
        project.addFile(
            path = "ios/${config.branding.name}/ContentView.swift",
            content = generateContentView(config)
        )

        // Generate Info.plist
        project.addFile(
            path = "ios/${config.branding.name}/Info.plist",
            content = generateInfoPlist(config)
        )
    }

    /**
     * Generate Desktop-specific module.
     */
    private fun generateDesktopModule(config: AppConfig, project: GeneratedProject) {
        if (Platform.DESKTOP !in config.platforms.targets) return

        val packagePath = config.branding.package.replace(".", "/")

        // Generate Main.kt
        project.addFile(
            path = "desktop/src/main/kotlin/$packagePath/Main.kt",
            content = generateDesktopMain(config)
        )
    }

    /**
     * Generate documentation files.
     */
    private fun generateDocumentation(config: AppConfig, project: GeneratedProject) {
        // Generate README.md
        project.addFile(
            path = "README.md",
            content = generateReadme(config)
        )

        // Generate SETUP.md
        project.addFile(
            path = "docs/SETUP.md",
            content = generateSetupGuide(config)
        )

        // Generate API.md
        project.addFile(
            path = "docs/API.md",
            content = generateAPIDocumentation(config)
        )
    }

    // ========== Code Generators ==========

    private fun generateRootBuildGradle(config: AppConfig): String = """
        plugins {
            kotlin("multiplatform") version "1.9.20" apply false
            kotlin("android") version "1.9.20" apply false
            id("com.android.application") version "8.1.4" apply false
            id("com.android.library") version "8.1.4" apply false
        }

        allprojects {
            repositories {
                google()
                mavenCentral()
            }
        }
    """.trimIndent()

    private fun generateSettingsGradle(project: GeneratedProject): String = buildString {
        appendLine("rootProject.name = \"${project.name}\"")
        appendLine()
        project.modules.forEach { module ->
            appendLine("include(\":${module.name}\")")
        }
    }

    private fun generateGradleProperties(config: AppConfig): String = """
        kotlin.code.style=official
        kotlin.mpp.stability.nowarn=true
        android.useAndroidX=true
        android.enableJetifier=false
        org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
    """.trimIndent()

    private fun generateModuleBuildGradle(config: AppConfig, module: Module): String {
        return when (module.name) {
            "shared" -> generateSharedBuildGradle(config)
            "android" -> generateAndroidBuildGradle(config)
            "desktop" -> generateDesktopBuildGradle(config)
            else -> ""
        }
    }

    private fun generateSharedBuildGradle(config: AppConfig): String = """
        plugins {
            kotlin("multiplatform")
            kotlin("plugin.serialization") version "1.9.20"
            id("com.android.library")
        }

        kotlin {
            androidTarget()
            ${if (Platform.IOS in config.platforms.targets) "iosArm64()\niosSimulatorArm64()" else ""}
            ${if (Platform.DESKTOP in config.platforms.targets) "jvm(\"desktop\")" else ""}

            sourceSets {
                val commonMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                        ${generateDependencies(config)}
                    }
                }
            }
        }

        android {
            namespace = "${config.branding.package}"
            compileSdk = ${config.platforms.android.compileSdk}
            defaultConfig {
                minSdk = ${config.platforms.android.minSdk}
            }
        }
    """.trimIndent()

    private fun generateAndroidBuildGradle(config: AppConfig): String = """
        plugins {
            kotlin("android")
            id("com.android.application")
        }

        android {
            namespace = "${config.branding.package}"
            compileSdk = ${config.platforms.android.compileSdk}

            defaultConfig {
                applicationId = "${config.branding.package}"
                minSdk = ${config.platforms.android.minSdk}
                targetSdk = ${config.platforms.android.targetSdk}
                versionCode = 1
                versionName = "1.0.0"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        dependencies {
            implementation(project(":shared"))
            implementation("androidx.activity:activity-compose:1.8.1")
        }
    """.trimIndent()

    private fun generateDesktopBuildGradle(config: AppConfig): String = """
        plugins {
            kotlin("jvm")
            application
        }

        dependencies {
            implementation(project(":shared"))
        }

        application {
            mainClass.set("${config.branding.package}.MainKt")
        }
    """.trimIndent()

    private fun generateDependencies(config: AppConfig): String = buildString {
        config.getDependencies().forEach { dep ->
            appendLine("implementation(\"${dep.coordinate}\")")
        }
    }

    private fun generateRepository(config: AppConfig, table: TableDefinition): String = """
        package ${config.branding.package}.data

        import kotlinx.coroutines.flow.Flow

        /**
         * Repository for ${table.name} table.
         */
        class ${table.name.capitalize()}Repository {
            // Generated repository methods will be added here
            // TODO: Implement database operations
        }
    """.trimIndent()

    private fun generateAppKt(config: AppConfig): String = """
        package ${config.branding.package}

        /**
         * Main application entry point.
         * Generated by IDEAMagic Templates v1.0.0
         */
        class App {
            fun start() {
                println("${config.branding.name} is running!")
                // Application initialization code
            }
        }
    """.trimIndent()

    private fun generateFormCode(config: AppConfig, form: com.augmentalis.avanues.avamagic.avacode.forms.Form<*>): String = """
        package ${config.branding.package}.forms

        // Form implementation for ${form.name}
        // TODO: Generate form code
    """.trimIndent()

    private fun generateWorkflowCode(config: AppConfig, workflow: com.augmentalis.avanues.avamagic.avacode.workflows.Workflow<*>): String = """
        package ${config.branding.package}.workflows

        // Workflow implementation for ${workflow.name}
        // TODO: Generate workflow code
    """.trimIndent()

    private fun generateComponentCode(config: AppConfig, component: ComponentTemplate): String = """
        package ${config.branding.package}.ui

        // Component implementation for ${component.name}
        // TODO: Generate component code
    """.trimIndent()

    private fun generateAndroidManifest(config: AppConfig): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application
                android:label="${config.branding.name}"
                android:theme="@android:style/Theme.Material.Light">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>
        </manifest>
    """.trimIndent()

    private fun generateMainActivity(config: AppConfig): String = """
        package ${config.branding.package}

        import android.os.Bundle
        import androidx.activity.ComponentActivity

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                // UI initialization
            }
        }
    """.trimIndent()

    private fun generateContentView(config: AppConfig): String = """
        import SwiftUI

        struct ContentView: View {
            var body: some View {
                Text("${config.branding.name}")
                    .padding()
            }
        }
    """.trimIndent()

    private fun generateInfoPlist(config: AppConfig): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>CFBundleName</key>
            <string>${config.branding.name}</string>
            <key>CFBundleIdentifier</key>
            <string>${config.branding.package}</string>
        </dict>
        </plist>
    """.trimIndent()

    private fun generateDesktopMain(config: AppConfig): String = """
        package ${config.branding.package}

        fun main() {
            val app = App()
            app.start()
        }
    """.trimIndent()

    private fun generateReadme(config: AppConfig): String = """
        # ${config.branding.name}

        Generated by **IDEAMagic Templates** v1.0.0

        ## Template
        ${config.template.metadata.name} (${config.template.metadata.description})

        ## Features
        ${config.features.joinToString("\n") { "- ${it.description}" }}

        ## Platform Targets
        ${config.platforms.targets.joinToString(", ") { it.displayName }}

        ## Getting Started

        ### Android
        ```bash
        ./gradlew :android:installDebug
        ```

        ### iOS
        ```bash
        cd ios && xcodebuild
        ```

        ### Desktop
        ```bash
        ./gradlew :desktop:run
        ```

        ## Documentation
        See `docs/` directory for detailed documentation.
    """.trimIndent()

    private fun generateSetupGuide(config: AppConfig): String = """
        # Setup Guide

        ## Database Setup
        - Dialect: ${config.database.dialect}
        - Host: ${config.database.host}:${config.database.port}
        - Database: ${config.database.name}

        ## Environment Variables
        ${if (config.payments != null) "- PAYMENT_API_KEY: Your ${config.payments.provider} API key" else ""}

        ## Build & Run
        See README.md for platform-specific instructions.
    """.trimIndent()

    private fun generateAPIDocumentation(config: AppConfig): String = """
        # API Documentation

        ## Forms
        ${config.template.forms.joinToString("\n") { "- ${it.name}" }}

        ## Workflows
        ${config.template.workflows.joinToString("\n") { "- ${it.name}" }}

        ## Database Schema
        ${config.template.database.tables.joinToString("\n") { "- ${it.name}" }}
    """.trimIndent()

    private fun String.capitalize(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Represents a generated project.
 *
 * @property rootPath Root directory path
 * @property name Project name
 * @property packageName Package name
 * @property modules List of modules
 */
data class GeneratedProject(
    val rootPath: String,
    val name: String,
    val packageName: String,
    val modules: List<Module>
) {
    private val files: MutableMap<String, String> = mutableMapOf()

    /**
     * Add a file to the project.
     */
    fun addFile(path: String, content: String) {
        files["$rootPath/$path"] = content
    }

    /**
     * Get all generated files.
     */
    fun getFiles(): Map<String, String> = files.toMap()

    /**
     * Write all files to disk.
     */
    fun writeToDisk() {
        // TODO: Implement file writing
        // This would create directories and write all files
    }
}

/**
 * Represents a project module.
 *
 * @property name Module name (e.g., "shared", "android", "ios")
 * @property sourcePath Source directory path
 */
data class Module(
    val name: String,
    val sourcePath: String
)
