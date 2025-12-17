# LD-CPT-API-Contracts-V1

**Living Document** | Cockpit API Contracts
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## API Overview

Cockpit provides management APIs for module control and monitoring.

---

## Module Management

### Get Module Status
```kotlin
suspend fun getModuleStatus(moduleId: String): ModuleStatus

data class ModuleStatus(
    val id: String,
    val name: String,
    val status: Status,
    val health: Int,
    val lastUpdate: Long
)
```

### Update Module Config
```kotlin
suspend fun updateModuleConfig(
    moduleId: String,
    config: Map<String, Any>
): Result<Unit>
```

---

## System Monitoring

### Get System Health
```kotlin
suspend fun getSystemHealth(): SystemHealth

data class SystemHealth(
    val cpu: Float,
    val memory: Float,
    val diskSpace: Long,
    val uptime: Long
)
```

---

## Configuration Management

### Load IDC Config
```kotlin
fun loadConfig(path: String): IdcConfig
```

### Validate Config
```kotlin
fun validateConfig(config: IdcConfig): ValidationResult
```

---

**Last Updated:** 2025-12-15 | **Version:** 12.0.0
