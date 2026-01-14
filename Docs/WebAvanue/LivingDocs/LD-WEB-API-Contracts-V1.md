# LD-WEB-API-Contracts-V1

**Living Document** | WebAvanue API Contracts
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## API Overview

WebAvanue provides REST APIs for web client interaction with backend services.

---

## REST Endpoints

### Authentication
```typescript
POST /api/auth/login
Request: { email: string, password: string }
Response: { token: string, user: User }

POST /api/auth/logout
Headers: { Authorization: "Bearer {token}" }
```

### Dashboard Data
```typescript
GET /api/dashboard/stats
Response: {
    modules: ModuleStatus[],
    system: SystemHealth,
    recent: Activity[]
}
```

---

## Data Contracts

### Module Status
```typescript
interface ModuleStatus {
    name: string;
    status: 'active' | 'inactive' | 'error';
    health: number; // 0-100
    lastUpdate: Date;
}
```

### System Health
```typescript
interface SystemHealth {
    cpu: number;
    memory: number;
    uptime: number;
}
```

---

## WebSocket Events

```typescript
// Subscribe to module updates
ws.on('module:update', (data: ModuleStatus) => {})

// System notifications
ws.on('system:notification', (data: Notification) => {})
```

---

**Last Updated:** 2025-12-15 | **Version:** 12.0.0
