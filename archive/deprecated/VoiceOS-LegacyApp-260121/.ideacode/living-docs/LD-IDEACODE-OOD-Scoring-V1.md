# LD-IDEACODE-OOD-Scoring-V1

## Purpose
Intelligent Object-Oriented Development scoring system for automatic OOD pattern recommendation and enforcement.

---

## OOD Score Calculation

### Formula
```
OOD Score = 0 + (positive signals) - (negative signals)
```

### Positive Signals (Add Points)

| Signal | Keywords/Patterns | Points |
|--------|-------------------|--------|
| Domain Logic | `domain/`, `model/`, `entity/`, `business/`, `core/` | +30 |
| Data Modeling | `User`, `Order`, `Product`, `Account`, `Transaction`, `Invoice`, `Payment` | +25 |
| State Management | `state`, `status`, `lifecycle`, `workflow`, `transition` | +20 |
| Relationships | `has_many`, `belongs_to`, `one_to_many`, `aggregate`, `composition` | +20 |
| Validation Rules | `validate`, `constraint`, `invariant`, `rule`, `policy` | +15 |
| Complex Types | Multiple properties (>5), nested objects, enums with behavior | +15 |
| Identity | `id`, `uuid`, `identifier`, `key`, `reference` | +10 |
| Persistence | `repository`, `store`, `persist`, `save`, `load` | +10 |
| Factory Pattern | `create`, `build`, `construct`, `factory`, `builder` | +10 |
| Service Layer | `service`, `handler`, `processor`, `manager`, `coordinator` | +10 |

### Negative Signals (Subtract Points)

| Signal | Keywords/Patterns | Points |
|--------|-------------------|--------|
| Simple CRUD | `list`, `get`, `set`, `update`, `delete` (without business logic) | -25 |
| Utility Code | `utils/`, `helpers/`, `common/`, `shared/` | -20 |
| Configuration | `config`, `settings`, `preferences`, `options` | -15 |
| UI Layer | `view`, `screen`, `component`, `widget`, `layout` | -15 |
| Scripts | `script`, `migration`, `seed`, `fixture` | -10 |
| DTOs Only | `dto`, `request`, `response`, `payload` (without behavior) | -10 |

---

## Thresholds

| Score | Level | Action |
|-------|-------|--------|
| < 40 | None | Proceed without OOD patterns |
| 40-59 | Suggested | Suggest: "OOD patterns may improve this code" |
| 60-79 | Recommended | Ask: "Apply OOD patterns? (Entity/Value Object/Service)" |
| >= 80 | Strongly Recommended | Ask: "OOD Strongly Recommended. Which patterns?" |

---

## OOD Pattern Detection

### Pattern Recommendations by Context

| Context | Recommended Patterns |
|---------|---------------------|
| Data with identity | Entity |
| Data without identity (immutable) | Value Object |
| Complex object creation | Factory / Builder |
| Cross-entity operations | Domain Service |
| Data access abstraction | Repository |
| Object clusters with root | Aggregate |
| Domain events | Event / Event Handler |
| Business rules | Specification / Policy |

### Pattern Detection Signals

| Pattern | Detection Keywords | When to Suggest |
|---------|-------------------|-----------------|
| **Entity** | `id`, `uuid`, identity comparison, mutable state | Data that needs tracking over time |
| **Value Object** | Immutable, equality by value, no id | Money, Address, DateRange, Email |
| **Aggregate** | Root entity, invariants across objects, transactional boundary | Order+OrderItems, User+Permissions |
| **Repository** | Collection semantics, persistence abstraction | Data access for aggregates |
| **Factory** | Complex construction, multiple creation paths | Objects with many dependencies |
| **Domain Service** | Cross-entity logic, stateless operations | Payment processing, validation |
| **Specification** | Complex query logic, reusable predicates | Filtering, eligibility rules |

---

## OOD Workflow

### Standard OOD Analysis Flow
```
1. DETECT: Analyze code context for OOD signals
2. SCORE: Calculate OOD score from signals
3. RECOMMEND: Suggest appropriate patterns
4. APPLY: Implement chosen patterns
5. VALIDATE: Check SOLID compliance
```

### Integration with Commands

| Command | OOD Behavior |
|---------|--------------|
| `/idevelop` | Calculate score → If >= 40, suggest patterns → Apply if accepted |
| `/ifix` | Detect if fix benefits from OOD → Suggest refactor opportunity |
| `/irefactor` | Analyze existing code → Recommend OOD improvements |
| `/ianalyze .code` | Include OOD analysis in report |

---

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.ood` | Force OOD analysis and pattern suggestions |
| `.skip-ood` | Skip OOD recommendations |
| `.ddd` | Full DDD analysis (Entity, Aggregate, Repository, Service) |
| `.solid` | Enforce SOLID principles check |

---

## Score Display Format

```
OOD Analysis: "implement user order management"

Signals Detected:
  + 30  Domain logic (domain/)
  + 25  Data modeling (User, Order)
  + 20  Relationships (has_many)
  + 15  Validation rules (validate)
  + 10  Identity (id, uuid)
  ─────────────────────────────
  = 100  OOD Strongly Recommended

Suggested Patterns:
  • Entity: User, Order
  • Value Object: Money, Address
  • Aggregate: Order (root) + OrderItems
  • Repository: UserRepository, OrderRepository
  • Service: OrderProcessingService

→ Apply OOD patterns? (Y/n): _
```

---

## Pattern Implementation Templates

### Entity Template
```
Entity: {Name}
├── Identity: {id_type}
├── Properties: {list}
├── Behaviors: {methods}
├── Invariants: {rules}
└── Events: {domain_events}
```

### Value Object Template
```
Value Object: {Name}
├── Properties: {immutable_list}
├── Validation: {constraints}
├── Equality: by value
└── Operations: {methods}
```

### Aggregate Template
```
Aggregate: {RootName}
├── Root Entity: {root}
├── Child Entities: {children}
├── Value Objects: {vos}
├── Invariants: {cross_entity_rules}
└── Repository: {aggregate}Repository
```

### Repository Template
```
Repository: {Aggregate}Repository
├── find(id): {Aggregate}
├── findBy{Criteria}: List<{Aggregate}>
├── save({Aggregate}): void
├── delete({Aggregate}): void
└── Specification support: yes/no
```

### Domain Service Template
```
Service: {Name}Service
├── Dependencies: {repositories, other_services}
├── Operations: {stateless_methods}
├── Validations: {business_rules}
└── Events: {published_events}
```

---

## SOLID Integration

OOD scoring includes automatic SOLID compliance checking:

| Principle | Detection | Action |
|-----------|-----------|--------|
| **S**ingle Responsibility | Class with >3 responsibilities | Suggest split |
| **O**pen/Closed | Modification vs extension | Suggest strategy/decorator |
| **L**iskov Substitution | Inheritance violations | Suggest composition |
| **I**nterface Segregation | Fat interfaces | Suggest split |
| **D**ependency Inversion | Concrete dependencies | Suggest abstraction |

### SOLID Score (Sub-score)
```
SOLID compliance calculated separately:
- 0-40: Major violations (block commit in strict mode)
- 41-70: Minor violations (warning)
- 71-100: Compliant
```

---

## Examples

### Example 1: High Score (OOD Strongly Recommended)
```
/idevelop "implement order management system"

OOD Score: 95
  + 30  Domain logic
  + 25  Data modeling (Order, Product, Customer)
  + 20  State management (order lifecycle)
  + 20  Relationships (Order → OrderItems)

→ OOD Strongly Recommended.
  Suggested: Entity, Aggregate, Repository, Service
```

### Example 2: Medium Score (OOD Recommended)
```
/idevelop "add user profile validation"

OOD Score: 55
  + 25  Data modeling (User)
  + 15  Validation rules
  + 15  Complex types
  - 10  DTO-like structure

→ OOD Recommended. Apply patterns? (Y/n): _
```

### Example 3: Low Score (No OOD)
```
/idevelop "add logging utility"

OOD Score: -15
  - 20  Utility code (utils/)
  + 10  Service pattern
  - 5   Script-like

→ Proceeding without OOD patterns.
```

### Example 4: Forced OOD
```
/idevelop .ood "create user preferences"

OOD Score: 25 (below threshold)
.ood modifier: FORCED

→ OOD Analysis enabled (forced by modifier).
  Suggested: Value Object (Preferences)
```

---

## Configuration (Optional)

Projects can customize in `.ideacode/config.yml`:

```yaml
ood:
  enabled: true
  threshold_suggest: 40
  threshold_recommend: 60
  threshold_strong: 80
  enforce_solid: true
  patterns:
    entity: true
    value_object: true
    aggregate: true
    repository: true
    factory: true
    service: true
    specification: false
  custom_signals:
    positive:
      - pattern: "inventory"
        points: 20
    negative:
      - pattern: "legacy"
        points: -15
```

---

## OOD + TDD Integration

When both OOD and TDD are recommended:

```
Analysis: "implement payment processing"

TDD Score: 85 (Strongly Recommended)
OOD Score: 90 (Strongly Recommended)

Recommended Workflow:
1. Define domain model (Entity, Value Object)
2. Write tests for domain behavior (TDD)
3. Implement domain logic
4. Add repository tests (TDD)
5. Implement persistence
6. Validate SOLID compliance
```

---

## Metadata
- **Document:** LD-IDEACODE-OOD-Scoring-V1
- **Version:** 1.0
- **Created:** 2025-12-05
- **Author:** IDEACODE
