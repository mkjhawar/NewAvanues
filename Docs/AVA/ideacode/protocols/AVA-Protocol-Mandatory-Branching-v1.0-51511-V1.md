# Protocol: Mandatory Branching Strategy v1.0

**Version:** 1.0
**Status:** Active
**Effective Date:** 2025-11-15
**Framework Version:** 8.4

---

## 🚨 MANDATORY: Branch-Based Development

**CRITICAL - AI MUST ENFORCE - NOT OPTIONAL:**

ALL development MUST happen on feature branches. Direct commits to `main` or `master` are PROHIBITED except for:
- Hotfixes (emergency production fixes)
- Automated version bumps
- Automated documentation updates

---

## Purpose

**Problem:** Without proper branching strategy:
- ❌ Broken `main` branch (incomplete features pushed)
- ❌ Lost work (conflicts, force pushes)
- ❌ Unclear history (what changed when?)
- ❌ Difficult rollbacks (no clean commit points)
- ❌ Team confusion (who's working on what?)

**Solution:** Structured branching with hierarchical organization and mandatory workflows

---

## Branch Hierarchy

### Primary Branches (Protected)

```
main                    # ⭐ Production-ready code (PROTECTED)
  ├─ release/*          # Release candidates (PROTECTED)
  └─ develop            # Integration branch (PROTECTED, if using GitFlow)
```

**Protection Rules:**
- ✅ Require pull request reviews (1+ approvers)
- ✅ Require status checks to pass (CI/CD)
- ✅ No force pushes
- ✅ No deletions
- ✅ Require linear history (no merge commits, squash only)

### Working Branches (Hierarchical)

```
feature/               # ⭐ New features
  ├─ auth/
  │   ├─ oauth2-pkce
  │   ├─ biometric
  │   └─ jwt-refresh
  ├─ ui/
  │   ├─ dark-mode
  │   ├─ navigation-v2
  │   └─ onboarding
  └─ voice/
      ├─ recognition-v2
      ├─ dsl-parser
      └─ offline-mode

bugfix/                # Bug fixes
  ├─ login-timeout
  ├─ memory-leak
  └─ crash-on-startup

refactor/              # Code refactoring
  ├─ database-layer
  ├─ api-client
  └─ dependency-injection

experiment/            # Experimental work (can be deleted)
  ├─ graphql-migration
  ├─ kotlin-native
  └─ ai-voice-synthesis

hotfix/                # Emergency production fixes
  ├─ security-patch-001
  └─ critical-crash-fix

chore/                 # Maintenance tasks
  ├─ dependency-updates
  ├─ ci-optimization
  └─ documentation
```

### Naming Conventions

**Format:** `{type}/{category}/{description}`

**Rules:**
1. ✅ Use lowercase with hyphens: `feature/auth/oauth2-pkce`
2. ✅ Max 2-3 levels deep: `feature/ui/dark-mode` ✅, `feature/ui/settings/dark-mode/advanced` ❌
3. ✅ Descriptive names (2-4 words): `feature/user-profile`, `bugfix/login-timeout`
4. ❌ No special characters: `feature/user@auth` ❌
5. ❌ No underscores: `feature/user_auth` ❌

**Examples:**
```bash
✅ CORRECT:
feature/auth/oauth2-pkce
feature/ui/dark-mode
bugfix/login-timeout
refactor/database-layer
experiment/kotlin-native

❌ WRONG:
Feature/Auth/OAuth2-PKCE         # Mixed case
feature/auth/oauth2_pkce         # Underscores
my-feature                       # No category
feature/auth/oauth2/pkce/google  # Too deep (4 levels)
```

---

## Workflow Patterns

### Pattern 1: Feature Development (GitFlow Style)

**Use when:** Building new features, team collaboration

```
main (protected)
  │
  ├─ feature/auth/oauth2-pkce
  │   │
  │   ├─ Develop: Implement OAuth2 with PKCE
  │   ├─ Test: Write unit + integration tests
  │   ├─ Document: Update API docs
  │   │
  │   └─ Pull Request → main
  │       ├─ Code review (2 approvers)
  │       ├─ CI/CD checks pass
  │       └─ Squash merge → main
  │
  └─ main updated with feature ✅
```

**Commands:**
```bash
# 1. Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/auth/oauth2-pkce

# 2. Develop feature
# ... make changes ...
git add .
git commit -m "feat(auth): implement OAuth2 PKCE flow"

# 3. Push to remote
git push -u origin feature/auth/oauth2-pkce

# 4. Create Pull Request
gh pr create --title "feat(auth): OAuth2 PKCE authentication" \
             --body "Implements OAuth2 with PKCE flow for secure mobile auth"

# 5. After approval, squash merge
gh pr merge --squash --delete-branch
```

### Pattern 2: Hotfix (Emergency Fix)

**Use when:** Critical production bug, security vulnerability

```
main (v1.5.0 - production)
  │
  ├─ hotfix/security-patch-001
  │   │
  │   ├─ Fix: Patch SQL injection vulnerability
  │   ├─ Test: Security regression tests
  │   │
  │   └─ Pull Request → main (HIGH PRIORITY)
  │       ├─ Fast-tracked review (1 approver)
  │       ├─ Security checks pass
  │       └─ Squash merge → main
  │
  └─ main (v1.5.1) deployed immediately ✅
```

**Commands:**
```bash
# 1. Create hotfix branch from main
git checkout main
git pull origin main
git checkout -b hotfix/security-patch-001

# 2. Fix critical issue
# ... make changes ...
git add .
git commit -m "fix(security): patch SQL injection in login endpoint"

# 3. Push and create URGENT PR
git push -u origin hotfix/security-patch-001
gh pr create --title "URGENT: Security patch for SQL injection" \
             --label "hotfix,security" \
             --assignee @reviewer

# 4. Fast-track merge
gh pr merge --squash --delete-branch

# 5. Tag new version
git tag v1.5.1
git push origin v1.5.1
```

### Pattern 3: Refactoring (Safe Restructuring)

**Use when:** Improving code structure, no behavior changes

```
main
  │
  ├─ refactor/database-layer
  │   │
  │   ├─ Refactor: Extract repository pattern
  │   ├─ Test: All existing tests still pass
  │   ├─ Document: Architecture decision record
  │   │
  │   └─ Pull Request → main
  │       ├─ Code review (focus on no behavior change)
  │       ├─ All tests pass (100% coverage)
  │       └─ Squash merge → main
  │
  └─ main updated with cleaner code ✅
```

### Pattern 4: Experimental Work (Can Fail)

**Use when:** Proof of concept, research, may be abandoned

```
main
  │
  ├─ experiment/graphql-migration
  │   │
  │   ├─ Experiment: Migrate REST to GraphQL
  │   ├─ Test: Performance benchmarks
  │   │
  │   ├─ Decision: Performance 2x better! ✅
  │   │
  │   └─ Convert to feature/api/graphql-migration
  │       └─ Pull Request → main
  │
  OR
  │
  │   ├─ Decision: Performance worse, abandon ❌
  │   │
  │   └─ Delete branch (documented in ADR)
```

**Commands:**
```bash
# 1. Create experiment branch
git checkout -b experiment/graphql-migration

# 2. Experiment freely
# ... rapid iteration ...

# 3. If successful, convert to feature branch
git branch -m experiment/graphql-migration feature/api/graphql-migration
git push origin :experiment/graphql-migration  # Delete old remote
git push -u origin feature/api/graphql-migration

# 4. If failed, document and delete
git checkout main
git branch -D experiment/graphql-migration
git push origin --delete experiment/graphql-migration

# Document in ADR (Architecture Decision Record)
echo "# ADR-005: GraphQL Migration Experiment

## Decision
Abandon GraphQL migration due to 30% performance regression.

## Evidence
- Benchmark: REST 50ms avg, GraphQL 65ms avg
- Cache hit rate: REST 85%, GraphQL 60%
- Query complexity: 3x more complex queries

## Recommendation
Keep REST API, optimize with HTTP/2 instead.
" > docs/adr/005-graphql-experiment.md
```

---

## MCP Integration

### Tool: Create Branch with Validation

**Signature:**
```typescript
export async function createBranch(params: {
  type: 'feature' | 'bugfix' | 'refactor' | 'experiment' | 'hotfix' | 'chore';
  category?: string;          // Optional: auth, ui, voice, etc.
  description: string;        // Branch description (kebab-case)
  baseBranch?: string;        // Default: main
  validateNaming?: boolean;   // Default: true
}): Promise<BranchResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { createBranch } from '/wrappers/git/create-branch.js'

    const result = await createBranch({
      type: 'feature',
      category: 'auth',
      description: 'oauth2-pkce',
      validateNaming: true
    })

    return result
  `
})
```

**Output:**
```json
{
  "success": true,
  "branch_name": "feature/auth/oauth2-pkce",
  "validation": {
    "naming_valid": true,
    "conflicts_detected": false,
    "parent_branch_exists": false
  },
  "created": true,
  "remote_pushed": true,
  "next_steps": [
    "Implement feature",
    "Write tests",
    "Create pull request"
  ]
}
```

### Tool: Enforce Branch Protection

**Signature:**
```typescript
export async function enforceBranchProtection(params: {
  branches: string[];         // Branches to protect (main, develop, release/*)
  requireReviews?: number;    // Number of required reviews (default: 1)
  requireChecks?: boolean;    // Require CI/CD checks (default: true)
  allowForcePush?: boolean;   // Allow force push (default: false)
}): Promise<ProtectionResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { enforceBranchProtection } from '/wrappers/git/enforce-protection.js'

    const result = await enforceBranchProtection({
      branches: ['main', 'develop', 'release/*'],
      requireReviews: 2,
      requireChecks: true,
      allowForcePush: false
    })

    return result
  `
})
```

---

## Branch Lifecycle

### 1. Creation

```bash
# Use MCP tool (recommended)
await createBranch({
  type: 'feature',
  category: 'auth',
  description: 'oauth2-pkce'
})

# Or manual
git checkout main
git pull origin main
git checkout -b feature/auth/oauth2-pkce
git push -u origin feature/auth/oauth2-pkce
```

### 2. Development

```bash
# Make changes
# ... coding ...

# Commit frequently (atomic commits)
git add src/auth/OAuth2Manager.kt
git commit -m "feat(auth): add OAuth2Manager interface"

git add src/auth/PKCE.kt
git commit -m "feat(auth): implement PKCE challenge generation"

# Push regularly (backup + collaboration)
git push
```

### 3. Synchronization with Main

```bash
# Option A: Rebase (recommended for clean history)
git fetch origin main
git rebase origin/main

# Resolve conflicts if any
git add .
git rebase --continue

git push --force-with-lease  # Safe force push

# Option B: Merge (if team prefers merge commits)
git fetch origin main
git merge origin/main

# Resolve conflicts if any
git add .
git commit -m "merge: sync with main"
git push
```

### 4. Pull Request

```bash
# Create PR with MCP tool (recommended)
await ideacode_execute_code({
  code: `
    import { createPullRequest } from '/wrappers/git/create-pr.js'

    const result = await createPullRequest({
      title: "feat(auth): OAuth2 PKCE authentication",
      body: \`
## Summary
- Implemented OAuth2 with PKCE flow
- Added token refresh logic
- Integrated with Android AccountManager

## Test Plan
- Unit tests: 95% coverage
- Integration tests: OAuth providers (Google, GitHub)
- Manual testing: Login flows tested

## Screenshots
[Attach screenshots if UI changes]
      \`,
      reviewers: ['@manoj', '@team-lead'],
      labels: ['feature', 'auth']
    })

    return result
  `
})

# Or manual with gh CLI
gh pr create --title "feat(auth): OAuth2 PKCE authentication" \
             --body-file PR_TEMPLATE.md \
             --reviewer @manoj \
             --label feature,auth
```

### 5. Code Review

**Reviewer checklist:**
- [ ] Code follows IDEACODE standards
- [ ] Tests cover new functionality (90%+ coverage)
- [ ] Documentation updated (README, API docs)
- [ ] No security vulnerabilities (OWASP Top 10)
- [ ] Performance acceptable (benchmarks if applicable)
- [ ] Breaking changes documented (migration guide)

### 6. Merge

```bash
# After approval, squash merge (recommended)
gh pr merge --squash --delete-branch

# Or merge commit (if preserving history)
gh pr merge --merge --delete-branch

# Or rebase (if already rebased)
gh pr merge --rebase --delete-branch
```

### 7. Cleanup

```bash
# Delete local branch (after merge)
git checkout main
git pull origin main
git branch -d feature/auth/oauth2-pkce

# Remote branch auto-deleted by --delete-branch flag ✅

# Prune stale branches
git fetch --prune
git branch -vv | grep ': gone]' | awk '{print $1}' | xargs git branch -D
```

---

## Quality Gates

### Before Pushing Branch

- [ ] Branch name follows convention (`type/category/description`)
- [ ] No conflicts with existing branches (check with `git ls-remote`)
- [ ] Base branch is up-to-date (`git pull origin main`)

### Before Creating Pull Request

- [ ] All tests pass locally (`./gradlew test`)
- [ ] Code coverage ≥90% for new code
- [ ] Documentation updated (README, API docs, CHANGELOG)
- [ ] Commit messages follow convention (`feat:`, `fix:`, `refactor:`)
- [ ] No merge conflicts with target branch

### Before Merging Pull Request

- [ ] Code review approved (1-2 reviewers depending on criticality)
- [ ] CI/CD checks pass (build, test, lint, security scan)
- [ ] No breaking changes OR migration guide provided
- [ ] Feature flag added (if large change requiring gradual rollout)

---

## Anti-Patterns (PROHIBITED)

### ❌ Anti-Pattern 1: Long-Lived Feature Branches

**WRONG:**
```
feature/mega-refactor
  - Created: 2025-01-01
  - Last updated: 2025-06-01 (6 months old!)
  - Commits: 500+
  - Merge conflicts: 200+ files
```

**CORRECT:**
```
feature/refactor/phase-1
  - Created: 2025-01-01
  - Merged: 2025-01-15 (2 weeks)
  - Commits: 20

feature/refactor/phase-2
  - Created: 2025-01-16
  - Merged: 2025-01-30 (2 weeks)
  - Commits: 25

...
```

**Rule:** Feature branches should live <2 weeks. Break large features into phases.

### ❌ Anti-Pattern 2: Direct Commits to Main

**WRONG:**
```bash
git checkout main
# ... make changes ...
git commit -m "quick fix"
git push origin main  # ❌ PROHIBITED!
```

**CORRECT:**
```bash
git checkout -b hotfix/quick-fix
# ... make changes ...
git commit -m "fix: quick fix for production bug"
git push -u origin hotfix/quick-fix
gh pr create --title "Hotfix: Quick fix"
# ... review + merge ...
```

### ❌ Anti-Pattern 3: Generic Branch Names

**WRONG:**
```
my-feature
wip
test
temp
fix
```

**CORRECT:**
```
feature/auth/oauth2-pkce
bugfix/login-timeout
refactor/database-layer
```

### ❌ Anti-Pattern 4: Force Push to Shared Branches

**WRONG:**
```bash
git push --force origin feature/shared-feature  # ❌ Destroys teammate's work!
```

**CORRECT:**
```bash
# Use force-with-lease (fails if remote changed)
git push --force-with-lease origin feature/my-branch

# Or communicate with team before force push
```

---

## References

- **Protocol-Git-Branch-Hierarchy-v1.0.md** - Hierarchical branch organization
- **Protocol-Modular-Architecture-v1.0.md** - Modular development practices
- **Protocol-Master-Repository-Integration-v1.0.md** - Shared library strategy

---

## Changelog

### v1.0 (2025-11-15)
- Initial protocol creation
- Defined branch hierarchy and naming conventions
- Added 4 workflow patterns (feature, hotfix, refactor, experiment)
- Created branch lifecycle management
- Added MCP integration (createBranch, enforceBranchProtection)
- Defined quality gates and anti-patterns

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary

---

**IDEACODE v8.4** - Mandatory branching strategy for organized development
