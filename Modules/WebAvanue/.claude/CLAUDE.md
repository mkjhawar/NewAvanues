# WebAvanue - Module Instructions

Parent Repository: NewAvanues
Module: WebAvanue

---

## SCOPE

Work within WebAvanue module only.
For cross-module changes, check with user first.

---

## INHERITED RULES

1. Parent repo rules: `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
2. Global rules: `/Volumes/M-Drive/Coding/.claude/CLAUDE.md`

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/WebAvanue/LivingDocs/LD-*.md`
**Registries:** `Modules/WebAvanue/.ideacode/registries/`
- FOLDER-REGISTRY.md - Folder structure for this module
- FILE-REGISTRY.md - File naming for this module
- COMPONENT-REGISTRY.md - Components in this module

**Check Registries FIRST** before creating files or folders.

---

## MODULE-SPECIFIC RULES

| Rule | Requirement |
|------|-------------|
| Framework | React 18+ with TypeScript |
| Styling | Tailwind CSS |
| State | React Hooks + Context API |
| Build | Vite |
| Testing | Jest + React Testing Library (90%+ coverage) |

---

## KEY COMPONENTS

- **Dashboard UI** - Main dashboard interface
- **API Integration** - REST client for backend
- **Authentication** - JWT-based auth
- **Real-time Updates** - WebSocket integration

---

## DEPENDENCIES

**Internal:**
- `Common/Core` - Shared utilities (planned)
- AVA integration (planned)

**External:**
- React, TypeScript
- Tailwind CSS
- Axios (HTTP client)
- Socket.io (WebSockets)

See: `/Volumes/M-Drive/Coding/NewAvanues/.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

---

## FILE NAMING

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-WEB-{Desc}-V#.md` | `LD-WEB-Feature-V1.md` |
| Specs | `WEB-Spec-{Feature}-YDDMM-V#.md` | `WEB-Spec-Dashboard-51215-V1.md` |
| Components | `{Component}.tsx` | `Dashboard.tsx` |
| Pages | `{Page}Page.tsx` | `DashboardPage.tsx` |
| Services | `{Service}Service.ts` | `ApiService.ts` |

---

## BUILDING & TESTING

```bash
# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Run tests
npm test

# Type check
npm run type-check
```

---

Updated: 2025-12-15 | Version: 12.0.0
