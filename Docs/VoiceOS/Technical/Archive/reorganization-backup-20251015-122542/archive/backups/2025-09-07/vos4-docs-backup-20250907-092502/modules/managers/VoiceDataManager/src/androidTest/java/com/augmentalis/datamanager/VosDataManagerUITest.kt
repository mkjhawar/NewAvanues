/**
 * VosDataManagerUITest.kt - UI instrumentation tests for VosDataManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Tests UI components, interactions, and visual elements
 */
package com.augmentalis.datamanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.datamanager.data.*
import com.augmentalis.datamanager.entities.CommandHistoryEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class VosDataManagerUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testVosDataManagerActivityLaunch() {
        composeTestRule.setContent {
            VosDataManagerContent(
                viewModel = VosDataViewModel(context)
            )
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("VOS Data Manager").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage and monitor your data").assertIsDisplayed()
    }

    @Test
    fun testStorageOverviewCardDisplay() {
        composeTestRule.setContent {
            StorageOverviewCard(
                storageInfo = StorageInfo(
                    databaseSize = 1024 * 1024 * 50, // 50MB
                    availableSpace = 1024 * 1024 * 1024, // 1GB
                    storageLevel = StorageLevel.NORMAL,
                    percentUsed = 25f
                ),
                onRefresh = { }
            )
        }

        // Verify storage card displays
        composeTestRule.onNodeWithText("Storage Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("25.0% used").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
    }

    @Test
    fun testDataStatisticsCardDisplay() {
        composeTestRule.setContent {
            DataStatisticsCard(
                statistics = DataStatistics(
                    totalRecords = 1500,
                    storageUsed = 1024 * 1024 * 25, // 25MB
                    lastSync = System.currentTimeMillis(),
                    dataBreakdown = mapOf(
                        "History" to 500,
                        "Preferences" to 100,
                        "Commands" to 200
                    ),
                    retentionDays = 30,
                    autoCleanupEnabled = true
                ),
                onRefresh = { }
            )
        }

        // Verify statistics are displayed
        composeTestRule.onNodeWithText("Data Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("1500").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Records").assertIsDisplayed()
    }

    @Test
    fun testQuickActionsCardInteraction() {
        var exportClicked = false
        var importClicked = false
        var cleanupClicked = false
        var clearClicked = false

        composeTestRule.setContent {
            QuickActionsCard(
                onExport = { exportClicked = true },
                onImport = { importClicked = true },
                onCleanup = { cleanupClicked = true },
                onClear = { clearClicked = true },
                isLoading = false
            )
        }

        // Verify all buttons are displayed and clickable
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Export").assertIsDisplayed().performClick()
        assertTrue(exportClicked)
        
        composeTestRule.onNodeWithText("Import").assertIsDisplayed().performClick()
        assertTrue(importClicked)
        
        composeTestRule.onNodeWithText("Cleanup").assertIsDisplayed().performClick()
        assertTrue(cleanupClicked)
        
        composeTestRule.onNodeWithText("Clear All").assertIsDisplayed().performClick()
        assertTrue(clearClicked)
    }

    @Test
    fun testDataBreakdownCardDisplay() {
        composeTestRule.setContent {
            DataBreakdownCard(
                breakdown = mapOf(
                    "History" to 500,
                    "Preferences" to 100,
                    "Commands" to 200,
                    "Gestures" to 50,
                    "Statistics" to 300,
                    "Profiles" to 25
                ),
                onCategoryClick = { }
            )
        }

        // Verify breakdown is displayed
        composeTestRule.onNodeWithText("Data Breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("500").assertIsDisplayed() // History count
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
        composeTestRule.onNodeWithText("Commands").assertIsDisplayed()
    }

    @Test
    fun testRecentHistoryCardDisplay() {
        val sampleHistory = listOf(
            CommandHistoryEntry(
                id = 1,
                processedCommand = "go back",
                success = true,
                timestamp = System.currentTimeMillis() - 60000
            ),
            CommandHistoryEntry(
                id = 2,
                processedCommand = "volume up",
                success = false,
                timestamp = System.currentTimeMillis() - 120000
            )
        )

        composeTestRule.setContent {
            RecentHistoryCard(
                history = sampleHistory,
                onViewAll = { }
            )
        }

        // Verify history is displayed
        composeTestRule.onNodeWithText("Recent Command History").assertIsDisplayed()
        composeTestRule.onNodeWithText("go back").assertIsDisplayed()
        composeTestRule.onNodeWithText("volume up").assertIsDisplayed()
        composeTestRule.onNodeWithText("View All").assertIsDisplayed()
    }

    @Test
    fun testRetentionSettingsCardInteraction() {
        var settingsClicked = false

        composeTestRule.setContent {
            RetentionSettingsCard(
                retentionDays = 30,
                autoCleanupEnabled = true,
                onSettingsClick = { settingsClicked = true }
            )
        }

        // Verify retention settings display
        composeTestRule.onNodeWithText("Data Retention").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep data for 30 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto-cleanup enabled").assertIsDisplayed()
        
        // Test click
        composeTestRule.onNode(hasTestTag("retention_card")).performClick()
        assertTrue(settingsClicked)
    }

    @Test
    fun testExportDataDialogDisplay() {
        composeTestRule.setContent {
            ExportDataDialog(
                onDismiss = { },
                onExport = { }
            )
        }

        // Verify export dialog displays
        composeTestRule.onNodeWithText("Export Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select data types to export:").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export").assertIsDisplayed()
    }

    @Test
    fun testImportDataDialogDisplay() {
        composeTestRule.setContent {
            ImportDataDialog(
                onDismiss = { },
                onImport = { }
            )
        }

        // Verify import dialog displays
        composeTestRule.onNodeWithText("Import Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter file path or select file:").assertIsDisplayed()
        composeTestRule.onNodeWithText("File path").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import").assertIsDisplayed()
    }

    @Test
    fun testCleanupDialogDisplay() {
        composeTestRule.setContent {
            CleanupDialog(
                onDismiss = { },
                onCleanup = { }
            )
        }

        // Verify cleanup dialog displays
        composeTestRule.onNodeWithText("Cleanup Old Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove data older than:").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cleanup").assertIsDisplayed()
    }

    @Test
    fun testClearDataDialogDisplay() {
        composeTestRule.setContent {
            ClearDataDialog(
                onDismiss = { },
                onClear = { }
            )
        }

        // Verify clear dialog displays with warning
        composeTestRule.onNodeWithText("Clear All Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("This will permanently delete all stored data. This action cannot be undone.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
    }

    @Test
    fun testOperationProgressOverlay() {
        composeTestRule.setContent {
            OperationProgressOverlay(
                message = "Exporting data...",
                progress = 0.75f
            )
        }

        // Verify progress overlay displays
        composeTestRule.onNodeWithText("Exporting data...").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }
}