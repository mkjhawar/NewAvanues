---
description: Multi-agent coordination .type .agent .create | /i.swarm .type pr-review
---

# /i.swarm - Multi-Agent Coordination

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.swarm`
Auto-start: API server starts automatically if not running

---


## Usage
`/i.swarm [.type <type>] [.agents <list>] [.mode <mode>]`

## Arguments
| Arg | Options | Default |
|-----|---------|---------|
| `.type` | pr-review, multi-platform, security-audit, performance | - |
| `.agents` | Comma-separated: security,code-review,testing,etc. | by type |
| `.mode` | parallel, sequential, loop, router, background | parallel |
| `.agent` | Create custom agent: `.agent "name"` | - |
| `.scope` | Agent scope: global, project | project |

## Swarm Types
| Type | Agents Activated | Use Case |
|------|-----------------|----------|
| pr-review | security, code-review, testing, design-system | PR validation |
| multi-platform | android, ios, backend, architecture | Cross-platform features |
| security-audit | security, code-review, architecture | Security review |
| performance | performance, architecture, testing | Performance optimization |

## Execution Modes
| Mode | Description | Best For |
|------|-------------|----------|
| parallel | Agents work simultaneously | Independent tasks, speed |
| sequential | Pipeline execution | Dependencies between agents |
| loop | Iterative refinement | Quality until threshold met |
| router | Master routes to specialists | Complex multi-domain |
| background | Agents run async (Claude Code `run_in_background`) | Non-blocking work |

## Claude Code Integration (v2.0.74+)

| Feature | Usage | Benefit |
|---------|-------|---------|
| Background Agents | `run_in_background: true` in Task tool | Non-blocking parallel work |
| Thinking Levels | `.think`, `.think-hard`, `.ultrathink` | Scaled reasoning budget |
| Built-in Explore | `subagent_type: Explore` with thoroughness | Efficient codebase search |
| Session Forking | `--fork-session` | Branch exploration paths |

## Available Agents
| Agent | Capabilities |
|-------|-------------|
| security | OWASP, vulnerability scanning, secrets detection |
| code-review | Quality, best practices, patterns |
| testing | Test generation, coverage, E2E |
| performance | Profiling, bottlenecks, optimization |
| documentation | Auto-docs, API specs |
| architecture | Design patterns, tech debt |
| design-system | UI consistency, accessibility |
| android | Kotlin, Compose, Android-specific |
| ios | Swift, SwiftUI, iOS-specific |
| backend | API, database, server-side |

## Auto-Activation
| Trigger | Condition |
|---------|-----------|
| `/iimplement` | 3+ platforms OR 15+ tasks |
| `/ifix` | Multi-domain bug |
| `.swarm` modifier | Any command |

## Examples
| Command | Result |
|---------|--------|
| `/i.swarm .type pr-review` | Full PR review swarm |
| `/i.swarm .agents security,testing` | Custom 2-agent swarm |
| `/i.swarm .type multi-platform .mode sequential` | Platform pipeline |
| `/ifix .swarm "complex bug"` | Auto swarm for bug |

## Transparency (MANDATORY)

Before swarm activation, announce:

```
SWARM: {type}
AGENTS: {count}x {types}
MODE: consensus voting (threshold: 75%)
EXPECTED: {outcome}
DURATION: {estimate}
OVERRIDE: .stop within 1s
```

During execution:
```
VOTE: {topic}
OPTIONS: {A: count}, {B: count}
CONSENSUS: {winner} | {percentage}%
DISSENT: {count} agents | {reason if >25%}
```

## Consensus Voting

ENABLED: Multi-agent voting with 75% threshold
AGENTS: Dynamic count (1 simple, 3 default, 5 critical)
ROUNDS: Max 3 re-votes before escalation

Process:
1. Spawn N agents per specialty
2. Collect votes independently
3. Calculate consensus percentage
4. If >= 75%: ACCEPT
5. If < 75%: Re-vote (max 3 rounds)
6. If no consensus: Escalate to user

Example:
```
VOTE: Security approach
OPTIONS: JWT RS256 (2), OAuth (1)
CONSENSUS: 66% | THRESHOLD: 75%
ACTION: Re-voting after review

VOTE: Security approach (round 2)
OPTIONS: JWT RS256 (3)
CONSENSUS: 100% for JWT RS256
```

## Output
- Per-agent findings
- Vote tallies and consensus
- Dissenting opinions (if >25%)
- Aggregated recommendation
- Scrum master coordination

## Related
| Command | Purpose |
|---------|---------|
| `/iagent` | Create individual agents |
| `/ifix` | Auto-uses swarm |
| `/iimplement` | Auto-uses swarm |

## Metadata
- **Command:** `/i.swarm`
- **Version:** 10.2
