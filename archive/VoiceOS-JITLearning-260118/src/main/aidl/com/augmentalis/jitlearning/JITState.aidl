/**
 * JITState.aidl - Parcelable declaration for JIT state
 *
 * AIDL parcelable declaration for JITState data class.
 * Allows JITState to be passed across process boundaries via IPC.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: JIT-LearnApp Separation (Phase 2)
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.jitlearning;

/**
 * JIT State Parcelable
 *
 * Declares JITState as a parcelable type for AIDL.
 * Actual implementation is in JITState.kt as @Parcelize data class.
 */
parcelable JITState;
