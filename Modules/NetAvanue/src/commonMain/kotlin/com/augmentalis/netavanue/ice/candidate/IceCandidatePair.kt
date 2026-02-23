package com.augmentalis.netavanue.ice.candidate

/**
 * ICE candidate pair — a local candidate paired with a remote candidate.
 *
 * The ICE agent forms pairs from all local and remote candidates, prioritizes
 * them, and performs connectivity checks (STUN Binding Requests) on each pair.
 *
 * Pair priority formula (RFC 8445 Section 6.1.2.3):
 * ```
 * pairPriority = 2^32 * min(G, D) + 2 * max(G, D) + (G > D ? 1 : 0)
 * ```
 * Where G = controlling agent's candidate priority, D = controlled agent's.
 */
enum class PairState {
    /** Initial state — not yet checked */
    FROZEN,
    /** Waiting for its turn to be checked */
    WAITING,
    /** Connectivity check in progress */
    IN_PROGRESS,
    /** Check succeeded — candidate pair works */
    SUCCEEDED,
    /** Check failed — pair doesn't work */
    FAILED,
}

class IceCandidatePair(
    val local: IceCandidate,
    val remote: IceCandidate,
    val isControlling: Boolean,
) {
    var state: PairState = PairState.FROZEN
        private set
    var nominated: Boolean = false
        private set
    var lastCheckTimestamp: Long = 0
        private set

    /**
     * Calculate pair priority per RFC 8445.
     *
     * The controlling agent's candidate priority is G, controlled is D.
     * This ensures both agents compute the same priority for the same pair.
     */
    val priority: Long
        get() {
            val g: Long
            val d: Long
            if (isControlling) {
                g = local.priority; d = remote.priority
            } else {
                g = remote.priority; d = local.priority
            }
            val minGD = minOf(g, d)
            val maxGD = maxOf(g, d)
            return (1L shl 32) * minGD + 2 * maxGD + if (g > d) 1 else 0
        }

    /**
     * Unique identifier for this pair (used for deduplication).
     */
    val pairId: String
        get() = "${local.foundation}:${local.address}:${local.port}-${remote.foundation}:${remote.address}:${remote.port}"

    fun transitionTo(newState: PairState) {
        state = newState
        if (newState == PairState.IN_PROGRESS) {
            lastCheckTimestamp = currentTimeMillis()
        }
    }

    fun nominate() {
        if (state == PairState.SUCCEEDED) {
            nominated = true
        }
    }

    override fun toString(): String =
        "Pair(${local.address}:${local.port} <-> ${remote.address}:${remote.port}, state=$state, pri=$priority, nom=$nominated)"

    companion object {
        /**
         * Form all possible candidate pairs from local and remote candidate lists.
         * Returns pairs sorted by priority (highest first).
         */
        fun formPairs(
            localCandidates: List<IceCandidate>,
            remoteCandidates: List<IceCandidate>,
            isControlling: Boolean,
        ): List<IceCandidatePair> {
            val pairs = mutableListOf<IceCandidatePair>()
            for (local in localCandidates) {
                for (remote in remoteCandidates) {
                    // Only pair same component and same protocol
                    if (local.componentId == remote.componentId && local.protocol == remote.protocol) {
                        pairs.add(IceCandidatePair(local, remote, isControlling))
                    }
                }
            }
            return pairs.sortedByDescending { it.priority }
        }
    }
}

/** Platform-independent time source */
internal expect fun currentTimeMillis(): Long
