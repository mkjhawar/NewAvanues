# NetAvanue — AvanueCentral Signaling Integration + STUN/TURN Infrastructure

**Module**: NetAvanue (KMP Client) + AvanueCentral (Cloud Server)
**Type**: Plan (Detailed Implementation)
**Date**: 2026-02-22
**Version**: V1
**Status**: Ready for Implementation
**Author**: Manoj Jhawar
**Depends On**: HTTPAvanue v2.0 (complete), AvanueCentral (existing)

---

## 1. Overview

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    AvanueCentral (Cloud)                      │
│                                                               │
│  ┌────────────┐  ┌──────────┐  ┌──────────────────────────┐ │
│  │  Licensing  │  │   User   │  │   Signaling Module       │ │
│  │  Module     │  │   Mgmt   │  │   (NEW NestJS module)    │ │
│  │  (existing) │  │(existing)│  │                          │ │
│  └──────┬──────┘  └────┬─────┘  │  ┌─────────────────────┐│ │
│         │               │        │  │ WebSocket Gateway   ││ │
│         │               │        │  │ Session Manager     ││ │
│         │               │        │  │ Device Registry     ││ │
│         │               │        │  │ Capability Scorer   ││ │
│         │               │        │  │ Role Election       ││ │
│         │               │        │  │ TURN Credential Mgr ││ │
│         │               │        │  └─────────────────────┘│ │
│         │               │        └──────────────────────────┘ │
│  ┌──────┴───────────────┴────────────────────────────────┐   │
│  │                   PostgreSQL 16                        │   │
│  │  users | licenses | subscriptions (existing)           │   │
│  │  device_fingerprints | device_pairings (NEW)           │   │
│  │  session_history | session_participants (NEW)          │   │
│  └────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────┐   │
│  │                     Redis 7                            │   │
│  │  active sessions | ICE candidates | TURN creds (NEW)   │   │
│  │  device capabilities | push tokens (NEW)               │   │
│  └────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
         │ WSS (443)                    │ UDP (3478)
         ▼                              ▼
    ┌───────────┐                 ┌───────────┐
    │ Signaling │                 │ STUN/TURN │
    │ Gateway   │                 │ Server    │
    │ (WS/WSS) │                 │ (coturn)  │
    └─────┬─────┘                 └─────┬─────┘
          │                              │
    ┌─────┴──────────────────────────────┴─────┐
    │            Internet / NAT                 │
    └─────┬──────────────────────────────┬─────┘
          │                              │
    ┌─────┴─────┐                 ┌──────┴────┐
    │ Device A  │ ←── P2P/Relay ──→│ Device B  │
    │ (Licensed │                  │ (Client/  │
    │  Host)    │                  │  Guest)   │
    │ NetAvanue │                  │ NetAvanue │
    └───────────┘                  └───────────┘
```

### Licensing Model: Host-Licensed, Clients Free

**Principle**: At least ONE participant (the session host) must have a valid Avanues license. All other participants can join as unlicensed guests — they only need the app installed.

| Role | License Required? | What They Can Do |
|------|-------------------|------------------|
| **Host** (session creator) | YES — valid license required | Create sessions, elect hub, manage participants, use TURN relay |
| **Client/Guest** (joins session) | NO — free, no license | Join sessions, send/receive data, participate in calls |
| **Hub** (elected by capability) | Depends — can be host or any peer | Route media, manage topology. If hub is unlicensed guest, relay costs go to host's license |

**Why this works**: The licensed host's subscription covers the infrastructure costs (TURN relay bandwidth, session persistence, push notifications). Guests use the infrastructure for free within the host's session limits. This is the Zoom/Teams model — meeting host pays, attendees don't.

---

## 2. Licensing Tiers

| Tier | Price | Max Peers | TURN Relay | Session Duration | Features |
|------|-------|-----------|------------|------------------|----------|
| **Free** | $0 | 2 (host + 1 guest) | No (STUN only, direct P2P) | 40 min limit | Basic casting, 1:1 calls |
| **Pro** | $9.99/mo | 10 | Yes (5 GB/mo) | Unlimited | Group calls, screen share, session history |
| **Business** | $24.99/mo | 50 | Yes (50 GB/mo) | Unlimited | Priority TURN, recording, analytics dashboard |
| **Enterprise** | Custom | Unlimited | Yes (unlimited) | Unlimited | Dedicated TURN, SLA, custom branding, SSO |

### License Validation Flow

```
Host creates session:
  1. Host → Signaling: CREATE_SESSION { licenseToken, fingerprint }
  2. Signaling → Licensing Module: validateLicense(licenseToken)
     ├── response: { valid: true, tier: "pro", maxPeers: 10, turnAllowed: true }
     └── cached in Redis for 5 min (avoid re-checking every message)
  3. Signaling → Host: SESSION_CREATED { sessionId, inviteCode, turnCredentials? }

Guest joins:
  1. Guest → Signaling: JOIN_SESSION { inviteCode, fingerprint }
  2. Signaling: Check session exists, count participants vs host's tier limit
     ├── Under limit → ALLOW
     ├── At limit → REJECT (session full)
     └── Session expired → REJECT (session ended)
  3. Signaling → Guest: SESSION_JOINED { sessionId, participants, hubId }
     No license check for guest — just validate the session.

License check is ONLY on session creation, NOT on every message.
```

---

## 3. Database Schema (AvanueCentral PostgreSQL)

### New Tables (TypeORM Entities)

```typescript
// src/modules/signaling/entities/device-fingerprint.entity.ts

@Entity('device_fingerprints')
export class DeviceFingerprint {
    @PrimaryGeneratedColumn('uuid')
    id: string;

    @Column({ type: 'uuid', nullable: true })
    @ManyToOne(() => User, { nullable: true })
    userId: string | null;                    // null for unregistered guests

    @Column({ type: 'varchar', length: 64, unique: true })
    fingerprint: string;                      // SHA256(platformId + installId)

    @Column({ type: 'text' })
    publicKey: string;                        // Ed25519 public key (base64)

    @Column({ type: 'varchar', length: 100 })
    deviceName: string;                       // "Manoj's Pixel 9"

    @Column({ type: 'varchar', length: 20 })
    platform: string;                         // ANDROID | IOS | DESKTOP | WEB

    @Column({ type: 'varchar', length: 20 })
    deviceType: string;                       // PHONE | TABLET | DESKTOP | GLASSES | TV

    @Column({ type: 'jsonb', nullable: true })
    lastCapabilities: object | null;          // Most recent capability snapshot

    @Column({ type: 'timestamptz', default: () => 'NOW()' })
    lastSeen: Date;

    @Column({ type: 'timestamptz', default: () => 'NOW()' })
    createdAt: Date;

    @Column({ type: 'boolean', default: false })
    isBlocked: boolean;                       // Admin can block rogue devices
}
```

```typescript
// src/modules/signaling/entities/device-pairing.entity.ts

@Entity('device_pairings')
@Unique(['deviceA', 'deviceB'])
export class DevicePairing {
    @PrimaryGeneratedColumn('uuid')
    id: string;

    @Column({ type: 'uuid' })
    @ManyToOne(() => DeviceFingerprint)
    deviceAId: string;

    @Column({ type: 'uuid' })
    @ManyToOne(() => DeviceFingerprint)
    deviceBId: string;

    @Column({ type: 'varchar', length: 64 })
    sharedSecret: string;                     // Derived via ECDH or pre-shared

    @Column({ type: 'varchar', length: 20, default: 'active' })
    status: string;                           // active | revoked

    @Column({ type: 'uuid', nullable: true })
    initiatedBy: string;                      // Which device initiated pairing

    @Column({ type: 'timestamptz', default: () => 'NOW()' })
    createdAt: Date;
}
```

```typescript
// src/modules/signaling/entities/session-history.entity.ts

@Entity('session_history')
export class SessionHistory {
    @PrimaryGeneratedColumn('uuid')
    id: string;

    @Column({ type: 'varchar', length: 20 })
    sessionType: string;                      // CALL | CAST | FILE_TRANSFER | SCREEN_SHARE

    @Column({ type: 'uuid' })
    @ManyToOne(() => DeviceFingerprint)
    hostDeviceId: string;                     // The licensed host

    @Column({ type: 'uuid', nullable: true })
    hostUserId: string;                       // Links to existing user table

    @Column({ type: 'varchar', length: 20 })
    licenseTier: string;                      // FREE | PRO | BUSINESS | ENTERPRISE

    @Column({ type: 'varchar', length: 20 })
    endReason: string;                        // HOST_LEFT | TIMEOUT | ERROR | COMPLETED

    @Column({ type: 'int', default: 0 })
    participantCount: number;

    @Column({ type: 'int', default: 0 })
    hubMigrationCount: number;                // How many times hub re-elected

    @Column({ type: 'bigint', default: 0 })
    totalBytesRelayed: number;                // TURN relay usage (for billing)

    @Column({ type: 'float', nullable: true })
    avgLatencyMs: number;

    @Column({ type: 'float', nullable: true })
    avgPacketLossPercent: number;

    @Column({ type: 'timestamptz', default: () => 'NOW()' })
    startedAt: Date;

    @Column({ type: 'timestamptz', nullable: true })
    endedAt: Date;

    @Column({ type: 'int', nullable: true })
    durationSeconds: number;
}
```

```typescript
// src/modules/signaling/entities/session-participant.entity.ts

@Entity('session_participants')
export class SessionParticipant {
    @PrimaryGeneratedColumn('uuid')
    id: string;

    @Column({ type: 'uuid' })
    @ManyToOne(() => SessionHistory)
    sessionId: string;

    @Column({ type: 'uuid' })
    @ManyToOne(() => DeviceFingerprint)
    deviceId: string;

    @Column({ type: 'varchar', length: 20 })
    role: string;                             // HOST | HUB | SPOKE | GUEST

    @Column({ type: 'boolean', default: false })
    isLicensed: boolean;                      // true for the host, false for guests

    @Column({ type: 'int', nullable: true })
    capabilityScore: number;

    @Column({ type: 'timestamptz', default: () => 'NOW()' })
    joinedAt: Date;

    @Column({ type: 'timestamptz', nullable: true })
    leftAt: Date;
}
```

### Migration

```typescript
// src/modules/signaling/migrations/XXXXXX-CreateSignalingTables.ts

export class CreateSignalingTables implements MigrationInterface {
    async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`
            CREATE TABLE device_fingerprints (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                fingerprint VARCHAR(64) UNIQUE NOT NULL,
                public_key TEXT NOT NULL,
                device_name VARCHAR(100) NOT NULL,
                platform VARCHAR(20) NOT NULL,
                device_type VARCHAR(20) NOT NULL DEFAULT 'PHONE',
                last_capabilities JSONB,
                last_seen TIMESTAMPTZ DEFAULT NOW(),
                created_at TIMESTAMPTZ DEFAULT NOW(),
                is_blocked BOOLEAN DEFAULT FALSE
            );

            CREATE INDEX idx_device_fingerprints_user ON device_fingerprints(user_id);
            CREATE INDEX idx_device_fingerprints_platform ON device_fingerprints(platform);

            CREATE TABLE device_pairings (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                device_a_id UUID REFERENCES device_fingerprints(id) ON DELETE CASCADE,
                device_b_id UUID REFERENCES device_fingerprints(id) ON DELETE CASCADE,
                shared_secret VARCHAR(64) NOT NULL,
                status VARCHAR(20) DEFAULT 'active',
                initiated_by UUID,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                UNIQUE(device_a_id, device_b_id)
            );

            CREATE TABLE session_history (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                session_type VARCHAR(20) NOT NULL,
                host_device_id UUID REFERENCES device_fingerprints(id),
                host_user_id UUID REFERENCES users(id),
                license_tier VARCHAR(20) NOT NULL,
                end_reason VARCHAR(20),
                participant_count INT DEFAULT 0,
                hub_migration_count INT DEFAULT 0,
                total_bytes_relayed BIGINT DEFAULT 0,
                avg_latency_ms FLOAT,
                avg_packet_loss_percent FLOAT,
                started_at TIMESTAMPTZ DEFAULT NOW(),
                ended_at TIMESTAMPTZ,
                duration_seconds INT
            );

            CREATE INDEX idx_session_history_host ON session_history(host_user_id);
            CREATE INDEX idx_session_history_started ON session_history(started_at);

            CREATE TABLE session_participants (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                session_id UUID REFERENCES session_history(id) ON DELETE CASCADE,
                device_id UUID REFERENCES device_fingerprints(id),
                role VARCHAR(20) NOT NULL,
                is_licensed BOOLEAN DEFAULT FALSE,
                capability_score INT,
                joined_at TIMESTAMPTZ DEFAULT NOW(),
                left_at TIMESTAMPTZ
            );

            CREATE INDEX idx_session_participants_session ON session_participants(session_id);
        `);
    }
}
```

---

## 4. Redis Key Structure

```
# Active sessions (TTL = session duration limit or 24h max)
signaling:session:{sessionId} → {
    hostDeviceId: "fp_abc123",
    hostUserId: "user_xyz",
    licenseTier: "pro",
    maxPeers: 10,
    turnAllowed: true,
    hubDeviceId: "fp_abc123",
    topology: "star",
    state: "active",
    inviteCode: "AVNE-1234-5678",
    createdAt: "2026-02-22T10:00:00Z"
}
TTL: 86400 (24h) or per tier limit

# Session participants (SET — fast membership check)
signaling:session:{sessionId}:participants → SET { "fp_abc123", "fp_def456", "fp_ghi789" }
TTL: same as session

# ICE candidates (LIST — consumed by peer, very short-lived)
signaling:ice:{sessionId}:{fromFingerprint}:{toFingerprint} → LIST [
    "candidate:0 1 UDP 2122255103 192.168.1.5 54321 typ host",
    "candidate:1 1 UDP 1686052863 203.0.113.50 41234 typ srflx raddr 192.168.1.5 rport 54321"
]
TTL: 30 (seconds — consumed immediately by the peer)

# Device capabilities (current state, refreshed on connect)
signaling:device:{fingerprint}:caps → {
    cpuCores: 8,
    ramMb: 12288,
    batteryPercent: 85,
    isCharging: false,
    networkType: "WIFI",
    bandwidthMbps: 50,
    score: 450,
    platform: "ANDROID",
    modules: ["RemoteCast", "NoteAvanue"]
}
TTL: 300 (5 min — refreshed on each capability update)

# TURN credentials (short-lived, shared between signaling and coturn)
turn:credential:{username} → {
    password: "hmac-derived-secret",
    realm: "avanues.com",
    sessionId: "sess_123",
    licenseTier: "pro",
    maxBandwidthBytes: 5368709120
}
TTL: 3600 (1 hour — matches TURN allocation lifetime)

# License cache (avoid hitting PG on every session create)
signaling:license:{licenseToken}:cache → {
    valid: true,
    tier: "pro",
    maxPeers: 10,
    turnAllowed: true,
    userId: "user_xyz"
}
TTL: 300 (5 min cache)

# Push notification tokens
signaling:push:{fingerprint} → {
    platform: "ANDROID",
    token: "fcm:abcdef...",
    updatedAt: "2026-02-22T10:00:00Z"
}
TTL: 2592000 (30 days)

# Invite code → session lookup
signaling:invite:{inviteCode} → sessionId
TTL: same as session
```

---

## 5. Signaling Protocol (WebSocket Messages)

### Client → Server Messages

```typescript
// Device registration (first connect ever, or new device)
{ type: "REGISTER_DEVICE", fingerprint: string, publicKey: string,
  deviceName: string, platform: string, deviceType: string }

// Create session (requires license)
{ type: "CREATE_SESSION", fingerprint: string, licenseToken: string,
  sessionType: "CALL" | "CAST" | "SCREEN_SHARE" | "FILE_TRANSFER",
  capabilities: DeviceCapability }

// Join session (no license needed)
{ type: "JOIN_SESSION", inviteCode: string, fingerprint: string,
  capabilities: DeviceCapability }

// Rejoin session (reconnect after disconnect)
{ type: "REJOIN_SESSION", sessionId: string, fingerprint: string,
  signature: string, capabilities: DeviceCapability }

// Leave session
{ type: "LEAVE_SESSION", sessionId: string, fingerprint: string }

// ICE candidate exchange
{ type: "ICE_CANDIDATE", sessionId: string, fromFingerprint: string,
  toFingerprint: string, candidate: string }

// SDP offer/answer
{ type: "SDP_OFFER", sessionId: string, fromFingerprint: string,
  toFingerprint: string, sdp: string }
{ type: "SDP_ANSWER", sessionId: string, fromFingerprint: string,
  toFingerprint: string, sdp: string }

// Capability update (battery changed, network switched, etc.)
{ type: "CAPABILITY_UPDATE", sessionId: string, fingerprint: string,
  capabilities: DeviceCapability }

// Request TURN credentials
{ type: "REQUEST_TURN", sessionId: string, fingerprint: string }

// Pair device (request pairing with another device)
{ type: "PAIR_REQUEST", fingerprint: string, targetFingerprint: string }
{ type: "PAIR_ACCEPT", fingerprint: string, requestId: string }
{ type: "PAIR_REJECT", fingerprint: string, requestId: string }

// Push token registration
{ type: "REGISTER_PUSH", fingerprint: string, platform: string, token: string }
```

### Server → Client Messages

```typescript
// Registration response
{ type: "DEVICE_REGISTERED", deviceId: string }

// Session lifecycle
{ type: "SESSION_CREATED", sessionId: string, inviteCode: string,
  turnCredentials?: TurnCredential }
{ type: "SESSION_JOINED", sessionId: string, participants: Participant[],
  hubFingerprint: string, turnCredentials?: TurnCredential }
{ type: "SESSION_REJOINED", sessionId: string, participants: Participant[],
  hubFingerprint: string, missedEvents: Event[] }

// Participant events (broadcast to all in session)
{ type: "PARTICIPANT_JOINED", sessionId: string, participant: Participant }
{ type: "PARTICIPANT_LEFT", sessionId: string, fingerprint: string, reason: string }

// Role changes
{ type: "HUB_ELECTED", sessionId: string, hubFingerprint: string,
  reason: "INITIAL" | "DISCONNECT" | "BATTERY_LOW" | "CAPABILITY_CHANGE" }
{ type: "ROLE_CHANGED", sessionId: string, fingerprint: string,
  oldRole: string, newRole: string }

// ICE/SDP relay (server just forwards between peers)
{ type: "ICE_CANDIDATE", fromFingerprint: string, candidate: string }
{ type: "SDP_OFFER", fromFingerprint: string, sdp: string }
{ type: "SDP_ANSWER", fromFingerprint: string, sdp: string }

// TURN credentials issued
{ type: "TURN_CREDENTIALS", username: string, password: string,
  urls: string[], ttlSeconds: number }

// Errors
{ type: "ERROR", code: string, message: string }
// Codes: LICENSE_REQUIRED, LICENSE_EXPIRED, SESSION_FULL,
//        SESSION_NOT_FOUND, DEVICE_BLOCKED, TURN_UNAVAILABLE

// Pairing
{ type: "PAIR_REQUESTED", fromFingerprint: string, fromDeviceName: string,
  requestId: string }
{ type: "PAIR_ESTABLISHED", pairingId: string, peerFingerprint: string }
```

---

## 6. AvanueCentral NestJS Implementation

### Module Structure

```
src/modules/signaling/
├── signaling.module.ts              # NestJS module declaration
├── signaling.gateway.ts             # WebSocket gateway (WS handler)
├── services/
│   ├── session.service.ts           # Session CRUD + lifecycle
│   ├── device.service.ts            # Device registration + fingerprint lookup
│   ├── capability.service.ts        # Capability scoring + hub election
│   ├── turn-credential.service.ts   # TURN credential generation (HMAC)
│   ├── pairing.service.ts           # Device pairing workflow
│   └── push-notification.service.ts # Wake sleeping devices
├── entities/
│   ├── device-fingerprint.entity.ts
│   ├── device-pairing.entity.ts
│   ├── session-history.entity.ts
│   └── session-participant.entity.ts
├── dto/
│   ├── create-session.dto.ts
│   ├── join-session.dto.ts
│   ├── device-capability.dto.ts
│   └── signaling-message.dto.ts
├── guards/
│   └── license.guard.ts             # Validates license on CREATE_SESSION
├── migrations/
│   └── XXXXXX-CreateSignalingTables.ts
└── tests/
    ├── session.service.spec.ts
    ├── capability.service.spec.ts
    └── signaling.gateway.spec.ts
```

### Key Implementation Files

#### WebSocket Gateway (~150 lines)

```typescript
// src/modules/signaling/signaling.gateway.ts

@WebSocketGateway({ namespace: '/signaling', cors: true })
export class SignalingGateway implements OnGatewayConnection, OnGatewayDisconnect {
    @WebSocketServer() server: Server;

    constructor(
        private sessionService: SessionService,
        private deviceService: DeviceService,
        private capabilityService: CapabilityService,
        private turnService: TurnCredentialService,
        private licensingService: LicensingService, // EXISTING module
    ) {}

    handleConnection(client: Socket) {
        // Track connected clients by fingerprint
    }

    handleDisconnect(client: Socket) {
        // Mark device offline, trigger hub re-election if needed
        // Grace period (30s) before removing from session
    }

    @SubscribeMessage('CREATE_SESSION')
    async handleCreateSession(client: Socket, payload: CreateSessionDto) {
        // 1. Validate license
        const license = await this.licensingService.validate(payload.licenseToken);
        if (!license.valid) return { type: 'ERROR', code: 'LICENSE_REQUIRED' };

        // 2. Register/update device
        await this.deviceService.upsert(payload.fingerprint, payload.capabilities);

        // 3. Create session in Redis
        const session = await this.sessionService.create({
            hostFingerprint: payload.fingerprint,
            hostUserId: license.userId,
            licenseTier: license.tier,
            sessionType: payload.sessionType,
        });

        // 4. Issue TURN credentials if tier allows
        const turnCreds = license.turnAllowed
            ? await this.turnService.generate(session.id, payload.fingerprint)
            : null;

        // 5. Join the session's WebSocket room
        client.join(session.id);

        return {
            type: 'SESSION_CREATED',
            sessionId: session.id,
            inviteCode: session.inviteCode,
            turnCredentials: turnCreds,
        };
    }

    @SubscribeMessage('JOIN_SESSION')
    async handleJoinSession(client: Socket, payload: JoinSessionDto) {
        // 1. Find session by invite code (NO license check)
        const session = await this.sessionService.findByInviteCode(payload.inviteCode);
        if (!session) return { type: 'ERROR', code: 'SESSION_NOT_FOUND' };

        // 2. Check participant limit (based on host's license tier)
        const count = await this.sessionService.participantCount(session.id);
        if (count >= session.maxPeers) return { type: 'ERROR', code: 'SESSION_FULL' };

        // 3. Register/update device
        await this.deviceService.upsert(payload.fingerprint, payload.capabilities);

        // 4. Add to session
        await this.sessionService.addParticipant(session.id, payload.fingerprint, false);

        // 5. Score capabilities, check if hub should change
        const election = await this.capabilityService.evaluateElection(session.id);

        // 6. Issue TURN credentials (charged to host's license)
        const turnCreds = session.turnAllowed
            ? await this.turnService.generate(session.id, payload.fingerprint)
            : null;

        // 7. Join room, notify existing participants
        client.join(session.id);
        this.server.to(session.id).emit('message', {
            type: 'PARTICIPANT_JOINED',
            sessionId: session.id,
            participant: { fingerprint: payload.fingerprint, ... },
        });

        // 8. If hub changed, broadcast
        if (election.hubChanged) {
            this.server.to(session.id).emit('message', {
                type: 'HUB_ELECTED',
                sessionId: session.id,
                hubFingerprint: election.newHubFingerprint,
                reason: 'CAPABILITY_CHANGE',
            });
        }

        return {
            type: 'SESSION_JOINED',
            sessionId: session.id,
            participants: await this.sessionService.getParticipants(session.id),
            hubFingerprint: election.currentHub,
            turnCredentials: turnCreds,
        };
    }

    @SubscribeMessage('ICE_CANDIDATE')
    @SubscribeMessage('SDP_OFFER')
    @SubscribeMessage('SDP_ANSWER')
    async handleRelay(client: Socket, payload: any) {
        // Pure relay — forward message to the target peer
        const targetSocket = this.getSocketByFingerprint(payload.toFingerprint);
        if (targetSocket) {
            targetSocket.emit('message', {
                ...payload,
                fromFingerprint: payload.fromFingerprint,
            });
        } else {
            // Peer offline — store in Redis for when they reconnect
            await this.sessionService.queueMessage(payload.sessionId, payload.toFingerprint, payload);
        }
    }
}
```

#### Capability Scorer (~60 lines)

```typescript
// src/modules/signaling/services/capability.service.ts

@Injectable()
export class CapabilityService {
    constructor(private redis: RedisService) {}

    calculateScore(caps: DeviceCapabilityDto): number {
        let score = 0;
        score += caps.cpuCores * 10;
        score += Math.floor(caps.ramMb / 100);
        score += caps.isCharging ? 200 : (caps.batteryPercent || 0) * 2;
        score += (caps.bandwidthMbps || 0) * 5;
        score += caps.deviceType === 'DESKTOP' ? 100 : 0;
        score += caps.networkType === 'ETHERNET' ? 50 : 0;
        score += Math.floor((caps.screenWidth || 0) / 10);
        score += (caps.supportedCodecs?.length || 0) * 15;
        return score;
    }

    async evaluateElection(sessionId: string): Promise<ElectionResult> {
        const participants = await this.redis.smembers(`signaling:session:${sessionId}:participants`);
        let highestScore = 0;
        let newHub = '';

        for (const fp of participants) {
            const capsJson = await this.redis.get(`signaling:device:${fp}:caps`);
            if (!capsJson) continue;
            const caps = JSON.parse(capsJson);
            if (caps.score > highestScore) {
                highestScore = caps.score;
                newHub = fp;
            }
        }

        const session = JSON.parse(await this.redis.get(`signaling:session:${sessionId}`));
        const hubChanged = newHub !== session.hubDeviceId;

        if (hubChanged) {
            session.hubDeviceId = newHub;
            await this.redis.set(`signaling:session:${sessionId}`, JSON.stringify(session));
        }

        return { currentHub: newHub, hubChanged, highestScore };
    }
}
```

#### TURN Credential Service (~40 lines)

```typescript
// src/modules/signaling/services/turn-credential.service.ts

@Injectable()
export class TurnCredentialService {
    constructor(
        private redis: RedisService,
        @Inject('TURN_SECRET') private turnSecret: string,
    ) {}

    async generate(sessionId: string, fingerprint: string): Promise<TurnCredential> {
        const ttlSeconds = 3600; // 1 hour
        const expiresAt = Math.floor(Date.now() / 1000) + ttlSeconds;
        const username = `${expiresAt}:${fingerprint}`;

        // HMAC-SHA1 as per coturn's long-term credential mechanism
        const password = this.hmacSha1(this.turnSecret, username);

        await this.redis.set(`turn:credential:${username}`, JSON.stringify({
            password, sessionId, realm: 'avanues.com',
        }), 'EX', ttlSeconds);

        return {
            username,
            password,
            urls: [
                `stun:stun.avanues.com:3478`,
                `turn:turn.avanues.com:3478?transport=udp`,
                `turn:turn.avanues.com:3478?transport=tcp`,
                `turns:turn.avanues.com:5349?transport=tcp`,
            ],
            ttlSeconds,
        };
    }

    private hmacSha1(secret: string, data: string): string {
        return createHmac('sha1', secret).update(data).digest('base64');
    }
}
```

---

## 7. STUN/TURN Server (coturn)

### Why coturn Instead of Custom

Building STUN is simple (~300 lines), but building a production TURN server is complex:
- RFC compliance (5 RFCs: 5389, 5766, 6062, 6156, 8656)
- UDP relay with proper port allocation
- TLS/DTLS support
- Rate limiting, DDoS protection
- Bandwidth accounting

**coturn** is the industry-standard open-source TURN server (used by Jitsi, NextCloud, Signal). It's battle-tested, performant, and supports Redis-backed credential validation.

### coturn Configuration

```bash
# /etc/turnserver.conf

# Network
listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0
relay-ip=<server-public-ip>
external-ip=<server-public-ip>

# Authentication via Redis (shares credentials with AvanueCentral)
use-auth-secret
static-auth-secret=<same-secret-as-TURN_SECRET-in-AvanueCentral>

# Or Redis-based dynamic credentials:
# redis-userdb="ip=127.0.0.1 dbname=0 port=6379 connect_timeout=30"
# redis-statsdb="ip=127.0.0.1 dbname=1 port=6379 connect_timeout=30"

# TLS certificates (same as AvanueCentral's domain cert)
cert=/etc/letsencrypt/live/turn.avanues.com/fullchain.pem
pkey=/etc/letsencrypt/live/turn.avanues.com/privkey.pem

# Limits
total-quota=100
bps-capacity=0
stale-nonce=600
max-bps=1000000           # 1 Mbps per allocation (adjustable per tier)

# Security
no-multicast-peers
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=192.168.0.0-192.168.255.255

# Logging
log-file=/var/log/turnserver.log
verbose

# Realm
realm=avanues.com
```

### Docker Compose Addition (AvanueCentral)

```yaml
# Add to AvanueCentral's docker-compose.yml

  coturn:
    image: coturn/coturn:4.6
    ports:
      - "3478:3478/udp"
      - "3478:3478/tcp"
      - "5349:5349/tcp"
      - "49152-49200:49152-49200/udp"  # Relay port range
    volumes:
      - ./config/turnserver.conf:/etc/turnserver.conf
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - redis
    networks:
      - ava-network
    restart: unless-stopped
```

---

## 8. NetAvanue KMP Client Library

The NetAvanue module on the device side connects to AvanueCentral's signaling server.

### Package Structure

```
Modules/NetAvanue/src/
├── commonMain/kotlin/com/augmentalis/netavanue/
│   ├── signaling/
│   │   ├── SignalingClient.kt          # WebSocket connection to AvanueCentral
│   │   ├── SignalingMessage.kt         # All message types (sealed class)
│   │   └── SignalingProtocol.kt        # Serialize/deserialize
│   ├── capability/
│   │   ├── DeviceCapability.kt         # Capability data model
│   │   ├── DeviceFingerprint.kt        # Stable device ID + key pair
│   │   ├── CapabilityCollector.kt      # Gather device capabilities (expect/actual)
│   │   └── CapabilityScorer.kt         # Score calculation (same algorithm as server)
│   ├── session/
│   │   ├── Session.kt                  # Session state + participant list
│   │   ├── SessionManager.kt          # Create/join/leave/rejoin
│   │   └── RoleManager.kt             # Track role (HOST/HUB/SPOKE/GUEST)
│   ├── ice/
│   │   ├── IceAgent.kt                # ICE candidate gathering + connectivity checks
│   │   ├── IceCandidate.kt            # Candidate data model
│   │   └── StunClient.kt              # STUN Binding Request/Response
│   ├── pairing/
│   │   ├── PairingManager.kt          # QR code / invite code pairing flow
│   │   └── PairedDevice.kt            # Stored paired device info
│   └── peer/
│       ├── PeerConnection.kt          # High-level: create offer, exchange, data channel
│       └── DataChannel.kt             # Bidirectional data channel
├── androidMain/kotlin/.../
│   ├── capability/CapabilityCollector.android.kt  # Android system info
│   ├── fingerprint/DeviceFingerprint.android.kt   # Android ID + Keystore
│   └── udp/UdpSocket.android.kt                   # DatagramSocket
├── desktopMain/kotlin/.../
│   ├── capability/CapabilityCollector.desktop.kt
│   ├── fingerprint/DeviceFingerprint.desktop.kt
│   └── udp/UdpSocket.desktop.kt
├── iosMain/kotlin/.../
│   ├── capability/CapabilityCollector.ios.kt       # UIDevice info
│   ├── fingerprint/DeviceFingerprint.ios.kt        # identifierForVendor + Keychain
│   └── udp/UdpSocket.ios.kt                        # POSIX sendto/recvfrom
└── jsMain/kotlin/.../
    ├── PeerConnection.js.kt                        # Wraps browser RTCPeerConnection
    └── capability/CapabilityCollector.js.kt        # navigator.hardwareConcurrency, etc.
```

### Build Configuration

```kotlin
// Modules/NetAvanue/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget { ... }
    jvm("desktop") { ... }
    iosArm64(); iosSimulatorArm64()
    js(IR) { browser(); nodejs() }  // Web target

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":Modules:HTTPAvanue"))  // WebSocket client for signaling
                api(project(":Modules:Foundation"))
                api(project(":Modules:Logging"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
```

---

## 9. Implementation Phases

| Phase | Scope | Where | Effort | Prerequisite |
|-------|-------|-------|--------|-------------|
| **1** | AvanueCentral: Signaling module scaffold | AvanueCentral repo | 4 hrs | None |
| | - NestJS module + WebSocket gateway | | | |
| | - PostgreSQL migration (4 tables) | | | |
| | - Redis key setup | | | |
| | - License integration | | | |
| **2** | AvanueCentral: Session + device services | AvanueCentral repo | 6 hrs | Phase 1 |
| | - CREATE_SESSION with license check | | | |
| | - JOIN_SESSION (no license) | | | |
| | - REJOIN_SESSION with fingerprint | | | |
| | - ICE/SDP relay (pure forward) | | | |
| | - Capability scoring + hub election | | | |
| **3** | AvanueCentral: TURN + pairing | AvanueCentral repo | 4 hrs | Phase 2 |
| | - coturn Docker setup | | | |
| | - TURN credential generation (HMAC) | | | |
| | - Device pairing flow | | | |
| | - Push notification registration | | | |
| **4** | NetAvanue: Signaling client | NewAvanues repo | 6 hrs | Phase 2 |
| | - WebSocket connection to AvanueCentral | | | |
| | - All message types (sealed class) | | | |
| | - SessionManager (create/join/leave/rejoin) | | | |
| | - DeviceFingerprint (platform expect/actual) | | | |
| **5** | NetAvanue: Capability + election | NewAvanues repo | 4 hrs | Phase 4 |
| | - CapabilityCollector (platform expect/actual) | | | |
| | - CapabilityScorer (same algorithm as server) | | | |
| | - RoleManager (track HOST/HUB/SPOKE/GUEST) | | | |
| **6** | NetAvanue: ICE + STUN | NewAvanues repo | 8 hrs | Phase 4 |
| | - STUN client (Binding Request/Response) | | | |
| | - ICE candidate gathering | | | |
| | - ICE connectivity checks | | | |
| | - UdpSocket (platform expect/actual) | | | |
| **7** | NetAvanue: PeerConnection + DataChannel | NewAvanues repo | 6 hrs | Phase 6 |
| | - PeerConnection high-level API | | | |
| | - DataChannel with Flow-based messages | | | |
| | - Session persistence (ICE restart) | | | |
| **8** | Integration: RemoteCast + NetAvanue | NewAvanues repo | 4 hrs | Phase 7 |
| | - Wire CastWebSocketServer to use NetAvanue | | | |
| | - NAT traversal for screen casting | | | |
| | - Browser receiver via SimpleWebServer | | | |
| **9** | Web/JS target | Both repos | 6 hrs | Phase 7 |
| | - jsMain: wrap browser RTCPeerConnection | | | |
| | - Browser signaling client | | | |
| | - Universal web access for calls | | | |
| **10** | Testing + polish | Both repos | 4 hrs | All |
| | - End-to-end test: phone → glasses through NAT | | | |
| | - Load test signaling server | | | |
| | - Session persistence test (disconnect/reconnect) | | | |
| | **Total** | | **~52 hrs** | |

---

## 10. Invite Code Format

```
Format: AVNE-XXXX-XXXX  (8 alphanumeric chars, grouped for readability)
Example: AVNE-K7M3-P9X2

Generation: Random 8 chars from [A-Z0-9], excluding confusable chars (0/O, 1/I/L)
Alphabet: "23456789ABCDEFGHJKMNPQRSTUVWXYZ" (30 chars, 30^8 = 656 billion combinations)
Uniqueness: Checked against Redis before issuing
Lifetime: Same as session TTL (24h max)
```

Also supports QR code containing: `avanues://join/{inviteCode}` deep link.

---

## 11. Security Considerations

| Threat | Mitigation |
|--------|-----------|
| Rogue device spoofing capabilities | Ed25519 signed capability messages, verified by server |
| Session hijacking | Invite codes are random, session ID is UUID, WebSocket uses WSS |
| TURN abuse (bandwidth theft) | HMAC credentials with TTL, per-tier bandwidth quotas, monitored by coturn |
| Man-in-the-middle | WSS for signaling, DTLS for media, Ed25519 fingerprint pinning |
| DDoS on STUN/TURN | coturn rate limiting, cloud provider DDoS protection |
| Expired license continuing session | License cached in Redis with 5-min TTL, periodic re-validation |
| Guest impersonating host | License token only accepted from the original session creator's fingerprint |

---

## 12. Related Documents

- NetAvanue Core Plan: `docs/plans/NetAvanue/NetAvanue-Plan-PeerNetworkingModule-260222-V1.md`
- HTTPAvanue v2.0: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-V2Implementation-260222-V1.md`
- HTTPAvanue Chapter 104: `Docs/MasterDocs/HTTPAvanue/Developer-Manual-Chapter104-HTTPAvanueV2ZeroDepEnhancements.md`
- RemoteCast Architecture: `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- AvanueCentral: `/Volumes/M-Drive/Coding/AvanueCentral/`
- AVACode Recipes: `docs/plans/AVACode/AVACode-Plan-RecipeSystemPending-260222-V1.md`
