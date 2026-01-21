# Token-Efficient AI Operations - v1

**Enforced by:** Global CLAUDE.md > HARD RULES > Token Efficiency

Method for optimizing token usage while maximizing throughput and quality.

---

## Objectives

| Goal | Metric |
|------|--------|
| Minimize tokens | Tokens per request |
| Reduce latency | Time to completion |
| Maintain quality | Task success rate |
| General applicability | Works for QA, generation, coding, classification |

---

## Algorithm

```
INPUT: task, user_input, knowledge_base?, constraints?
PROCESS:
  1. Measure input tokens
  2. If tokens > threshold:
     - Truncate history (keep recent + key context)
     - Compress redundant content
     - Use RAG for top-k relevant chunks only
  3. Select prompt pattern (see templates)
  4. Route to appropriate model:
     - Simple/low-risk -> smaller/faster model
     - Complex/high-risk -> larger model
  5. Batch if multiple small requests
OUTPUT: model, prompt, max_tokens, post_process_steps
```

---

## Prompt Patterns (Token-Efficient)

### Pattern 1: Classification
```
Classify: [input]
Categories: A|B|C|D
Output: category only
```

### Pattern 2: Analytical Answer
```
Q: [question]
Context: [minimal relevant context]
Answer in 1-2 sentences.
```

### Pattern 3: Code Change (Diff-Only)
```
Task: [change description]
File: [path]
Output: diff format only, no explanation
```

### Pattern 4: Structured Output
```
Extract from: [input]
Return JSON: {field1, field2, field3}
No prose.
```

---

## Context Management Rules

| Scenario | Action |
|----------|--------|
| History > 10 messages | Keep last 5 + summary of earlier |
| Input > 4000 tokens | Compress: remove redundancy, keep facts |
| Multiple files needed | RAG: top-3 most relevant only |
| Repeated context | Reference by name, don't re-include |

---

## Throughput Techniques

| Technique | When to Use |
|-----------|-------------|
| Batching | Multiple independent small tasks |
| Parallel calls | Independent operations (search + read) |
| Model routing | Simple tasks -> fast model; complex -> full model |
| Targeted reads | Read specific lines, not full files |
| Incremental output | Stream partial results when possible |

---

## Guardrails (Quality Protection)

| Check | Action |
|-------|--------|
| Confidence < threshold | Fallback to verbose mode |
| Critical operation | Include full context, no compression |
| Ambiguous input | Ask clarification vs. guess |
| Minimum context | Never compress below essential facts |

---

## Anti-Patterns (Avoid)

| Anti-Pattern | Efficient Alternative |
|--------------|----------------------|
| Read entire file | Read specific line ranges |
| Repeat instructions | Reference previous |
| Long explanations in output | Structured/diff output only |
| Re-read unchanged files | Cache/remember from session |
| Verbose error messages | Code + 1-line cause |

---

## Evaluation

Compare baseline vs optimized:

| Metric | Measure |
|--------|---------|
| Tokens | Count input + output |
| Latency | Time to response |
| Quality | Task success (manual or automated) |
| Cost | Tokens * rate |

Test on 10-20 representative tasks per category.

---

**Updated:** 2025-11-29
