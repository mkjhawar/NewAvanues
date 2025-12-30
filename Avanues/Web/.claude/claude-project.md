# MainAvanues Monorepo

## Overview
MainAvanues is a monolithic repository containing multiple Avanue project modules managed via git subtrees.

## Structure
```
MainAvanues/
├── .claude/              # Claude Code configuration (root only)
├── .ideacode/            # IdeaCode configuration (root only)
│   ├── protocols/
│   └── specs/
└── Modules/
    └── WebAvanue/        # Cross-platform browser project
```

## Modules

### WebAvanue
Cross-platform web browser built with Kotlin Multiplatform (KMP).
- **Platforms**: Android, iOS, Desktop
- **Key Components**: BrowserCoreData, universal UI layer
- **Tech Stack**: Compose Multiplatform, SQLDelight, Kotlin Coroutines

## Git Workflow
- **Primary Branch**: `development`
- **Module Branches**: Each module has its own branch (e.g., `WebAvanue`)
- **Subtree Management**: Modules imported via `git subtree` to preserve history

## Adding New Modules
```bash
git subtree add --prefix=Modules/<ModuleName> <repo-path> <branch>
```
