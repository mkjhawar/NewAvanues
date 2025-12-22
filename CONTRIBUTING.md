# Contributing to NewAvanues

Thank you for contributing to NewAvanues! This document outlines our development workflow and best practices.

---

## 🔄 Git Workflow

We follow **Trunk-Based Development** with short-lived feature branches.

### Daily Workflow

```bash
# 1. Start your day - pull latest main
git checkout Avanues-Main
git pull origin Avanues-Main

# 2. Create feature branch (keep it short-lived: 1-3 days max)
git checkout -b feature/module-description
# or
git checkout -b bugfix/issue-description
# or
git checkout -b hotfix/critical-issue

# 3. Work on your changes
# ... make changes ...

# 4. Commit frequently with clear messages
git commit -m "feat(module): add user authentication"
git commit -m "fix(voiceos): resolve accessibility crash"

# 5. Push and create Pull Request
git push origin feature/module-description

# 6. Request code review in GitLab/GitHub

# 7. After approval, merge to Avanues-Main

# 8. Delete your feature branch
git branch -d feature/module-description
git push origin --delete feature/module-description
```

---

## 📝 Branch Naming Convention

| Type | Pattern | Example | Use Case |
|------|---------|---------|----------|
| Feature | `feature/module-description` | `feature/ava-login-screen` | New functionality |
| Bug Fix | `bugfix/module-description` | `bugfix/voiceos-crash` | Bug fixes |
| Hotfix | `hotfix/critical-issue` | `hotfix/security-patch` | Critical production fixes |
| Refactor | `refactor/description` | `refactor/solid-compliance` | Code improvements |

**Rules:**
- ✅ Lowercase with hyphens
- ✅ Include module name when possible
- ✅ Keep descriptive but concise
- ❌ No personal names or dates

---

## 💬 Commit Message Format

Follow **Conventional Commits** specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types
| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(ava): add voice recognition` |
| `fix` | Bug fix | `fix(voiceos): resolve memory leak` |
| `docs` | Documentation only | `docs(readme): update installation steps` |
| `style` | Code style (formatting) | `style(web): apply prettier formatting` |
| `refactor` | Code refactoring | `refactor(nlu): extract intent classifier` |
| `test` | Add/update tests | `test(database): add migration tests` |
| `chore` | Build/tooling changes | `chore(deps): update kotlin to 1.9.24` |
| `perf` | Performance improvement | `perf(query): optimize database query` |

### Scopes (Module Names)
- `ava` - AVA module
- `voiceos` - VoiceOS module
- `web` - WebAvanue module
- `cockpit` - Cockpit module
- `nlu` - NLU module
- `database` - Database layer
- `build` - Build system

### Examples
```bash
# Good commits
git commit -m "feat(ava): add user authentication screen"
git commit -m "fix(voiceos): resolve accessibility crash on Android 14"
git commit -m "docs(api): document REST endpoints"
git commit -m "refactor(nlu): apply SOLID SRP to intent classifier"

# Bad commits
git commit -m "fixed stuff"
git commit -m "WIP"
git commit -m "updates"
```

---

## 🔍 Code Review Process

### Before Creating Pull Request
- [ ] All tests pass locally
- [ ] Code follows project style guidelines
- [ ] No compiler warnings
- [ ] Documentation updated (if needed)
- [ ] Self-review completed

### Pull Request Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Module(s) Affected
- AVA / VoiceOS / WebAvanue / Cockpit / NLU / Shared

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows SOLID principles
- [ ] No hardcoded values
- [ ] Error handling implemented
- [ ] Logging added where appropriate
```

### Review Requirements
- ✅ Minimum 1 approval required
- ✅ All CI checks must pass
- ✅ No unresolved comments
- ✅ Branch up-to-date with Avanues-Main

---

## 🏗️ Module Structure

Our monorepo contains multiple modules:

```
NewAvanues/
├── Modules/
│   ├── AVA/              # AI assistant platform
│   ├── VoiceOS/          # Voice-first accessibility service
│   ├── WebAvanue/        # Web browser platform
│   ├── Cockpit/          # Management dashboard
│   └── Shared/           # Shared libraries (NLU, etc.)
├── Common/               # Common utilities
├── Docs/                 # Documentation
└── Tools/                # Build tools and scripts
```

**Isolation Rules:**
- Work within your module's directory
- For cross-module changes, discuss with team first
- Update module's `.claude/CLAUDE.md` for module-specific guidelines

---

## 🧪 Testing Requirements

### Coverage Targets
- **Unit Tests:** 90%+ for critical paths
- **Integration Tests:** All public APIs
- **E2E Tests:** Major user flows

### Running Tests
```bash
# All tests
./gradlew test

# Module-specific tests
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:test

# Android instrumented tests
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:connectedAndroidTest
```

---

## 🚫 Common Pitfalls to Avoid

| ❌ Don't | ✅ Do |
|---------|------|
| Long-lived branches (>3 days) | Merge to main every 1-3 days |
| Work directly on Avanues-Main | Always use feature branches |
| Large PRs (1000+ lines) | Keep PRs small and focused |
| Skip code review | Get 1+ approval on all PRs |
| Hardcode values | Use config files or constants |
| Commit without testing | Run tests before committing |
| Push broken code | Ensure build passes locally |
| Mix multiple features in one PR | One feature/fix per PR |

---

## 📦 Dependencies

### Adding Dependencies
1. Check if dependency already exists in `gradle/libs.versions.toml`
2. Add to version catalog (don't hardcode in build files)
3. Document why dependency is needed in PR
4. Update module documentation

### Example
```toml
# gradle/libs.versions.toml
[versions]
retrofit = "2.9.0"

[libraries]
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
```

```kotlin
// Modules/AVA/build.gradle.kts
dependencies {
    implementation(libs.retrofit.core)  // ✅ Use catalog
    // NOT: implementation("com.squareup.retrofit2:retrofit:2.9.0")  // ❌ Hardcoded
}
```

---

## 🔐 Security

- **Never commit secrets** (.env files, API keys, credentials)
- **Use environment variables** for sensitive data
- **Review `.gitignore`** before committing
- **Report security issues** privately to team leads

---

## 📖 Documentation

### When to Update Documentation
- Adding new features
- Changing public APIs
- Modifying architecture
- Fixing bugs that need explanation

### Documentation Locations
| Type | Location | Naming |
|------|----------|--------|
| Living Docs | `Docs/{Module}/LivingDocs/` | `LD-{Module}-{Desc}-V#.md` |
| Plans | `Docs/Plans/` | `NAV-Plan-{Feature}-YYMMDD-V#.md` |
| Technical | `Docs/{Module}/Technical/` | `NAV-{Module}-{Desc}-YYMMDD-V#.md` |
| Module README | `Modules/{Module}/README.md` | - |

---

## 🤝 Getting Help

- **Questions?** Ask in team chat or create discussion issue
- **Bugs?** Create issue with reproduction steps
- **Feature ideas?** Open feature request issue
- **Stuck?** Tag team member in PR for guidance

---

## 📜 Code Style

### Kotlin
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Max file size: 500 lines (SOLID - SRP)
- Max function size: 50 lines
- Use meaningful variable names

### TypeScript/React (WebAvanue)
- Follow [Airbnb Style Guide](https://github.com/airbnb/javascript)
- Use TypeScript strict mode
- Functional components with hooks

### General
- **SOLID Principles** - Always
- **DRY** - Don't Repeat Yourself
- **KISS** - Keep It Simple, Stupid
- **YAGNI** - You Aren't Gonna Need It

---

## 🎯 Quality Gates (Mandatory)

Before merging to Avanues-Main:
- [ ] All tests pass
- [ ] Build succeeds
- [ ] No lint errors/warnings
- [ ] Code review approved
- [ ] CI/CD checks pass
- [ ] Documentation updated
- [ ] Zero Tolerance compliance (file naming, etc.)

---

## 🔄 Release Process

*(To be defined - discuss with team)*

---

## 📞 Contact

- **Project Lead:** [Your Name]
- **Repository:** https://gitlab.com/AugmentalisES/newavanues
- **Issues:** https://gitlab.com/AugmentalisES/newavanues/issues

---

**Remember:** We value quality over speed. Take time to write clean, tested, documented code. Your teammates (and future you) will thank you! 🙏

---

*Last Updated: 2025-12-21*
*Version: 1.0.0*
