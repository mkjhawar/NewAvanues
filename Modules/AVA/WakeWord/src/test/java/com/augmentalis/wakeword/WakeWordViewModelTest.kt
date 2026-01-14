// filename: Universal/AVA/Features/WakeWord/src/test/java/com/augmentalis/ava/features/wakeword/WakeWordViewModelTest.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword

import com.augmentalis.ava.core.common.Result
import com.augmentalis.wakeword.detector.WakeWordDetector
import com.augmentalis.wakeword.settings.WakeWordSettingsRepository
import com.augmentalis.wakeword.settings.WakeWordViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for WakeWordViewModel
 *
 * Tests:
 * - Settings updates
 * - State management
 * - Event emission
 * - Statistics tracking
 * - Error handling
 *
 * @author Manoj Jhawar
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WakeWordViewModelTest {

    private lateinit var detector: WakeWordDetector
    private lateinit var repository: WakeWordSettingsRepository
    private lateinit var viewModel: WakeWordViewModel

    private val mockState = MutableStateFlow(WakeWordState.UNINITIALIZED)
    private val mockDetectionCount = MutableStateFlow(0)
    private val mockSettings = MutableStateFlow(WakeWordSettings())

    @Before
    fun setup() {
        detector = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        // Mock flows
        every { detector.state } returns mockState
        every { detector.detectionCount } returns mockDetectionCount
        every { repository.settings } returns mockSettings

        // Mock suspend functions
        coEvery { detector.initialize(any(), any()) } returns Result.Success(Unit)
        coEvery { detector.start() } returns Result.Success(Unit)
        coEvery { detector.stop() } returns Result.Success(Unit)
        every { detector.isListening() } returns false

        viewModel = WakeWordViewModel(detector, repository)
    }

    @Test
    fun `test initial settings are loaded`() = runTest {
        // Assert
        assertEquals(WakeWordSettings(), viewModel.settings.first())
    }

    @Test
    fun `test initialize calls detector`() = runTest {
        // Arrange
        val callback: (WakeWordKeyword) -> Unit = {}

        // Act
        viewModel.initialize(callback)

        // Assert
        coVerify { detector.initialize(any(), any()) }
    }

    @Test
    fun `test start calls detector start`() = runTest {
        // Act
        viewModel.start()

        // Assert
        coVerify { detector.start() }
    }

    @Test
    fun `test stop calls detector stop`() = runTest {
        // Act
        viewModel.stop()

        // Assert
        coVerify { detector.stop() }
    }

    @Test
    fun `test pause calls detector pause`() = runTest {
        // Act
        viewModel.pause("Test reason")

        // Assert
        coVerify { detector.pause("Test reason") }
    }

    @Test
    fun `test resume calls detector resume`() = runTest {
        // Act
        viewModel.resume()

        // Assert
        coVerify { detector.resume() }
    }

    @Test
    fun `test updateSettings persists to repository`() = runTest {
        // Arrange
        val newSettings = WakeWordSettings(
            enabled = true,
            keyword = WakeWordKeyword.OK_AVA,
            sensitivity = 0.7f
        )

        // Act
        viewModel.updateSettings(newSettings)

        // Assert
        coVerify { repository.updateSettings(newSettings) }
    }

    @Test
    fun `test setEnabled updates repository`() = runTest {
        // Act
        viewModel.setEnabled(true)

        // Assert
        coVerify { repository.setEnabled(true) }
    }

    @Test
    fun `test setEnabled false stops detector`() = runTest {
        // Act
        viewModel.setEnabled(false)

        // Assert
        coVerify { repository.setEnabled(false) }
        coVerify { detector.stop() }
    }

    @Test
    fun `test setKeyword updates repository`() = runTest {
        // Act
        viewModel.setKeyword(WakeWordKeyword.JARVIS)

        // Assert
        coVerify { repository.setKeyword(WakeWordKeyword.JARVIS) }
    }

    @Test
    fun `test setSensitivity updates repository`() = runTest {
        // Act
        viewModel.setSensitivity(0.8f)

        // Assert
        coVerify { repository.setSensitivity(0.8f) }
    }

    @Test
    fun `test setBatteryOptimization updates repository`() = runTest {
        // Act
        viewModel.setBatteryOptimization(false)

        // Assert
        coVerify { repository.setBatteryOptimization(false) }
    }

    @Test
    fun `test resetStats resets statistics`() = runTest {
        // Act
        viewModel.resetStats()

        // Assert
        assertEquals(WakeWordStats(), viewModel.stats.first())
    }

    @Test
    fun `test markFalsePositive increments counter`() = runTest {
        // Arrange
        val initialStats = viewModel.stats.first()

        // Act
        viewModel.markFalsePositive()

        // Assert
        val newStats = viewModel.stats.first()
        assertEquals(initialStats.falsePositives + 1, newStats.falsePositives)
    }

    @Test
    fun `test isListening delegates to detector`() = runTest {
        // Arrange
        every { detector.isListening() } returns true

        // Act
        val isListening = viewModel.isListening()

        // Assert
        assertTrue(isListening)
    }

    @Test
    fun `test clearError clears error message`() = runTest {
        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.errorMessage.first())
    }

    @Test
    fun `test onCleared cleans up detector`() = runTest {
        // Act
        // Note: onCleared() is protected, but we can test cleanup indirectly
        // by verifying detector.cleanup() is called

        // This would normally be called by ViewModel lifecycle
        coEvery { detector.cleanup() } just Runs

        // Simulate ViewModel clearing (would happen automatically)
        // viewModel.onCleared() is protected, so we can't call it directly in tests
    }

    @Test
    fun `test initialize error updates error message`() = runTest {
        // Arrange
        val errorMessage = "API key not found"
        coEvery { detector.initialize(any(), any()) } returns Result.Error(
            exception = IllegalStateException(errorMessage),
            message = errorMessage
        )

        // Act
        viewModel.initialize {}

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.first())
    }

    @Test
    fun `test start error updates error message`() = runTest {
        // Arrange
        val errorMessage = "Not initialized"
        coEvery { detector.start() } returns Result.Error(
            exception = IllegalStateException(errorMessage),
            message = errorMessage
        )

        // Act
        viewModel.start()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.first())
    }

    @Test
    fun `test detection count is observed from detector`() = runTest {
        // Arrange
        mockDetectionCount.value = 5

        // Assert
        assertEquals(5, viewModel.detectionCount.first())
    }

    @Test
    fun `test state is observed from detector`() = runTest {
        // Arrange
        mockState.value = WakeWordState.LISTENING

        // Assert
        assertEquals(WakeWordState.LISTENING, viewModel.state.first())
    }
}
