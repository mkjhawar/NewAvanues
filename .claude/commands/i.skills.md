---
name: i.skills
description: List all available IDEACODE skills (global + project)
---

# /i.skills - Skills Listing (32 Total)

## Document Processing
| Skill | Trigger |
|-------|---------|
| `pdf` | PDF, extract, merge, OCR |
| `docx` | Word, document, .docx |
| `xlsx` | Excel, spreadsheet, formulas |
| `pptx` | PowerPoint, presentation, slides |

## Platform Development
| Skill | Trigger |
|-------|---------|
| `developing-android` | Android, Kotlin, Compose, Material3 |
| `developing-ios` | iOS, Swift, SwiftUI, UIKit |
| `developing-react` | React, hooks, TypeScript, shadcn |
| `developing-kmp` | KMP, shared code, expect/actual |
| `developing-kotlin` | coroutines, flows, DSL |
| `developing-web` | DOM, CSS, accessibility |
| `developing-tauri` | Tauri, Rust, desktop |

## Workflows
| Skill | Trigger |
|-------|---------|
| `develop-feature` | build, create, implement, add |
| `fix-bug` | fix, bug, error, broken |
| `create-ui` | screen, interface, layout, UI |
| `create-spec` | spec, requirements, define |
| `analyze-code` | analyze, review, evaluate |

## Quality & Patterns
| Skill | Trigger |
|-------|---------|
| `tdd-development` | TDD, test-first, business logic |
| `ood-development` | OOD, domain models, entities |
| `verification` | verify, after spec/plan/implement |

## Tools & Testing
| Skill | Trigger |
|-------|---------|
| `api-builder` | REST, GraphQL, LLM, RAG, API |
| `mcp-builder` | MCP, protocol, tool server |
| `webapp-testing` | Playwright, automation, browser |
| `implementing-webrtc` | WebRTC, peer-to-peer, video |
| `managing-cloud-storage` | S3, GCP, Firebase, Azure |

## Design & Creative
| Skill | Trigger |
|-------|---------|
| `theme-factory` | theme, colors, palette, fonts |
| `algorithmic-art` | generative, p5.js, creative |

## Meta & Analysis
| Skill | Trigger |
|-------|---------|
| `skill-creator` | create skill, new skill |
| `analyzing-security` | security, vulnerabilities, OWASP |
| `reviewing-code` | code review, PR review |
| `running-tests` | run tests, test execution |
| `writing-documentation` | docs, documentation |
| `building-html-artifacts` | HTML artifact, bundle |

## Location

Global: `/Volumes/M-Drive/Coding/.claude/skills/`
Project: `{repo}/.claude/skills/` (overrides global)

## Auto-Invoke

Skills auto-invoke based on:
- Task keywords (see Trigger column)
- TDD/OOD scores (>50 = suggest, >70 = recommend, >90 = enforce)
- Platform detection (Android files → developing-android)
- File types (.pdf → pdf, .xlsx → xlsx)

## Manual Use

```
# Read skill directly
Read .claude/skills/{skill-name}/SKILL.md

# Or use modifier
/i.skills .read api-builder
```

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.list` | Show this listing (default) |
| `.read {name}` | Read specific skill |
| `.search {term}` | Search skills by keyword |
