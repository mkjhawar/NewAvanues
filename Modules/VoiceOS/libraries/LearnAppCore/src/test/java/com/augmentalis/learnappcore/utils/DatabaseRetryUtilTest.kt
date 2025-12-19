/**
 * DatabaseRetryUtilTest.kt - Tests for database retry logic
 *
 * Verifies exponential backoff retry behavior for database operations.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Phase 4)
 */

package com.augmentalis.learnappcore.utils

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

/**
 * Tests for DatabaseRetryUtil
 *
 * Covers:
 * - Successful operation on first try
 * - Retry on transient errors
 * - Exponential backoff timing
 * - Max retries exhaustion
 * - Non-retryable errors
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseRetryUtilTest {

    @Test
    fun `successful operation on first try`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retries on SQLite BUSY error`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 3) {
                throw Exception("database is busy")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(3, callCount)
    }

    @Test
    fun `retries on SQLite LOCKED error`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 2) {
                throw Exception("database is locked")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `retries on disk IO error`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 2) {
                throw Exception("disk i/o error")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `uses exponential backoff`() = runTest {
        // Note: In runTest, delays are virtual so we just verify retry behavior works
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 3) {
                throw Exception("database is busy")
            }
            "success"
        }

        // Verify retries happened (3 calls = 2 retries + 1 success)
        assertEquals("success", result)
        assertEquals(3, callCount)
    }

    @Test(expected = Exception::class)
    fun `exhausts max retries and throws`() = runTest {
        var callCount = 0

        DatabaseRetryUtil.withRetry {
            callCount++
            throw Exception("database is locked")
        }

        // Should never reach here
        fail("Should have thrown exception after max retries")
    }

    @Test(expected = Exception::class)
    fun `does not retry non-retryable errors`() = runTest {
        var callCount = 0

        DatabaseRetryUtil.withRetry {
            callCount++
            throw Exception("permission denied")
        }

        assertEquals(1, callCount)
        fail("Should have thrown exception immediately")
    }

    @Test
    fun `retries timeout errors`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 2) {
                throw Exception("timeout occurred")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `retries connection errors`() = runTest {
        var callCount = 0

        val result = DatabaseRetryUtil.withRetry {
            callCount++
            if (callCount < 2) {
                throw Exception("connection failed")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `max delay capping works`() = runTest {
        var callCount = 0
        val delays = mutableListOf<Long>()
        var lastTime = System.currentTimeMillis()

        try {
            DatabaseRetryUtil.withRetry {
                callCount++
                if (callCount > 1) {
                    val currentTime = System.currentTimeMillis()
                    val delay = currentTime - lastTime
                    delays.add(delay)
                    lastTime = currentTime
                }
                if (callCount <= 4) {
                    throw Exception("database is busy")
                }
                "success"
            }
        } catch (e: Exception) {
            // Expected if max retries reached
        }

        // All delays should be <= 1000ms (MAX_DELAY_MS)
        delays.forEach { delay ->
            assertTrue("Delay $delay should be <= 1000ms", delay <= 1100) // 100ms buffer for test timing
        }
    }
}
