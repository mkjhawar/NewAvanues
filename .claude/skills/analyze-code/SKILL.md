---
name: analyze-code
description: Code analysis workflow. Use when user wants to understand, review, analyze, or evaluate code quality, architecture, or implementation.
---

# Code Analysis

## Trigger Words

| Intent | Examples |
|--------|----------|
| Understand | "explain", "how does", "what does" |
| Review | "review", "check", "evaluate" |
| Analyze | "analyze", "assess", "examine" |

## Analysis Types

| Type | Focus | Output |
|------|-------|--------|
| Quality | Code smells, SOLID | Issues list |
| Security | Vulnerabilities | Security report |
| Performance | Bottlenecks | Optimization plan |
| Architecture | Structure, patterns | Architecture doc |
| Coverage | Tests, gaps | Coverage report |

## Workflow

```
1. Index    → Read MASTER-INDEX.md (conventions)
2. Scope    → Identify files/modules
3. Read     → Understand code
4. Validate → Check folder/naming (Layer 8)
5. Analyze  → Apply analysis type
6. Report   → Structured findings
```

## Folder Validation (Zero Tolerance)

| Check | Rule | Action |
|-------|------|--------|
| KMP source sets | Exact Gradle names | REJECT |
| Package depth | Max 4 levels | WARN |
| Redundant folders | No classes/, helpers/ | REJECT |

**Reference:** `/Volumes/M-Drive/Coding/.ideacode/MASTER-INDEX.md`

## Output Format

```
## Analysis: {scope}

### Summary
[1-2 sentences]

### Findings
| Category | Severity | Location | Issue | Recommendation |
|----------|----------|----------|-------|----------------|

### Metrics
| Metric | Value | Target |
|--------|-------|--------|

### Recommendations
1. [Priority ordered]
```

## Modifiers

| Modifier | Effect |
|----------|--------|
| .swarm | Multi-agent analysis |
| .deep | Extended analysis |
| .cot | Step-by-step reasoning |
