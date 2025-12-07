# Shared Directory

Shared resources, assets, configurations, and tools used across the monorepo.

## Structure

```
shared/
├── assets/            # Images, icons, fonts
├── configs/           # Shared configuration files
├── scripts/           # Build scripts, automation
├── design-tokens/     # Design system tokens
└── docs/              # Shared documentation
```

## What Goes Here?

- **Assets**: Images, icons, fonts, media
- **Configs**: ESLint, Prettier, TypeScript configs
- **Scripts**: Build automation, deployment scripts
- **Design tokens**: Colors, spacing, typography
- **Documentation**: Architecture docs, ADRs

## What Doesn't Go Here?

- **Code** → goes in `packages/`
- **Apps** → goes in `apps/`
- **Module-specific resources** → stays in module

## Guidelines

1. **Organized by type** (assets/, configs/, etc.)
2. **Platform-agnostic** where possible
3. **Well documented**
4. **Versioned with repo**

## Example Structure

```
shared/
├── assets/
│   ├── images/
│   │   ├── logo.svg
│   │   └── icons/
│   └── fonts/
│       └── Inter/
├── configs/
│   ├── eslint.config.js
│   ├── prettier.config.js
│   └── tsconfig.base.json
├── scripts/
│   ├── build.sh
│   └── deploy.sh
└── design-tokens/
    ├── colors.json
    └── spacing.json
```

## Usage

### Assets
```typescript
// Reference in code
const logo = require('@shared/assets/images/logo.svg')
```

### Configs
```json
// Extend in app
{
  "extends": "../../shared/configs/tsconfig.base.json"
}
```

### Scripts
```bash
# Run from root
./shared/scripts/build.sh
```

## Commands

```bash
# List shared resources
/ideacode.monorepo list --type shared
```

## See Also

- `/apps/` - Applications
- `/packages/` - Code libraries
- `/Modules/` - Legacy modules
