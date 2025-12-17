/**
 * IVoiceOSServiceInternal.kt - Internal interface for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Internal interface providing callback methods for tightly-coupled
 * integration components like LearnAppIntegration and JustInTimeLearner.
 *
 * Used to notify VoiceOSService when commands are generated/updated
 * so it can update the speech recognition engine.
 */
package com.augmentalis.voiceoscore.accessibility

/**
 * Internal interface for VoiceOSService callbacks.
 *
 * This is a minimal interface used by integration components to
 * notify VoiceOSService of command updates.
 */
interface IVoiceOSServiceInternal {

    /**
     * Called when new commands have been generated or updated.
     * VoiceOSService should refresh the speech recognition engine
     * with the updated command list.
     */
    fun onNewCommandsGenerated()
}
