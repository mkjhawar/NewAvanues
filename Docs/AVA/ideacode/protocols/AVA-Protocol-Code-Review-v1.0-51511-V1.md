# Protocol: Mandatory Code Review v1.0

**Version:** 1.0
**Status:** Active
**Effective Date:** 2025-11-15
**Framework Version:** 8.4

---

## 🚨 MANDATORY: Self-Review After Each Phase/Sprint

**CRITICAL - AI MUST EXECUTE - NOT OPTIONAL:**

AI assistants MUST perform a comprehensive self-review of ALL code written during a phase or sprint BEFORE marking it complete. This review checks for errors, omissions, inconsistencies, unimplemented TODOs, and missing functions/classes.

---

## Purpose

**Problem:** Without mandatory code review:
- ❌ Errors slip through (typos, logic bugs, edge cases)
- ❌ TODOs left unimplemented ("// TODO: implement later")
- ❌ Missing functions referenced but never written
- ❌ Inconsistencies between spec and implementation
- ❌ Incomplete features shipped
- ❌ Documentation out of sync with code

**Solution:** Systematic self-review checklist executed BEFORE phase completion

---

## When to Review

### Review Triggers

**MANDATORY review BEFORE:**
1. ✅ Completing any phase (specify → plan → implement → test → archive)
2. ✅ Marking feature as "done" or "completed"
3. ✅ Creating pull request for merge to main
4. ✅ Sprint/iteration end (if using Agile)
5. ✅ Before `ideacode_archive` tool invocation
6. ✅ After YOLO mode completion
7. ✅ Before final commit of feature branch

**OPTIONAL review (but recommended):**
- After implementing complex algorithm
- After major refactoring
- Before code review by human
- Mid-sprint checkpoint

---

## Review Checklist

### 1. Code Completeness (30 checks)

#### 1.1 TODO/FIXME Audit
- [ ] Search entire codebase for `TODO`, `FIXME`, `HACK`, `XXX`
- [ ] Each TODO has:
  - [ ] Clear description of what needs to be done
  - [ ] Reason why it's deferred (if applicable)
  - [ ] Issue/ticket number (if tracked)
- [ ] Critical TODOs implemented BEFORE marking phase complete
- [ ] Non-critical TODOs moved to backlog with ticket number

**Command:**
```bash
# Find all TODOs
grep -rn "TODO\|FIXME\|HACK\|XXX" src/

# Example output analysis:
# src/auth/AuthManager.kt:42: // TODO: implement token refresh
# ❌ BLOCKER: Critical functionality not implemented

# src/ui/ProfileScreen.kt:120: // TODO: add loading indicator (nice-to-have)
# ✅ OK: Non-critical, moved to backlog #1234
```

#### 1.2 Function/Class Implementation
- [ ] All declared functions have implementations (not just stubs)
- [ ] All abstract classes have concrete implementations
- [ ] All interfaces have at least one implementation
- [ ] No empty function bodies (except intentional no-ops)
- [ ] No commented-out code blocks (remove or explain)

**Check:**
```kotlin
// ❌ INCOMPLETE: Empty stub
fun authenticateUser(credentials: Credentials): AuthResult {
    // TODO: implement
}

// ✅ COMPLETE: Full implementation
fun authenticateUser(credentials: Credentials): AuthResult {
    validateCredentials(credentials)
    val token = oauth2Provider.authenticate(credentials)
    return AuthResult.Success(token)
}
```

#### 1.3 Error Handling
- [ ] All `try-catch` blocks handle errors appropriately
- [ ] No empty catch blocks (`catch (e: Exception) {}`)
- [ ] Errors logged with context (user ID, timestamp, stack trace)
- [ ] User-facing error messages are clear and actionable
- [ ] Network calls have timeout and retry logic
- [ ] Database operations handle exceptions

**Check:**
```kotlin
// ❌ INCOMPLETE: Silent failure
try {
    apiClient.fetchUserData()
} catch (e: Exception) {
    // Swallows error - user sees nothing!
}

// ✅ COMPLETE: Proper error handling
try {
    apiClient.fetchUserData()
} catch (e: NetworkException) {
    logger.error("Failed to fetch user data", e)
    showErrorToUser("Unable to load profile. Check your connection.")
    return Result.Error(e)
}
```

#### 1.4 Edge Cases
- [ ] Null checks where nullable types used
- [ ] Empty collection handling (lists, maps, sets)
- [ ] Boundary conditions tested (min/max values, zero, negative)
- [ ] Concurrent access handled (thread safety, race conditions)
- [ ] Resource cleanup in finally blocks or use-scopes

#### 1.5 Missing Dependencies
- [ ] All imported classes/functions exist and are accessible
- [ ] All dependencies declared in build.gradle.kts
- [ ] Version conflicts resolved
- [ ] Transitive dependencies compatible

---

### 2. Consistency Checks (20 checks)

#### 2.1 Spec-to-Implementation Alignment
- [ ] All requirements from spec.md implemented
- [ ] API signatures match spec exactly (function names, parameters, return types)
- [ ] Data models match spec definitions
- [ ] Validation rules implemented as specified
- [ ] Business logic matches spec algorithms

**Cross-reference:**
```markdown
Spec (spec.md):
- User SHALL be authenticated using OAuth2 PKCE flow
- Access tokens SHALL expire after 15 minutes
- Refresh tokens SHALL be stored encrypted

Code Review:
✅ OAuth2 PKCE implemented: AuthManager.kt:45-120
✅ Token expiry: 15min configured in OAuth2Config.kt:12
✅ Encryption: RefreshTokenStore.kt:30-45 (AES-256)
```

#### 2.2 Naming Consistency
- [ ] Naming conventions followed (camelCase, PascalCase, SCREAMING_SNAKE_CASE)
- [ ] Variable names descriptive (`user` not `u`, `totalPrice` not `tp`)
- [ ] Function names describe action (`calculateTotal()` not `calc()`)
- [ ] Class names are nouns (`UserRepository` not `UserRepoService`)
- [ ] Boolean variables prefixed appropriately (`isActive`, `hasPermission`)

#### 2.3 Code Style
- [ ] Indentation consistent (2 or 4 spaces, not mixed)
- [ ] Line length within limits (<120 characters)
- [ ] No trailing whitespace
- [ ] Imports organized (alphabetical, grouped by package)
- [ ] Comments use consistent format (KDoc, JSDoc, etc.)

#### 2.4 Architecture Consistency
- [ ] Layering respected (UI → ViewModel → Repository → DataSource)
- [ ] Dependency injection used consistently
- [ ] No circular dependencies
- [ ] Single Responsibility Principle followed
- [ ] DRY principle applied (no duplicated logic)

---

### 3. Test Coverage (15 checks)

#### 3.1 Unit Tests
- [ ] All public functions have unit tests
- [ ] Test coverage ≥90% for new code
- [ ] Edge cases tested (null, empty, boundary conditions)
- [ ] Error paths tested (exceptions, failures)
- [ ] Mock objects used appropriately (no real network/DB calls)

#### 3.2 Integration Tests
- [ ] API endpoints tested (request/response validation)
- [ ] Database queries tested (CRUD operations)
- [ ] Authentication flow tested end-to-end
- [ ] Third-party integrations tested (or mocked)

#### 3.3 Test Quality
- [ ] Tests have descriptive names (`shouldReturnErrorWhenTokenExpired`)
- [ ] Tests are independent (no shared mutable state)
- [ ] Tests use arrange-act-assert pattern
- [ ] Tests clean up resources (temp files, test data)

**Check:**
```kotlin
// ❌ INCOMPLETE: Missing tests
class AuthManager {
    fun authenticate(credentials: Credentials): AuthResult {
        // Implementation...
    }
    // No corresponding test file!
}

// ✅ COMPLETE: Full test coverage
class AuthManagerTest {
    @Test
    fun shouldAuthenticateValidCredentials() { /* ... */ }

    @Test
    fun shouldRejectInvalidCredentials() { /* ... */ }

    @Test
    fun shouldHandleNetworkTimeout() { /* ... */ }

    @Test
    fun shouldRefreshExpiredToken() { /* ... */ }
}
```

---

### 4. Documentation (10 checks)

#### 4.1 Code Documentation
- [ ] All public classes have KDoc/JSDoc comments
- [ ] All public functions documented (parameters, return, exceptions)
- [ ] Complex algorithms explained with inline comments
- [ ] Magic numbers replaced with named constants
- [ ] API contracts documented (preconditions, postconditions)

**Check:**
```kotlin
// ❌ INCOMPLETE: No documentation
fun calculateDiscount(price: Double, quantity: Int): Double {
    return price * quantity * 0.15  // What is 0.15?
}

// ✅ COMPLETE: Fully documented
/**
 * Calculates bulk purchase discount.
 *
 * Applies 15% discount when quantity ≥ 10 items.
 *
 * @param price Unit price per item
 * @param quantity Number of items purchased
 * @return Total price after discount
 * @throws IllegalArgumentException if price or quantity negative
 */
fun calculateDiscount(price: Double, quantity: Int): Double {
    require(price >= 0) { "Price must be non-negative" }
    require(quantity >= 0) { "Quantity must be non-negative" }

    return if (quantity >= BULK_DISCOUNT_THRESHOLD) {
        price * quantity * (1 - BULK_DISCOUNT_RATE)
    } else {
        price * quantity
    }
}

private const val BULK_DISCOUNT_THRESHOLD = 10
private const val BULK_DISCOUNT_RATE = 0.15  // 15%
```

#### 4.2 External Documentation
- [ ] README.md updated with new features
- [ ] API documentation generated (OpenAPI, Swagger)
- [ ] Migration guide written (if breaking changes)
- [ ] Changelog updated (CHANGELOG.md)
- [ ] Architecture diagrams updated (if structure changed)

---

### 5. Security (10 checks)

#### 5.1 OWASP Top 10
- [ ] Input validation on all user inputs
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (output escaping)
- [ ] CSRF protection (tokens)
- [ ] Authentication required for protected endpoints
- [ ] Authorization checks before sensitive operations
- [ ] Secrets not hardcoded (use environment variables)
- [ ] Encryption for sensitive data (passwords, tokens, PII)
- [ ] HTTPS enforced for API calls
- [ ] Dependencies scanned for vulnerabilities

**Check:**
```kotlin
// ❌ SECURITY RISK: SQL injection
fun findUser(username: String): User {
    val query = "SELECT * FROM users WHERE username = '$username'"
    return db.rawQuery(query)  // Vulnerable!
}

// ✅ SECURE: Parameterized query
fun findUser(username: String): User {
    val query = "SELECT * FROM users WHERE username = ?"
    return db.query(query, arrayOf(username))
}
```

---

### 6. Performance (8 checks)

#### 6.1 Efficiency
- [ ] No N+1 query problems (database calls in loops)
- [ ] Collections processed efficiently (avoid nested loops if possible)
- [ ] Lazy loading used where appropriate
- [ ] Caching implemented for expensive operations
- [ ] Pagination for large datasets
- [ ] Background threads for long-running tasks
- [ ] Memory leaks checked (weak references, lifecycle awareness)
- [ ] Unnecessary object allocations avoided

**Check:**
```kotlin
// ❌ PERFORMANCE ISSUE: N+1 queries
fun loadUsersWithPosts(): List<UserWithPosts> {
    val users = userDao.getAllUsers()
    return users.map { user ->
        val posts = postDao.getPostsByUserId(user.id)  // Query in loop!
        UserWithPosts(user, posts)
    }
}

// ✅ OPTIMIZED: Single query with join
fun loadUsersWithPosts(): List<UserWithPosts> {
    return userDao.getUsersWithPosts()  // Single query with JOIN
}
```

---

### 7. Accessibility (5 checks - for UI code)

- [ ] All interactive elements have content descriptions
- [ ] Color contrast ratios meet WCAG AA standards
- [ ] Focus order logical for keyboard navigation
- [ ] Screen reader announcements appropriate
- [ ] Touch targets ≥48dp × 48dp

---

## Review Process

### Step 1: Automated Checks (5 minutes)

```bash
# Run all automated checks
./gradlew check  # Lint, compile, unit tests

# Check for TODOs
grep -rn "TODO\|FIXME\|HACK\|XXX" src/ > review-todos.txt

# Check test coverage
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Security scan
./gradlew dependencyCheckAnalyze

# Code style
./gradlew ktlintCheck
```

### Step 2: Manual Review (10-15 minutes)

**Use this checklist systematically:**

```markdown
## Phase Completion Review: [Feature Name]

**Date:** 2025-11-15
**Reviewer:** AI Assistant
**Phase:** Implementation

### 1. Code Completeness
- [x] No unresolved TODOs (3 found, 2 implemented, 1 moved to backlog #456)
- [x] All functions implemented (checked 45 functions, all complete)
- [x] Error handling present (12 try-catch blocks reviewed)
- [x] Edge cases covered (null checks: 23, empty collection checks: 15)

### 2. Consistency Checks
- [x] Spec alignment verified (all 12 requirements implemented)
- [x] Naming conventions followed (camelCase for variables, PascalCase for classes)
- [x] Code style consistent (ktlint passed with 0 warnings)
- [x] Architecture respected (MVVM pattern followed)

### 3. Test Coverage
- [x] Unit tests: 94% coverage (target: 90%+) ✅
- [x] Integration tests: 8 scenarios covered
- [x] Test quality: All tests follow AAA pattern

### 4. Documentation
- [x] KDoc comments: 45/45 public functions documented (100%)
- [x] README updated with OAuth2 setup instructions
- [x] CHANGELOG updated with v1.2.0 changes

### 5. Security
- [x] OWASP Top 10 checked (no vulnerabilities found)
- [x] Input validation: 12 entry points validated
- [x] Secrets externalized (API keys moved to env vars)

### 6. Performance
- [x] No N+1 queries (checked all database operations)
- [x] Caching implemented for user profile (30s TTL)
- [x] Background threads used for network calls

### 7. Accessibility (UI only)
- [x] Content descriptions: 23/23 buttons labeled
- [x] Color contrast: All text meets WCAG AA (4.5:1+)

---

**RESULT:** ✅ PASS - Feature ready for merge

**Issues Found:** 3
1. ❌ TODO at AuthManager.kt:42 - RESOLVED (implemented token refresh)
2. ❌ TODO at ProfileScreen.kt:120 - DEFERRED (backlog #456, low priority)
3. ⚠️  Missing test for edge case - RESOLVED (added test_shouldHandleEmptyUsername)

**Recommendations:**
- Consider adding retry logic to network calls (currently fails fast)
- Profile screen could benefit from skeleton loader (UX improvement)

**Sign-off:** Ready for pull request
```

### Step 3: Fix Issues (variable time)

**For each issue found:**
1. ✅ **Fix immediately** if critical (unimplemented function, security hole)
2. ✅ **Create backlog item** if non-critical (nice-to-have feature)
3. ✅ **Document decision** if intentionally deferred (explain why in commit message)

### Step 4: Re-Review After Fixes (2-3 minutes)

- [ ] Re-run automated checks
- [ ] Verify all critical issues resolved
- [ ] Update review checklist with fixes

### Step 5: Document Review (1 minute)

**Add review summary to commit message:**
```bash
git commit -m "feat(auth): implement OAuth2 PKCE authentication

Implements OAuth2 with PKCE flow for secure mobile authentication.

Features:
- OAuth2Provider with token refresh logic
- PKCE challenge generation (SHA-256)
- Encrypted token storage (AES-256)
- Automatic token refresh on expiry

Self-Review Completed:
- Code completeness: 100% (0 unresolved TODOs)
- Test coverage: 94% (target: 90%+)
- Security: OWASP Top 10 verified
- Documentation: All public APIs documented

Resolves #123"
```

---

## Integration with IDEACODE Workflow

### In MCP Tools

**Before `ideacode_archive`:**
```typescript
await ideacode_execute_code({
  code: `
    import { validateCodeCompleteness } from '/wrappers/quality/validate-completeness.js'

    const result = await validateCodeCompleteness({
      featurePath: '.ideacode/specs/001-oauth-auth',
      checkTodos: true,
      checkTests: true,
      checkDocs: true,
      minCoverage: 90
    })

    if (!result.passed) {
      throw new Error(\`Review failed: \${result.issues.join(', ')}\`)
    }

    return result
  `
})

// Only archive if validation passed
await ideacode_archive({
  feature_dir: '.ideacode/specs/001-oauth-auth'
})
```

### In YOLO Mode

**Automatic review BEFORE final commit:**
```
User: yolo - implement OAuth2 authentication

AI: 🚀 YOLO MODE ACTIVATED

    [Implements feature...]

    🔍 MANDATORY SELF-REVIEW (pre-commit)

    Checking code completeness...
    ✅ No unresolved TODOs (3 found, all implemented)
    ✅ All functions complete (45/45)
    ✅ Error handling present (12 try-catch blocks)

    Checking test coverage...
    ✅ Unit tests: 94% coverage
    ✅ Integration tests: 8 scenarios

    Checking documentation...
    ✅ KDoc: 100% public APIs documented
    ✅ README updated

    Checking security...
    ✅ OWASP Top 10 verified
    ✅ No hardcoded secrets

    📊 REVIEW RESULT: ✅ PASS

    [Commits and pushes...]

    ✅ YOLO MODE COMPLETE
```

---

## Quality Gates

**MANDATORY before phase completion:**

| Check | Threshold | Enforcement |
|-------|-----------|-------------|
| Unresolved TODOs | 0 critical | BLOCKER |
| Test coverage | ≥90% | BLOCKER |
| Public API docs | 100% | BLOCKER |
| Security scan | 0 high/critical vulns | BLOCKER |
| Code style | 0 errors | WARNING |
| Performance | No obvious N+1 | WARNING |

**BLOCKER** = Cannot proceed to next phase
**WARNING** = Can proceed but should fix

---

## Examples

### Example 1: Incomplete Function Found

**Review Output:**
```
❌ BLOCKER: Incomplete function found

File: src/auth/TokenRefresher.kt:45
Issue: Function stub without implementation

fun refreshToken(oldToken: String): String {
    // TODO: implement token refresh logic
    throw NotImplementedError()
}

Action Required: Implement token refresh BEFORE marking phase complete
```

**Resolution:**
```kotlin
fun refreshToken(oldToken: String): String {
    val refreshToken = tokenStore.getRefreshToken(oldToken)
        ?: throw TokenNotFoundException("Refresh token not found")

    return oauth2Provider.refreshAccessToken(refreshToken)
}
```

### Example 2: Missing Tests

**Review Output:**
```
❌ BLOCKER: Test coverage below threshold

Current coverage: 78%
Target coverage: 90%

Missing tests for:
- AuthManager.kt: authenticateWithBiometric() (0% coverage)
- TokenStore.kt: cleanup() (0% coverage)
- OAuth2Provider.kt: handleAuthCallback() (50% coverage)

Action Required: Add tests for uncovered functions
```

**Resolution:**
```kotlin
@Test
fun shouldAuthenticateWithBiometric() {
    // Arrange
    val biometricPrompt = mock<BiometricPrompt>()

    // Act
    val result = authManager.authenticateWithBiometric(biometricPrompt)

    // Assert
    assertTrue(result is AuthResult.Success)
}

// ... (add missing tests)
```

### Example 3: Documentation Missing

**Review Output:**
```
⚠️  WARNING: Missing documentation

File: src/data/UserRepository.kt
Issue: 5 public functions without KDoc

Functions missing documentation:
- getUserById(id: String): User?
- updateUser(user: User): Result<Unit>
- deleteUser(id: String): Result<Unit>
- searchUsers(query: String): List<User>
- getUsersInRange(start: Int, count: Int): List<User>

Action Required: Add KDoc comments before committing
```

**Resolution:**
```kotlin
/**
 * Retrieves user by unique identifier.
 *
 * @param id Unique user identifier (UUID format)
 * @return User object if found, null otherwise
 * @throws IllegalArgumentException if id is invalid UUID
 */
fun getUserById(id: String): User? {
    require(isValidUuid(id)) { "Invalid user ID format" }
    return userDao.findById(id)
}
```

---

## Tools & Automation

### MCP Tool: Code Review

**Signature:**
```typescript
export async function reviewCode(params: {
  featurePath: string;        // Path to feature directory
  checkTodos?: boolean;       // Default: true
  checkTests?: boolean;       // Default: true
  checkDocs?: boolean;        // Default: true
  checkSecurity?: boolean;    // Default: true
  minCoverage?: number;       // Default: 90
  autoFix?: boolean;          // Default: false
}): Promise<ReviewResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { reviewCode } from '/wrappers/quality/review-code.js'

    const result = await reviewCode({
      featurePath: '.ideacode/specs/001-oauth-auth',
      checkTodos: true,
      checkTests: true,
      checkDocs: true,
      checkSecurity: true,
      minCoverage: 90,
      autoFix: false
    })

    return result
  `
})
```

**Output:**
```json
{
  "passed": false,
  "blockers": 2,
  "warnings": 5,
  "issues": [
    {
      "severity": "BLOCKER",
      "category": "completeness",
      "file": "src/auth/TokenRefresher.kt",
      "line": 45,
      "message": "Unimplemented function: refreshToken()",
      "suggestion": "Implement token refresh logic"
    },
    {
      "severity": "BLOCKER",
      "category": "testing",
      "file": "src/auth/AuthManager.kt",
      "line": 120,
      "message": "Test coverage: 78% (target: 90%)",
      "suggestion": "Add tests for authenticateWithBiometric()"
    }
  ],
  "summary": {
    "todos": { "total": 3, "critical": 0, "resolved": 3 },
    "coverage": { "current": 78, "target": 90, "passed": false },
    "documentation": { "documented": 40, "total": 45, "percent": 89 },
    "security": { "vulnerabilities": 0, "passed": true }
  }
}
```

---

## References

- **Protocol-Zero-Tolerance-Pre-Code.md** - Quality gates
- **Protocol-Test-Driven-Development.md** - TDD workflow
- **Protocol-Phase-Completion-Workflow.md** - Phase transitions
- **OWASP Top 10:** https://owasp.org/www-project-top-ten/

---

## Changelog

### v1.0 (2025-11-15)
- Initial protocol creation
- Defined 7 review categories (completeness, consistency, tests, docs, security, performance, accessibility)
- Created 98-point checklist
- Added MCP integration (reviewCode tool)
- Defined quality gates (blockers vs warnings)
- Integrated with IDEACODE workflow

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary

---

**IDEACODE v8.4** - Mandatory code review for zero-defect delivery
