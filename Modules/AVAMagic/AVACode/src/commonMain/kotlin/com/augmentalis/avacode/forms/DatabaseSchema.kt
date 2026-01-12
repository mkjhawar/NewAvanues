package com.augmentalis.avacode.forms

/**
 * Database schema generator from form definitions.
 *
 * Automatically generates SQL DDL statements to create database tables
 * from form definitions, including columns, constraints, and indexes.
 *
 * @property tableName Database table name
 * @property columns List of column definitions
 * @property constraints List of table constraints
 * @property indexes List of index definitions
 * @since 1.2.0
 */
data class DatabaseSchema(
    val tableName: String,
    val columns: List<ColumnDefinition>,
    val constraints: List<String>,
    val indexes: List<IndexDefinition>
) {
    /**
     * Generate CREATE TABLE SQL statement.
     */
    fun toCreateTableSQL(dialect: SQLDialect = SQLDialect.SQLITE): String {
        val columnDefs = columns.joinToString(",\n    ") { it.toSQL(dialect) }
        val constraintDefs = if (constraints.isNotEmpty()) {
            ",\n    " + constraints.joinToString(",\n    ")
        } else ""

        return """
            |CREATE TABLE IF NOT EXISTS $tableName (
            |    id INTEGER PRIMARY KEY ${if (dialect == SQLDialect.SQLITE) "AUTOINCREMENT" else "AUTO_INCREMENT"},
            |    $columnDefs,
            |    created_at ${dialect.timestampType} DEFAULT ${dialect.currentTimestamp},
            |    updated_at ${dialect.timestampType} DEFAULT ${dialect.currentTimestamp}$constraintDefs
            |);
        """.trimMargin()
    }

    /**
     * Generate CREATE INDEX SQL statements.
     */
    fun toCreateIndexSQL(): List<String> {
        return indexes.map { index ->
            val columns = index.columns.joinToString(", ")
            val unique = if (index.unique) "UNIQUE " else ""
            "CREATE ${unique}INDEX IF NOT EXISTS ${index.name} ON $tableName ($columns);"
        }
    }

    /**
     * Generate complete SQL DDL (tables + indexes).
     */
    fun toSQL(dialect: SQLDialect = SQLDialect.SQLITE): String {
        val createTable = toCreateTableSQL(dialect)
        val createIndexes = toCreateIndexSQL()
        return if (createIndexes.isNotEmpty()) {
            createTable + "\n\n" + createIndexes.joinToString("\n")
        } else {
            createTable
        }
    }

    companion object {
        /**
         * Generate schema from form definition.
         */
        fun fromForm(form: FormDefinition): DatabaseSchema {
            val columns = form.fields.map { field ->
                ColumnDefinition(
                    name = field.id,
                    type = field.type.sqlType,
                    nullable = field.databaseConfig.nullable,
                    unique = field.databaseConfig.unique,
                    defaultValue = field.databaseConfig.defaultValue,
                    checkConstraint = field.databaseConfig.checkConstraint
                )
            }

            val constraints = mutableListOf<String>()
            val indexes = mutableListOf<IndexDefinition>()

            // Add unique constraints
            form.fields.filter { it.databaseConfig.unique }.forEach { field ->
                constraints.add("UNIQUE (${field.id})")
            }

            // Add indexes
            form.fields.filter { it.databaseConfig.indexed }.forEach { field ->
                indexes.add(
                    IndexDefinition(
                        name = "idx_${form.id}_${field.id}",
                        columns = listOf(field.id),
                        unique = field.databaseConfig.unique
                    )
                )
            }

            return DatabaseSchema(
                tableName = form.id,
                columns = columns,
                constraints = constraints,
                indexes = indexes
            )
        }
    }
}

/**
 * Database column definition.
 */
data class ColumnDefinition(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val unique: Boolean,
    val defaultValue: String?,
    val checkConstraint: String?
) {
    fun toSQL(dialect: SQLDialect): String {
        val parts = mutableListOf<String>()
        parts.add(name)
        parts.add(type)

        if (!nullable) parts.add("NOT NULL")
        if (defaultValue != null) parts.add("DEFAULT $defaultValue")
        if (checkConstraint != null) parts.add("CHECK ($checkConstraint)")

        return parts.joinToString(" ")
    }
}

/**
 * Database index definition.
 */
data class IndexDefinition(
    val name: String,
    val columns: List<String>,
    val unique: Boolean = false
)

/**
 * SQL dialect configuration.
 */
enum class SQLDialect(
    val timestampType: String,
    val currentTimestamp: String,
    val autoIncrement: String
) {
    SQLITE("TIMESTAMP", "CURRENT_TIMESTAMP", "AUTOINCREMENT"),
    MYSQL("TIMESTAMP", "CURRENT_TIMESTAMP", "AUTO_INCREMENT"),
    POSTGRESQL("TIMESTAMP", "CURRENT_TIMESTAMP", "SERIAL"),
    H2("TIMESTAMP", "CURRENT_TIMESTAMP", "AUTO_INCREMENT")
}
