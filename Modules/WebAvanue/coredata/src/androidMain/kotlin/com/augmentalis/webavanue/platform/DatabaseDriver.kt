package com.augmentalis.webavanue.platform

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.security.EncryptionManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory as SQLCipherSupportFactory

/**
 * Creates an Android-specific SQLDelight driver for the browser database.
 *
 * SECURITY: Database is encrypted using SQLCipher with AES-256 encryption.
 * Encryption keys are stored in Android Keystore (hardware-backed when available).
 *
 * @param context The Android application context
 * @param useEncryption Whether to enable database encryption (default: true)
 * @return A SqlDriver configured for Android with encryption
 */
fun createAndroidDriver(context: Context, useEncryption: Boolean = true): SqlDriver {
    return if (useEncryption) {
        createEncryptedDriver(context)
    } else {
        createPlaintextDriver(context)
    }
}

/**
 * Creates an encrypted database driver using SQLCipher.
 *
 * Encryption Details:
 * - Algorithm: AES-256-CBC (SQLCipher default)
 * - Key derivation: PBKDF2 with 256,000 iterations (SQLCipher 4.x default)
 * - Key storage: Android Keystore (hardware-backed)
 *
 * CWE-311 Mitigation: Encrypts all sensitive browser data at rest.
 */
private fun createEncryptedDriver(context: Context): SqlDriver {
    // Load SQLCipher native library before ANY database operations
    // This MUST be called before creating SQLCipherSupportFactory or opening database
    System.loadLibrary("sqlcipher")

    val encryptionManager = EncryptionManager(context)
    val passphrase = encryptionManager.getOrCreateDatabasePassphrase()

    // Check if we need to migrate from plaintext database
    val plaintextDbFile = context.getDatabasePath("browser.db")
    val encryptedDbFile = context.getDatabasePath("browser_encrypted.db")

    if (plaintextDbFile.exists() && !encryptedDbFile.exists()) {
        println("DatabaseDriver: Migrating plaintext database to encrypted format...")
        migratePlaintextToEncrypted(context, passphrase)
    }

    // Create SQLCipher support factory
    val factory = SQLCipherSupportFactory(passphrase, null, false)

    return AndroidSqliteDriver(
        schema = BrowserDatabase.Schema,
        context = context,
        name = "browser_encrypted.db",
        factory = factory,
        callback = object : AndroidSqliteDriver.Callback(BrowserDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                println("DatabaseDriver: Encrypted database opened successfully")
            }
        }
    )
}

/**
 * Creates a plaintext database driver (for testing or legacy support).
 * WARNING: This stores sensitive data unencrypted.
 */
private fun createPlaintextDriver(context: Context): SqlDriver {
    println("DatabaseDriver: WARNING - Using unencrypted database!")
    return AndroidSqliteDriver(
        schema = BrowserDatabase.Schema,
        context = context,
        name = "browser.db"
    )
}

/**
 * Migrates existing plaintext database to encrypted format.
 *
 * Process:
 * 1. Open plaintext database
 * 2. Create new encrypted database
 * 3. Copy all data using ATTACH DATABASE
 * 4. Rename plaintext DB as backup
 * 5. Rename encrypted DB to primary
 *
 * @param context Android context
 * @param passphrase Encryption passphrase for new database
 */
private fun migratePlaintextToEncrypted(context: Context, passphrase: ByteArray) {
    try {
        val plaintextDbPath = context.getDatabasePath("browser.db").absolutePath
        val encryptedDbPath = context.getDatabasePath("browser_encrypted.db").absolutePath
        val backupDbPath = context.getDatabasePath("browser_plaintext_backup.db").absolutePath

        // Create plaintext driver to read existing data
        val plaintextConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("browser.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(BrowserDatabase.Schema.version.toInt()) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val plaintextHelper = FrameworkSQLiteOpenHelperFactory().create(plaintextConfig)
        val plaintextDb = plaintextHelper.writableDatabase

        // Create encrypted database
        val factory = SQLCipherSupportFactory(passphrase, null, false)
        val encryptedConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("browser_encrypted.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(BrowserDatabase.Schema.version.toInt()) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Schema will be created by SQLDelight
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val encryptedHelper = factory.create(encryptedConfig)
        val encryptedDb = encryptedHelper.writableDatabase

        // Execute schema creation in encrypted database
        BrowserDatabase.Schema.create(object : SqlDriver {
            override fun close() {
                encryptedDb.close()
            }

            override fun currentTransaction(): app.cash.sqldelight.Transacter.Transaction? = null

            override fun execute(
                identifier: Int?,
                sql: String,
                parameters: Int,
                binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
            ): app.cash.sqldelight.db.QueryResult<Long> {
                encryptedDb.execSQL(sql)
                return app.cash.sqldelight.db.QueryResult.Value(0L)
            }

            override fun <R> executeQuery(
                identifier: Int?,
                sql: String,
                mapper: (app.cash.sqldelight.db.SqlCursor) -> app.cash.sqldelight.db.QueryResult<R>,
                parameters: Int,
                binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
            ): app.cash.sqldelight.db.QueryResult<R> {
                throw UnsupportedOperationException("Not needed for migration")
            }

            override fun newTransaction(): app.cash.sqldelight.db.QueryResult<app.cash.sqldelight.Transacter.Transaction> {
                throw UnsupportedOperationException("Not needed for migration")
            }

            override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
                // No-op for migration
            }

            override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
                // No-op for migration
            }

            override fun notifyListeners(vararg queryKeys: String) {
                // No-op for migration
            }
        })

        // Copy data from plaintext to encrypted using SQL ATTACH
        // Note: This is a simplified approach. Full implementation would use
        // SQLDelight queries to copy table-by-table for better error handling
        val tables = listOf(
            "tab", "favorite", "history_entry", "download",
            "browser_settings", "site_permission"
        )

        plaintextDb.beginTransaction()
        try {
            for (table in tables) {
                // Get all data from plaintext database
                val cursor = plaintextDb.query("SELECT * FROM $table")
                val columns = mutableListOf<String>()
                for (i in 0 until cursor.columnCount) {
                    columns.add(cursor.getColumnName(i))
                }

                // Insert into encrypted database
                while (cursor.moveToNext()) {
                    val values = mutableListOf<String>()
                    for (i in 0 until cursor.columnCount) {
                        val value = when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> "NULL"
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                            android.database.Cursor.FIELD_TYPE_STRING -> "'${cursor.getString(i)?.replace("'", "''")}'"
                            android.database.Cursor.FIELD_TYPE_BLOB -> "X'${cursor.getBlob(i)?.joinToString("") {
                                String.format("%02x", it)
                            }}'"
                            else -> "NULL"
                        }
                        values.add(value)
                    }

                    val insertSql = "INSERT INTO $table (${columns.joinToString(",")}) VALUES (${values.joinToString(",")})"
                    encryptedDb.execSQL(insertSql)
                }
                cursor.close()
            }
            plaintextDb.setTransactionSuccessful()
        } finally {
            plaintextDb.endTransaction()
        }

        // Close databases
        plaintextDb.close()
        encryptedDb.close()
        plaintextHelper.close()
        encryptedHelper.close()

        // Rename plaintext database as backup
        val plaintextFile = context.getDatabasePath("browser.db")
        val backupFile = context.getDatabasePath("browser_plaintext_backup.db")
        plaintextFile.renameTo(backupFile)

        println("DatabaseDriver: Migration completed successfully. Plaintext backup saved.")
    } catch (e: Exception) {
        println("DatabaseDriver: Migration failed: ${e.message}")
        e.printStackTrace()
        // Don't delete plaintext database on failure - let user retry
        throw e
    }
}
