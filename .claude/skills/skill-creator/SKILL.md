---
name: skill-creator
description: Create new Claude Code skills. Use when building modular packages that extend Claude's capabilities with specialized knowledge, workflows, or tools.
---

# Skill Creator

## Skill Philosophy

| Principle | Guideline |
|-----------|-----------|
| Concise | Context window is precious - only include what Claude doesn't know |
| Freedom levels | High for flexible tasks, low for error-prone sequences |
| Progressive | Load metadata always, body when triggered, resources as needed |

## Skill Structure

```
skill-name/
├── SKILL.md          # Required: Instructions
├── scripts/          # Optional: Executable code
├── references/       # Optional: Documentation
└── assets/           # Optional: Templates, boilerplate
```

## SKILL.md Format

```markdown
---
name: skill-name
description: When to use this skill. Clear trigger words.
---

# Skill Title

## Purpose
What this skill does and when to invoke it.

## Workflow
1. Step one
2. Step two
3. Step three

## Rules
| Rule | Requirement |
|------|-------------|
| ... | ... |

## Modifiers
| Modifier | Effect |
|----------|--------|
| .modifier | What it changes |
```

## Design Guidelines

| Guideline | Reason |
|-----------|--------|
| < 500 lines | Split larger into references |
| Imperative form | "Do X" not "This does X" |
| Tables over prose | Scannable, compact |
| No README/CHANGELOG | Only execution essentials |

## Token Budget

| Component | Target |
|-----------|--------|
| Metadata | ~100 tokens (always loaded) |
| SKILL.md body | <5k tokens (on trigger) |
| References | Load as needed |

## Creation Workflow

```
1. Understand → Gather concrete examples
2. Plan       → Identify reusable content
3. Initialize → Create structure
4. Edit       → Write SKILL.md + resources
5. Test       → Verify with real usage
6. Iterate    → Refine based on results
```

## Trigger Word Design

| Good | Bad |
|------|-----|
| Specific verbs: "create PDF", "merge Excel" | Generic: "help with files" |
| Domain terms: "Playwright test", "MCP server" | Vague: "test something" |
| Action-oriented: "build", "generate", "analyze" | Passive: "regarding", "about" |

## Reference Files

When SKILL.md exceeds 500 lines:

```
skill-name/
├── SKILL.md           # Core workflow (<500 lines)
└── references/
    ├── api.md         # API reference
    ├── examples.md    # Code examples
    └── advanced.md    # Edge cases
```

Reference in SKILL.md:
```markdown
See: references/api.md for full API documentation
```

## Script Integration

```
skill-name/
├── SKILL.md
└── scripts/
    ├── process.py     # Processing logic
    └── validate.sh    # Validation
```

In SKILL.md:
```markdown
Run: `python scripts/process.py --input file.txt`
```

## Anti-Patterns

| Avoid | Why |
|-------|-----|
| Duplicate Claude's knowledge | Wastes tokens |
| Verbose explanations | Use tables instead |
| Meta-documentation | README, CHANGELOG unnecessary |
| Deep nesting | One level from SKILL.md max |
| Multiple similar skills | Combine with modifiers |

## Skill Categories

| Category | Examples |
|----------|----------|
| Document | pdf, docx, xlsx, pptx |
| Platform | android, ios, react, tauri |
| Workflow | develop-feature, fix-bug, tdd |
| Tool | mcp-builder, webapp-testing |
| Creative | algorithmic-art, theme-factory |

## Quality Checklist

- [ ] Clear trigger words in description
- [ ] Under 500 lines (or split)
- [ ] Uses tables, not paragraphs
- [ ] Imperative language
- [ ] No redundant information
- [ ] Tested with real use case
