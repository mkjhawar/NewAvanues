/**
 * ScreenChangeEvent.aidl - Parcelable declaration for screen change events
 *
 * AIDL parcelable declaration for ScreenChangeEvent data class.
 * Allows ScreenChangeEvent to be passed across process boundaries via IPC.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning;

/**
 * Screen Change Event Parcelable
 *
 * Declares ScreenChangeEvent as a parcelable type for AIDL.
 * Actual implementation is in ScreenChangeEvent.kt as @Parcelize data class.
 */
parcelable ScreenChangeEvent;
