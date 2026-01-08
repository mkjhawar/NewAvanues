/**
 * SafeCursorManagerInstrumentedTest.kt - Instrumented tests for SafeCursorManager
 *
 * YOLO Phase 1 - Critical Issue #6: Database Cursor Leaks - EMULATOR TESTS
 *
 * Tests SafeCursorManager with real Android database cursors on emulator/device.
 * Validates behavior with actual Android framework components.
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test suite for SafeCursorManager
 *
 * Tests verify cursor management with real SQLite database on Android.
 */
@RunWith(AndroidJUnit4::class)
class SafeCursorManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var database: SQLiteDatabase
    private lateinit var dbFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath("test_cursor_manager.db")

        // Delete existing database
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // Create test database
        database = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

        // Create test table
        database.execSQL("""
            CREATE TABLE test_table (
                id INTEGER PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)

        // Insert test data
        database.execSQL("INSERT INTO test_table (id, value) VALUES (1, 'test1')")
        database.execSQL("INSERT INTO test_table (id, value) VALUES (2, 'test2')")
        database.execSQL("INSERT INTO test_table (id, value) VALUES (3, 'test3')")
    }

    @After
    fun teardown() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        if (::dbFile.isInitialized && dbFile.exists()) {
            dbFile.delete()
        }
    }

    /**
     * TEST 1: Verify SafeCursorManager closes real Android cursor
     */
    @Test
    fun testRealCursorClosedByManager() {
        val cursor = database.rawQuery("SELECT * FROM test_table", null)

        SafeCursorManager().use { manager ->
            manager.track(cursor)
            assertThat(cursor.isClosed).isFalse()
        }

        // Cursor should be closed after manager.use
        assertThat(cursor.isClosed).isTrue()
    }

    /**
     * TEST 2: Verify cursor can read data before close
     */
    @Test
    fun testCursorDataReadable() {
        val values = mutableListOf<String>()

        SafeCursorManager().use { manager ->
            val cursor = manager.track(database.rawQuery("SELECT value FROM test_table ORDER BY id", null))

            while (cursor!!.moveToNext()) {
                values.add(cursor.getString(0))
            }
        }

        assertThat(values).containsExactly("test1", "test2", "test3").inOrder()
    }

    /**
     * TEST 3: Verify useSafely extension function with real cursor
     */
    @Test
    fun testUseSafelyExtensionWithRealCursor() {
        val result = database.rawQuery("SELECT value FROM test_table WHERE id = 1", null).useSafely { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }

        assertThat(result).isEqualTo("test1")
    }

    /**
     * TEST 4: Verify extractValues extension function
     */
    @Test
    fun testExtractValuesExtension() {
        val values = database.rawQuery("SELECT value FROM test_table ORDER BY id", null)
            .extractValues { it.getString(0) }

        assertThat(values).containsExactly("test1", "test2", "test3").inOrder()
    }

    /**
     * TEST 5: Verify getFirstOrNull extension function
     */
    @Test
    fun testGetFirstOrNullExtension() {
        val result = database.rawQuery("SELECT value FROM test_table WHERE id = 2", null)
            .getFirstOrNull { it.getString(0) }

        assertThat(result).isEqualTo("test2")
    }

    /**
     * TEST 6: Verify getFirstOrNull returns null for empty result
     */
    @Test
    fun testGetFirstOrNullReturnsNullWhenEmpty() {
        val result = database.rawQuery("SELECT value FROM test_table WHERE id = 999", null)
            .getFirstOrNull { it.getString(0) }

        assertThat(result).isNull()
    }

    /**
     * TEST 7: Verify multiple cursors all closed
     */
    @Test
    fun testMultipleCursorsAllClosed() {
        val cursor1 = database.rawQuery("SELECT * FROM test_table WHERE id = 1", null)
        val cursor2 = database.rawQuery("SELECT * FROM test_table WHERE id = 2", null)
        val cursor3 = database.rawQuery("SELECT * FROM test_table WHERE id = 3", null)

        SafeCursorManager().use { manager ->
            manager.track(cursor1)
            manager.track(cursor2)
            manager.track(cursor3)

            assertThat(manager.getTrackedCursorCount()).isEqualTo(3)
        }

        // All cursors should be closed
        assertThat(cursor1.isClosed).isTrue()
        assertThat(cursor2.isClosed).isTrue()
        assertThat(cursor3.isClosed).isTrue()
    }

    /**
     * TEST 8: Verify cursor closed even when exception thrown
     */
    @Test
    fun testCursorClosedOnException() {
        val cursor = database.rawQuery("SELECT * FROM test_table", null)

        try {
            SafeCursorManager().use { manager ->
                manager.track(cursor)
                throw IllegalStateException("Test exception")
            }
        } catch (e: IllegalStateException) {
            // Expected
        }

        // Cursor still closed despite exception
        assertThat(cursor.isClosed).isTrue()
    }

    /**
     * TEST 9: Verify queryAndClose helper with real database
     */
    @Test
    fun testQueryAndCloseHelper() {
        val manager = SafeCursorManager()

        val values = manager.queryAndClose(
            queryCursor = { database.rawQuery("SELECT value FROM test_table ORDER BY id", null) },
            processCursor = { cursor ->
                val list = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0))
                }
                list
            }
        )

        assertThat(values).containsExactly("test1", "test2", "test3").inOrder()
        manager.close()
    }

    /**
     * TEST 10: Verify performance with large result set
     */
    @Test
    fun testPerformanceWithLargeResultSet() {
        // Insert 1000 rows
        database.beginTransaction()
        try {
            for (i in 100 until 1100) {
                database.execSQL("INSERT INTO test_table (id, value) VALUES ($i, 'value$i')")
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        val startTime = System.currentTimeMillis()

        val count = database.rawQuery("SELECT COUNT(*) FROM test_table", null).useSafely { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        val duration = System.currentTimeMillis() - startTime

        assertThat(count).isEqualTo(1003) // 3 original + 1000 new
        assertThat(duration).isLessThan(1000L) // Should complete in under 1 second
    }
}
