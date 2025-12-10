# VoiceOS Exception Handling Guide

**Version:** 1.0
**Author:** Manoj Jhawar
**Created:** 2025-11-09
**Phase:** 4 (Code Quality)

---

## Overview

VoiceOS provides a comprehensive custom exception hierarchy for domain-specific error handling. This guide shows how to use these exceptions effectively.

### Exception Hierarchy

```
VoiceOSException (base)
├── DatabaseException
│   ├── BackupException
│   ├── RestoreException
│   ├── IntegrityException
│   ├── MigrationException
│   └── TransactionException
├── SecurityException
│   ├── EncryptionException
│   ├── DecryptionException
│   ├── SignatureException
│   ├── UnauthorizedException
│   └── KeystoreException
├── CommandException
│   ├── ExecutionException
│   ├── ParsingException
│   ├── RateLimitException
│   └── CircuitBreakerException
├── ScrapingException
│   ├── ElementException
│   ├── HierarchyException
│   └── CacheException
├── PrivacyException
│   ├── ConsentException
│   └── RetentionException
└── AccessibilityException
    ├── ServiceException
    ├── NodeException
    └── ActionException
```

---

## Table of Contents

1. [Basic Usage](#basic-usage)
2. [Database Exceptions](#database-exceptions)
3. [Security Exceptions](#security-exceptions)
4. [Command Exceptions](#command-exceptions)
5. [Error Handling Patterns](#error-handling-patterns)
6. [Best Practices](#best-practices)

---

## Basic Usage

### Throwing Custom Exceptions

```kotlin
import com.augmentalis.voiceoscore.exceptions.*

// Simple exception
throw DatabaseException.BackupException("Failed to create backup")

// With cause
try {
    performDatabaseOperation()
} catch (e: IOException) {
    throw DatabaseException.BackupException("Backup failed due to I/O error", e)
}

// With metadata
throw SecurityException.SignatureException(
    message = "Invalid app signature",
    packageName = "com.example.app"
)
```

### Catching Custom Exceptions

```kotlin
try {
    createDatabaseBackup()
} catch (e: DatabaseException.BackupException) {
    Log.e(TAG, "Backup failed: ${e.getFullMessage()}", e)
    notifyUser("Backup failed: ${e.message}")
} catch (e: DatabaseException) {
    Log.e(TAG, "Database error: ${e.getFullMessage()}", e)
}
```

---

## Database Exceptions

### BackupException

**When to use:** Database backup operations fail

```kotlin
class DatabaseBackupManager(private val context: Context) {
    suspend fun createBackup(label: String?): BackupResult {
        return try {
            // Backup logic...
            BackupResult(success = true, message = "Backup created")
        } catch (e: IOException) {
            throw DatabaseException.BackupException(
                message = "Failed to write backup file: ${e.message}",
                cause = e
            )
        } catch (e: Exception) {
            throw DatabaseException.BackupException(
                message = "Backup creation failed: ${e.message}",
                cause = e
            )
        }
    }
}
```

### IntegrityException

**When to use:** Database integrity checks fail

```kotlin
class DatabaseIntegrityChecker {
    fun checkIntegrity(databaseName: String): IntegrityResult {
        val errors = mutableListOf<String>()

        // Run integrity check...
        if (errors.isNotEmpty()) {
            throw DatabaseException.IntegrityException(
                message = "Database integrity check failed",
                corruptionDetails = errors
            )
        }

        return IntegrityResult(isHealthy = true)
    }
}
```

### MigrationException

**When to use:** Database migrations fail

```kotlin
class MigrationRollbackManager {
    suspend fun prepareMigration(
        databaseName: String,
        fromVersion: Int,
        toVersion: Int
    ): PreparationResult {
        try {
            // Migration logic...
        } catch (e: Exception) {
            throw DatabaseException.MigrationException(
                message = "Migration preparation failed",
                cause = e,
                fromVersion = fromVersion,
                toVersion = toVersion
            )
        }
    }
}
```

---

## Security Exceptions

### EncryptionException / DecryptionException

**When to use:** Encryption/decryption operations fail

```kotlin
class DataEncryptionManager {
    fun encrypt(plaintext: String?): String? {
        try {
            // Encryption logic...
            return encryptedData
        } catch (e: GeneralSecurityException) {
            throw SecurityException.EncryptionException(
                message = "Encryption failed: ${e.message}",
                cause = e
            )
        }
    }

    fun decrypt(ciphertext: String?): String? {
        try {
            // Decryption logic...
            return decryptedData
        } catch (e: GeneralSecurityException) {
            throw SecurityException.DecryptionException(
                message = "Decryption failed: ${e.message}",
                cause = e
            )
        } catch (e: BadPaddingException) {
            throw SecurityException.DecryptionException(
                message = "Invalid ciphertext or corrupted data",
                cause = e
            )
        }
    }
}
```

### UnauthorizedException

**When to use:** Unauthorized access attempts

```kotlin
class ContentProviderSecurityValidator {
    fun validateCaller(callingPackage: String?): Boolean {
        if (callingPackage == null) {
            throw SecurityException.UnauthorizedException(
                message = "Caller package is null",
                packageName = "unknown"
            )
        }

        if (packageBlacklist.contains(callingPackage)) {
            throw SecurityException.UnauthorizedException(
                message = "Package is blacklisted",
                packageName = callingPackage
            )
        }

        return true
    }
}
```

### SignatureException

**When to use:** Signature validation fails

```kotlin
fun validateSignature(packageName: String): Boolean {
    val fingerprint = getSignatureFingerprint(packageName)

    if (!signatureWhitelist.contains(fingerprint)) {
        throw SecurityException.SignatureException(
            message = "Invalid signature fingerprint",
            packageName = packageName
        )
    }

    return true
}
```

---

## Command Exceptions

### ExecutionException

**When to use:** Command execution fails

```kotlin
class CommandManager {
    fun executeCommand(command: Command): CommandResult {
        try {
            // Execute command...
            return CommandResult(success = true)
        } catch (e: Exception) {
            throw CommandException.ExecutionException(
                message = "Command execution failed: ${e.message}",
                cause = e,
                commandText = command.text
            )
        }
    }
}
```

### RateLimitException

**When to use:** Command rate limit exceeded

```kotlin
class CommandRateLimiter {
    fun checkRateLimit(commandId: String): Boolean {
        val state = rateLimitStates[commandId]

        if (state != null && state.isLimitExceeded()) {
            throw CommandException.RateLimitException(
                message = "Rate limit exceeded",
                commandId = commandId,
                retryAfterMs = state.getRetryAfterMs()
            )
        }

        return true
    }
}
```

### CircuitBreakerException

**When to use:** Circuit breaker is open

```kotlin
class CircuitBreaker {
    fun execute(commandId: String, action: () -> Unit) {
        if (state == State.OPEN) {
            throw CommandException.CircuitBreakerException(
                message = "Circuit breaker is OPEN, rejecting command",
                commandId = commandId
            )
        }

        try {
            action()
        } catch (e: Exception) {
            recordFailure()
            throw e
        }
    }
}
```

---

## Error Handling Patterns

### Pattern 1: Wrap and Rethrow

**When to use:** Convert low-level exceptions to domain exceptions

```kotlin
suspend fun createBackup(): BackupResult {
    return withContext(Dispatchers.IO) {
        try {
            // Low-level operations...
            performFileOperations()
        } catch (e: IOException) {
            // Wrap in domain exception
            throw DatabaseException.BackupException(
                message = "I/O error during backup",
                cause = e
            )
        } catch (e: SecurityException) {
            // Wrap in domain exception
            throw DatabaseException.BackupException(
                message = "Permission denied for backup",
                cause = e
            )
        }
    }
}
```

### Pattern 2: Try-Catch with Recovery

**When to use:** Attempt recovery from errors

```kotlin
fun executeCommandWithRetry(command: Command): CommandResult {
    repeat(MAX_RETRIES) { attempt ->
        try {
            return executeCommand(command)
        } catch (e: CommandException.RateLimitException) {
            // Wait and retry
            Thread.sleep(e.retryAfterMs ?: 1000)
        } catch (e: CommandException.CircuitBreakerException) {
            // Circuit breaker open, give up
            return CommandResult(
                success = false,
                error = e
            )
        }
    }

    throw CommandException.ExecutionException(
        message = "Max retries exceeded",
        commandText = command.text
    )
}
```

### Pattern 3: Granular Error Handling

**When to use:** Different handling for different exception types

```kotlin
fun performDatabaseOperation() {
    try {
        database.query()
    } catch (e: DatabaseException.IntegrityException) {
        // Database corrupted - attempt repair
        Log.e(TAG, "Database corrupted: ${e.corruptionDetails}")
        attemptDatabaseRepair()
    } catch (e: DatabaseException.BackupException) {
        // Backup failed - notify user
        Log.e(TAG, "Backup failed: ${e.message}")
        notifyBackupFailure()
    } catch (e: DatabaseException) {
        // Generic database error - log and continue
        Log.e(TAG, "Database error: ${e.getFullMessage()}", e)
    }
}
```

### Pattern 4: Check Exception Cause

**When to use:** Need to handle specific underlying causes

```kotlin
fun handleException(e: VoiceOSException) {
    when {
        e.isCausedBy<IOException>() -> {
            Log.e(TAG, "I/O error: ${e.message}")
            retryWithBackoff()
        }
        e.isCausedBy<SecurityException>() -> {
            Log.e(TAG, "Security error: ${e.message}")
            requestPermissions()
        }
        else -> {
            Log.e(TAG, "Unknown error: ${e.getFullMessage()}", e)
        }
    }
}
```

---

## Best Practices

### DO ✅

1. **Use specific exception types**
   ```kotlin
   // ✅ GOOD: Specific exception
   throw DatabaseException.BackupException("Backup failed")

   // ❌ BAD: Generic exception
   throw Exception("Backup failed")
   ```

2. **Include context in error messages**
   ```kotlin
   // ✅ GOOD: Descriptive message
   throw SecurityException.UnauthorizedException(
       message = "Access denied: insufficient permissions",
       packageName = callingPackage
   )

   // ❌ BAD: Vague message
   throw SecurityException.UnauthorizedException("Access denied")
   ```

3. **Chain exceptions properly**
   ```kotlin
   // ✅ GOOD: Preserve cause
   try {
       performOperation()
   } catch (e: IOException) {
       throw DatabaseException.BackupException("Backup failed", e)
   }

   // ❌ BAD: Lose original exception
   try {
       performOperation()
   } catch (e: IOException) {
       throw DatabaseException.BackupException("Backup failed")
   }
   ```

4. **Add metadata when available**
   ```kotlin
   // ✅ GOOD: Include version info
   throw DatabaseException.MigrationException(
       message = "Migration failed",
       fromVersion = 9,
       toVersion = 10
   )
   ```

### DON'T ❌

1. **Don't catch and ignore custom exceptions**
   ```kotlin
   // ❌ BAD: Swallowing exceptions
   try {
       createBackup()
   } catch (e: DatabaseException) {
       // Silent failure
   }

   // ✅ GOOD: Log at minimum
   try {
       createBackup()
   } catch (e: DatabaseException) {
       Log.e(TAG, "Backup failed: ${e.getFullMessage()}", e)
       throw e // or handle appropriately
   }
   ```

2. **Don't use generic exceptions for domain errors**
   ```kotlin
   // ❌ BAD
   throw RuntimeException("Database backup failed")

   // ✅ GOOD
   throw DatabaseException.BackupException("Backup failed")
   ```

3. **Don't lose exception information**
   ```kotlin
   // ❌ BAD: Converting to string loses stack trace
   val errorMessage = exception.toString()
   throw DatabaseException.BackupException(errorMessage)

   // ✅ GOOD: Preserve original exception
   throw DatabaseException.BackupException("Backup failed", exception)
   ```

---

## Error Code Reference

| Exception Type | Error Code | Meaning |
|----------------|------------|---------|
| DatabaseException.BackupException | DB_BACKUP_FAILED | Backup operation failed |
| DatabaseException.RestoreException | DB_RESTORE_FAILED | Restore operation failed |
| DatabaseException.IntegrityException | DB_INTEGRITY_FAILED | Integrity check failed |
| DatabaseException.MigrationException | DB_MIGRATION_FAILED | Migration failed |
| DatabaseException.TransactionException | DB_TRANSACTION_FAILED | Transaction failed |
| SecurityException.EncryptionException | SECURITY_ENCRYPTION_FAILED | Encryption failed |
| SecurityException.DecryptionException | SECURITY_DECRYPTION_FAILED | Decryption failed |
| SecurityException.SignatureException | SECURITY_SIGNATURE_INVALID | Invalid signature |
| SecurityException.UnauthorizedException | SECURITY_UNAUTHORIZED | Unauthorized access |
| SecurityException.KeystoreException | SECURITY_KEYSTORE_FAILED | Keystore operation failed |
| CommandException.ExecutionException | COMMAND_EXECUTION_FAILED | Command execution failed |
| CommandException.ParsingException | COMMAND_PARSING_FAILED | Command parsing failed |
| CommandException.RateLimitException | COMMAND_RATE_LIMIT | Rate limit exceeded |
| CommandException.CircuitBreakerException | COMMAND_CIRCUIT_BREAKER_OPEN | Circuit breaker open |

---

## Migration Guide

### Converting Existing Code

**Before (Generic exceptions):**
```kotlin
fun createBackup(): BackupResult {
    try {
        // backup logic...
    } catch (e: Exception) {
        Log.e(TAG, "Backup failed", e)
        return BackupResult(success = false, message = "Backup failed")
    }
}
```

**After (Custom exceptions):**
```kotlin
fun createBackup(): BackupResult {
    try {
        // backup logic...
    } catch (e: IOException) {
        throw DatabaseException.BackupException(
            message = "I/O error during backup: ${e.message}",
            cause = e
        )
    } catch (e: Exception) {
        throw DatabaseException.BackupException(
            message = "Backup creation failed: ${e.message}",
            cause = e
        )
    }
}
```

---

**End of Guide**

**Last Updated:** 2025-11-09
**Version:** 1.0
**Phase:** 4 (Code Quality)
