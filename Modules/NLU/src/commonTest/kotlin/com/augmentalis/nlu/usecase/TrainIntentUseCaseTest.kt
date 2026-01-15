package com.augmentalis.nlu.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * DEFEND Phase: TrainIntentUseCase tests
 * Validates Teach-Ava training logic
 */
class TrainIntentUseCaseTest {

    private lateinit var trainExampleRepository: TrainExampleRepository
    private lateinit var useCase: TrainIntentUseCase

    @Before
    fun setup() {
        trainExampleRepository = mockk()
        useCase = TrainIntentUseCase(trainExampleRepository)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `invoke should add training example when no duplicate`() = runTest {
        // Given
        val utterance = "Open settings"
        val intent = "open_app"

        coEvery { trainExampleRepository.findDuplicate(any()) } returns Result.Success(null)
        coEvery { trainExampleRepository.addTrainExample(any()) } returns Result.Success(
            TrainExample(
                id = 1,
                exampleHash = "hash123",
                utterance = utterance,
                intent = intent,
                locale = "en-US",
                source = TrainExampleSource.MANUAL,
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = useCase(utterance, intent)

        // Then
        assertTrue(result.isSuccess)
        val example = (result as Result.Success).data
        assertEquals(utterance, example.utterance)
        assertEquals(intent, example.intent)
        coVerify { trainExampleRepository.findDuplicate(any()) }
        coVerify { trainExampleRepository.addTrainExample(any()) }
    }

    @Test
    fun `invoke should return error when duplicate exists`() = runTest {
        // Given
        val utterance = "Open settings"
        val intent = "open_app"

        val existingExample = TrainExample(
            id = 10,
            exampleHash = "hash123",
            utterance = utterance,
            intent = intent,
            locale = "en-US",
            source = TrainExampleSource.MANUAL,
            createdAt = 1000L
        )
        coEvery { trainExampleRepository.findDuplicate(any()) } returns Result.Success(existingExample)

        // When
        val result = useCase(utterance, intent)

        // Then
        assertTrue(result.isError)
        assertEquals("This training example already exists", (result as Result.Error).message)
        coVerify { trainExampleRepository.findDuplicate(any()) }
        coVerify(exactly = 0) { trainExampleRepository.addTrainExample(any()) }
    }

    @Test
    fun `invoke should return error for empty utterance`() = runTest {
        // When
        val result = useCase("", "open_app")

        // Then
        assertTrue(result.isError)
        assertEquals("Utterance cannot be empty", (result as Result.Error).message)
        coVerify(exactly = 0) { trainExampleRepository.findDuplicate(any()) }
    }

    @Test
    fun `invoke should return error for empty intent`() = runTest {
        // When
        val result = useCase("Open settings", "")

        // Then
        assertTrue(result.isError)
        assertEquals("Intent cannot be empty", (result as Result.Error).message)
        coVerify(exactly = 0) { trainExampleRepository.findDuplicate(any()) }
    }

    @Test
    fun `invoke should handle repository errors`() = runTest {
        // Given
        coEvery { trainExampleRepository.findDuplicate(any()) } returns Result.Error(
            exception = RuntimeException("DB error"),
            message = "Database failure"
        )

        // When
        val result = useCase("Open settings", "open_app")

        // Then
        assertTrue(result.isError)
        assertEquals("Failed to check for duplicates", (result as Result.Error).message)
    }

    @Test
    fun `invoke should use correct locale`() = runTest {
        // Given
        coEvery { trainExampleRepository.findDuplicate(any()) } returns Result.Success(null)
        coEvery { trainExampleRepository.addTrainExample(any()) } returns Result.Success(
            TrainExample(
                id = 1,
                exampleHash = "hash123",
                utterance = "Abrir configuración",
                intent = "open_app",
                locale = "es-ES",
                source = TrainExampleSource.MANUAL,
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = useCase(
            utterance = "Abrir configuración",
            intent = "open_app",
            locale = "es-ES"
        )

        // Then
        assertTrue(result.isSuccess)
        val example = (result as Result.Success).data
        assertEquals("es-ES", example.locale)
    }

    @Test
    fun `invoke should use correct source type`() = runTest {
        // Given
        coEvery { trainExampleRepository.findDuplicate(any()) } returns Result.Success(null)
        coEvery { trainExampleRepository.addTrainExample(any()) } returns Result.Success(
            TrainExample(
                id = 1,
                exampleHash = "hash123",
                utterance = "Test",
                intent = "test",
                locale = "en-US",
                source = TrainExampleSource.CORRECTION,
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = useCase(
            utterance = "Test",
            intent = "test",
            source = TrainExampleSource.CORRECTION
        )

        // Then
        assertTrue(result.isSuccess)
        val example = (result as Result.Success).data
        assertEquals(TrainExampleSource.CORRECTION, example.source)
    }

    @Test
    fun `hash generation should be deterministic`() = runTest {
        // Given
        val utterance = "Open settings"
        val intent = "open_app"

        val hashSlot1 = slot<String>()
        val hashSlot2 = slot<String>()

        coEvery { trainExampleRepository.findDuplicate(capture(hashSlot1)) } returns Result.Success(null)
        coEvery { trainExampleRepository.addTrainExample(any()) } returns Result.Success(
            TrainExample(
                id = 1,
                exampleHash = "hash",
                utterance = utterance,
                intent = intent,
                locale = "en-US",
                source = TrainExampleSource.MANUAL,
                createdAt = System.currentTimeMillis()
            )
        )

        // When - Call twice with same inputs
        useCase(utterance, intent)

        clearMocks(trainExampleRepository, answers = false)
        coEvery { trainExampleRepository.findDuplicate(capture(hashSlot2)) } returns Result.Success(null)
        coEvery { trainExampleRepository.addTrainExample(any()) } returns Result.Success(
            TrainExample(
                id = 2,
                exampleHash = "hash",
                utterance = utterance,
                intent = intent,
                locale = "en-US",
                source = TrainExampleSource.MANUAL,
                createdAt = System.currentTimeMillis()
            )
        )

        useCase(utterance, intent)

        // Then - Hashes should be identical
        assertNotNull(hashSlot1.captured)
        assertNotNull(hashSlot2.captured)
        assertEquals(hashSlot1.captured, hashSlot2.captured)
    }
}
