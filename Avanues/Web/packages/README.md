# Packages Directory

Shared code libraries used across multiple apps in the monorepo.

## Structure

```
packages/
├── core/              # Core utilities and helpers
├── ui-components/     # Shared UI components
├── api-client/        # API client library
├── auth/              # Authentication logic
├── database/          # Database access layer
└── models/            # Shared data models
```

## What Goes Here?

- **Utilities** used by 2+ apps
- **UI components** shared across platforms
- **Business logic** that's platform-agnostic
- **API clients** and data access
- **Data models** and types
- **Common functionality**

## What Doesn't Go Here?

- **App-specific code** → stays in `apps/`
- **Cross-project shared code** → goes in `/ideacode/libraries/`
- **Assets and resources** → goes in `shared/`

## Adding a New Package

Use the monorepo wizard:

```bash
/ideacode.monorepo add
# Select type: package
```

Or create manually:

```bash
mkdir -p packages/my-package/src
cd packages/my-package
# Create .ideacode/config.yml
# Add to registry
```

## Package Guidelines

1. **Small and focused** - one responsibility
2. **Well documented** - README + API docs
3. **Well tested** - 90%+ coverage
4. **Versioned** (internal versioning)
5. **No circular dependencies**

## Example Package Structure

```
packages/core/
├── .ideacode/
│   └── config.yml
├── src/
│   ├── index.ts
│   ├── utils/
│   └── types/
├── tests/
├── package.json       # For JS/TS
├── build.gradle       # For Kotlin/Android
└── README.md
```

## Importing Packages

### TypeScript/JavaScript
```typescript
import { helper } from '@mainavanues/core'
```

### Kotlin/Android
```kotlin
implementation(project(":packages:core"))
```

### Swift/iOS
```swift
import MainAvanuesCore
```

## Commands

```bash
# List all packages
/ideacode.monorepo list --type package

# Show dependencies
/ideacode.monorepo deps

# Validate structure
/ideacode.monorepo validate
```

## Extraction from Apps

When you find code being duplicated across apps:

```bash
# Use migration wizard to extract
/ideacode.monorepmigration --extract apps/mobile-ios/utils/
# → Creates packages/utils/
# → Updates imports in mobile-ios
```

## See Also

- `/apps/` - Platform-specific applications
- `/shared/` - Shared resources
- `.ideacode/registries/modules.registry.json` - Dependency tracking
