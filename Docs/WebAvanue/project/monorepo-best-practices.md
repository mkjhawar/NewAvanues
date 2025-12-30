# Monorepo Best Practices - Multi-User, Multi-App Environment

**Repository:** MainAvanues Monorepo
**Users:** Multiple developers working concurrently
**Scope:** Multiple apps, modules, and libraries
**Version:** 1.0
**Date:** 2025-11-25

---

## 📋 Table of Contents

1. [Branching Strategy](#branching-strategy)
2. [Git Workflow](#git-workflow)
3. [Code Ownership & CODEOWNERS](#code-ownership--codeowners)
4. [Dependency Management](#dependency-management)
5. [Build & CI/CD Strategy](#build--cicd-strategy)
6. [Merge Conflict Prevention](#merge-conflict-prevention)
7. [Code Review Process](#code-review-process)
8. [Versioning Strategy](#versioning-strategy)
9. [Testing Strategy](#testing-strategy)
10. [Documentation](#documentation)
11. [Tooling & Automation](#tooling--automation)
12. [Communication Protocols](#communication-protocols)

---

## 🌳 Branching Strategy

### Branch Hierarchy

```
main (protected)
├── develop (integration branch)
├── feature/{module}/{feature-name}
├── bugfix/{module}/{bug-name}
├── hotfix/{version}/{fix-name}
└── release/{version}
```

### Branch Types

#### 1. **Main Branch** (Protected)
- **Purpose:** Production-ready code
- **Protection:**
  - No direct commits
  - Requires 2+ approvals
  - CI must pass
  - Up-to-date with base branch
- **Merge from:** `release/*` or `hotfix/*` only

```bash
# Main branch rules
- Branch protection: ON
- Required approvals: 2+
- Require CI to pass: YES
- Allow force push: NO
- Allow deletion: NO
```

#### 2. **Develop Branch** (Semi-Protected)
- **Purpose:** Integration branch for features
- **Protection:**
  - No direct commits (except urgent fixes)
  - Requires 1+ approval
  - CI must pass
- **Merge from:** `feature/*`, `bugfix/*`

#### 3. **Feature Branches** (Module-Specific)
- **Naming:** `feature/{module}/{feature-description}`
- **Examples:**
  - `feature/webavanue/webxr-support`
  - `feature/voiceos/voice-recognition`
  - `feature/shared-ui/dark-mode`
- **Lifespan:** < 2 weeks, < 50 commits
- **Base:** `develop`
- **Merge to:** `develop`

```bash
# Create feature branch
git checkout develop
git pull origin develop
git checkout -b feature/webavanue/tab-groups

# Work on feature...

# Merge back to develop via PR
```

#### 4. **Module Development Branches** (For Large Modules)
- **Naming:** `{ModuleName}-Develop` (e.g., `WebAvanue-Develop`)
- **Purpose:** Isolated development for major modules
- **When to use:**
  - Module under active development by multiple devs
  - Major refactoring or migration
  - Want to isolate from main develop branch
- **Merge to:** `develop` after stabilization
- **Lifespan:** Weeks to months

**Example: WebAvanue-Develop**
```
develop
└── WebAvanue-Develop (module branch)
    ├── feature/webavanue/tab-persistence
    ├── feature/webavanue/voice-commands
    └── bugfix/webavanue/crash-on-home
```

---

## 🔄 Git Workflow

### Multi-User Concurrent Development

#### Daily Workflow

```bash
# 1. Start of day - Update your branch
git checkout feature/webavanue/my-feature
git fetch origin
git rebase origin/develop

# 2. Work on your changes
# ... make changes ...
git add .
git commit -m "feat(webavanue): add tab persistence"

# 3. Push regularly (for backup and collaboration)
git push origin feature/webavanue/my-feature

# 4. Before creating PR - rebase on latest develop
git fetch origin
git rebase origin/develop
git push --force-with-lease origin feature/webavanue/my-feature

# 5. Create Pull Request
# Via GitLab UI or gh CLI
```

#### Rebase vs Merge

**Use Rebase for:**
- Updating feature branches with latest develop
- Cleaning up commit history before PR
- Keeping linear history

**Use Merge for:**
- Merging PRs into develop/main
- Preserving feature branch history
- Creating release branches

```bash
# Rebase feature on develop (daily)
git checkout feature/webavanue/my-feature
git rebase origin/develop

# Merge PR into develop (via GitLab - squash merge)
# This preserves the PR as a single commit
```

---

## 👥 Code Ownership & CODEOWNERS

### CODEOWNERS File

Create `.gitlab/CODEOWNERS` to automatically request reviews from module owners:

```
# CODEOWNERS - MainAvanues Monorepo
# Syntax: path @username @team

# Global defaults
* @manoj_mbpm14

# Android Apps
/android/apps/webavanue/ @webavanue-team @manoj_mbpm14
/android/apps/voiceos/ @voiceos-team
/android/apps/avaconnect/ @avaconnect-team

# Common Libraries
/common/libs/webavanue/ @webavanue-team @manoj_mbpm14
/common/libs/voiceos/ @voiceos-team
/common/libs/shared-ui/ @ui-team @design-team

# Shared Infrastructure
/common/libs/network/ @backend-team @infra-team
/common/libs/security/ @security-team @backend-team
/common/libs/database/ @backend-team

# Build Configuration
/gradle/ @build-team @infra-team
/buildSrc/ @build-team
*.gradle.kts @build-team

# CI/CD
/.gitlab-ci.yml @devops-team @infra-team
/scripts/ @devops-team

# Documentation
/docs/ @tech-writers @manoj_mbpm14
```

### Code Ownership Benefits

1. **Automatic Review Assignment:** GitLab assigns reviewers based on changed files
2. **Expertise Routing:** Changes go to people who know the code
3. **Accountability:** Clear ownership of each module
4. **Review Load Balancing:** Distributes reviews across teams

---

## 📦 Dependency Management

### Module Dependencies

#### 1. **Define Clear Boundaries**

```kotlin
// settings.gradle.kts - Clear module structure
rootProject.name = "MainAvanues"

// Apps (depend on libs)
include(":android:apps:webavanue")
include(":android:apps:voiceos")
include(":android:apps:avaconnect")

// Libraries (minimal cross-dependencies)
include(":common:libs:webavanue:universal")
include(":common:libs:webavanue:coredata")
include(":common:libs:voiceos:core")
include(":common:libs:shared-ui")
include(":common:libs:network")

// Dependency direction: Apps → Libs → Core
// NEVER: Core → Libs or Libs → Apps
```

#### 2. **Dependency Graph Rules**

**✅ ALLOWED:**
```
android/apps/webavanue → common/libs/webavanue/universal
common/libs/webavanue/universal → common/libs/webavanue/coredata
common/libs/webavanue/coredata → common/libs/network
```

**❌ FORBIDDEN:**
```
common/libs/network → common/libs/webavanue/coredata  # Core depending on feature
android/apps/webavanue → android/apps/voiceos          # App depending on app
```

#### 3. **Version Catalogs** (Gradle 7.0+)

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.21"
compose = "1.5.4"
coroutines = "1.7.3"
ktor = "2.3.5"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

# Compose
compose-runtime = { module = "androidx.compose.runtime:runtime", version.ref = "compose" }
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "compose" }

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }

[bundles]
compose = ["compose-runtime", "compose-material3", "compose-ui"]
ktor = ["ktor-client-core", "ktor-client-cio"]

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

**Benefits:**
- Single source of truth for versions
- Type-safe dependency declarations
- Easy version updates across all modules

#### 4. **Shared Dependencies**

Create `buildSrc/src/main/kotlin/Dependencies.kt`:

```kotlin
object Versions {
    const val kotlin = "1.9.21"
    const val compose = "1.5.4"
    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35
}

object Libs {
    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    }

    object Compose {
        const val runtime = "androidx.compose.runtime:runtime:${Versions.compose}"
        const val material3 = "androidx.compose.material3:material3:${Versions.compose}"
    }
}
```

---

## 🏗️ Build & CI/CD Strategy

### Selective Building (Build Only What Changed)

#### 1. **Module-Specific CI Pipelines**

```yaml
# .gitlab-ci.yml
stages:
  - detect-changes
  - build
  - test
  - deploy

# Detect what changed
detect-changes:
  stage: detect-changes
  script:
    - |
      # Get changed files in this MR
      CHANGED_FILES=$(git diff --name-only $CI_MERGE_REQUEST_DIFF_BASE_SHA $CI_COMMIT_SHA)

      # Detect affected modules
      echo "$CHANGED_FILES" | grep -q "^android/apps/webavanue/" && echo "webavanue" >> affected_modules.txt
      echo "$CHANGED_FILES" | grep -q "^android/apps/voiceos/" && echo "voiceos" >> affected_modules.txt
      echo "$CHANGED_FILES" | grep -q "^common/libs/" && echo "all_apps" >> affected_modules.txt
  artifacts:
    paths:
      - affected_modules.txt

# Build only affected modules
build-webavanue:
  stage: build
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
      changes:
        - android/apps/webavanue/**/*
        - common/libs/webavanue/**/*
  script:
    - ./gradlew :android:apps:webavanue:assembleDebug

build-voiceos:
  stage: build
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
      changes:
        - android/apps/voiceos/**/*
        - common/libs/voiceos/**/*
  script:
    - ./gradlew :android:apps:voiceos:assembleDebug

# Test only affected modules
test-webavanue:
  stage: test
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
      changes:
        - android/apps/webavanue/**/*
        - common/libs/webavanue/**/*
  script:
    - ./gradlew :android:apps:webavanue:test
```

#### 2. **Local Selective Building**

```bash
# Build only WebAvanue
./gradlew :android:apps:webavanue:assembleDebug

# Build only changed modules (use gradle --dry-run to detect)
./gradlew build --dry-run | grep "Task.*webavanue" | cut -d: -f2-3

# Test only WebAvanue
./gradlew :common:libs:webavanue:universal:test

# Build all that depend on a library
./gradlew :common:libs:network:assemble :common:libs:network:dependentTasks
```

#### 3. **Gradle Build Cache**

```kotlin
// gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true

# Build cache configuration
org.gradle.caching.debug=false
```

```kotlin
// settings.gradle.kts
buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }

    // Remote cache (optional - for team)
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/cache/")
        credentials {
            username = System.getenv("CACHE_USERNAME")
            password = System.getenv("CACHE_PASSWORD")
        }
        isPush = System.getenv("CI") != null
    }
}
```

---

## 🚧 Merge Conflict Prevention

### Strategies for Concurrent Development

#### 1. **Communicate Before Large Changes**

```markdown
# Team Communication Protocol

Before starting work that affects multiple modules:
1. Post in #dev-coordination Slack channel
2. Tag relevant module owners
3. Create a design doc in /docs/design/
4. Get feedback before implementation

Example:
"🔧 Planning to refactor common/libs/network/ to add retry logic.
This affects all apps. Design doc: docs/design/network-retry.md
@webavanue-team @voiceos-team - please review by EOD"
```

#### 2. **Small, Frequent Commits**

```bash
# ❌ BAD: One huge commit after 2 weeks
git commit -m "feat: implement entire feature" # 500 files changed

# ✅ GOOD: Small, atomic commits
git commit -m "feat(webavanue): add Tab domain model"
git commit -m "feat(webavanue): add TabRepository interface"
git commit -m "feat(webavanue): implement TabRepositoryImpl"
git commit -m "test(webavanue): add TabRepository tests"
```

#### 3. **Module Isolation**

```kotlin
// Each module has clear boundaries
android/apps/webavanue/
  - Only WebAvanue-specific code
  - Depends on common/libs/webavanue/*
  - No direct dependencies on other apps

common/libs/webavanue/
  - WebAvanue business logic
  - No app-specific code
  - Can be used by multiple apps
```

#### 4. **Feature Flags for Large Changes**

```kotlin
// common/libs/shared/FeatureFlags.kt
object FeatureFlags {
    const val ENABLE_NEW_NETWORK_LAYER = false
    const val ENABLE_WEBXR = true
    const val ENABLE_VOICE_COMMANDS = true
}

// In code
if (FeatureFlags.ENABLE_NEW_NETWORK_LAYER) {
    // Use new implementation
    newNetworkClient.fetch()
} else {
    // Use old implementation
    oldNetworkClient.fetch()
}
```

**Benefits:**
- Merge incomplete features to develop without breaking
- Enable/disable features per environment
- A/B testing in production

#### 5. **Rebase Frequently**

```bash
# Every morning - rebase on latest develop
git fetch origin
git rebase origin/develop

# Before creating PR
git fetch origin
git rebase origin/develop
git push --force-with-lease origin feature/webavanue/my-feature
```

#### 6. **Lock Files for Critical Sections**

Create `.gitlab/LOCKS.md` to coordinate exclusive access:

```markdown
# Active Locks

## common/libs/network/ - Network Module
**Locked by:** @developer1
**Reason:** Major refactoring for retry logic
**Duration:** 2025-11-25 to 2025-11-27
**Contact:** DM @developer1 before making changes

## gradle/libs.versions.toml - Version Catalog
**Locked by:** @developer2
**Reason:** Upgrading Kotlin to 2.0
**Duration:** 2025-11-25 to 2025-11-26
**Contact:** Wait for PR #123 to merge
```

---

## 👀 Code Review Process

### Review Requirements by Impact

#### 1. **App-Specific Changes** (Low Risk)
- **Scope:** Changes only in `android/apps/{app}/`
- **Required Reviewers:** 1 from app team
- **Review Time:** < 24 hours
- **CI Requirements:** Tests pass for that app

#### 2. **Library Changes** (Medium Risk)
- **Scope:** Changes in `common/libs/{module}/`
- **Required Reviewers:** 2 (1 from lib team, 1 from app team)
- **Review Time:** < 48 hours
- **CI Requirements:** Tests pass for lib + all dependent apps

#### 3. **Shared Infrastructure Changes** (High Risk)
- **Scope:** Changes in `common/libs/network/`, `security/`, `database/`
- **Required Reviewers:** 3+ (2 from infra team, 1+ from affected app teams)
- **Review Time:** < 72 hours
- **CI Requirements:** Full test suite passes

#### 4. **Build System Changes** (Critical Risk)
- **Scope:** Changes in `gradle/`, `buildSrc/`, `.gitlab-ci.yml`
- **Required Reviewers:** 3+ (build team lead + 2 module owners)
- **Review Time:** < 72 hours
- **Testing:** Manual verification on clean checkout

### Review Checklist

```markdown
## Code Review Checklist

### Functionality
- [ ] Does the code do what it's supposed to do?
- [ ] Are edge cases handled?
- [ ] Is error handling appropriate?

### Testing
- [ ] Are there unit tests for new code?
- [ ] Are there integration tests if needed?
- [ ] Do all tests pass?
- [ ] Is test coverage >= 90% for critical paths?

### Design
- [ ] Is the code in the right module?
- [ ] Are dependencies appropriate (no circular deps)?
- [ ] Is the API design clean and intuitive?

### Code Quality
- [ ] Is the code readable and maintainable?
- [ ] Are naming conventions followed?
- [ ] Are comments present for complex logic?
- [ ] No code smells (long methods, deep nesting, etc.)?

### Security
- [ ] No hardcoded secrets or credentials?
- [ ] Input validation for external data?
- [ ] No SQL injection vulnerabilities?
- [ ] No XSS vulnerabilities?

### Performance
- [ ] No obvious performance issues?
- [ ] Appropriate data structures used?
- [ ] No N+1 queries?

### Documentation
- [ ] Public APIs documented?
- [ ] README updated if needed?
- [ ] Migration guide if breaking change?
```

---

## 📌 Versioning Strategy

### Semantic Versioning for Modules

Each module has independent versioning:

```
common/libs/webavanue/universal/
  version: 2.1.0

common/libs/network/
  version: 1.3.2

android/apps/webavanue/
  versionName: 1.0.0
  versionCode: 5
```

### Version Format

**Libraries:** `MAJOR.MINOR.PATCH`
- **MAJOR:** Breaking API changes
- **MINOR:** New features, backward compatible
- **PATCH:** Bug fixes, backward compatible

**Apps:** `MAJOR.MINOR.PATCH (BUILD)`
- **MAJOR:** Major release with new features
- **MINOR:** Feature updates
- **PATCH:** Bug fixes
- **BUILD:** CI build number (auto-increment)

### Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
# Internal modules
webavanue-universal = "2.1.0"
webavanue-coredata = "1.5.0"
voiceos-core = "3.0.0"
shared-ui = "1.2.0"
network = "1.3.2"

# External dependencies
kotlin = "1.9.21"
compose = "1.5.4"
```

### Breaking Changes

When making breaking changes to shared libraries:

1. **Announce** in team channels (7 days advance notice)
2. **Create migration guide** in `docs/migrations/`
3. **Deprecate old API** first (if possible)
4. **Bump major version** of the library
5. **Update all consumers** in the same PR or coordinated PRs

```kotlin
// Step 1: Deprecate old API
@Deprecated(
    message = "Use fetchData() instead",
    replaceWith = ReplaceWith("fetchData()")
)
fun getData(): Data = fetchData()

// Step 2: Add new API
fun fetchData(): Data { ... }

// Step 3: After 1-2 releases, remove deprecated API
// (with major version bump)
```

---

## 🧪 Testing Strategy

### Test Pyramid for Monorepo

```
           E2E Tests (5%)
          /              \
     Integration Tests (15%)
    /                        \
   Unit Tests (80%)
```

#### 1. **Unit Tests** (80% of tests)
- **Scope:** Individual classes/functions
- **Location:** `src/test/` in each module
- **Run frequency:** On every commit
- **Speed:** Fast (< 1 minute for full suite)

```bash
# Run unit tests for specific module
./gradlew :common:libs:webavanue:universal:test

# Run all unit tests
./gradlew test
```

#### 2. **Integration Tests** (15% of tests)
- **Scope:** Multiple modules working together
- **Location:** `src/test/` in dependent modules
- **Run frequency:** On PR creation
- **Speed:** Medium (5-10 minutes)

```bash
# Run integration tests for app
./gradlew :android:apps:webavanue:testDebugUnitTest
```

#### 3. **E2E Tests** (5% of tests)
- **Scope:** Full user flows
- **Location:** `src/androidTest/` in apps
- **Run frequency:** Before merge to main
- **Speed:** Slow (20-30 minutes)

```bash
# Run E2E tests on emulator
./gradlew :android:apps:webavanue:connectedDebugAndroidTest
```

### Test Isolation

**Each module tests independently:**

```kotlin
// common/libs/webavanue/universal/src/test/kotlin/
class TabRepositoryTest {
    // Test only TabRepository
    // Mock external dependencies

    @Test
    fun `should save tab to database`() {
        val mockDatabase = mockk<Database>()
        val repository = TabRepositoryImpl(mockDatabase)

        repository.saveTab(tab)

        verify { mockDatabase.insert(tab) }
    }
}
```

### Shared Test Utilities

```kotlin
// common/libs/test-utils/
object TestFixtures {
    fun createTab(id: String = "tab1") = Tab(
        id = id,
        url = "https://example.com",
        title = "Test Tab"
    )
}

// Used across modules
class TabRepositoryTest {
    @Test
    fun test() {
        val tab = TestFixtures.createTab()
        // ...
    }
}
```

---

## 📚 Documentation

### Documentation Structure

```
docs/
├── architecture/
│   ├── MONOREPO-STRUCTURE.md
│   ├── MODULE-DEPENDENCIES.md
│   └── DESIGN-DECISIONS.md
├── modules/
│   ├── webavanue/
│   │   ├── README.md
│   │   ├── API.md
│   │   └── MIGRATION.md
│   ├── voiceos/
│   └── shared-ui/
├── development/
│   ├── SETUP.md
│   ├── BUILD.md
│   ├── TESTING.md
│   └── CONTRIBUTING.md
├── operations/
│   ├── DEPLOYMENT.md
│   ├── MONITORING.md
│   └── TROUBLESHOOTING.md
└── design/
    ├── network-retry.md
    └── voice-commands.md
```

### Module README Template

```markdown
# Module Name

## Overview
Brief description of what this module does.

## Dependencies
- `common/libs/network` - For API calls
- `common/libs/database` - For local storage

## Public API
### Classes
- `TabRepository` - Manages browser tabs
- `Tab` - Domain model for browser tab

### Functions
- `fun saveTab(tab: Tab)` - Saves tab to database
- `fun getTabs(): Flow<List<Tab>>` - Gets all tabs as flow

## Usage Example
```kotlin
val repository = TabRepositoryImpl(database)
repository.saveTab(tab)
```

## Testing
```bash
./gradlew :common:libs:webavanue:universal:test
```

## Maintainers
- @webavanue-team
- @manoj_mbpm14
```

---

## 🛠️ Tooling & Automation

### Pre-commit Hooks

Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Pre-commit hook for MainAvanues monorepo

echo "🔍 Running pre-commit checks..."

# 1. Run ktlint (Kotlin linter)
echo "  → Checking code style..."
./gradlew ktlintCheck || {
    echo "❌ Code style issues found. Run './gradlew ktlintFormat' to fix."
    exit 1
}

# 2. Run unit tests for changed modules
echo "  → Running unit tests..."
CHANGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

if echo "$CHANGED_FILES" | grep -q "^android/apps/webavanue/"; then
    ./gradlew :android:apps:webavanue:testDebugUnitTest || {
        echo "❌ WebAvanue tests failed."
        exit 1
    }
fi

# 3. Check for TODOs in committed code
echo "  → Checking for TODOs..."
if echo "$CHANGED_FILES" | xargs grep -n "TODO" | grep -v "// TODO:"; then
    echo "⚠️  Warning: Found TODO comments without ticket reference."
    echo "   Use '// TODO(JIRA-123): description' format."
fi

# 4. Check for secrets
echo "  → Checking for secrets..."
if echo "$CHANGED_FILES" | xargs grep -i "api_key\|password\|secret" | grep -v "test"; then
    echo "❌ Possible secrets found in code!"
    exit 1
fi

echo "✅ Pre-commit checks passed!"
```

### Pre-push Hooks

Create `.git/hooks/pre-push`:

```bash
#!/bin/bash
# Pre-push hook for MainAvanues monorepo

echo "🚀 Running pre-push checks..."

# Get current branch
CURRENT_BRANCH=$(git symbolic-ref --short HEAD)

# Prevent push to main directly
if [ "$CURRENT_BRANCH" = "main" ]; then
    echo "❌ Direct push to main is not allowed!"
    echo "   Create a PR instead."
    exit 1
fi

# Run full test suite before push
echo "  → Running full test suite..."
./gradlew test || {
    echo "❌ Tests failed. Fix tests before pushing."
    exit 1
}

echo "✅ Pre-push checks passed!"
```

### Install hooks:

```bash
# Make hooks executable
chmod +x .git/hooks/pre-commit
chmod +x .git/hooks/pre-push

# Or use a hook manager like Husky (for team distribution)
npm install -D husky
npx husky install
npx husky add .husky/pre-commit "./gradlew ktlintCheck"
```

---

## 💬 Communication Protocols

### Daily Standups (Per Module)

Each module team has daily standups:

**Format:**
1. What did I do yesterday?
2. What will I do today?
3. Any blockers?
4. **Cross-module dependencies?** (new)

**Example:**
```
Dev 1: Working on WebAvanue tab persistence. Need Network module's
       new retry API. @network-team - when will that land?

Dev 2: Fixing VoiceOS crash. Need to update common/libs/security.
       Checking with @security-team first.
```

### Weekly Cross-Module Sync

**Purpose:** Coordinate changes affecting multiple modules

**Attendees:** Module leads + architecture team

**Agenda:**
1. Upcoming breaking changes
2. Shared library updates
3. Build system changes
4. Dependency updates

### Slack Channels

```
#dev-general         - General development discussion
#dev-coordination    - Cross-module coordination
#webavanue-dev       - WebAvanue module
#voiceos-dev         - VoiceOS module
#shared-libs         - Shared library changes
#build-system        - Build/CI/CD discussions
#code-reviews        - Review requests
#merge-conflicts     - Conflict resolution help
```

### Notification Triggers

**Critical Changes (Notify ALL):**
- Build system changes
- Gradle version upgrades
- Shared library breaking changes
- Security vulnerabilities

**Module Changes (Notify MODULE TEAM):**
- Feature additions to module
- Bug fixes in module
- API changes in module

### RFC Process (Request for Comments)

For major changes, create an RFC:

```markdown
# RFC: Add Retry Logic to Network Module

**Author:** @developer1
**Date:** 2025-11-25
**Status:** Draft / Under Review / Accepted / Rejected

## Summary
Add exponential backoff retry logic to network module.

## Motivation
Current network client fails immediately on timeout.
Users experience failures that could be retried.

## Detailed Design
### API Changes
```kotlin
// Before
interface NetworkClient {
    suspend fun fetch(url: String): Response
}

// After
interface NetworkClient {
    suspend fun fetch(
        url: String,
        retryConfig: RetryConfig = RetryConfig.DEFAULT
    ): Response
}
```

### Implementation
- Exponential backoff: 1s, 2s, 4s, 8s
- Max retries: 3
- Retry on: Timeout, 5xx errors
- No retry on: 4xx errors

## Impact Analysis
**Affected Modules:**
- ✅ common/libs/webavanue (uses network)
- ✅ common/libs/voiceos (uses network)
- ✅ android/apps/webavanue
- ✅ android/apps/voiceos

**Breaking Changes:** None (backward compatible)

## Migration Guide
No migration needed. Existing code continues to work.
To opt into retry:
```kotlin
networkClient.fetch(url, retryConfig = RetryConfig(maxRetries = 3))
```

## Alternatives Considered
1. Manual retry in each module (rejected - code duplication)
2. Ktor's built-in retry (rejected - not flexible enough)

## Timeline
- Week 1: Implementation + tests
- Week 2: Code review + integration
- Week 3: Deploy to production

## Questions & Feedback
(Comment below)
```

**Post RFC in:** `docs/rfcs/YYYY-MM-DD-retry-logic.md`

**Notify:** Post in #dev-coordination with @mentions

**Review Period:** 1 week for feedback

---

## 📋 Quick Reference - Common Scenarios

### Scenario 1: Starting New Feature

```bash
# 1. Create feature branch
git checkout develop
git pull origin develop
git checkout -b feature/webavanue/tab-groups

# 2. Make changes
# ... code ...

# 3. Commit frequently
git add .
git commit -m "feat(webavanue): add TabGroup domain model"

# 4. Push to backup
git push origin feature/webavanue/tab-groups

# 5. Before PR - rebase on latest
git fetch origin
git rebase origin/develop
git push --force-with-lease origin feature/webavanue/tab-groups

# 6. Create PR in GitLab
# - Title: "feat(webavanue): Add tab groups feature"
# - Description: Link to design doc, screenshots, etc.
# - Reviewers: Auto-assigned via CODEOWNERS
```

### Scenario 2: Fixing Merge Conflicts

```bash
# 1. Fetch latest
git fetch origin

# 2. Rebase on develop
git rebase origin/develop

# 3. Conflicts detected
# CONFLICT (content): Merge conflict in android/apps/webavanue/...

# 4. Resolve conflicts manually
# Edit conflicted files
# Look for <<<<<<< HEAD markers

# 5. Mark as resolved
git add <conflicted-file>

# 6. Continue rebase
git rebase --continue

# 7. Force push (with safety)
git push --force-with-lease origin feature/webavanue/my-feature
```

### Scenario 3: Updating Shared Library

```bash
# 1. Create branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/network/add-retry

# 2. Update library
# ... make changes to common/libs/network/ ...

# 3. Update version
# Edit gradle/libs.versions.toml
# network = "1.3.2" -> "1.4.0"

# 4. Write migration guide
# Create docs/migrations/network-1.4.0.md

# 5. Post RFC
# Post in #shared-libs: "Planning to add retry logic to network module"

# 6. Update all consumers in the SAME PR
# Update android/apps/webavanue to use new API
# Update android/apps/voiceos to use new API

# 7. Create PR
# Title: "feat(network): add retry logic with exponential backoff"
# Link to RFC and migration guide
# Tag all affected module owners
```

### Scenario 4: Hotfix Production Bug

```bash
# 1. Branch from main (not develop!)
git checkout main
git pull origin main
git checkout -b hotfix/1.0.1/fix-crash

# 2. Fix the bug (minimal changes only)
# ... fix ...

# 3. Commit
git commit -m "fix(webavanue): prevent crash on null webview"

# 4. Create PR to main
# Expedited review process
# Requires 2 approvals

# 5. After merge to main, also merge to develop
git checkout develop
git merge main
git push origin develop
```

---

## ✅ Monorepo Health Checklist

### Daily
- [ ] Rebase feature branches on develop
- [ ] Push work-in-progress for backup
- [ ] Respond to code review comments

### Weekly
- [ ] Attend cross-module sync meeting
- [ ] Review LOCKS.md for coordination needs
- [ ] Update module documentation

### Monthly
- [ ] Update dependencies in version catalog
- [ ] Review and close stale branches
- [ ] Update migration guides
- [ ] Archive old documentation

### Quarterly
- [ ] Review CODEOWNERS for accuracy
- [ ] Update monorepo best practices
- [ ] Analyze build times and optimize
- [ ] Review dependency graph

---

## 🎯 Success Metrics

Track these metrics to ensure monorepo health:

1. **Build Times**
   - Target: < 2 minutes for incremental builds
   - Target: < 10 minutes for clean builds

2. **PR Merge Time**
   - Target: < 24 hours for app changes
   - Target: < 48 hours for library changes

3. **Merge Conflict Rate**
   - Target: < 10% of PRs have conflicts
   - Action: If >20%, review branching strategy

4. **Test Reliability**
   - Target: < 1% flaky test rate
   - Action: If >5%, dedicate sprint to test stability

5. **CI/CD Success Rate**
   - Target: >95% of builds pass
   - Action: If <90%, investigate CI issues

---

**Last Updated:** 2025-11-25
**Maintainers:** @manoj_mbpm14, @architecture-team
**Version:** 1.0
