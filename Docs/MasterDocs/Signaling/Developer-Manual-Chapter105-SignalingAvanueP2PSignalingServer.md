# Developer Manual — Chapter 105: SignalingAvanue — P2P Signaling Server

**Module**: AvanueCentral `src/modules/signaling/` (NestJS)
**Client**: NewAvanues `Modules/NetAvanue/` (KMP — Phase 4+)
**Platforms**: Cloud (NestJS + PostgreSQL + Redis + coturn)
**Dependencies**: LicensingModule (license validation), Redis (session state), PostgreSQL (audit/history)
**Created**: 2026-02-22
**Author**: Manoj Jhawar

---

## 1. Overview

SignalingAvanue is the cloud-side signaling server that enables device-to-device P2P connections across NAT boundaries. It lives in the AvanueCentral NestJS monolith as a self-contained module and handles:

- **Device registration** — fingerprint-based identity with Ed25519 public keys
- **Session lifecycle** — create/join/leave/rejoin with invite codes (AVNE-XXXX-XXXX)
- **ICE/SDP relay** — pure message forwarding between peers for WebRTC negotiation
- **Hub election** — capability-scored leader election for star topology routing
- **TURN credentials** — HMAC-SHA1 time-limited credentials for coturn NAT relay
- **Device pairing** — persistent trust relationships with shared secrets
- **License gating** — host must have a valid license; guests join free

**Design Principle**: The signaling server is a thin relay — it never processes media. All media flows P2P (or through coturn TURN relay). The server's job is to help peers find each other, negotiate connections, and manage session metadata.

### Architecture

```
┌──────────────────────────────────────────────────────┐
│                AvanueCentral (Cloud)                   │
│                                                        │
│  ┌─────────────┐  ┌──────────────────────────────────┐│
│  │  Licensing   │  │   Signaling Module               ││
│  │  Module      │──│                                  ││
│  │  (existing)  │  │  Gateway (/signaling namespace)  ││
│  └─────────────┘  │  SessionService (Redis + PG)     ││
│                    │  DeviceService (PG + Redis caps)  ││
│                    │  CapabilityService (scoring)      ││
│                    │  TurnCredentialService (HMAC)     ││
│                    │  PairingService (PG)              ││
│                    │  PushNotificationService (Redis)  ││
│                    │  SignalingLicenseGuard             ││
│                    └──────────────────────────────────┘│
│  ┌────────────────────────────────────────────────────┐│
│  │  PostgreSQL 16          │  Redis 7                 ││
│  │  device_fingerprints    │  signaling:session:*     ││
│  │  device_pairings        │  signaling:device:*:caps ││
│  │  session_history        │  signaling:invite:*      ││
│  │  session_participants   │  turn:credential:*       ││
│  └────────────────────────────────────────────────────┘│
└────────────┬───────────────────────────┬───────────────┘
             │ WSS (/signaling)          │ UDP (3478)
             ▼                           ▼
        ┌─────────┐               ┌───────────┐
        │ Device A │←── P2P/TURN ──→│ Device B │
        │ NetAvanue│               │ NetAvanue │
        └─────────┘               └───────────┘
```

---

## 2. Licensing Model: Host-Licensed, Clients Free

Only the session **host** (creator) needs a valid Avanues license. All other participants join as free guests within the host's session limits.

| Role | License? | Capabilities |
|------|----------|-------------|
| **Host** | Required | Create sessions, manage participants, use TURN relay |
| **Guest** | Not required | Join sessions, send/receive data, participate |
| **Hub** | Inherited | Route media in star topology (can be host or any peer) |

### Tier Limits

| Tier | Price | Max Peers | TURN Relay | Session Duration |
|------|-------|-----------|------------|-----------------|
| FREE | $0 | 2 | STUN only | 40 min |
| PRO | $9.99/mo | 10 | 5 GB/mo | Unlimited |
| BUSINESS | $24.99/mo | 50 | 50 GB/mo | Unlimited |
| ENTERPRISE | Custom | Unlimited | Unlimited | Unlimited |

### License Tier Mapping

The existing `LicenseTier` in the Licensing module maps to signaling tiers:

| LicenseTier (existing) | SignalingLicenseTier |
|------------------------|-------------------|
| TRIAL, STANDARD | FREE |
| PROFESSIONAL | PRO |
| CUSTOM | BUSINESS |
| ENTERPRISE | ENTERPRISE |

License validation is cached in Redis for 5 minutes (`signaling:license:{token}:cache`) to avoid hitting PostgreSQL on every session creation.

---

## 3. Database Schema

Four new PostgreSQL tables added via migration `1740200001000-CreateSignalingTables`:

### device_fingerprints

Persistent device identity. Each device generates a SHA256 fingerprint from platform ID + install ID, plus an Ed25519 key pair for message signing.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (PK) | Auto-generated |
| user_id | UUID (FK → users) | Nullable — null for unregistered guests |
| fingerprint | VARCHAR(64) UNIQUE | SHA256(platformId + installId) |
| public_key | TEXT | Ed25519 public key (base64) |
| device_name | VARCHAR(100) | Human-readable name |
| platform | VARCHAR(20) | ANDROID, IOS, DESKTOP, WEB |
| device_type | VARCHAR(20) | PHONE, TABLET, DESKTOP, GLASSES, TV |
| last_capabilities | JSONB | Most recent capability snapshot |
| last_seen | TIMESTAMPTZ | Updated on every connection |
| is_blocked | BOOLEAN | Admin can block rogue devices |

**Indexes**: `user_id`, `platform`, `fingerprint` (unique)

### device_pairings

Trusted device pairs established via QR code or invite flow. The shared secret enables encrypted direct communication.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (PK) | |
| device_a_id | UUID (FK → device_fingerprints) | CASCADE delete |
| device_b_id | UUID (FK → device_fingerprints) | CASCADE delete |
| shared_secret | VARCHAR(64) | 256-bit random hex |
| status | VARCHAR(20) | `active` or `revoked` |
| initiated_by | UUID | Which device started pairing |

**Constraint**: UNIQUE(device_a_id, device_b_id)

### session_history

Persistent audit trail of all sessions. Used for billing (TURN bytes relayed), analytics (latency, packet loss), and debugging.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (PK) | Also used as Redis session key |
| session_type | VARCHAR(20) | CALL, CAST, FILE_TRANSFER, SCREEN_SHARE |
| host_device_id | UUID (FK) | The licensed host device |
| host_user_id | UUID (FK → users) | For billing attribution |
| license_tier | VARCHAR(20) | FREE, PRO, BUSINESS, ENTERPRISE |
| end_reason | VARCHAR(20) | HOST_LEFT, TIMEOUT, ERROR, COMPLETED |
| participant_count | INT | Current/final count |
| hub_migration_count | INT | How many times hub re-elected |
| total_bytes_relayed | BIGINT | TURN relay usage for billing |
| avg_latency_ms | FLOAT | Average measured latency |
| started_at / ended_at | TIMESTAMPTZ | Session time bounds |
| duration_seconds | INT | Computed on end |

**Indexes**: `host_user_id`, `started_at`, `host_device_id`

### session_participants

Per-participant record for each session. Tracks role, capability score, and join/leave times.

| Column | Type | Notes |
|--------|------|-------|
| session_id | UUID (FK → session_history) | CASCADE delete |
| device_id | UUID (FK → device_fingerprints) | |
| role | VARCHAR(20) | HOST, HUB, SPOKE, GUEST |
| is_licensed | BOOLEAN | True for host |
| capability_score | INT | Score at join time |
| joined_at / left_at | TIMESTAMPTZ | |

---

## 4. Redis Key Schema

All signaling state uses typed key functions from `signaling.enums.ts` → `RedisKeys.*`:

| Key Pattern | TTL | Type | Purpose |
|-------------|-----|------|---------|
| `signaling:session:{id}` | 24h (or tier limit) | STRING (JSON) | Active session state: host, hub, topology, invite code, tier limits |
| `signaling:session:{id}:participants` | Same as session | SET | Fingerprints of active participants |
| `signaling:ice:{sessionId}:{from}:{to}` | 30s | LIST | ICE candidates (consumed immediately by peer) |
| `signaling:device:{fp}:caps` | 5m | STRING (JSON) | Device capabilities + computed score |
| `turn:credential:{username}` | 1h | STRING (JSON) | TURN HMAC credentials (shared with coturn) |
| `signaling:license:{token}:cache` | 5m | STRING (JSON) | License validation result cache |
| `signaling:push:{fp}` | 30d | STRING (JSON) | FCM/APNS push notification token |
| `signaling:invite:{code}` | Same as session | STRING | Invite code → session ID lookup |
| `signaling:queue:{sessionId}:{fp}` | 5m | LIST | Queued messages for offline peer (reconnect) |

### Redis Session Data Shape

```typescript
interface RedisSessionData {
  hostDeviceId: string;     // fingerprint of licensed host
  hostUserId: string | null;
  licenseTier: string;
  maxPeers: number;
  turnAllowed: boolean;
  hubDeviceId: string;      // current hub (highest capability score)
  topology: string;         // "star" | "mesh" | "sfu"
  state: string;            // "active" | "ending" | "ended"
  sessionType: string;
  inviteCode: string;       // AVNE-XXXX-XXXX
  createdAt: string;        // ISO 8601
}
```

---

## 5. Services

### 5.1 DeviceService

**File**: `services/device.service.ts`
**Stores**: PostgreSQL (`device_fingerprints`) + Redis (capabilities cache)

| Method | Description |
|--------|-----------|
| `upsert(dto)` | Register new device or update existing by fingerprint |
| `findByFingerprint(fp)` | Look up device by SHA256 fingerprint |
| `findByUserId(id)` | All devices for a user account |
| `linkToUser(fp, userId)` | Associate device with user account |
| `updateCapabilities(fp, caps, score)` | Store in Redis (fast) + PG (persistent) |
| `getCapabilities(fp)` | Read from Redis cache |
| `blockDevice(fp)` | Admin action — blocks device from connecting |
| `isBlocked(fp)` | Check blocked status |
| `touchLastSeen(fp)` | Update timestamp on connection |

### 5.2 SessionService

**File**: `services/session.service.ts`
**Stores**: Redis (active state) + PostgreSQL (history)

| Method | Description |
|--------|-----------|
| `create(options)` | Create session in Redis + PG, generate invite code |
| `findById(id)` | Get active session from Redis |
| `findByInviteCode(code)` | Resolve invite code → session |
| `participantCount(id)` | Count via Redis SCARD |
| `addParticipant(id, fp, ...)` | Add to Redis SET + PG record |
| `removeParticipant(id, fp)` | Remove from SET, update PG leftAt |
| `getParticipantFingerprints(id)` | Get all active fingerprints |
| `queueMessage(id, fp, msg)` | Store message for offline peer |
| `drainQueue(id, fp)` | Get all queued messages on reconnect |
| `endSession(id, reason)` | Compute duration, update PG, clean Redis |

**Invite Code Format**: `AVNE-XXXX-XXXX` using 30-char unambiguous alphabet (excludes 0/O, 1/I/L). 30^8 = 656 billion combinations. Checked against Redis for uniqueness.

### 5.3 CapabilityService

**File**: `services/capability.service.ts`
**Stores**: Redis only (reads device caps, updates session hub)

**Score Calculation** (same algorithm used on client and server):

```
score  = cpuCores * 10
       + ramMb / 100
       + (isCharging ? 200 : batteryPercent * 2)
       + bandwidthMbps * 5
       + (deviceType == DESKTOP ? 100 : 0)
       + (networkType == ETHERNET ? 50 : 0)
       + screenWidth / 10
       + supportedCodecs.length * 15
```

**Hub Election**: Reads all participants' cached capabilities, finds highest scorer. If different from current hub, updates Redis session and returns `hubChanged: true`.

### 5.4 TurnCredentialService

**File**: `services/turn-credential.service.ts`
**Protocol**: HMAC-SHA1 (coturn long-term credential mechanism, RFC 5389)

```
username = "{expiresAt}:{fingerprint}"
password = HMAC-SHA1(TURN_SECRET, username)  → base64
TTL = 1 hour
```

The same `TURN_SECRET` must be configured in both AvanueCentral (env var) and coturn (`static-auth-secret`). coturn validates credentials independently using the same HMAC calculation.

**TURN URLs provided to clients**:
- `stun:{TURN_DOMAIN}:3478`
- `turn:{TURN_DOMAIN}:3478?transport=udp`
- `turn:{TURN_DOMAIN}:3478?transport=tcp`
- `turns:{TURN_DOMAIN}:5349?transport=tcp`

### 5.5 PairingService

**File**: `services/pairing.service.ts`
**Stores**: PostgreSQL (persistent pairings) + in-memory (pending requests)

Pairing flow:
1. Device A sends `PAIR_REQUEST` → server generates 256-bit shared secret
2. Pending request stored in-memory with 2-minute auto-expiry
3. Device B receives `PAIR_REQUESTED` notification
4. Device B sends `PAIR_ACCEPT` → server persists pairing to PG
5. Both devices receive `PAIR_ESTABLISHED` with pairing ID

### 5.6 PushNotificationService

**File**: `services/push-notification.service.ts`
**Stores**: Redis (push tokens, 30-day TTL)

Manages FCM/APNS push token storage for wake-on-invite functionality. Token registration/retrieval only — actual push delivery delegates to the existing NotificationsModule.

---

## 6. WebSocket Gateway

**File**: `signaling.gateway.ts`
**Namespace**: `/signaling`
**Transport**: socket.io over WSS

### Connection Lifecycle

1. Client connects with `auth: { fingerprint: "fp_abc123" }` in handshake
2. Server validates fingerprint exists and device not blocked
3. Server tracks `fingerprint → socketId` mapping for message routing
4. On disconnect, connection tracking is cleaned up

### Phase 1 Message Handlers

| Message | Handler | Description |
|---------|---------|-------------|
| `REGISTER_DEVICE` | `handleRegisterDevice` | Upserts device in PG, returns device ID |
| `REGISTER_PUSH` | `handleRegisterPush` | Stores FCM/APNS token in Redis |

### Phase 2 Message Handlers (Planned)

| Message | Direction | License Required | Description |
|---------|-----------|-----------------|-------------|
| `CREATE_SESSION` | C→S | Yes (host) | Create session with license check |
| `JOIN_SESSION` | C→S | No | Join via invite code |
| `REJOIN_SESSION` | C→S | No | Reconnect with signature |
| `LEAVE_SESSION` | C→S | No | Leave session |
| `ICE_CANDIDATE` | C→S→C | No | Relay ICE candidates |
| `SDP_OFFER` | C→S→C | No | Relay SDP offers |
| `SDP_ANSWER` | C→S→C | No | Relay SDP answers |
| `CAPABILITY_UPDATE` | C→S | No | Update caps, trigger re-election |
| `REQUEST_TURN` | C→S | No | Request TURN credentials |
| `PAIR_REQUEST` | C→S | No | Initiate device pairing |
| `PAIR_ACCEPT/REJECT` | C→S | No | Respond to pairing |

### Utility Methods

- `getSocketByFingerprint(fp)` — Resolve fingerprint to socket for targeted messaging
- `isDeviceConnected(fp)` — Check if device is online
- `getConnectedDeviceCount()` — Current connected device count

---

## 7. License Guard

**File**: `guards/license.guard.ts`
**Type**: Injectable service (NOT a NestJS route guard)

WebSocket message handlers can't use NestJS route guards directly. The `SignalingLicenseGuard` is injected into the gateway and called explicitly on the `CREATE_SESSION` path.

**Flow**:
1. Check Redis cache (`signaling:license:{token}:cache`, 5-min TTL)
2. Cache miss → call `LicensingService.validateLicense(token)`
3. Map `LicenseTier` → `SignalingLicenseTier`
4. Look up tier limits (maxPeers, turnAllowed)
5. Cache result in Redis
6. Return `LicenseValidationResult`

---

## 8. Module Registration

**File**: `signaling.module.ts`

```typescript
@HubModuleDecorator({
  moduleId: 'signaling-avanue',
  capabilities: [
    SIGNALING_SESSION,
    SIGNALING_DEVICE_REGISTRY,
    SIGNALING_ICE_RELAY,
    SIGNALING_TURN_CREDENTIAL,
    SIGNALING_DEVICE_PAIRING,
  ],
})
@Module({
  imports: [
    TypeOrmModule.forFeature([...4 entities]),
    ConfigModule,
    LicensingModule,
  ],
  providers: [...8 providers],
  exports: [...7 services + gateway + guard],
})
```

Registered in `app.module.ts` as `SignalingModule`.

---

## 9. coturn (STUN/TURN Server)

**Docker**: `docker-compose.yml` under `signaling` profile

```bash
docker compose --profile signaling up coturn
```

Uses `static-auth-secret` matching `TURN_SECRET` env var. Shares credential validation with AvanueCentral via the same HMAC algorithm — no Redis connection needed from coturn.

**Ports**:
- 3478/udp + 3478/tcp — STUN + TURN
- 5349/tcp — TURNS (TLS)
- 49152-49200/udp — Relay port range

---

## 10. File Inventory

```
packages/api/src/modules/signaling/        (18 files)
├── enums/signaling.enums.ts                # 11 enums, RedisKeys, TTLs, TierLimits
├── entities/
│   ├── device-fingerprint.entity.ts        # Device identity
│   ├── device-pairing.entity.ts            # Trusted pairs
│   ├── session-history.entity.ts           # Session audit trail
│   └── session-participant.entity.ts       # Per-participant records
├── dto/
│   ├── create-session.dto.ts               # Session creation DTO
│   ├── join-session.dto.ts                 # Join + Rejoin + Leave DTOs
│   ├── device-capability.dto.ts            # Capability data model
│   └── signaling-message.dto.ts            # All message types + interfaces
├── services/
│   ├── session.service.ts                  # Redis + PG session CRUD
│   ├── device.service.ts                   # Device registration
│   ├── capability.service.ts               # Scoring + hub election
│   ├── turn-credential.service.ts          # HMAC TURN credentials
│   ├── pairing.service.ts                  # Device pairing flow
│   └── push-notification.service.ts        # Push token storage
├── guards/license.guard.ts                 # License validation service
├── signaling.gateway.ts                    # WebSocket gateway
└── signaling.module.ts                     # NestJS module

packages/api/src/migrations/
└── 1740200001000-CreateSignalingTables.ts   # 4 tables migration
```

---

## 11. Implementation Phases

| Phase | Scope | Status |
|-------|-------|--------|
| **1** | Module scaffold, entities, migration, Redis keys, license integration | **Done** |
| **2** | CREATE/JOIN/REJOIN session handlers, ICE/SDP relay, hub election | Planned |
| **3** | coturn production setup, TURN credential flow, device pairing handlers | Planned |
| **4-7** | NetAvanue KMP client (NewAvanues repo) | Planned |
| **8** | RemoteCast integration | Planned |
| **9** | Web/JS target | Planned |
| **10** | End-to-end testing | Planned |

---

## 12. Related Documents

- NetAvanue Core Plan: `docs/plans/NetAvanue/NetAvanue-Plan-PeerNetworkingModule-260222-V1.md`
- Signaling Integration Plan: `docs/plans/NetAvanue/NetAvanue-Plan-AvanueCentralSignalingIntegration-260222-V1.md`
- Phase 1 Implementation Record: `AvanueCentral/docs/plans/Signaling-Plan-Phase1Implementation-260222-V1.md`
- RemoteCast Architecture (Chapter 103): `Docs/MasterDocs/RemoteCast/`
- HTTPAvanue v2.0 (Chapter 104): `Docs/MasterDocs/HTTPAvanue/`
