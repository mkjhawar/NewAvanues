---
description: Specification operations | /i.spec "feature" | /i.spec .review | /i.spec .respec
---

# Task: Specification Operations

API: `http://localhost:3847/i.spec` | Token savings: 97%

---

## Modes

| Mode | Usage | Action |
|------|-------|--------|
| Create | `/i.spec "feature"` | Create new spec with platform detection |
| Review | `/i.spec .review` | Analyze specs vs implementation, report gaps |
| Respec | `/i.spec .respec` | Review + Q&A to update requirements |
| Fix | `/i.spec .fix` | Fix implementation to match specs |
| Continue | `/i.spec .continue` | Resume development from specs |
| Variant | `/i.spec .variant "Name"` | Create new project with same/updated specs |
| Clarify | `/i.spec .clarify "spec.md"` | Add questions for underspecified areas |

---

## Create Mode Workflow

| Phase | Action |
|-------|--------|
| 1. Config | Read `.ideacode/config.ideacode`, detect context |
| 2. Platform | Detect from keywords, recommend stacks |
| 3. Interview | 5 questions: scope, value, users, context, constraints |
| 4. Generate | Create `{App}-Spec-{Feature}-{YDDMMHH}-V1.md` |
| 5. Validate | Check measurable criteria, completeness |
| 6. Chain | Offer: /plan, /clarify, /tasks |

---

## Review Mode Workflow

| Step | Action | Output |
|------|--------|--------|
| 1. Discover | Find specs in docs/specifications/, .ideacode/living-docs/ | Inventory |
| 2. Analyze | Compare to src/ or platform dirs | ✓/⚠️/✗/⚡ per requirement |
| 3. Report | Coverage %, gaps, violations | `docs/analysis/Spec-Review-*.md` |

---

## Respec Mode Workflow

| Step | Action |
|------|--------|
| 1. Review | Same as review mode |
| 2. Q&A | Ask about each gap: keep/remove/update requirement? |
| 3. Update | Modify specs based on answers, increment version V1→V2 |
| 4. Options | Offer: .fix, .continue, .variant |

---

## Platform Detection Keywords

| Keyword | Platforms | Confidence |
|---------|-----------|------------|
| mobile app | Android, iOS | 95% |
| authentication | All + Backend | 100% |
| website | Web, Backend | 100% |
| api | Backend | 100% |
| real-time | Backend + clients | 90% |
| offline | Android, iOS (KMP) | 85% |

---

## Tech Stacks

| Platform | Language | UI | Database | Auth | Testing |
|----------|----------|----|---------| -----|---------|
| Android | Kotlin | Compose | SQLDelight | BiometricPrompt | JUnit+Espresso |
| iOS | Swift | SwiftUI | CoreData | FaceID/TouchID | XCTest |
| Web | TypeScript | React+Next | PostgreSQL | JWT+OAuth2 | Jest+RTL |
| Backend | Kotlin/TS/Python | Ktor/Express/FastAPI | PostgreSQL | JWT+OAuth2 | JUnit/Jest |
| KMP | Kotlin | N/A | Shared models | Crypto | Shared tests |

---

## Interview Questions

| # | Category | Platform Follow-ups |
|---|----------|---------------------|
| 1 | Scope | "What functionality?" | Android: API level, Material3, offline |
| 2 | Value | "Why needed? Problem?" | iOS: iOS version, dark mode, VoiceOver |
| 3 | Users | "Who uses this?" | Backend: Load, retention, geo, compliance |
| 4 | Context | "Which project?" | Web: Browsers, responsive, SEO, PWA |
| 5 | Constraints | "Limitations/deadlines?" | |

---

## Spec Template Sections

| Section | Platform-Aware |
|---------|----------------|
| Executive Summary | ✓ |
| Problem Statement | ✓ |
| Functional Requirements | ✓ Per platform: FR-{num}: {Platform} - {Requirement} |
| Non-Functional | ✓ Performance, security, accessibility, offline |
| Platform Sections | ✓ Android, iOS, Web, Backend subsections |
| Acceptance Criteria | ✓ Measurable per requirement |
| Out of Scope | ✓ |
| Dependencies | ✓ |

---

## Modifiers

| Modifier | Effect |
|----------|--------|
| .review | Report only, no changes |
| .respec | Review + Q&A update |
| .fix | Fix code vs specs |
| .continue | Resume development |
| .variant "Name" | Create new project |
| .clarify | Add clarification questions |
| .checklist | Generate feature checklist |
| .yolo | Auto-chain: spec→plan→implement |
| .implement | Auto-chain enabled |
| .stop | Disable chaining |
| .cot | Show reasoning |
| .tot | Explore alternatives |
| .tasks | TodoWrite tasks |

---

## Output Files

| Mode | File |
|------|------|
| Create | `docs/specifications/{App}-Spec-{Feature}-{YDDMMHH}-V#.md` |
| Review | `docs/analysis/Spec-Review-{YYYYMMDD}-{HHMMSS}.md` |
| Respec | Updated specs (version increment) |
| Fix | Updated code + tests |
| Continue | Implementation files |
| Variant | New repo with copied/updated specs |

---

## Examples

```bash
/i.spec "auth with biometric"          # Create: Android+iOS→KMP
/i.spec .review                        # Coverage: Auth 80%, Offline 40%
/i.spec .respec                        # Q&A: Keep offline? Update auth?
/i.spec .fix .tdd                      # Fix: 3 gaps, add tests
/i.spec .continue .ood                 # Resume: Features C,D,E (60%)
/i.spec .variant "MobileApp" .cot      # Variant: web→mobile
```

---

## Related

| Command | Use |
|---------|-----|
| /i.plan | After spec creation |
| /i.implement | After planning |
| /i.develop | Full: spec→plan→implement |
| /i.analyze | Part of .review |

---

**Version:** 2.0 (consolidated) | **Updated:** 2025-12-11

