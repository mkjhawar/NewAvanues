package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import com.augmentalis.webavanue.FakeBrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * DownloadViewModelTest - Unit tests for DownloadViewModel
 *
 * Tests:
 * - Loading downloads (all and by status)
 * - Loading active downloads
 * - Adding downloads
 * - Updating download progress
 * - Updating download status
 * - Canceling downloads
 * - Retrying failed downloads
 * - Deleting downloads
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: DownloadViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = DownloadViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun `loadDownloads loads all downloads`() = runTest(testDispatcher) {
        // Given
        val download1 = Download(
            id = "1",
            url = "https://example.com/file1.pdf",
            filename = "file1.pdf",
            filePath = "/downloads/file1.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 0,
            totalBytes = 1000,
            status = DownloadStatus.PENDING,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        val download2 = Download(
            id = "2",
            url = "https://example.com/file2.zip",
            filename = "file2.zip",
            filePath = "/downloads/file2.zip",
            mimeType = "application/zip",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download1, download2))

        // When
        viewModel.loadDownloads()
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(2, downloads.size)
        assertEquals("file1.pdf", downloads[0].filename)
        assertEquals("file2.zip", downloads[1].filename)
    }

    @Test
    fun `loadDownloads loads only active downloads`() = runTest(testDispatcher) {
        // Given
        val download1 = Download(
            id = "1",
            url = "https://example.com/active.pdf",
            filename = "active.pdf",
            filePath = "/downloads/active.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        val download2 = Download(
            id = "2",
            url = "https://example.com/completed.zip",
            filename = "completed.zip",
            filePath = "/downloads/completed.zip",
            mimeType = "application/zip",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.COMPLETED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download1, download2))
        repository.setActiveDownloads(listOf(download1))

        // When
        viewModel.loadDownloads()
        advanceUntilIdle()

        // Then
        val activeDownloads = viewModel.activeDownloads.first()
        assertEquals(1, activeDownloads.size)
        assertEquals("active.pdf", activeDownloads[0].filename)
        assertEquals(DownloadStatus.IN_PROGRESS, activeDownloads[0].status)
    }

    @Test
    fun `loadDownloads filters by status`() = runTest(testDispatcher) {
        // Given
        val download1 = Download(
            id = "1",
            url = "https://example.com/completed.pdf",
            filename = "completed.pdf",
            filePath = "/downloads/completed.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.COMPLETED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        val download2 = Download(
            id = "2",
            url = "https://example.com/failed.zip",
            filename = "failed.zip",
            filePath = "/downloads/failed.zip",
            mimeType = "application/zip",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.FAILED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download1, download2))

        // When
        viewModel.loadDownloads(DownloadStatus.COMPLETED)
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(1, downloads.size)
        assertEquals("completed.pdf", downloads[0].filename)
        assertEquals(DownloadStatus.COMPLETED, downloads[0].status)
    }

    @Test
    fun `addDownload creates new download`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/newfile.pdf",
            filename = "newfile.pdf",
            filePath = "/downloads/newfile.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 0,
            totalBytes = 1000,
            status = DownloadStatus.PENDING,
            createdAt = kotlinx.datetime.Clock.System.now()
        )

        // When
        viewModel.addDownload(download)
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(1, downloads.size)
        assertEquals("newfile.pdf", downloads[0].filename)
    }

    @Test
    fun `updateProgress updates download bytes`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            filePath = "/downloads/file.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 0,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.updateProgress("1", downloadedBytes = 500, totalBytes = 1000)
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(500, downloads[0].downloadedBytes)
        assertEquals(1000, downloads[0].totalBytes)
    }

    @Test
    fun `updateProgress changes download status`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            filePath = "/downloads/file.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.updateProgress("1", DownloadStatus.COMPLETED)
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(DownloadStatus.COMPLETED, downloads[0].status)
    }

    @Test
    fun `cancelDownload updates status to CANCELLED`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            filePath = "/downloads/file.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.cancelDownload("1")
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(DownloadStatus.CANCELLED, downloads[0].status)
    }

    @Test
    fun `retryDownload resets failed download to PENDING`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            filePath = "/downloads/file.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.FAILED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.retryDownload("1")
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(DownloadStatus.PENDING, downloads[0].status)
        assertEquals(0, downloads[0].downloadedBytes)  // Reset progress
    }

    @Test
    fun `deleteDownload removes download`() = runTest(testDispatcher) {
        // Given
        val download = Download(
            id = "1",
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            filePath = "/downloads/file.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.COMPLETED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.deleteDownload("1")
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(0, downloads.size)
    }

    @Test
    fun `clearCompletedDownloads removes all downloads`() = runTest(testDispatcher) {
        // Given
        val download1 = Download(
            id = "1",
            url = "https://example.com/file1.pdf",
            filename = "file1.pdf",
            filePath = "/downloads/file1.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.COMPLETED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        val download2 = Download(
            id = "2",
            url = "https://example.com/file2.zip",
            filename = "file2.zip",
            filePath = "/downloads/file2.zip",
            mimeType = "application/zip",
            downloadedBytes = 1000,
            totalBytes = 1000,
            status = DownloadStatus.COMPLETED,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download1, download2))
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.clearCompletedDownloads()
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertEquals(0, downloads.size)
    }

    @Test
    fun `clearCompletedDownloads cancels all active downloads`() = runTest(testDispatcher) {
        // Given
        val download1 = Download(
            id = "1",
            url = "https://example.com/file1.pdf",
            filename = "file1.pdf",
            filePath = "/downloads/file1.pdf",
            mimeType = "application/pdf",
            downloadedBytes = 500,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        val download2 = Download(
            id = "2",
            url = "https://example.com/file2.zip",
            filename = "file2.zip",
            filePath = "/downloads/file2.zip",
            mimeType = "application/zip",
            downloadedBytes = 300,
            totalBytes = 1000,
            status = DownloadStatus.IN_PROGRESS,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setDownloads(listOf(download1, download2))
        repository.setActiveDownloads(listOf(download1, download2))
        viewModel.loadDownloads()
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.clearCompletedDownloads()
        advanceUntilIdle()

        // Then
        val downloads = viewModel.downloads.first()
        assertTrue(downloads.all { it.status == DownloadStatus.CANCELLED })
    }

    @Test
    fun `error state is set when repository operation fails`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)

        // When
        viewModel.loadDownloads()
        advanceUntilIdle()

        // Then
        val error = viewModel.errorMessage.first()
        assertNotNull(error)
        assertTrue(error.contains("Failed"))
    }

    @Test
    fun `clearError clears error state`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)
        viewModel.loadDownloads()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val error = viewModel.errorMessage.first()
        assertNull(error)
    }
}
