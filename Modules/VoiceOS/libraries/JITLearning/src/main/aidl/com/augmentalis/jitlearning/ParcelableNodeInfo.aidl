/**
 * ParcelableNodeInfo.aidl - Parcelable declaration for node information
 *
 * AIDL parcelable declaration for ParcelableNodeInfo data class.
 * Allows accessibility node information to be passed across process boundaries.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning;

/**
 * Parcelable Node Info
 *
 * Declares ParcelableNodeInfo as a parcelable type for AIDL.
 * Actual implementation is in ParcelableNodeInfo.kt as @Parcelize data class.
 *
 * Contains:
 * - className: Android class name
 * - text: Visible text
 * - contentDescription: Accessibility description
 * - resourceId: View resource ID
 * - bounds: Screen coordinates (left, top, right, bottom)
 * - isClickable, isEnabled, etc.
 * - children: Child nodes
 */
parcelable ParcelableNodeInfo;
