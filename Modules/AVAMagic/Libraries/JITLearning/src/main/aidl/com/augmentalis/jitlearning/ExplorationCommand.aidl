/**
 * ExplorationCommand.aidl - Parcelable declaration for exploration commands
 *
 * AIDL parcelable declaration for ExplorationCommand data class.
 * Allows exploration commands to be passed from LearnApp to JIT service.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning;

/**
 * Exploration Command Parcelable
 *
 * Declares ExplorationCommand as a parcelable type for AIDL.
 * Actual implementation is in ExplorationCommand.kt as @Parcelize data class.
 *
 * Command types:
 * - CLICK: Click on element (requires elementUuid)
 * - LONG_CLICK: Long press on element (requires elementUuid)
 * - SCROLL: Scroll in direction (requires direction, optional distance)
 * - SWIPE: Swipe gesture (requires startX, startY, endX, endY)
 * - SET_TEXT: Enter text in field (requires elementUuid, text)
 * - BACK: Press back button
 * - HOME: Press home button
 */
parcelable ExplorationCommand;
