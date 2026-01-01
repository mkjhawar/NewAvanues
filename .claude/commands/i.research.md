---
description: Web research and deep reasoning | /i.research "topic" | /i.research .think "problem"
---

# /i.research - Web Research

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.research`
Auto-start: API server starts automatically if not running

---


## Usage
`/i.research "<topic>" [.depth <level>]`

## Arguments
| Arg | Options | Default |
|-----|---------|---------|
| topic | Any technology, API, best practice | required |
| `.think` | Deep reasoning mode (no web search, extended thinking) | - |
| `.depth` | quick, standard, comprehensive | standard |

## When to Use
| Scenario | Example |
|----------|---------|
| New library | "Ktor 3.0 features" |
| Version changes | "React 19 migration" |
| Best practices | "Compose navigation patterns 2025" |
| Security | "OAuth 2.1 implementation" |
| APIs | "OpenAI Assistants API v2" |

## Auto-Research Triggers
| Detected Pattern | Action |
|------------------|--------|
| Version numbers | Auto-research if newer than Jan 2025 |
| "latest", "new" | Auto-research current state |
| Unfamiliar API | Auto-research documentation |
| Security topics | Auto-research vulnerabilities |
| Migration | Auto-research guides |

## Process
1. **Identify** - Parse research topic
2. **Query** - Formulate search queries
3. **Search** - Query authoritative sources
4. **Synthesize** - Combine findings
5. **Cite** - Provide sources

## Search Priority
| Source Type | Priority |
|-------------|----------|
| Official docs | 1 (highest) |
| GitHub releases | 2 |
| Stack Overflow | 3 |
| Blog posts | 4 |
| Forums | 5 |

## Output Format
```
## Research: {topic}

### Summary
[Key findings in 2-3 sentences]

### Details
| Aspect | Finding |
|--------|---------|

### Code Example
[If applicable]

### Sources
- [Source 1](url)
- [Source 2](url)
```

## Examples
| Command | Result |
|---------|--------|
| `/i.research "Jetpack Compose 1.7"` | Feature research |
| `/i.research "Ktor vs Spring Boot 2025"` | Comparison |
| `/i.research "KMP iOS interop" .depth comprehensive` | Deep dive |

## Related
| Command | Purpose |
|---------|---------|
| `/ithink` | Deep reasoning (no web) |
| `/ispecify` | Auto-researches tech stack |

## Metadata
- **Command:** `/i.research`
- **Version:** 10.2
- **Uses:** WebSearch tool
