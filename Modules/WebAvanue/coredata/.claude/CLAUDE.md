# WebAvanue CoreData - Module Instructions

Parent: `Modules/WebAvanue/.claude/CLAUDE.md`

---

## SCOPE

Data layer for WebAvanue browser. Handles persistence, caching, and data models.

---

## RESPONSIBILITIES

| Component | Purpose |
|-----------|---------|
| Repository implementations | Data access layer |
| Database entities | SQLDelight models |
| Cache management | LRU caching for performance |
| Data mappers | Entity ↔ Domain mapping |

---

## RULES

| Rule | Requirement |
|------|-------------|
| Database | SQLDelight only (KMP compatible) |
| No UI logic | Data layer must be UI-agnostic |
| Repository pattern | All data access through repositories |
| Interface first | Define interfaces in universal module |

---

## DEPENDENCIES

- SQLDelight
- Kotlinx Serialization
- Kotlinx Coroutines

---

## FILE NAMING

| Type | Pattern |
|------|---------|
| Entities | `{Name}Entity.kt` |
| DAOs | `{Name}Dao.kt` |
| Repositories | `{Name}RepositoryImpl.kt` |
| Mappers | `{Name}Mapper.kt` |

---

Updated: 2025-12-17
