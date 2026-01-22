/**
 * CPUStateManagerTest.kt - Unit tests for CPU state management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-27
 */
package com.augmentalis.voiceui.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CPUStateManager fallback implementation
 */
class CPUStateManagerTest {

    private lateinit var manager: CPUStateManager

    @Before
    fun setup() {
        manager = CPUStateManager(maxCacheSize = 100)
    }

    @Test
    fun `cacheStateSync returns true for new entries`() {
        val result = manager.cacheStateSync("key1", "value1")
        assertTrue("New entry should return true", result)
    }

    @Test
    fun `cacheStateSync returns false for unchanged values`() {
        manager.cacheStateSync("key1", "value1")
        val result = manager.cacheStateSync("key1", "value1")
        assertFalse("Unchanged value should return false", result)
    }

    @Test
    fun `cacheStateSync returns true for changed values`() {
        manager.cacheStateSync("key1", "value1")
        val result = manager.cacheStateSync("key1", "value2")
        assertTrue("Changed value should return true", result)
    }

    @Test
    fun `getCachedState returns stored value`() {
        manager.cacheStateSync("key1", "stored_value")
        val result: String? = manager.getCachedState("key1")
        assertEquals("stored_value", result)
    }

    @Test
    fun `getCachedState returns null for missing key`() {
        val result: String? = manager.getCachedState("nonexistent")
        assertNull(result)
    }

    @Test
    fun `clearCache removes all entries`() {
        manager.cacheStateSync("key1", "value1")
        manager.cacheStateSync("key2", "value2")
        manager.clearCache()

        assertNull(manager.getCachedState<String>("key1"))
        assertNull(manager.getCachedState<String>("key2"))
    }

    @Test
    fun `diffStateSync detects new state`() {
        val result = manager.diffStateSync("key1", "value1")
        assertTrue(result.changed)
        assertTrue(result.isNew)
    }

    @Test
    fun `diffStateSync detects changed state`() {
        manager.cacheStateSync("key1", "value1")
        val result = manager.diffStateSync("key1", "value2")
        assertTrue(result.changed)
        assertFalse(result.isNew)
        assertNotNull(result.previousHash)
    }

    @Test
    fun `diffStateSync detects unchanged state`() {
        manager.cacheStateSync("key1", "value1")
        val result = manager.diffStateSync("key1", "value1")
        assertFalse(result.changed)
        assertFalse(result.isNew)
    }

    @Test
    fun `getCacheStats returns valid statistics`() {
        manager.cacheStateSync("key1", "value1")
        manager.cacheStateSync("key2", "value2")

        val stats = manager.getCacheStats()
        assertEquals(2, stats.size)
        assertEquals(100, stats.maxSize)
    }

    @Test
    fun `cache evicts oldest entry when full`() {
        val smallManager = CPUStateManager(maxCacheSize = 3)

        smallManager.cacheStateSync("key1", "value1")
        smallManager.cacheStateSync("key2", "value2")
        smallManager.cacheStateSync("key3", "value3")
        smallManager.cacheStateSync("key4", "value4") // Should evict key1

        val stats = smallManager.getCacheStats()
        assertEquals(3, stats.size)
    }

    @Test
    fun `async diff completes with callback`() = runBlocking {
        var callbackCalled = false
        var resultChanged: Boolean? = null

        manager.updateStateAsync("key1", "value1") { result ->
            callbackCalled = true
            resultChanged = result.changed
        }

        // Give async operation time to complete
        Thread.sleep(100)

        assertTrue(callbackCalled)
        assertTrue(resultChanged == true)
    }

    @Test
    fun `diffTimeNanos is measured for changed state`() {
        manager.cacheStateSync("key1", "value1")
        val result = manager.diffStateSync("key1", "value2")
        assertTrue(result.diffTimeNanos >= 0)
    }

    @Test
    fun `hitRate increases with repeated access`() {
        // Access same keys multiple times
        repeat(10) {
            manager.diffStateSync("key1", "value1")
            manager.diffStateSync("key2", "value2")
        }

        val stats = manager.getCacheStats()
        assertTrue("Hit rate should be positive after repeated access", stats.hitRate > 0)
    }
}
