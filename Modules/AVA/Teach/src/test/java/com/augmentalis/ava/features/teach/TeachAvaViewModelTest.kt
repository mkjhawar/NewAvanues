package com.augmentalis.ava.features.teach

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TeachAvaViewModel
 * Tests delete, update, and error handling logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeachAvaViewModelTest {

    private lateinit var repository: TrainExampleRepository
    private lateinit var viewModel: TeachAvaViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true) {
            coEvery { getAllExamples() } returns flowOf(emptyList())
        }
        viewModel = TeachAvaViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init should load training examples`() = runTest(testDispatcher) {
        // Given
        val examples = listOf(
            TrainExample(
                id = 1,
                exampleHash = "hash1",
                utterance = "Test",
                intent = "test",
                locale = "en-US",
                source = TrainExampleSource.MANUAL,
                createdAt = 1000L
            )
        )
        coEvery { repository.getAllExamples() } returns flowOf(examples)

        // When
        viewModel.loadTrainingExamples()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TeachAvaUiState.Success)
        assertEquals(1, (state as TeachAvaUiState.Success).examples.size)
    }

    @Test
    fun `deleteExample should call repository deleteTrainExample`() = runTest(testDispatcher) {
        // Given
        val exampleId = 42L
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(exampleId) } returns Result.Success(Unit)

        // When
        viewModel.deleteExample(exampleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.deleteTrainExample(exampleId) }
    }

    @Test
    fun `deleteExample should update UI state on error`() = runTest(testDispatcher) {
        // Given
        val exampleId = 42L
        val errorMessage = "Database error"
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(exampleId) } returns Result.Error(
            message = errorMessage,
            exception = RuntimeException("DB error")
        )

        // When
        viewModel.deleteExample(exampleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TeachAvaUiState.Error)
        assertEquals(errorMessage, (state as TeachAvaUiState.Error).message)
    }

    @Test
    fun `updateExample should delete old and add new`() = runTest(testDispatcher) {
        // Given
        val example = TrainExample(
            id = 42,
            exampleHash = "hash1",
            utterance = "Updated utterance",
            intent = "test",
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(42) } returns Result.Success(Unit)
        coEvery { repository.addTrainExample(example) } returns Result.Success(example)

        // When
        viewModel.updateExample(example)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.deleteTrainExample(42) }
        coVerify(exactly = 1) { repository.addTrainExample(example) }
    }

    @Test
    fun `updateExample should handle delete failure`() = runTest(testDispatcher) {
        // Given
        val example = TrainExample(
            id = 42,
            exampleHash = "hash1",
            utterance = "Updated",
            intent = "test",
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(42) } returns Result.Error(
            message = "Delete failed",
            exception = RuntimeException()
        )

        // When
        viewModel.updateExample(example)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TeachAvaUiState.Error)
        assertEquals("Failed to update example", (state as TeachAvaUiState.Error).message)
        coVerify(exactly = 0) { repository.addTrainExample(any()) }  // Should not add if delete fails
    }

    @Test
    fun `updateExample should handle add failure after successful delete`() = runTest(testDispatcher) {
        // Given
        val example = TrainExample(
            id = 42,
            exampleHash = "hash1",
            utterance = "Updated",
            intent = "test",
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(42) } returns Result.Success(Unit)
        coEvery { repository.addTrainExample(example) } returns Result.Error(
            message = "Add failed",
            exception = RuntimeException()
        )

        // When
        viewModel.updateExample(example)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TeachAvaUiState.Error)
        assertEquals("Add failed", (state as TeachAvaUiState.Error).message)
    }

    @Test
    fun `addExample should call repository addTrainExample`() = runTest(testDispatcher) {
        // Given
        val example = TrainExample(
            id = 0,
            exampleHash = "hash1",
            utterance = "New example",
            intent = "test",
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.addTrainExample(example) } returns Result.Success(example.copy(id = 1))

        // When
        viewModel.addExample(example)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.addTrainExample(example) }
    }

    @Test
    fun `addExample should update UI state on error`() = runTest(testDispatcher) {
        // Given
        val example = TrainExample(
            id = 0,
            exampleHash = "hash1",
            utterance = "New",
            intent = "test",
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        val errorMessage = "Duplicate entry"
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.addTrainExample(example) } returns Result.Error(
            message = errorMessage,
            exception = RuntimeException()
        )

        // When
        viewModel.addExample(example)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TeachAvaUiState.Error)
        assertEquals(errorMessage, (state as TeachAvaUiState.Error).message)
    }

    @Test
    fun `setLocaleFilter should load examples for specific locale`() = runTest(testDispatcher) {
        // Given
        val locale = "es-ES"
        val examples = listOf(
            TrainExample(
                id = 1,
                exampleHash = "hash1",
                utterance = "Hola",
                intent = "greeting",
                locale = locale,
                source = TrainExampleSource.MANUAL,
                createdAt = 1000L
            )
        )
        coEvery { repository.getExamplesForLocale(locale) } returns flowOf(examples)

        // When
        viewModel.setLocaleFilter(locale)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(locale, viewModel.selectedLocale.value)
        coVerify { repository.getExamplesForLocale(locale) }
    }

    @Test
    fun `setIntentFilter should load examples for specific intent`() = runTest(testDispatcher) {
        // Given
        val intent = "open_app"
        val examples = listOf(
            TrainExample(
                id = 1,
                exampleHash = "hash1",
                utterance = "Open settings",
                intent = intent,
                locale = "en-US",
                source = TrainExampleSource.MANUAL,
                createdAt = 1000L
            )
        )
        coEvery { repository.getExamplesForIntent(intent) } returns flowOf(examples)

        // When
        viewModel.setIntentFilter(intent)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(intent, viewModel.selectedIntent.value)
        coVerify { repository.getExamplesForIntent(intent) }
    }

    @Test
    fun `setIntentFilter with null should load all examples`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())

        // When
        viewModel.setIntentFilter(null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.selectedIntent.value)
        coVerify { repository.getAllExamples() }
    }

    @Test
    fun `clearError should reload training examples`() = runTest(testDispatcher) {
        // Given - Set error state first
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())
        coEvery { repository.deleteTrainExample(1) } returns Result.Error(
            message = "Error",
            exception = RuntimeException()
        )
        viewModel.deleteExample(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        assertTrue(viewModel.uiState.value is TeachAvaUiState.Error)

        // When - Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should reload examples
        coVerify(atLeast = 2) { repository.getAllExamples() }
    }

    @Test
    fun `loadTrainingExamples should set Empty state when no examples`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAllExamples() } returns flowOf(emptyList())

        // When
        viewModel.loadTrainingExamples()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is TeachAvaUiState.Empty)
    }

    @Test
    fun `loadTrainingExamples should extract unique intents`() = runTest(testDispatcher) {
        // Given
        val examples = listOf(
            TrainExample(1, "h1", "Test 1", "open_app", "en-US", TrainExampleSource.MANUAL, 1000L),
            TrainExample(2, "h2", "Test 2", "open_app", "en-US", TrainExampleSource.MANUAL, 2000L),
            TrainExample(3, "h3", "Test 3", "get_weather", "en-US", TrainExampleSource.MANUAL, 3000L)
        )
        coEvery { repository.getAllExamples() } returns flowOf(examples)

        // When
        viewModel.loadTrainingExamples()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as TeachAvaUiState.Success
        assertEquals(2, state.intents.size)
        assertTrue(state.intents.contains("get_weather"))
        assertTrue(state.intents.contains("open_app"))
    }
}
