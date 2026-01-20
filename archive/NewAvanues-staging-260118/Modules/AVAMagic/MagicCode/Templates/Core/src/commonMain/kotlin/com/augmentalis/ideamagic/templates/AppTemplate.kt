package com.augmentalis.magicui.templates

import com.augmentalis.avanues.avacode.forms.Form
import com.augmentalis.avanues.avacode.workflows.Workflow
import com.augmentalis.magicui.ui.core.Component

/**
 * Base interface for all app templates.
 *
 * An `AppTemplate` defines a complete application structure including:
 * - Metadata (name, description, platforms)
 * - Forms (data input structures)
 * - Workflows (multi-step processes)
 * - UI components (screens and layouts)
 * - Features (capabilities that can be enabled/disabled)
 * - Database schema
 *
 * **Example**:
 * ```kotlin
 * object ECommerceTemplate : AppTemplate {
 *     override val metadata = TemplateMetadata.ECOMMERCE
 *     override val forms = listOf(ProductForm, CheckoutForm, OrderForm)
 *     override val workflows = listOf(CheckoutWorkflow, OrderTrackingWorkflow)
 *     override val features = Feature.ECOMMERCE_DEFAULTS
 *     // ...
 * }
 * ```
 *
 * @since 1.0.0
 */
sealed interface AppTemplate {
    /**
     * Template metadata (name, description, version, etc.)
     */
    val metadata: TemplateMetadata

    /**
     * Forms included in this template.
     */
    val forms: List<Form<*>>

    /**
     * Workflows included in this template.
     */
    val workflows: List<Workflow<*>>

    /**
     * UI components/screens included in this template.
     */
    val components: List<ComponentTemplate>

    /**
     * Default features enabled for this template.
     *
     * Can be customized during app configuration.
     */
    val features: Set<Feature>

    /**
     * Database schema for this template.
     */
    val database: DatabaseTemplate

    /**
     * Dependencies required by this template.
     */
    val dependencies: Set<Dependency>
        get() = emptySet()

    /**
     * Build configuration for this template.
     */
    val buildConfig: BuildConfig
        get() = BuildConfig.DEFAULT

    /**
     * Generate database schema SQL for the specified dialect.
     *
     * @param dialect SQL dialect to generate (SQLite, PostgreSQL, MySQL, SQL Server)
     * @return SQL statements to create database schema
     */
    fun generateDatabaseSchema(dialect: SQLDialect): String {
        return database.toSQL(dialect)
    }

    /**
     * Validate that the template is properly configured.
     *
     * @throws IllegalStateException if validation fails
     */
    fun validate() {
        require(forms.isNotEmpty()) { "Template must have at least one form" }
        require(components.isNotEmpty()) { "Template must have at least one component" }
        require(features.isNotEmpty()) { "Template must have at least one feature enabled" }
    }

    companion object {
        /**
         * E-Commerce template.
         */
        val ECOMMERCE: AppTemplate
            get() = throw NotImplementedError("E-Commerce template will be implemented in Phase 7 Week 2")

        /**
         * Task Management template.
         */
        val TASK_MANAGEMENT: AppTemplate
            get() = throw NotImplementedError("Task Management template will be implemented in Phase 7 Week 3")

        /**
         * Social Media template.
         */
        val SOCIAL_MEDIA: AppTemplate
            get() = throw NotImplementedError("Social Media template will be implemented in Phase 7 Week 3")

        /**
         * Learning Management System template.
         */
        val LMS: AppTemplate
            get() = throw NotImplementedError("LMS template will be implemented in Phase 7 Week 4")

        /**
         * Healthcare template.
         */
        val HEALTHCARE: AppTemplate
            get() = throw NotImplementedError("Healthcare template will be implemented in Phase 7 Week 4")

        /**
         * All available templates.
         */
        val ALL: List<AppTemplate>
            get() = listOf()  // Will be populated as templates are implemented
    }
}

/**
 * Represents a UI component/screen template.
 *
 * @property name Component name (e.g., "ProductCatalog", "ShoppingCart")
 * @property type Component type (screen, dialog, widget)
 * @property component The actual component definition
 */
data class ComponentTemplate(
    val name: String,
    val type: ComponentType,
    val component: Component<*>
) {
    init {
        require(name.isNotBlank()) { "Component name cannot be blank" }
    }
}

/**
 * Types of UI components.
 */
enum class ComponentType {
    SCREEN,      // Full screen
    DIALOG,      // Modal dialog
    WIDGET,      // Reusable widget
    LAYOUT       // Layout container
}

/**
 * Database schema template.
 *
 * @property tables List of table definitions
 * @property relationships Foreign key relationships
 * @property indices Database indices for performance
 */
data class DatabaseTemplate(
    val tables: List<TableDefinition>,
    val relationships: List<Relationship> = emptyList(),
    val indices: List<Index> = emptyList()
) {
    /**
     * Generate SQL schema for the specified dialect.
     */
    fun toSQL(dialect: SQLDialect): String = buildString {
        // Generate CREATE TABLE statements
        tables.forEach { table ->
            appendLine(table.toSQL(dialect))
            appendLine()
        }

        // Generate CREATE INDEX statements
        indices.forEach { index ->
            appendLine(index.toSQL(dialect))
            appendLine()
        }
    }
}

/**
 * Database table definition.
 *
 * @property name Table name
 * @property columns List of columns
 * @property primaryKey Primary key column name
 */
data class TableDefinition(
    val name: String,
    val columns: List<ColumnDefinition>,
    val primaryKey: String
) {
    init {
        require(name.isNotBlank()) { "Table name cannot be blank" }
        require(columns.isNotEmpty()) { "Table must have at least one column" }
        require(columns.any { it.name == primaryKey }) {
            "Primary key '$primaryKey' must be a column in the table"
        }
    }

    /**
     * Generate SQL CREATE TABLE statement.
     */
    fun toSQL(dialect: SQLDialect): String = buildString {
        append("CREATE TABLE $name (\n")

        val columnSQL = columns.joinToString(",\n") { column ->
            "    ${column.toSQL(dialect)}"
        }
        append(columnSQL)

        append(",\n    PRIMARY KEY ($primaryKey)")
        append("\n)")

        when (dialect) {
            SQLDialect.MYSQL -> append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            else -> {}
        }

        append(";")
    }
}

/**
 * Database column definition.
 *
 * @property name Column name
 * @property type SQL data type
 * @property nullable Whether column allows NULL
 * @property defaultValue Default value (optional)
 * @property unique Whether column has UNIQUE constraint
 * @property check CHECK constraint (optional)
 */
data class ColumnDefinition(
    val name: String,
    val type: String,
    val nullable: Boolean = false,
    val defaultValue: String? = null,
    val unique: Boolean = false,
    val check: String? = null
) {
    init {
        require(name.isNotBlank()) { "Column name cannot be blank" }
        require(type.isNotBlank()) { "Column type cannot be blank" }
    }

    /**
     * Generate SQL column definition.
     */
    fun toSQL(dialect: SQLDialect): String = buildString {
        append("$name $type")

        if (!nullable) append(" NOT NULL")
        if (unique) append(" UNIQUE")
        if (defaultValue != null) append(" DEFAULT $defaultValue")
        if (check != null) append(" CHECK($check)")
    }
}

/**
 * Foreign key relationship.
 *
 * @property fromTable Source table
 * @property fromColumn Source column
 * @property toTable Referenced table
 * @property toColumn Referenced column
 */
data class Relationship(
    val fromTable: String,
    val fromColumn: String,
    val toTable: String,
    val toColumn: String
)

/**
 * Database index definition.
 *
 * @property name Index name
 * @property table Table name
 * @property columns Columns to index
 * @property unique Whether index is unique
 */
data class Index(
    val name: String,
    val table: String,
    val columns: List<String>,
    val unique: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Index name cannot be blank" }
        require(table.isNotBlank()) { "Table name cannot be blank" }
        require(columns.isNotEmpty()) { "Index must have at least one column" }
    }

    /**
     * Generate SQL CREATE INDEX statement.
     */
    fun toSQL(dialect: SQLDialect): String {
        val uniqueKeyword = if (unique) "UNIQUE " else ""
        val columnList = columns.joinToString(", ")
        return "CREATE ${uniqueKeyword}INDEX $name ON $table ($columnList);"
    }
}

/**
 * SQL database dialects.
 */
enum class SQLDialect {
    SQLITE,
    POSTGRESQL,
    MYSQL,
    SQLSERVER
}

/**
 * External dependency.
 *
 * @property groupId Maven group ID
 * @property artifactId Maven artifact ID
 * @property version Dependency version
 */
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    val coordinate: String
        get() = "$groupId:$artifactId:$version"
}

/**
 * Build configuration.
 *
 * @property minAndroidSdk Minimum Android SDK version
 * @property targetAndroidSdk Target Android SDK version
 * @property minIOSVersion Minimum iOS version
 * @property jvmTarget JVM target version
 */
data class BuildConfig(
    val minAndroidSdk: Int = 26,
    val targetAndroidSdk: Int = 34,
    val minIOSVersion: String = "15.0",
    val jvmTarget: String = "17"
) {
    companion object {
        val DEFAULT = BuildConfig()
    }
}
