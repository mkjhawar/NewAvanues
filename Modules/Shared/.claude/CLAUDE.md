# Shared - Module Instructions

Module: Shared - Cross-Platform Shared Libraries

---

## PURPOSE

Shared modules contain Kotlin Multiplatform (KMP) libraries used across multiple platform-specific modules. These provide common functionality without platform dependencies.

---

## MODULE STRUCTURE

| Path | Purpose |
|------|---------|
| NLU/ | Natural Language Understanding library |

---

## TECH STACK

| Component | Technology |
|-----------|------------|
| Build | Gradle with KMP |
| Language | Kotlin Multiplatform |
| Database | SQLDelight (if needed) |
| Targets | Android, iOS, Desktop, JS |

---

## NLU SUBMODULE

| Feature | Description |
|---------|-------------|
| Intent parsing | Extract user intent from text |
| Entity recognition | Identify entities in commands |
| Cross-platform | Works on all KMP targets |

---

## DEVELOPMENT RULES

| Rule | Requirement |
|------|-------------|
| Platform code | Use expect/actual pattern |
| Dependencies | KMP-compatible only |
| Testing | commonTest for shared, platform-specific for actual |
| No Android deps | Avoid android.* imports in commonMain |

---

## RELATED MODULES

| Module | Relationship |
|--------|--------------|
| AVA | NLU consumer |
| VoiceOS | Voice command parsing |
| Modules/NLU | Legacy NLU (being migrated) |

---

Updated: 2025-12-18 | Version: 1.0
