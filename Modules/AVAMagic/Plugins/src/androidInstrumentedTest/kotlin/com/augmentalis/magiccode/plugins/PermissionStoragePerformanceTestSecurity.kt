package com.augmentalis.avacode.plugins

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avacode.plugins.PluginLog
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for encrypted permission storage.
 *
 * Tests verify:
 * - Encryption latency < 5ms (FR-002)
 * - Decryption latency < 5ms (FR-002)
 * - Bulk operation performance
 * - Cold start vs warm cache performance
 *
 * ## Test Strategy
 * - **Real-world scenarios**: Test with realistic permission counts
 * - **Statistical significance**: Multiple iterations for accurate measurements
 * - **Warm-up**: Discard first iteration (JIT compilation, cache warming)
 * - **Percentile reporting**: P50, P95, P99 latencies
 *
 * ## Performance Requirements (FR-002)
 * - Single permission save: <5ms
 * - Single permission query: <5ms
 * - 100 permissions save: <500ms
 * - 100 permissions query: <500ms
 *
 * @since 1.1.0
 */
@RunWith(AndroidJUnit4::class)
class PermissionStoragePerformanceTest {

    private lateinit var context: Context
    private lateinit var permissionStorage: PermissionStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cleanupTestFiles()
        permissionStorage = PermissionStorage.create(context)

        // Warm-up: Ensure JIT compilation and cache warming
        warmUp()
    }

    @After
    fun tearDown() {
        cleanupTestFiles()
    }

    private fun cleanupTestFiles() {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        sharedPrefsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("test_") || file.name.startsWith("plugin_permissions")) {
                file.delete()
            }
        }
    }

    private fun warmUp() {
        // Perform warm-up operations to trigger JIT and cache loading
        repeat(10) {
            val pluginId = "warmup.plugin"
            val permission = "warmup.permission.$it"
            permissionStorage.savePermission(pluginId, permission)
            permissionStorage.hasPermission(pluginId, permission)
        }
    }

    /**
     * T017: Performance benchmark for overall encryption operations.
     *
     * **Acceptance Criteria**:
     * - Save operation: P95 < 5ms
     * - Query operation: P95 < 5ms
     * - Bulk operations: Linear scaling
     *
     * **Success Criteria (SC-002, FR-002)**:
     * - Encryption adds <5ms latency
     */
    @Test
    fun testPerformanceBenchmark() = runTest {
        val iterations = 100
        val saveLatencies = mutableListOf<Long>()
        val queryLatencies = mutableListOf<Long>()

        // WHEN: Performing many save/query operations
        repeat(iterations) { i ->
            val pluginId = "com.benchmark.plugin$i"
            val permission = "android.permission.BENCHMARK_$i"

            // Measure save latency
            val saveTime = measureTimeMillis {
                permissionStorage.savePermission(pluginId, permission)
            }
            saveLatencies.add(saveTime)

            // Measure query latency
            val queryTime = measureTimeMillis {
                val hasPermission = permissionStorage.hasPermission(pluginId, permission)
                assertTrue("Permission should exist", hasPermission)
            }
            queryLatencies.add(queryTime)
        }

        // THEN: Calculate statistics
        val saveStats = calculateStats(saveLatencies)
        val queryStats = calculateStats(queryLatencies)

        // Log detailed statistics
        PluginLog.i("PerformanceBenchmark", "=== SAVE LATENCY ===")
        PluginLog.i("PerformanceBenchmark", "P50: ${saveStats.p50}ms")
        PluginLog.i("PerformanceBenchmark", "P95: ${saveStats.p95}ms")
        PluginLog.i("PerformanceBenchmark", "P99: ${saveStats.p99}ms")
        PluginLog.i("PerformanceBenchmark", "Max: ${saveStats.max}ms")

        PluginLog.i("PerformanceBenchmark", "=== QUERY LATENCY ===")
        PluginLog.i("PerformanceBenchmark", "P50: ${queryStats.p50}ms")
        PluginLog.i("PerformanceBenchmark", "P95: ${queryStats.p95}ms")
        PluginLog.i("PerformanceBenchmark", "P99: ${queryStats.p99}ms")
        PluginLog.i("PerformanceBenchmark", "Max: ${queryStats.max}ms")

        // AND: Verify performance requirements (FR-002)
        assertTrue(
            "Save P95 latency should be <5ms (actual: ${saveStats.p95}ms)",
            saveStats.p95 < 5
        )
        assertTrue(
            "Query P95 latency should be <5ms (actual: ${queryStats.p95}ms)",
            queryStats.p95 < 5
        )
    }

    /**
     * T018: Benchmark encryption latency in detail.
     *
     * **Acceptance Criteria**:
     * - Single permission encryption: <5ms
     * - Bulk permission encryption: Linear scaling
     * - Cold start overhead acceptable
     *
     * **Success Criteria (SC-002, FR-002)**:
     * - Encryption performance meets requirements
     */
    @Test
    fun benchmarkEncryptionLatency() = runTest {
        val pluginId = "com.encryption.benchmark"
        val iterations = 1000
        val latencies = mutableListOf<Long>()

        // WHEN: Performing many encryption operations
        repeat(iterations) { i ->
            val permission = "android.permission.ENC_$i"

            val encryptTime = measureTimeMillis {
                permissionStorage.savePermission(pluginId, permission)
            }

            latencies.add(encryptTime)
        }

        // THEN: Calculate statistics
        val stats = calculateStats(latencies)

        // Log detailed statistics
        PluginLog.i("EncryptionBenchmark", "Iterations: $iterations")
        PluginLog.i("EncryptionBenchmark", "P50: ${stats.p50}ms")
        PluginLog.i("EncryptionBenchmark", "P95: ${stats.p95}ms")
        PluginLog.i("EncryptionBenchmark", "P99: ${stats.p99}ms")
        PluginLog.i("EncryptionBenchmark", "Max: ${stats.max}ms")
        PluginLog.i("EncryptionBenchmark", "Min: ${stats.min}ms")

        // AND: Verify performance requirements
        assertTrue(
            "Encryption P95 latency should be <5ms (actual: ${stats.p95}ms)",
            stats.p95 < 5
        )

        // AND: Verify P99 is reasonable (allow some outliers)
        assertTrue(
            "Encryption P99 latency should be <10ms (actual: ${stats.p99}ms)",
            stats.p99 < 10
        )
    }

    /**
     * T019: Benchmark decryption latency in detail.
     *
     * **Acceptance Criteria**:
     * - Single permission decryption: <5ms
     * - Bulk permission decryption: Linear scaling
     * - Cache hits faster than cache misses
     *
     * **Success Criteria (SC-002, FR-002)**:
     * - Decryption performance meets requirements
     */
    @Test
    fun benchmarkDecryptionLatency() = runTest {
        val pluginId = "com.decryption.benchmark"
        val permissionCount = 1000

        // GIVEN: Pre-encrypted permissions
        repeat(permissionCount) { i ->
            val permission = "android.permission.DEC_$i"
            permissionStorage.savePermission(pluginId, permission)
        }

        // WHEN: Performing many decryption operations
        val latencies = mutableListOf<Long>()
        repeat(permissionCount) { i ->
            val permission = "android.permission.DEC_$i"

            val decryptTime = measureTimeMillis {
                val hasPermission = permissionStorage.hasPermission(pluginId, permission)
                assertTrue("Permission should exist", hasPermission)
            }

            latencies.add(decryptTime)
        }

        // THEN: Calculate statistics
        val stats = calculateStats(latencies)

        // Log detailed statistics
        PluginLog.i("DecryptionBenchmark", "Iterations: $permissionCount")
        PluginLog.i("DecryptionBenchmark", "P50: ${stats.p50}ms")
        PluginLog.i("DecryptionBenchmark", "P95: ${stats.p95}ms")
        PluginLog.i("DecryptionBenchmark", "P99: ${stats.p99}ms")
        PluginLog.i("DecryptionBenchmark", "Max: ${stats.max}ms")
        PluginLog.i("DecryptionBenchmark", "Min: ${stats.min}ms")

        // AND: Verify performance requirements
        assertTrue(
            "Decryption P95 latency should be <5ms (actual: ${stats.p95}ms)",
            stats.p95 < 5
        )

        // AND: Verify P99 is reasonable (allow some outliers)
        assertTrue(
            "Decryption P99 latency should be <10ms (actual: ${stats.p99}ms)",
            stats.p99 < 10
        )

        // AND: Test bulk getAllPermissions() performance
        val bulkTime = measureTimeMillis {
            val allPermissions = permissionStorage.getAllPermissions(pluginId)
            assertEquals("Should return all $permissionCount permissions", permissionCount, allPermissions.size)
        }

        PluginLog.i("DecryptionBenchmark", "Bulk getAllPermissions($permissionCount): ${bulkTime}ms")

        // Bulk operation should be <500ms for 1000 permissions (FR-002)
        assertTrue(
            "Bulk decryption of $permissionCount permissions should be <500ms (actual: ${bulkTime}ms)",
            bulkTime < 500
        )
    }

    // ========== Helper Methods ==========

    /**
     * Calculate performance statistics from latency measurements.
     */
    private fun calculateStats(latencies: List<Long>): PerformanceStats {
        val sorted = latencies.sorted()
        val size = sorted.size

        return PerformanceStats(
            p50 = sorted[size / 2],
            p95 = sorted[(size * 0.95).toInt()],
            p99 = sorted[(size * 0.99).toInt()],
            max = sorted.last(),
            min = sorted.first(),
            mean = sorted.average()
        )
    }

    /**
     * Performance statistics data class.
     */
    data class PerformanceStats(
        val p50: Long,
        val p95: Long,
        val p99: Long,
        val max: Long,
        val min: Long,
        val mean: Double
    )
}
