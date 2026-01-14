package com.augmentalis.voiceoscore.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Unit tests for SafeNullHandler utility
 *
 * Tests safe nullable handling patterns to eliminate unsafe force unwraps (!!)
 * Addresses Critical Issue #7 from accessibility code evaluation report
 */
class SafeNullHandlerTest {

    // ========================================
    // requireNotNull Extension Tests
    // ========================================

    @Test
    fun `requireNotNull returns non-null value when not null`() {
        val value: String? = "test"
        val result = value.requireNotNull("Value")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `requireNotNull throws IllegalStateException when null`() {
        val value: String? = null
        val exception = assertFailsWith<IllegalStateException> {
            value.requireNotNull("TestValue")
        }
        assertThat(exception.message).contains("TestValue")
        assertThat(exception.message).contains("must not be null")
    }

    @Test
    fun `requireNotNull includes custom message in exception`() {
        val value: String? = null
        val exception = assertFailsWith<IllegalStateException> {
            value.requireNotNull("BluetoothAdapter", "Initialize Bluetooth before use")
        }
        assertThat(exception.message).contains("BluetoothAdapter")
        assertThat(exception.message).contains("Initialize Bluetooth before use")
    }

    // ========================================
    // orThrow Extension Tests
    // ========================================

    @Test
    fun `orThrow returns non-null value when not null`() {
        val value: String? = "test"
        val result = value.orThrow { IllegalArgumentException("Should not be thrown") }
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `orThrow throws custom exception when null`() {
        val value: String? = null
        val exception = assertFailsWith<IllegalArgumentException> {
            value.orThrow { IllegalArgumentException("Custom error message") }
        }
        assertThat(exception.message).isEqualTo("Custom error message")
    }

    @Test
    fun `orThrow can throw different exception types`() {
        val value: String? = null

        // Test with IllegalStateException
        assertFailsWith<IllegalStateException> {
            value.orThrow { IllegalStateException("State error") }
        }

        // Test with NullPointerException
        assertFailsWith<NullPointerException> {
            value.orThrow { NullPointerException("Null pointer") }
        }
    }

    // ========================================
    // orDefault Extension Tests
    // ========================================

    @Test
    fun `orDefault returns value when not null`() {
        val value: String? = "test"
        val result = value.orDefault("default")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `orDefault returns default when null`() {
        val value: String? = null
        val result = value.orDefault("default")
        assertThat(result).isEqualTo("default")
    }

    @Test
    fun `orDefault works with different types`() {
        val intValue: Int? = null
        assertThat(intValue.orDefault(42)).isEqualTo(42)

        val boolValue: Boolean? = null
        assertThat(boolValue.orDefault(true)).isTrue()

        val listValue: List<String>? = null
        assertThat(listValue.orDefault(emptyList())).isEmpty()
    }

    // ========================================
    // orCompute Extension Tests
    // ========================================

    @Test
    fun `orCompute returns value when not null`() {
        val value: String? = "test"
        var computeCalled = false
        val result = value.orCompute {
            computeCalled = true
            "computed"
        }
        assertThat(result).isEqualTo("test")
        assertThat(computeCalled).isFalse() // Should not compute when value present
    }

    @Test
    fun `orCompute computes default when null`() {
        val value: String? = null
        var computeCalled = false
        val result = value.orCompute {
            computeCalled = true
            "computed"
        }
        assertThat(result).isEqualTo("computed")
        assertThat(computeCalled).isTrue()
    }

    @Test
    fun `orCompute allows lazy expensive computation`() {
        val value: String? = null
        var expensiveCallCount = 0

        fun expensiveComputation(): String {
            expensiveCallCount++
            return "expensive result"
        }

        val result = value.orCompute { expensiveComputation() }
        assertThat(result).isEqualTo("expensive result")
        assertThat(expensiveCallCount).isEqualTo(1)
    }

    // ========================================
    // orLog Extension Tests
    // ========================================

    @Test
    fun `orLog returns value when not null`() {
        val value: String? = "test"
        val result = value.orLog("TAG", "Value is null")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `orLog returns null when null and logs warning`() {
        val value: String? = null
        val result = value.orLog("TestTag", "Test value was null")
        assertThat(result).isNull()
        // Note: Actual logging verification would require a log capture mechanism
        // This test primarily verifies the function returns null correctly
    }

    @Test
    fun `orLog with default returns value when not null`() {
        val value: String? = "test"
        val result = value.orLog("TAG", "Value is null", "default")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `orLog with default returns default when null`() {
        val value: String? = null
        val result = value.orLog("TestTag", "Value was null", "default")
        assertThat(result).isEqualTo("default")
    }

    // ========================================
    // requireAllNotNull Extension Tests
    // ========================================

    @Test
    fun `requireAllNotNull returns list when all non-null`() {
        val a: String? = "first"
        val b: String? = "second"
        val c: String? = "third"

        val result = requireAllNotNull(a, b, c) { "Values" }
        assertThat(result).containsExactly("first", "second", "third").inOrder()
    }

    @Test
    fun `requireAllNotNull throws when any value is null`() {
        val a: String? = "first"
        val b: String? = null
        val c: String? = "third"

        val exception = assertFailsWith<IllegalStateException> {
            requireAllNotNull(a, b, c) { "RequiredValues" }
        }
        assertThat(exception.message).contains("RequiredValues")
        assertThat(exception.message).contains("null values")
    }

    @Test
    fun `requireAllNotNull works with single value`() {
        val a: String? = "only"
        val result = requireAllNotNull(a) { "Single" }
        assertThat(result).containsExactly("only")
    }

    @Test
    fun `requireAllNotNull works with many values`() {
        val values = (1..10).map { it.toString() as String? }
        val result = requireAllNotNull(*values.toTypedArray()) { "Numbers" }
        assertThat(result).hasSize(10)
    }

    // ========================================
    // Safe Chain Tests
    // ========================================

    @Test
    fun `safe chain with requireNotNull prevents crash`() {
        data class Config(val name: String?)
        val config: Config? = Config("test")

        // Safe chain that won't crash
        val result = config?.name.requireNotNull("Config.name")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `safe chain with orDefault provides fallback`() {
        data class Config(val timeout: Int?)
        val config: Config? = null

        val timeout = config?.timeout.orDefault(30)
        assertThat(timeout).isEqualTo(30)
    }

    // ========================================
    // Real-world Pattern Tests
    // ========================================

    @Test
    fun `replacing bluetoothAdapter!! pattern`() {
        // Simulates BluetoothHandler.kt pattern
        var bluetoothAdapter: Any? = null

        // Old pattern: bluetoothAdapter!!.state (crashes if null)
        // New pattern:
        val exception = assertFailsWith<IllegalStateException> {
            bluetoothAdapter.requireNotNull("BluetoothAdapter", "Bluetooth not initialized")
        }
        assertThat(exception.message).contains("BluetoothAdapter")
    }

    @Test
    fun `replacing cursor!! pattern in SafeCursorManager`() {
        // Simulates SafeCursorManager.kt pattern
        fun track(cursor: Any?): Any? = cursor

        val cursor: Any? = "cursor"
        val tracked = track(cursor)

        // Old pattern: processCursor(cursor!!)
        // New pattern:
        val result = tracked.requireNotNull("Cursor", "Query returned null cursor")
        assertThat(result).isEqualTo("cursor")
    }

    @Test
    fun `replacing app!! pattern in AccessibilityScrapingIntegration`() {
        // Simulates AccessibilityScrapingIntegration.kt:1298
        data class AppInfo(val appName: String)
        val app: AppInfo? = null

        // Old pattern: app!!.appName
        // New pattern:
        val appName = app?.appName.orDefault("Unknown App")
        assertThat(appName).isEqualTo("Unknown App")
    }
}
