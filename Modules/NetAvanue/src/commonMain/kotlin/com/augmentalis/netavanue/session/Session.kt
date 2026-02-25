package com.augmentalis.netavanue.session

import com.augmentalis.netavanue.signaling.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents an active signaling session.
 *
 * Tracks session metadata, participant list, hub device, own role,
 * and TURN credentials. Updated reactively as server events arrive.
 */
class Session(
    val sessionId: String,
    val inviteCode: String,
    val sessionType: SessionType,
    initialRole: ParticipantRole,
    initialHubFingerprint: String,
    turnCredentials: TurnCredential? = null,
) {
    private val _participants = MutableStateFlow<List<ParticipantInfo>>(emptyList())
    val participants: StateFlow<List<ParticipantInfo>> = _participants.asStateFlow()

    private val _hubFingerprint = MutableStateFlow(initialHubFingerprint)
    val hubFingerprint: StateFlow<String> = _hubFingerprint.asStateFlow()

    private val _myRole = MutableStateFlow(initialRole)
    val myRole: StateFlow<ParticipantRole> = _myRole.asStateFlow()

    private val _turnCredentials = MutableStateFlow(turnCredentials)
    val turnCredentials: StateFlow<TurnCredential?> = _turnCredentials.asStateFlow()

    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun updateParticipants(list: List<ParticipantInfo>) {
        _participants.value = list
    }

    fun addParticipant(participant: ParticipantInfo) {
        _participants.value = _participants.value + participant
    }

    fun removeParticipant(fingerprint: String) {
        _participants.value = _participants.value.filter { it.fingerprint != fingerprint }
    }

    fun updateHub(hubFingerprint: String) {
        _hubFingerprint.value = hubFingerprint
    }

    fun updateRole(role: ParticipantRole) {
        _myRole.value = role
    }

    fun updateTurnCredentials(creds: TurnCredential) {
        _turnCredentials.value = creds
    }

    fun end() {
        _isActive.value = false
    }

    val participantCount: Int get() = _participants.value.size
    val isHub: Boolean get() = _myRole.value == ParticipantRole.HUB
    val isHost: Boolean get() = _myRole.value == ParticipantRole.HOST
}
