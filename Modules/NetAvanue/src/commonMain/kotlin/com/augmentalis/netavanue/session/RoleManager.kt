package com.augmentalis.netavanue.session

import com.augmentalis.netavanue.capability.CapabilityScorer
import com.augmentalis.netavanue.signaling.ParticipantInfo
import com.augmentalis.netavanue.signaling.ParticipantRole

/**
 * Tracks and computes role assignments within a session.
 *
 * Roles:
 * - HOST: the session creator (has the license, cannot change)
 * - HUB: the device routing media/data (highest capability score)
 * - SPOKE: a connected peer that is not the hub
 * - GUEST: a peer who joined without a license
 *
 * The hub role is determined by capability score. The server performs the
 * authoritative election, but clients can predict locally for UI purposes.
 */
object RoleManager {

    /**
     * Predict which peer should be hub based on capability scores.
     * Returns the fingerprint of the highest-scoring participant.
     */
    fun predictHub(participants: List<ParticipantInfo>): String? {
        return participants.maxByOrNull { it.capabilityScore }?.fingerprint
    }

    /**
     * Determine the appropriate role for a local device given the current session state.
     */
    fun determineLocalRole(
        myFingerprint: String,
        isHost: Boolean,
        hubFingerprint: String,
    ): ParticipantRole {
        return when {
            myFingerprint == hubFingerprint && isHost -> ParticipantRole.HOST
            myFingerprint == hubFingerprint -> ParticipantRole.HUB
            isHost -> ParticipantRole.HOST
            else -> ParticipantRole.SPOKE
        }
    }

    /**
     * Check if a hub re-election is likely based on a capability change.
     * Returns true if the new score would overtake the current hub's score.
     */
    fun wouldTriggerReElection(
        newScore: Int,
        currentHubScore: Int,
        isCurrentHub: Boolean,
    ): Boolean {
        if (isCurrentHub) return false
        return newScore > currentHubScore
    }
}
