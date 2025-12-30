# Apps Directory

Platform-specific applications that are end-user facing.

## Structure

```
apps/
├── mobile-ios/         # iOS application
├── mobile-android/     # Android application
├── web-app/            # Web application
├── desktop-macos/      # macOS desktop app
└── desktop-windows/    # Windows desktop app
```

## Adding a New App

Use the monorepo wizard:

```bash
/ideacode.monorepo add
```

Or migrate an existing project:

```bash
/ideacode.monorepmigration
```

## App Guidelines

1. **One app per platform** (or variant)
2. **Use shared packages** from `packages/` directory
3. **Extract reusable code** to packages
4. **Include IDEACODE integration** (`.ideacode/` directory)
5. **Document dependencies** in module registry

## Example App Structure

```
apps/mobile-ios/
├── .ideacode/
│   ├── config.yml
│   └── standards/
├── Sources/
│   ├── App/
│   ├── Features/
│   └── UI/
├── Tests/
├── Package.swift
└── README.md
```

## Commands

```bash
# List all apps
/ideacode.monorepo list --type app

# Inspect an app
/ideacode.monorepo inspect <app-name>

# Validate structure
/ideacode.monorepo validate
```

## See Also

- `/Modules/` - Legacy/custom modules
- `/packages/` - Shared code libraries
- `/shared/` - Shared resources
- `.ideacode/registries/modules.registry.json` - Module registry
