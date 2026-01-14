package com.augmentalis.avacode.templates

/**
 * Database configuration for generated apps.
 *
 * Defines database connection, dialect, migrations, and other database-related settings.
 *
 * **Example**:
 * ```kotlin
 * val dbConfig = databaseConfig {
 *     dialect = SQLDialect.POSTGRESQL
 *     host = "localhost"
 *     port = 5432
 *     name = "myapp_db"
 *     username = "postgres"
 *     password = System.getenv("DB_PASSWORD")
 *     migrations = true
 *     poolSize = 10
 * }
 * ```
 *
 * @since 1.0.0
 */
data class DatabaseConfig(
    val dialect: SQLDialect = SQLDialect.SQLITE,
    val host: String = "localhost",
    val port: Int = getDefaultPort(dialect),
    val name: String = "app_db",
    val username: String? = null,
    val password: String? = null,
    val migrations: Boolean = true,
    val poolSize: Int = 10,
    val ssl: Boolean = false,
    val connectionString: String? = null,
    val options: Map<String, String> = emptyMap()
) {
    init {
        require(name.isNotBlank()) { "Database name cannot be blank" }
        require(name.matches(Regex("[a-z0-9_]+"))) {
            "Database name must contain only lowercase letters, numbers, and underscores"
        }
        require(port in 1..65535) { "Port must be in range 1-65535" }
        require(poolSize > 0) { "Pool size must be positive" }

        // Validate credentials for remote databases
        if (dialect != SQLDialect.SQLITE) {
            require(!username.isNullOrBlank()) { "Username required for ${dialect.name}" }
        }
    }

    /**
     * Generate connection string for this database configuration.
     */
    fun getConnectionString(): String {
        if (connectionString != null) return connectionString

        return when (dialect) {
            SQLDialect.SQLITE -> "jdbc:sqlite:$name.db"
            SQLDialect.POSTGRESQL -> buildString {
                append("jdbc:postgresql://$host:$port/$name")
                if (ssl) append("?ssl=true&sslmode=require")
            }
            SQLDialect.MYSQL -> buildString {
                append("jdbc:mysql://$host:$port/$name")
                append("?useSSL=$ssl")
                if (options.isNotEmpty()) {
                    options.forEach { (key, value) ->
                        append("&$key=$value")
                    }
                }
            }
            SQLDialect.SQLSERVER -> buildString {
                append("jdbc:sqlserver://$host:$port")
                append(";databaseName=$name")
                if (ssl) append(";encrypt=true")
            }
        }
    }

    /**
     * Get JDBC driver class name.
     */
    fun getDriverClass(): String = when (dialect) {
        SQLDialect.SQLITE -> "org.sqlite.JDBC"
        SQLDialect.POSTGRESQL -> "org.postgresql.Driver"
        SQLDialect.MYSQL -> "com.mysql.cj.jdbc.Driver"
        SQLDialect.SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    }

    /**
     * Get required dependencies for this database.
     */
    fun getDependencies(): Set<Dependency> = when (dialect) {
        SQLDialect.SQLITE -> setOf(
            Dependency("org.xerial", "sqlite-jdbc", "3.43.0.0")
        )
        SQLDialect.POSTGRESQL -> setOf(
            Dependency("org.postgresql", "postgresql", "42.6.0")
        )
        SQLDialect.MYSQL -> setOf(
            Dependency("com.mysql", "mysql-connector-j", "8.1.0")
        )
        SQLDialect.SQLSERVER -> setOf(
            Dependency("com.microsoft.sqlserver", "mssql-jdbc", "12.4.0.jre11")
        )
    }

    /**
     * Validate database configuration.
     */
    fun validate() {
        // Test connection string generation
        try {
            getConnectionString()
        } catch (e: Exception) {
            throw IllegalStateException("Invalid database configuration: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Default database configuration (SQLite).
         */
        val DEFAULT = DatabaseConfig()

        /**
         * Get default port for SQL dialect.
         */
        private fun getDefaultPort(dialect: SQLDialect): Int = when (dialect) {
            SQLDialect.SQLITE -> 0
            SQLDialect.POSTGRESQL -> 5432
            SQLDialect.MYSQL -> 3306
            SQLDialect.SQLSERVER -> 1433
        }

        /**
         * PostgreSQL configuration.
         */
        fun postgresql(
            host: String = "localhost",
            port: Int = 5432,
            name: String = "app_db",
            username: String = "postgres",
            password: String = ""
        ) = DatabaseConfig(
            dialect = SQLDialect.POSTGRESQL,
            host = host,
            port = port,
            name = name,
            username = username,
            password = password
        )

        /**
         * MySQL configuration.
         */
        fun mysql(
            host: String = "localhost",
            port: Int = 3306,
            name: String = "app_db",
            username: String = "root",
            password: String = ""
        ) = DatabaseConfig(
            dialect = SQLDialect.MYSQL,
            host = host,
            port = port,
            name = name,
            username = username,
            password = password
        )

        /**
         * SQL Server configuration.
         */
        fun sqlserver(
            host: String = "localhost",
            port: Int = 1433,
            name: String = "app_db",
            username: String = "sa",
            password: String = ""
        ) = DatabaseConfig(
            dialect = SQLDialect.SQLSERVER,
            host = host,
            port = port,
            name = name,
            username = username,
            password = password
        )

        /**
         * SQLite configuration.
         */
        fun sqlite(name: String = "app_db") = DatabaseConfig(
            dialect = SQLDialect.SQLITE,
            name = name
        )
    }
}

/**
 * Migration configuration.
 *
 * @property enabled Whether migrations are enabled
 * @property tool Migration tool to use (Flyway, Liquibase)
 * @property locations Migration script locations
 * @property baseline Baseline version for existing databases
 */
data class MigrationConfig(
    val enabled: Boolean = true,
    val tool: MigrationTool = MigrationTool.FLYWAY,
    val locations: List<String> = listOf("db/migration"),
    val baseline: String = "1.0"
) {
    /**
     * Get required dependencies for migration tool.
     */
    fun getDependencies(): Set<Dependency> = when (tool) {
        MigrationTool.FLYWAY -> setOf(
            Dependency("org.flywaydb", "flyway-core", "9.22.3")
        )
        MigrationTool.LIQUIBASE -> setOf(
            Dependency("org.liquibase", "liquibase-core", "4.24.0")
        )
    }

    companion object {
        val DEFAULT = MigrationConfig()
    }
}

/**
 * Database migration tools.
 */
enum class MigrationTool {
    FLYWAY,
    LIQUIBASE
}

/**
 * Connection pool configuration.
 *
 * @property minSize Minimum number of connections
 * @property maxSize Maximum number of connections
 * @property timeout Connection timeout (milliseconds)
 * @property idleTimeout Idle connection timeout (milliseconds)
 * @property maxLifetime Maximum connection lifetime (milliseconds)
 */
data class PoolConfig(
    val minSize: Int = 5,
    val maxSize: Int = 10,
    val timeout: Long = 30000,
    val idleTimeout: Long = 600000,
    val maxLifetime: Long = 1800000
) {
    init {
        require(minSize > 0) { "Min size must be positive" }
        require(maxSize >= minSize) { "Max size must be >= min size" }
        require(timeout > 0) { "Timeout must be positive" }
        require(idleTimeout > 0) { "Idle timeout must be positive" }
        require(maxLifetime > 0) { "Max lifetime must be positive" }
    }

    companion object {
        val DEFAULT = PoolConfig()
    }
}

/**
 * DSL builder for database configuration.
 */
@DslMarker
annotation class DatabaseDsl

/**
 * Build a database configuration.
 */
@DatabaseDsl
fun databaseConfig(builder: DatabaseConfigBuilder.() -> Unit): DatabaseConfig {
    return DatabaseConfigBuilder().apply(builder).build()
}

/**
 * Builder for database configuration.
 */
@DatabaseDsl
class DatabaseConfigBuilder {
    var dialect: SQLDialect = SQLDialect.SQLITE
    var host: String = "localhost"
    var port: Int? = null
    var name: String = "app_db"
    var username: String? = null
    var password: String? = null
    var migrations: Boolean = true
    var poolSize: Int = 10
    var ssl: Boolean = false
    var connectionString: String? = null
    var options: MutableMap<String, String> = mutableMapOf()

    /**
     * Add connection option.
     */
    fun option(key: String, value: String) {
        options[key] = value
    }

    fun build(): DatabaseConfig = DatabaseConfig(
        dialect = dialect,
        host = host,
        port = port ?: getDefaultPort(dialect),
        name = name,
        username = username,
        password = password,
        migrations = migrations,
        poolSize = poolSize,
        ssl = ssl,
        connectionString = connectionString,
        options = options
    )

    private fun getDefaultPort(dialect: SQLDialect): Int = when (dialect) {
        SQLDialect.SQLITE -> 0
        SQLDialect.POSTGRESQL -> 5432
        SQLDialect.MYSQL -> 3306
        SQLDialect.SQLSERVER -> 1433
    }
}
