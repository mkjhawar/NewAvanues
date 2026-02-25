package com.augmentalis.avacode

import com.augmentalis.avacode.forms.DatabaseSchema
import com.augmentalis.avacode.forms.SQLDialect
import com.augmentalis.avacode.forms.form
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseSchemaTest {

    private fun registrationForm() = form("user_registration") {
        textField("username") {
            required()
            unique()
        }
        emailField("email") {
            required()
            unique()
            indexed()
        }
    }

    // ─── DatabaseSchema tests ──────────────────────────────────────────────────

    @Test
    fun tableNameMatchesFormId() {
        val schema = DatabaseSchema.fromForm(registrationForm())
        assertTrue(schema.tableName == "user_registration")
    }

    @Test
    fun toCreateTableSqlContainsExpectedTokens() {
        val schema = DatabaseSchema.fromForm(registrationForm())
        val sql = schema.toCreateTableSQL(SQLDialect.SQLITE)

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS user_registration"), "Missing table declaration")
        assertTrue(sql.contains("AUTOINCREMENT"), "Missing AUTOINCREMENT for SQLite")
        assertTrue(sql.contains("username"), "Missing username column")
        assertTrue(sql.contains("email"), "Missing email column")
        assertTrue(sql.contains("CURRENT_TIMESTAMP"), "Missing timestamp defaults")
    }

    @Test
    fun uniqueFieldsGenerateConstraints() {
        val schema = DatabaseSchema.fromForm(registrationForm())
        // both username and email are unique — constraints list should be non-empty
        assertTrue(schema.constraints.isNotEmpty(), "Expected UNIQUE constraints")
        val joined = schema.constraints.joinToString()
        assertTrue(joined.contains("username") || joined.contains("email"))
    }

    @Test
    fun indexedFieldsGenerateIndexStatements() {
        val schema = DatabaseSchema.fromForm(registrationForm())
        val indexSql = schema.toCreateIndexSQL()
        // email is indexed
        assertTrue(indexSql.isNotEmpty(), "Expected at least one index")
        assertTrue(indexSql.any { it.contains("email") }, "Expected index on email column")
    }

    @Test
    fun toSqlCombinesTableAndIndexes() {
        val schema = DatabaseSchema.fromForm(registrationForm())
        val fullSql = schema.toSQL(SQLDialect.SQLITE)
        assertTrue(fullSql.contains("CREATE TABLE"), "Missing CREATE TABLE")
        assertTrue(fullSql.contains("CREATE") && fullSql.contains("INDEX"), "Missing CREATE INDEX")
    }
}
