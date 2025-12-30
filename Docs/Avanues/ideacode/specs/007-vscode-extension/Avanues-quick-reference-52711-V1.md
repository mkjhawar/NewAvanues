# AVAMagic VSCode Extension - Quick Reference

**Feature ID:** 007
**Version:** 1.0.0
**Status:** Ready for Planning

---

## At a Glance

**What:** VSCode extension for AVAMagic framework development
**Why:** 100% feature parity with Android Studio plugin, serve 70%+ VSCode market
**Complexity:** Tier 3 (Large, Multi-Module)
**Effort:** 400-500 hours

---

## Core Features (6)

1. **Multi-Provider AI** (5 providers, 16 features)
2. **Component Palette** (263 components, 8 categories)
3. **Language Server** (LSP with full IntelliSense)
4. **Code Generation** (AI + templates, 4 platforms)
5. **Template Library** (60+ UI patterns)
6. **Settings UI** (Comprehensive configuration)

---

## AI Providers (5)

| Provider | Cost | Speed | Status |
|----------|------|-------|--------|
| Claude API | $3-15/M | Medium | v1.0 |
| Claude Code | $20/mo | Medium | Planned |
| Gemini | FREE | Fast | v1.0 |
| GPT-4 | $10-30/M | Slow | Planned |
| Local LLM | FREE | Very Slow | Planned |

---

## AI Features (16)

**Basic (4):**
1. Generate from description
2. Explain component
3. Optimize component
4. Multi-platform code generation

**Advanced (12):**
5. Code review
6. Accessibility check (WCAG 2.1)
7. Performance analysis
8. Test generation
9. Documentation generation
10. Bug detection
11. Refactoring suggestions
12. Design system validation
13. Semantic search
14. Template suggestions
15. Usage statistics
16. Availability check

---

## Component Categories (8)

1. **Layout** (18) - Container, Row, Column, Grid, etc.
2. **Input** (24) - TextField, Select, DatePicker, etc.
3. **Display** (32) - Text, Image, Badge, Chip, etc.
4. **Navigation** (18) - AppBar, Tabs, Drawer, Menu, etc.
5. **Feedback** (15) - Dialog, Alert, Toast, Snackbar, etc.
6. **Data** (22) - List, Table, Tree, Accordion, etc.
7. **Charts** (45) - Line, Bar, Pie, Scatter, Heatmap, etc.
8. **Calendar** (12) - Calendar, EventCalendar, DatePicker, etc.

**Total:** 263 components

---

## LSP Features (9)

1. Syntax highlighting
2. IntelliSense (autocomplete)
3. Hover documentation
4. Go to definition
5. Find references
6. Real-time diagnostics
7. Code actions (quick fixes)
8. Code formatting
9. Semantic tokens

---

## Template Categories (8)

1. Authentication (8) - Login, signup, OTP, etc.
2. Dashboard (6) - Analytics, admin, KPI, etc.
3. Forms (12) - Contact, survey, payment, etc.
4. Lists & Grids (8) - Card grid, infinite scroll, etc.
5. Charts (10) - Dashboard, reports, analytics
6. E-Commerce (8) - Product list, cart, checkout
7. Settings (4) - Preferences, account, privacy
8. Profile (4) - User profile, edit, social

**Total:** 60+ templates

---

## Commands & Shortcuts

| Command | Shortcut |
|---------|----------|
| Generate from Description | Ctrl+Shift+G |
| Explain Component | Ctrl+Shift+E |
| Optimize Component | Ctrl+Shift+O |
| Generate Platform Code | Ctrl+Shift+P |
| Review Code | Ctrl+Shift+R |
| Check Accessibility | Ctrl+Shift+A |
| New Component | Ctrl+Alt+C |
| New Screen | Ctrl+Alt+S |
| Insert Template | Ctrl+Alt+T |
| Open Component Palette | Ctrl+Alt+P |

---

## Tech Stack

**Core:**
- TypeScript 5.0+
- VSCode Extension API 1.80+
- Node.js 18+

**Language:**
- Language Server Protocol (LSP)
- Custom parser for .vos/.ava files

**UI:**
- React 18+ (webviews)
- VSCode Webview UI Toolkit

**AI:**
- @anthropic-ai/sdk (Claude)
- @google-ai/generativelanguage (Gemini)
- openai (GPT-4)

**Build:**
- Webpack 5
- Jest (testing)

---

## Performance Targets

| Metric | Target |
|--------|--------|
| Extension activation | < 2 sec |
| AI response (Claude) | 2-4 sec |
| AI response (Gemini) | 1-2 sec |
| LSP IntelliSense | < 100 ms |
| LSP diagnostics | < 200 ms |
| Bundle size (compressed) | < 5 MB |
| Memory usage | < 400 MB |

---

## Platform Support

**OS:** Windows 10+, macOS 11+, Linux (Ubuntu 20.04+)
**VSCode:** 1.80.0+
**Distributions:** VSCode, VSCode Insiders, VSCodium

---

## File Types

- `.vos` - AVAMagic screen files
- `.ava` - AVAMagic component files

---

## Code Generation Targets (4)

1. Android (Jetpack Compose)
2. iOS (SwiftUI)
3. Web (React + TypeScript)
4. Desktop (Compose Desktop)

---

## Success Criteria

**Must Have (v1.0):**
- [ ] 100% feature parity with Android Studio plugin
- [ ] All 16 AI features operational
- [ ] 263 components in palette
- [ ] LSP with full IntelliSense
- [ ] 60+ templates
- [ ] Published to VSCode Marketplace
- [ ] 90%+ test coverage

---

## Timeline

**Estimated:** 14 weeks (1 developer, 35-40 hrs/week)

**Phases:**
1. Foundation (Weeks 1-2)
2. Language Server (Weeks 3-4)
3. Component Palette (Week 5)
4. AI Integration (Weeks 6-8)
5. Advanced AI (Weeks 9-10)
6. Templates (Week 11)
7. Settings & Polish (Week 12)
8. Documentation & Release (Weeks 13-14)

---

## Out of Scope (v1.0)

- Visual drag-and-drop designer → v2.0
- Live preview → v2.0
- Mobile device preview → v2.0
- Team collaboration → v2.0+
- Design-to-code → v2.0+

---

## Key Documents

**This Spec:**
- `/Volumes/M-Drive/Coding/Avanues/docs/ideacode/specs/007-vscode-extension/spec-vscode-extension.md`

**Android Studio Plugin (Reference):**
- `/Volumes/M-Drive/Coding/Avanues/tools/android-studio-plugin/README.md`
- `/Volumes/M-Drive/Coding/Avanues/tools/android-studio-plugin/AI-ENHANCEMENTS-SPEC.md`
- `/Volumes/M-Drive/Coding/Avanues/tools/android-studio-plugin/DEVELOPER-MANUAL.md`

**Component Registry:**
- `/Volumes/M-Drive/Coding/Avanues/docs/COMPLETE-COMPONENT-REGISTRY-LIVING.md`

---

## Next Steps

1. **Agent 4:** Create implementation plan
2. **Developers:** Begin Phase 1 (Foundation)
3. **Stakeholders:** Review and approve spec

---

**Created:** 2025-11-27
**Agent:** Agent 3 (Specification Agent)
**Methodology:** IDEACODE v9.0
**Swarm:** 007-vscode-extension
