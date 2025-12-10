# Core Domain

## Purpose
Contains the business logic and domain entities of AVA AI. This is the innermost layer with zero external dependencies, representing pure business rules and domain models.

## Implementation Phase
Phase 1.0, Week 1-2 (Foundation)

## Responsibilities
- Define core entities (User, Conversation, Message, Intent, etc.)
- Define use cases for all business operations
- Define repository interfaces (contracts)
- Define domain events and business rules
- Define value objects and domain services
- Model the AI interaction domain

## Dependencies
- None (Pure Kotlin/Java, no framework dependencies)

## Architecture Notes
**Layer**: Core (Innermost)
**Dependency Rule**: This layer depends on nothing. All other layers depend on this.
**Testing**: Highly testable with pure unit tests, no mocking required.

The domain layer encapsulates enterprise-wide business rules. Entities should be plain Kotlin classes with business logic. Use cases orchestrate the flow of data to/from entities and direct them to use their enterprise-wide business rules.
