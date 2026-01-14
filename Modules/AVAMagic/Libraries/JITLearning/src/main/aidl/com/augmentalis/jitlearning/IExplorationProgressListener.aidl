/**
 * IExplorationProgressListener.aidl - Exploration progress callback
 *
 * Callback interface for receiving exploration progress updates.
 *
 * @since 2.1.0 (P2 - Exploration Sync)
 */

package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.ExplorationProgress;

/**
 * Exploration Progress Listener
 *
 * Receives exploration progress updates from VoiceOS.
 */
interface IExplorationProgressListener {

    /**
     * Called when exploration progress changes
     *
     * @param progress Current exploration progress
     */
    void onProgressUpdate(in ExplorationProgress progress);

    /**
     * Called when exploration completes
     *
     * @param progress Final exploration progress with state="completed"
     */
    void onCompleted(in ExplorationProgress progress);

    /**
     * Called when exploration fails
     *
     * @param progress Progress at time of failure with state="failed"
     * @param errorMessage Description of the error
     */
    void onFailed(in ExplorationProgress progress, String errorMessage);
}
