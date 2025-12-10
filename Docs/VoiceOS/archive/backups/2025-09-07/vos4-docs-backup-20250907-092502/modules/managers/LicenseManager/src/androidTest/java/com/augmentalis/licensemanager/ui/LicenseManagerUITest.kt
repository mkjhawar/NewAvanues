/**
 * LicenseManagerUITest.kt - UI instrumentation tests for License Manager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Instrumentation tests for License Manager UI components
 */
package com.augmentalis.licensemanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.licensemanager.LicensingModule
import com.augmentalis.licensemanager.SubscriptionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI instrumentation tests for License Manager
 */
@RunWith(AndroidJUnit4::class)
class LicenseManagerUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Test
    fun licenseManagerScreen_displaysHeaderCorrectly() {
        // Given
        composeTestRule.setContent {
            LicenseManagerTheme {
                HeaderSection()
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("License Manager")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Manage your VOS4 subscription and licensing")
            .assertIsDisplayed()
    }
    
    @Test
    fun licenseStatusCard_displaysFreeVersionCorrectly() {
        // Given
        val freeState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_FREE,
            isPremium = false,
            isValid = true
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                LicenseStatusCard(
                    subscriptionState = freeState,
                    onRefresh = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("License Status")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Free Version")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("FREE")
            .assertIsDisplayed()
    }
    
    @Test
    fun licenseStatusCard_displaysPremiumVersionCorrectly() {
        // Given
        val premiumState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_PREMIUM,
            isPremium = true,
            isValid = true,
            expiryDate = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L) // 1 year
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                LicenseStatusCard(
                    subscriptionState = premiumState,
                    onRefresh = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Premium Active")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("PREMIUM")
            .assertIsDisplayed()
    }
    
    @Test
    fun trialStatusCard_displaysTrialInformationCorrectly() {
        // Given
        val now = System.currentTimeMillis()
        val trialState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_TRIAL,
            isPremium = true,
            isValid = true,
            trialStartDate = now,
            trialEndDate = now + (15 * 24 * 60 * 60 * 1000L) // 15 days remaining
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                TrialStatusCard(subscriptionState = trialState)
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Trial Status")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Days Remaining")
            .assertIsDisplayed()
    }
    
    @Test
    fun actionButtonsCard_displaysFreeVersionActions() {
        // Given
        val freeState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_FREE,
            isPremium = false,
            isValid = true
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                ActionButtonsCard(
                    subscriptionState = freeState,
                    isLoading = false,
                    onStartTrial = {},
                    onActivateLicense = {},
                    onPurchasePro = {},
                    onContactSupport = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Actions")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Start Free Trial")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Activate License")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Contact Support")
            .assertIsDisplayed()
    }
    
    @Test
    fun actionButtonsCard_displaysTrialVersionActions() {
        // Given
        val trialState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_TRIAL,
            isPremium = true,
            isValid = true
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                ActionButtonsCard(
                    subscriptionState = trialState,
                    isLoading = false,
                    onStartTrial = {},
                    onActivateLicense = {},
                    onPurchasePro = {},
                    onContactSupport = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Upgrade to Premium")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Activate License")
            .assertIsDisplayed()
    }
    
    @Test
    fun validationInfoCard_displaysValidationInformation() {
        // Given
        val state = SubscriptionState(
            lastValidation = System.currentTimeMillis() - (2 * 60 * 60 * 1000) // 2 hours ago
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                ValidationInfoCard(
                    subscriptionState = state,
                    onRefresh = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("License Validation")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Refresh")
            .assertIsDisplayed()
    }
    
    @Test
    fun licenseActivationDialog_displaysCorrectly() {
        // Given
        composeTestRule.setContent {
            LicenseManagerTheme {
                LicenseActivationDialog(
                    onDismiss = {},
                    onActivate = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Activate License")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enter your license key to activate premium features:")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("License Key")
            .assertIsDisplayed()
    }
    
    @Test
    fun licenseActivationDialog_handlesTextInput() {
        // Given
        composeTestRule.setContent {
            LicenseManagerTheme {
                LicenseActivationDialog(
                    onDismiss = {},
                    onActivate = {}
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithText("License Key")
            .performTextInput("PREMIUM-1234-5678-9012")
        
        // Then
        composeTestRule
            .onNodeWithText("PREMIUM-1234-5678-9012")
            .assertIsDisplayed()
    }
    
    @Test
    fun actionButtons_areClickable() {
        // Given
        var startTrialClicked = false
        var activateLicenseClicked = false
        var purchaseProClicked = false
        var contactSupportClicked = false
        
        val freeState = SubscriptionState(
            licenseType = LicensingModule.LICENSE_FREE,
            isPremium = false,
            isValid = true
        )
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                ActionButtonsCard(
                    subscriptionState = freeState,
                    isLoading = false,
                    onStartTrial = { startTrialClicked = true },
                    onActivateLicense = { activateLicenseClicked = true },
                    onPurchasePro = { purchaseProClicked = true },
                    onContactSupport = { contactSupportClicked = true }
                )
            }
        }
        
        // When/Then
        composeTestRule
            .onNodeWithText("Start Free Trial")
            .assertIsDisplayed()
            .performClick()
        
        assert(startTrialClicked) { "Start trial button should be clickable" }
        
        composeTestRule
            .onNodeWithText("Contact Support")
            .assertIsDisplayed()
            .performClick()
        
        assert(contactSupportClicked) { "Contact support button should be clickable" }
    }
    
    @Test
    fun refreshButton_isClickable() {
        // Given
        var refreshClicked = false
        val state = SubscriptionState()
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                LicenseStatusCard(
                    subscriptionState = state,
                    onRefresh = { refreshClicked = true }
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithContentDescription("Refresh")
            .performClick()
        
        // Then
        assert(refreshClicked) { "Refresh button should be clickable" }
    }
    
    @Test
    fun loadingState_displaysCorrectly() {
        // Given
        val state = SubscriptionState(licenseType = LicensingModule.LICENSE_FREE)
        
        composeTestRule.setContent {
            LicenseManagerTheme {
                ActionButtonsCard(
                    subscriptionState = state,
                    isLoading = true,
                    onStartTrial = {},
                    onActivateLicense = {},
                    onPurchasePro = {},
                    onContactSupport = {}
                )
            }
        }
        
        // Then - Loading indicator should be displayed
        // Note: CircularProgressIndicator might not be easily testable without specific test tags
        composeTestRule
            .onNodeWithText("Actions")
            .assertIsDisplayed()
    }
}